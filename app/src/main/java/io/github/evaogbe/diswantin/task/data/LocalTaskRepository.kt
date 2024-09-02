package io.github.evaogbe.diswantin.task.data

import io.github.evaogbe.diswantin.data.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
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
                            it.deadlinePriority
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

    override fun getById(id: Long) = taskDao.getById(id).flowOn(ioDispatcher)

    override fun getTaskDetailById(id: Long) =
        taskDao.getTaskDetailById(id).flowOn(ioDispatcher)

    override fun search(query: String) = taskDao.search(escapeSql(query)).flowOn(ioDispatcher)

    private fun escapeSql(str: String) =
        str.replace("'", "''").replace("\"", "\"\"")

    override suspend fun create(form: NewTaskForm): Task {
        val task = form.newTask
        return withContext(ioDispatcher) {
            task.copy(id = taskDao.insertWithPath(task))
        }
    }

    override suspend fun update(form: EditTaskForm) =
        withContext(ioDispatcher) {
            taskDao.update(form.updatedTask)
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
}
