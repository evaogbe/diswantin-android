package io.github.evaogbe.diswantin.task.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Relation
import java.time.Instant

data class TaskSummary(
    val id: Long,
    val name: String,
    val recurring: Boolean,
    @ColumnInfo("done_at") val doneAt: Instant?,
)

data class TaskSummaryWithRecurrences(
    @Embedded val task: TaskSummary,
    @Relation(parentColumn = "id", entityColumn = "task_id") val recurrences: List<TaskRecurrence>,
)
