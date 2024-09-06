package io.github.evaogbe.diswantin.task.data

import io.github.evaogbe.diswantin.data.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
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
            scheduledBefore = params.scheduledBefore,
            doneBefore = params.doneBefore,
        )
            .map { priorities ->
                priorities.sortedWith(
                    compareBy(nullsLast(), TaskPriority::scheduledAtPriority)
                        .thenComparing({
                            it.deadline
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

    private val TaskPriority.deadline
        get() = when {
            deadlineDatePriority != null && deadlineTimePriority != null -> {
                deadlineDatePriority.atTime(deadlineTimePriority).atZone(clock.zone)
            }

            deadlineDatePriority != null -> {
                deadlineDatePriority.atTime(LocalTime.MAX).atZone(clock.zone)
            }

            deadlineTimePriority != null -> ZonedDateTime.now(clock).with(deadlineTimePriority)
            else -> null
        }

    override fun getById(id: Long) = taskDao.getById(id).flowOn(ioDispatcher)

    override fun getTaskDetailById(id: Long) =
        taskDao.getTaskDetailById(id).flowOn(ioDispatcher)

    override fun getParentTask(id: Long) = taskDao.getParentTask(id).flowOn(ioDispatcher)

    override fun search(query: String) = taskDao.search(escapeSql(query)).flowOn(ioDispatcher)

    private fun escapeSql(str: String) =
        str.replace("'", "''").replace("\"", "\"\"")

    override fun getCount(excludeDone: Boolean) =
        if (excludeDone) {
            taskDao.getUndoneCount(ZonedDateTime.now(clock).with(LocalTime.MIN).toInstant())
                .flowOn(ioDispatcher)
        } else {
            taskDao.getCount().flowOn(ioDispatcher)
        }

    override suspend fun create(form: NewTaskForm): Task {
        val task = form.newTask
        return withContext(ioDispatcher) {
            task.copy(id = taskDao.insertWithParent(task, form.parentTaskId))
        }
    }

    override suspend fun update(form: EditTaskForm) =
        withContext(ioDispatcher) {
            when (form.parentUpdateType) {
                is PathUpdateType.Keep -> taskDao.update(form.updatedTask)
                is PathUpdateType.Remove -> taskDao.updateWithoutParent(form.updatedTask)
                is PathUpdateType.Replace -> {
                    taskDao.updateWithParent(form.updatedTask, form.parentUpdateType.id)
                }
            }
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

    override suspend fun addParent(id: Long, parentId: Long) {
        withContext(ioDispatcher) {
            taskDao.connectPath(parentId = parentId, childId = id)
        }
    }
}
