package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.task.data.TaskCategoryRepository
import javax.inject.Inject

@HiltViewModel
class TaskCategoryListViewModel @Inject constructor(
    taskCategoryRepository: TaskCategoryRepository,
) : ViewModel() {
    val categoryPagingData = taskCategoryRepository.categoryPagingData.cachedIn(viewModelScope)
}
