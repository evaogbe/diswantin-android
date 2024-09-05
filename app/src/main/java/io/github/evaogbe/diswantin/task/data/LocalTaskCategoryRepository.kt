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
    override val categoryListStream = taskCategoryDao.getTaskCategories().flowOn(ioDispatcher)

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
