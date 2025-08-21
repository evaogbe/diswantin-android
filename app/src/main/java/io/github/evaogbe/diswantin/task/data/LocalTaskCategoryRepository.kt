package io.github.evaogbe.diswantin.task.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import io.github.evaogbe.diswantin.data.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalTaskCategoryRepository @Inject constructor(
    private val taskCategoryDao: TaskCategoryDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TaskCategoryRepository {
    override val categoryPagingData = Pager(PagingConfig(pageSize = 20)) {
        taskCategoryDao.getTaskCategoryPagingSource()
    }.flow

    override val hasCategoriesStream = taskCategoryDao.hasCategories().flowOn(ioDispatcher)

    override fun getById(id: Long) = taskCategoryDao.getById(id).flowOn(ioDispatcher)

    override fun getByTaskId(taskId: Long) =
        taskCategoryDao.getByTaskId(taskId).flowOn(ioDispatcher)

    override fun search(query: String) =
        taskCategoryDao.search(escapeSql("$query*")).flowOn(ioDispatcher)

    private fun escapeSql(str: String) = str.replace("'", "''").replace("\"", "\"\"")

    override suspend fun create(form: NewTaskCategoryForm) = withContext(ioDispatcher) {
        val id = taskCategoryDao.insertWithTasks(
            category = form.newCategory,
            taskIds = form.newTaskIds,
        )
        TaskCategoryWithTasks(
            form.newCategory.copy(id = id),
            form.newTasks.map { it.copy(categoryId = id) },
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
            form.newTasks.map { it.copy(categoryId = form.updatedCategory.id) },
        )
    }

    override suspend fun delete(category: TaskCategory) {
        withContext(ioDispatcher) {
            taskCategoryDao.delete(category)
        }
    }
}
