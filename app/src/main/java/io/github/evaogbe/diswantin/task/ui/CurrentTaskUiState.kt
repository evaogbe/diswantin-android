package io.github.evaogbe.diswantin.task.ui

import android.os.Parcelable
import io.github.evaogbe.diswantin.task.data.Task
import kotlinx.parcelize.Parcelize

@Parcelize
data class CurrentTaskTopBarState(val canSkip: Boolean) : Parcelable

enum class CurrentTaskTopBarAction {
    Skip
}

sealed interface CurrentTaskUiState {
    data object Pending : CurrentTaskUiState

    data class Failure(val exception: Throwable) : CurrentTaskUiState

    data object Empty : CurrentTaskUiState

    data class Present(val currentTask: Task, val canSkip: Boolean) : CurrentTaskUiState
}
