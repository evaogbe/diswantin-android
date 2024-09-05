package io.github.evaogbe.diswantin.task.data

data class TaskCategoryWithTasks(val category: TaskCategory, val tasks: List<Task>)

data class TaskCategoryWithTaskItems(val category: TaskCategory, val tasks: List<TaskItem>)
