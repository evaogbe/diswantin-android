package io.github.evaogbe.diswantin.task.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "task_completion",
    indices = [Index("task_id", "done_at", unique = true)],
    foreignKeys = [ForeignKey(
        entity = Task::class,
        parentColumns = ["id"],
        childColumns = ["task_id"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE,
    )]
)
data class TaskCompletion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo("task_id") val taskId: Long,
    @ColumnInfo("done_at") val doneAt: Instant,
)
