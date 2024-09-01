package io.github.evaogbe.diswantin.task.data

import io.github.evaogbe.diswantin.data.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalTaskListRepository @Inject constructor(
    private val taskListDao: TaskListDao,
    private val taskDao: TaskDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TaskListRepository {
    override val taskListsStream = taskListDao.getTaskLists().flowOn(ioDispatcher)

    override fun getTaskListWithTasksById(id: Long) =
        combine(taskListDao.getById(id), taskDao.getTasksByListId(id)) { taskList, tasks ->
            TaskListWithTasks(checkNotNull(taskList), tasks)
        }.flowOn(ioDispatcher)

    override fun getTaskListWithTaskItemsById(id: Long) =
        combine(taskListDao.getById(id), taskDao.getTaskItemsByListId(id)) { taskList, tasks ->
            taskList?.let { TaskListWithTaskItems(it, tasks) }
        }.flowOn(ioDispatcher)

    override suspend fun create(form: NewTaskListForm) = withContext(ioDispatcher) {
        val id = taskListDao.insertWithTasks(
            taskList = form.newTaskList,
            taskIds = form.newTaskIds,
            taskPaths = form.taskPaths,
        )
        TaskListWithTasks(form.newTaskList.copy(id = id), form.tasks.map { it.copy(listId = id) })
    }

    override suspend fun update(form: EditTaskListForm) = withContext(ioDispatcher) {
        taskListDao.updateWithTasks(
            taskList = form.updatedTaskList,
            taskIdsToInsert = form.taskIdsToInsert,
            taskPathsToInsert = form.taskPathsToInsert,
            taskIdsToRemove = form.taskIdsToRemove,
            taskPathTaskIdsToRemove = form.taskPathTaskIdsToRemove,
        )
        TaskListWithTasks(
            form.updatedTaskList,
            form.tasks.map { it.copy(listId = form.updatedTaskList.id) },
        )
    }

    override suspend fun delete(taskList: TaskList) {
        withContext(ioDispatcher) {
            taskListDao.deleteWithPaths(taskList)
        }
    }
}
