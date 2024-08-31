package io.github.evaogbe.diswantin.task.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_path",
    indices = [Index("ancestor", "descendant", unique = true)],
    foreignKeys = [ForeignKey(
        entity = Task::class,
        parentColumns = ["id"],
        childColumns = ["ancestor"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    ), ForeignKey(
        entity = Task::class,
        parentColumns = ["id"],
        childColumns = ["descendant"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )]
)
data class TaskPath(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val ancestor: Long,
    @ColumnInfo(index = true) val descendant: Long,
    val depth: Int,
)
