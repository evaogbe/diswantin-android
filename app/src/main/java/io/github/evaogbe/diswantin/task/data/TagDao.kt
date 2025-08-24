package io.github.evaogbe.diswantin.task.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tag ORDER BY name")
    fun getTagPagingSource(): PagingSource<Int, Tag>

    @Query("SELECT * FROM tag WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<Tag?>

    @Query(
        """SELECT t.*
        FROM tag t
        JOIN task_tag tt ON tt.tag_id = t.id
        WHERE tt.task_id = :taskId
        LIMIT 20"""
    )
    fun getTagsByTaskId(taskId: Long): Flow<List<Tag>>

    @Query(
        """SELECT tag.*
        FROM tag
        JOIN tag_fts tf ON tf.name = tag.name
        WHERE tag_fts MATCH :query
        LIMIT :size"""
    )
    fun search(query: String, size: Int): Flow<List<Tag>>

    @Query("SELECT EXISTS(SELECT * FROM tag)")
    fun hasTags(): Flow<Boolean>

    @Insert
    suspend fun insert(tag: Tag): Long

    @Insert
    suspend fun insertTaskTags(taskTag: Collection<TaskTag>)

    @Update
    suspend fun update(tag: Tag)

    @Delete
    suspend fun delete(tag: Tag)

    @Transaction
    suspend fun insertWithTasks(tag: Tag, taskIds: Set<Long>): Long {
        val id = insert(tag)
        insertTaskTags(taskIds.map { TaskTag(taskId = it, tagId = id) })
        return id
    }

    @Query("DELETE FROM task_tag WHERE tag_id == :tagId AND task_id in (:taskIds)")
    suspend fun removeTagFromTasks(tagId: Long, taskIds: Set<Long>)

    @Transaction
    suspend fun updateWithTasks(tag: Tag, taskIdsToInsert: Set<Long>, taskIdsToRemove: Set<Long>) {
        update(tag)
        removeTagFromTasks(tag.id, taskIdsToRemove)
        insertTaskTags(taskIdsToInsert.map { TaskTag(taskId = it, tagId = tag.id) })
    }
}
