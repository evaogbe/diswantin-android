package io.github.evaogbe.diswantin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.evaogbe.diswantin.data.ClockMonitor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import kotlin.time.Duration.Companion.seconds

abstract class BaseViewModel(clockMonitor: ClockMonitor) : ViewModel() {
    internal val clock = clockMonitor.clock.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = Clock.systemDefaultZone(),
    )
}
