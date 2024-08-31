package io.github.evaogbe.diswantin.task.data

import androidx.room.ColumnInfo
import java.time.Instant

data class TaskWithTaskList(
    val id: Long,
    val name: String,
    val deadline: Instant?,
    @ColumnInfo("scheduled_at") val scheduledAt: Instant?,
    @ColumnInfo("list_id") val listId: Long?,
    @ColumnInfo("list_name") val listName: String?,
)
