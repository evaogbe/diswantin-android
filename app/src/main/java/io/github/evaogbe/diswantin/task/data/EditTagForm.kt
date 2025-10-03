package io.github.evaogbe.diswantin.task.data

import java.time.Instant

data class EditTagForm(
    private val name: String,
    private val now: Instant,
    val existingId: Long,
) {
    init {
        require(name.isNotBlank()) { "Name must be present" }
    }

    fun getUpdatedTag(tag: Tag) = tag.copy(name = name.trim(), updatedAt = now)
}
