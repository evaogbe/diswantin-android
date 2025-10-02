package io.github.evaogbe.diswantin.task.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.Instant
import java.time.LocalDate

@Parcelize
data class TaskSearchCriteria(
    val name: String = "",
    val deadlineDateRange: Pair<LocalDate, LocalDate>? = null,
    val startAfterDateRange: Pair<LocalDate, LocalDate>? = null,
    val scheduledDateRange: Pair<LocalDate, LocalDate>? = null,
    val doneRange: Pair<Instant, Instant>? = null,
    val recurrenceDate: LocalDate? = null,
) : Parcelable {
    val isEmpty
        get() = name.isBlank() && listOf(
            deadlineDateRange,
            startAfterDateRange,
            scheduledDateRange,
            doneRange,
            recurrenceDate,
        ).all { it == null }
}
