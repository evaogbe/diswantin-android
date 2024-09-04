package io.github.evaogbe.diswantin.app.ui

import android.os.Parcelable
import io.github.evaogbe.diswantin.task.ui.CurrentTaskTopBarState
import io.github.evaogbe.diswantin.task.ui.TaskDetailTopBarState
import io.github.evaogbe.diswantin.task.ui.TaskFormTopBarState
import io.github.evaogbe.diswantin.task.ui.TaskListDetailTopBarState
import io.github.evaogbe.diswantin.task.ui.TaskListFormTopBarState
import io.github.evaogbe.diswantin.task.ui.TaskSearchTopBarState
import kotlinx.parcelize.Parcelize

sealed interface TopBarState : Parcelable {
    @Parcelize
    data object Advice : TopBarState

    @Parcelize
    data class CurrentTask(val uiState: CurrentTaskTopBarState) : TopBarState

    @Parcelize
    data class TaskDetail(val uiState: TaskDetailTopBarState) : TopBarState

    @Parcelize
    data class TaskForm(val uiState: TaskFormTopBarState) : TopBarState

    @Parcelize
    data class TaskListDetail(val uiState: TaskListDetailTopBarState) : TopBarState

    @Parcelize
    data class TaskListForm(val uiState: TaskListFormTopBarState) : TopBarState

    @Parcelize
    data object TaskLists : TopBarState

    @Parcelize
    data class TaskSearch(val uiState: TaskSearchTopBarState) : TopBarState
}
