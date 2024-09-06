package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.data.Result
import io.github.evaogbe.diswantin.data.getOrDefault
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.ui.components.AutocompleteField
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.IconSizeLg
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceXl
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import java.time.Instant
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentTaskTopBar(
    uiState: CurrentTaskTopBarState,
    onSearch: () -> Unit,
    onEditTask: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {},
        modifier = modifier,
        actions = {
            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_tasks_button),
                )
            }

            if (uiState.taskId != null) {
                IconButton(onClick = { onEditTask(uiState.taskId) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_button),
                    )
                }
            }
        },
    )
}

@Composable
fun CurrentTaskScreen(
    setTopBarState: (CurrentTaskTopBarState) -> Unit,
    setUserMessage: (String) -> Unit,
    onAddTask: () -> Unit,
    currentTaskViewModel: CurrentTaskViewModel = hiltViewModel(),
) {
    val uiState by currentTaskViewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalContext.current.resources
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(currentTaskViewModel) {
        currentTaskViewModel.refresh()
    }

    LaunchedEffect(lifecycleOwner) {
        flow {
            while (true) {
                delay(1.hours)
                emit(Unit)
            }
        }.flowWithLifecycle(lifecycleOwner.lifecycle)
            .collectLatest {
                currentTaskViewModel.refresh()
            }
    }

    LaunchedEffect(uiState, setTopBarState) {
        setTopBarState(
            CurrentTaskTopBarState(
                taskId = (uiState as? CurrentTaskUiState.Present)?.currentTask?.id,
            )
        )
    }

    (uiState as? CurrentTaskUiState.Present)?.userMessage?.let { message ->
        LaunchedEffect(message, setUserMessage) {
            setUserMessage(resources.getString(message))
            currentTaskViewModel.userMessageShown()
        }
    }

    when (val state = uiState) {
        is CurrentTaskUiState.Pending -> PendingLayout()
        is CurrentTaskUiState.Failure -> {
            LoadFailureLayout(message = stringResource(R.string.current_task_fetch_error))
        }

        is CurrentTaskUiState.Empty -> EmptyCurrentTaskLayout(onAddTask = onAddTask)
        is CurrentTaskUiState.Present -> {
            CurrentTaskLayout(
                uiState = state,
                onMarkTaskDone = currentTaskViewModel::markCurrentTaskDone,
                onSelectParentTask = currentTaskViewModel::selectParentTask,
                onSearchTasks = currentTaskViewModel::searchTasks,
            )
        }
    }
}

@Composable
fun CurrentTaskLayout(
    uiState: CurrentTaskUiState.Present,
    onMarkTaskDone: () -> Unit,
    onSelectParentTask: (Task) -> Unit,
    onSearchTasks: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSkipDialog by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .padding(SpaceMd)
                .widthIn(max = ScreenLg)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SelectionContainer {
                Text(text = uiState.currentTask.name, style = typography.displaySmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                OutlinedButton(
                    onClick = { showSkipDialog = true },
                    enabled = uiState.canSkip,
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_skip_next_24),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.skip_button))
                }
                FilledTonalButton(
                    onClick = onMarkTaskDone,
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.mark_done_button))
                }
            }
        }
    }

    if (showSkipDialog) {
        var parentTask by rememberSaveable { mutableStateOf<Task?>(null) }

        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSkipDialog = false
                        parentTask?.let(onSelectParentTask)
                    },
                    enabled = parentTask != null,
                ) {
                    Text(stringResource(R.string.confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            },
            title = { Text(stringResource(R.string.skip_dialog_title)) },
            text = {
                var query by rememberSaveable { mutableStateOf("") }

                Column {
                    if (uiState.parentTaskOptions is Result.Failure) {
                        SelectionContainer {
                            Text(
                                text = stringResource(R.string.search_task_options_error),
                                color = colorScheme.error,
                                style = typography.titleSmall,
                            )
                        }
                        Spacer(Modifier.size(SpaceMd))
                    }

                    AutocompleteField(
                        query = query,
                        onQueryChange = { query = it },
                        label = { Text(stringResource(R.string.parent_task_label)) },
                        onSearch = onSearchTasks,
                        options = uiState.parentTaskOptions.getOrDefault(persistentListOf()),
                        formatOption = Task::name,
                        onSelectOption = {
                            parentTask = it
                            query = it.name
                        },
                        autoFocus = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        )
    }
}

@Composable
fun EmptyCurrentTaskLayout(onAddTask: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = colorScheme.surfaceVariant) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(SpaceMd),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_add_task_24),
                contentDescription = null,
                modifier = Modifier.size(IconSizeLg),
            )
            Spacer(Modifier.size(SpaceXl))
            Text(
                stringResource(R.string.current_task_empty),
                textAlign = TextAlign.Center,
                style = typography.headlineLarge
            )
            Spacer(Modifier.size(SpaceLg))
            Button(
                onClick = onAddTask,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.add_task_button))
            }
        }
    }
}

@DevicePreviews
@Composable
private fun CurrentTaskScreenPreview_Present() {
    DiswantinTheme {
        Scaffold(topBar = {
            CurrentTaskTopBar(
                uiState = CurrentTaskTopBarState(taskId = 1L),
                onSearch = {},
                onEditTask = {},
            )
        }) { innerPadding ->
            CurrentTaskLayout(
                uiState = CurrentTaskUiState.Present(
                    currentTask = Task(
                        id = 1L,
                        createdAt = Instant.now(),
                        name = "Brush teeth",
                    ),
                    canSkip = true,
                    parentTaskOptions = Result.Success(persistentListOf()),
                    userMessage = null,
                ),
                onMarkTaskDone = {},
                onSelectParentTask = {},
                onSearchTasks = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun CurrentTaskScreenPreview_Empty() {
    DiswantinTheme {
        Scaffold(topBar = {
            CurrentTaskTopBar(
                uiState = CurrentTaskTopBarState(taskId = null),
                onSearch = {},
                onEditTask = {},
            )
        }) { innerPadding ->
            EmptyCurrentTaskLayout(onAddTask = {}, modifier = Modifier.padding(innerPadding))
        }
    }
}
