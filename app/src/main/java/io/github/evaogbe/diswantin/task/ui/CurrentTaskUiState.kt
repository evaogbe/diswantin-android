package io.github.evaogbe.diswantin.task.ui

import android.os.Parcelable
import io.github.evaogbe.diswantin.task.data.Task
import kotlinx.parcelize.Parcelize

@Parcelize
data class CurrentTaskTopBarState(val canSkip: Boolean) : Parcelable

enum class CurrentTaskTopBarAction {
    Refresh, Skip
}

enum class CurrentTaskUserMessage {
    FetchRecurrencesError, SkipError, MarkDoneError
}

sealed interface CurrentTaskUiState {
    data object Pending : CurrentTaskUiState

    data class Failure(val exception: Throwable) : CurrentTaskUiState

    data class Empty(val isRefreshing: Boolean) : CurrentTaskUiState

    data class Present(val currentTask: Task, val isRefreshing: Boolean, val canSkip: Boolean) :
        CurrentTaskUiState
}
