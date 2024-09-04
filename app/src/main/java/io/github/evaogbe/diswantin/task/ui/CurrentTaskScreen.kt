package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.IconSizeLg
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceXl
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import java.time.Instant

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

    LaunchedEffect(currentTaskViewModel) {
        currentTaskViewModel.initialize()
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
                task = state.currentTask,
                onMarkTaskDone = currentTaskViewModel::markCurrentTaskDone,
            )
        }
    }
}

@Composable
fun CurrentTaskLayout(task: Task, onMarkTaskDone: () -> Unit, modifier: Modifier = Modifier) {
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
                Text(text = task.name, style = typography.displaySmall)
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
                task = Task(
                    id = 1L,
                    createdAt = Instant.now(),
                    name = "Brush teeth"
                ),
                onMarkTaskDone = {},
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
