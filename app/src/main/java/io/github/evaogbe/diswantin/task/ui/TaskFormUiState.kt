package io.github.evaogbe.diswantin.task.ui

import android.os.Parcelable
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
import java.time.LocalTime

@Parcelize
data class TaskFormTopBarState(
    val isNew: Boolean,
    val showSave: Boolean,
    val saveEnabled: Boolean,
) : Parcelable

enum class TaskFormTopBarAction {
    Save
}

sealed interface TaskFormUiState {
    data object Pending : TaskFormUiState

    data class Failure(val exception: Throwable) : TaskFormUiState

    data class Success(
        val recurrence: TaskRecurrenceUiState?,
        val deadlineDate: LocalDate?,
        val deadlineTime: LocalTime?,
        val startAfterDate: LocalDate?,
        val startAfterTime: LocalTime?,
        val scheduledDate: LocalDate?,
        val scheduledTime: LocalTime?,
        val showCategoryField: Boolean,
        val category: TaskCategory?,
        val categoryOptions: ImmutableList<TaskCategory>,
        val showParentTaskField: Boolean,
        val parentTask: Task?,
        val parentTaskOptions: ImmutableList<Task>,
        val userMessage: UserMessage?,
    ) : TaskFormUiState {
        val canSchedule =
            listOf(deadlineDate, deadlineTime, startAfterDate, startAfterTime).all { it == null }
    }

    data object Saved : TaskFormUiState
}
