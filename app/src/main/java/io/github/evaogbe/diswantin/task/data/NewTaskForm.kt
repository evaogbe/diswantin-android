package io.github.evaogbe.diswantin.task.data

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class NewTaskForm(
    private val name: String,
    private val deadlineDate: LocalDate?,
    private val deadlineTime: LocalTime?,
    private val scheduledDate: LocalDate?,
    private val scheduledTime: LocalTime?,
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
        require(scheduledTime != null || scheduledDate == null) {
            "Must have scheduledTime if scheduledDate is set, but got scheduledDate: $scheduledDate"
        }
    }

    val newTask
        get() = Task(
            createdAt = Instant.now(clock),
            name = name,
            deadlineDate = deadlineDate,
            deadlineTime = deadlineTime,
            scheduledDate = scheduledDate,
            scheduledTime = scheduledTime,
        )
}
