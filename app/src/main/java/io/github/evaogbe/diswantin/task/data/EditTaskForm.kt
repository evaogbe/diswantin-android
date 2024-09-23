package io.github.evaogbe.diswantin.task.data

import java.time.LocalDate
import java.time.LocalTime

sealed interface PathUpdateType {
    data object Keep : PathUpdateType

    data object Remove : PathUpdateType

    data class Replace(val id: Long) : PathUpdateType
}

data class EditTaskForm(
    private val name: String,
    private val deadlineDate: LocalDate?,
    private val deadlineTime: LocalTime?,
    private val scheduledDate: LocalDate?,
    private val scheduledTime: LocalTime?,
    private val categoryId: Long?,
    private val recurrences: Collection<TaskRecurrence>,
    val parentUpdateType: PathUpdateType,
    private val existingTask: Task,
    private val existingRecurrences: Collection<TaskRecurrence>,
) {
    val recurrencesToRemove: Collection<TaskRecurrence>

    val recurrencesToAdd: Collection<TaskRecurrence>

    init {
        require(name.isNotBlank()) { "Name must be present" }
        require(
            (deadlineDate == null && deadlineTime == null) ||
                    (scheduledDate == null && scheduledTime == null)
        ) {
            """Must not have both deadline fields and scheduled fields, but got 
                |deadlineDate: $deadlineDate, 
                |deadlineTime: $deadlineTime, 
                |scheduledDate: $scheduledDate, and
                |scheduledTime: $scheduledTime""".trimMargin()
        }
        require(scheduledTime != null || scheduledDate == null) {
            "Must have scheduledTime if scheduledDate is set, but got scheduledDate: $scheduledDate"
        }

        val newRecurrenceSet = recurrences.toSet()
        val oldRecurrenceSet = existingRecurrences.map { it.copy(id = 0L) }.toSet()
        recurrencesToRemove = existingRecurrences.filterNot { it.copy(id = 0L) in newRecurrenceSet }
        recurrencesToAdd = recurrences.filterNot { it in oldRecurrenceSet }
    }

    val updatedTask = existingTask.copy(
        name = name.trim(),
        deadlineDate = deadlineDate,
        deadlineTime = deadlineTime,
        scheduledDate = scheduledDate,
        scheduledTime = scheduledTime,
        categoryId = categoryId,
    )
}
