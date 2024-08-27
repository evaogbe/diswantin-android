package io.github.evaogbe.diswantin.task.ui

import io.github.evaogbe.diswantin.task.data.Task
import java.time.ZonedDateTime

sealed interface TaskFormUiState {
    data object Pending : TaskFormUiState

    data object Failure : TaskFormUiState

    data class Success(
        val dueAtInput: ZonedDateTime?,
        val scheduledAtInput: ZonedDateTime?,
        val canUpdatePrevTask: Boolean,
        val prevTask: Task?,
        val prevTaskOptions: List<Task>,
        val hasSaveError: Boolean,
    ) : TaskFormUiState

    data object Saved : TaskFormUiState
}
