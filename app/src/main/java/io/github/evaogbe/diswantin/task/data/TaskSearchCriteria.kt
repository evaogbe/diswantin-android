package io.github.evaogbe.diswantin.task.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.LocalDate

@Parcelize
data class TaskSearchCriteria(
    val name: String = "",
    val deadlineDateRange: Pair<LocalDate, LocalDate>? = null,
    val startAfterDateRange: Pair<LocalDate, LocalDate>? = null,
    val scheduledDateRange: Pair<LocalDate, LocalDate>? = null,
    val doneDateRange: Pair<LocalDate, LocalDate>? = null,
    val recurrenceDate: LocalDate? = null,
) : Parcelable {
    val isEmpty
        get() = name.isBlank() && listOf(
            deadlineDateRange,
            startAfterDateRange,
            scheduledDateRange,
            doneDateRange,
            recurrenceDate,
        ).all { it == null }
}
