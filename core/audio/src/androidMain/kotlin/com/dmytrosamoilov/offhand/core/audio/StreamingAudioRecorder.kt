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
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

class StreamingAudioRecorder(
    private val context: Context,
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

        val chunker = AudioChunker(SAMPLE_RATE, minChunkMs, maxChunkMs, silenceGapMs, silenceDb)
        val frame = ShortArray(FRAME_SAMPLES)

        paused.set(false)
        active.set(true)
        mutableVad.value = VadSnapshot()

        try {
            recorder.startRecording()

            while (active.get()) {
                val read = recorder.read(frame, 0, frame.size)
                if (read <= 0) continue

                val outcome = chunker.onFrame(frame, read, paused.get())
                outcome.frameBytes?.let { pcmSink?.invoke(it) }
                mutableVad.value = outcome.vad
                outcome.completedChunk?.let { trySendChunk(it) }
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

        chunker.drainTail()?.let { trySendChunk(it) }
        close()
        awaitClose { active.set(false) }
    }.flowOn(Dispatchers.IO)

    private fun SendChannel<AudioChunk>.trySendChunk(chunk: AudioChunker.PcmChunk) {
        if (chunk.pcm.isEmpty()) return
        val wav = WavCodec.wrap(chunk.pcm, SAMPLE_RATE, channels = 1, bitsPerSample = 16)
        trySend(
            AudioChunk(
                id = chunk.id,
                wav = wav,
                durationMs = chunk.durationMs,
                speechMs = chunk.speechMs,
                reason = chunk.reason,
            ),
        )
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

    companion object {
        const val SAMPLE_RATE = 16_000
        const val FRAME_SAMPLES = 800
        const val FRAME_BYTES = FRAME_SAMPLES * 2
        // The offline Whisper decoder discards everything past 30 seconds per
        // decode, so a chunk must never reach that cap.
        const val DEFAULT_MIN_CHUNK_MS = 20_000L
        const val DEFAULT_MAX_CHUNK_MS = 29_000L
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
