package io.github.evaogbe.diswantin.data

import java.time.temporal.WeekFields
import java.util.Locale

fun weekOfMonthField(locale: Locale = Locale.getDefault()) =
    WeekFields.of(WeekFields.of(locale).firstDayOfWeek, 1).weekOfMonth()
