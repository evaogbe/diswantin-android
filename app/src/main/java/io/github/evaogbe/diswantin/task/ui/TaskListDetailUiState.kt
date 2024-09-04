package io.github.evaogbe.diswantin.task.ui

import android.os.Parcelable
import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.task.data.TaskList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.Parcelize

@Parcelize
data class TaskListDetailTopBarState(val taskListId: Long?, val onDeleteTaskList: () -> Unit) :
    Parcelable

data class TaskItemState(
    val id: Long,
    val name: String,
    val recurring: Boolean,
    val isDone: Boolean,
)

sealed interface TaskListDetailUiState {
    data object Pending : TaskListDetailUiState

    data object Failure : TaskListDetailUiState

    data class Success(
        val taskList: TaskList,
        val tasks: ImmutableList<TaskItemState>,
        @StringRes val userMessage: Int?
    ) : TaskListDetailUiState

    data object Deleted : TaskListDetailUiState
}
