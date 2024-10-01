package io.github.evaogbe.diswantin.task.data

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class NewTaskForm(
    private val name: String,
    private val note: String,
    private val deadlineDate: LocalDate?,
    private val deadlineTime: LocalTime?,
    private val startAfterDate: LocalDate?,
    private val startAfterTime: LocalTime?,
    private val scheduledDate: LocalDate?,
    private val scheduledTime: LocalTime?,
    private val categoryId: Long?,
    val recurrences: Collection<TaskRecurrence>,
    val parentTaskId: Long?,
    private val clock: Clock,
) {
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
        require(scheduledTime != null || scheduledDate == null) {
            "Must have scheduledTime if scheduledDate is set, but got scheduledDate: $scheduledDate"
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
    }

    val newTask
        get() = Task(
            createdAt = Instant.now(clock),
            name = name.trim(),
            note = note.trim(),
            deadlineDate = deadlineDate,
            deadlineTime = deadlineTime,
            startAfterDate = startAfterDate,
            startAfterTime = startAfterTime,
            scheduledDate = scheduledDate,
            scheduledTime = scheduledTime,
            categoryId = categoryId,
        )
}
