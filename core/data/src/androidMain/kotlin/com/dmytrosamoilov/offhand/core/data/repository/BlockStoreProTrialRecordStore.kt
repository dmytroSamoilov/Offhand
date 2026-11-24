package com.dmytrosamoilov.offhand.core.data.repository

import android.content.Context
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRecord
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRecordStore
import com.google.android.gms.auth.blockstore.Blockstore
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest
import com.google.android.gms.auth.blockstore.StoreBytesData
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.suspendCancellableCoroutine

// Block Store keeps a few bytes per app inside Google Play services, outside
// the app's own data, so they are still there after a reinstall. The record
// is not sent to the cloud: it stays on the phone. A phone without Play
// services throws here, which the repository reads as "nothing stored".
internal class BlockStoreProTrialRecordStore(
    context: Context,
) : ProTrialRecordStore {

    private val client = Blockstore.getClient(context.applicationContext)

    override val externalChanges: Flow<Unit> = emptyFlow()

    override suspend fun read(): ProTrialRecord? {
        val request = RetrieveBytesRequest.Builder().setKeys(listOf(KEY)).build()
        val bytes = client.retrieveBytes(request).await().blockstoreDataMap[KEY]?.bytes ?: return null
        return decode(bytes.decodeToString())
    }

    override suspend fun write(record: ProTrialRecord) {
        val text = "${record.startedAtMs}$SEPARATOR${record.isRevoked}"
        val data = StoreBytesData.Builder().setKey(KEY).setBytes(text.encodeToByteArray()).build()
        client.storeBytes(data).await()
    }

    private fun decode(text: String): ProTrialRecord? {
        val parts = text.split(SEPARATOR)
        val startedAtMs = parts.firstOrNull()?.toLongOrNull() ?: return null
        return ProTrialRecord(startedAtMs = startedAtMs, isRevoked = parts.getOrNull(1) == true.toString())
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }

    private companion object {
        const val KEY = "pro_trial"
        const val SEPARATOR = "|"
    }
}
