package io.github.evaogbe.diswantin.task.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

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
