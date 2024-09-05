package io.github.evaogbe.diswantin.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.ui.AdviceScreen
import io.github.evaogbe.diswantin.task.ui.AdviceTopBar
import io.github.evaogbe.diswantin.task.ui.CurrentTaskScreen
import io.github.evaogbe.diswantin.task.ui.CurrentTaskTopBar
import io.github.evaogbe.diswantin.task.ui.CurrentTaskTopBarState
import io.github.evaogbe.diswantin.task.ui.TaskCategoryDetailScreen
import io.github.evaogbe.diswantin.task.ui.TaskCategoryDetailTopBar
import io.github.evaogbe.diswantin.task.ui.TaskCategoryFormScreen
import io.github.evaogbe.diswantin.task.ui.TaskCategoryFormTopBar
import io.github.evaogbe.diswantin.task.ui.TaskCategoryListScreen
import io.github.evaogbe.diswantin.task.ui.TaskCategoryListTopBar
import io.github.evaogbe.diswantin.task.ui.TaskDetailScreen
import io.github.evaogbe.diswantin.task.ui.TaskDetailTopBar
import io.github.evaogbe.diswantin.task.ui.TaskFormScreen
import io.github.evaogbe.diswantin.task.ui.TaskFormTopBar
import io.github.evaogbe.diswantin.task.ui.TaskSearchScreen
import io.github.evaogbe.diswantin.task.ui.TaskSearchTopBar
import io.github.evaogbe.diswantin.ui.navigation.NavArguments
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews

@Composable
fun DiswantinApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var topBarState by rememberSaveable {
        mutableStateOf<TopBarState>(
            TopBarState.CurrentTask(uiState = CurrentTaskTopBarState(taskId = null))
        )
    }

    var userMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    userMessage?.let { message ->
        LaunchedEffect(message, snackbarHostState) {
            snackbarHostState.showSnackbar(message)
            userMessage = null
        }
    }

    LaunchedEffect(currentRoute) {
        snackbarHostState.currentSnackbarData?.dismiss()
    }

    Scaffold(
        topBar = {
            when (val state = topBarState) {
                is TopBarState.Advice -> {
                    AdviceTopBar(onSearch = {
                        navController.navigate(route = TopLevelDestination.TaskSearch.route)
                    })
                }

                is TopBarState.CurrentTask -> {
                    CurrentTaskTopBar(
                        uiState = state.uiState,
                        onSearch = {
                            navController.navigate(route = TopLevelDestination.TaskSearch.route)
                        },
                        onEditTask = {
                            navController.navigate(
                                route = TopLevelDestination.EditTaskForm(it).route
                            )
                        },
                    )
                }

                is TopBarState.TaskDetail -> {
                    TaskDetailTopBar(
                        uiState = state.uiState,
                        onBackClick = navController::popBackStack,
                        onEditTask = {
                            navController.navigate(
                                route = TopLevelDestination.EditTaskForm(it).route
                            )
                        },
                    )
                }

                is TopBarState.TaskForm -> {
                    TaskFormTopBar(uiState = state.uiState, onClose = navController::popBackStack)
                }

                is TopBarState.TaskCategoryDetail -> {
                    TaskCategoryDetailTopBar(
                        uiState = state.uiState,
                        onBackClick = navController::popBackStack,
                        onEditCategory = {
                            navController.navigate(
                                route = TopLevelDestination.EditTaskCategoryForm(it).route
                            )
                        },
                    )
                }

                is TopBarState.TaskCategoryForm -> {
                    TaskCategoryFormTopBar(
                        uiState = state.uiState,
                        onClose = navController::popBackStack,
                    )
                }

                is TopBarState.TaskCategoryList -> {
                    TaskCategoryListTopBar(onSearch = {
                        navController.navigate(route = TopLevelDestination.TaskSearch.route)
                    })
                }

                is TopBarState.TaskSearch -> {
                    TaskSearchTopBar(
                        uiState = state.uiState,
                        onBackClick = navController::popBackStack,
                    )
                }
            }
        },
        bottomBar = {
            if (currentRoute in BottomBarDestination.entries.map(BottomBarDestination::route)) {
                DiswantinBottomBar(
                    isCurrentRoute = { currentRoute == it.route },
                    navigate = {
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
            if (currentRoute in BottomBarDestination.entries.map(BottomBarDestination::route)) {
                DiswantinFab(onClick = {
                    navController.navigate(
                        route = TopLevelDestination.NewTaskForm(name = null).route
                    )
                })
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomBarDestination.CurrentTask.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(BottomBarDestination.Advice.route) {
                LaunchedEffect(Unit) {
                    topBarState = TopBarState.Advice
                }

                AdviceScreen()
            }
            composable(BottomBarDestination.CurrentTask.route) {
                CurrentTaskScreen(
                    setTopBarState = { topBarState = TopBarState.CurrentTask(uiState = it) },
                    setUserMessage = { userMessage = it },
                    onAddTask = {
                        navController.navigate(
                            route = TopLevelDestination.NewTaskForm(name = null).route
                        )
                    },
                )
            }
            composable(
                TopLevelDestination.TaskDetail.route,
                arguments = listOf(navArgument(NavArguments.ID_KEY) {
                    type = NavType.LongType
                }),
            ) {
                TaskDetailScreen(
                    onPopBackStack = navController::popBackStack,
                    setTopBarState = { topBarState = TopBarState.TaskDetail(uiState = it) },
                    setUserMessage = { userMessage = it },
                    onSelectCategory = {
                        navController.navigate(
                            route = TopLevelDestination.TaskCategoryDetail(it).route
                        )
                    },
                )
            }
            composable(
                TopLevelDestination.NewTaskForm.route,
                arguments = listOf(navArgument(NavArguments.NAME_KEY) {
                    type = NavType.StringType
                    nullable = true
                }),
            ) { backStackEntry ->
                TaskFormScreen(
                    onPopBackStack = navController::popBackStack,
                    setTopBarState = { topBarState = TopBarState.TaskForm(uiState = it) },
                    onSelectCategoryType = {
                        navController.navigate(
                            route = TopLevelDestination.NewTaskCategoryForm(name = it).route
                        ) {
                            popUpTo(backStackEntry.destination.id) {
                                inclusive = true
                            }
                        }
                    },
                )
            }
            composable(
                TopLevelDestination.EditTaskForm.route,
                arguments = listOf(navArgument(NavArguments.ID_KEY) {
                    type = NavType.LongType
                }),
            ) {
                TaskFormScreen(
                    onPopBackStack = navController::popBackStack,
                    setTopBarState = { topBarState = TopBarState.TaskForm(uiState = it) },
                    onSelectCategoryType = {},
                )
            }
            composable(
                TopLevelDestination.TaskCategoryDetail.route,
                arguments = listOf(navArgument(NavArguments.ID_KEY) {
                    type = NavType.LongType
                }),
            ) {
                TaskCategoryDetailScreen(
                    onPopBackStack = navController::popBackStack,
                    setTopBarState = { topBarState = TopBarState.TaskCategoryDetail(uiState = it) },
                    setUserMessage = { userMessage = it },
                    onSelectTask = {
                        navController.navigate(route = TopLevelDestination.TaskDetail(it).route)
                    },
                )
            }
            composable(
                TopLevelDestination.NewTaskCategoryForm.route,
                arguments = listOf(navArgument(NavArguments.NAME_KEY) {
                    type = NavType.StringType
                    nullable = true
                }),
            ) { backStackEntry ->
                TaskCategoryFormScreen(
                    onPopBackStack = navController::popBackStack,
                    setTopBarState = { topBarState = TopBarState.TaskCategoryForm(uiState = it) },
                    setUserMessage = { userMessage = it },
                    onSelectTaskType = {
                        navController.navigate(
                            route = TopLevelDestination.NewTaskForm(name = it).route
                        ) {
                            popUpTo(backStackEntry.destination.id) {
                                inclusive = true
                            }
                        }
                    },
                )
            }
            composable(
                TopLevelDestination.EditTaskCategoryForm.route,
                arguments = listOf(navArgument(NavArguments.ID_KEY) {
                    type = NavType.LongType
                }),
            ) {
                TaskCategoryFormScreen(
                    onPopBackStack = navController::popBackStack,
                    setTopBarState = { topBarState = TopBarState.TaskCategoryForm(uiState = it) },
                    setUserMessage = { userMessage = it },
                    onSelectTaskType = {},
                )
            }
            composable(BottomBarDestination.TaskCategoryList.route) {
                LaunchedEffect(Unit) {
                    topBarState = TopBarState.TaskCategoryList
                }

                TaskCategoryListScreen(
                    onAddCategory = {
                        navController.navigate(
                            route = TopLevelDestination.NewTaskCategoryForm(name = null).route
                        )
                    },
                    onSelectCategory = {
                        navController.navigate(
                            route = TopLevelDestination.TaskCategoryDetail(it).route
                        )
                    },
                )
            }
            composable(TopLevelDestination.TaskSearch.route) {
                TaskSearchScreen(
                    setTopBarState = { topBarState = TopBarState.TaskSearch(uiState = it) },
                    onAddTask = {
                        navController.navigate(
                            route = TopLevelDestination.NewTaskForm(name = it).route
                        )
                    },
                    onSelectSearchResult = {
                        navController.navigate(route = TopLevelDestination.TaskDetail(it).route)
                    },
                )
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
                label = { Text(stringResource(destination.titleId), textAlign = TextAlign.Center) },
            )
        }
    }
}

@Composable
fun DiswantinFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Default.Add,
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
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.search_tasks_button),
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
            floatingActionButton = { DiswantinFab(onClick = {}) }
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
