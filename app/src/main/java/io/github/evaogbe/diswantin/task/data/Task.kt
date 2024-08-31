package io.github.evaogbe.diswantin.task.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "task",
    foreignKeys = [ForeignKey(
        entity = TaskList::class,
        parentColumns = ["id"],
        childColumns = ["list_id"],
        onDelete = ForeignKey.SET_NULL,
        onUpdate = ForeignKey.CASCADE,
    )]
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo("created_at") val createdAt: Instant,
    val name: String,
    val deadline: Instant? = null,
    @ColumnInfo("scheduled_at") val scheduledAt: Instant? = null,
    @ColumnInfo("done_at") val doneAt: Instant? = null,
    @ColumnInfo(defaultValue = "0") val recurring: Boolean = false,
    @ColumnInfo("list_id", index = true) val listId: Long? = null,
)
