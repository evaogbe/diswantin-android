package io.github.evaogbe.diswantin.task.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "task_category_fts")
@Fts4(contentEntity = TaskCategory::class)
data class TaskCategoryFts(@PrimaryKey @ColumnInfo(name = "rowid") val id: Long, val name: String)
