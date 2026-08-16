package com.dmytrosamoilov.offhand.feature.notes.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

internal object NoteShareIntentFactory {

    private const val AUTHORITY_SUFFIX = ".fileprovider"

    fun createChooser(context: Context, share: NoteShareUi): Intent {
        val uris = share.filePaths.map { path -> toContentUri(context, path) }
        return Intent.createChooser(buildSendIntent(uris, share.mimeType), null)
    }

    private fun buildSendIntent(uris: List<Uri>, mimeType: String): Intent {
        val sendIntent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE)
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
        sendIntent.type = mimeType
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return sendIntent
    }

    private fun toContentUri(context: Context, path: String): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", File(path))
}
