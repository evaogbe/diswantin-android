package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRepository
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import java.util.regex.Pattern
import javax.inject.Inject

@HiltViewModel
class TaskSearchViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    private val hasSearched = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(query, hasSearched, ::Pair)
        .flatMapLatest { (query, hasSearched) ->
            when {
                query.isNotBlank() -> {
                    taskRepository.search(query.trim())
                        .map<List<Task>, TaskSearchUiState> {
                            TaskSearchUiState.Success(searchResults = it.toPersistentList())
                        }.catch { e ->
                            Timber.e(e, "Failed to search for tasks by query: %s", query)
                            emit(TaskSearchUiState.Failure)
                        }.onStart { emit(TaskSearchUiState.Pending) }
                }

                hasSearched -> {
                    flowOf(TaskSearchUiState.Success(searchResults = persistentListOf()))
                }

                else -> flowOf(TaskSearchUiState.Initial)
            }
        }.onEach {
            if (it !is TaskSearchUiState.Initial) {
                hasSearched.value = true
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = TaskSearchUiState.Initial
        )

    fun searchTasks(value: String) {
        query.value = value
    }
}

fun String.findOccurrences(query: String) =
    query.trim().split("""\s+""".toRegex()).flatMap {
        Pattern.quote(it).toRegex(RegexOption.IGNORE_CASE)
            .findAll(this)
            .map(MatchResult::range)
    }
