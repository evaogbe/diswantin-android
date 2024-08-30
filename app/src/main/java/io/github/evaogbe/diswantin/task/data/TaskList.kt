package io.github.evaogbe.diswantin.task.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "task_list", indices = [Index("name", unique = true)])
data class TaskList(@PrimaryKey(autoGenerate = true) val id: Long = 0L, val name: String)
