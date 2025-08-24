package io.github.evaogbe.diswantin.task.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_tag",
    indices = [Index("task_id", "tag_id", unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        )
    ],
)
data class TaskTag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo("task_id") val taskId: Long,
    @ColumnInfo("tag_id", index = true) val tagId: Long,
)
