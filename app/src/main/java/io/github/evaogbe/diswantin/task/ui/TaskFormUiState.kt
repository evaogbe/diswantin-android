package io.github.evaogbe.diswantin.task.ui

import android.os.Parcelable
import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.task.data.Task
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

    data object Failure : TaskFormUiState

    data class Success(
        val deadlineDate: LocalDate?,
        val deadlineTime: LocalTime?,
        val scheduledDate: LocalDate?,
        val scheduledTime: LocalTime?,
        val recurrence: TaskRecurrenceUiState?,
        val showParentTaskField: Boolean,
        val parentTask: Task?,
        val parentTaskOptions: ImmutableList<Task>,
        val hasSaveError: Boolean,
        @StringRes val userMessage: Int?,
    ) : TaskFormUiState

    data object Saved : TaskFormUiState
}
