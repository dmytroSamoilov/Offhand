package com.dmytrosamoilov.offhand.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dmytrosamoilov.offhand.feature.notes.presentation.NotesScreen
import com.dmytrosamoilov.offhand.feature.recording.presentation.RecordingSheetHost
import com.dmytrosamoilov.offhand.feature.settings.presentation.SettingsScreen

@Composable
fun OffhandApp(
    requestedNoteId: Long?,
    onRequestedNoteConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    var isRecordingSheetVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(requestedNoteId) {
        if (requestedNoteId != null) {
            navController.navigateToTopLevel(NotesRoute)
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                val selected = currentDestination?.hierarchy
                    ?.any { it.hasRoute(destination.route::class) } == true
                item(
                    selected = selected,
                    onClick = { navController.navigateToTopLevel(destination.route) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = stringResource(destination.labelRes),
                        )
                    },
                    label = { Text(text = stringResource(destination.labelRes)) },
                )
            }
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = NotesRoute,
        ) {
            composable<NotesRoute> {
                NotesScreen(
                    requestedNoteId = requestedNoteId,
                    onRequestedNoteConsumed = onRequestedNoteConsumed,
                    onNewRecording = { isRecordingSheetVisible = true },
                )
            }
            composable<SettingsRoute> { SettingsScreen() }
        }
    }

    RecordingSheetHost(
        isVisible = isRecordingSheetVisible,
        onVisibilityChange = { isRecordingSheetVisible = it },
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
