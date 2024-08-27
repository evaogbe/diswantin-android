package io.github.evaogbe.diswantin.task.ui

import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.task.data.Task

sealed interface CurrentTaskUiState {
    data object Pending : CurrentTaskUiState

    data object Failure : CurrentTaskUiState

    data object Empty : CurrentTaskUiState

    data class Present(val currentTask: Task, @StringRes val userMessage: Int?) : CurrentTaskUiState
}
