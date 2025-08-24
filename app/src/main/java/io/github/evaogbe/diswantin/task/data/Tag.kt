package io.github.evaogbe.diswantin.task.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tag", indices = [Index("name", unique = true)])
data class Tag(@PrimaryKey(autoGenerate = true) val id: Long = 0L, val name: String)
