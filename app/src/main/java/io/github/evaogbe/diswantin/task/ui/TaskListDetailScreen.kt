package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.TaskList
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListDetailTopBar(
    uiState: TaskListDetailTopBarState,
    onBackClick: () -> Unit,
    onEditTaskList: (Long) -> Unit,
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
            if (uiState.taskListId != null) {
                IconButton(onClick = { onEditTaskList(uiState.taskListId) }) {
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
                        onClick = uiState.onDeleteTaskList,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        },
                    )
                }
            }
        }
    )
}

@Composable
fun TaskListDetailScreen(
    onPopBackStack: () -> Unit,
    setTopBarState: (TaskListDetailTopBarState) -> Unit,
    setUserMessage: (String) -> Unit,
    onSelectTask: (Long) -> Unit,
    taskListDetailViewModel: TaskListDetailViewModel = hiltViewModel(),
) {
    val uiState by taskListDetailViewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalContext.current.resources

    if (uiState is TaskListDetailUiState.Deleted) {
        LaunchedEffect(onPopBackStack) {
            onPopBackStack()
        }
    }

    LaunchedEffect(setTopBarState, uiState, taskListDetailViewModel) {
        setTopBarState(
            TaskListDetailTopBarState(
                taskListId = (uiState as? TaskListDetailUiState.Success)?.taskList?.id,
                onDeleteTaskList = taskListDetailViewModel::deleteTaskList,
            )
        )
    }

    (uiState as? TaskListDetailUiState.Success)?.userMessage?.let { message ->
        LaunchedEffect(message, setUserMessage) {
            setUserMessage(resources.getString(message))
            taskListDetailViewModel.userMessageShown()
        }
    }

    when (val state = uiState) {
        is TaskListDetailUiState.Pending,
        is TaskListDetailUiState.Deleted -> {
            PendingLayout()
        }

        is TaskListDetailUiState.Failure -> {
            LoadFailureLayout(message = stringResource(R.string.task_list_detail_fetch_error))
        }

        is TaskListDetailUiState.Success -> {
            TaskListDetailLayout(uiState = state, onSelectTask = onSelectTask)
        }
    }
}

@Composable
fun TaskListDetailLayout(
    uiState: TaskListDetailUiState.Success,
    onSelectTask: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .fillMaxWidth()
                .padding(vertical = SpaceMd),
        ) {
            item {
                SelectionContainer(modifier = Modifier.padding(horizontal = SpaceMd)) {
                    Text(text = uiState.taskList.name, style = typography.displaySmall)
                }
            }

            if (uiState.tasks.isNotEmpty()) {
                item {
                    Spacer(Modifier.size(SpaceMd))
                    Text(
                        stringResource(R.string.tasks_label),
                        modifier = Modifier.padding(horizontal = SpaceMd),
                        color = colorScheme.onSurfaceVariant,
                        style = typography.labelSmall,
                    )
                }

                items(uiState.tasks, key = TaskItemState::id) { task ->
                    ListItem(
                        headlineContent = {
                            if (task.isDone) {
                                Text(
                                    text = task.name,
                                    modifier = Modifier.semantics {
                                        contentDescription =
                                            resources.getString(R.string.task_name_done, task.name)
                                    },
                                    textDecoration = TextDecoration.LineThrough,
                                )
                            } else {
                                Text(text = task.name)
                            }
                        },
                        modifier = Modifier.clickable { onSelectTask(task.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@DevicePreviews
@Composable
private fun TaskListDetailScreenPreview() {
    DiswantinTheme {
        Scaffold(topBar = {
            TaskListDetailTopBar(
                uiState = TaskListDetailTopBarState(taskListId = 1L, onDeleteTaskList = {}),
                onBackClick = {},
                onEditTaskList = {},
            )
        }) { innerPadding ->
            TaskListDetailLayout(
                uiState = TaskListDetailUiState.Success(
                    taskList = TaskList(name = "Morning Routine"),
                    tasks = persistentListOf(
                        TaskItemState(
                            id = 1L,
                            name = "Brush teeth",
                            recurring = false,
                            isDone = true,
                        ),
                        TaskItemState(id = 2L, name = "Shower", recurring = false, isDone = false),
                        TaskItemState(
                            id = 3L,
                            name = "Eat breakfast",
                            recurring = false,
                            isDone = false,
                        ),
                    ),
                    userMessage = null,
                ),
                onSelectTask = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
