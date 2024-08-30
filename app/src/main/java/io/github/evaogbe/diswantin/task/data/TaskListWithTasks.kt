package io.github.evaogbe.diswantin.task.data

import androidx.room.Embedded
import androidx.room.Relation

data class TaskListWithTasks(
    @Embedded val taskList: TaskList,
    @Relation(
        parentColumn = "id",
        entityColumn = "list_id"
    ) val tasks: List<Task>,
)
