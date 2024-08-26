package io.github.evaogbe.diswantin.activity.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant

@Dao
interface ActivityDao {
    @Query(
        """SELECT a.*
        FROM activity a
        JOIN activity_path p ON p.ancestor = a.id
        JOIN (
                SELECT descendant, MAX(depth) AS depth
                FROM activity_path
                GROUP BY descendant
            ) leaf ON leaf.descendant = p.descendant AND leaf.depth = p.depth
        JOIN activity a2 ON p.descendant = a2.id
        WHERE a.scheduled_at IS NULL OR a.scheduled_at <= :scheduledBefore
        ORDER BY
            a2.scheduled_at IS NULL,
            a2.scheduled_at,
            a2.due_at IS NULL,
            a2.due_at,
            a2.created_at,
            a2.id
        LIMIT 1"""
    )
    fun getCurrentActivity(scheduledBefore: Instant): Flow<Activity?>

    @Query("SELECT * FROM activity WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<Activity?>

    @Query(
        """SELECT DISTINCT activity.*
        FROM activity
        JOIN activity_fts ON activity_fts.name = activity.name
        WHERE activity_fts MATCH :query || '*'
        LIMIT 20"""
    )
    fun search(query: String): Flow<List<Activity>>

    @Query(
        """SELECT DISTINCT activity.*
        FROM activity
        JOIN activity_fts ON activity_fts.name = activity.name
        WHERE activity_fts MATCH :query || '*'
            AND activity.id NOT IN (SELECT ancestor FROM activity_path WHERE depth > 0)
        LIMIT 20"""
    )
    fun searchTails(query: String): Flow<List<Activity>>

    @Query(
        """SELECT DISTINCT activity.*
        FROM activity
        JOIN activity_fts ON activity_fts.name = activity.name
        WHERE activity_fts MATCH :query || '*'
            AND activity.id NOT IN (SELECT ancestor FROM activity_path WHERE depth > 0)
            AND activity.id NOT IN (SELECT descendant FROM activity_path WHERE ancestor = :id)
        LIMIT 20"""
    )
    fun searchAvailableParents(query: String, id: Long): Flow<List<Activity>>

    @Query(
        """SELECT a.*
        FROM activity a
        JOIN activity_path p ON p.ancestor = a.id
        WHERE p.descendant = :id AND depth = 1
        LIMIT 1"""
    )
    fun getParent(id: Long): Flow<Activity?>

    @Query(
        """SELECT a.*
        FROM activity a
        JOIN activity_path p1 ON p1.descendant = a.id
        WHERE p1.ancestor IN (
            SELECT p2.ancestor
            FROM activity_path p2
            WHERE p2.descendant = :id
            ORDER BY p2.depth DESC
            LIMIT 1
        )
        ORDER BY p1.depth
        LIMIT 20"""
    )
    fun getChain(id: Long): Flow<List<Activity>>

    @Query("""SELECT EXISTS(SELECT * FROM activity)""")
    fun hasActivities(): Flow<Boolean>

    @Query(
        """SELECT EXISTS(
            SELECT *
            FROM activity
            WHERE id NOT IN (SELECT ancestor FROM activity_path WHERE descendant = :id)
                AND id NOT IN (SELECT descendant FROM activity_path WHERE ancestor = :id))"""
    )
    fun hasActivitiesOutsideChain(id: Long): Flow<Boolean>

    @Insert
    suspend fun insert(activity: Activity): Long

    @Insert
    suspend fun insertPath(path: ActivityPath)

    @Query(
        """INSERT INTO activity_path (ancestor, descendant, depth)
        SELECT ancestor, :id, depth + 1
        FROM activity_path
        WHERE descendant = :parentId"""
    )
    suspend fun insertAncestors(id: Long, parentId: Long)

    @Query(
        """INSERT INTO activity_path (ancestor, descendant, depth)
        SELECT p1.ancestor, p2.descendant, p1.depth + p2.depth + 1
        FROM activity_path p1, activity_path p2
        WHERE p1.descendant = :id
        AND p2.ancestor = :childId AND p2.depth > 0"""
    )
    suspend fun insertDescendants(id: Long, childId: Long)

    @Transaction
    suspend fun insertWithParent(activity: Activity, parentId: Long?): Long {
        val id = insert(activity)
        insertPath(ActivityPath(ancestor = id, descendant = id, depth = 0))
        if (parentId != null) {
            insertAncestors(id = id, parentId = parentId)
        }
        return id
    }

    @Update
    suspend fun update(activity: Activity)

    @Query(
        """DELETE FROM activity_path
        WHERE ancestor IN (SELECT ancestor FROM activity_path WHERE descendant = :parentId)
            AND descendant IN (SELECT descendant FROM activity_path WHERE ancestor = :id)"""
    )
    suspend fun deleteChain(id: Long, parentId: Long)

    @Transaction
    suspend fun updateAndReplaceParent(activity: Activity, parentId: Long?, oldParentId: Long?) {
        update(activity)
        if (oldParentId != null) {
            deleteChain(id = activity.id, parentId = oldParentId)
        }
        if (parentId != null) {
            insertAncestors(id = activity.id, parentId = parentId)
            insertDescendants(id = parentId, childId = activity.id)
        }
    }

    @Query("SELECT descendant FROM activity_path WHERE ancestor = :id AND depth = 1 LIMIT 1")
    suspend fun getChildId(id: Long): Long?

    @Query("DELETE FROM activity WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """UPDATE activity_path
        SET depth = depth - 1
        WHERE ancestor IN (SELECT ancestor FROM activity_path WHERE descendant = :parentId)
            AND descendant IN (SELECT descendant FROM activity_path WHERE ancestor = :id)"""
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
