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
    val endOfToday: ZonedDateTime,
) {
    val now: LocalDateTime = today.atTime(currentTime)

    constructor(now: ZonedDateTime) : this(
        now.toLocalDate(),
        now.toLocalTime(),
        now.with(LocalTime.MIN).toInstant(),
        now.with(LocalTime.MAX),
    )
}
