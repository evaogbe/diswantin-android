package io.github.evaogbe.diswantin.task.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import java.time.Instant

data class TaskPriority(
    @Embedded val task: Task,
    @ColumnInfo("scheduled_at_priority") val scheduledAtPriority: Instant?,
    @ColumnInfo("deadline_priority") val deadlinePriority: Instant?,
    @ColumnInfo("recurring_priority") val recurringPriority: Boolean,
    @ColumnInfo("created_at_priority") val createdAtPriority: Instant,
    @ColumnInfo("id_priority") val idPriority: Long,
)
