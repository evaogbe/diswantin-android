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
            AND task.id NOT IN (SELECT ancestor FROM task_path WHERE depth > 0)
        LIMIT 20"""
    )
    fun searchTails(query: String): Flow<List<Task>>

    @Query(
        """SELECT DISTINCT task.*
        FROM task
        JOIN task_fts ON task_fts.name = task.name
        WHERE task_fts MATCH :query || '*'
            AND task.id NOT IN (SELECT ancestor FROM task_path WHERE depth > 0)
            AND task.id NOT IN (SELECT descendant FROM task_path WHERE ancestor = :id)
        LIMIT 20"""
    )
    fun searchAvailableParents(query: String, id: Long): Flow<List<Task>>

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
    fun getChain(id: Long): Flow<List<Task>>

    @Query("""SELECT EXISTS(SELECT * FROM task)""")
    fun hasTasks(): Flow<Boolean>

    @Query(
        """SELECT EXISTS(
            SELECT *
            FROM task
            WHERE id NOT IN (SELECT ancestor FROM task_path WHERE descendant = :id)
                AND id NOT IN (SELECT descendant FROM task_path WHERE ancestor = :id))"""
    )
    fun hasTasksOutsideChain(id: Long): Flow<Boolean>

    @Insert
    suspend fun insert(task: Task): Long

    @Insert
    suspend fun insertPath(path: TaskPath)

    @Query(
        """INSERT INTO task_path (ancestor, descendant, depth)
        SELECT ancestor, :id, depth + 1
        FROM task_path
        WHERE descendant = :parentId"""
    )
    suspend fun insertAncestors(id: Long, parentId: Long)

    @Query(
        """INSERT INTO task_path (ancestor, descendant, depth)
        SELECT p1.ancestor, p2.descendant, p1.depth + p2.depth + 1
        FROM task_path p1, task_path p2
        WHERE p1.descendant = :id
        AND p2.ancestor = :childId AND p2.depth > 0"""
    )
    suspend fun insertDescendants(id: Long, childId: Long)

    @Transaction
    suspend fun insertWithParent(task: Task, parentId: Long?): Long {
        val id = insert(task)
        insertPath(TaskPath(ancestor = id, descendant = id, depth = 0))
        if (parentId != null) {
            insertAncestors(id = id, parentId = parentId)
        }
        return id
    }

    @Update
    suspend fun update(task: Task)

    @Query(
        """DELETE FROM task_path
        WHERE ancestor IN (SELECT ancestor FROM task_path WHERE descendant = :parentId)
            AND descendant IN (SELECT descendant FROM task_path WHERE ancestor = :id)"""
    )
    suspend fun deleteChain(id: Long, parentId: Long)

    @Transaction
    suspend fun updateAndReplaceParent(task: Task, parentId: Long?, oldParentId: Long?) {
        update(task)
        if (oldParentId != null) {
            deleteChain(id = task.id, parentId = oldParentId)
        }
        if (parentId != null) {
            insertAncestors(id = task.id, parentId = parentId)
            insertDescendants(id = parentId, childId = task.id)
        }
    }

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
    suspend fun deleteWithChain(id: Long) {
        val parentId = getParent(id).first()?.id
        val childId = getChildId(id)
        deleteById(id)
        if (parentId != null && childId != null) {
            decrementDepth(id = childId, parentId = parentId)
        }
    }
}
