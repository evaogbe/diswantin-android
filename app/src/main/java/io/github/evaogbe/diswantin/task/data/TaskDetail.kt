package io.github.evaogbe.diswantin.task.data

import androidx.room.ColumnInfo
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class TaskDetail(
    val id: Long,
    val name: String,
    @ColumnInfo("deadline_date") val deadlineDate: LocalDate?,
    @ColumnInfo("deadline_time") val deadlineTime: LocalTime?,
    @ColumnInfo("start_after_date") val startAfterDate: LocalDate?,
    @ColumnInfo("start_after_time") val startAfterTime: LocalTime?,
    @ColumnInfo("scheduled_date") val scheduledDate: LocalDate?,
    @ColumnInfo("scheduled_time") val scheduledTime: LocalTime?,
    @ColumnInfo("done_at") val doneAt: Instant?,
    @ColumnInfo("category_id") val categoryId: Long?,
    @ColumnInfo("category_name") val categoryName: String?,
    @ColumnInfo("parent_id") val parentId: Long?,
    @ColumnInfo("parent_name") val parentName: String?,
)
