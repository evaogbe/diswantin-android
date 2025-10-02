package io.github.evaogbe.diswantin.task.data

import androidx.room.Embedded
import androidx.room.Relation

data class DueTask(val id: Long, val name: String)

data class DueTaskWithRecurrences(
    @Embedded val task: DueTask,
    @Relation(parentColumn = "id", entityColumn = "task_id") val recurrences: List<TaskRecurrence>,
)
