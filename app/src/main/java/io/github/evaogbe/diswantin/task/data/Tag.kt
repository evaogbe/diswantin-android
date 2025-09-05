package io.github.evaogbe.diswantin.task.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.time.Instant

@Parcelize
@Entity(tableName = "tag", indices = [Index("name", unique = true)])
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    @ColumnInfo("created_at") val createdAt: Instant,
    @ColumnInfo("updated_at") val updatedAt: Instant,
) : Parcelable
