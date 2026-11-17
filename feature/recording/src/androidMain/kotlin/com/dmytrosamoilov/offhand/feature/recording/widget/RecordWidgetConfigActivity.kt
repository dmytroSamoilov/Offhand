package com.dmytrosamoilov.offhand.feature.recording.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.recording.R
import kotlinx.coroutines.flow.map
import org.koin.android.ext.android.inject

// Opened by the launcher when the widget is placed or reconfigured: picks the
// folder its recordings are saved into. The widget id comes from the launcher
// and is the only thing this screen writes for.
class RecordWidgetConfigActivity : ComponentActivity() {

    private val foldersRepository: FoldersRepository by inject()
    private val store: RecordWidgetStore by inject()
    private val updater: RecordWidgetUpdater by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, resultFor(widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val folders = foldersRepository.observeFolders().map { list -> list.map { WidgetFolder(it.id, it.name) } }
        setContent {
            OffhandTheme {
                val choices by folders.collectAsStateWithLifecycle(initialValue = emptyList())
                FolderChoice(
                    folders = choices,
                    initial = store.folderOf(widgetId)?.id,
                    onSave = { folder -> save(widgetId, folder) },
                )
            }
        }
    }

    private fun save(widgetId: Int, folder: WidgetFolder?) {
        store.setFolder(widgetId, folder)
        updater.refresh()
        setResult(RESULT_OK, resultFor(widgetId))
        finish()
    }

    private fun resultFor(widgetId: Int): Intent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
}

@Composable
private fun FolderChoice(folders: List<WidgetFolder>, initial: Long?, onSave: (WidgetFolder?) -> Unit) {
    var selectedId by rememberSaveable { mutableStateOf(initial) }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.safeDrawingPadding().padding(24.dp)) {
            Text(text = stringResource(R.string.record_widget_config_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                FolderRow(label = stringResource(R.string.record_widget_no_folder), isSelected = selectedId == null) {
                    selectedId = null
                }
                folders.forEach { folder ->
                    FolderRow(label = folder.name, isSelected = selectedId == folder.id) { selectedId = folder.id }
                }
            }
            Button(
                onClick = { onSave(folders.firstOrNull { it.id == selectedId }) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.record_widget_config_save))
            }
        }
    }
}

@Composable
private fun FolderRow(label: String, isSelected: Boolean, onSelected: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onSelected)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
