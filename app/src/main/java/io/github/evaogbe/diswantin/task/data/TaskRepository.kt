package io.github.evaogbe.diswantin.task.data

import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface TaskRepository {
    fun getCurrentTask(scheduledBefore: Instant): Flow<Task?>

    fun getById(id: Long): Flow<Task?>

    fun search(
        query: String,
        tailsOnly: Boolean = false,
        excludeChainFor: Long? = null
    ): Flow<List<Task>>

    fun getChain(id: Long): Flow<List<Task>>

    fun getParent(id: Long): Flow<Task?>

    fun hasTasks(excludeChainFor: Long?): Flow<Boolean>

    suspend fun create(form: NewTaskForm): Task

    suspend fun update(form: EditTaskForm)

    suspend fun remove(id: Long)
}
