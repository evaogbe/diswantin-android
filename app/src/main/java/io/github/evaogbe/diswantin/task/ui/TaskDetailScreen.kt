package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.TaskWithTaskList
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import java.time.Clock
import java.time.Instant

@Composable
fun TaskDetailScreen(
    onPopBackStack: () -> Unit,
    onEditTask: (Long) -> Unit,
    onSelectTaskList: (Long) -> Unit,
    taskDetailViewModel: TaskDetailViewModel = hiltViewModel(),
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
        onEditTask = onEditTask,
        onRemoveTask = taskDetailViewModel::removeTask,
        snackbarHostState = snackbarHostState,
        uiState = uiState,
        onSelectTaskList = onSelectTaskList,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onBackClick: () -> Unit,
    onEditTask: (Long) -> Unit,
    onRemoveTask: () -> Unit,
    snackbarHostState: SnackbarHostState,
    uiState: TaskDetailUiState,
    onSelectTaskList: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
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
                        IconButton(onClick = { onEditTask(uiState.task.id) }) {
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
                },
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
                    onSelectTaskList = onSelectTaskList,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
fun TaskDetailLayout(
    uiState: TaskDetailUiState.Success,
    onSelectTaskList: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .fillMaxWidth()
                .padding(vertical = SpaceMd)
        ) {
            SelectionContainer(modifier = Modifier.padding(horizontal = SpaceMd)) {
                Text(text = uiState.task.name, style = typography.displaySmall)
            }
            Spacer(Modifier.size(SpaceMd))
            HorizontalDivider()

            if (uiState.formattedDeadline != null) {
                ListItem(
                    headlineContent = {
                        SelectionContainer {
                            Text(text = uiState.formattedDeadline)
                        }
                    },
                    overlineContent = { Text(stringResource(R.string.deadline_label)) }
                )
                HorizontalDivider()
            }

            if (uiState.formattedScheduledAt != null) {
                ListItem(
                    headlineContent = {
                        SelectionContainer {
                            Text(text = uiState.formattedScheduledAt)
                        }
                    },
                    overlineContent = { Text(stringResource(R.string.scheduled_at_label)) }
                )
                HorizontalDivider()
            }

            if (uiState.task.listId != null && uiState.task.listName != null) {
                ListItem(
                    headlineContent = {
                        SelectionContainer {
                            Text(text = uiState.task.listName)
                        }
                    },
                    overlineContent = { Text(stringResource(R.string.task_list_label)) },
                    supportingContent = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.TopEnd,
                        ) {
                            TextButton(onClick = { onSelectTaskList(uiState.task.listId) }) {
                                Text("Go to list")
                            }
                        }
                    }
                )
                HorizontalDivider()
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
                task = TaskWithTaskList(
                    id = 1L,
                    name = "Brush teeth",
                    deadline = null,
                    scheduledAt = null,
                    listId = null,
                    listName = null,
                ),
                userMessage = null,
                clock = Clock.systemDefaultZone()
            ),
            onSelectTaskList = {},
        )
    }
}

@DevicePreviews
@Composable
private fun TaskDetailLayoutPreview() {
    DiswantinTheme {
        Surface {
            TaskDetailLayout(
                uiState = TaskDetailUiState.Success(
                    task = TaskWithTaskList(
                        id = 1L,
                        name = "Brush teeth",
                        deadline = Instant.now(),
                        scheduledAt = null,
                        listId = 1L,
                        listName = "Morning Routine",
                    ),
                    userMessage = null,
                    clock = Clock.systemDefaultZone()
                ),
                onSelectTaskList = {},
            )
        }
    }
}
