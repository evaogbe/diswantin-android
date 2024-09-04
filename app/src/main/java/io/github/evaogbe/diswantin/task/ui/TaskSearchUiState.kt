package io.github.evaogbe.diswantin.task.ui

import android.os.Parcelable
import io.github.evaogbe.diswantin.task.data.Task
import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.Parcelize

@Parcelize
data class TaskSearchTopBarState(
    val query: String,
    val onQueryChange: (String) -> Unit,
    val onSearch: (String) -> Unit,
) : Parcelable

sealed interface TaskSearchUiState {
    data object Initial : TaskSearchUiState

    data object Failure : TaskSearchUiState

    data class Success(val searchResults: ImmutableList<Task>) : TaskSearchUiState
}
