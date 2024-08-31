package io.github.evaogbe.diswantin.task.ui

import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskList
import kotlinx.collections.immutable.ImmutableList

sealed interface TaskListDetailUiState {
    data object Pending : TaskListDetailUiState

    data object Failure : TaskListDetailUiState

    data class Success(
        val taskList: TaskList,
        val tasks: ImmutableList<Task>,
        @StringRes val userMessage: Int?
    ) : TaskListDetailUiState

    data object Deleted : TaskListDetailUiState
}
