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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskList
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.persistentListOf
import java.time.Instant

@Composable
fun TaskListDetailScreen(
    onBackClick: () -> Unit,
    onSelectTask: (Long) -> Unit,
    taskListDetailViewModel: TaskListDetailViewModel = hiltViewModel(),
) {
    val uiState by taskListDetailViewModel.uiState.collectAsStateWithLifecycle()

    TaskListDetailScreen(
        onBackClick = onBackClick,
        uiState = uiState,
        onSelectTask = { onSelectTask(it.id) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListDetailScreen(
    onBackClick: () -> Unit,
    uiState: TaskListDetailUiState,
    onSelectTask: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
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
            )
        },
    ) { innerPadding ->
        when (uiState) {
            is TaskListDetailUiState.Pending -> {
                PendingLayout(modifier = Modifier.padding(innerPadding))
            }

            is TaskListDetailUiState.Failure -> {
                LoadFailureLayout(
                    message = stringResource(R.string.task_list_detail_fetch_error),
                    modifier = Modifier.padding(innerPadding),
                )
            }

            is TaskListDetailUiState.Success -> {
                TaskListDetailLayout(
                    uiState = uiState,
                    onSelectTask = onSelectTask,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
fun TaskListDetailLayout(
    uiState: TaskListDetailUiState.Success,
    onSelectTask: (Task) -> Unit,
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

                items(uiState.tasks, key = Task::id) { task ->
                    ListItem(
                        headlineContent = { Text(text = task.name) },
                        modifier = Modifier.clickable { onSelectTask(task) },
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
        TaskListDetailScreen(
            onBackClick = {},
            uiState = TaskListDetailUiState.Success(
                taskList = TaskList(name = "Morning Routine"),
                tasks = persistentListOf(
                    Task(id = 1L, createdAt = Instant.now(), name = "Brush teeth"),
                    Task(id = 2L, createdAt = Instant.now(), name = "Shower"),
                    Task(id = 3L, createdAt = Instant.now(), name = "Eat breakfast"),
                ),
            ),
            onSelectTask = {},
        )
    }
}
