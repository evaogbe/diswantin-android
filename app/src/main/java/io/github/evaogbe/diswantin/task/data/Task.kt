package io.github.evaogbe.diswantin.task.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "task")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo("created_at") val createdAt: Instant,
    val name: String,
    @ColumnInfo("deadline_date") val deadlineDate: LocalDate? = null,
    @ColumnInfo("deadline_time") val deadlineTime: LocalTime? = null,
    @ColumnInfo("start_after_date") val startAfterDate: LocalDate? = null,
    @ColumnInfo("start_after_time") val startAfterTime: LocalTime? = null,
    @ColumnInfo("scheduled_date") val scheduledDate: LocalDate? = null,
    @ColumnInfo("scheduled_time") val scheduledTime: LocalTime? = null,
    @ColumnInfo(defaultValue = "") val note: String = "",
)
