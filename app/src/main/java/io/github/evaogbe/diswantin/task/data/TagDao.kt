package io.github.evaogbe.diswantin.task.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
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
        ORDER BY t.name
        LIMIT :size"""
    )
    fun getTagsByTaskId(taskId: Long, size: Int): Flow<List<Tag>>

    @Query(
        """SELECT tag.*
        FROM tag
        JOIN tag_fts tf ON tf.name = tag.name
        WHERE tag_fts MATCH :query
        ORDER BY tag.name
        LIMIT :size"""
    )
    fun search(query: String, size: Int): Flow<List<Tag>>

    @Query("SELECT EXISTS(SELECT * FROM tag)")
    fun hasTags(): Flow<Boolean>

    @Insert
    suspend fun insert(tag: Tag): Long

    @Update
    suspend fun update(tag: Tag)

    @Delete
    suspend fun delete(tag: Tag)
}
