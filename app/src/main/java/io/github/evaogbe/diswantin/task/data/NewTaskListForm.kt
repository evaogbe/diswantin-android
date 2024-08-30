package io.github.evaogbe.diswantin.task.data

data class NewTaskListForm(private val name: String, private val tasks: List<Task>) {
    init {
        require(name.isNotBlank()) { "Name must be present" }
    }

    val newTaskListWithTasks = TaskListWithTasks(TaskList(name = name.trim()), tasks)

    val taskPaths = tasks.flatMapIndexed { i, ancestor ->
        tasks.drop(i + 1).mapIndexed { j, descendant ->
            TaskPath(ancestor = ancestor.id, descendant = descendant.id, depth = j + 1)
        }
    }
}
