package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.data.ClockMonitor
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.task.data.TaskSearchCriteria
import io.github.evaogbe.diswantin.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.util.regex.Pattern
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class TaskSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    taskRepository: TaskRepository,
    clockMonitor: ClockMonitor,
) : BaseViewModel(clockMonitor) {
    private val criteria = savedStateHandle.getMutableStateFlow(CRITERIA_KEY, TaskSearchCriteria())

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResultPagingData = criteria.filterNot { it.isEmpty }.flatMapLatest { criteria ->
        val doneBefore = now().toLocalDate().atStartOfDay(clock.value.zone).toInstant()
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
        started = SharingStarted.WhileSubscribed(5.seconds),
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
            doneRange = doneDateRange?.let { (startDate, endDate) ->
                val start = startDate.atStartOfDay(clock.value.zone).toInstant()
                val end = endDate.plusDays(1).atStartOfDay(clock.value.zone).toInstant()
                start to end
            },
            recurrenceDate = recurrenceDate,
        )
    }
}

fun String.findOccurrences(query: String) = query.trim().split("""\s+""".toRegex()).flatMap {
    Pattern.quote(it).toRegex(RegexOption.IGNORE_CASE).findAll(this).map(MatchResult::range)
}

private const val CRITERIA_KEY = "criteria"
