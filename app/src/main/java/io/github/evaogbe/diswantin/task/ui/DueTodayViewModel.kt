package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.data.ClockMonitor
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DueTodayViewModel @Inject constructor(
    taskRepository: TaskRepository,
    clockMonitor: ClockMonitor,
) : BaseViewModel(clockMonitor) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val taskPagingData = clock.flatMapLatest { clock ->
        taskRepository.getTasksDueAt(LocalDate.now(clock), clock.zone).map { pagingData ->
            pagingData.map {
                DueTaskUiState(
                    id = it.task.id,
                    name = it.task.name,
                    recurrence = TaskRecurrenceUiState.tryFromEntities(it.recurrences),
                )
            }
        }
    }.cachedIn(viewModelScope)
}
