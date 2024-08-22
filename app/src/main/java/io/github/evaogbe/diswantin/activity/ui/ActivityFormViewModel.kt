package io.github.evaogbe.diswantin.activity.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.activity.data.Activity
import io.github.evaogbe.diswantin.activity.data.ActivityForm
import io.github.evaogbe.diswantin.activity.data.ActivityRepository
import io.github.evaogbe.diswantin.ui.navigation.Destination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class ActivityFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val activityRepository: ActivityRepository
) : ViewModel() {
    private val activityId = savedStateHandle.get<Long>(Destination.EditActivityForm.ID_KEY)

    val isNew = activityId == null

    var nameInput by mutableStateOf("")
        private set

    var dueAtInput by mutableStateOf<ZonedDateTime?>(null)
        private set

    var scheduledAtInput by mutableStateOf<ZonedDateTime?>(null)
        private set

    private var activity: Activity? = null

    var uiState by mutableStateOf<ActivityFormUiState>(ActivityFormUiState.Pending)
        private set

    var userMessage by mutableStateOf<Int?>(null)
        private set

    fun initialize() {
        if (activityId != null) {
            viewModelScope.launch {
                try {
                    val activity = activityRepository.findById(activityId)
                    this@ActivityFormViewModel.activity = activity
                    nameInput = activity.name
                    dueAtInput = activity.dueAt?.atZone(ZoneId.systemDefault())
                    scheduledAtInput = activity.scheduledAt?.atZone(ZoneId.systemDefault())
                    uiState = ActivityFormUiState.Success(hasSaveError = false)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to fetch activity by id: %d", activityId)
                    uiState = ActivityFormUiState.Failure
                }
            }
        } else {
            uiState = ActivityFormUiState.Success(hasSaveError = false)
        }
    }

    fun updateNameInput(value: String) {
        nameInput = value
    }

    fun updateDueAtInput(value: ZonedDateTime?) {
        dueAtInput = value
    }

    fun updateScheduledAtInput(value: ZonedDateTime?) {
        scheduledAtInput = value
    }

    fun saveActivity() {
        when {
            nameInput.isBlank() -> {}
            activity == null -> {
                viewModelScope.launch {
                    try {
                        activityRepository.create(
                            ActivityForm(
                                name = nameInput,
                                dueAt = dueAtInput?.toInstant(),
                                scheduledAt = scheduledAtInput?.toInstant()
                            ),
                        )
                        uiState = ActivityFormUiState.Saved
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to create activity with name: %s", nameInput)
                        uiState = ActivityFormUiState.Success(hasSaveError = true)
                    }
                }
            }

            else -> {
                val activity = this.activity ?: return
                viewModelScope.launch {
                    try {
                        activityRepository.update(
                            ActivityForm(
                                name = nameInput,
                                dueAt = dueAtInput?.toInstant(),
                                scheduledAt = scheduledAtInput?.toInstant()
                            ).getUpdatedActivity(activity)
                        )
                        uiState = ActivityFormUiState.Saved
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to update activity: %s", activity)
                        uiState = ActivityFormUiState.Success(hasSaveError = true)
                    }
                }
            }
        }
    }

    fun removeActivity() {
        val activity = this.activity ?: return
        viewModelScope.launch {
            try {
                activityRepository.remove(activity)
                uiState = ActivityFormUiState.Removed
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove activity: %s", activity)
                userMessage = R.string.activity_form_delete_error
            }
        }
    }

    fun userMessageShown() {
        userMessage = null
    }
}
