package io.github.evaogbe.diswantin.task.data

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface TaskCategoryRepository {
    val categoryPagingData: Flow<PagingData<TaskCategory>>

    val hasCategoriesStream: Flow<Boolean>

    fun getByTaskId(taskId: Long): Flow<TaskCategory?>

    fun getCategoryWithTasksById(id: Long): Flow<TaskCategoryWithTasks>

    fun getCategoryWithTaskItemsById(id: Long): Flow<TaskCategoryWithTaskItems?>

    fun search(query: String): Flow<List<TaskCategory>>

    suspend fun create(form: NewTaskCategoryForm): TaskCategoryWithTasks

    suspend fun update(form: EditTaskCategoryForm): TaskCategoryWithTasks

    suspend fun delete(category: TaskCategory)
}
