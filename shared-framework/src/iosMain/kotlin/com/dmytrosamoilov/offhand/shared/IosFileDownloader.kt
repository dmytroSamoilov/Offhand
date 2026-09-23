@file:OptIn(ExperimentalForeignApi::class)

package com.dmytrosamoilov.offhand.shared

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadDelegateProtocol
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSURLSessionTaskStateCompleted
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

// One background NSURLSession for the whole process: the system keeps its
// transfers going while the app is suspended and relaunches the app when they
// finish, which a default session does not survive. Every mutation of the
// waiter map runs on the session's serial delegate queue, so no lock is needed.
class IosFileDownloader {

    class DownloadFailedException(message: String) : Exception(message)

    private class Waiter(
        val onProgress: (bytesDownloaded: Long, bytesTotal: Long) -> Unit,
        val continuation: CancellableContinuation<Boolean>,
    )

    private val delegateQueue = NSOperationQueue().apply { maxConcurrentOperationCount = 1 }
    private val waiters = mutableMapOf<ULong, Waiter>()
    private var onBackgroundEventsDelivered: (() -> Unit)? = null
    private val session: NSURLSession by lazy { createSession() }

    suspend fun download(
        url: String,
        destinationPath: String,
        onProgress: (bytesDownloaded: Long, bytesTotal: Long) -> Unit,
    ): Boolean = suspendCancellableCoroutine { continuation ->
        delegateQueue.addOperationWithBlock {
            session.getAllTasksWithCompletionHandler { tasks ->
                val task = runningTaskFor(url, tasks) ?: newTask(url, destinationPath)
                attach(task, Waiter(onProgress, continuation))
            }
        }
    }

    // Called when the system relaunched or woke the app for this session's
    // events; touching the session reconnects the delegate so they are delivered.
    fun handleBackgroundEvents(identifier: String, completion: () -> Unit) {
        if (identifier != SESSION_IDENTIFIER) {
            completion()
            return
        }
        delegateQueue.addOperationWithBlock {
            onBackgroundEventsDelivered = completion
            session
        }
    }

    private fun runningTaskFor(url: String, tasks: List<*>?): NSURLSessionDownloadTask? =
        tasks.orEmpty().filterIsInstance<NSURLSessionDownloadTask>().firstOrNull { task ->
            task.originalRequest?.URL?.absoluteString == url &&
                task.state != NSURLSessionTaskStateCompleted
        }

    // The destination travels with the task so a download that finishes while
    // nobody awaits it (the app was relaunched for it) still lands in place.
    private fun newTask(url: String, destinationPath: String): NSURLSessionDownloadTask =
        session.downloadTaskWithURL(NSURL(string = url)).apply {
            taskDescription = destinationPath
        }

    private fun attach(task: NSURLSessionDownloadTask, waiter: Waiter) {
        if (!waiter.continuation.isActive) return
        waiters[task.taskIdentifier] = waiter
        waiter.continuation.invokeOnCancellation {
            delegateQueue.addOperationWithBlock { waiters.remove(task.taskIdentifier) }
        }
        task.resume()
    }

    private fun createSession(): NSURLSession {
        val configuration =
            NSURLSessionConfiguration.backgroundSessionConfigurationWithIdentifier(SESSION_IDENTIFIER)
        configuration.sessionSendsLaunchEvents = true
        configuration.discretionary = false
        return NSURLSession.sessionWithConfiguration(
            configuration,
            delegate = SessionDelegate(),
            delegateQueue = delegateQueue,
        )
    }

    private fun moveToDestination(temporaryUrl: NSURL, destinationPath: String): Boolean {
        val fileManager = NSFileManager.defaultManager
        fileManager.removeItemAtPath(destinationPath, error = null)
        return fileManager.moveItemAtPath(
            temporaryUrl.path.orEmpty(),
            toPath = destinationPath,
            error = null,
        )
    }

    private inner class SessionDelegate : NSObject(), NSURLSessionDownloadDelegateProtocol {

        override fun URLSession(
            session: NSURLSession,
            downloadTask: NSURLSessionDownloadTask,
            didWriteData: Long,
            totalBytesWritten: Long,
            totalBytesExpectedToWrite: Long,
        ) {
            waiters[downloadTask.taskIdentifier]?.onProgress(totalBytesWritten, totalBytesExpectedToWrite)
        }

        override fun URLSession(
            session: NSURLSession,
            downloadTask: NSURLSessionDownloadTask,
            didFinishDownloadingToURL: NSURL,
        ) {
            val destinationPath = downloadTask.taskDescription
            val moved = destinationPath != null && moveToDestination(didFinishDownloadingToURL, destinationPath)
            waiters.remove(downloadTask.taskIdentifier)?.continuation?.resume(moved)
        }

        override fun URLSession(
            session: NSURLSession,
            task: NSURLSessionTask,
            didCompleteWithError: NSError?,
        ) {
            if (didCompleteWithError == null) return
            waiters.remove(task.taskIdentifier)?.continuation?.resume(false)
        }

        override fun URLSessionDidFinishEventsForBackgroundURLSession(session: NSURLSession) {
            val completion = onBackgroundEventsDelivered ?: return
            onBackgroundEventsDelivered = null
            dispatch_async(dispatch_get_main_queue()) { completion() }
        }
    }

    private companion object {
        const val SESSION_IDENTIFIER = "com.dmytrosamoilov.offhand.on-device-ai"
    }
}
