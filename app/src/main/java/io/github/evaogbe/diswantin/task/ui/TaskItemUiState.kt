package io.github.evaogbe.diswantin.task.ui

import io.github.evaogbe.diswantin.task.data.TaskItem
import java.time.Instant

data class TaskItemUiState(val id: Long, val name: String, val isDone: Boolean) {
    companion object {
        fun fromTaskItem(task: TaskItem, doneBefore: Instant) =
            TaskItemUiState(
                id = task.id,
                name = task.name,
                isDone = if (task.recurring) {
                    task.doneAt?.let { it < doneBefore } == false
                } else {
                    task.doneAt != null
                },
            )
    }
}
