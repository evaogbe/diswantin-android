package io.github.evaogbe.diswantin.task.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskCategoryDao {
    @Query("SELECT * FROM task_category ORDER BY name LIMIT 20")
    fun getTaskCategories(): Flow<List<TaskCategory>>

    @Query("SELECT * FROM task_category WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<TaskCategory?>

    @Query(
        """SELECT c.*
        FROM task_category c
        JOIN task t ON t.category_id = c.id
        WHERE t.id = :taskId
        LIMIT 1"""
    )
    fun getByTaskId(taskId: Long): Flow<TaskCategory?>

    @Query(
        """SELECT c.*
        FROM task_category c
        JOIN task_category_fts cf ON cf.name = c.name
        WHERE task_category_fts MATCH :query || '*'
        LIMIT 20"""
    )
    fun search(query: String): Flow<List<TaskCategory>>

    @Query("SELECT EXISTS(SELECT * FROM task_category)")
    fun hasCategories(): Flow<Boolean>

    @Insert
    suspend fun insert(category: TaskCategory): Long

    @Update
    suspend fun update(category: TaskCategory)

    @Delete
    suspend fun delete(category: TaskCategory)

    @Query("UPDATE task SET category_id = :id WHERE id IN (:taskIds)")
    suspend fun addCategoryToTasks(id: Long, taskIds: List<Long>)

    @Transaction
    suspend fun insertWithTasks(category: TaskCategory, taskIds: List<Long>): Long {
        val id = insert(category)
        addCategoryToTasks(id, taskIds)
        return id
    }

    @Query("UPDATE task SET category_id = NULL WHERE id in (:taskIds)")
    suspend fun removeCategoryFromTasks(taskIds: List<Long>)

    @Transaction
    suspend fun updateWithTasks(
        category: TaskCategory,
        taskIdsToInsert: List<Long>,
        taskIdsToRemove: List<Long>,
    ) {
        update(category)
        removeCategoryFromTasks(taskIdsToRemove)
        addCategoryToTasks(category.id, taskIdsToInsert)
    }
}
