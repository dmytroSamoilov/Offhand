package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource

class UnavailableAudioDecoder : AudioDecoder {

    override suspend fun decode(
        source: AudioImportSource,
        onPcm: (bytes: ByteArray, length: Int) -> Unit,
        onProgress: (Float) -> Unit,
    ): DecodedAudio = throw AudioImportException.Unsupported()

    override fun discard(source: AudioImportSource) = Unit
}
