package io.github.evaogbe.diswantin.task.data

import java.time.Instant

data class EditTagForm(
    private val name: String,
    private val now: Instant,
    private val existingTag: Tag,
) {
    init {
        require(name.isNotBlank()) { "Name must be present" }
    }

    val updatedTag = existingTag.copy(name = name.trim(), updatedAt = now)
}
