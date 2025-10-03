package io.github.evaogbe.diswantin.app.ui

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.evaogbe.diswantin.data.ClockMonitor
import io.github.evaogbe.diswantin.task.ui.TaskFormRoute
import io.github.evaogbe.diswantin.task.ui.TaskSearchRoute
import io.github.evaogbe.diswantin.ui.snackbar.SnackbarState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import kotlin.time.Duration.Companion.seconds

@Stable
class DiswantinAppState(
    val navController: NavHostController,
    val snackbarHostState: SnackbarHostState,
    val query: TextFieldState,
    private val coroutineScope: CoroutineScope,
    clockMonitor: ClockMonitor,
) {
    val clock = clockMonitor.clock.stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = Clock.systemDefaultZone(),
    )

    val currentNavDestination: NavDestination?
        @Composable
        get() {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            return navBackStackEntry?.destination
        }

    val currentBottomBarDestination
        @Composable
        get() = BottomBarDestination.entries.firstOrNull { it.matches(currentNavDestination) }

    fun navigateToTopLevel(destination: BottomBarDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToTaskSearchFromTopLevel() {
        query.clearText()
        navController.navigate(route = TaskSearchRoute)
    }

    fun navigateToNewTaskFormFromFab() {
        navController.navigate(route = TaskFormRoute.Main.new(name = null))
    }

    fun showSnackbar(state: SnackbarState) {
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = state.message,
                actionLabel = state.actionLabel,
                withDismissAction = state.actionLabel != null,
            )
            if (result == SnackbarResult.ActionPerformed) {
                state.onAction()
            }
        }
    }

    fun dismissSnackbar() {
        snackbarHostState.currentSnackbarData?.dismiss()
    }
}

@Composable
fun rememberDiswantinAppState(
    clockMonitor: ClockMonitor,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberNavController(),
    snackbarHostState: SnackbarHostState = SnackbarHostState(),
    query: TextFieldState = TextFieldState(),
) = remember(clockMonitor, coroutineScope, navController, snackbarHostState, query) {
    DiswantinAppState(
        navController = navController,
        snackbarHostState = snackbarHostState,
        query = query,
        coroutineScope = coroutineScope,
        clockMonitor = clockMonitor,
    )
}
