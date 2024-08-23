package io.github.evaogbe.diswantin.activity.ui

import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.activity.data.Activity

sealed interface CurrentActivityUiState {
    data object Pending : CurrentActivityUiState

    data object Failure : CurrentActivityUiState

    data object Empty : CurrentActivityUiState

    data class Present(val currentActivity: Activity, @StringRes val userMessage: Int?) :
        CurrentActivityUiState
}
