package io.github.evaogbe.diswantin.task.data

import kotlinx.coroutines.flow.Flow

interface TaskListRepository {
    val taskListsStream: Flow<List<TaskList>>

    fun getTaskListWithTasksById(id: Long): Flow<TaskListWithTasks>

    fun getTaskListWithTaskItemsById(id: Long): Flow<TaskListWithTaskItems?>

    suspend fun create(form: NewTaskListForm): TaskListWithTasks

    suspend fun update(form: EditTaskListForm): TaskListWithTasks

    suspend fun delete(taskList: TaskList)
}
