package io.github.evaogbe.diswantin.task.data

data class EditTaskCategoryForm(
    private val name: String,
    val tasks: List<Task>,
    private val existingCategoryWithTasks: TaskCategoryWithTasks,
) {
    val taskIdsToRemove: List<Long>

    val taskIdsToInsert: List<Long>

    init {
        require(name.isNotBlank()) { "Name must be present" }

        val oldTaskIds = existingCategoryWithTasks.tasks.map { it.id }.toSet()
        val newTaskIds = tasks.map { it.id }.toSet()
        taskIdsToRemove = oldTaskIds.filterNot { it in newTaskIds }
        taskIdsToInsert = newTaskIds.filterNot { it in oldTaskIds }
    }

    val updatedCategory = existingCategoryWithTasks.category.copy(name = name.trim())
}
