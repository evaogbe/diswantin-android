package io.github.evaogbe.diswantin.task.data

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getCurrentTask(params: CurrentTaskParams): Flow<CurrentTask?>

    fun getById(id: Long): Flow<Task>

    fun getTaskDetailById(id: Long): Flow<TaskDetail?>

    fun getTaskSummariesByTagId(tagId: Long): Flow<PagingData<TaskSummary>>

    fun searchTaskSummaries(criteria: TaskSearchCriteria): Flow<PagingData<TaskSummary>>

    fun getParent(id: Long): Flow<Task?>

    fun getChildren(id: Long): Flow<PagingData<TaskSummary>>

    fun getTaskRecurrencesByTaskId(taskId: Long): Flow<List<TaskRecurrence>>

    fun getCount(): Flow<Long>

    suspend fun create(form: NewTaskForm): Task

    suspend fun update(form: EditTaskForm): Task

    suspend fun delete(id: Long)

    suspend fun markDone(id: Long)

    suspend fun unmarkDone(id: Long)

    suspend fun skip(id: Long)
}
