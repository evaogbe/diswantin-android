package io.github.evaogbe.diswantin.task.data

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface TaskCategoryRepository {
    val categoryPagingData: Flow<PagingData<TaskCategory>>

    val hasCategoriesStream: Flow<Boolean>

    fun getById(id: Long): Flow<TaskCategory?>

    fun getByTaskId(taskId: Long): Flow<TaskCategory?>

    fun search(query: String): Flow<List<TaskCategory>>

    suspend fun create(form: NewTaskCategoryForm): TaskCategoryWithTasks

    suspend fun update(form: EditTaskCategoryForm): TaskCategoryWithTasks

    suspend fun delete(category: TaskCategory)
}
