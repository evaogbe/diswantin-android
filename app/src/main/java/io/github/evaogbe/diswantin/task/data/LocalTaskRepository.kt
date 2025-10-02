package io.github.evaogbe.diswantin.task.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

class LocalTaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val clock: Clock,
) : TaskRepository {
    override fun getCurrentTask(params: CurrentTaskParams) = taskDao.getCurrentTask(
        today = params.today,
        currentTime = params.currentTime,
        startOfToday = params.startOfToday,
        overdueTime = params.overdueTime,
    )

    override fun getTaskById(id: Long) = taskDao.getTaskById(id)

    override fun getTaskDetailById(id: Long) = taskDao.getTaskDetailById(id)

    override fun getTaskSummariesByTagId(tagId: Long): Flow<PagingData<TaskSummary>> {
        val startOfToday = ZonedDateTime.now(clock).with(LocalTime.MIN).toInstant()
        return Pager(PagingConfig(pageSize = 40)) {
            taskDao.getTaskSummariesByTagId(tagId, startOfToday)
        }.flow
    }

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
                    doneStart = criteria.doneDateRange?.first?.atStartOfDay(clock.zone)
                        ?.toInstant(),
                    doneEnd = criteria.doneDateRange?.second?.atStartOfDay(clock.zone)
                        ?.with(LocalTime.MAX)?.toInstant(),
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
                    doneStart = criteria.doneDateRange?.first?.atStartOfDay(clock.zone)
                        ?.toInstant(),
                    doneEnd = criteria.doneDateRange?.second?.atStartOfDay(clock.zone)
                        ?.with(LocalTime.MAX)?.toInstant(),
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
        taskDao.update(form)
        return form.updatedTask
    }

    override suspend fun delete(id: Long) {
        taskDao.deleteWithPath(id)
    }

    override suspend fun markDone(id: Long) {
        taskDao.insertCompletion(TaskCompletion(taskId = id, doneAt = Instant.now(clock)))
    }

    override suspend fun unmarkDone(id: Long) {
        taskDao.deleteLatestTaskCompletionByTaskId(id)
    }

    override suspend fun skip(id: Long) {
        taskDao.insertSkip(TaskSkip(taskId = id, skippedAt = Instant.now(clock)))
    }
}
