package io.github.evaogbe.diswantin.task.data

import io.github.evaogbe.diswantin.data.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalTaskListRepository @Inject constructor(
    private val taskListDao: TaskListDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TaskListRepository {
    override val taskListsStream = taskListDao.getTaskLists().flowOn(ioDispatcher)

    override suspend fun create(form: NewTaskListForm) = withContext(ioDispatcher) {
        val id = taskListDao.insertWithTasks(
            taskList = form.newTaskListWithTasks.taskList,
            taskIds = form.newTaskListWithTasks.tasks.map { it.id },
            taskPaths = form.taskPaths,
        )
        TaskListWithTasks(
            form.newTaskListWithTasks.taskList.copy(id = id),
            form.newTaskListWithTasks.tasks.map { it.copy(listId = id) },
        )
    }

    override suspend fun update(form: EditTaskListForm) = withContext(ioDispatcher) {
        taskListDao.updateWithTasks(
            taskList = form.updatedTaskListWithTasks.taskList,
            taskIdsToInsert = form.taskIdsToInsert,
            taskPathsToInsert = form.taskPathsToInsert,
            taskIdsToRemove = form.taskIdsToRemove,
            taskPathTaskIdsToRemove = form.taskPathTaskIdsToRemove,
        )
        form.updatedTaskListWithTasks
    }
}
