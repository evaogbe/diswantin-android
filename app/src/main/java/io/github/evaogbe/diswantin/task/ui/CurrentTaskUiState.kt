package io.github.evaogbe.diswantin.task.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CurrentTaskTopBarState(val canSkip: Boolean) : Parcelable

enum class CurrentTaskTopBarAction {
    Refresh, Skip
}

enum class CurrentTaskUserMessage {
    SkipError, MarkDoneError
}

sealed interface CurrentTaskUiState {
    data object Pending : CurrentTaskUiState

    data class Failure(val exception: Throwable) : CurrentTaskUiState

    data class Empty(val isRefreshing: Boolean) : CurrentTaskUiState

    data class Present(
        val id: Long,
        val name: String,
        val note: String,
        val isRefreshing: Boolean,
        val canSkip: Boolean,
    ) : CurrentTaskUiState
}
