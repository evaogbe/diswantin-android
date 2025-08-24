package io.github.evaogbe.diswantin.task.data

import androidx.room.ColumnInfo

data class TaggedTask(
    val id: Long,
    val name: String,
    @ColumnInfo("is_tagged") val isTagged: Boolean,
)
