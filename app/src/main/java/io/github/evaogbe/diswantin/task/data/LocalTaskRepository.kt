package io.github.evaogbe.diswantin.task.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.filter
import androidx.paging.map
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class LocalTaskRepository @Inject constructor(private val taskDao: TaskDao) : TaskRepository {
    override fun getCurrentTask(params: CurrentTaskParams) = taskDao.getCurrentTask(
        today = params.today,
        currentTime = params.currentTime,
        startOfToday = params.startOfToday,
        overdueTime = params.overdueTime,
    )

    override fun getTaskById(id: Long) = taskDao.getTaskById(id)

    override fun getTaskDetailById(id: Long) = taskDao.getTaskDetailById(id)

    override fun getTaskSummariesByTagId(tagId: Long, startOfToday: Instant) =
        Pager(PagingConfig(pageSize = 40)) {
            taskDao.getTaskSummariesByTagId(tagId, startOfToday)
        }.flow

    override fun searchTaskSummaries(criteria: TaskSearchCriteria) =
        Pager(PagingConfig(pageSize = 50)) {
            if (criteria.name.isEmpty()) {
                taskDao.filterTaskSummaries(
                    deadlineStartDate = criteria.deadlineDateRange?.first,
                    deadlineEndDate = criteria.deadlineDateRange?.second,
                    startAfterStartDate = criteria.startAfterDateRange?.first,
                    startAfterEndDate = criteria.startAfterDateRange?.second,
                    scheduledStartDate = criteria.scheduledDateRange?.first,
                    scheduledEndDate = criteria.scheduledDateRange?.second,
                    doneStart = criteria.doneRange?.first,
                    doneEnd = criteria.doneRange?.second,
                    recurrenceDate = criteria.recurrenceDate,
                )
            } else {
                taskDao.searchTaskSummaries(
                    query = escapeSql("${criteria.name}*"),
                    deadlineStartDate = criteria.deadlineDateRange?.first,
                    deadlineEndDate = criteria.deadlineDateRange?.second,
                    startAfterStartDate = criteria.startAfterDateRange?.first,
                    startAfterEndDate = criteria.startAfterDateRange?.second,
                    scheduledStartDate = criteria.scheduledDateRange?.first,
                    scheduledEndDate = criteria.scheduledDateRange?.second,
                    doneStart = criteria.doneRange?.first,
                    doneEnd = criteria.doneRange?.second,
                    recurrenceDate = criteria.recurrenceDate,
                )
            }
        }.flow.map { results ->
            results.filter { (task, recurrences) ->
                !task.recurring || (criteria.deadlineDateRange?.let { (start, end) ->
                    generateSequence(start) { if (it < end) it.plusDays(1) else null }.any {
                        doesRecurOnDate(recurrences, it)
                    }
                } != false && criteria.startAfterDateRange?.let { (start, end) ->
                    generateSequence(start) { if (it < end) it.plusDays(1) else null }.any {
                        doesRecurOnDate(recurrences, it)
                    }
                } != false && criteria.scheduledDateRange?.let { (start, end) ->
                    generateSequence(start) { if (it < end) it.plusDays(1) else null }.any {
                        doesRecurOnDate(recurrences, it)
                    }
                } != false)
            }.map { it.task }
        }

    private fun escapeSql(str: String) = str.replace("'", "''").replace("\"", "\"\"")

    override fun getTasksDueAt(dueAt: LocalDate, zone: ZoneId) =
        Pager(PagingConfig(pageSize = 40)) {
            taskDao.getTasksDueAt(
                dueAt,
                dueAt.atStartOfDay(zone).toInstant(),
                dueAt.plusDays(1).atStartOfDay(zone).toInstant(),
            )
        }.flow

    override fun getParent(id: Long) = taskDao.getParent(id)

    override fun getChildren(id: Long) = Pager(PagingConfig(pageSize = 40)) {
        taskDao.getChildren(id)
    }.flow

    override fun getTaskRecurrencesByTaskId(taskId: Long) =
        taskDao.getTaskRecurrencesByTaskId(taskId)

    override fun getTaskCount() = taskDao.getTaskCount()

    override suspend fun create(form: NewTaskForm): Task {
        return form.newTask.copy(id = taskDao.insert(form))
    }

    override suspend fun update(form: EditTaskForm): Task {
        return taskDao.update(form)
    }

    override suspend fun delete(id: Long) {
        taskDao.deleteWithPath(id)
    }

    override suspend fun markDone(data: TaskCompletion) {
        taskDao.insertCompletion(data)
    }

    override suspend fun unmarkDone(id: Long) {
        taskDao.deleteLatestTaskCompletionByTaskId(id)
    }

    override suspend fun skip(data: TaskSkip) {
        taskDao.insertSkip(data)
    }
}
