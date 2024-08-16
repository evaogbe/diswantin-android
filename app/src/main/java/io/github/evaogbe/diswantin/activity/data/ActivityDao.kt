package io.github.evaogbe.diswantin.activity.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity ORDER BY skipped_at, created_at, id LIMIT 1")
    fun getCurrentActivity(): Flow<Activity?>

    @Query("SELECT * FROM activity WHERE id = :id LIMIT 1")
    fun findById(id: Long): Flow<Activity?>

    @Query(
        """SELECT DISTINCT activity.* 
        FROM activity 
        JOIN activity_fts ON activity_fts.name = activity.name 
        WHERE activity_fts MATCH :query || '*'
        LIMIT 20"""
    )
    fun search(query: String): Flow<List<Activity>>

    @Insert
    suspend fun insert(activity: Activity): Long

    @Update
    suspend fun update(activity: Activity)

    @Delete
    suspend fun delete(activity: Activity)
}
