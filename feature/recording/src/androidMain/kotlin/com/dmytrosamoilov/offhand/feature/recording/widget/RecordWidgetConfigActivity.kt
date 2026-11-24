package com.dmytrosamoilov.offhand.feature.recording.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import com.dmytrosamoilov.offhand.core.designsystem.component.ChoiceRow
import com.dmytrosamoilov.offhand.core.designsystem.component.PrimaryButton
import com.dmytrosamoilov.offhand.core.designsystem.component.ScreenTitle
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandSpacing
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffhandTheme.colors.ground)
            .safeDrawingPadding()
            .padding(horizontal = OffhandSpacing.screen, vertical = OffhandSpacing.screen),
    ) {
        ScreenTitle(title = stringResource(R.string.record_widget_config_title))
        Column(modifier = Modifier.weight(1f).padding(top = 16.dp).verticalScroll(rememberScrollState())) {
            ChoiceRow(
                title = stringResource(R.string.record_widget_no_folder),
                isSelected = selectedId == null,
                onClick = { selectedId = null },
            )
            folders.forEachIndexed { index, folder ->
                ChoiceRow(
                    title = folder.name,
                    isSelected = selectedId == folder.id,
                    onClick = { selectedId = folder.id },
                    showDivider = index < folders.lastIndex,
                )
            }
        }
        PrimaryButton(
            text = stringResource(R.string.record_widget_config_save),
            onClick = { onSave(folders.firstOrNull { it.id == selectedId }) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
