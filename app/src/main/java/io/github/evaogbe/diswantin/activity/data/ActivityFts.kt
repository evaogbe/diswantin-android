package io.github.evaogbe.diswantin.activity.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "activity_fts")
@Fts4(contentEntity = Activity::class)
data class ActivityFts(@PrimaryKey @ColumnInfo(name = "rowid") val id: Long, val name: String)
