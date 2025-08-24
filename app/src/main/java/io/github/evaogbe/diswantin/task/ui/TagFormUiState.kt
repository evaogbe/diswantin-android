package io.github.evaogbe.diswantin.task.ui

import android.os.Parcelable
import io.github.evaogbe.diswantin.task.data.TaggedTask
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.Parcelize

@Parcelize
data class TagFormTopBarState(
    val isNew: Boolean,
    val showSave: Boolean,
    val saveEnabled: Boolean,
) : Parcelable

enum class TagFormTopBarAction {
    Save, Close
}

sealed interface TagFormUiState {
    data object Pending : TagFormUiState

    data class Failure(val exception: Throwable) : TagFormUiState

    data class Success(
        val name: String,
        val newTasks: ImmutableList<TaggedTask>,
        val isEditing: Boolean,
        val taskOptions: ImmutableList<TaggedTask>,
        val changed: Boolean,
        val userMessage: UserMessage?,
    ) : TagFormUiState

    data object Saved : TagFormUiState
}
