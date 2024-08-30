package io.github.evaogbe.diswantin.task.ui

import io.github.evaogbe.diswantin.task.data.Task
import kotlinx.collections.immutable.ImmutableList

sealed interface TaskListFormUiState {
    data class Editing(
        val tasks: ImmutableList<Task>,
        val editingTaskIndex: Int?,
        val taskOptions: ImmutableList<Task>,
        val hasSaveError: Boolean,
    ) : TaskListFormUiState

    data object Saved : TaskListFormUiState
}
