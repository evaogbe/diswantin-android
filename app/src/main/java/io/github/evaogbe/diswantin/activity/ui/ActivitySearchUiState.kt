package io.github.evaogbe.diswantin.activity.ui

import io.github.evaogbe.diswantin.activity.data.Activity
import kotlinx.collections.immutable.PersistentList

sealed interface ActivitySearchUiState {
    data object Initial : ActivitySearchUiState

    data object Pending : ActivitySearchUiState

    data object Failure : ActivitySearchUiState

    data class Success(val searchResults: PersistentList<Activity>) : ActivitySearchUiState
}
