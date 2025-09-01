package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.task.data.NewTagForm
import io.github.evaogbe.diswantin.task.data.TagRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TagListViewModel @Inject constructor(private val tagRepository: TagRepository) : ViewModel() {
    val tagPagingData = tagRepository.tagPagingData.cachedIn(viewModelScope)

    val userMessage = MutableStateFlow<TagListUserMessage?>(null)

    fun saveTag(name: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            try {
                tagRepository.create(NewTagForm(name = name))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to create tag with name: %s", name)
                userMessage.value = TagListUserMessage.CreateError
            }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }
}
