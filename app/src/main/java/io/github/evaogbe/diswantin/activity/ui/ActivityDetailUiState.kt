package io.github.evaogbe.diswantin.activity.ui

import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.activity.data.Activity
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

sealed interface ActivityDetailUiState {
    data object Pending : ActivityDetailUiState

    data object Failure : ActivityDetailUiState

    data class Success(
        val activity: Activity,
        val activityChain: List<Activity>,
        @StringRes val userMessage: Int?,
        private val clock: Clock
    ) : ActivityDetailUiState {
        val formattedDueAt = activity.dueAt?.let(::formatDateTime)

        val formattedScheduledAt = activity.scheduledAt?.let(::formatDateTime)

        private fun formatDateTime(dateTime: Instant) =
            dateTime.atZone(clock.zone)
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT))
    }

    data object Removed : ActivityDetailUiState
}
