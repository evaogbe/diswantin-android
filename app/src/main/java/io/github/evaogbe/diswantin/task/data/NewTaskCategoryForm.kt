package io.github.evaogbe.diswantin.task.data

data class NewTaskCategoryForm(private val name: String, val newTasks: List<Task>) {
    init {
        require(name.isNotBlank()) { "Name must be present" }
    }

    val newCategory = TaskCategory(name = name.trim())

    val newTaskIds = newTasks.map { it.id }
}
