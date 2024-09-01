package io.github.evaogbe.diswantin.task.ui

import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.task.data.TaskList
import kotlinx.collections.immutable.ImmutableList

data class TaskItemState(
    val id: Long,
    val name: String,
    val recurring: Boolean,
    val isDone: Boolean,
)

sealed interface TaskListDetailUiState {
    data object Pending : TaskListDetailUiState

    data object Failure : TaskListDetailUiState

    data class Success(
        val taskList: TaskList,
        val tasks: ImmutableList<TaskItemState>,
        @StringRes val userMessage: Int?
    ) : TaskListDetailUiState

    data object Deleted : TaskListDetailUiState
}
