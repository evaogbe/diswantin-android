package io.github.evaogbe.diswantin.task.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface TaskDao {
    @Query(
        """SELECT t.*
        FROM task t
        JOIN task_path p ON p.ancestor = t.id
        JOIN (
                SELECT p2.descendant, MAX(p2.depth) AS depth
                FROM task_path p2
                JOIN task t3 ON p2.ancestor = t3.id
                LEFT JOIN (
                    SELECT task_id, MAX(done_at) AS done_at
                    FROM task_completion
                    GROUP BY task_id
                ) c ON c.task_id = t3.id
                WHERE c.done_at IS NULL OR (t3.recurring AND c.done_at < :doneBefore)
                GROUP BY p2.descendant
            ) leaf ON leaf.descendant = p.descendant AND leaf.depth = p.depth
        JOIN task t2 ON p.descendant = t2.id
        WHERE t.scheduled_at IS NULL OR t.scheduled_at <= :scheduledBefore
        ORDER BY
            t2.scheduled_at IS NULL,
            t2.scheduled_at,
            t2.deadline IS NULL,
            t2.deadline,
            t.recurring DESC,
            t2.created_at,
            t2.id
        LIMIT 1"""
    )
    fun getCurrentTask(scheduledBefore: Instant, doneBefore: Instant): Flow<Task?>

    @Query("SELECT * FROM task WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<Task>

    @Query(
        """SELECT
            t.id,
            t.name,
            t.deadline,
            t.scheduled_at,
            t.recurring,
            c.done_at,
            t.list_id,
            l.name AS list_name
        FROM task t
        LEFT JOIN (
            SELECT task_id, MAX(done_at) AS done_at
            FROM task_completion
            GROUP BY task_id
        ) c ON c.task_id = t.id
        LEFT JOIN task_list l ON l.id = t.list_id
        WHERE t.id = :id 
        LIMIT 1"""
    )
    fun getTaskDetailById(id: Long): Flow<TaskDetail?>

    @Query(
        """SELECT t.*
        FROM task t
        JOIN (
            SELECT ancestor, MAX(depth) AS depth
            FROM task_path
            GROUP BY ancestor
        ) p ON p.ancestor = t.id
        WHERE t.list_id = :listId
        ORDER BY p.depth DESC
        LIMIT 20"""
    )
    fun getTasksByListId(listId: Long): Flow<List<Task>>

    @Query(
        """SELECT t.id, t.name, t.recurring, c.done_at
        FROM task t
        JOIN (
            SELECT ancestor, MAX(depth) AS depth
            FROM task_path
            GROUP BY ancestor
        ) p ON p.ancestor = t.id
        LEFT JOIN (
            SELECT task_id, MAX(done_at) AS done_at
            FROM task_completion
            GROUP BY task_id
        ) c ON c.task_id = t.id
        WHERE t.list_id = :listId
        ORDER BY p.depth DESC
        LIMIT 20"""
    )
    fun getTaskItemsByListId(listId: Long): Flow<List<TaskItem>>

    @Query(
        """SELECT DISTINCT task.*
        FROM task
        JOIN task_fts ON task_fts.name = task.name
        WHERE task_fts MATCH :query || '*'
        LIMIT 20"""
    )
    fun search(query: String): Flow<List<Task>>

    @Insert
    suspend fun insert(task: Task): Long

    @Insert
    suspend fun insertPath(path: TaskPath)

    @Insert
    suspend fun insertCompletion(completion: TaskCompletion)

    @Transaction
    suspend fun insertWithPath(task: Task): Long {
        val id = insert(task)
        insertPath(TaskPath(ancestor = id, descendant = id, depth = 0))
        return id
    }

    @Update
    suspend fun update(task: Task)

    @Query("SELECT ancestor FROM task_path WHERE descendant = :id AND depth = 1 LIMIT 1")
    suspend fun getParentId(id: Long): Long?

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
        val parentId = getParentId(id)
        val childId = getChildId(id)
        deleteById(id)
        if (parentId != null && childId != null) {
            decrementDepth(id = childId, parentId = parentId)
        }
    }
}
