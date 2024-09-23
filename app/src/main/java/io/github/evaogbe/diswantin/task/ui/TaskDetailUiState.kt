package io.github.evaogbe.diswantin.task.ui

import android.os.Parcelable
import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.task.data.TaskDetail
import kotlinx.parcelize.Parcelize
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Parcelize
data class TaskDetailTopBarState(val taskId: Long?, val isDone: Boolean) : Parcelable

enum class TaskDetailTopBarAction {
    MarkDone, UnmarkDone, Delete
}

sealed interface TaskDetailUiState {
    data object Pending : TaskDetailUiState

    data object Failure : TaskDetailUiState

    data class Success(
        val task: TaskDetail,
        val recurrence: TaskRecurrenceUiState?,
        @StringRes val userMessage: Int?,
        private val clock: Clock,
    ) : TaskDetailUiState {
        val formattedDeadline = formatDateTime(task.deadlineDate, task.deadlineTime)

        val formattedScheduledAt = formatDateTime(task.scheduledDate, task.scheduledTime)

        val isDone = if (recurrence == null) {
            task.doneAt != null
        } else {
            task.doneAt?.let {
                it < ZonedDateTime.now(clock).with(LocalTime.MIN).toInstant()
            } == false
        }

        private fun formatDateTime(date: LocalDate?, time: LocalTime?) = when {
            date != null && time != null -> {
                date.atTime(time).format(
                    DateTimeFormatter.ofLocalizedDateTime(
                        FormatStyle.FULL,
                        FormatStyle.SHORT,
                    )
                )
            }

            date != null -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
            time != null -> time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
            else -> null
        }
    }

    data object Deleted : TaskDetailUiState
}
