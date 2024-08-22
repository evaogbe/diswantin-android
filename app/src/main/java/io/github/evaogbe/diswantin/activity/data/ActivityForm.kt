package io.github.evaogbe.diswantin.activity.data

import java.time.Clock
import java.time.Instant

data class ActivityForm(val name: String, val dueAt: Instant?, val scheduledAt: Instant?) {
    init {
        require(dueAt == null || scheduledAt == null) {
            "Must have only one of dueAt and scheduledAt, but got $dueAt and $scheduledAt"
        }
    }

    fun getNewActivity(clock: Clock) = Activity(
        createdAt = Instant.now(clock),
        name = name.trim(),
        dueAt = dueAt,
        scheduledAt = scheduledAt
    )

    fun getUpdatedActivity(activity: Activity) =
        activity.copy(name = name.trim(), dueAt = dueAt, scheduledAt = scheduledAt)
}
