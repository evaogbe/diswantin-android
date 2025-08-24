package io.github.evaogbe.diswantin.task.data

data class EditTagForm(
    private val name: String,
    val taskIdsToInsert: Set<Long>,
    val taskIdsToRemove: Set<Long>,
    private val existingTag: Tag,
) {
    init {
        require(name.isNotBlank()) { "Name must be present" }
    }

    val updatedTag = existingTag.copy(name = name.trim())
}
