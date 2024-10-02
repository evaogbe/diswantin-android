package io.github.evaogbe.diswantin.task.data

import java.time.LocalDate

data class TaskSearchCriteria(
    val name: String = "",
    val deadlineDateRange: Pair<LocalDate, LocalDate>? = null,
    val startAfterDateRange: Pair<LocalDate, LocalDate>? = null,
    val scheduledDateRange: Pair<LocalDate, LocalDate>? = null,
) {
    val isEmpty =
        name.isBlank() &&
                deadlineDateRange == null &&
                startAfterDateRange == null &&
                scheduledDateRange == null
}
