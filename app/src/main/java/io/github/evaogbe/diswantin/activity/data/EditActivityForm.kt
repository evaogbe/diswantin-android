package io.github.evaogbe.diswantin.activity.data

import java.time.Instant

data class EditActivityForm(
    private val name: String,
    private val dueAt: Instant?,
    private val scheduledAt: Instant?,
    val oldParentId: Long?,
    val parentId: Long?,
    private val activity: Activity,
) {
    init {
        require(dueAt == null || scheduledAt == null) {
            "Must have only one of dueAt and scheduledAt, but got $dueAt and $scheduledAt"
        }
    }

    val updatedActivity = activity.copy(
        name = name.trim(),
        dueAt = dueAt,
        scheduledAt = scheduledAt,
    )
}
