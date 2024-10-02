package io.github.evaogbe.diswantin.task.data

import androidx.room.Embedded
import androidx.room.Relation

data class TaskItemWithRecurrences(
    @Embedded val task: TaskItem,
    @Relation(parentColumn = "id", entityColumn = "task_id") val recurrences: List<TaskRecurrence>,
)
