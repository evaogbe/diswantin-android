package io.github.evaogbe.diswantin.task.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Dao
interface TaskDao {
    @Query(
        """SELECT
            t.*,
            t2.scheduled_date AS scheduled_date_priority,
            t2.scheduled_time AS scheduled_time_priority,
            t2.deadline_date AS deadline_date_priority,
            t2.deadline_time AS deadline_time_priority,
            t2.recurring AS recurring_priority,
            t2.created_at AS created_at_priority,
            t2.id AS id_priority
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
        WHERE (t.scheduled_date IS NULL OR t.scheduled_date <= :scheduledDateBefore)
            AND (t.scheduled_time IS NULL OR t.scheduled_time <= :scheduledTimeBefore)
        ORDER BY
            scheduled_date_priority IS NULL,
            scheduled_date_priority,
            scheduled_time_priority IS NULL,
            scheduled_time_priority,
            recurring_priority DESC,
            deadline_date_priority IS NULL,
            deadline_date_priority,
            deadline_time_priority IS NULL,
            deadline_time_priority,
            created_at_priority,
            id_priority
        LIMIT 20"""
    )
    fun getTaskPriorities(
        scheduledDateBefore: LocalDate,
        scheduledTimeBefore: LocalTime,
        doneBefore: Instant,
    ): Flow<List<TaskPriority>>

    @Query("SELECT * FROM task WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<Task>

    @Query(
        """SELECT
            t.id,
            t.name,
            t.deadline_date,
            t.deadline_time,
            t.scheduled_date,
            t.scheduled_time,
            t.recurring,
            com.done_at,
            t.category_id,
            cat.name AS category_name,
            t2.id AS parent_id,
            t2.name AS parent_name
        FROM task t
        LEFT JOIN (
            SELECT task_id, MAX(done_at) AS done_at
            FROM task_completion
            GROUP BY task_id
        ) com ON com.task_id = t.id
        LEFT JOIN task_category cat ON cat.id = t.category_id
        LEFT JOIN task_path p ON p.descendant = t.id AND p.depth = 1
        LEFT JOIN task t2 ON t2.id = p.ancestor
        WHERE t.id = :id 
        LIMIT 1"""
    )
    fun getTaskDetailById(id: Long): Flow<TaskDetail?>

    @Query(
        """SELECT t.*
        FROM task t
        JOIN task_path p ON p.ancestor = t.id
        WHERE p.descendant = :id AND p.depth = 1
        LIMIT 1"""
    )
    fun getParent(id: Long): Flow<Task?>

    @Query("SELECT * FROM task WHERE category_id = :categoryId ORDER BY name LIMIT 20")
    fun getTasksByCategoryId(categoryId: Long): Flow<List<Task>>

    @Query(
        """SELECT t.id, t.name, t.recurring, c.done_at
        FROM task t
        LEFT JOIN (
            SELECT task_id, MAX(done_at) AS done_at
            FROM task_completion
            GROUP BY task_id
        ) c ON c.task_id = t.id
        WHERE t.category_id = :categoryId
        ORDER BY c.done_at, t.name
        LIMIT 20"""
    )
    fun getTaskItemsByCategoryId(categoryId: Long): Flow<List<TaskItem>>

    @Query(
        """SELECT DISTINCT task.*
        FROM task
        JOIN task_fts ON task_fts.name = task.name
        WHERE task_fts MATCH :query || '*'
        LIMIT 20"""
    )
    fun search(query: String): Flow<List<Task>>

    @Query("SELECT COUNT(*) FROM task")
    fun getCount(): Flow<Long>

    @Query(
        """SELECT COUNT(*)
        FROM task t
        LEFT JOIN (
            SELECT task_id, MAX(done_at) AS done_at
            FROM task_completion
            GROUP BY task_id
        ) c ON c.task_id = t.id
        WHERE c.done_at IS NULL OR (t.recurring AND c.done_at < :doneBefore)"""
    )
    fun getUndoneCount(doneBefore: Instant): Flow<Long>

    @Insert
    suspend fun insert(task: Task): Long

    @Insert
    suspend fun insertPath(path: TaskPath)

    @Insert
    suspend fun insertCompletion(completion: TaskCompletion)

    @Query(
        """INSERT INTO task_path
        (ancestor, descendant, depth)
        SELECT a.ancestor, d.descendant, a.depth + d.depth + 1
        FROM (SELECT ancestor, depth FROM task_path WHERE descendant = :parentId) a,
            (SELECT descendant, depth FROM task_path WHERE ancestor = :childId) d"""
    )
    suspend fun insertChain(parentId: Long, childId: Long)

    @Transaction
    suspend fun insertWithParent(task: Task, parentId: Long?): Long {
        val id = insert(task)
        insertPath(TaskPath(ancestor = id, descendant = id, depth = 0))
        if (parentId != null) {
            insertChain(parentId = parentId, childId = id)
        }
        return id
    }

    @Update
    suspend fun update(task: Task)

    @Query(
        """SELECT *
        FROM task_path
        WHERE (ancestor = :taskId1 AND descendant = :taskId2)
            OR (ancestor = :taskId2 AND descendant = :taskId1)
        LIMIT 1"""
    )
    suspend fun getConnectingPath(taskId1: Long, taskId2: Long): TaskPath?

    @Query("DELETE FROM task_path WHERE (ancestor = :taskId OR descendant = :taskId) AND depth > 0")
    suspend fun deletePathsByTaskId(taskId: Long)

    @Query(
        """UPDATE task_path
        SET depth = depth + 1
        WHERE ancestor IN (SELECT ancestor FROM task_path WHERE descendant = :descendant)
            AND descendant IN (SELECT descendant FROM task_path WHERE ancestor = :descendant)
            AND depth > 0"""
    )
    suspend fun incrementDepth(descendant: Long)

    @Transaction
    suspend fun connectPath(parentId: Long, childId: Long) {
        val connectingPath = getConnectingPath(parentId, childId)
        when (connectingPath?.ancestor) {
            parentId -> {}
            childId -> {
                val existingParentId = getParent(childId).first()?.id
                val existingChildIds = getChildIds(childId)
                if (existingParentId != null && existingChildIds.isNotEmpty()) {
                    decrementDepth(parentId = existingParentId, childIds = existingChildIds)
                }
                deletePathsByTaskId(childId)
                insertChain(parentId = parentId, childId = childId)
            }

            null -> {
                if (getParent(childId).first() != null) {
                    incrementDepth(childId)
                }

                insertChain(parentId = parentId, childId = childId)
            }
        }
    }

    @Query(
        """DELETE FROM task_path
        WHERE ancestor IN (SELECT ancestor FROM task_path WHERE descendant = :id AND depth > 0)
            AND descendant IN (SELECT descendant FROM task_path WHERE ancestor = :id)"""
    )
    suspend fun deleteAncestors(id: Long)

    @Transaction
    suspend fun updateWithoutParent(task: Task) {
        update(task)
        deleteAncestors(task.id)
    }

    @Transaction
    suspend fun updateWithParent(task: Task, parentId: Long) {
        update(task)
        connectPath(parentId = parentId, childId = task.id)
    }

    @Query("SELECT descendant FROM task_path WHERE ancestor = :id AND depth = 1")
    suspend fun getChildIds(id: Long): List<Long>

    @Query("DELETE FROM task WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """UPDATE task_path
        SET depth = depth - 1
        WHERE ancestor IN (SELECT ancestor FROM task_path WHERE descendant = :parentId)
            AND descendant IN (SELECT descendant FROM task_path WHERE ancestor IN (:childIds))"""
    )
    suspend fun decrementDepth(parentId: Long, childIds: List<Long>)

    @Transaction
    suspend fun deleteWithPath(id: Long) {
        val parentId = getParent(id).first()?.id
        val childIds = getChildIds(id)
        deleteById(id)
        if (parentId != null && childIds.isNotEmpty()) {
            decrementDepth(parentId = parentId, childIds = childIds)
        }
    }
}
