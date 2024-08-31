package io.github.evaogbe.diswantin.task.data

import java.time.Clock
import java.time.Instant

data class NewTaskForm(
    private val name: String,
    private val deadline: Instant?,
    private val scheduledAt: Instant?,
    private val recurring: Boolean,
    private val clock: Clock,
) {
    init {
        require(name.isNotBlank()) { "Name must be present" }
        require(deadline == null || scheduledAt == null) {
            "Must have only one of deadline and scheduledAt, but got $deadline and $scheduledAt"
        }
    }

    val newTask
        get() = Task(
            createdAt = Instant.now(clock),
            name = name,
            deadline = deadline,
            scheduledAt = scheduledAt,
            recurring = recurring,
        )
}
