package io.github.evaogbe.diswantin.task.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "tag", indices = [Index("name", unique = true)])
data class Tag(@PrimaryKey(autoGenerate = true) val id: Long = 0L, val name: String) : Parcelable
