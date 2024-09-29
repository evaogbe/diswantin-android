package io.github.evaogbe.diswantin.task.ui

import android.os.Parcelable
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.Parcelize

@Parcelize
data class TaskCategoryDetailTopBarState(val categoryId: Long?) : Parcelable

enum class TaskCategoryDetailTopBarAction {
    Delete
}

sealed interface TaskCategoryDetailUiState {
    data object Pending : TaskCategoryDetailUiState

    data class Failure(val exception: Throwable) : TaskCategoryDetailUiState

    data class Success(
        val category: TaskCategory,
        val tasks: ImmutableList<TaskItemUiState>,
        val userMessage: UserMessage?,
    ) : TaskCategoryDetailUiState

    data object Deleted : TaskCategoryDetailUiState
}
