package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.task.data.Task
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
import java.util.regex.Pattern
import javax.inject.Inject

@HiltViewModel
class TaskSearchViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = query.flatMapLatest { query ->
        if (query.isBlank()) {
            flowOf(TaskSearchUiState.Initial)
        } else {
            taskRepository.search(query.trim())
                .map<List<Task>, TaskSearchUiState> {
                    TaskSearchUiState.Success(searchResults = it.toImmutableList())
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
