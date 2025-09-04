package io.github.evaogbe.diswantin.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
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
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
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
import io.github.evaogbe.diswantin.advice.AdviceRoute
import io.github.evaogbe.diswantin.advice.BodySensationAdviceScreen
import io.github.evaogbe.diswantin.advice.CheckTheFactsScreen
import io.github.evaogbe.diswantin.advice.DistressLevelAdviceScreen
import io.github.evaogbe.diswantin.advice.ExtremeDistressAdviceScreen
import io.github.evaogbe.diswantin.advice.HighDistressAdviceScreen
import io.github.evaogbe.diswantin.advice.HungryAdviceScreen
import io.github.evaogbe.diswantin.advice.InnerAdviceTopBar
import io.github.evaogbe.diswantin.advice.LowDistressAdviceScreen
import io.github.evaogbe.diswantin.advice.PainAdviceScreen
import io.github.evaogbe.diswantin.advice.StartAdviceTopBar
import io.github.evaogbe.diswantin.advice.TiredAdviceScreen
import io.github.evaogbe.diswantin.task.ui.CurrentTaskRoute
import io.github.evaogbe.diswantin.task.ui.CurrentTaskScreen
import io.github.evaogbe.diswantin.task.ui.CurrentTaskTopBar
import io.github.evaogbe.diswantin.task.ui.CurrentTaskTopBarAction
import io.github.evaogbe.diswantin.task.ui.CurrentTaskTopBarState
import io.github.evaogbe.diswantin.task.ui.ParentTask
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
import io.github.evaogbe.diswantin.ui.snackbar.SnackbarHandler
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.coroutines.launch

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

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val showSnackbar = remember<SnackbarHandler> {
        { state ->
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
    }

    LaunchedEffect(currentDestination) {
        snackbarHostState.currentSnackbarData?.dismiss()
    }

    Scaffold(
        topBar = {
            DiswantinTopBar(
                topBarState = topBarState,
                setTopBarState = { topBarState = it },
                query = query,
                navController = navController,
            )
        },
        bottomBar = {
            if (currentDestination?.hierarchy.orEmpty().any { dest ->
                    BottomBarDestination.entries.any { dest.hasRoute(it.route::class) }
                }) {
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
                            restoreState = it.restoreState
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

                currentDestination?.hierarchy.orEmpty().any { it.hasRoute<AdviceRoute>() } -> {
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
                    showSnackbar = showSnackbar,
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
            composable<TagListRoute> {
                LaunchedEffect(Unit) {
                    if (topBarState !is TopBarState.TagList) {
                        topBarState = TopBarState.TagList
                    }
                }

                TagListScreen(
                    onSelectTag = {
                        navController.navigate(route = TagDetailRoute(id = it))
                    },
                    showSnackbar = showSnackbar,
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
                    showSnackbar = showSnackbar,
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
                    showSnackbar = showSnackbar,
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
                    showSnackbar = showSnackbar,
                    onAddTask = {
                        navController.navigate(route = TaskFormRoute.Main.new(name = it))
                    },
                    onSelectSearchResult = {
                        navController.navigate(route = TaskDetailRoute(id = it.id))
                    },
                )
            }
            navigation<TaskFormRoute>(startDestination = TaskFormRoute.Main.Start) {
                composable<TaskFormRoute.Main> { backStackEntry ->
                    val previousQueryText by rememberSaveable {
                        mutableStateOf(query.text.toString())
                    }
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(TaskFormRoute)
                    }
                    val taskFormViewModel = hiltViewModel<TaskFormViewModel>(parentEntry)

                    TaskFormScreen(
                        onPopBackStack = {
                            query.setTextAndPlaceCursorAtEnd(previousQueryText)
                            navController.popBackStack()
                        },
                        setTopBarState = {
                            topBarState = TopBarState.TaskForm(uiState = it, action = null)
                        },
                        topBarAction = (topBarState as? TopBarState.TaskForm)?.action,
                        topBarActionHandled = {
                            (topBarState as? TopBarState.TaskForm)?.copy(action = null)?.let {
                                topBarState = it
                            }
                        },
                        showSnackbar = showSnackbar,
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
                        if (topBarState !is TopBarState.TaskRecurrenceForm) {
                            topBarState = TopBarState.TaskRecurrenceForm(action = null)
                        }
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
                        showSnackbar = showSnackbar,
                        onAddTask = null,
                        onSelectSearchResult = {
                            taskFormViewModel.updateParent(ParentTask(id = it.id, name = it.name))
                            navController.popBackStack()
                        },
                    )
                }
            }
            navigation<AdviceRoute>(startDestination = AdviceRoute.BodySensation) {
                composable<AdviceRoute.BodySensation> {
                    LaunchedEffect(Unit) {
                        if (topBarState !is TopBarState.AdviceStart) {
                            topBarState = TopBarState.AdviceStart
                        }
                    }

                    BodySensationAdviceScreen(
                        onHungryClick = { navController.navigate(route = AdviceRoute.Hungry) },
                        onTiredClick = { navController.navigate(route = AdviceRoute.Tired) },
                        onPainClick = { navController.navigate(route = AdviceRoute.Pain) },
                        onOtherClick = {
                            navController.navigate(route = AdviceRoute.DistressLevel)
                        },
                    )
                }
                composable<AdviceRoute.Hungry> {
                    LaunchedEffect(Unit) {
                        if (topBarState !is TopBarState.AdviceInner) {
                            topBarState = TopBarState.AdviceInner
                        }
                    }

                    HungryAdviceScreen(
                        onContinueClick = {
                            navController.navigate(route = AdviceRoute.DistressLevel)
                        },
                    )
                }
                composable<AdviceRoute.Tired> {
                    LaunchedEffect(Unit) {
                        if (topBarState !is TopBarState.AdviceInner) {
                            topBarState = TopBarState.AdviceInner
                        }
                    }

                    TiredAdviceScreen(
                        onContinueClick = {
                            navController.navigate(route = AdviceRoute.DistressLevel)
                        },
                    )
                }
                composable<AdviceRoute.Pain> {
                    LaunchedEffect(Unit) {
                        if (topBarState !is TopBarState.AdviceInner) {
                            topBarState = TopBarState.AdviceInner
                        }
                    }

                    PainAdviceScreen(
                        onContinueClick = {
                            navController.navigate(route = AdviceRoute.DistressLevel)
                        },
                    )
                }
                composable<AdviceRoute.DistressLevel> {
                    LaunchedEffect(Unit) {
                        if (topBarState !is TopBarState.AdviceInner) {
                            topBarState = TopBarState.AdviceInner
                        }
                    }

                    DistressLevelAdviceScreen(
                        onLowClick = {
                            navController.navigate(route = AdviceRoute.LowDistress)
                        },
                        onHighClick = {
                            navController.navigate(route = AdviceRoute.HighDistress)
                        },
                        onExtremeClick = {
                            navController.navigate(route = AdviceRoute.ExtremeDistress)
                        },
                    )
                }
                composable<AdviceRoute.LowDistress> {
                    LaunchedEffect(Unit) {
                        if (topBarState !is TopBarState.AdviceInner) {
                            topBarState = TopBarState.AdviceInner
                        }
                    }

                    LowDistressAdviceScreen(
                        onCheckTheFactsClick = {
                            navController.navigate(route = AdviceRoute.CheckTheFacts)
                        },
                    )
                }
                composable<AdviceRoute.CheckTheFacts> {
                    LaunchedEffect(Unit) {
                        if (topBarState !is TopBarState.AdviceInner) {
                            topBarState = TopBarState.AdviceInner
                        }
                    }

                    CheckTheFactsScreen()
                }
                composable<AdviceRoute.HighDistress> {
                    LaunchedEffect(Unit) {
                        if (topBarState !is TopBarState.AdviceInner) {
                            topBarState = TopBarState.AdviceInner
                        }
                    }

                    HighDistressAdviceScreen()
                }
                composable<AdviceRoute.ExtremeDistress> {
                    LaunchedEffect(Unit) {
                        if (topBarState !is TopBarState.AdviceInner) {
                            topBarState = TopBarState.AdviceInner
                        }
                    }

                    ExtremeDistressAdviceScreen()
                }
            }
        }
    }
}

@Composable
fun DiswantinTopBar(
    topBarState: TopBarState,
    setTopBarState: (TopBarState) -> Unit,
    query: TextFieldState,
    navController: NavController,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    when (topBarState) {
        is TopBarState.CurrentTask -> {
            CurrentTaskTopBar(
                uiState = topBarState.uiState,
                onRefresh = {
                    setTopBarState(topBarState.copy(action = CurrentTaskTopBarAction.Refresh))
                },
                onSkip = {
                    setTopBarState(topBarState.copy(action = CurrentTaskTopBarAction.Skip))
                },
            )
        }

        is TopBarState.AdviceStart -> {
            StartAdviceTopBar()
        }

        is TopBarState.AdviceInner -> {
            InnerAdviceTopBar(
                onBackClick = navController::popBackStack,
                onRestart = { navController.navigate(route = AdviceRoute.BodySensation) },
            )
        }

        is TopBarState.TagList -> {
            TagListTopBar()
        }

        is TopBarState.TaskDetail -> {
            TaskDetailTopBar(
                uiState = topBarState.uiState,
                onBackClick = navController::popBackStack,
                onEditTask = {
                    navController.navigate(route = TaskFormRoute.Main.edit(id = it))
                },
                onDeleteTask = {
                    setTopBarState(topBarState.copy(action = TaskDetailTopBarAction.Delete))
                },
                onMarkTaskDone = {
                    setTopBarState(topBarState.copy(action = TaskDetailTopBarAction.MarkDone))
                },
                onUnmarkTaskDone = {
                    setTopBarState(topBarState.copy(action = TaskDetailTopBarAction.UnmarkDone))
                },
            )
        }

        is TopBarState.TaskForm -> {
            TaskFormTopBar(
                uiState = topBarState.uiState,
                onClose = {
                    setTopBarState(topBarState.copy(action = TaskFormTopBarAction.Close))
                },
                onSave = {
                    setTopBarState(topBarState.copy(action = TaskFormTopBarAction.Save))
                },
            )
        }

        is TopBarState.TaskRecurrenceForm -> {
            TaskRecurrenceFormTopBar(
                onClose = navController::popBackStack,
                onConfirm = {
                    setTopBarState(
                        topBarState.copy(
                            action = TaskRecurrenceFormTopBarAction.Confirm
                        )
                    )
                    navController.popBackStack()
                },
            )
        }

        is TopBarState.TagDetail -> {
            TagDetailTopBar(
                uiState = topBarState.uiState,
                onBackClick = navController::popBackStack,
                onEditTag = {
                    setTopBarState(topBarState.copy(action = TagDetailTopBarAction.Edit))
                },
                onDeleteTag = {
                    setTopBarState(topBarState.copy(action = TagDetailTopBarAction.Delete))
                },
            )
        }

        is TopBarState.TaskSearch -> {
            val isNested = currentDestination?.hasRoute<TaskFormRoute.TaskSearch>() != false

            TaskSearchTopBar(
                query = query,
                onBackClick = if (isNested) {
                    { navController.popBackStack() }
                } else null,
                onSearch = {
                    setTopBarState(topBarState.copy(action = TaskSearchTopBarAction.Search))
                },
            )
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
