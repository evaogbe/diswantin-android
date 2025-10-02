package io.github.evaogbe.diswantin.task.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

data class CurrentTaskParams(val now: ZonedDateTime) {
    val today: LocalDate = now.toLocalDate()

    val currentTime: LocalTime = now.toLocalTime()

    val startOfToday: Instant = today.atStartOfDay(now.zone).toInstant()

    val overdueTime: LocalTime

    init {
        val defaultOverdueTime = currentTime.plusHours(1)
        overdueTime = if (defaultOverdueTime > currentTime) defaultOverdueTime else LocalTime.MAX
    }
}
