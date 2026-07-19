package com.dmytrosamoilov.offhand.core.ai.local

import timber.log.Timber

internal object GenAiLog {
    private const val TAG_RAW = "GenAI.raw"
    private const val TAG_HTTP = "GenAI.http"
    private const val CHUNK_SIZE = 3000

    fun logModelOutput(stage: String, payload: String) {
        if (!BuildConfig.DEBUG) return
        Timber.tag(TAG_RAW).d("─── %s ───", stage)
        payload.chunked(CHUNK_SIZE).forEach { Timber.tag(TAG_RAW).d(it) }
        Timber.tag(TAG_RAW).d("─── /%s ───", stage)
    }

    fun logHttp(line: String) {
        if (!BuildConfig.DEBUG) return
        Timber.tag(TAG_HTTP).d(line)
    }

    fun warn(stage: String, throwable: Throwable? = null, message: String? = null) {
        val composed = composeMessage(stage, message, throwable)
        if (throwable != null) Timber.tag(TAG_RAW).w(throwable, composed)
        else Timber.tag(TAG_RAW).w(composed)
    }

    fun error(stage: String, throwable: Throwable? = null, message: String? = null) {
        val composed = composeMessage(stage, message, throwable)
        if (throwable != null) Timber.tag(TAG_RAW).e(throwable, composed)
        else Timber.tag(TAG_RAW).e(composed)
    }

    private fun composeMessage(stage: String, message: String?, throwable: Throwable?): String {
        val detail = message ?: throwable?.message ?: throwable?.javaClass?.simpleName ?: "<no message>"
        return "$stage: $detail"
    }
}
