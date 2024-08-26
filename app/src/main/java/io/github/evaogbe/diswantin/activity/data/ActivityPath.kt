package io.github.evaogbe.diswantin.activity.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity_path",
    indices = [Index("ancestor", "descendant", unique = true)],
    foreignKeys = [ForeignKey(
        entity = Activity::class,
        parentColumns = ["id"],
        childColumns = ["ancestor"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    ), ForeignKey(
        entity = Activity::class,
        parentColumns = ["id"],
        childColumns = ["descendant"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )]
)
data class ActivityPath(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val ancestor: Long,
    val descendant: Long,
    val depth: Int,
)
