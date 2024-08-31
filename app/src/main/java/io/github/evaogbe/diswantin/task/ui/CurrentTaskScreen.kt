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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

@Composable
fun CurrentTaskScreen(
    setCurrentTaskId: (Long?) -> Unit,
    setUserMessage: (String) -> Unit,
    onAddTask: () -> Unit,
    onAdviceClick: () -> Unit,
    currentTaskViewModel: CurrentTaskViewModel = hiltViewModel(),
) {
    val uiState by currentTaskViewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalContext.current.resources

    LaunchedEffect(currentTaskViewModel) {
        currentTaskViewModel.initialize()
    }

    LaunchedEffect(uiState) {
        setCurrentTaskId((uiState as? CurrentTaskUiState.Present)?.currentTask?.id)
    }

    (uiState as? CurrentTaskUiState.Present)?.userMessage?.let { message ->
        LaunchedEffect(message) {
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
                onAdviceClick = onAdviceClick,
                onMarkTaskDone = currentTaskViewModel::markCurrentTaskDone,
            )
        }
    }
}

@Composable
fun CurrentTaskLayout(
    task: Task,
    onAdviceClick: () -> Unit,
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
                Text(text = task.name, style = typography.displaySmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                OutlinedButton(onClick = onAdviceClick) {
                    Text(stringResource(R.string.advice_button))
                }
                OutlinedButton(onClick = onMarkTaskDone) {
                    Text(stringResource(R.string.mark_done_button))
                }
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
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.add_task_button))
            }
        }
    }
}

@DevicePreviews
@Composable
private fun CurrentTaskLayoutPreview() {
    DiswantinTheme {
        Surface {
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

@DevicePreviews
@Composable
private fun EmptyCurrentTaskLayoutPreview() {
    DiswantinTheme {
        EmptyCurrentTaskLayout(onAddTask = {})
    }
}
