package io.github.evaogbe.diswantin.task.ui

import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.task.data.TaskDetail
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

sealed interface TaskDetailUiState {
    data object Pending : TaskDetailUiState

    data object Failure : TaskDetailUiState

    data class Success(
        val task: TaskDetail,
        @StringRes val userMessage: Int?,
        private val clock: Clock,
    ) : TaskDetailUiState {
        val formattedDeadline = task.deadline?.let(::formatDateTime)

        val formattedScheduledAt = task.scheduledAt?.let(::formatDateTime)

        val isDone = if (task.recurring) {
            task.doneAt?.let {
                it < ZonedDateTime.now(clock).truncatedTo(ChronoUnit.DAYS).toInstant()
            } == false
        } else {
            task.doneAt != null
        }

        private fun formatDateTime(dateTime: Instant) =
            dateTime.atZone(clock.zone)
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT))
    }

    data object Deleted : TaskDetailUiState
}
