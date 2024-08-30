package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.TaskList
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.IconSizeLg
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceXl
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun TaskListsScreen(
    onAddList: () -> Unit,
    taskListsViewModel: TaskListsViewModel = hiltViewModel(),
) {
    val uiState by taskListsViewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is TaskListsUiState.Pending -> PendingLayout()
        is TaskListsUiState.Failure -> {
            LoadFailureLayout(message = stringResource(R.string.task_lists_fetch_error))
        }

        is TaskListsUiState.Success -> {
            if (state.taskLists.isEmpty()) {
                EmptyTaskListsLayout(onAddList = onAddList)
            } else {
                TaskListsLayout(taskLists = state.taskLists)
            }
        }
    }
}

@Composable
fun TaskListsLayout(taskLists: ImmutableList<TaskList>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .fillMaxSize(),
        ) {
            items(taskLists, key = TaskList::id) { taskList ->
                ListItem(headlineContent = { Text(text = taskList.name) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun EmptyTaskListsLayout(onAddList: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = colorScheme.surfaceVariant) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.list_alt_add_24px),
                contentDescription = null,
                modifier = Modifier.size(IconSizeLg),
            )
            Spacer(Modifier.size(SpaceXl))
            Text(
                stringResource(R.string.task_lists_empty),
                textAlign = TextAlign.Center,
                style = typography.headlineLarge,
            )
            Spacer(Modifier.size(SpaceLg))
            Button(
                onClick = onAddList,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.add_task_list_button))
            }
        }
    }
}

@DevicePreviews
@Composable
private fun TaskListsLayoutPreview() {
    DiswantinTheme {
        Surface {
            TaskListsLayout(
                taskLists = persistentListOf(
                    TaskList(id = 1L, name = "Morning routine"),
                    TaskList(id = 2L, name = "Work"),
                    TaskList(id = 3L, name = "Bedtime routine")
                ),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun EmptyTaskListsLayoutPreview() {
    DiswantinTheme {
        EmptyTaskListsLayout(onAddList = {})
    }
}
