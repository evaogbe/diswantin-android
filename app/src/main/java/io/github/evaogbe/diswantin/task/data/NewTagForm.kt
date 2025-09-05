package io.github.evaogbe.diswantin.task.data

import java.time.Instant

data class NewTagForm(private val name: String, private val now: Instant) {
    init {
        require(name.isNotBlank()) { "Name must be present" }
    }

    val newTag = Tag(name = name.trim(), createdAt = now, updatedAt = now)
}
