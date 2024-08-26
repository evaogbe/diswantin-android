package io.github.evaogbe.diswantin.activity.ui

import io.github.evaogbe.diswantin.activity.data.Activity
import java.time.ZonedDateTime

sealed interface ActivityFormUiState {
    data object Pending : ActivityFormUiState

    data object Failure : ActivityFormUiState

    data class Success(
        val dueAtInput: ZonedDateTime?,
        val scheduledAtInput: ZonedDateTime?,
        val canUpdatePrevActivity: Boolean,
        val prevActivity: Activity?,
        val prevActivityOptions: List<Activity>,
        val hasSaveError: Boolean,
    ) : ActivityFormUiState

    data object Saved : ActivityFormUiState
}
