package io.github.evaogbe.diswantin.activity.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "activity")
data class Activity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo("created_at") val createdAt: Instant,
    val name: String,
    @ColumnInfo("skipped_at") val skippedAt: Instant? = null,
    @ColumnInfo("due_at") val dueAt: Instant? = null,
)
