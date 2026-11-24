package com.dmytrosamoilov.offhand.core.data.domain

import kotlinx.coroutines.flow.Flow

// Keeps the trial record where a reinstall does not erase it: Google's Block
// Store on Android, the iCloud key-value store on iOS. Either can be missing
// or empty, so every caller treats a failed read as "nothing stored".
interface ProTrialRecordStore {

    // Fires when the record changed outside this process, which is how a
    // value from before a reinstall arrives late on iOS.
    val externalChanges: Flow<Unit>

    suspend fun read(): ProTrialRecord?

    suspend fun write(record: ProTrialRecord)
}
