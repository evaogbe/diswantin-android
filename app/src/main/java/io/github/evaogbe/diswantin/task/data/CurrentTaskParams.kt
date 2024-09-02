package io.github.evaogbe.diswantin.task.data

import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime

data class CurrentTaskParams(
    val scheduledBefore: Instant,
    val doneBefore: Instant,
    val recurringDeadline: ZonedDateTime,
) {
    constructor(now: ZonedDateTime) : this(
        now.plusHours(1).toInstant(),
        now.with(LocalTime.MIN).toInstant(),
        now.with(LocalTime.MAX),
    )
}
