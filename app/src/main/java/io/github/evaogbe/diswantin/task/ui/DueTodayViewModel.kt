package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.task.data.TaskRepository
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DueTodayViewModel @Inject constructor(taskRepository: TaskRepository, clock: Clock) :
    ViewModel() {
    val taskPagingData =
        taskRepository.getTasksDueAt(LocalDate.now(clock), clock.zone).map { pagingData ->
            pagingData.map {
                DueTaskUiState(
                    id = it.task.id,
                    name = it.task.name,
                    recurrence = TaskRecurrenceUiState.tryFromEntities(it.recurrences),
                )
            }
        }.cachedIn(viewModelScope)
}
