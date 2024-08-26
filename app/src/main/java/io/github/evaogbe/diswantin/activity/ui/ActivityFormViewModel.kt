package io.github.evaogbe.diswantin.activity.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.activity.data.Activity
import io.github.evaogbe.diswantin.activity.data.ActivityRepository
import io.github.evaogbe.diswantin.activity.data.EditActivityForm
import io.github.evaogbe.diswantin.activity.data.NewActivityForm
import io.github.evaogbe.diswantin.ui.navigation.Destination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class ActivityFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val activityRepository: ActivityRepository,
    private val clock: Clock,
) : ViewModel() {
    private val activityId: Long? = savedStateHandle[Destination.EditActivityForm.ID_KEY]

    val isNew = activityId == null

    var nameInput by mutableStateOf("")
        private set

    private val dueAtInput = MutableStateFlow<ZonedDateTime?>(null)

    private val scheduledAtInput = MutableStateFlow<ZonedDateTime?>(null)

    private val prevActivity = MutableStateFlow<Activity?>(null)

    private val prevActivityQuery = MutableStateFlow("")

    private val saveResult = MutableStateFlow<Result<Unit>?>(null)

    private val activityStream = activityId?.let { id ->
        activityRepository.getById(id).catch { e ->
            Timber.e(e, "Failed to fetch activity by id: %d", id)
            emit(null)
        }
    } ?: flowOf(null)

    val uiState = initUiState()

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun initUiState(): StateFlow<ActivityFormUiState> {
        viewModelScope.launch {
            val activity = activityStream.first() ?: return@launch
            nameInput = activity.name
            dueAtInput.value = activity.dueAt?.atZone(clock.zone)
            scheduledAtInput.value = activity.scheduledAt?.atZone(clock.zone)
            prevActivity.value = activityRepository.getParent(activity.id).first()
        }
        val prevActivityOptionsStream = prevActivityQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                activityRepository.search(
                    query = query.trim(),
                    tailsOnly = true,
                    excludeChainFor = activityId
                ).catch { e ->
                    Timber.e(
                        e,
                        "Failed to search for activities by query: %s, id: %d",
                        query,
                        activityId
                    )
                    emit(emptyList())
                }
            }
        }
        val hasActivitiesOutsideChainStream =
            activityRepository.hasActivities(excludeChainFor = activityId).catch { e ->
                Timber.e(e, "Failed to fetch activities outside chain of: %d", activityId)
                emit(false)
            }
        return combine(
            dueAtInput,
            scheduledAtInput,
            prevActivity,
            saveResult,
            activityStream,
            prevActivityOptionsStream,
            hasActivitiesOutsideChainStream,
        ) { args ->
            val dueAtInput = args[0] as ZonedDateTime?
            val scheduledAtInput = args[1] as ZonedDateTime?
            val prevActivity = args[2] as Activity?
            val saveResult = args[3] as Result<Unit>?
            val activity = args[4]
            val prevActivityOptions = args[5] as List<Activity>
            val hasActivitiesOutsideChain = args[6] as Boolean
            when {
                saveResult?.isSuccess == true -> ActivityFormUiState.Saved
                activityId != null && activity == null -> ActivityFormUiState.Failure
                else -> ActivityFormUiState.Success(
                    dueAtInput = dueAtInput,
                    scheduledAtInput = scheduledAtInput,
                    canUpdatePrevActivity = hasActivitiesOutsideChain || prevActivity != null,
                    prevActivity = prevActivity,
                    prevActivityOptions = prevActivityOptions,
                    hasSaveError = saveResult?.isFailure == true,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ActivityFormUiState.Pending,
        )
    }

    fun updateNameInput(value: String) {
        nameInput = value
    }

    fun updateDueAtInput(value: ZonedDateTime?) {
        dueAtInput.value = value
    }

    fun updateScheduledAtInput(value: ZonedDateTime?) {
        scheduledAtInput.value = value
    }

    fun updatePrevActivity(value: Activity?) {
        prevActivity.value = value
    }

    fun searchPrevActivity(query: String) {
        prevActivityQuery.value = query
    }

    fun saveActivity() {
        if (nameInput.isBlank()) return
        val state = (uiState.value as? ActivityFormUiState.Success) ?: return
        if (activityId == null) {
            val form = NewActivityForm(
                name = nameInput,
                dueAt = state.dueAtInput?.toInstant(),
                scheduledAt = state.scheduledAtInput?.toInstant(),
                prevActivityId = state.prevActivity?.id,
                clock = clock,
            )
            viewModelScope.launch {
                try {
                    activityRepository.create(form)
                    saveResult.value = Result.success(Unit)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to create activity with form: %s", form)
                    saveResult.value = Result.failure(e)
                }
            }
        } else {
            viewModelScope.launch {
                try {
                    val activity = checkNotNull(activityStream.first())
                    val existingParent = activityRepository.getParent(activityId).first()
                    activityRepository.update(
                        EditActivityForm(
                            name = nameInput,
                            dueAt = state.dueAtInput?.toInstant(),
                            scheduledAt = state.scheduledAtInput?.toInstant(),
                            oldParentId = existingParent?.id,
                            parentId = state.prevActivity?.id,
                            activity = activity,
                        )
                    )
                    saveResult.value = Result.success(Unit)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to update activity with id: %d", activityId)
                    saveResult.value = Result.failure(e)
                }
            }
        }
    }
}
