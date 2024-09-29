package io.github.evaogbe.diswantin.task.data

class NewTaskCategoryForm(private val name: String, tasks: List<Task>) {
    init {
        require(name.isNotBlank()) { "Name must be present" }
    }

    val newCategory = TaskCategory(name = name.trim())

    val tasks = tasks.take(20)

    val newTaskIds = tasks.take(20).map { it.id }
}
