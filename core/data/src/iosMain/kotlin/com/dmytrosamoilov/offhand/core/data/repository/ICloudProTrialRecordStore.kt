package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRecord
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRecordStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUbiquitousKeyValueStore
import platform.Foundation.NSUbiquitousKeyValueStoreDidChangeExternallyNotification

// The iCloud key-value store belongs to the user's Apple account, so the
// record outlives a reinstall and follows them to another iPhone. A write
// made before the first download from iCloud is discarded in favour of what
// iCloud holds, and that arrives as an external change.
internal class ICloudProTrialRecordStore : ProTrialRecordStore {

    private val store = NSUbiquitousKeyValueStore.defaultStore

    override val externalChanges: Flow<Unit> = callbackFlow {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = NSUbiquitousKeyValueStoreDidChangeExternallyNotification,
            `object` = store,
            queue = null,
        ) { trySend(Unit) }
        store.synchronize()
        awaitClose { NSNotificationCenter.defaultCenter.removeObserver(observer) }
    }

    override suspend fun read(): ProTrialRecord? {
        store.synchronize()
        if (store.objectForKey(KEY_STARTED_AT_MS) == null) return null
        return ProTrialRecord(
            startedAtMs = store.longLongForKey(KEY_STARTED_AT_MS),
            isRevoked = store.boolForKey(KEY_REVOKED),
        )
    }

    override suspend fun write(record: ProTrialRecord) {
        store.setLongLong(record.startedAtMs, forKey = KEY_STARTED_AT_MS)
        store.setBool(record.isRevoked, forKey = KEY_REVOKED)
        store.synchronize()
    }

    private companion object {
        const val KEY_STARTED_AT_MS = "pro_trial_started_at_ms"
        const val KEY_REVOKED = "pro_trial_revoked"
    }
}
