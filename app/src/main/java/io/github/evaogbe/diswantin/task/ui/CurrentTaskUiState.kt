package io.github.evaogbe.diswantin.task.ui

import android.os.Parcelable
import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.task.data.Task
import kotlinx.parcelize.Parcelize

@Parcelize
data class CurrentTaskTopBarState(val taskId: Long?) : Parcelable

sealed interface CurrentTaskUiState {
    data object Pending : CurrentTaskUiState

    data object Failure : CurrentTaskUiState

    data object Empty : CurrentTaskUiState

    data class Present(val currentTask: Task, @StringRes val userMessage: Int?) : CurrentTaskUiState
}
