package io.github.evaogbe.diswantin.task.data

import io.github.evaogbe.diswantin.data.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalTaskCategoryRepository @Inject constructor(
    private val taskCategoryDao: TaskCategoryDao,
    private val taskDao: TaskDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TaskCategoryRepository {
    override val categoriesStream = taskCategoryDao.getTaskCategories().flowOn(ioDispatcher)

    override val hasCategoriesStream = taskCategoryDao.hasCategories().flowOn(ioDispatcher)

    override fun getByTaskId(taskId: Long) =
        taskCategoryDao.getByTaskId(taskId).flowOn(ioDispatcher)

    override fun getCategoryWithTasksById(id: Long) =
        combine(taskCategoryDao.getById(id), taskDao.getTasksByCategoryId(id)) { category, tasks ->
            TaskCategoryWithTasks(checkNotNull(category), tasks)
        }.flowOn(ioDispatcher)

    override fun getCategoryWithTaskItemsById(id: Long) =
        combine(
            taskCategoryDao.getById(id),
            taskDao.getTaskItemsByCategoryId(id),
        ) { category, tasks ->
            category?.let { TaskCategoryWithTaskItems(it, tasks) }
        }.flowOn(ioDispatcher)

    override fun search(query: String) =
        taskCategoryDao.search(escapeSql(query)).flowOn(ioDispatcher)

    private fun escapeSql(str: String) = str.replace("'", "''").replace("\"", "\"\"")

    override suspend fun create(form: NewTaskCategoryForm) = withContext(ioDispatcher) {
        val id = taskCategoryDao.insertWithTasks(
            category = form.newCategory,
            taskIds = form.newTaskIds,
        )
        TaskCategoryWithTasks(
            form.newCategory.copy(id = id),
            form.tasks.map { it.copy(categoryId = id) },
        )
    }

    override suspend fun update(form: EditTaskCategoryForm) = withContext(ioDispatcher) {
        taskCategoryDao.updateWithTasks(
            category = form.updatedCategory,
            taskIdsToInsert = form.taskIdsToInsert,
            taskIdsToRemove = form.taskIdsToRemove,
        )
        TaskCategoryWithTasks(
            form.updatedCategory,
            form.tasks.map { it.copy(categoryId = form.updatedCategory.id) },
        )
    }

    override suspend fun delete(category: TaskCategory) {
        withContext(ioDispatcher) {
            taskCategoryDao.delete(category)
        }
    }
}
