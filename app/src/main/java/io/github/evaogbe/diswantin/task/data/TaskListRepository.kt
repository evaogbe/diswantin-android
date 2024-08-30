package io.github.evaogbe.diswantin.task.data

import kotlinx.coroutines.flow.Flow

interface TaskListRepository {
    val taskListsStream: Flow<List<TaskList>>

    suspend fun create(form: NewTaskListForm): TaskListWithTasks

    suspend fun update(form: EditTaskListForm): TaskListWithTasks
}
