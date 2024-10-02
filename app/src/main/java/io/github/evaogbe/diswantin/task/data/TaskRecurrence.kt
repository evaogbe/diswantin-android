package io.github.evaogbe.diswantin.task.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.evaogbe.diswantin.data.weekOfMonthField
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

enum class RecurrenceType {
    Day, Week, DayOfMonth, WeekOfMonth, Year
}

@Entity(
    tableName = "task_recurrence",
    indices = [Index("task_id", "start", unique = true)],
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
    val start: LocalDate,
    val type: RecurrenceType,
    val step: Int,
    val week: Int,
)

fun doesRecurOnDate(recurrences: List<TaskRecurrence>, date: LocalDate): Boolean {
    val recurrence = recurrences.first()
    return when (recurrence.type) {
        RecurrenceType.Day -> {
            ChronoUnit.DAYS.between(recurrence.start, date) % recurrence.step == 0L
        }

        RecurrenceType.Week -> {
            recurrences.any {
                (ChronoUnit.WEEKS.between(it.start, date) % it.step == 0L) &&
                        it.start.dayOfWeek == date.dayOfWeek
            }
        }

        RecurrenceType.DayOfMonth -> {
            (ChronoUnit.MONTHS.between(recurrence.start, date) % recurrence.step == 0L) &&
                    (recurrence.start.dayOfMonth == date.dayOfMonth ||
                            (recurrence.start.dayOfMonth > date.dayOfMonth &&
                                    recurrence.start.dayOfMonth ==
                                    recurrence.start.lengthOfMonth() &&
                                    date.dayOfMonth == date.lengthOfMonth()))
        }

        RecurrenceType.WeekOfMonth -> {
            (ChronoUnit.MONTHS.between(recurrence.start, date) % recurrence.step == 0L) &&
                    recurrence.start.dayOfWeek == date.dayOfWeek &&
                    recurrence.week == date.get(weekOfMonthField())
        }

        RecurrenceType.Year -> {
            (ChronoUnit.YEARS.between(recurrence.start, date) % recurrence.step == 0L) &&
                    recurrence.start.month == date.month &&
                    (recurrence.start.dayOfMonth == date.dayOfMonth ||
                            (recurrence.start.month == Month.FEBRUARY &&
                                    recurrence.start.dayOfMonth == 29 &&
                                    date.dayOfMonth == 28 &&
                                    !date.isLeapYear))
        }
    }
}
