package io.github.evaogbe.diswantin.task.ui

import android.os.Parcelable
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import kotlinx.parcelize.Parcelize

@Parcelize
data class TagDetailTopBarState(val tagId: Long?) : Parcelable

enum class TagDetailTopBarAction {
    Delete
}

sealed interface TagDetailUiState {
    data object Pending : TagDetailUiState

    data class Failure(val exception: Throwable) : TagDetailUiState

    data class Success(
        val tag: Tag,
        val userMessage: UserMessage?,
    ) : TagDetailUiState

    data object Deleted : TagDetailUiState
}
