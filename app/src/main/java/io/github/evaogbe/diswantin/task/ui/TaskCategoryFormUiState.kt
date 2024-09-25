package io.github.evaogbe.diswantin.task.ui

import android.os.Parcelable
import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.task.data.Task
import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.Parcelize

@Parcelize
data class TaskCategoryFormTopBarState(
    val isNew: Boolean,
    val showSave: Boolean,
    val saveEnabled: Boolean,
) : Parcelable

enum class TaskCategoryFormTopBarAction {
    Save
}

sealed interface TaskCategoryFormUiState {
    data object Pending : TaskCategoryFormUiState

    data class Failure(val exception: Throwable) : TaskCategoryFormUiState

    data class Success(
        val tasks: ImmutableList<Task>,
        val editingTaskIndex: Int?,
        val taskOptions: ImmutableList<Task>,
        val hasSaveError: Boolean,
        @StringRes val userMessage: Int?,
    ) : TaskCategoryFormUiState

    data object Saved : TaskCategoryFormUiState
}
