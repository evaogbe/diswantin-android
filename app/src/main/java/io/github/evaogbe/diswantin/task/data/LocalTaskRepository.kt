package io.github.evaogbe.diswantin.task.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.map
import io.github.evaogbe.diswantin.data.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject

class LocalTaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val clock: Clock,
) : TaskRepository {
    override fun getCurrentTask(params: CurrentTaskParams) = taskDao.getTaskPriorities(
        today = params.today,
        currentTime = params.currentTime,
        startOfToday = params.startOfToday,
    ).map { priorities ->
        priorities.sortedWith(
            compareBy<TaskPriority, ZonedDateTime?>(nullsLast()) {
                dateTimePartsToZonedDateTime(
                    it.task.scheduledDate,
                    it.task.scheduledTime,
                    LocalTime.MIN,
                )
            }.thenComparing { priority ->
                priority.task.deadlineDate?.let { it >= params.today } != false
            }.thenComparing { priority ->
                priority.task.deadlineTime?.let {
                    it > params.currentTime.plusHours(1)
                } != false
            }.thenComparing({
                dateTimePartsToZonedDateTime(
                    it.scheduledDatePriority,
                    it.scheduledTimePriority,
                    LocalTime.MIN,
                )
            }, nullsLast()).thenComparing { priority ->
                priority.deadlineDatePriority?.let { it >= params.today } != false
            }.thenComparing { priority ->
                priority.deadlineTimePriority?.let {
                    it > params.currentTime.plusHours(1)
                } != false
            }.thenComparing { it.startAfterTimePriority != null }.thenComparing({
                dateTimePartsToZonedDateTime(
                    it.deadlineDatePriority,
                    it.deadlineTimePriority,
                    LocalTime.MAX,
                ) ?: if (it.recurringPriority) params.endOfToday else null
            }, nullsLast()).thenComparing(TaskPriority::recurringPriority, reverseOrder())
                .thenComparing({
                    dateTimePartsToZonedDateTime(
                        it.startAfterDatePriority,
                        it.startAfterTimePriority,
                        LocalTime.MIN,
                    )
                }, nullsFirst()).thenComparing(TaskPriority::createdAtPriority)
                .thenComparing(TaskPriority::idPriority)
        ).firstOrNull()?.task
    }.flowOn(ioDispatcher)

    private fun dateTimePartsToZonedDateTime(
        date: LocalDate?,
        time: LocalTime?,
        defaultTime: LocalTime,
    ) = when {
        date != null -> date.atTime(time ?: defaultTime).atZone(clock.zone)
        time != null -> ZonedDateTime.now(clock).with(time)
        else -> null
    }

    override fun getById(id: Long) = taskDao.getById(id).flowOn(ioDispatcher)

    override fun getTaskDetailById(id: Long) = taskDao.getTaskDetailById(id).flowOn(ioDispatcher)

    override fun getTaskSummariesByTagId(tagId: Long): Flow<PagingData<TaskSummary>> {
        val startOfToday = ZonedDateTime.now(clock).with(LocalTime.MIN).toInstant()
        return Pager(PagingConfig(pageSize = 20)) {
            taskDao.getTaskSummariesByTagId(tagId, startOfToday)
        }.flow.flowOn(ioDispatcher)
    }

    override fun getTaggedTasksByTagId(tagId: Long) = Pager(PagingConfig(pageSize = 20)) {
        taskDao.getTaggedTasksByTagId(tagId)
    }.flow.flowOn(ioDispatcher)

    override fun search(query: String, size: Int) =
        taskDao.search(escapeSql("$query*"), size).flowOn(ioDispatcher)

    override fun searchTaggedTasks(query: String, tagId: Long?, size: Int) =
        taskDao.searchTaggedTasks(escapeSql("$query*"), tagId, size).flowOn(ioDispatcher)

    override fun searchTaskSummaries(criteria: TaskSearchCriteria) =
        Pager(PagingConfig(pageSize = 30)) {
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
        }.flowOn(ioDispatcher)

    private fun escapeSql(str: String) = str.replace("'", "''").replace("\"", "\"\"")

    override fun getParent(id: Long) = taskDao.getParent(id).flowOn(ioDispatcher)

    override fun getChildren(id: Long) = Pager(PagingConfig(pageSize = 20)) {
        taskDao.getChildren(id)
    }.flow.flowOn(ioDispatcher)

    override fun getTaskRecurrencesByTaskId(taskId: Long) =
        taskDao.getTaskRecurrencesByTaskId(taskId).flowOn(ioDispatcher)

    override fun getCount() = taskDao.getCount().flowOn(ioDispatcher)

    override suspend fun create(form: NewTaskForm): Task {
        val task = form.newTask
        return withContext(ioDispatcher) {
            task.copy(id = taskDao.insert(form))
        }
    }

    override suspend fun update(form: EditTaskForm) = withContext(ioDispatcher) {
        taskDao.update(form)
        form.updatedTask
    }

    override suspend fun delete(id: Long) {
        withContext(ioDispatcher) {
            taskDao.deleteWithPath(id)
        }
    }

    override suspend fun markDone(id: Long) {
        withContext(ioDispatcher) {
            taskDao.insertCompletion(TaskCompletion(taskId = id, doneAt = Instant.now(clock)))
        }
    }

    override suspend fun unmarkDone(id: Long) {
        withContext(ioDispatcher) {
            taskDao.deleteLatestTaskCompletionByTaskId(id)
        }
    }

    override suspend fun skip(id: Long) {
        withContext(ioDispatcher) {
            taskDao.insertSkip(TaskSkip(taskId = id, skippedAt = Instant.now(clock)))
        }
    }
}
