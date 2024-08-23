package io.github.evaogbe.diswantin.activity.ui

sealed interface ActivityFormUiState {
    data object Pending : ActivityFormUiState

    data object Failure : ActivityFormUiState

    data class Success(val hasSaveError: Boolean) : ActivityFormUiState

    data object Saved : ActivityFormUiState
}
