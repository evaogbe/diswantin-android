package io.github.evaogbe.diswantin.task.ui

import android.os.Parcelable
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.Parcelize

@Parcelize
data class TaskCategoryFormTopBarState(
    val isNew: Boolean,
    val showSave: Boolean,
    val saveEnabled: Boolean,
) : Parcelable

enum class TaskCategoryFormTopBarAction {
    Save, Close
}

sealed interface TaskCategoryFormUiState {
    data object Pending : TaskCategoryFormUiState

    data class Failure(val exception: Throwable) : TaskCategoryFormUiState

    data class Success(
        val name: String,
        val newTasks: ImmutableList<Task>,
        val isEditing: Boolean,
        val taskOptions: ImmutableList<Task>,
        val changed: Boolean,
        val userMessage: UserMessage?,
    ) : TaskCategoryFormUiState

    data object Saved : TaskCategoryFormUiState
}
