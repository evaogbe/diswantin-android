package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.coroutines.launch
import java.time.Instant

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onSearch: () -> Unit,
    onEditTask: (Long) -> Unit,
    onAddTask: () -> Unit,
    onAddList: () -> Unit,
    onAdviceClick: () -> Unit,
    onSelectTaskList: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    var userMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val (currentTaskId, setCurrentTaskId) = rememberSaveable { mutableStateOf<Long?>(null) }

    userMessage?.let { message ->
        LaunchedEffect(message, snackbarHostState) {
            snackbarHostState.showSnackbar(message)
            userMessage = null
        }
    }

    HomeScreen(
        onSearch = onSearch,
        onEditTask = currentTaskId?.let { { onEditTask(it) } },
        snackbarHostState = snackbarHostState,
        onFabClick = onAddTask,
        onTabClick = {
            scope.launch {
                pagerState.animateScrollToPage(it)
            }
        },
        pagerState = pagerState,
        modifier = modifier,
    ) { page ->
        when (page) {
            0 -> CurrentTaskScreen(
                setCurrentTaskId = setCurrentTaskId,
                setUserMessage = { userMessage = it },
                onAddTask = onAddTask,
                onAdviceClick = onAdviceClick,
            )

            1 -> {
                TaskListsScreen(onAddList = onAddList, onSelectTaskList = onSelectTaskList)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSearch: () -> Unit,
    onEditTask: (() -> Unit)?,
    snackbarHostState: SnackbarHostState,
    onFabClick: () -> Unit,
    onTabClick: (Int) -> Unit,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    pageContent: @Composable PagerScope.(Int) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_tasks_button)
                        )
                    }

                    if (pagerState.currentPage == 0 && onEditTask != null) {
                        IconButton(onClick = onEditTask) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_button),
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onFabClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_button)
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { onTabClick(0) },
                    text = { Text(stringResource(R.string.current_task_title)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.baseline_task_alt_24),
                            contentDescription = null,
                        )
                    },
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { onTabClick(1) },
                    text = { Text(stringResource(R.string.task_lists_title)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.baseline_list_alt_24),
                            contentDescription = null,
                        )
                    },
                )
            }
            HorizontalPager(state = pagerState, pageContent = pageContent)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@DevicePreviews
@Composable
private fun HomeScreenPreview() {
    DiswantinTheme {
        HomeScreen(
            onSearch = {},
            onEditTask = {},
            snackbarHostState = SnackbarHostState(),
            onFabClick = {},
            onTabClick = {},
            pagerState = rememberPagerState(pageCount = { 2 }),
        ) {
            CurrentTaskLayout(
                task = Task(
                    id = 1L,
                    createdAt = Instant.now(),
                    name = "Brush teeth"
                ),
                onAdviceClick = {},
                onMarkTaskDone = {},
            )
        }
    }
}
