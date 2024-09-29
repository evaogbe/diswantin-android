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
import androidx.compose.material.icons.filled.Done
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskDetail
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailTopBar(
    uiState: TaskDetailTopBarState,
    onBackClick: () -> Unit,
    onEditTask: (Long) -> Unit,
    onDeleteTask: () -> Unit,
    onMarkTaskDone: () -> Unit,
    onUnmarkTaskDone: () -> Unit,
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
                    contentDescription = stringResource(R.string.back_button)
                )
            }
        },
        actions = {
            if (uiState.taskId != null) {
                IconButton(onClick = { onEditTask(uiState.taskId) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_button),
                    )
                }

                if (uiState.isDone) {
                    IconButton(onClick = onUnmarkTaskDone) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_remove_done_24),
                            contentDescription = stringResource(R.string.unmark_done_button)
                        )
                    }
                } else {
                    IconButton(onClick = onMarkTaskDone) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = stringResource(R.string.mark_done_button),
                        )
                    }
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
                            onDeleteTask()
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
fun TaskDetailScreen(
    onPopBackStack: () -> Unit,
    setTopBarState: (TaskDetailTopBarState) -> Unit,
    topBarAction: TaskDetailTopBarAction?,
    topBarActionHandled: () -> Unit,
    setUserMessage: (UserMessage) -> Unit,
    onNavigateToTask: (Long) -> Unit,
    onNavigateToCategory: (Long) -> Unit,
    taskDetailViewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val uiState by taskDetailViewModel.uiState.collectAsStateWithLifecycle()

    if (uiState is TaskDetailUiState.Deleted) {
        LaunchedEffect(onPopBackStack) {
            onPopBackStack()
        }
    }

    LaunchedEffect(setTopBarState, uiState) {
        setTopBarState(
            TaskDetailTopBarState(
                taskId = (uiState as? TaskDetailUiState.Success)?.task?.id,
                isDone = (uiState as? TaskDetailUiState.Success)?.isDone == true,
            ),
        )
    }

    LaunchedEffect(topBarAction, taskDetailViewModel) {
        when (topBarAction) {
            null -> {}
            TaskDetailTopBarAction.MarkDone -> {
                taskDetailViewModel.markTaskDone()
                topBarActionHandled()
            }

            TaskDetailTopBarAction.UnmarkDone -> {
                taskDetailViewModel.unmarkTaskDone()
                topBarActionHandled()
            }

            TaskDetailTopBarAction.Delete -> {
                taskDetailViewModel.deleteTask()
                topBarActionHandled()
            }
        }
    }

    (uiState as? TaskDetailUiState.Success)?.userMessage?.let { message ->
        LaunchedEffect(message, setUserMessage) {
            setUserMessage(message)
            taskDetailViewModel.userMessageShown()
        }
    }

    when (val state = uiState) {
        is TaskDetailUiState.Pending,
        is TaskDetailUiState.Deleted -> {
            PendingLayout()
        }

        is TaskDetailUiState.Failure -> {
            LoadFailureLayout(message = stringResource(R.string.task_detail_fetch_error))
        }

        is TaskDetailUiState.Success -> {
            TaskDetailLayout(
                uiState = state,
                onNavigateToTask = onNavigateToTask,
                onNavigateToCategory = onNavigateToCategory,
            )
        }
    }
}

@Composable
fun TaskDetailLayout(
    uiState: TaskDetailUiState.Success,
    onNavigateToTask: (Long) -> Unit,
    onNavigateToCategory: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .fillMaxWidth()
                .padding(vertical = SpaceMd)
        ) {
            item {
                SelectionContainer(modifier = Modifier.padding(horizontal = SpaceMd)) {
                    if (uiState.isDone) {
                        Text(
                            text = uiState.task.name,
                            modifier = Modifier.semantics {
                                contentDescription =
                                    resources.getString(R.string.task_name_done, uiState.task.name)
                            },
                            textDecoration = TextDecoration.LineThrough,
                            style = typography.displaySmall,
                        )
                    } else {
                        Text(text = uiState.task.name, style = typography.displaySmall)
                    }
                }
                Spacer(Modifier.size(SpaceMd))
            }

            if (uiState.task.note.isNotEmpty()) {
                item {
                    SelectionContainer(modifier = Modifier.padding(horizontal = SpaceMd)) {
                        Text(
                            text = uiState.task.note,
                            color = colorScheme.onSurfaceVariant,
                            style = typography.bodyMedium,
                        )
                    }
                }
            }

            if (uiState.formattedDeadline != null) {
                item {
                    ListItem(
                        headlineContent = {
                            SelectionContainer {
                                Text(text = uiState.formattedDeadline)
                            }
                        },
                        overlineContent = { Text(stringResource(R.string.deadline_label)) },
                    )
                }
            }

            if (uiState.formattedStartAfter != null) {
                item {
                    ListItem(
                        headlineContent = {
                            SelectionContainer {
                                Text(text = uiState.formattedStartAfter)
                            }
                        },
                        overlineContent = { Text(stringResource(R.string.start_after_label)) },
                    )
                }
            }

            if (uiState.formattedScheduledAt != null) {
                item {
                    ListItem(
                        headlineContent = {
                            SelectionContainer {
                                Text(text = uiState.formattedScheduledAt)
                            }
                        },
                        overlineContent = { Text(stringResource(R.string.scheduled_at_label)) },
                    )
                }
            }

            if (uiState.recurrence != null) {
                item {
                    ListItem(
                        headlineContent = {
                            SelectionContainer {
                                Text(text = taskRecurrenceText(uiState.recurrence))
                            }
                        },
                        overlineContent = { Text(stringResource(R.string.recurrence_label)) },
                    )
                }
            }

            if (uiState.task.categoryId != null && uiState.task.categoryName != null) {
                item {
                    ListItem(
                        headlineContent = {
                            SelectionContainer {
                                Text(text = uiState.task.categoryName)
                            }
                        },
                        overlineContent = { Text(stringResource(R.string.task_category_label)) },
                        supportingContent = {
                            TextButton(onClick = { onNavigateToCategory(uiState.task.categoryId) }) {
                                Text(stringResource(R.string.view_task_category_button))
                            }
                        },
                    )
                }
            }

            if (uiState.task.parentId != null && uiState.task.parentName != null) {
                item {
                    ListItem(
                        headlineContent = {
                            SelectionContainer {
                                Text(text = uiState.task.parentName)
                            }
                        },
                        overlineContent = { Text(stringResource(R.string.parent_task_label)) },
                        supportingContent = {
                            TextButton(onClick = { onNavigateToTask(uiState.task.parentId) }) {
                                Text(stringResource(R.string.view_task_button))
                            }
                        },
                    )
                }
            }

            if (uiState.childTasks.isNotEmpty()) {
                item {
                    Spacer(Modifier.size(SpaceSm))
                    Text(
                        stringResource(R.string.child_tasks_label),
                        modifier = Modifier.padding(horizontal = SpaceMd),
                        color = colorScheme.onSurfaceVariant,
                        style = typography.labelSmall,
                    )
                }

                items(uiState.childTasks, key = Task::id) { task ->
                    ListItem(
                        headlineContent = { Text(text = task.name) },
                        modifier = Modifier.clickable { onNavigateToTask(task.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@DevicePreviews
@Composable
private fun TaskDetailScreenPreview_Minimal() {
    DiswantinTheme {
        Scaffold(
            topBar = {
                TaskDetailTopBar(
                    uiState = TaskDetailTopBarState(taskId = 2L, isDone = false),
                    onBackClick = {},
                    onEditTask = {},
                    onDeleteTask = {},
                    onMarkTaskDone = {},
                    onUnmarkTaskDone = {},
                )
            },
        ) { innerPadding ->
            TaskDetailLayout(
                uiState = TaskDetailUiState.Success(
                    task = TaskDetail(
                        id = 2L,
                        name = "Shower",
                        note = "",
                        deadlineDate = null,
                        deadlineTime = null,
                        startAfterDate = null,
                        startAfterTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        doneAt = null,
                        categoryId = null,
                        categoryName = null,
                        parentId = null,
                        parentName = null,
                    ),
                    recurrence = null,
                    childTasks = persistentListOf(),
                    userMessage = null,
                    clock = Clock.systemDefaultZone(),
                ),
                onNavigateToTask = {},
                onNavigateToCategory = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskDetailScreenPreview_Detailed() {
    DiswantinTheme {
        Scaffold(
            topBar = {
                TaskDetailTopBar(
                    uiState = TaskDetailTopBarState(taskId = 2L, isDone = true),
                    onBackClick = {},
                    onEditTask = {},
                    onDeleteTask = {},
                    onMarkTaskDone = {},
                    onUnmarkTaskDone = {},
                )
            },
        ) { innerPadding ->
            TaskDetailLayout(
                uiState = TaskDetailUiState.Success(
                    task = TaskDetail(
                        id = 2L,
                        name = "Shower",
                        note = "Wash hair and deep condition before appointment at hair salon",
                        deadlineDate = LocalDate.now(),
                        deadlineTime = null,
                        startAfterDate = null,
                        startAfterTime = LocalTime.now(),
                        scheduledDate = null,
                        scheduledTime = null,
                        doneAt = Instant.now(),
                        categoryId = 1L,
                        categoryName = "Morning Routine",
                        parentId = 1L,
                        parentName = "Brush teeth",
                    ),
                    recurrence = null,
                    childTasks = persistentListOf(
                        Task(id = 3L, createdAt = Instant.now(), name = "Eat breakfast"),
                        Task(id = 4L, createdAt = Instant.now(), name = "Go to work"),
                    ),
                    userMessage = null,
                    clock = Clock.systemDefaultZone(),
                ),
                onNavigateToTask = {},
                onNavigateToCategory = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskDetailLayoutPreview() {
    DiswantinTheme {
        Surface {
            TaskDetailLayout(
                uiState = TaskDetailUiState.Success(
                    task = TaskDetail(
                        id = 2L,
                        name = "Shower",
                        note = "",
                        deadlineDate = null,
                        deadlineTime = null,
                        startAfterDate = null,
                        startAfterTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        doneAt = null,
                        categoryId = null,
                        categoryName = null,
                        parentId = null,
                        parentName = null,
                    ),
                    recurrence = TaskRecurrenceUiState(
                        start = LocalDate.now(),
                        type = RecurrenceType.Day,
                        step = 1,
                        weekdays = persistentSetOf(),
                        locale = Locale.getDefault(),
                    ),
                    childTasks = persistentListOf(),
                    userMessage = null,
                    clock = Clock.systemDefaultZone(),
                ),
                onNavigateToTask = {},
                onNavigateToCategory = {},
            )
        }
    }
}
