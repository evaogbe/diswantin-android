package io.github.evaogbe.diswantin.app.data

import androidx.room.TypeConverter
import java.time.Instant

object Converters {
    @TypeConverter
    fun toInstant(value: Long?) = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun fromInstant(value: Instant?) = value?.toEpochMilli()
}
