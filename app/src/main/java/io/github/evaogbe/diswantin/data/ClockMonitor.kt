package io.github.evaogbe.diswantin.data

import kotlinx.coroutines.flow.Flow
import java.time.Clock

interface ClockMonitor {
    val clock: Flow<Clock>
}
