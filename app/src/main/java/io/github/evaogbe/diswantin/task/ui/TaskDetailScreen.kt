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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceXs
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.persistentListOf
import java.time.Clock
import java.time.Instant

@Composable
fun TaskDetailScreen(
    onPopBackStack: () -> Unit,
    onEditTask: (Long) -> Unit,
    onSelectTaskItem: (Long) -> Unit,
    taskDetailViewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by taskDetailViewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalContext.current.resources
    val snackbarHostState = remember { SnackbarHostState() }

    if (uiState is TaskDetailUiState.Removed) {
        LaunchedEffect(onPopBackStack) {
            onPopBackStack()
        }
    }

    (uiState as? TaskDetailUiState.Success)?.userMessage?.let { message ->
        LaunchedEffect(message, snackbarHostState) {
            snackbarHostState.showSnackbar(resources.getString(message))
            taskDetailViewModel.userMessageShown()
        }
    }

    TaskDetailScreen(
        onBackClick = onPopBackStack,
        onEditTask = { onEditTask(it.id) },
        onRemoveTask = taskDetailViewModel::removeTask,
        snackbarHostState = snackbarHostState,
        uiState = uiState,
        onSelectTaskItem = { onSelectTaskItem(it.id) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onBackClick: () -> Unit,
    onEditTask: (Task) -> Unit,
    onRemoveTask: () -> Unit,
    snackbarHostState: SnackbarHostState,
    uiState: TaskDetailUiState,
    onSelectTaskItem: (Task) -> Unit,
) {
    Scaffold(
        topBar = {
            var menuExpanded by remember { mutableStateOf(false) }

            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back_button)
                        )
                    }
                },
                actions = {
                    if (uiState is TaskDetailUiState.Success) {
                        IconButton(onClick = { onEditTask(uiState.task) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_button)
                            )
                        }

                        IconButton(onClick = { menuExpanded = !menuExpanded }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more_actions_button)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.delete_button)) },
                                onClick = onRemoveTask,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null
                                    )
                                },
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        when (uiState) {
            is TaskDetailUiState.Pending,
            is TaskDetailUiState.Removed -> {
                PendingLayout(modifier = Modifier.padding(innerPadding))
            }

            is TaskDetailUiState.Failure -> {
                LoadFailureLayout(
                    message = stringResource(R.string.task_detail_fetch_error),
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is TaskDetailUiState.Success -> {
                TaskDetailLayout(
                    uiState = uiState,
                    onSelectTaskItem = onSelectTaskItem,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
fun TaskDetailLayout(
    uiState: TaskDetailUiState.Success,
    onSelectTaskItem: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .fillMaxWidth()
                .padding(vertical = SpaceMd)
        ) {
            item {
                SelectionContainer {
                    Text(
                        text = uiState.task.name,
                        modifier = Modifier.padding(horizontal = SpaceMd),
                        style = typography.displaySmall
                    )
                }
            }

            if (uiState.formattedDueAt != null) {
                item {
                    Spacer(Modifier.size(SpaceMd))
                    Text(
                        stringResource(R.string.due_at_label),
                        modifier = Modifier.padding(horizontal = SpaceMd),
                        color = colorScheme.onSurfaceVariant,
                        style = typography.labelSmall
                    )
                }

                item {
                    Spacer(Modifier.size(SpaceXs))
                    SelectionContainer {
                        Text(
                            text = uiState.formattedDueAt,
                            modifier = Modifier.padding(horizontal = SpaceMd),
                            style = typography.bodyLarge
                        )
                    }
                }
            }

            if (uiState.formattedScheduledAt != null) {
                item {
                    Spacer(Modifier.size(SpaceMd))
                    Text(
                        stringResource(R.string.scheduled_at_label),
                        modifier = Modifier.padding(horizontal = SpaceMd),
                        color = colorScheme.onSurfaceVariant,
                        style = typography.labelSmall
                    )
                }

                item {
                    Spacer(Modifier.size(SpaceXs))
                    SelectionContainer {
                        Text(
                            text = uiState.formattedScheduledAt,
                            modifier = Modifier.padding(horizontal = SpaceMd),
                            style = typography.bodyLarge
                        )
                    }
                }
            }

            if (uiState.taskListItems.isNotEmpty()) {
                item {
                    Spacer(Modifier.size(SpaceLg))
                    Text(
                        stringResource(R.string.task_list_label),
                        modifier = Modifier.padding(horizontal = SpaceMd),
                        color = colorScheme.onSurfaceVariant,
                        style = typography.labelSmall
                    )
                }

                items(uiState.taskListItems, key = Task::id) { task ->
                    ListItem(
                        headlineContent = { Text(text = task.name) },
                        modifier = Modifier.clickable { onSelectTaskItem(task) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@DevicePreviews
@Composable
private fun TaskDetailScreenPreview() {
    DiswantinTheme {
        TaskDetailScreen(
            onBackClick = {},
            onEditTask = {},
            onRemoveTask = {},
            snackbarHostState = SnackbarHostState(),
            uiState = TaskDetailUiState.Success(
                task = Task(
                    id = 1L,
                    createdAt = Instant.now(),
                    name = "Brush teeth",
                ),
                taskListItems = persistentListOf(),
                userMessage = null,
                clock = Clock.systemDefaultZone()
            ),
            onSelectTaskItem = {},
        )
    }
}

@DevicePreviews
@Composable
private fun TaskDetailLayoutPreview_withTaskList() {
    DiswantinTheme {
        Surface {
            TaskDetailLayout(
                uiState = TaskDetailUiState.Success(
                    task = Task(
                        id = 1L,
                        createdAt = Instant.now(),
                        name = "Brush teeth",
                    ),
                    taskListItems = persistentListOf(
                        Task(
                            id = 1L,
                            createdAt = Instant.now(),
                            name = "Brush teeth",
                            dueAt = Instant.now(),
                        ),
                        Task(
                            id = 2L,
                            createdAt = Instant.now(),
                            name = "Shower",
                        ),
                        Task(
                            id = 3L,
                            createdAt = Instant.now(),
                            name = "Eat breakfast",
                        ),
                    ),
                    userMessage = null,
                    clock = Clock.systemDefaultZone()
                ),
                onSelectTaskItem = {},
            )
        }
    }
}


@DevicePreviews
@Composable
private fun TaskDetailLayoutPreview_withDueAtAndTaskList() {
    DiswantinTheme {
        Surface {
            TaskDetailLayout(
                uiState = TaskDetailUiState.Success(
                    task = Task(
                        id = 1L,
                        createdAt = Instant.now(),
                        name = "Brush teeth",
                        dueAt = Instant.now(),
                    ),
                    taskListItems = persistentListOf(
                        Task(
                            id = 1L,
                            createdAt = Instant.now(),
                            name = "Brush teeth",
                            dueAt = Instant.now(),
                        ),
                        Task(
                            id = 2L,
                            createdAt = Instant.now(),
                            name = "Shower",
                        ),
                        Task(
                            id = 3L,
                            createdAt = Instant.now(),
                            name = "Eat breakfast",
                        ),
                    ),
                    userMessage = null,
                    clock = Clock.systemDefaultZone()
                ),
                onSelectTaskItem = {},
            )
        }
    }
}
