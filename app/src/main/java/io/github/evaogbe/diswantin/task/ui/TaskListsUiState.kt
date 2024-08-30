package io.github.evaogbe.diswantin.task.ui

import io.github.evaogbe.diswantin.task.data.TaskList
import kotlinx.collections.immutable.ImmutableList

sealed interface TaskListsUiState {
    data object Pending : TaskListsUiState

    data object Failure : TaskListsUiState

    data class Success(val taskLists: ImmutableList<TaskList>) : TaskListsUiState
}
