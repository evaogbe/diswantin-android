package io.github.evaogbe.diswantin.task.data

import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface TaskRepository {
    fun getCurrentTask(scheduledBefore: Instant): Flow<Task?>

    fun getById(id: Long): Flow<Task?>

    fun getTaskWithTaskListById(id: Long): Flow<TaskWithTaskList?>

    fun search(query: String, singletonsOnly: Boolean = false): Flow<List<Task>>

    suspend fun create(form: NewTaskForm): Task

    suspend fun update(form: EditTaskForm): Task

    suspend fun delete(id: Long)
}
