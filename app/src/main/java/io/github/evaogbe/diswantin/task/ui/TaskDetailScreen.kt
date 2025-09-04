package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.ui.loadstate.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.loadstate.PendingLayout
import io.github.evaogbe.diswantin.ui.loadstate.pagedListFooter
import io.github.evaogbe.diswantin.ui.snackbar.SnackbarHandler
import io.github.evaogbe.diswantin.ui.snackbar.SnackbarState
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.flowOf
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
                    painterResource(R.drawable.baseline_arrow_back_24),
                    contentDescription = stringResource(R.string.back_button)
                )
            }
        },
        actions = {
            if (uiState.taskId != null) {
                IconButton(onClick = { onEditTask(uiState.taskId) }) {
                    Icon(
                        painterResource(R.drawable.outline_edit_24),
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
                            painterResource(R.drawable.baseline_done_24),
                            contentDescription = stringResource(R.string.mark_done_button),
                        )
                    }
                }

                IconButton(onClick = { menuExpanded = !menuExpanded }) {
                    Icon(
                        painterResource(R.drawable.outline_more_vert_24),
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
                            Icon(
                                painterResource(R.drawable.baseline_delete_24),
                                contentDescription = null
                            )
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
    showSnackbar: SnackbarHandler,
    onNavigateToTask: (Long) -> Unit,
    onNavigateToTag: (Long) -> Unit,
    taskDetailViewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val currentOnPopBackstack by rememberUpdatedState(onPopBackStack)
    val currentTopBarActionHandled by rememberUpdatedState(topBarActionHandled)
    val currentShowSnackbar by rememberUpdatedState(showSnackbar)
    val childTaskPagingItems = taskDetailViewModel.childTaskPagingData.collectAsLazyPagingItems()
    val uiState by taskDetailViewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val currentResources by rememberUpdatedState(resources)

    LaunchedEffect(uiState, setTopBarState) {
        setTopBarState(
            TaskDetailTopBarState(
                taskId = (uiState as? TaskDetailUiState.Success)?.id,
                isDone = (uiState as? TaskDetailUiState.Success)?.isDone == true,
            ),
        )
    }

    LaunchedEffect(topBarAction) {
        when (topBarAction) {
            null -> {}
            TaskDetailTopBarAction.MarkDone -> {
                taskDetailViewModel.markTaskDone()
                currentTopBarActionHandled()
            }

            TaskDetailTopBarAction.UnmarkDone -> {
                taskDetailViewModel.unmarkTaskDone()
                currentTopBarActionHandled()
            }

            TaskDetailTopBarAction.Delete -> {
                taskDetailViewModel.deleteTask()
                currentTopBarActionHandled()
            }
        }
    }

    when (val state = uiState) {
        is TaskDetailUiState.Pending -> {
            PendingLayout()
        }

        is TaskDetailUiState.Failure -> {
            LoadFailureLayout(message = stringResource(R.string.task_detail_fetch_error))
        }

        is TaskDetailUiState.Deleted -> {
            LaunchedEffect(Unit) {
                currentOnPopBackstack()
            }

            PendingLayout()
        }

        is TaskDetailUiState.Success -> {
            when (childTaskPagingItems.loadState.refresh) {
                is LoadState.Loading -> PendingLayout()
                is LoadState.Error -> {
                    LoadFailureLayout(
                        message = stringResource(R.string.task_detail_fetch_error),
                        onRetry = childTaskPagingItems::retry,
                    )
                }

                is LoadState.NotLoading -> {
                    LaunchedEffect(state.userMessage) {
                        when (state.userMessage) {
                            null -> {}
                            TaskDetailUserMessage.MarkDoneError -> {
                                currentShowSnackbar(
                                    SnackbarState.create(
                                        currentResources.getString(R.string.task_detail_mark_done_error)
                                    )
                                )
                                taskDetailViewModel.userMessageShown()
                            }

                            TaskDetailUserMessage.UnmarkDoneError -> {
                                currentShowSnackbar(
                                    SnackbarState.create(
                                        currentResources.getString(R.string.task_detail_unmark_done_error)
                                    )
                                )
                                taskDetailViewModel.userMessageShown()
                            }

                            TaskDetailUserMessage.DeleteError -> {
                                currentShowSnackbar(
                                    SnackbarState.create(
                                        currentResources.getString(R.string.task_detail_delete_error)
                                    )
                                )
                                taskDetailViewModel.userMessageShown()
                            }
                        }
                    }

                    TaskDetailLayout(
                        uiState = state,
                        childTaskItems = childTaskPagingItems,
                        onNavigateToTask = onNavigateToTask,
                        onNavigateToTag = onNavigateToTag,
                    )
                }
            }
        }
    }
}

@Composable
fun TaskDetailLayout(
    uiState: TaskDetailUiState.Success,
    childTaskItems: LazyPagingItems<TaskSummaryUiState>,
    onNavigateToTask: (Long) -> Unit,
    onNavigateToTag: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    TaskDetailLayout(
        uiState = uiState,
        childTaskItems = if (childTaskItems.itemCount > 0) {
            {
                items(
                    childTaskItems.itemCount,
                    key = childTaskItems.itemKey(TaskSummaryUiState::id),
                ) { index ->
                    val task = childTaskItems[index]!!
                    TaskSummaryItem(task = task, onSelectTask = onNavigateToTask)
                    HorizontalDivider()
                }

                pagedListFooter(
                    pagingItems = childTaskItems,
                    errorMessage = {
                        Text(stringResource(R.string.task_detail_fetch_children_error))
                    },
                )
            }
        } else null,
        onNavigateToTask = onNavigateToTask,
        onNavigateToTag = onNavigateToTag,
        modifier = modifier,
    )
}

@Composable
fun TaskDetailLayout(
    uiState: TaskDetailUiState.Success,
    childTaskItems: (LazyListScope.() -> Unit)?,
    onNavigateToTask: (Long) -> Unit,
    onNavigateToTag: (Long) -> Unit,
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
                SelectionContainer(modifier = Modifier.padding(horizontal = SpaceMd)) {
                    TaskSummaryName(task = uiState.summary, style = typography.displaySmall)
                }
                Spacer(Modifier.size(SpaceMd))
            }

            if (uiState.note.isNotEmpty()) {
                item {
                    SelectionContainer(modifier = Modifier.padding(horizontal = SpaceMd)) {
                        Text(
                            text = uiState.note,
                            color = colorScheme.onSurfaceVariant,
                            style = typography.bodyLarge,
                        )
                    }
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

            if (uiState.parent != null) {
                item {
                    ListItem(
                        headlineContent = {
                            SelectionContainer {
                                TaskSummaryName(task = uiState.parent)
                            }
                        },
                        overlineContent = { Text(stringResource(R.string.parent_task_label)) },
                        supportingContent = {
                            TextButton(onClick = { onNavigateToTask(uiState.parent.id) }) {
                                Text(stringResource(R.string.view_task_button))
                            }
                        },
                    )
                }
            }

            if (childTaskItems != null) {
                item {
                    Spacer(Modifier.size(SpaceSm))
                    Text(
                        stringResource(R.string.child_tasks_label),
                        modifier = Modifier.padding(horizontal = SpaceMd),
                        color = colorScheme.onSurfaceVariant,
                        style = typography.labelSmall,
                    )
                }

                childTaskItems()

                item {
                    Spacer(Modifier.size(SpaceSm))
                }
            }

            if (uiState.tags.isNotEmpty()) {
                item {
                    Spacer(Modifier.size(SpaceSm))
                    Text(
                        stringResource(R.string.tags_label),
                        modifier = Modifier.padding(horizontal = SpaceMd),
                        color = colorScheme.onSurfaceVariant,
                        style = typography.labelSmall,
                    )
                    FlowRow(
                        modifier = Modifier.padding(horizontal = SpaceMd),
                        horizontalArrangement = Arrangement.spacedBy(SpaceSm),
                    ) {
                        uiState.tags.forEach { tag ->
                            SuggestionChip(
                                onClick = { onNavigateToTag(tag.id) },
                                label = { Text(text = tag.name) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@DevicePreviews
@Composable
private fun TaskDetailScreenPreview_Minimal() {
    val childTaskItems = flowOf(PagingData.empty<TaskSummaryUiState>()).collectAsLazyPagingItems()

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
                    id = 2L,
                    name = "Shower",
                    note = "",
                    formattedDeadline = null,
                    formattedStartAfter = null,
                    formattedScheduledAt = null,
                    recurrence = null,
                    isDone = false,
                    parent = null,
                    tags = persistentListOf(),
                    userMessage = null,
                ),
                childTaskItems = childTaskItems,
                onNavigateToTask = {},
                onNavigateToTag = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskDetailScreenPreview_Detailed() {
    val childTaskItems = listOf(
        TaskSummaryUiState(id = 3L, name = "Eat breakfast", isDone = false),
        TaskSummaryUiState(id = 4L, name = "Go to work", isDone = true),
    )

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
                    id = 2L,
                    name = "Shower",
                    note = "Wash hair and deep condition before appointment at hair salon",
                    formattedDeadline = formatDateTime(LocalDate.now(), null),
                    formattedStartAfter = formatDateTime(null, LocalTime.now()),
                    formattedScheduledAt = null,
                    recurrence = null,
                    isDone = true,
                    parent = TaskSummaryUiState(id = 1L, name = "Brush teeth", isDone = false),
                    tags = persistentListOf(
                        Tag(id = 1L, name = "goal"),
                        Tag(id = 2L, name = "hygiene"),
                        Tag(id = 3L, name = "low effort"),
                        Tag(id = 4L, name = "morning routine"),
                    ),
                    userMessage = null,
                ),
                childTaskItems = {
                    items(childTaskItems, key = TaskSummaryUiState::id) { task ->
                        TaskSummaryItem(task = task, onSelectTask = {})
                        HorizontalDivider()
                    }
                },
                onNavigateToTask = {},
                onNavigateToTag = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskDetailLayoutPreview() {
    val childTaskItems = flowOf(PagingData.empty<TaskSummaryUiState>()).collectAsLazyPagingItems()

    DiswantinTheme {
        Surface {
            TaskDetailLayout(
                uiState = TaskDetailUiState.Success(
                    id = 2L,
                    name = "Shower",
                    note = "",
                    formattedDeadline = null,
                    formattedStartAfter = null,
                    formattedScheduledAt = formatDateTime(null, LocalTime.now()),
                    recurrence = TaskRecurrenceUiState(
                        startDate = LocalDate.now(),
                        endDate = null,
                        type = RecurrenceType.Day,
                        step = 1,
                        weekdays = persistentSetOf(),
                        locale = Locale.getDefault(),
                    ),
                    isDone = false,
                    parent = TaskSummaryUiState(id = 1L, name = "Brush teeth", isDone = true),
                    tags = persistentListOf(Tag(id = 1L, name = "morning routine")),
                    userMessage = null,
                ),
                childTaskItems = childTaskItems,
                onNavigateToTask = {},
                onNavigateToTag = {},
            )
        }
    }
}
