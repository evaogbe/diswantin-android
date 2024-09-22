package io.github.evaogbe.diswantin.task.ui

import android.os.Parcelable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.Parcelize

@Parcelize
data class TaskSearchTopBarState(val query: String) : Parcelable

enum class TaskSearchTopBarAction {
    Search
}

sealed interface TaskSearchUiState {
    data object Initial : TaskSearchUiState

    data object Failure : TaskSearchUiState

    data class Success(val searchResults: ImmutableList<TaskItemUiState>) : TaskSearchUiState
}
