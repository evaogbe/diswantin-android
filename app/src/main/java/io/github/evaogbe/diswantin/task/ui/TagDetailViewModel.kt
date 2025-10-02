package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.data.Result
import io.github.evaogbe.diswantin.task.data.EditTagForm
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.task.data.TagRepository
import io.github.evaogbe.diswantin.task.data.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class TagDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tagRepository: TagRepository,
    taskRepository: TaskRepository,
    private val clock: Clock,
) : ViewModel() {
    private val tagId = savedStateHandle.toRoute<TagDetailRoute>().id

    private val initialized = MutableStateFlow(false)

    private val userMessage = MutableStateFlow<TagDetailUserMessage?>(null)

    private val startOfToday
        get() = LocalDate.now(clock).atStartOfDay(clock.zone).toInstant()

    val taskSummaryPagingData =
        taskRepository.getTaskSummariesByTagId(tagId, startOfToday).map { pagingData ->
            pagingData.map {
                TaskSummaryUiState.fromTaskSummary(it, startOfToday)
            }
        }.cachedIn(viewModelScope)

    private val tagStream = tagRepository.getTagById(tagId).onEach {
        if (it != null) {
            initialized.value = true
        }
    }.map<Tag?, Result<Tag?>> {
        Result.Success(it)
    }.catch { e ->
        Timber.e(e, "Failed to fetch tag by id: %d", tagId)
        emit(Result.Failure(e))
    }

    val uiState = combine(
        initialized,
        tagStream,
        userMessage,
    ) { initialized, tagResult, userMessage ->
        tagResult.fold(
            onSuccess = { tag ->
                when {
                    tag != null -> {
                        TagDetailUiState.Success(
                            tag = tag,
                            userMessage = userMessage,
                        )
                    }

                    initialized -> TagDetailUiState.Deleted
                    else -> {
                        TagDetailUiState.Failure(
                            NullPointerException("Tag with id $tagId not found"),
                        )
                    }
                }
            },
            onFailure = TagDetailUiState::Failure,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = TagDetailUiState.Pending,
    )

    fun saveTag(name: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            try {
                val tag = checkNotNull(tagStream.first().getOrNull())
                tagRepository.update(
                    EditTagForm(
                        name = name,
                        now = Instant.now(clock),
                        existingTag = tag,
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to update tag with id: %s", tagId)
                userMessage.value = TagDetailUserMessage.EditError
            }
        }
    }

    fun deleteTag() {
        val tag = (uiState.value as? TagDetailUiState.Success)?.tag ?: return

        viewModelScope.launch {
            try {
                tagRepository.delete(tag)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete tag: %s", tag)
                userMessage.value = TagDetailUserMessage.DeleteError
            }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }
}
