package io.github.evaogbe.diswantin.task.ui

import io.github.evaogbe.diswantin.task.data.TaskCategory
import kotlinx.collections.immutable.ImmutableList

sealed interface TaskCategoryListUiState {
    data object Pending : TaskCategoryListUiState

    data object Failure : TaskCategoryListUiState

    data class Success(val categories: ImmutableList<TaskCategory>) : TaskCategoryListUiState
}
