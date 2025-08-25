package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.data.Result
import io.github.evaogbe.diswantin.task.data.EditTagForm
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.task.data.TagRepository
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class TagDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tagRepository: TagRepository,
    taskRepository: TaskRepository,
    clock: Clock,
) : ViewModel() {
    private val tagId = savedStateHandle.toRoute<TagDetailRoute>().id

    private val initialized = MutableStateFlow(false)

    private val userMessage = MutableStateFlow<UserMessage?>(null)

    val taskSummaryPagingData = taskRepository.getTaskSummariesByTagId(tagId).map { pagingData ->
        val doneBefore = ZonedDateTime.now(clock).with(LocalTime.MIN).toInstant()
        pagingData.map { TaskSummaryUiState.fromTaskSummary(it, doneBefore) }
    }.cachedIn(viewModelScope)

    private val tagStream = tagRepository.getById(tagId).onEach {
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
        started = SharingStarted.WhileSubscribed(5_000L),
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
                        existingTag = tag,
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to update tag with id: %s", tagId)
                userMessage.value = UserMessage.String(R.string.tag_form_save_error_edit)
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
                userMessage.value = UserMessage.String(R.string.tag_detail_delete_error)
            }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }
}
