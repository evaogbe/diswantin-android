package io.github.evaogbe.diswantin.task.ui

import android.os.Parcelable
import io.github.evaogbe.diswantin.task.data.TaskDetail
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import kotlinx.parcelize.Parcelize
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Parcelize
data class TaskDetailTopBarState(val taskId: Long?, val isDone: Boolean) : Parcelable

enum class TaskDetailTopBarAction {
    MarkDone, UnmarkDone, Delete
}

sealed interface TaskDetailUiState {
    data object Pending : TaskDetailUiState

    data class Failure(val exception: Throwable) : TaskDetailUiState

    data class Success(
        val id: Long,
        val name: String,
        val note: String,
        val formattedDeadline: String?,
        val formattedStartAfter: String?,
        val formattedScheduledAt: String?,
        val recurrence: TaskRecurrenceUiState?,
        val isDone: Boolean,
        val parent: TaskItemUiState?,
        val categoryId: Long?,
        val categoryName: String?,
        val userMessage: UserMessage?,
    ) : TaskDetailUiState {
        val taskItem = TaskItemUiState(id = id, name = name, isDone = isDone)
    }

    data object Deleted : TaskDetailUiState

    companion object {
        fun success(
            task: TaskDetail,
            recurrence: TaskRecurrenceUiState?,
            userMessage: UserMessage?,
            doneBefore: Instant,
        ) = Success(
            id = task.id,
            name = task.name,
            note = task.note,
            formattedDeadline = formatDateTime(task.deadlineDate, task.deadlineTime),
            formattedStartAfter = formatDateTime(task.startAfterDate, task.startAfterTime),
            formattedScheduledAt = formatDateTime(task.scheduledDate, task.scheduledTime),
            recurrence = recurrence,
            isDone = isTaskDone(
                doneAt = task.doneAt,
                doneBefore = doneBefore,
                recurring = recurrence != null,
            ),
            parent = if (task.parentId != null && task.parentName != null) {
                TaskItemUiState(
                    id = task.parentId,
                    name = task.parentName,
                    isDone = isTaskDone(
                        doneAt = task.parentDoneAt,
                        doneBefore = doneBefore,
                        recurring = task.parentRecurring,
                    ),
                )
            } else {
                null
            },
            categoryId = task.categoryId,
            categoryName = task.categoryName,
            userMessage = userMessage,
        )
    }
}

fun formatDateTime(date: LocalDate?, time: LocalTime?) = when {
    date != null && time != null -> {
        date.atTime(time).format(
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT),
        )
    }

    date != null -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
    time != null -> time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    else -> null
}
