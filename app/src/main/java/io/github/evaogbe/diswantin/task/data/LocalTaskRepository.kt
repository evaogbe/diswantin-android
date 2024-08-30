package io.github.evaogbe.diswantin.task.data

import io.github.evaogbe.diswantin.data.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

class LocalTaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TaskRepository {
    override fun getCurrentTask(scheduledBefore: Instant) =
        taskDao.getCurrentTask(scheduledBefore = scheduledBefore).flowOn(ioDispatcher)

    override fun getById(id: Long) = taskDao.getById(id).flowOn(ioDispatcher)

    override fun search(
        query: String,
        singletonsOnly: Boolean
    ): Flow<List<Task>> = (if (singletonsOnly) {
        taskDao.searchSingletons(escapeSql(query))
    } else {
        taskDao.search(escapeSql(query))
    }).flowOn(ioDispatcher)

    private fun escapeSql(str: String) =
        str.replace("'", "''").replace("\"", "\"\"")

    override fun getTaskListItems(id: Long) = taskDao.getTaskListItems(id).flowOn(ioDispatcher)

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

    override suspend fun remove(id: Long) {
        withContext(ioDispatcher) {
            taskDao.deleteWithPath(id)
        }
    }
}
