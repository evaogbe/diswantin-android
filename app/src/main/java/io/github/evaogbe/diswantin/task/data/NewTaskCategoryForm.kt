package io.github.evaogbe.diswantin.task.data

data class NewTaskCategoryForm(private val name: String, val tasks: List<Task>) {
    init {
        require(name.isNotBlank()) { "Name must be present" }
    }

    val newCategory = TaskCategory(name = name.trim())

    val newTaskIds = tasks.map { it.id }
}
