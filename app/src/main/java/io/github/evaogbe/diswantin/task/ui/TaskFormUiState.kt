package io.github.evaogbe.diswantin.task.ui

import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

sealed interface TaskFormUiState {
    data object Pending : TaskFormUiState

    data object Failure : TaskFormUiState

    data class Success(
        val deadlineDateInput: LocalDate?,
        val deadlineTimeInput: LocalTime?,
        val scheduledAtInput: ZonedDateTime?,
        val recurringInput: Boolean,
        val hasSaveError: Boolean,
        private val clock: Clock
    ) : TaskFormUiState {
        val deadlineDateAsZonedDateTime = deadlineDateInput?.atStartOfDay(clock.zone)
    }

    data object Saved : TaskFormUiState
}
