package io.github.evaogbe.diswantin.task.data

import java.time.LocalDate

data class TaskSearchCriteria(
    val name: String = "",
    val deadlineDate: LocalDate? = null,
    val scheduledDate: LocalDate? = null,
) {
    val isEmpty = name.isBlank() && deadlineDate == null && scheduledDate == null
}
