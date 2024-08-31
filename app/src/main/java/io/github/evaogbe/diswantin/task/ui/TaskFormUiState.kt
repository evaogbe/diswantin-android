package io.github.evaogbe.diswantin.task.ui

import java.time.ZonedDateTime

sealed interface TaskFormUiState {
    data object Pending : TaskFormUiState

    data object Failure : TaskFormUiState

    data class Success(
        val deadlineInput: ZonedDateTime?,
        val scheduledAtInput: ZonedDateTime?,
        val recurringInput: Boolean,
        val hasSaveError: Boolean,
    ) : TaskFormUiState

    data object Saved : TaskFormUiState
}
