package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.task.data.TagRepository
import javax.inject.Inject

@HiltViewModel
class TagListViewModel @Inject constructor(tagRepository: TagRepository) : ViewModel() {
    val tagPagingData = tagRepository.tagPagingData.cachedIn(viewModelScope)
}
