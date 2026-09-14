package com.dmytrosamoilov.offhand.core.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class SensitiveClipboard(private val context: Context) {

    fun copy(label: String, text: String, copiedMessage: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(sensitiveClip(label, text))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun sensitiveClip(label: String, text: String): ClipData =
        ClipData.newPlainText(label, text).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
        }
}

@Composable
fun rememberSensitiveClipboard(): SensitiveClipboard {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) { SensitiveClipboard(appContext) }
}
