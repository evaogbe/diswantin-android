package io.github.evaogbe.diswantin.task.ui

import android.content.res.Resources
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.tooling.preview.Preview
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
@ReadOnlyComposable
fun taskRecurrenceText(recurrence: TaskRecurrenceUiState): String {
    val resources = LocalResources.current
    return resources.getTaskRecurrenceText(recurrence)
}

fun Resources.getTaskRecurrenceText(recurrence: TaskRecurrenceUiState): String {
    val startText = when (recurrence.type) {
        RecurrenceType.Day -> {
            getQuantityString(R.plurals.recurrence_daily, recurrence.step, recurrence.step)
        }

        RecurrenceType.Week -> {
            getQuantityString(
                R.plurals.recurrence_weekly,
                recurrence.step,
                recurrence.step,
                recurrence.weekdaysText,
            )
        }

        RecurrenceType.DayOfMonth -> {
            getQuantityString(
                R.plurals.recurrence_monthly_on_day,
                recurrence.step,
                recurrence.step,
                recurrence.startDate.dayOfMonth.ordinal,
            )
        }

        RecurrenceType.WeekOfMonth -> {
            getQuantityString(
                R.plurals.recurrence_monthly_on_week,
                recurrence.step,
                recurrence.step,
                recurrence.startWeek.ordinal,
                recurrence.startWeekdayText,
            )
        }

        RecurrenceType.Year -> {
            getQuantityString(R.plurals.recurrence_yearly, recurrence.step, recurrence.step)
        }
    }
    return if (recurrence.endDate == null) {
        startText
    } else {
        val endText = getString(
            R.string.recurrence_end,
            recurrence.endDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)),
        )
        "$startText; $endText"
    }
}

private val Int.ordinal: String
    get() {
        val suffix = when (if (this > 13) this % 10 else this) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
        return "$this$suffix"
    }

@Preview(showBackground = true)
@Composable
private fun TaskRecurrenceTextPreview() {
    val now = LocalDate.now()
    val locale = Locale.getDefault()
    val recurrences = listOf(
        TaskRecurrenceUiState(
            startDate = now,
            endDate = null,
            type = RecurrenceType.Day,
            step = 1,
            weekdays = persistentSetOf(),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            startDate = now,
            endDate = null,
            type = RecurrenceType.Day,
            step = 2,
            weekdays = persistentSetOf(),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            startDate = now,
            endDate = now.plusYears(1),
            type = RecurrenceType.Day,
            step = 1,
            weekdays = persistentSetOf(),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            startDate = now,
            endDate = null,
            type = RecurrenceType.Week,
            step = 1,
            weekdays = persistentSetOf(now.dayOfWeek),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            startDate = now,
            endDate = null,
            type = RecurrenceType.Week,
            step = 2,
            weekdays = persistentSetOf(now.dayOfWeek),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            startDate = now,
            endDate = null,
            type = RecurrenceType.Week,
            step = 1,
            weekdays = DayOfWeek.entries.toPersistentSet(),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            startDate = now,
            endDate = null,
            type = RecurrenceType.DayOfMonth,
            step = 1,
            weekdays = persistentSetOf(),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            startDate = now,
            endDate = null,
            type = RecurrenceType.DayOfMonth,
            step = 2,
            weekdays = persistentSetOf(),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            startDate = now,
            endDate = null,
            type = RecurrenceType.WeekOfMonth,
            step = 1,
            weekdays = persistentSetOf(),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            startDate = now,
            endDate = null,
            type = RecurrenceType.WeekOfMonth,
            step = 2,
            weekdays = persistentSetOf(),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            startDate = now,
            endDate = null,
            type = RecurrenceType.Year,
            step = 1,
            weekdays = persistentSetOf(),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            startDate = now,
            endDate = null,
            type = RecurrenceType.Year,
            step = 2,
            weekdays = persistentSetOf(),
            locale = locale,
        ),
    )

    DiswantinTheme {
        Column {
            recurrences.forEach {
                ListItem(headlineContent = { Text(text = taskRecurrenceText(it)) })
                HorizontalDivider()
            }
        }
    }
}
