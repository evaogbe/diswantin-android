package io.github.evaogbe.diswantin.app.data

import androidx.room.TypeConverter
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object Converters {
    @TypeConverter
    fun toInstant(value: Long?) = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun fromInstant(value: Instant?) = value?.toEpochMilli()

    @TypeConverter
    fun toLocalDate(value: String?) = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromLocalDate(value: LocalDate?) = value?.toString()

    @TypeConverter
    fun toLocalTime(value: String?) = value?.let(LocalTime::parse)

    @TypeConverter
    fun fromLocalTime(value: LocalTime?) = value?.format(DateTimeFormatter.ofPattern("HH:mm"))

    @TypeConverter
    fun toRecurrenceType(value: Int?) = value?.let { RecurrenceType.entries[it] }

    @TypeConverter
    fun fromRecurrenceType(value: RecurrenceType?) = value?.ordinal
}
