package com.dmytrosamoilov.offhand.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.dmytrosamoilov.offhand.R
import com.dmytrosamoilov.offhand.core.designsystem.component.BottomBar
import com.dmytrosamoilov.offhand.core.designsystem.component.BottomBarDestination
import com.dmytrosamoilov.offhand.core.designsystem.component.BottomBarLabels
import com.dmytrosamoilov.offhand.core.designsystem.haptics.haptics
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.backup.presentation.BackupScreen
import com.dmytrosamoilov.offhand.feature.notes.presentation.NotesScreen
import com.dmytrosamoilov.offhand.feature.paywall.presentation.PaywallHost
import com.dmytrosamoilov.offhand.feature.recording.presentation.RecordingSheetHost
import com.dmytrosamoilov.offhand.feature.settings.presentation.AboutSupportScreen
import com.dmytrosamoilov.offhand.feature.settings.presentation.NoteStyleEditorScreen
import com.dmytrosamoilov.offhand.feature.settings.presentation.NoteStylesScreen
import com.dmytrosamoilov.offhand.feature.settings.presentation.SettingsScreen
import com.dmytrosamoilov.offhand.feature.settings.presentation.SharedAudioImportHost
import com.dmytrosamoilov.offhand.root.RecordingRequestUi

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OffhandApp(
    requestedNoteId: Long?,
    onRequestedNoteConsumed: () -> Unit,
    recordingRequest: RecordingRequestUi?,
    onRecordingRequestConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    var isRecordingSheetVisible by rememberSaveable { mutableStateOf(false) }
    var recordingFolderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var recordedNoteId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(recordingRequest) {
        if (recordingRequest != null) {
            recordingFolderId = recordingRequest.folderId
            isRecordingSheetVisible = true
            onRecordingRequestConsumed()
        }
    }

    LaunchedEffect(requestedNoteId) {
        if (requestedNoteId != null) {
            navController.navigateToTopLevel(NotesGraphRoute)
        }
    }

    val startRecording = { folderId: Long? ->
        recordingFolderId = folderId
        isRecordingSheetVisible = true
    }
    val navigateToTopLevel = { destination: TopLevelDestination -> navController.navigateToTopLevel(destination.route) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffhandTheme.colors.ground)
            .semantics { testTagsAsResourceId = true },
    ) {
        NavHost(
            navController = navController,
            startDestination = NotesGraphRoute,
            modifier = Modifier.weight(1f),
        ) {
            notesGraph(
                navController = navController,
                requestedNoteId = { requestedNoteId ?: recordedNoteId },
                onRequestedNoteConsumed = {
                    recordedNoteId = null
                    onRequestedNoteConsumed()
                },
                onNewRecording = startRecording,
                onNavigate = navigateToTopLevel,
            )
            settingsGraph(navController = navController)
        }
        if (backStackEntry?.destination?.hasRoute(SettingsRoute::class) == true) {
            AppBottomBar(selected = TopLevelDestination.SETTINGS, onNavigate = navigateToTopLevel, onRecord = { startRecording(null) })
        }
    }

    RecordingSheetHost(
        isVisible = isRecordingSheetVisible,
        folderId = recordingFolderId,
        onVisibilityChange = { isRecordingSheetVisible = it },
        onNoteSaved = { noteId -> recordedNoteId = noteId },
    )
    SharedAudioImportHost()
    PaywallHost()
}

private fun NavGraphBuilder.notesGraph(
    navController: NavController,
    requestedNoteId: () -> Long?,
    onRequestedNoteConsumed: () -> Unit,
    onNewRecording: (Long?) -> Unit,
    onNavigate: (TopLevelDestination) -> Unit,
) {
    navigation<NotesGraphRoute>(startDestination = NotesRoute) {
        composable<NotesRoute> {
            NotesScreen(
                requestedNoteId = requestedNoteId(),
                onRequestedNoteConsumed = onRequestedNoteConsumed,
                onCreateNoteStyle = { navController.navigate(NewNoteStyleFromNoteRoute) },
                onNewRecording = onNewRecording,
                bottomBar = { onRecord ->
                    AppBottomBar(selected = TopLevelDestination.NOTES, onNavigate = onNavigate, onRecord = onRecord)
                },
            )
        }
        composable<NewNoteStyleFromNoteRoute> {
            NoteStyleEditorScreen(styleId = 0L, onBack = { navController.navigateUp() })
        }
    }
}

private fun NavGraphBuilder.settingsGraph(navController: NavController) {
    navigation<SettingsGraphRoute>(startDestination = SettingsRoute) {
        composable<SettingsRoute> {
            SettingsScreen(
                onAboutSupportClick = { navController.navigate(AboutSupportRoute) },
                onBackupClick = { navController.navigate(BackupRoute) },
                onNoteStylesClick = { navController.navigate(NoteStylesRoute) },
            )
        }
        composable<NoteStylesRoute> {
            NoteStylesScreen(
                onBack = { navController.navigateUp() },
                onCreateStyle = { navController.navigate(NoteStyleEditorRoute(styleId = 0L)) },
                onEditStyle = { styleId -> navController.navigate(NoteStyleEditorRoute(styleId)) },
            )
        }
        composable<NoteStyleEditorRoute> { entry ->
            NoteStyleEditorScreen(
                styleId = entry.toRoute<NoteStyleEditorRoute>().styleId,
                onBack = { navController.navigateUp() },
            )
        }
        composable<AboutSupportRoute> {
            AboutSupportScreen(onBack = { navController.navigateUp() })
        }
        composable<BackupRoute> {
            BackupScreen(onBack = { navController.navigateUp() })
        }
    }
}

@Composable
private fun AppBottomBar(
    selected: TopLevelDestination,
    onNavigate: (TopLevelDestination) -> Unit,
    onRecord: () -> Unit,
) {
    val haptics = haptics()
    BottomBar(
        selected = if (selected == TopLevelDestination.NOTES) BottomBarDestination.Notes else BottomBarDestination.Settings,
        labels = BottomBarLabels(
            notes = stringResource(TopLevelDestination.NOTES.labelRes),
            settings = stringResource(TopLevelDestination.SETTINGS.labelRes),
            record = stringResource(R.string.destination_record),
            recordDescription = stringResource(R.string.destination_record_description),
        ),
        onNotes = { onNavigate(TopLevelDestination.NOTES) },
        onRecord = {
            haptics.confirm()
            onRecord()
        },
        onSettings = { onNavigate(TopLevelDestination.SETTINGS) },
        notesModifier = Modifier.testTag(TopLevelDestination.NOTES.testTag),
        settingsModifier = Modifier.testTag(TopLevelDestination.SETTINGS.testTag),
    )
}

private fun NavController.navigateToTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
