package io.github.evaogbe.diswantin.task.ui

import kotlinx.collections.immutable.ImmutableList

enum class TaskSearchTopBarAction {
    Search
}

sealed interface TaskSearchUiState {
    data object Initial : TaskSearchUiState

    data class Failure(val exception: Throwable) : TaskSearchUiState

    data class Success(val searchResults: ImmutableList<TaskItemUiState>) : TaskSearchUiState
}
