package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.ui.loadstate.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.loadstate.PendingLayout
import io.github.evaogbe.diswantin.ui.loadstate.pagedListFooter
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCategoryDetailTopBar(
    uiState: TaskCategoryDetailTopBarState,
    onBackClick: () -> Unit,
    onEditCategory: (Long) -> Unit,
    onDeleteCategory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {},
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back_button),
                )
            }
        },
        actions = {
            if (uiState.categoryId != null) {
                IconButton(onClick = { onEditCategory(uiState.categoryId) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_button),
                    )
                }

                IconButton(onClick = { menuExpanded = !menuExpanded }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.more_actions_button),
                    )
                }

                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_button)) },
                        onClick = {
                            onDeleteCategory()
                            menuExpanded = false
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        },
                    )
                }
            }
        },
    )
}

@Composable
fun TaskCategoryDetailScreen(
    onPopBackStack: () -> Unit,
    topBarAction: TaskCategoryDetailTopBarAction?,
    topBarActionHandled: () -> Unit,
    setUserMessage: (UserMessage) -> Unit,
    onSelectTask: (Long) -> Unit,
    taskCategoryDetailViewModel: TaskCategoryDetailViewModel = hiltViewModel(),
) {
    val taskPagingItems = taskCategoryDetailViewModel.taskItemPagingData.collectAsLazyPagingItems()
    val uiState by taskCategoryDetailViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(topBarAction) {
        when (topBarAction) {
            null -> {}
            TaskCategoryDetailTopBarAction.Delete -> {
                taskCategoryDetailViewModel.deleteCategory()
                topBarActionHandled()
            }
        }
    }

    when (val state = uiState) {
        is TaskCategoryDetailUiState.Pending -> {
            PendingLayout()
        }

        is TaskCategoryDetailUiState.Failure -> {
            LoadFailureLayout(message = stringResource(R.string.task_category_detail_fetch_error))
        }

        is TaskCategoryDetailUiState.Deleted -> {
            LaunchedEffect(Unit) {
                onPopBackStack()
            }

            PendingLayout()
        }

        is TaskCategoryDetailUiState.Success -> {
            when (taskPagingItems.loadState.refresh) {
                is LoadState.Loading -> PendingLayout()
                is LoadState.Error -> {
                    LoadFailureLayout(
                        message = stringResource(R.string.task_category_detail_fetch_error),
                    )
                }

                is LoadState.NotLoading -> {
                    LaunchedEffect(state.userMessage) {
                        if (state.userMessage != null) {
                            setUserMessage(state.userMessage)
                            taskCategoryDetailViewModel.userMessageShown()
                        }
                    }

                    TaskCategoryDetailLayout(
                        uiState = state,
                        taskItems = taskPagingItems,
                        onSelectTask = onSelectTask,
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCategoryDetailLayout(
    uiState: TaskCategoryDetailUiState.Success,
    taskItems: LazyPagingItems<TaskItemUiState>,
    onSelectTask: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    TaskCategoryDetailLayout(
        uiState = uiState,
        taskItems = if (taskItems.itemCount > 0) {
            {
                items(taskItems.itemCount, key = taskItems.itemKey(TaskItemUiState::id)) { index ->
                    val task = taskItems[index]!!
                    TaskItem(task = task, onSelectTask = onSelectTask)
                    HorizontalDivider()
                }

                pagedListFooter(
                    pagingItems = taskItems,
                    errorMessage = {
                        Text(stringResource(R.string.task_category_detail_fetch_tasks_error))
                    },
                )
            }
        } else null,
        modifier = modifier,
    )
}

@Composable
private fun TaskCategoryDetailLayout(
    uiState: TaskCategoryDetailUiState.Success,
    taskItems: (LazyListScope.() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .fillMaxWidth()
                .padding(vertical = SpaceMd),
        ) {
            item {
                SelectionContainer(modifier = Modifier.padding(horizontal = SpaceMd)) {
                    Text(text = uiState.category.name, style = typography.displaySmall)
                }
            }

            if (taskItems != null) {
                item {
                    Spacer(Modifier.size(SpaceMd))
                    Text(
                        stringResource(R.string.tasks_label),
                        modifier = Modifier.padding(horizontal = SpaceMd),
                        color = colorScheme.onSurfaceVariant,
                        style = typography.labelSmall,
                    )
                }

                taskItems()
            }
        }
    }
}

@DevicePreviews
@Composable
private fun TaskCategoryDetailScreenPreview() {
    val taskItems = listOf(
        TaskItemUiState(id = 1L, name = "Brush teeth", isDone = true),
        TaskItemUiState(id = 2L, name = "Shower", isDone = false),
        TaskItemUiState(id = 3L, name = "Eat breakfast", isDone = false),
    )

    DiswantinTheme {
        Scaffold(topBar = {
            TaskCategoryDetailTopBar(
                uiState = TaskCategoryDetailTopBarState(categoryId = 1L),
                onBackClick = {},
                onEditCategory = {},
                onDeleteCategory = {},
            )
        }) { innerPadding ->
            TaskCategoryDetailLayout(
                uiState = TaskCategoryDetailUiState.Success(
                    category = TaskCategory(name = "Morning Routine"),
                    userMessage = null,
                ),
                taskItems = {
                    items(taskItems, key = TaskItemUiState::id) { task ->
                        TaskItem(task = task, onSelectTask = {})
                        HorizontalDivider()
                    }
                },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
