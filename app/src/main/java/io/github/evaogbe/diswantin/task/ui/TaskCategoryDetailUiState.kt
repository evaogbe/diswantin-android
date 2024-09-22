package io.github.evaogbe.diswantin.task.ui

import android.os.Parcelable
import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.task.data.TaskCategory
import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.Parcelize

@Parcelize
data class TaskCategoryDetailTopBarState(val categoryId: Long?) : Parcelable

enum class TaskCategoryDetailTopBarAction {
    Delete
}

sealed interface TaskCategoryDetailUiState {
    data object Pending : TaskCategoryDetailUiState

    data object Failure : TaskCategoryDetailUiState

    data class Success(
        val category: TaskCategory,
        val tasks: ImmutableList<TaskItemUiState>,
        @StringRes val userMessage: Int?
    ) : TaskCategoryDetailUiState

    data object Deleted : TaskCategoryDetailUiState
}
