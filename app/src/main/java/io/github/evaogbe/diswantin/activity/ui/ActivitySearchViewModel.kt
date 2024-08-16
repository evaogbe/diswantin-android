package io.github.evaogbe.diswantin.activity.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.activity.data.Activity
import io.github.evaogbe.diswantin.activity.data.ActivityRepository
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
class ActivitySearchViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    private val hasSearched = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(query, hasSearched, ::Pair)
        .flatMapLatest { (query, hasSearched) ->
            when {
                query.isNotBlank() -> {
                    activityRepository.search(query.trim())
                        .map<List<Activity>, ActivitySearchUiState> {
                            ActivitySearchUiState.Success(searchResults = it.toPersistentList())
                        }.catch { e ->
                            Timber.e(e, "Failed to search for activities by query: %s", query)
                            emit(ActivitySearchUiState.Failure)
                        }.onStart { emit(ActivitySearchUiState.Pending) }
                }

                hasSearched -> {
                    flowOf(ActivitySearchUiState.Success(searchResults = persistentListOf()))
                }

                else -> flowOf(ActivitySearchUiState.Initial)
            }
        }.onEach {
            if (it !is ActivitySearchUiState.Initial) {
                hasSearched.value = true
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ActivitySearchUiState.Initial
        )

    fun searchActivities(value: String) {
        query.value = value
    }
}

fun String.findOccurrences(query: String) =
    query.trim().split("""\s+""".toRegex()).flatMap {
        Pattern.quote(it).toRegex(RegexOption.IGNORE_CASE)
            .findAll(this)
            .map(MatchResult::range)
    }
