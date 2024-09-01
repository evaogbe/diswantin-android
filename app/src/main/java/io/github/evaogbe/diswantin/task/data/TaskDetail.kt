package io.github.evaogbe.diswantin.task.data

import androidx.room.ColumnInfo
import java.time.Instant

data class TaskDetail(
    val id: Long,
    val name: String,
    val deadline: Instant?,
    @ColumnInfo("scheduled_at") val scheduledAt: Instant?,
    val recurring: Boolean,
    @ColumnInfo("done_at") val doneAt: Instant?,
    @ColumnInfo("list_id") val listId: Long?,
    @ColumnInfo("list_name") val listName: String?,
)
