package io.github.evaogbe.diswantin.task.data

import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getCurrentTask(params: CurrentTaskParams): Flow<Task?>

    fun getById(id: Long): Flow<Task>

    fun getParentTask(id: Long): Flow<Task?>

    fun getTaskDetailById(id: Long): Flow<TaskDetail?>

    fun search(query: String): Flow<List<Task>>

    fun hasTasksExcluding(ids: Collection<Long>): Flow<Boolean>

    suspend fun create(form: NewTaskForm): Task

    suspend fun update(form: EditTaskForm): Task

    suspend fun delete(id: Long)

    suspend fun markDone(id: Long)

    suspend fun addParent(id: Long, parentId: Long)
}
