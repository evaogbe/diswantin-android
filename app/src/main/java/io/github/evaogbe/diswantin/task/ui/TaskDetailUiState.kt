package io.github.evaogbe.diswantin.task.ui

import android.os.Parcelable
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.task.data.TaskDetail
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.parcelize.Parcelize
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Parcelize
data class TaskDetailTopBarState(val taskId: Long?, val isDone: Boolean) : Parcelable

enum class TaskDetailTopBarAction {
    MarkDone, UnmarkDone, Delete
}

enum class TaskDetailUserMessage {
    MarkDoneError, UnmarkDoneError, DeleteError
}

sealed interface TaskDetailUiState {
    data object Pending : TaskDetailUiState

    data class Failure(val exception: Throwable) : TaskDetailUiState

    data class Success(
        val id: Long,
        val name: String,
        val note: String,
        val deadlineDate: LocalDate?,
        val deadlineTime: LocalTime?,
        val startAfterDate: LocalDate?,
        val startAfterTime: LocalTime?,
        val scheduledDate: LocalDate?,
        val scheduledTime: LocalTime?,
        val recurrence: TaskRecurrenceUiState?,
        val isDone: Boolean,
        val parent: TaskSummaryUiState?,
        val tags: ImmutableList<Tag>,
        val userMessage: TaskDetailUserMessage?,
    ) : TaskDetailUiState {
        val summary = TaskSummaryUiState(id = id, name = name, isDone = isDone)
    }

    data object Deleted : TaskDetailUiState

    companion object {
        fun success(
            task: TaskDetail,
            tags: List<Tag>,
            recurrences: List<TaskRecurrence>,
            userMessage: TaskDetailUserMessage?,
            doneBefore: Instant,
        ) = Success(
            id = task.id,
            name = task.name,
            note = task.note,
            deadlineDate = task.deadlineDate,
            deadlineTime = task.deadlineTime,
            startAfterDate = task.startAfterDate,
            startAfterTime = task.startAfterTime,
            scheduledDate = task.scheduledDate,
            scheduledTime = task.scheduledTime,
            recurrence = TaskRecurrenceUiState.tryFromEntities(recurrences),
            isDone = isTaskDone(
                doneAt = task.doneAt,
                doneBefore = doneBefore,
                recurring = recurrences.isNotEmpty(),
            ),
            parent = if (task.parentId != null && task.parentName != null) {
                TaskSummaryUiState(
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
            tags = tags.toImmutableList(),
            userMessage = userMessage,
        )
    }
}

fun formatDateTime(date: LocalDate?, time: LocalTime?, locale: Locale) = when {
    date != null && time != null -> {
        date.atTime(time).format(
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)
                .withLocale(locale)
        )
    }

    date != null -> date.format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    )

    time != null -> time.format(
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    )

    else -> null
}
