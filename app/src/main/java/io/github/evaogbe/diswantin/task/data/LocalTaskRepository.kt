package io.github.evaogbe.diswantin.task.data

import io.github.evaogbe.diswantin.data.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject

class LocalTaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    clock: Clock,
) : TaskRepository {
    override val currentTaskStream = taskDao.getCurrentTask(
        scheduledBefore = ZonedDateTime.now(clock).plusHours(1).toInstant()
    ).flowOn(ioDispatcher)

    override fun getById(id: Long) = taskDao.getById(id).flowOn(ioDispatcher)

    override fun search(
        query: String,
        tailsOnly: Boolean,
        excludeChainFor: Long?
    ): Flow<List<Task>> = when {
        !tailsOnly -> taskDao.search(escapeSql(query))
        excludeChainFor == null -> taskDao.searchTails(escapeSql(query))
        else -> taskDao.searchAvailableParents(escapeSql(query), excludeChainFor)
    }.flowOn(ioDispatcher)

    private fun escapeSql(str: String) =
        str.replace("'", "''").replace("\"", "\"\"")

    override fun getChain(id: Long) = taskDao.getChain(id).flowOn(ioDispatcher)

    override fun getParent(id: Long) = taskDao.getParent(id).flowOn(ioDispatcher)

    override fun hasTasks(excludeChainFor: Long?) =
        (excludeChainFor?.let { taskDao.hasTasksOutsideChain(it) }
            ?: taskDao.hasTasks())
            .flowOn(ioDispatcher)

    override suspend fun create(form: NewTaskForm): Task {
        val task = form.newTask
        return withContext(ioDispatcher) {
            task.copy(id = taskDao.insertWithParent(task, form.prevTaskId))
        }
    }

    override suspend fun update(form: EditTaskForm) {
        withContext(ioDispatcher) {
            if (form.parentId == form.oldParentId) {
                taskDao.update(form.updatedTask)
            } else {
                taskDao.updateAndReplaceParent(
                    task = form.updatedTask,
                    parentId = form.parentId,
                    oldParentId = form.oldParentId,
                )
            }
        }
    }

    override suspend fun remove(id: Long) {
        withContext(ioDispatcher) {
            taskDao.deleteWithChain(id)
        }
    }
}
