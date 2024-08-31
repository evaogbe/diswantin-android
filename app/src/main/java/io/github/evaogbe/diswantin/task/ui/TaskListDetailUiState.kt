package io.github.evaogbe.diswantin.task.ui

import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskList
import kotlinx.collections.immutable.ImmutableList

sealed interface TaskListDetailUiState {
    data object Pending : TaskListDetailUiState

    data object Failure : TaskListDetailUiState

    data class Success(val taskList: TaskList, val tasks: ImmutableList<Task>) :
        TaskListDetailUiState
}
