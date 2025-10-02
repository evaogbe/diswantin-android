package io.github.evaogbe.diswantin.task.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

data class CurrentTaskParams(
    val today: LocalDate,
    val currentTime: LocalTime,
    val startOfToday: Instant,
    val overdueTime: LocalTime,
) {
    val now: LocalDateTime = today.atTime(currentTime)

    companion object {
        fun create(now: ZonedDateTime): CurrentTaskParams {
            val today = now.toLocalDate()
            val currentTime = now.toLocalTime()
            val startOfToday = now.toLocalDate().atStartOfDay(now.zone).toInstant()
            val overdueTime = currentTime.plusHours(1)
            return CurrentTaskParams(
                today = today,
                currentTime = currentTime,
                startOfToday = startOfToday,
                overdueTime = if (overdueTime > currentTime) overdueTime else LocalTime.MAX,
            )
        }
    }
}
