package io.github.evaogbe.diswantin.task.ui

import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.task.data.TaskDetail
import java.time.Clock
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

sealed interface TaskDetailUiState {
    data object Pending : TaskDetailUiState

    data object Failure : TaskDetailUiState

    data class Success(
        val task: TaskDetail,
        @StringRes val userMessage: Int?,
        private val clock: Clock,
    ) : TaskDetailUiState {
        val formattedDeadline = when {
            task.deadlineDate != null && task.deadlineTime != null -> {
                task.deadlineDate
                    .atTime(task.deadlineTime)
                    .format(
                        DateTimeFormatter.ofLocalizedDateTime(
                            FormatStyle.FULL,
                            FormatStyle.SHORT
                        )
                    )
            }

            task.deadlineDate != null -> {
                task.deadlineDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
            }

            task.deadlineTime != null -> {
                task.deadlineTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
            }

            else -> null
        }

        val formattedScheduledAt =
            task.scheduledAt
                ?.atZone(clock.zone)
                ?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT))

        val isDone = if (task.recurring) {
            task.doneAt?.let {
                it < ZonedDateTime.now(clock).with(LocalTime.MIN).toInstant()
            } == false
        } else {
            task.doneAt != null
        }
    }

    data object Deleted : TaskDetailUiState
}
