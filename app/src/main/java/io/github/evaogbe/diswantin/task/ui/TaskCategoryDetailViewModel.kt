package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.data.Result
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.task.data.TaskCategoryRepository
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.ui.navigation.NavArguments
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
class TaskCategoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskCategoryRepository: TaskCategoryRepository,
    taskRepository: TaskRepository,
    clock: Clock,
) : ViewModel() {
    private val categoryId: Long = checkNotNull(savedStateHandle[NavArguments.ID_KEY])

    private val initialized = MutableStateFlow(false)

    private val userMessage = MutableStateFlow<UserMessage?>(null)

    val taskItemPagingData = taskRepository.getTaskItemsByCategoryId(categoryId).map { pagingData ->
        val doneBefore = ZonedDateTime.now(clock).with(LocalTime.MIN).toInstant()
        pagingData.map { TaskItemUiState.fromTaskItem(it, doneBefore) }
    }.cachedIn(viewModelScope)

    val uiState = combine(
        initialized,
        taskCategoryRepository.getById(categoryId).onEach {
            if (it != null) {
                initialized.value = true
            }
        }.map<TaskCategory?, Result<TaskCategory?>> {
            Result.Success(it)
        }.catch { e ->
            Timber.e(e, "Failed to fetch task category by id: %d", categoryId)
            emit(Result.Failure(e))
        },
        userMessage,
    ) { initialized, categoryResult, userMessage ->
        categoryResult.fold(
            onSuccess = { category ->
                when {
                    category != null -> {
                        TaskCategoryDetailUiState.Success(
                            category = category,
                            userMessage = userMessage,
                        )
                    }

                    initialized -> TaskCategoryDetailUiState.Deleted
                    else -> {
                        TaskCategoryDetailUiState.Failure(
                            NullPointerException("Category with id $categoryId not found"),
                        )
                    }
                }
            },
            onFailure = TaskCategoryDetailUiState::Failure,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TaskCategoryDetailUiState.Pending,
    )

    fun deleteCategory() {
        val category = (uiState.value as? TaskCategoryDetailUiState.Success)?.category ?: return

        viewModelScope.launch {
            try {
                taskCategoryRepository.delete(category)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete task category: %s", category)
                userMessage.value = UserMessage.String(R.string.task_category_detail_delete_error)
            }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }
}
