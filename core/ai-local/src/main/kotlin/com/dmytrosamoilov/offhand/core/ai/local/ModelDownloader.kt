package com.dmytrosamoilov.offhand.core.ai.local

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor

internal const val PART_FILE_SUFFIX = ".part"

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
        val tempFile = File(destination.parentFile, destination.name + PART_FILE_SUFFIX)
        var attempt = 1
        while (true) {
            try {
                val httpFailure = fetchInto(url, tempFile)
                if (httpFailure != null) {
                    tempFile.delete()
                    emit(httpFailure)
                    return@flow
                }
                break
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (io: IOException) {
                // The temp file is kept: the next attempt resumes from its size.
                GenAiLog.warn(
                    "model-download",
                    io,
                    "attempt $attempt/$MAX_ATTEMPTS failed, keeping ${tempFile.length()} bytes",
                )
                if (attempt >= MAX_ATTEMPTS) {
                    emit(DownloadProgress.Failed(io.message ?: "Download failed"))
                    return@flow
                }
                delay(attempt * RETRY_DELAY_MS)
                attempt++
            } catch (t: Throwable) {
                GenAiLog.error("model-download", t)
                emit(DownloadProgress.Failed(t.message ?: "Download failed"))
                return@flow
            }
        }
        if (!tempFile.renameTo(destination)) {
            emit(DownloadProgress.Failed("Failed to finalize model file"))
            return@flow
        }
        emit(DownloadProgress.Completed(destination))
    }.flowOn(Dispatchers.IO)

    private suspend fun FlowCollector<DownloadProgress>.fetchInto(
        url: String,
        tempFile: File,
    ): DownloadProgress.Failed? {
        val resumeFrom = tempFile.length()
        val request = Request.Builder().url(url).apply {
            if (resumeFrom > 0) header("Range", "bytes=$resumeFrom-")
        }.build()
        GenAiLog.logHttp("download → $url (resume from $resumeFrom)")
        client.newCall(request).execute().use { response ->
            if (response.code == HTTP_RANGE_NOT_SATISFIABLE) {
                response.close()
                tempFile.delete()
                return fetchInto(url, tempFile)
            }
            if (!response.isSuccessful) {
                GenAiLog.logHttp("← ${response.code} ${response.message} for $url")
                return DownloadProgress.Failed(
                    DownloadErrorMessages.describeHttpFailure(response.code, response.message),
                )
            }
            GenAiLog.logHttp("← ${response.code} ${response.message} (streaming body)")
            val body = response.body
                ?: return DownloadProgress.Failed("Empty response body")
            val isResumed = response.code == HTTP_PARTIAL_CONTENT && resumeFrom > 0
            val startOffset = if (isResumed) resumeFrom else 0L
            val bytesTotal = (startOffset + body.contentLength()).coerceAtLeast(1L)
            var bytesDownloaded = startOffset
            var lastEmittedPercent = -1
            val buffer = ByteArray(BUFFER_SIZE)
            body.byteStream().use { input ->
                FileOutputStream(tempFile, isResumed).buffered().use { output ->
                    var read = input.read(buffer)
                    while (read != -1) {
                        output.write(buffer, 0, read)
                        bytesDownloaded += read
                        val progress = bytesDownloaded.toFloat() / bytesTotal
                        val percent = (progress * 100).toInt()
                        if (percent != lastEmittedPercent) {
                            emit(DownloadProgress.InProgress(progress, bytesDownloaded, bytesTotal))
                            lastEmittedPercent = percent
                        }
                        read = input.read(buffer)
                    }
                }
            }
        }
        return null
    }

    private companion object {
        const val BUFFER_SIZE = 8 * 1024
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 2_000L
        const val HTTP_PARTIAL_CONTENT = 206
        const val HTTP_RANGE_NOT_SATISFIABLE = 416
    }
}
