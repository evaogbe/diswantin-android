package io.github.evaogbe.diswantin.task.data

import java.time.Instant

data class EditTaskForm(
    private val name: String,
    private val dueAt: Instant?,
    private val scheduledAt: Instant?,
    val oldParentId: Long?,
    val parentId: Long?,
    private val task: Task,
) {
    init {
        require(dueAt == null || scheduledAt == null) {
            "Must have only one of dueAt and scheduledAt, but got $dueAt and $scheduledAt"
        }
    }

    val updatedTask = task.copy(
        name = name.trim(),
        dueAt = dueAt,
        scheduledAt = scheduledAt,
    )
}
