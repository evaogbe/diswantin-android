package io.github.evaogbe.diswantin.task.ui

import io.github.evaogbe.diswantin.task.data.NamedEntity
import io.github.evaogbe.diswantin.task.data.TaskSummary
import java.time.Instant

data class TaskSummaryUiState(val id: Long, val name: String, val isDone: Boolean) {
    fun toNamedEntity() = NamedEntity(id = id, name = name)

    companion object {
        fun fromTaskSummary(task: TaskSummary, doneBefore: Instant) = TaskSummaryUiState(
            id = task.id,
            name = task.name,
            isDone = isTaskDone(
                doneAt = task.doneAt,
                doneBefore = doneBefore,
                recurring = task.recurring,
            ),
        )
    }
}

fun isTaskDone(doneAt: Instant?, doneBefore: Instant, recurring: Boolean) = if (recurring) {
    doneAt?.let { it < doneBefore } == false
} else {
    doneAt != null
}
