package io.github.evaogbe.diswantin.ui.preferences

import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

val LocalLocale = staticCompositionLocalOf { Locale.getDefault() }
