package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.task.data.TaskItem
import io.github.evaogbe.diswantin.task.data.TaskRepository
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import java.time.Clock
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.regex.Pattern
import javax.inject.Inject

@HiltViewModel
class TaskSearchViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    clock: Clock,
) : ViewModel() {
    private val query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = query.flatMapLatest { query ->
        if (query.isBlank()) {
            flowOf(TaskSearchUiState.Initial)
        } else {
            taskRepository.searchTaskItems(query.trim())
                .map<List<TaskItem>, TaskSearchUiState> { searchResults ->
                    val doneBefore = ZonedDateTime.now(clock).with(LocalTime.MIN).toInstant()
                    TaskSearchUiState.Success(
                        searchResults = searchResults.map {
                            TaskItemUiState.fromTaskItem(it, doneBefore)
                        }.toImmutableList(),
                    )
                }.catch { e ->
                    Timber.e(e, "Failed to search for tasks by query: %s", query)
                    emit(TaskSearchUiState.Failure)
                }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TaskSearchUiState.Initial
    )

    fun searchTasks(query: String) {
        this.query.value = query
    }
}

fun String.findOccurrences(query: String) =
    query.trim().split("""\s+""".toRegex()).flatMap {
        Pattern.quote(it).toRegex(RegexOption.IGNORE_CASE)
            .findAll(this)
            .map(MatchResult::range)
    }
