package io.github.evaogbe.diswantin.task.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant

@Dao
interface TaskDao {
    @Query(
        """SELECT t.*
        FROM task t
        JOIN task_path p ON p.ancestor = t.id
        JOIN (
                SELECT descendant, MAX(depth) AS depth
                FROM task_path
                GROUP BY descendant
            ) leaf ON leaf.descendant = p.descendant AND leaf.depth = p.depth
        JOIN task t2 ON p.descendant = t2.id
        WHERE t.scheduled_at IS NULL OR t.scheduled_at <= :scheduledBefore
        ORDER BY
            t2.scheduled_at IS NULL,
            t2.scheduled_at,
            t2.due_at IS NULL,
            t2.due_at,
            t2.created_at,
            t2.id
        LIMIT 1"""
    )
    fun getCurrentTask(scheduledBefore: Instant): Flow<Task?>

    @Query("SELECT * FROM task WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<Task?>

    @Query(
        """SELECT DISTINCT task.*
        FROM task
        JOIN task_fts ON task_fts.name = task.name
        WHERE task_fts MATCH :query || '*'
        LIMIT 20"""
    )
    fun search(query: String): Flow<List<Task>>

    @Query(
        """SELECT DISTINCT task.*
        FROM task
        JOIN task_fts ON task_fts.name = task.name
        WHERE task_fts MATCH :query || '*'
            AND task.list_id IS NULL
        LIMIT 20"""
    )
    fun searchSingletons(query: String): Flow<List<Task>>

    @Query(
        """SELECT t.*
        FROM task t
        JOIN task_path p ON p.ancestor = t.id
        WHERE p.descendant = :id AND depth = 1
        LIMIT 1"""
    )
    fun getParent(id: Long): Flow<Task?>

    @Query(
        """SELECT t.*
        FROM task t
        JOIN task_path p1 ON p1.descendant = t.id
        WHERE p1.ancestor IN (
            SELECT p2.ancestor
            FROM task_path p2
            WHERE p2.descendant = :id
            ORDER BY p2.depth DESC
            LIMIT 1
        )
        ORDER BY p1.depth
        LIMIT 20"""
    )
    fun getTaskListItems(id: Long): Flow<List<Task>>

    @Insert
    suspend fun insert(task: Task): Long

    @Insert
    suspend fun insertPath(path: TaskPath)

    @Transaction
    suspend fun insertWithPath(task: Task): Long {
        val id = insert(task)
        insertPath(TaskPath(ancestor = id, descendant = id, depth = 0))
        return id
    }

    @Update
    suspend fun update(task: Task)

    @Query("SELECT descendant FROM task_path WHERE ancestor = :id AND depth = 1 LIMIT 1")
    suspend fun getChildId(id: Long): Long?

    @Query("DELETE FROM task WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """UPDATE task_path
        SET depth = depth - 1
        WHERE ancestor IN (SELECT ancestor FROM task_path WHERE descendant = :parentId)
            AND descendant IN (SELECT descendant FROM task_path WHERE ancestor = :id)"""
    )
    suspend fun decrementDepth(id: Long, parentId: Long)

    @Transaction
    suspend fun deleteWithPath(id: Long) {
        val parentId = getParent(id).first()?.id
        val childId = getChildId(id)
        deleteById(id)
        if (parentId != null && childId != null) {
            decrementDepth(id = childId, parentId = parentId)
        }
    }
}
