package io.github.evaogbe.diswantin.task.ui

import android.os.Parcelable
import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.task.data.Task
import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.Parcelize

@Parcelize
data class TaskListFormTopBarState(
    val isNew: Boolean,
    val showSave: Boolean,
    val onSave: () -> Unit,
    val saveEnabled: Boolean,
) : Parcelable

sealed interface TaskListFormUiState {
    data object Pending : TaskListFormUiState

    data object Failure : TaskListFormUiState

    data class Success(
        val tasks: ImmutableList<Task>,
        val editingTaskIndex: Int?,
        val taskOptions: ImmutableList<Task>,
        val hasSaveError: Boolean,
        @StringRes val userMessage: Int?,
    ) : TaskListFormUiState

    data object Saved : TaskListFormUiState
}
