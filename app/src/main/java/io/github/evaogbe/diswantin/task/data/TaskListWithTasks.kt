package io.github.evaogbe.diswantin.task.data

data class TaskListWithTasks(val taskList: TaskList, val tasks: List<Task>)

data class TaskListWithTaskItems(val taskList: TaskList, val tasks: List<TaskItem>)
