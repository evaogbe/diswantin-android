package io.github.evaogbe.diswantin.task.ui

import android.os.Parcelable
import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.data.Result
import io.github.evaogbe.diswantin.task.data.Task
import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.Parcelize

@Parcelize
data class CurrentTaskTopBarState(val taskId: Long?) : Parcelable

sealed interface CurrentTaskUiState {
    data object Pending : CurrentTaskUiState

    data object Failure : CurrentTaskUiState

    data object Empty : CurrentTaskUiState

    data class Present(
        val currentTask: Task,
        val canSkip: Boolean,
        val parentTaskOptions: Result<ImmutableList<Task>>,
        @StringRes val userMessage: Int?,
    ) : CurrentTaskUiState
}
