package io.github.evaogbe.diswantin.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.navigation.toRoute
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.ui.AdviceRoute
import io.github.evaogbe.diswantin.task.ui.AdviceScreen
import io.github.evaogbe.diswantin.task.ui.AdviceTopBar
import io.github.evaogbe.diswantin.task.ui.CurrentTaskRoute
import io.github.evaogbe.diswantin.task.ui.CurrentTaskScreen
import io.github.evaogbe.diswantin.task.ui.CurrentTaskTopBar
import io.github.evaogbe.diswantin.task.ui.CurrentTaskTopBarAction
import io.github.evaogbe.diswantin.task.ui.CurrentTaskTopBarState
import io.github.evaogbe.diswantin.task.ui.TagDetailRoute
import io.github.evaogbe.diswantin.task.ui.TagDetailScreen
import io.github.evaogbe.diswantin.task.ui.TagDetailTopBar
import io.github.evaogbe.diswantin.task.ui.TagDetailTopBarAction
import io.github.evaogbe.diswantin.task.ui.TagDetailTopBarState
import io.github.evaogbe.diswantin.task.ui.TagListRoute
import io.github.evaogbe.diswantin.task.ui.TagListScreen
import io.github.evaogbe.diswantin.task.ui.TagListTopBar
import io.github.evaogbe.diswantin.task.ui.TaskDetailRoute
import io.github.evaogbe.diswantin.task.ui.TaskDetailScreen
import io.github.evaogbe.diswantin.task.ui.TaskDetailTopBar
import io.github.evaogbe.diswantin.task.ui.TaskDetailTopBarAction
import io.github.evaogbe.diswantin.task.ui.TaskFormRoute
import io.github.evaogbe.diswantin.task.ui.TaskFormScreen
import io.github.evaogbe.diswantin.task.ui.TaskFormTopBar
import io.github.evaogbe.diswantin.task.ui.TaskFormTopBarAction
import io.github.evaogbe.diswantin.task.ui.TaskFormViewModel
import io.github.evaogbe.diswantin.task.ui.TaskRecurrenceFormTopBar
import io.github.evaogbe.diswantin.task.ui.TaskRecurrenceFormTopBarAction
import io.github.evaogbe.diswantin.task.ui.TaskRecurrentFormScreen
import io.github.evaogbe.diswantin.task.ui.TaskSearchRoute
import io.github.evaogbe.diswantin.task.ui.TaskSearchScreen
import io.github.evaogbe.diswantin.task.ui.TaskSearchTopBar
import io.github.evaogbe.diswantin.task.ui.TaskSearchTopBarAction
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews

@Composable
fun DiswantinApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val query = rememberTextFieldState()
    var topBarState by rememberSaveable {
        mutableStateOf<TopBarState>(
            TopBarState.CurrentTask(
                uiState = CurrentTaskTopBarState(canSkip = false),
                action = null,
            ),
        )
    }

    var fabClicked by rememberSaveable { mutableStateOf(false) }

    val resources = LocalResources.current
    var userMessage by remember { mutableStateOf<UserMessage?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    userMessage?.let { message ->
        LaunchedEffect(message, snackbarHostState) {
            snackbarHostState.showSnackbar(
                when (message) {
                    is UserMessage.String -> resources.getString(message.resId)
                    is UserMessage.Plural -> {
                        resources.getQuantityString(message.resId, message.count, message.count)
                    }
                }
            )
            userMessage = null
        }
    }

    LaunchedEffect(currentDestination) {
        snackbarHostState.currentSnackbarData?.dismiss()
    }

    Scaffold(
        topBar = {
            when (val state = topBarState) {
                is TopBarState.CurrentTask -> {
                    CurrentTaskTopBar(
                        uiState = state.uiState,
                        onRefresh = {
                            topBarState = state.copy(action = CurrentTaskTopBarAction.Refresh)
                        },
                        onSkip = {
                            topBarState = state.copy(action = CurrentTaskTopBarAction.Skip)
                        },
                    )
                }

                is TopBarState.Advice -> {
                    AdviceTopBar()
                }

                is TopBarState.TagList -> {
                    TagListTopBar()
                }

                is TopBarState.TaskDetail -> {
                    TaskDetailTopBar(
                        uiState = state.uiState,
                        onBackClick = navController::popBackStack,
                        onEditTask = {
                            navController.navigate(route = TaskFormRoute.Main.edit(id = it))
                        },
                        onDeleteTask = {
                            topBarState = state.copy(action = TaskDetailTopBarAction.Delete)
                        },
                        onMarkTaskDone = {
                            topBarState = state.copy(action = TaskDetailTopBarAction.MarkDone)
                        },
                        onUnmarkTaskDone = {
                            topBarState = state.copy(action = TaskDetailTopBarAction.UnmarkDone)
                        },
                    )
                }

                is TopBarState.TaskForm -> {
                    TaskFormTopBar(
                        uiState = state.uiState,
                        onClose = {
                            topBarState = state.copy(action = TaskFormTopBarAction.Close)
                        },
                        onSave = {
                            topBarState = state.copy(action = TaskFormTopBarAction.Save)
                        },
                    )
                }

                is TopBarState.TaskRecurrenceForm -> {
                    TaskRecurrenceFormTopBar(
                        onClose = navController::popBackStack,
                        onConfirm = {
                            topBarState = state.copy(
                                action = TaskRecurrenceFormTopBarAction.Confirm,
                            )
                            navController.popBackStack()
                        },
                    )
                }

                is TopBarState.TagDetail -> {
                    TagDetailTopBar(
                        uiState = state.uiState,
                        onBackClick = navController::popBackStack,
                        onEditTag = {
                            topBarState = state.copy(action = TagDetailTopBarAction.Edit)
                        },
                        onDeleteTag = {
                            topBarState = state.copy(action = TagDetailTopBarAction.Delete)
                        },
                    )
                }

                is TopBarState.TaskSearch -> {
                    TaskSearchTopBar(
                        query = query,
                        onBackClick = if (currentDestination?.hasRoute<TaskFormRoute.TaskSearch>() != false) {
                            { navController.popBackStack() }
                        } else {
                            null
                        },
                        onSearch = {
                            topBarState = state.copy(action = TaskSearchTopBarAction.Search)
                        },
                    )
                }
            }
        },
        bottomBar = {
            if (currentDestination?.route in BottomBarDestination.routeNames) {
                DiswantinBottomBar(
                    isCurrentRoute = { dest ->
                        currentDestination?.hierarchy.orEmpty().any {
                            it.hasRoute(dest.route::class)
                        }
                    },
                    navigate = {
                        query.clearText()
                        navController.navigate(it.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.imePadding())
        },
        floatingActionButton = {
            when {
                currentDestination?.hasRoute<CurrentTaskRoute>() == true -> {
                    DiswantinFab(
                        onClick = {
                            navController.navigate(route = TaskFormRoute.Main.new(name = null))
                        },
                    )
                }

                currentDestination?.hasRoute<AdviceRoute>() == true -> {
                    DiswantinFab(
                        onClick = {
                            navController.navigate(route = TaskFormRoute.Main.new(name = null))
                        },
                    )
                }

                currentDestination?.hasRoute<TagListRoute>() == true -> {
                    DiswantinFab(onClick = { fabClicked = true })
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = CurrentTaskRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<CurrentTaskRoute> {
                CurrentTaskScreen(
                    setTopBarState = {
                        topBarState = TopBarState.CurrentTask(uiState = it, action = null)
                    },
                    topBarAction = (topBarState as? TopBarState.CurrentTask)?.action,
                    topBarActionHandled = {
                        (topBarState as? TopBarState.CurrentTask)?.copy(action = null)?.let {
                            topBarState = it
                        }
                    },
                    setUserMessage = { userMessage = it },
                    onNavigateToAdvice = {
                        navController.navigate(route = AdviceRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAddTask = {
                        navController.navigate(route = TaskFormRoute.Main.new(name = null))
                    },
                    onNavigateToTask = {
                        navController.navigate(route = TaskDetailRoute(id = it))
                    },
                )
            }
            composable<AdviceRoute> {
                LaunchedEffect(Unit) {
                    topBarState = TopBarState.Advice
                }

                AdviceScreen()
            }
            composable<TagListRoute> {
                LaunchedEffect(Unit) {
                    topBarState = TopBarState.TagList
                }

                TagListScreen(
                    onSelectTag = {
                        navController.navigate(route = TagDetailRoute(id = it))
                    },
                    setUserMessage = { userMessage = it },
                    fabClicked = fabClicked,
                    fabClickHandled = { fabClicked = false },
                )
            }
            composable<TaskDetailRoute> {
                TaskDetailScreen(
                    onPopBackStack = navController::popBackStack,
                    setTopBarState = {
                        topBarState = TopBarState.TaskDetail(uiState = it, action = null)
                    },
                    topBarAction = (topBarState as? TopBarState.TaskDetail)?.action,
                    topBarActionHandled = {
                        (topBarState as? TopBarState.TaskDetail)?.copy(action = null)?.let {
                            topBarState = it
                        }
                    },
                    setUserMessage = { userMessage = it },
                    onNavigateToTask = {
                        navController.navigate(route = TaskDetailRoute(id = it))
                    },
                    onNavigateToTag = {
                        navController.navigate(route = TagDetailRoute(id = it))
                    },
                )
            }
            composable<TagDetailRoute> { backStackEntry ->
                LaunchedEffect(backStackEntry) {
                    topBarState = TopBarState.TagDetail(
                        uiState = TagDetailTopBarState(
                            tagId = backStackEntry.toRoute<TagDetailRoute>().id,
                        ),
                        action = null,
                    )
                }

                TagDetailScreen(
                    onPopBackStack = navController::popBackStack,
                    topBarAction = (topBarState as? TopBarState.TagDetail)?.action,
                    topBarActionHandled = {
                        (topBarState as? TopBarState.TagDetail)?.copy(action = null)?.let {
                            topBarState = it
                        }
                    },
                    setUserMessage = { userMessage = it },
                    onSelectTask = {
                        navController.navigate(route = TaskDetailRoute(id = it))
                    },
                )
            }
            composable<TaskSearchRoute> {
                LaunchedEffect(Unit) {
                    if (topBarState !is TopBarState.TaskSearch) {
                        topBarState = TopBarState.TaskSearch(action = null)
                    }
                }

                TaskSearchScreen(
                    query = query.text.toString(),
                    topBarAction = (topBarState as? TopBarState.TaskSearch)?.action,
                    topBarActionHandled = {
                        (topBarState as? TopBarState.TaskSearch)?.copy(action = null)?.let {
                            topBarState = it
                        }
                    },
                    onAddTask = {
                        navController.navigate(route = TaskFormRoute.Main.new(name = it))
                    },
                    onSelectSearchResult = {
                        navController.navigate(route = TaskDetailRoute(id = it.id))
                    },
                )
            }
            navigation<TaskFormRoute>(startDestination = TaskFormRoute.Main()) {
                composable<TaskFormRoute.Main> { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(TaskFormRoute)
                    }
                    val taskFormViewModel = hiltViewModel<TaskFormViewModel>(parentEntry)

                    TaskFormScreen(
                        onPopBackStack = navController::popBackStack,
                        setTopBarState = {
                            topBarState = TopBarState.TaskForm(uiState = it, action = null)
                        },
                        topBarAction = (topBarState as? TopBarState.TaskForm)?.action,
                        topBarActionHandled = {
                            (topBarState as? TopBarState.TaskForm)?.copy(action = null)?.let {
                                topBarState = it
                            }
                        },
                        setUserMessage = { userMessage = it },
                        onEditRecurrence = {
                            taskFormViewModel.commitInputs()
                            navController.navigate(route = TaskFormRoute.Recurrence)
                        },
                        onEditParent = {
                            query.setTextAndPlaceCursorAtEnd(it)
                            taskFormViewModel.commitInputs()
                            navController.navigate(route = TaskFormRoute.TaskSearch)
                        },
                        taskFormViewModel = taskFormViewModel,
                    )
                }
                composable<TaskFormRoute.Recurrence> { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(TaskFormRoute)
                    }

                    LaunchedEffect(Unit) {
                        topBarState = TopBarState.TaskRecurrenceForm(action = null)
                    }

                    TaskRecurrentFormScreen(
                        topBarAction = (topBarState as? TopBarState.TaskRecurrenceForm)?.action,
                        topBarActionHandled = {
                            (topBarState as? TopBarState.TaskRecurrenceForm)?.copy(action = null)
                                ?.let { topBarState = it }
                        },
                        taskFormViewModel = hiltViewModel(parentEntry),
                    )
                }
                composable<TaskFormRoute.TaskSearch> { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(TaskFormRoute)
                    }
                    val taskFormViewModel = hiltViewModel<TaskFormViewModel>(parentEntry)

                    LaunchedEffect(Unit) {
                        if (topBarState !is TopBarState.TaskSearch) {
                            topBarState = TopBarState.TaskSearch(action = null)
                        }
                    }

                    TaskSearchScreen(
                        query = query.text.toString(),
                        topBarAction = (topBarState as? TopBarState.TaskSearch)?.action,
                        topBarActionHandled = {
                            (topBarState as? TopBarState.TaskSearch)?.copy(action = null)?.let {
                                topBarState = it
                            }
                        },
                        onAddTask = null,
                        onSelectSearchResult = {
                            taskFormViewModel.updateParent(it)
                            navController.popBackStack()
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun DiswantinBottomBar(
    isCurrentRoute: (BottomBarDestination) -> Boolean,
    navigate: (BottomBarDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        BottomBarDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = isCurrentRoute(destination),
                onClick = { navigate(destination) },
                icon = {
                    Icon(
                        painter = painterResource(destination.iconId),
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        stringResource(destination.titleId),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

@Composable
fun DiswantinFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(onClick = onClick, modifier = modifier) {
        Icon(
            painterResource(R.drawable.baseline_add_24),
            contentDescription = stringResource(R.string.add_button),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@DevicePreviews
@Composable
private fun DiswantinScaffoldPreview() {
    DiswantinTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(
                                painterResource(R.drawable.outline_more_vert_24),
                                contentDescription = stringResource(R.string.more_actions_button),
                            )
                        }
                    },
                )
            },
            bottomBar = {
                DiswantinBottomBar(
                    isCurrentRoute = { it == BottomBarDestination.CurrentTask },
                    navigate = {},
                )
            },
            floatingActionButton = { DiswantinFab(onClick = {}) },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(color = colorScheme.surfaceVariant),
            )
        }
    }
}
