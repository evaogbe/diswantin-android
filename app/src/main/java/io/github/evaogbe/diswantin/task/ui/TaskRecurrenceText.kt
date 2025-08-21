package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

@Composable
@ReadOnlyComposable
fun taskRecurrenceText(recurrence: TaskRecurrenceUiState): String {
    return when (recurrence.type) {
        RecurrenceType.Day -> {
            pluralStringResource(R.plurals.recurrence_daily, recurrence.step, recurrence.step)
        }

        RecurrenceType.Week -> {
            pluralStringResource(
                R.plurals.recurrence_weekly,
                recurrence.step,
                recurrence.step,
                recurrence.weekdaysText,
            )
        }

        RecurrenceType.DayOfMonth -> {
            pluralStringResource(
                R.plurals.recurrence_monthly_on_day,
                recurrence.step,
                recurrence.start.dayOfMonth.ordinal,
                recurrence.step,
            )
        }

        RecurrenceType.WeekOfMonth -> {
            val week = recurrence.startWeek
            pluralStringResource(
                when (week) {
                    1 -> R.plurals.recurrence_monthly_on_week_1
                    2 -> R.plurals.recurrence_monthly_on_week_2
                    3 -> R.plurals.recurrence_monthly_on_week_3
                    4 -> R.plurals.recurrence_monthly_on_week_4
                    5 -> R.plurals.recurrence_monthly_on_week_5
                    6 -> R.plurals.recurrence_monthly_on_week_6
                    else -> {
                        throw IllegalArgumentException(
                            """Start week must be between 1 and 6, but got 
                                |start: ${recurrence.start}, 
                                |week: $week""".trimMargin()
                        )
                    }
                },
                recurrence.step,
                recurrence.step,
                recurrence.startWeekdayText,
            )
        }

        RecurrenceType.Year -> {
            pluralStringResource(R.plurals.recurrence_yearly, recurrence.step, recurrence.step)
        }
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
            start = now,
            type = RecurrenceType.Day,
            step = 1,
            weekdays = persistentSetOf(),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            start = now,
            type = RecurrenceType.Day,
            step = 2,
            weekdays = persistentSetOf(),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            start = now,
            type = RecurrenceType.Week,
            step = 1,
            weekdays = persistentSetOf(now.dayOfWeek),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            start = now,
            type = RecurrenceType.Week,
            step = 2,
            weekdays = persistentSetOf(now.dayOfWeek),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            start = now,
            type = RecurrenceType.Week,
            step = 1,
            weekdays = DayOfWeek.entries.toPersistentSet(),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            start = now,
            type = RecurrenceType.DayOfMonth,
            step = 1,
            weekdays = persistentSetOf(),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            start = now,
            type = RecurrenceType.DayOfMonth,
            step = 2,
            weekdays = persistentSetOf(),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            start = now,
            type = RecurrenceType.WeekOfMonth,
            step = 1,
            weekdays = persistentSetOf(),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            start = now,
            type = RecurrenceType.WeekOfMonth,
            step = 2,
            weekdays = persistentSetOf(),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            start = now,
            type = RecurrenceType.Year,
            step = 1,
            weekdays = persistentSetOf(),
            locale = locale,
        ),
        TaskRecurrenceUiState(
            start = now,
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
