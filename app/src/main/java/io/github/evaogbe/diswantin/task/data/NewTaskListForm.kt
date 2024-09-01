package io.github.evaogbe.diswantin.task.data

data class NewTaskListForm(private val name: String, val tasks: List<Task>) {
    init {
        require(name.isNotBlank()) { "Name must be present" }
    }

    val newTaskList = TaskList(name = name.trim())

    val newTaskIds = tasks.map { it.id }

    val taskPaths = tasks.flatMapIndexed { i, ancestor ->
        tasks.drop(i + 1).mapIndexed { j, descendant ->
            TaskPath(ancestor = ancestor.id, descendant = descendant.id, depth = j + 1)
        }
    }
}
