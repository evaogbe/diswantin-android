package io.github.evaogbe.diswantin.activity.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.activity.data.ActivityRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CurrentActivityViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
) : ViewModel() {
    private val userMessage = MutableStateFlow<Int?>(null)

    val uiState =
        combine(activityRepository.currentActivityStream, userMessage) { activity, userMessage ->
            if (activity == null) {
                CurrentActivityUiState.Empty
            } else {
                CurrentActivityUiState.Present(
                    currentActivity = activity,
                    userMessage = userMessage
                )
            }
        }.catch { e ->
            Timber.e(e, "Failed to fetch current activity")
            emit(CurrentActivityUiState.Failure)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = CurrentActivityUiState.Pending
        )

    fun removeCurrentActivity() {
        val activity = (uiState.value as? CurrentActivityUiState.Present)?.currentActivity ?: return

        viewModelScope.launch {
            try {
                activityRepository.remove(activity.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove activity: %s", activity)
                userMessage.value = R.string.current_activity_remove_error
            }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }
}
