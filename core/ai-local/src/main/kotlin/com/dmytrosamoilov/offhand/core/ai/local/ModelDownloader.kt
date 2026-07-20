package com.dmytrosamoilov.offhand.core.ai.local

import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor

sealed interface DownloadProgress {
    data class InProgress(
        val progress: Float,
        val bytesDownloaded: Long,
        val bytesTotal: Long,
    ) : DownloadProgress

    data class Completed(val file: File) : DownloadProgress
    data class Failed(val message: String) : DownloadProgress
}

internal object DownloadErrorMessages {
    fun describeHttpFailure(code: Int, message: String): String = when (code) {
        401, 403 -> "Access denied ($code). The model host rejected the download."
        404 -> "Model file not found (404). Verify the catalog entry."
        else -> "HTTP $code — $message"
    }
}

@Singleton
class ModelDownloader @Inject constructor() {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .followRedirects(true)
        .followSslRedirects(true)
        .apply {
            if (BuildConfig.DEBUG) {
                val logger = HttpLoggingInterceptor { line -> GenAiLog.logHttp(line) }
                    .setLevel(HttpLoggingInterceptor.Level.HEADERS)
                logger.redactHeader("Authorization")
                logger.redactHeader("Cookie")
                addInterceptor(logger)
            }
        }
        .build()

    fun download(
        url: String,
        destination: File,
    ): Flow<DownloadProgress> = flow {
        val tempFile = File(destination.parentFile, destination.name + ".part")
        try {
            val request = Request.Builder().url(url).build()
            GenAiLog.logHttp("download → $url")
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    GenAiLog.logHttp("← ${response.code} ${response.message} for $url")
                    emit(
                        DownloadProgress.Failed(
                            DownloadErrorMessages.describeHttpFailure(response.code, response.message),
                        ),
                    )
                    return@flow
                }
                GenAiLog.logHttp("← ${response.code} ${response.message} (streaming body)")
                val body = response.body
                    ?: return@flow emit(DownloadProgress.Failed("Empty response body"))
                val contentLength = body.contentLength().coerceAtLeast(1L)
                var bytesRead = 0L
                val buffer = ByteArray(BUFFER_SIZE)

                body.byteStream().use { input ->
                    tempFile.outputStream().buffered().use { output ->
                        var read = input.read(buffer)
                        var lastEmittedPercent = -1
                        while (read != -1) {
                            output.write(buffer, 0, read)
                            bytesRead += read
                            val progress = bytesRead.toFloat() / contentLength
                            val percent = (progress * 100).toInt()
                            if (percent != lastEmittedPercent) {
                                emit(DownloadProgress.InProgress(progress, bytesRead, contentLength))
                                lastEmittedPercent = percent
                            }
                            read = input.read(buffer)
                        }
                    }
                }
            }
            if (!tempFile.renameTo(destination)) {
                emit(DownloadProgress.Failed("Failed to finalize model file"))
                return@flow
            }
            emit(DownloadProgress.Completed(destination))
        } catch (t: Throwable) {
            tempFile.delete()
            GenAiLog.error("model-download", t)
            emit(DownloadProgress.Failed(t.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val BUFFER_SIZE = 8 * 1024
    }
}
