package io.github.evaogbe.diswantin.testing

import io.github.evaogbe.diswantin.data.ClockMonitor
import kotlinx.coroutines.flow.flowOf
import java.time.Clock
import java.time.ZonedDateTime

class FixedClockMonitor(fixedDateTime: ZonedDateTime = ZonedDateTime.now()) : ClockMonitor {
    override val clock = flowOf(Clock.fixed(fixedDateTime.toInstant(), fixedDateTime.zone))
}
