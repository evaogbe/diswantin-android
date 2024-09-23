package io.github.evaogbe.diswantin.task.data

import io.github.evaogbe.diswantin.data.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val clock: Clock,
) : TaskRepository {
    override fun getCurrentTask(params: CurrentTaskParams) =
        taskDao.getTaskPriorities(
            today = params.today,
            scheduledTimeBefore = params.scheduledTimeBefore,
            doneBefore = params.doneBefore,
            week = params.week,
        )
            .map { priorities ->
                priorities.sortedWith(
                    compareBy<TaskPriority, ZonedDateTime?>(nullsLast()) {
                        dateTimePartsToZonedDateTime(
                            it.scheduledDatePriority,
                            it.scheduledTimePriority,
                            LocalTime.of(9, 0),
                        )
                    }
                        .thenComparing({
                            dateTimePartsToZonedDateTime(
                                it.deadlineDatePriority,
                                it.deadlineTimePriority,
                                LocalTime.MAX,
                            )
                                ?: if (it.recurringPriority) params.recurringDeadline else null
                        }, nullsLast())
                        .thenComparing(TaskPriority::recurringPriority, reverseOrder())
                        .thenComparing(TaskPriority::createdAtPriority)
                        .thenComparing(TaskPriority::idPriority)
                )
                    .firstOrNull()
                    ?.task
            }
            .flowOn(ioDispatcher)

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

    override fun getTaskDetailById(id: Long) =
        taskDao.getTaskDetailById(id).flowOn(ioDispatcher)

    override fun search(query: String) = taskDao.search(escapeSql(query)).flowOn(ioDispatcher)

    override fun searchTaskItems(query: String) =
        taskDao.searchTaskItems(escapeSql(query)).flowOn(ioDispatcher)

    private fun escapeSql(str: String) =
        str.replace("'", "''").replace("\"", "\"\"")

    override fun getParent(id: Long) = taskDao.getParent(id).flowOn(ioDispatcher)

    override fun getChildren(id: Long) = taskDao.getChildren(id).flowOn(ioDispatcher)

    override fun getTaskRecurrencesByTaskId(taskId: Long) =
        taskDao.getTaskRecurrencesByTaskId(taskId).flowOn(ioDispatcher)

    override fun getCount() =
        taskDao.getCount().flowOn(ioDispatcher)

    override suspend fun create(form: NewTaskForm): Task {
        val task = form.newTask
        return withContext(ioDispatcher) {
            task.copy(id = taskDao.insert(form))
        }
    }

    override suspend fun update(form: EditTaskForm) =
        withContext(ioDispatcher) {
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

    override suspend fun addParent(id: Long, parentId: Long) {
        withContext(ioDispatcher) {
            taskDao.connectPath(parentId = parentId, childId = id)
        }
    }
}
