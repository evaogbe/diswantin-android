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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.ui.components.ButtonWithIcon
import io.github.evaogbe.diswantin.ui.components.FilledTonalButtonWithIcon
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.OutlinedButtonWithIcon
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.IconSizeLg
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceXl
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import java.time.Instant
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentTaskTopBar(onSearch: () -> Unit, modifier: Modifier = Modifier) {
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
        },
    )
}

@Composable
fun CurrentTaskScreen(
    setUserMessage: (Int) -> Unit,
    onAddTask: () -> Unit,
    onNavigateToTask: (Long) -> Unit,
    currentTaskViewModel: CurrentTaskViewModel = hiltViewModel(),
) {
    val uiState by currentTaskViewModel.uiState.collectAsStateWithLifecycle()
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

    (uiState as? CurrentTaskUiState.Present)?.userMessage?.let { message ->
        LaunchedEffect(message, setUserMessage) {
            setUserMessage(message)
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
                onNavigateToTask = onNavigateToTask,
                onMarkTaskDone = currentTaskViewModel::markCurrentTaskDone,
            )
        }
    }
}

@Composable
fun CurrentTaskLayout(
    uiState: CurrentTaskUiState.Present,
    onNavigateToTask: (Long) -> Unit,
    onMarkTaskDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                OutlinedButtonWithIcon(
                    onClick = { onNavigateToTask(uiState.currentTask.id) },
                    painter = painterResource(R.drawable.baseline_details_24),
                    text = stringResource(R.string.current_task_view_details_button),
                )
                FilledTonalButtonWithIcon(
                    onClick = onMarkTaskDone,
                    imageVector = Icons.Default.Done,
                    text = stringResource(R.string.current_task_mark_done_button),
                )
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
            ButtonWithIcon(
                onClick = onAddTask,
                imageVector = Icons.Default.Add,
                text = stringResource(R.string.add_task_button),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun CurrentTaskScreenPreview_Present() {
    DiswantinTheme {
        Scaffold(topBar = { CurrentTaskTopBar(onSearch = {}) }) { innerPadding ->
            CurrentTaskLayout(
                uiState = CurrentTaskUiState.Present(
                    currentTask = Task(
                        id = 1L,
                        createdAt = Instant.now(),
                        name = "Brush teeth",
                    ),
                    userMessage = null,
                ),
                onNavigateToTask = {},
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
        Scaffold(topBar = { CurrentTaskTopBar(onSearch = {}) }) { innerPadding ->
            EmptyCurrentTaskLayout(onAddTask = {}, modifier = Modifier.padding(innerPadding))
        }
    }
}
