package io.github.evaogbe.diswantin.task.data

data class EditTaskCategoryForm(
    private val name: String,
    val newTasks: List<Task>,
    val taskIdsToRemove: Set<Long>,
    private val existingCategory: TaskCategory,
) {
    init {
        require(name.isNotBlank()) { "Name must be present" }
    }

    val taskIdsToInsert = newTasks.map { it.id }

    val updatedCategory = existingCategory.copy(name = name.trim())
}
