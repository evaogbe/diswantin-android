package io.github.evaogbe.diswantin.task.ui

import io.github.evaogbe.diswantin.task.data.Task
import kotlinx.collections.immutable.PersistentList

sealed interface TaskSearchUiState {
    data object Initial : TaskSearchUiState

    data object Pending : TaskSearchUiState

    data object Failure : TaskSearchUiState

    data class Success(val searchResults: PersistentList<Task>) : TaskSearchUiState
}
