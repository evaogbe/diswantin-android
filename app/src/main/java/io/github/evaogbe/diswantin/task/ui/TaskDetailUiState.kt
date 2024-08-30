package io.github.evaogbe.diswantin.task.ui

import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.task.data.Task
import kotlinx.collections.immutable.ImmutableList
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

sealed interface TaskDetailUiState {
    data object Pending : TaskDetailUiState

    data object Failure : TaskDetailUiState

    data class Success(
        val task: Task,
        val taskListItems: ImmutableList<Task>,
        @StringRes val userMessage: Int?,
        private val clock: Clock
    ) : TaskDetailUiState {
        val formattedDeadline = task.deadline?.let(::formatDateTime)

        val formattedScheduledAt = task.scheduledAt?.let(::formatDateTime)

        private fun formatDateTime(dateTime: Instant) =
            dateTime.atZone(clock.zone)
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT))
    }

    data object Removed : TaskDetailUiState
}
