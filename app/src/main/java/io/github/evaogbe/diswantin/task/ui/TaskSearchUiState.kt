package io.github.evaogbe.diswantin.task.ui

import io.github.evaogbe.diswantin.task.data.Task
import kotlinx.collections.immutable.ImmutableList

sealed interface TaskSearchUiState {
    data object Initial : TaskSearchUiState

    data object Pending : TaskSearchUiState

    data object Failure : TaskSearchUiState

    data class Success(val searchResults: ImmutableList<Task>) : TaskSearchUiState
}
