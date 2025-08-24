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
    private val note: String,
    private val deadlineDate: LocalDate?,
    private val deadlineTime: LocalTime?,
    private val startAfterDate: LocalDate?,
    private val startAfterTime: LocalTime?,
    private val scheduledDate: LocalDate?,
    private val scheduledTime: LocalTime?,
    private val tagIds: Set<Long>,
    private val recurrences: Collection<TaskRecurrence>,
    val parentUpdateType: PathUpdateType,
    private val existingTask: Task,
    private val existingTagIds: Set<Long>,
    private val existingRecurrences: Collection<TaskRecurrence>,
) {
    val recurrencesToRemove: Collection<TaskRecurrence>

    val recurrencesToAdd: Collection<TaskRecurrence>

    val tagIdsToRemove = existingTagIds - tagIds

    val tagIdsToAdd = tagIds - existingTagIds

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
        require(
            (startAfterDate == null && startAfterTime == null) ||
                    (scheduledDate == null && scheduledTime == null)
        ) {
            """Must not have both start after fields and scheduled fields, but got 
                |startAfterDate: $startAfterDate, 
                |startAfterTime: $startAfterTime, 
                |scheduledDate: $scheduledDate, and
                |scheduledTime: $scheduledTime""".trimMargin()
        }
        require(scheduledTime == null || scheduledDate != null || recurrences.isNotEmpty()) {
            """Must have scheduledDate if scheduledTime is set for non-recurring tasks, but got
                |scheduledTime: $scheduledTime""".trimMargin()
        }
        require(recurrences.isEmpty() || deadlineDate == null) {
            """Must not set deadline date for recurring tasks, but got 
                |deadlineDate: $deadlineDate, 
                |recurrences: $recurrences""".trimMargin()
        }
        require(recurrences.isEmpty() || startAfterDate == null) {
            """Must not set start after date for recurring tasks, but got 
                |startAfterDate: $startAfterDate, 
                |recurrences: $recurrences""".trimMargin()
        }
        require(recurrences.isEmpty() || scheduledDate == null) {
            """Must not set scheduled date for recurring tasks, but got 
                |scheduledDate: $scheduledDate, 
                |recurrences: $recurrences""".trimMargin()
        }

        val newRecurrenceSet = recurrences.toSet()
        val oldRecurrenceSet = existingRecurrences.map { it.copy(id = 0L) }.toSet()
        recurrencesToRemove = existingRecurrences.filterNot { it.copy(id = 0L) in newRecurrenceSet }
        recurrencesToAdd = recurrences.filterNot { it in oldRecurrenceSet }
    }

    val updatedTask = existingTask.copy(
        name = name.trim(),
        note = note.trim(),
        deadlineDate = deadlineDate,
        deadlineTime = deadlineTime,
        startAfterDate = startAfterDate,
        startAfterTime = startAfterTime,
        scheduledDate = scheduledDate,
        scheduledTime = scheduledTime,
    )
}
