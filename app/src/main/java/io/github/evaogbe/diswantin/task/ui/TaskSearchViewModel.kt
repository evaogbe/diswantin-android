package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.task.data.TaskSearchCriteria
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.regex.Pattern
import javax.inject.Inject

@HiltViewModel
class TaskSearchViewModel @Inject constructor(
    taskRepository: TaskRepository,
    clock: Clock,
) : ViewModel() {
    private val criteria = MutableStateFlow(TaskSearchCriteria())

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResultPagingData = criteria.filterNot { it.isEmpty }.flatMapLatest { criteria ->
        val doneBefore = ZonedDateTime.now(clock).with(LocalTime.MIN).toInstant()
        taskRepository.searchTaskSummaries(criteria).map { searchResults ->
            searchResults.map {
                TaskSummaryUiState.fromTaskSummary(it, doneBefore)
            }
        }
    }.cachedIn(viewModelScope)

    val uiState = criteria.map { criteria ->
        TaskSearchUiState(hasCriteria = !criteria.isEmpty)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TaskSearchUiState(hasCriteria = false),
    )

    fun searchTasks(
        name: String,
        deadlineDateRange: Pair<LocalDate, LocalDate>?,
        startAfterDateRange: Pair<LocalDate, LocalDate>?,
        scheduledDateRange: Pair<LocalDate, LocalDate>?,
        doneDateRange: Pair<LocalDate, LocalDate>?,
        recurrenceDate: LocalDate?,
    ) {
        criteria.value = TaskSearchCriteria(
            name = name.trim(),
            deadlineDateRange = deadlineDateRange,
            startAfterDateRange = startAfterDateRange,
            scheduledDateRange = scheduledDateRange,
            doneDateRange = doneDateRange,
            recurrenceDate = recurrenceDate,
        )
    }
}

fun String.findOccurrences(query: String) = query.trim().split("""\s+""".toRegex()).flatMap {
    Pattern.quote(it).toRegex(RegexOption.IGNORE_CASE).findAll(this).map(MatchResult::range)
}
