package io.github.evaogbe.diswantin.task.data

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getCurrentTask(params: CurrentTaskParams): Flow<Task?>

    fun getById(id: Long): Flow<Task>

    fun getTaskDetailById(id: Long): Flow<TaskDetail?>

    fun getTasksByCategoryId(categoryId: Long): Flow<PagingData<Task>>

    fun getTaskItemsByCategoryId(categoryId: Long): Flow<PagingData<TaskItemData>>

    fun search(query: String): Flow<List<Task>>

    fun searchTaskItems(criteria: TaskSearchCriteria): Flow<PagingData<TaskItemData>>

    fun getParent(id: Long): Flow<Task?>

    fun getChildren(id: Long): Flow<PagingData<TaskItemData>>

    fun getTaskRecurrencesByTaskId(taskId: Long): Flow<List<TaskRecurrence>>

    fun getCount(): Flow<Long>

    suspend fun create(form: NewTaskForm): Task

    suspend fun update(form: EditTaskForm): Task

    suspend fun delete(id: Long)

    suspend fun markDone(id: Long)

    suspend fun unmarkDone(id: Long)

    suspend fun skip(id: Long)
}
