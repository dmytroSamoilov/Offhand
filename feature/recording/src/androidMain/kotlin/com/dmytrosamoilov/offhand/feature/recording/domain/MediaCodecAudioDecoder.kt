package com.dmytrosamoilov.offhand.feature.recording.domain

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.dmytrosamoilov.offhand.core.audio.PcmResampler
import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class MediaCodecAudioDecoder : AudioDecoder {

    override suspend fun decode(
        source: AudioImportSource,
        onPcm: (bytes: ByteArray, length: Int) -> Unit,
        onProgress: (Float) -> Unit,
    ): DecodedAudio = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(source.handle)
            val track = audioTrack(extractor)
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val durationUs = format.durationUs()
            if (durationUs / MICROS_PER_MILLI > AudioImportLimits.MAX_DURATION_MS) {
                throw AudioImportException.TooLong(durationUs / MICROS_PER_MILLI)
            }
            decodeTrack(extractor, format, durationUs, onPcm, onProgress)
        } catch (rejected: AudioImportException) {
            throw rejected
        } catch (failure: Exception) {
            throw AudioImportException.Unreadable(failure)
        } finally {
            extractor.release()
        }
    }

    override fun discard(source: AudioImportSource) {
        File(source.handle).delete()
    }

    private fun audioTrack(extractor: MediaExtractor): Int =
        (0 until extractor.trackCount).firstOrNull { index ->
            extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith(AUDIO_MIME_PREFIX) == true
        } ?: throw AudioImportException.Unsupported()

    private fun MediaFormat.durationUs(): Long =
        if (containsKey(MediaFormat.KEY_DURATION)) getLong(MediaFormat.KEY_DURATION) else 0L

    private suspend fun decodeTrack(
        extractor: MediaExtractor,
        format: MediaFormat,
        durationUs: Long,
        onPcm: (ByteArray, Int) -> Unit,
        onProgress: (Float) -> Unit,
    ): DecodedAudio {
        val codec = createDecoder(format)
        val pump = PcmPump(onPcm)
        try {
            codec.configure(format, null, null, 0)
            codec.start()
            drive(codec, extractor, durationUs, pump, onProgress)
        } finally {
            runCatching { codec.stop() }
            codec.release()
        }
        return DecodedAudio(durationMs = pump.outputFrames * MILLIS_PER_SECOND / OUTPUT_RATE)
    }

    private fun createDecoder(format: MediaFormat): MediaCodec {
        val mime = format.getString(MediaFormat.KEY_MIME) ?: throw AudioImportException.Unsupported()
        return try {
            MediaCodec.createDecoderByType(mime)
        } catch (unsupported: Exception) {
            throw AudioImportException.Unsupported()
        }
    }

    private suspend fun drive(
        codec: MediaCodec,
        extractor: MediaExtractor,
        durationUs: Long,
        pump: PcmPump,
        onProgress: (Float) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        while (!outputDone) {
            ensureActive()
            if (!inputDone) inputDone = feedInput(codec, extractor)
            outputDone = drainOutput(codec, info, pump)
            if (durationUs > 0) onProgress((info.presentationTimeUs.toFloat() / durationUs).coerceIn(0f, 1f))
        }
        onProgress(1f)
    }

    private fun feedInput(codec: MediaCodec, extractor: MediaExtractor): Boolean {
        val index = codec.dequeueInputBuffer(TIMEOUT_US)
        if (index < 0) return false
        val buffer = codec.getInputBuffer(index) ?: return false
        val size = extractor.readSampleData(buffer, 0)
        if (size < 0) {
            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            return true
        }
        codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
        extractor.advance()
        return false
    }

    private fun drainOutput(codec: MediaCodec, info: MediaCodec.BufferInfo, pump: PcmPump): Boolean {
        val index = codec.dequeueOutputBuffer(info, TIMEOUT_US)
        if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) pump.formatChanged(codec.outputFormat)
        if (index < 0) return false
        codec.getOutputBuffer(index)?.let { buffer ->
            if (info.size > 0) pump.push(buffer, info.offset, info.size, codec.outputFormat)
        }
        codec.releaseOutputBuffer(index, false)
        return info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
    }

    private class PcmPump(private val onPcm: (ByteArray, Int) -> Unit) {
        var outputFrames = 0L
        private var sampleRate = OUTPUT_RATE
        private var channels = 1
        private var isFloat = false

        fun formatChanged(format: MediaFormat) {
            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            isFloat = format.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                format.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
        }

        fun push(buffer: ByteBuffer, offset: Int, size: Int, format: MediaFormat) {
            formatChanged(format)
            val slice = buffer.duplicate().order(ByteOrder.nativeOrder())
            slice.position(offset)
            slice.limit(offset + size)
            val mono = PcmResampler.downmix(readSamples(slice), channels)
            val resampled = PcmResampler.resample(mono, sampleRate, OUTPUT_RATE)
            outputFrames += resampled.size
            onPcm(toLittleEndian(resampled), resampled.size * BYTES_PER_SAMPLE)
        }

        private fun readSamples(slice: ByteBuffer): ShortArray = if (isFloat) {
            val floats = slice.asFloatBuffer()
            ShortArray(floats.remaining()) { (floats.get(it).coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort() }
        } else {
            val shorts = slice.asShortBuffer()
            ShortArray(shorts.remaining()) { shorts.get(it) }
        }

        private fun toLittleEndian(samples: ShortArray): ByteArray {
            val bytes = ByteBuffer.allocate(samples.size * BYTES_PER_SAMPLE).order(ByteOrder.LITTLE_ENDIAN)
            samples.forEach { bytes.putShort(it) }
            return bytes.array()
        }
    }

    private companion object {
        const val AUDIO_MIME_PREFIX = "audio/"
        const val OUTPUT_RATE = AudioRecorder.SAMPLE_RATE
        const val BYTES_PER_SAMPLE = 2
        const val TIMEOUT_US = 10_000L
        const val MICROS_PER_MILLI = 1_000L
        const val MILLIS_PER_SECOND = 1_000L
    }
}
