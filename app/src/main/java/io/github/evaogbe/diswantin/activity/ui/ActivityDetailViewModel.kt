package io.github.evaogbe.diswantin.activity.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.activity.data.ActivityRepository
import io.github.evaogbe.diswantin.ui.navigation.Destination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class ActivityDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val activityRepository: ActivityRepository,
    private val clock: Clock,
) : ViewModel() {
    private val activityId: Long = checkNotNull(savedStateHandle[Destination.ActivityDetail.ID_KEY])

    private val userMessage = MutableStateFlow<Int?>(null)

    val uiState =
        combine(activityRepository.findById(activityId), userMessage) { activity, userMessage ->
            if (activity != null) {
                ActivityDetailUiState.Success(
                    activity = activity,
                    userMessage = userMessage,
                    clock = clock
                )
            } else {
                ActivityDetailUiState.Removed
            }
        }.catch { e ->
            Timber.e(e, "Failed to fetch activity by id: %d", activityId)
            emit(ActivityDetailUiState.Failure)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ActivityDetailUiState.Pending
        )

    fun removeActivity() {
        val activity = (uiState.value as? ActivityDetailUiState.Success)?.activity ?: return
        viewModelScope.launch {
            try {
                activityRepository.remove(activity)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove activity: %s", activity)
                userMessage.value = R.string.activity_detail_delete_error
            }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }
}
