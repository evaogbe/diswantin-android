package io.github.evaogbe.diswantin.task.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "task",
    foreignKeys = [ForeignKey(
        entity = TaskCategory::class,
        parentColumns = ["id"],
        childColumns = ["category_id"],
        onDelete = ForeignKey.SET_NULL,
        onUpdate = ForeignKey.CASCADE,
    )]
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo("created_at") val createdAt: Instant,
    val name: String,
    @ColumnInfo("deadline_date") val deadlineDate: LocalDate? = null,
    @ColumnInfo("deadline_time") val deadlineTime: LocalTime? = null,
    @ColumnInfo("scheduled_date") val scheduledDate: LocalDate? = null,
    @ColumnInfo("scheduled_time") val scheduledTime: LocalTime? = null,
    @ColumnInfo(defaultValue = "0") val recurring: Boolean = false,
    @ColumnInfo("category_id", index = true) val categoryId: Long? = null,
)
