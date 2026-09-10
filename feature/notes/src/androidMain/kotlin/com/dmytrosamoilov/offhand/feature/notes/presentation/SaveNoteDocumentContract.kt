package com.dmytrosamoilov.offhand.feature.notes.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import java.io.File

// Lets the user pick where the exported note goes; the caller streams the
// share cache copy into the returned document.
internal class SaveNoteDocumentContract : ActivityResultContract<NoteShareUi, Uri?>() {

    override fun createIntent(context: Context, input: NoteShareUi): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.mimeType)
            .putExtra(Intent.EXTRA_TITLE, File(input.filePaths.first()).name)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = intent?.data
}

internal fun copyToDocument(context: Context, sourcePath: String, target: Uri): Boolean = runCatching {
    context.contentResolver.openOutputStream(target)?.use { output ->
        File(sourcePath).inputStream().use { input -> input.copyTo(output) }
    } != null
}.getOrDefault(false)
