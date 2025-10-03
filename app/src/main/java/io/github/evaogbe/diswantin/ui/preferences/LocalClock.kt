package io.github.evaogbe.diswantin.ui.preferences

import androidx.compose.runtime.staticCompositionLocalOf
import java.time.Clock

val LocalClock = staticCompositionLocalOf { Clock.systemDefaultZone() }
