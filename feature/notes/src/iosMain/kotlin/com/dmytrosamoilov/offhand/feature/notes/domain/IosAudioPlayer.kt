@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.dmytrosamoilov.offhand.feature.notes.domain

import co.touchlab.kermit.Logger
import com.dmytrosamoilov.offhand.core.audio.SilenceJumper
import com.dmytrosamoilov.offhand.core.audio.SilenceSkip
import com.dmytrosamoilov.offhand.core.audio.WavCodec
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileProtectionCompleteUnlessOpen
import platform.Foundation.NSFileProtectionKey
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.closeFile
import platform.Foundation.create
import platform.Foundation.fileHandleForWritingAtPath
import platform.Foundation.seekToFileOffset
import platform.Foundation.writeData

class IosAudioPlayer(
    private val audioStore: EncryptedAudioStore,
) : AudioPlayer {

    private val mutableState = MutableStateFlow(AudioPlaybackState())
    override val state: StateFlow<AudioPlaybackState> = mutableState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: AVAudioPlayer? = null
    private var progressJob: Job? = null
    private var loadJob: Job? = null
    private var silenceJumper = SilenceJumper(emptyList())

    override fun load(audioFileName: String) {
        reset()
        val previousLoad = loadJob
        loadJob = scope.launch {
            previousLoad?.cancelAndJoin()
            try {
                val wavPath = withContext(Dispatchers.IO) { writePlaybackWav(audioFileName) }
                val loaded = AVAudioPlayer(NSURL.fileURLWithPath(wavPath), error = null)
                loaded.prepareToPlay()
                player = loaded
                mutableState.value = AudioPlaybackState(
                    isLoaded = true,
                    durationMs = (loaded.duration * MS_PER_SECOND).toLong(),
                )
            } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                Logger.withTag(LOG_TAG).w(t) { "Audio unavailable for playback" }
                mutableState.value = AudioPlaybackState()
            }
        }
    }

    override fun play() {
        val active = player ?: return
        active.play()
        mutableState.update { it.copy(isPlaying = true) }
        progressJob?.cancel()
        progressJob = scope.launch {
            while (active.isPlaying()) {
                jumpOverSilence(active)
                publishPosition(active)
                delay(if (silenceJumper.isEmpty) PROGRESS_INTERVAL_MS else SKIP_CHECK_INTERVAL_MS)
            }
            silenceJumper.onSeek(positionOf(active))
            publishPosition(active)
            mutableState.update { it.copy(isPlaying = false) }
        }
    }

    override fun pause() {
        player?.pause()
        progressJob?.cancel()
        mutableState.update { it.copy(isPlaying = false) }
    }

    override fun seekTo(positionMs: Long) {
        val active = player ?: return
        silenceJumper.onSeek(positionMs)
        active.currentTime = positionMs / MS_PER_SECOND
        publishPosition(active)
    }

    override fun setSilenceSkips(skips: List<SilenceSkip>) {
        silenceJumper = SilenceJumper(skips)
    }

    override fun reset() {
        silenceJumper = SilenceJumper(emptyList())
        loadJob?.cancel()
        progressJob?.cancel()
        player?.stop()
        player = null
        NSFileManager.defaultManager.removeItemAtPath(playbackWavPath(), error = null)
        mutableState.value = AudioPlaybackState()
    }

    override fun release() {
        reset()
        scope.cancel()
    }

    // AVAudioPlayer has no hook mid-buffer, so the progress loop checks often
    // while there is anything to skip and jumps once it is inside a pause.
    private fun jumpOverSilence(active: AVAudioPlayer) {
        val targetMs = silenceJumper.jumpTargetAt(positionOf(active)) ?: return
        active.currentTime = targetMs / MS_PER_SECOND
    }

    private fun publishPosition(active: AVAudioPlayer) {
        val positionMs = silenceJumper.shownPosition(positionOf(active))
        mutableState.update { it.copy(positionMs = positionMs) }
    }

    private fun positionOf(active: AVAudioPlayer): Long = (active.currentTime * MS_PER_SECOND).toLong()

    private fun playbackWavPath(): String {
        val caches = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
            .first() as String
        return "$caches/$PLAYBACK_FILE"
    }

    private suspend fun writePlaybackWav(audioFileName: String): String {
        val path = playbackWavPath()
        // createFileAtPath leaves an existing file untouched, so a stale copy must go first.
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
        NSFileManager.defaultManager.createFileAtPath(
            path,
            contents = null,
            attributes = mapOf(NSFileProtectionKey to NSFileProtectionCompleteUnlessOpen),
        )
        val handle = requireNotNull(NSFileHandle.fileHandleForWritingAtPath(path))
        var pcmBytes = 0
        try {
            handle.writeData(ByteArray(WavCodec.HEADER_BYTES).toNsData())
            val input = audioStore.openForRead(audioFileName)
            try {
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val chunk = input.read(COPY_CHUNK_BYTES) ?: break
                    handle.writeData(chunk.toNsData())
                    pcmBytes += chunk.size
                }
            } finally {
                input.close()
            }
            handle.seekToFileOffset(0uL)
            handle.writeData(
                WavCodec.header(pcmBytes, SAMPLE_RATE, CHANNELS, BITS_PER_SAMPLE).toNsData(),
            )
        } finally {
            handle.closeFile()
        }
        return path
    }

    private fun ByteArray.toNsData(): NSData = if (isEmpty()) {
        NSData()
    } else {
        usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }
    }

    private companion object {
        const val LOG_TAG = "IosAudioPlayer"
        const val PLAYBACK_FILE = "playback.wav"
        const val MS_PER_SECOND = 1000.0
        const val PROGRESS_INTERVAL_MS = 200L
        const val SKIP_CHECK_INTERVAL_MS = 40L
        const val SAMPLE_RATE = 16_000
        const val CHANNELS = 1
        const val BITS_PER_SAMPLE = 16
        const val COPY_CHUNK_BYTES = 512L * 1024L
    }
}
