package io.github.evaogbe.diswantin.app.ui

import android.os.Parcelable
import io.github.evaogbe.diswantin.task.ui.CurrentTaskTopBarAction
import io.github.evaogbe.diswantin.task.ui.CurrentTaskTopBarState
import io.github.evaogbe.diswantin.task.ui.TagDetailTopBarAction
import io.github.evaogbe.diswantin.task.ui.TagDetailTopBarState
import io.github.evaogbe.diswantin.task.ui.TaskDetailTopBarAction
import io.github.evaogbe.diswantin.task.ui.TaskDetailTopBarState
import io.github.evaogbe.diswantin.task.ui.TaskFormTopBarAction
import io.github.evaogbe.diswantin.task.ui.TaskFormTopBarState
import io.github.evaogbe.diswantin.task.ui.TaskRecurrenceFormTopBarAction
import io.github.evaogbe.diswantin.task.ui.TaskSearchTopBarAction
import kotlinx.parcelize.Parcelize

sealed interface TopBarState : Parcelable {
    @Parcelize
    data class CurrentTask(
        val uiState: CurrentTaskTopBarState,
        val action: CurrentTaskTopBarAction?,
    ) : TopBarState

    @Parcelize
    data object AdviceStart : TopBarState

    @Parcelize
    data object AdviceInner : TopBarState

    @Parcelize
    data object TagList : TopBarState

    @Parcelize
    data class TaskDetail(val uiState: TaskDetailTopBarState, val action: TaskDetailTopBarAction?) :
        TopBarState

    @Parcelize
    data class TaskForm(val uiState: TaskFormTopBarState, val action: TaskFormTopBarAction?) :
        TopBarState

    @Parcelize
    data class TagDetail(
        val uiState: TagDetailTopBarState,
        val action: TagDetailTopBarAction?,
    ) : TopBarState

    @Parcelize
    data class TaskRecurrenceForm(val action: TaskRecurrenceFormTopBarAction?) : TopBarState

    @Parcelize
    data class TaskSearch(val action: TaskSearchTopBarAction?) : TopBarState
}
