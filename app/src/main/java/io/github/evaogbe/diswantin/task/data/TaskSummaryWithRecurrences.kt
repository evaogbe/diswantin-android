package io.github.evaogbe.diswantin.task.data

import androidx.room.Embedded
import androidx.room.Relation

data class TaskSummaryWithRecurrences(
    @Embedded val task: TaskSummary,
    @Relation(parentColumn = "id", entityColumn = "task_id") val recurrences: List<TaskRecurrence>,
)
