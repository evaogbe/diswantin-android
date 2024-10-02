package io.github.evaogbe.diswantin.task.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.LocalDate

@RunWith(Parameterized::class)
class DoesRecurOnDateTest(
    private val recurrences: List<TaskRecurrence>,
    private val date: LocalDate,
    private val expected: Boolean,
) {
    companion object {
        @JvmStatic
        @get:Parameterized.Parameters
        val data = listOf(
            arrayOf(
                listOf(
                    TaskRecurrence(
                        taskId = 0,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 2,
                        week = 4,
                    ),
                ),
                LocalDate.parse("2024-08-24"),
                true,
            ),
            arrayOf(
                listOf(
                    TaskRecurrence(
                        taskId = 0,
                        start = LocalDate.parse("2024-08-23"),
                        type = RecurrenceType.Day,
                        step = 2,
                        week = 4,
                    ),
                ),
                LocalDate.parse("2024-08-24"),
                false,
            ),
            arrayOf(
                listOf(
                    TaskRecurrence(
                        taskId = 0,
                        start = LocalDate.parse("2024-08-16"),
                        type = RecurrenceType.Week,
                        step = 1,
                        week = 3,
                    ),
                    TaskRecurrence(
                        taskId = 0,
                        start = LocalDate.parse("2024-08-17"),
                        type = RecurrenceType.Week,
                        step = 1,
                        week = 3,
                    ),
                ),
                LocalDate.parse("2024-08-24"),
                true,
            ),
            arrayOf(
                listOf(
                    TaskRecurrence(
                        taskId = 0,
                        start = LocalDate.parse("2024-08-15"),
                        type = RecurrenceType.Week,
                        step = 1,
                        week = 3,
                    ),
                    TaskRecurrence(
                        taskId = 0,
                        start = LocalDate.parse("2024-08-16"),
                        type = RecurrenceType.Week,
                        step = 1,
                        week = 3,
                    ),
                ),
                LocalDate.parse("2024-08-24"),
                false,
            ),
            arrayOf(
                listOf(
                    TaskRecurrence(
                        taskId = 0,
                        start = LocalDate.parse("2024-07-24"),
                        type = RecurrenceType.DayOfMonth,
                        step = 1,
                        week = 4,
                    ),
                ),
                LocalDate.parse("2024-08-24"),
                true,
            ),
            arrayOf(
                listOf(
                    TaskRecurrence(
                        taskId = 0,
                        start = LocalDate.parse("2024-07-27"),
                        type = RecurrenceType.DayOfMonth,
                        step = 1,
                        week = 4,
                    ),
                ),
                LocalDate.parse("2024-08-24"),
                false,
            ),
            arrayOf(
                listOf(
                    TaskRecurrence(
                        taskId = 0,
                        start = LocalDate.parse("2024-03-31"),
                        type = RecurrenceType.DayOfMonth,
                        step = 1,
                        week = 6,
                    ),
                ),
                LocalDate.parse("2024-06-30"),
                true,
            ),
            arrayOf(
                listOf(
                    TaskRecurrence(
                        taskId = 0,
                        start = LocalDate.parse("2024-03-31"),
                        type = RecurrenceType.DayOfMonth,
                        step = 1,
                        week = 6,
                    ),
                ),
                LocalDate.parse("2024-05-30"),
                false,
            ),
            arrayOf(
                listOf(
                    TaskRecurrence(
                        taskId = 0,
                        start = LocalDate.parse("2024-06-30"),
                        type = RecurrenceType.DayOfMonth,
                        step = 1,
                        week = 6,
                    ),
                ),
                LocalDate.parse("2024-07-31"),
                false,
            ),
            arrayOf(
                listOf(
                    TaskRecurrence(
                        taskId = 0,
                        start = LocalDate.parse("2024-07-27"),
                        type = RecurrenceType.WeekOfMonth,
                        step = 1,
                        week = 4,
                    ),
                ),
                LocalDate.parse("2024-08-24"),
                true,
            ),
            arrayOf(
                listOf(
                    TaskRecurrence(
                        taskId = 0,
                        start = LocalDate.parse("2024-07-24"),
                        type = RecurrenceType.WeekOfMonth,
                        step = 1,
                        week = 4,
                    ),
                ),
                LocalDate.parse("2024-08-24"),
                false,
            ),
            arrayOf(
                listOf(
                    TaskRecurrence(
                        taskId = 0,
                        start = LocalDate.parse("2023-08-24"),
                        type = RecurrenceType.Year,
                        step = 1,
                        week = 4,
                    ),
                ),
                LocalDate.parse("2024-08-24"),
                true,
            ),
            arrayOf(
                listOf(
                    TaskRecurrence(
                        taskId = 0,
                        start = LocalDate.parse("2023-07-24"),
                        type = RecurrenceType.Year,
                        step = 1,
                        week = 5,
                    ),
                ),
                LocalDate.parse("2024-08-24"),
                false,
            ),
            arrayOf(
                listOf(
                    TaskRecurrence(
                        taskId = 0,
                        start = LocalDate.parse("2020-02-29"),
                        type = RecurrenceType.Year,
                        step = 1,
                        week = 5,
                    ),
                ),
                LocalDate.parse("2023-02-28"),
                true,
            ),
            arrayOf(
                listOf(
                    TaskRecurrence(
                        taskId = 0,
                        start = LocalDate.parse("2020-02-29"),
                        type = RecurrenceType.Year,
                        step = 1,
                        week = 5,
                    ),
                ),
                LocalDate.parse("2024-02-28"),
                false,
            ),
        )
    }

    @Test
    fun `returns whether any of the recurrences occur on the date`() {
        assertThat(doesRecurOnDate(recurrences, date)).isEqualTo(expected)
    }
}
