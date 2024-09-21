package io.github.evaogbe.diswantin.task.ui

import io.github.evaogbe.diswantin.data.weekOfMonthField
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

enum class TaskRecurrenceFormTopBarAction {
    Confirm
}

data class TaskRecurrenceUiState(
    val start: LocalDate,
    val type: RecurrenceType,
    val step: Int,
    val weekdays: PersistentSet<DayOfWeek>,
    private val locale: Locale,
) {
    val startWeek = start.get(weekOfMonthField(locale))

    val startWeekdayText = start.dayOfWeek.getDisplayName(TextStyle.FULL, locale)

    val weekdaysText: String

    init {
        val textStyle = if (weekdays.size > 1) TextStyle.SHORT else TextStyle.FULL
        weekdaysText = weekdays.sorted()
            .partition { it == WeekFields.of(locale).firstDayOfWeek }
            .let { it.first + it.second }
            .joinToString { it.getDisplayName(textStyle, locale) }
    }

    companion object {
        fun tryFromEntities(recurrences: List<TaskRecurrence>, locale: Locale) =
            recurrences.firstOrNull()?.let { recurrence ->
                TaskRecurrenceUiState(
                    start = recurrence.start,
                    type = recurrence.type,
                    step = recurrence.step,
                    weekdays = if (recurrence.type == RecurrenceType.Week) {
                        recurrences.map { it.start.dayOfWeek }.toPersistentSet()
                    } else {
                        persistentSetOf()
                    },
                    locale = locale,
                )
            }
    }
}
