package io.github.evaogbe.diswantin.activity.ui

import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.activity.data.Activity

sealed interface ActivityDetailUiState {
    data object Pending : ActivityDetailUiState

    data object Failure : ActivityDetailUiState

    data class Success(
        val activity: Activity,
        val saveResult: Result<Unit>? = null,
        @StringRes val userMessage: Int? = null
    ) : ActivityDetailUiState

    data object Removed : ActivityDetailUiState
}
