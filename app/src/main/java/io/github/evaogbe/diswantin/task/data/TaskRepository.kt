package io.github.evaogbe.diswantin.task.data

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

interface TaskRepository {
    fun getCurrentTask(params: CurrentTaskParams): Flow<CurrentTask?>

    fun getTaskById(id: Long): Flow<Task>

    fun getTaskDetailById(id: Long): Flow<TaskDetail?>

    fun getTaskSummariesByTagId(tagId: Long, startOfToday: Instant): Flow<PagingData<TaskSummary>>

    fun searchTaskSummaries(criteria: TaskSearchCriteria): Flow<PagingData<TaskSummary>>

    fun getParent(id: Long): Flow<Task?>

    fun getChildren(id: Long): Flow<PagingData<TaskSummary>>

    fun getTasksDueAt(dueAt: LocalDate, zone: ZoneId): Flow<PagingData<DueTaskWithRecurrences>>

    fun getTaskRecurrencesByTaskId(taskId: Long): Flow<List<TaskRecurrence>>

    fun getTaskCount(): Flow<Long>

    suspend fun create(form: NewTaskForm): Task

    suspend fun update(form: EditTaskForm): Task

    suspend fun delete(id: Long)

    suspend fun markDone(data: TaskCompletion)

    suspend fun unmarkDone(id: Long)

    suspend fun skip(data: TaskSkip)
}
