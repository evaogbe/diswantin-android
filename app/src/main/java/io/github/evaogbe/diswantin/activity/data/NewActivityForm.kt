package io.github.evaogbe.diswantin.activity.data

import java.time.Clock
import java.time.Instant

data class NewActivityForm(
    private val name: String,
    private val dueAt: Instant?,
    private val scheduledAt: Instant?,
    val prevActivityId: Long?,
    private val clock: Clock,
) {
    init {
        require(dueAt == null || scheduledAt == null) {
            "Must have only one of dueAt and scheduledAt, but got $dueAt and $scheduledAt"
        }
    }

    val newActivity
        get() = Activity(
            createdAt = Instant.now(clock),
            name = name,
            dueAt = dueAt,
            scheduledAt = scheduledAt,
        )
}
