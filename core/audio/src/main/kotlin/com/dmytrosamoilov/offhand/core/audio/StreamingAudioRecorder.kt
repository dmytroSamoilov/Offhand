package com.dmytrosamoilov.offhand.core.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

@Singleton
class StreamingAudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val active = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)

    private val mutableVad = MutableStateFlow(VadSnapshot())
    val vad: StateFlow<VadSnapshot> = mutableVad.asStateFlow()

    private val mutableExternalInputName = MutableStateFlow<String?>(null)
    val externalInputName: StateFlow<String?> = mutableExternalInputName.asStateFlow()

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    fun pause() {
        paused.set(true)
    }

    fun resume() {
        paused.set(false)
    }

    fun stop() {
        active.set(false)
    }

    fun resetVad() {
        mutableVad.value = VadSnapshot()
    }

    @SuppressLint("MissingPermission")
    fun recordStream(
        minChunkMs: Long = DEFAULT_MIN_CHUNK_MS,
        maxChunkMs: Long = DEFAULT_MAX_CHUNK_MS,
        silenceGapMs: Long = DEFAULT_SILENCE_GAP_MS,
        silenceDb: Float = DEFAULT_SILENCE_DB,
        pcmSink: ((ByteArray) -> Unit)? = null,
    ): Flow<AudioChunk> = callbackFlow {
        if (!hasPermission()) {
            close(SecurityException("RECORD_AUDIO permission not granted"))
            return@callbackFlow
        }
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        require(minBuffer > 0) { "AudioRecord.getMinBufferSize failed ($minBuffer)" }
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL,
            ENCODING,
            (minBuffer * 4).coerceAtLeast(FRAME_BYTES * 8),
        )
        val audioManager = context.getSystemService(AudioManager::class.java)
        val deviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                applyInputRouting(recorder)
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                applyInputRouting(recorder)
            }
        }
        audioManager?.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
        applyInputRouting(recorder)

        val frame = ShortArray(FRAME_SAMPLES)
        val chunkBuffer = ByteArrayOutputStream()
        var chunkId = 1
        var chunkRecordedMs = 0L
        var totalRecordedMs = 0L
        var consecutiveSilentMs = 0L

        paused.set(false)
        active.set(true)
        mutableVad.value = VadSnapshot()

        try {
            recorder.startRecording()

            while (active.get()) {
                val read = recorder.read(frame, 0, frame.size)
                if (read <= 0) continue

                if (paused.get()) {
                    // Keep draining the hardware buffer, but drop the audio so
                    // paused stretches never reach the note.
                    consecutiveSilentMs = 0L
                    mutableVad.value = VadSnapshot(
                        rmsDb = VadSnapshot.SILENCE_DB,
                        isSilent = true,
                        isPaused = true,
                        chunkElapsedMs = chunkRecordedMs,
                        totalElapsedMs = totalRecordedMs,
                        currentChunkId = chunkId,
                    )
                    continue
                }

                val rmsDb = rmsDb(frame, read)
                val isSilent = rmsDb < silenceDb

                val frameBytes = toLittleEndianBytes(frame, read)
                chunkBuffer.write(frameBytes, 0, frameBytes.size)
                pcmSink?.invoke(frameBytes)

                val frameMs = (read.toLong() * 1000L) / SAMPLE_RATE
                if (isSilent) consecutiveSilentMs += frameMs else consecutiveSilentMs = 0L
                chunkRecordedMs += frameMs
                totalRecordedMs += frameMs

                mutableVad.value = VadSnapshot(
                    rmsDb = rmsDb,
                    isSilent = isSilent,
                    chunkElapsedMs = chunkRecordedMs,
                    totalElapsedMs = totalRecordedMs,
                    currentChunkId = chunkId,
                )

                val boundary = when {
                    chunkRecordedMs >= maxChunkMs -> ChunkBoundaryReason.MAX_DURATION
                    chunkRecordedMs >= minChunkMs && consecutiveSilentMs >= silenceGapMs ->
                        ChunkBoundaryReason.SILENCE_GAP

                    else -> null
                }
                if (boundary != null) {
                    val pcm = chunkBuffer.toByteArray()
                    chunkBuffer.reset()
                    trySendChunk(chunkId, pcm, chunkRecordedMs, boundary)
                    chunkId += 1
                    chunkRecordedMs = 0L
                    consecutiveSilentMs = 0L
                }
            }
        } finally {
            audioManager?.unregisterAudioDeviceCallback(deviceCallback)
            audioManager?.clearCommunicationDevice()
            runCatching { recorder.stop() }
            runCatching { recorder.release() }
            mutableExternalInputName.value = null
            active.set(false)
            paused.set(false)
        }

        val tail = chunkBuffer.toByteArray()
        if (tail.isNotEmpty()) {
            val tailMs = (tail.size.toLong() / 2L * 1000L) / SAMPLE_RATE
            trySendChunk(chunkId, tail, tailMs, ChunkBoundaryReason.USER_STOP)
        }
        close()
        awaitClose { active.set(false) }
    }.flowOn(Dispatchers.IO)

    private fun SendChannel<AudioChunk>.trySendChunk(
        id: Int,
        pcm: ByteArray,
        durationMs: Long,
        reason: ChunkBoundaryReason,
    ) {
        if (pcm.isEmpty()) return
        val wav = WavCodec.wrap(pcm, SAMPLE_RATE, channels = 1, bitsPerSample = 16)
        trySend(AudioChunk(id = id, wav = wav, durationMs = durationMs, reason = reason))
    }

    private fun applyInputRouting(recorder: AudioRecord) {
        if (recorder.state != AudioRecord.STATE_INITIALIZED) return
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return
        val device = selectExternalInputDevice(audioManager)
        val routed = when {
            device == null -> null
            device.isBluetoothInput() -> device.takeIf { activateBluetoothInput(audioManager, it) }
            else -> device
        }
        if (routed == null || !routed.isBluetoothInput()) {
            audioManager.clearCommunicationDevice()
        }
        recorder.preferredDevice = routed
        mutableExternalInputName.value = routed?.let { it.productName?.toString().orEmpty() }
    }

    private fun selectExternalInputDevice(audioManager: AudioManager): AudioDeviceInfo? =
        audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            .filter { it.type in EXTERNAL_INPUT_PRIORITY }
            .minByOrNull { EXTERNAL_INPUT_PRIORITY.indexOf(it.type) }

    private fun activateBluetoothInput(
        audioManager: AudioManager,
        device: AudioDeviceInfo,
    ): Boolean {
        val candidates = audioManager.availableCommunicationDevices
            .filter { it.type == device.type }
        val communicationDevice = candidates.firstOrNull { it.address == device.address }
            ?: candidates.firstOrNull()
        return communicationDevice?.let(audioManager::setCommunicationDevice) == true
    }

    private fun AudioDeviceInfo.isBluetoothInput(): Boolean =
        type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO

    private fun rmsDb(frame: ShortArray, count: Int): Float {
        if (count <= 0) return VadSnapshot.SILENCE_DB
        var sumOfSquares = 0.0
        for (i in 0 until count) {
            val normalized = frame[i].toDouble() / 32768.0
            sumOfSquares += normalized * normalized
        }
        val rms = sqrt(sumOfSquares / count)
        if (rms <= 1e-9) return VadSnapshot.SILENCE_DB
        return (20.0 * log10(rms)).toFloat()
    }

    private fun toLittleEndianBytes(frame: ShortArray, count: Int): ByteArray {
        val bytes = ByteArray(count * 2)
        for (i in 0 until count) {
            val sample = frame[i].toInt()
            bytes[i * 2] = (sample and 0xff).toByte()
            bytes[i * 2 + 1] = ((sample ushr 8) and 0xff).toByte()
        }
        return bytes
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        const val FRAME_SAMPLES = 800
        const val FRAME_BYTES = FRAME_SAMPLES * 2
        const val DEFAULT_MIN_CHUNK_MS = 25_000L
        const val DEFAULT_MAX_CHUNK_MS = 35_000L
        const val DEFAULT_SILENCE_GAP_MS = 300L
        const val DEFAULT_SILENCE_DB = -45f
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private val EXTERNAL_INPUT_PRIORITY = listOf(
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
        )
    }
}
