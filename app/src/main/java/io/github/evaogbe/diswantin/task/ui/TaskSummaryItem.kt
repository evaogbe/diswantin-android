package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews

@Composable
fun TaskSummaryName(task: TaskSummaryUiState, style: TextStyle = LocalTextStyle.current) {
    val resources = LocalResources.current

    if (task.isDone) {
        Text(
            text = task.name,
            modifier = Modifier.semantics {
                contentDescription = resources.getString(R.string.task_name_done, task.name)
            },
            textDecoration = TextDecoration.LineThrough,
            style = style,
        )
    } else {
        Text(text = task.name, style = style)
    }
}

@Composable
fun TaskSummaryItem(task: TaskSummaryUiState, onSelectTask: (Long) -> Unit) {
    ListItem(
        headlineContent = { TaskSummaryName(task = task) },
        modifier = Modifier.clickable { onSelectTask(task.id) },
    )
}

@DevicePreviews
@Composable
private fun TaskSummaryItemPreview_Undone() {
    DiswantinTheme {
        Surface {
            TaskSummaryItem(
                task = TaskSummaryUiState(id = 1L, name = "Brush teeth", isDone = false),
                onSelectTask = {},
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskSummaryItemPreview_Done() {
    DiswantinTheme {
        Surface {
            TaskSummaryItem(
                task = TaskSummaryUiState(id = 1L, name = "Brush teeth", isDone = true),
                onSelectTask = {},
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskSummaryListPreview() {
    val taskItems = listOf(
        TaskSummaryUiState(id = 1L, name = "Brush teeth", isDone = true),
        TaskSummaryUiState(id = 2L, name = "Shower", isDone = false),
        TaskSummaryUiState(id = 3L, name = "Eat breakfast", isDone = false),
    )

    DiswantinTheme {
        Surface {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = ScreenLg)
                    .fillMaxWidth()
                    .padding(vertical = SpaceMd),
            ) {
                items(taskItems, key = TaskSummaryUiState::id) { task ->
                    TaskSummaryItem(task = task, onSelectTask = {})
                    HorizontalDivider()
                }
            }
        }
    }
}
