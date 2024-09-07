package io.github.evaogbe.diswantin.task.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

data class CurrentTaskParams(
    val scheduledDateBefore: LocalDate,
    val scheduledTimeBefore: LocalTime,
    val doneBefore: Instant,
    val recurringDeadline: ZonedDateTime,
) {
    constructor(now: ZonedDateTime) : this(
        now.toLocalDate(),
        now.plusHours(1).toLocalTime(),
        now.with(LocalTime.MIN).toInstant(),
        now.with(LocalTime.MAX),
    )
}
