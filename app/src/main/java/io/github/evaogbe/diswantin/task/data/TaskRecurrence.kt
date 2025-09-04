package io.github.evaogbe.diswantin.task.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

enum class RecurrenceType {
    Day, Week, DayOfMonth, WeekOfMonth, Year
}

@Entity(
    tableName = "task_recurrence",
    indices = [Index("task_id", "start_date", unique = true)],
    foreignKeys = [ForeignKey(
        entity = Task::class,
        parentColumns = ["id"],
        childColumns = ["task_id"],
        onDelete = ForeignKey.Companion.CASCADE,
        onUpdate = ForeignKey.Companion.CASCADE,
    )]
)
data class TaskRecurrence(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo("task_id") val taskId: Long,
    @ColumnInfo("start_date") val startDate: LocalDate,
    val type: RecurrenceType,
    val step: Int,
    @ColumnInfo("end_date") val endDate: LocalDate? = null,
)

fun doesRecurOnDate(recurrences: List<TaskRecurrence>, date: LocalDate): Boolean {
    return recurrences.any { recurrence ->
        if (recurrence.startDate > date) return false
        if (recurrence.endDate?.let { it < date } == true) return false

        when (recurrence.type) {
            RecurrenceType.Day -> {
                ChronoUnit.DAYS.between(recurrence.startDate, date) % recurrence.step == 0L
            }

            RecurrenceType.Week -> {
                if (ChronoUnit.WEEKS.between(
                        recurrence.startDate, date
                    ) % recurrence.step != 0L
                ) {
                    return false
                }
                recurrence.startDate.dayOfWeek == date.dayOfWeek
            }

            RecurrenceType.DayOfMonth -> {
                if (ChronoUnit.MONTHS.between(
                        recurrence.startDate, date
                    ) % recurrence.step != 0L
                ) {
                    return false
                }
                if (recurrence.startDate.dayOfMonth == date.dayOfMonth) {
                    return true
                }
                if (recurrence.startDate.dayOfMonth != recurrence.startDate.lengthOfMonth()) {
                    return false
                }
                if (date.dayOfMonth != date.lengthOfMonth()) {
                    return false
                }
                recurrence.startDate.dayOfMonth > date.dayOfMonth
            }

            RecurrenceType.WeekOfMonth -> {
                if (ChronoUnit.MONTHS.between(
                        YearMonth.from(recurrence.startDate),
                        YearMonth.from(date),
                    ) % recurrence.step != 0L
                ) {
                    return false
                }
                if (recurrence.startDate.dayOfWeek != date.dayOfWeek) {
                    return false
                }
                ceil(recurrence.startDate.dayOfMonth / 7.0) == ceil(date.dayOfMonth / 7.0)
            }

            RecurrenceType.Year -> {
                if (ChronoUnit.YEARS.between(
                        recurrence.startDate, date
                    ) % recurrence.step != 0L
                ) {
                    return false
                }
                if (recurrence.startDate.month != date.month) {
                    return false
                }
                if (recurrence.startDate.dayOfMonth == date.dayOfMonth) {
                    return true
                }
                if (recurrence.startDate.month != Month.FEBRUARY) {
                    return false
                }
                if (recurrence.startDate.dayOfMonth != 29) {
                    return false
                }
                date.dayOfMonth == 28 && !date.isLeapYear
            }
        }
    }
}
