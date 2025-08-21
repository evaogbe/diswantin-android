package io.github.evaogbe.diswantin.task.data

import androidx.room.ColumnInfo
import java.time.Instant

data class TaskItemData(
    val id: Long,
    val name: String,
    val recurring: Boolean,
    @ColumnInfo("done_at") val doneAt: Instant?,
)
