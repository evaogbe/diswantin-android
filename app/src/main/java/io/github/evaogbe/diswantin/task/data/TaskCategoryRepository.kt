package io.github.evaogbe.diswantin.task.data

import kotlinx.coroutines.flow.Flow

interface TaskCategoryRepository {
    val categoryListStream: Flow<List<TaskCategory>>

    fun getCategoryWithTasksById(id: Long): Flow<TaskCategoryWithTasks>

    fun getCategoryWithTaskItemsById(id: Long): Flow<TaskCategoryWithTaskItems?>

    suspend fun create(form: NewTaskCategoryForm): TaskCategoryWithTasks

    suspend fun update(form: EditTaskCategoryForm): TaskCategoryWithTasks

    suspend fun delete(category: TaskCategory)
}
