package io.github.evaogbe.diswantin.task.ui

enum class TaskSearchTopBarAction {
    Search
}

data class TaskSearchResult(val id: Long, val name: String)

fun TaskSummaryUiState.toSearchResult() = TaskSearchResult(id = id, name = name)

data class TaskSearchUiState(val hasCriteria: Boolean)
