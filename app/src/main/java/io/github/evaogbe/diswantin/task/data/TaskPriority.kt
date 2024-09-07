package io.github.evaogbe.diswantin.task.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class TaskPriority(
    @Embedded val task: Task,
    @ColumnInfo("scheduled_date_priority") val scheduledDatePriority: LocalDate?,
    @ColumnInfo("scheduled_time_priority") val scheduledTimePriority: LocalTime?,
    @ColumnInfo("deadline_date_priority") val deadlineDatePriority: LocalDate?,
    @ColumnInfo("deadline_time_priority") val deadlineTimePriority: LocalTime?,
    @ColumnInfo("recurring_priority") val recurringPriority: Boolean,
    @ColumnInfo("created_at_priority") val createdAtPriority: Instant,
    @ColumnInfo("id_priority") val idPriority: Long,
)
