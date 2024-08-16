package io.github.evaogbe.diswantin.activity.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.activity.data.Activity
import io.github.evaogbe.diswantin.activity.data.ActivityRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class CurrentActivityViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
) : ViewModel() {
    val uiState = activityRepository.currentActivityStream
        .map { activity ->
            if (activity == null) {
                CurrentActivityUiState.Empty
            } else {
                CurrentActivityUiState.Present(currentActivity = activity)
            }
        }
        .catch { e ->
            Timber.e(e, "Failed to fetch current activity")
            emit(CurrentActivityUiState.Failure)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = CurrentActivityUiState.Pending
        )

    var saveResult by mutableStateOf<Result<Unit>?>(null)
        private set

    @get:StringRes
    var userMessage by mutableStateOf<Int?>(null)
        private set

    fun createActivity(name: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            val newActivity = try {
                activityRepository.create(name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to create activity with name: %s", name)
                saveResult = Result.failure(e)
                null
            }
            if (newActivity != null) {
                saveResult = Result.success(Unit)
                userMessage = R.string.activity_saved_message_new
            }
        }
    }

    fun updateActivity(name: String) {
        if (name.isBlank()) return

        val updatedActivity = (uiState.value as? CurrentActivityUiState.Present)
            ?.currentActivity
            ?.copy(name = name.trim())
            ?: return
        viewModelScope.launch {
            if (update(updatedActivity)) {
                saveResult = Result.success(Unit)
                userMessage = R.string.activity_saved_message_edit
            }
        }
    }

    private suspend fun update(activity: Activity) =
        try {
            activityRepository.update(activity)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to update activity: %s", activity)
            saveResult = Result.failure(e)
            false
        }

    fun saveHandled() {
        saveResult = null
    }

    fun skipCurrentActivity() {
        val activity = (uiState.value as? CurrentActivityUiState.Present)?.currentActivity ?: return
        val updatedActivity = activity.copy(skippedAt = Instant.now())

        viewModelScope.launch {
            try {
                activityRepository.update(updatedActivity)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to skip current activity: %s", activity)
                userMessage = R.string.current_activity_skip_error
            }
        }
    }

    fun removeCurrentActivity() {
        val activity = (uiState.value as? CurrentActivityUiState.Present)?.currentActivity ?: return

        viewModelScope.launch {
            try {
                activityRepository.remove(activity)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove activity: %s", activity)
                userMessage = R.string.current_activity_remove_error
            }
        }
    }

    fun userMessageShown() {
        userMessage = null
    }
}
