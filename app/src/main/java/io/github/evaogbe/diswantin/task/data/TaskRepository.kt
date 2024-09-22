package io.github.evaogbe.diswantin.task.data

import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getCurrentTask(params: CurrentTaskParams): Flow<Task?>

    fun getById(id: Long): Flow<Task>

    fun getParent(id: Long): Flow<Task?>

    fun getTaskDetailById(id: Long): Flow<TaskDetail?>

    fun search(query: String): Flow<List<Task>>

    fun searchTaskItems(query: String): Flow<List<TaskItem>>

    fun getTaskRecurrencesByTaskId(taskId: Long): Flow<List<TaskRecurrence>>

    fun getCount(): Flow<Long>

    suspend fun create(form: NewTaskForm): Task

    suspend fun update(form: EditTaskForm): Task

    suspend fun delete(id: Long)

    suspend fun markDone(id: Long)

    suspend fun addParent(id: Long, parentId: Long)
}
