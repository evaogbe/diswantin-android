package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.TaskCategoryRepository
import io.github.evaogbe.diswantin.ui.navigation.NavArguments
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
    clock: Clock,
) : ViewModel() {
    private val categoryId: Long = checkNotNull(savedStateHandle[NavArguments.ID_KEY])

    private val initialized = MutableStateFlow(false)

    private val userMessage = MutableStateFlow<Int?>(null)

    val uiState =
        combine(
            initialized,
            taskCategoryRepository.getCategoryWithTaskItemsById(categoryId),
            userMessage,
        ) { initialized, categoryWithTasks, userMessage ->
            when {
                categoryWithTasks != null -> {
                    val midnight = ZonedDateTime.now(clock).with(LocalTime.MIN).toInstant()
                    TaskCategoryDetailUiState.Success(
                        category = categoryWithTasks.category,
                        tasks = categoryWithTasks.tasks.map { task ->
                            TaskItemUiState(
                                id = task.id,
                                name = task.name,
                                isDone = if (task.recurring) {
                                    task.doneAt?.let { it < midnight } == false
                                } else {
                                    task.doneAt != null
                                },
                            )
                        }.toImmutableList(),
                        userMessage = userMessage,
                    )
                }

                initialized -> TaskCategoryDetailUiState.Deleted
                else -> TaskCategoryDetailUiState.Failure
            }
        }.onEach {
            if (it is TaskCategoryDetailUiState.Success) {
                initialized.value = true
            }
        }.catch { e ->
            Timber.e(e, "Failed to fetch task category by id: %d", categoryId)
            emit(TaskCategoryDetailUiState.Failure)
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
                userMessage.value = R.string.task_category_detail_delete_error
            }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }
}
