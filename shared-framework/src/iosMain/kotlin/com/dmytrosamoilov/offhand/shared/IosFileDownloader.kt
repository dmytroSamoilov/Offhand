@file:OptIn(ExperimentalForeignApi::class)

package com.dmytrosamoilov.offhand.shared

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionDownloadDelegateProtocol
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSURLSessionTask
import platform.darwin.NSObject

class IosFileDownloader {

    class DownloadFailedException(message: String) : Exception(message)

    suspend fun download(
        url: String,
        destinationPath: String,
        onProgress: (bytesDownloaded: Long, bytesTotal: Long) -> Unit,
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val delegate = object : NSObject(), NSURLSessionDownloadDelegateProtocol {
            override fun URLSession(
                session: NSURLSession,
                downloadTask: NSURLSessionDownloadTask,
                didWriteData: Long,
                totalBytesWritten: Long,
                totalBytesExpectedToWrite: Long,
            ) {
                onProgress(totalBytesWritten, totalBytesExpectedToWrite)
            }

            override fun URLSession(
                session: NSURLSession,
                downloadTask: NSURLSessionDownloadTask,
                didFinishDownloadingToURL: NSURL,
            ) {
                val fileManager = NSFileManager.defaultManager
                fileManager.removeItemAtPath(destinationPath, error = null)
                val moved = fileManager.moveItemAtPath(
                    didFinishDownloadingToURL.path.orEmpty(),
                    toPath = destinationPath,
                    error = null,
                )
                if (continuation.isActive) continuation.resume(moved)
                session.finishTasksAndInvalidate()
            }

            override fun URLSession(
                session: NSURLSession,
                task: NSURLSessionTask,
                didCompleteWithError: NSError?,
            ) {
                if (didCompleteWithError != null && continuation.isActive) {
                    continuation.resume(false)
                }
                session.finishTasksAndInvalidate()
            }
        }
        val session = NSURLSession.sessionWithConfiguration(
            platform.Foundation.NSURLSessionConfiguration.defaultSessionConfiguration,
            delegate = delegate,
            delegateQueue = null,
        )
        val task = session.downloadTaskWithURL(NSURL(string = url))
        continuation.invokeOnCancellation {
            task.cancel()
            session.invalidateAndCancel()
        }
        task.resume()
    }
}
