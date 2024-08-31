package io.github.evaogbe.diswantin.task.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskListDao {
    @Query("SELECT * FROM task_list ORDER BY name LIMIT 20")
    fun getTaskLists(): Flow<List<TaskList>>

    @Query("SELECT * FROM task_list WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<TaskList>

    @Insert
    suspend fun insert(taskList: TaskList): Long

    @Query("UPDATE task SET list_id = :id WHERE id IN (:taskIds)")
    suspend fun addListToTasks(id: Long, taskIds: List<Long>)

    @Insert
    suspend fun insertTaskPaths(paths: List<TaskPath>)

    @Transaction
    suspend fun insertWithTasks(
        taskList: TaskList,
        taskIds: List<Long>,
        taskPaths: List<TaskPath>,
    ): Long {
        val id = insert(taskList)
        addListToTasks(id, taskIds)
        insertTaskPaths(taskPaths)
        return id
    }

    @Update
    suspend fun update(taskList: TaskList)

    @Query("UPDATE task SET list_id = NULL WHERE id in (:taskIds)")
    suspend fun removeListFromTasks(taskIds: List<Long>)

    @Query(
        """DELETE FROM task_path
        WHERE (ancestor IN (:taskIds) OR descendant IN (:taskIds)) AND depth > 0"""
    )
    suspend fun deleteTaskPathsByTaskIds(taskIds: List<Long>)

    @Transaction
    suspend fun updateWithTasks(
        taskList: TaskList,
        taskIdsToInsert: List<Long>,
        taskPathsToInsert: List<TaskPath>,
        taskIdsToRemove: List<Long>,
        taskPathTaskIdsToRemove: List<Long>,
    ) {
        update(taskList)
        removeListFromTasks(taskIdsToRemove)
        deleteTaskPathsByTaskIds(taskPathTaskIdsToRemove)
        addListToTasks(taskList.id, taskIdsToInsert)
        insertTaskPaths(taskPathsToInsert)
    }
}
