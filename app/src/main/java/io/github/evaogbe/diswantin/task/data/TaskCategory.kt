package io.github.evaogbe.diswantin.task.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "task_category", indices = [Index("name", unique = true)])
data class TaskCategory(@PrimaryKey(autoGenerate = true) val id: Long = 0L, val name: String)
