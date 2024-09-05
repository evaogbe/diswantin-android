package io.github.evaogbe.diswantin.testing

import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.task.data.TaskCompletion
import io.github.evaogbe.diswantin.task.data.TaskPath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeDatabase {
    private var taskCategoryIdGen = 0L

    private val _taskCategoryTable = MutableStateFlow(emptyMap<Long, TaskCategory>())

    val taskCategoryTable = _taskCategoryTable.asStateFlow()

    private var taskIdGen = 0L

    private val _taskTable = MutableStateFlow(emptyMap<Long, Task>())

    val taskTable = _taskTable.asStateFlow()

    private var taskPathIdGen = 0L

    private val _taskPathTable = MutableStateFlow(emptyMap<Long, TaskPath>())

    val taskPathTable = _taskPathTable.asStateFlow()

    private var taskCompletionIdGen = 0L

    private val _taskCompletionTable = MutableStateFlow(emptyMap<Long, TaskCompletion>())

    val taskCompletionTable = _taskCompletionTable.asStateFlow()

    fun insertTaskCategory(taskCategory: TaskCategory, taskIds: Set<Long>): TaskCategory {
        val newTaskList = if (taskCategory.id > 0) {
            taskCategory
        } else {
            taskCategory.copy(id = ++taskCategoryIdGen)
        }
        _taskCategoryTable.update { it + (taskCategory.id to taskCategory) }
        _taskTable.update { taskTable ->
            taskTable + taskTable.values
                .filter { it.id in taskIds }
                .map { it.copy(categoryId = taskCategory.id) }
                .associateBy { it.id }
        }
        return newTaskList
    }

    fun updateTaskCategory(
        taskCategory: TaskCategory,
        taskIdsToInsert: List<Long>,
        taskIdsToRemove: List<Long>,
    ) {
        _taskCategoryTable.update { it + (taskCategory.id to taskCategory) }
        _taskTable.update { taskTable ->
            taskTable +
                    taskIdsToRemove.mapNotNull { taskTable[it]?.copy(categoryId = null) }
                        .associateBy { it.id } +
                    taskIdsToInsert.mapNotNull { taskTable[it]?.copy(categoryId = taskCategory.id) }
                        .associateBy { it.id }
        }
    }

    fun deleteTaskCategory(id: Long) {
        _taskTable.update { taskTable ->
            taskTable + taskTable.values
                .filter<Task> { it.categoryId == id }
                .map { it.copy(categoryId = null) }
                .associateBy { it.id }
        }
        _taskCategoryTable.update { it - id }
    }

    fun insertTask(task: Task): Task {
        val newTask = if (task.id > 0) task else task.copy(id = ++taskIdGen)
        val path = TaskPath(
            id = ++taskPathIdGen,
            ancestor = newTask.id,
            descendant = newTask.id,
            depth = 0
        )
        _taskTable.update { it + (newTask.id to newTask) }
        _taskPathTable.update { it + (path.id to path) }
        return newTask
    }

    fun updateTask(task: Task) {
        _taskTable.update { it + (task.id to task) }
    }

    fun getTaskParentId(id: Long) =
        taskPathTable.value.values.firstOrNull {
            it.descendant == id && it.depth == 1
        }?.ancestor

    fun deleteTask(id: Long) {
        _taskTable.update { it - id }
        _taskPathTable.update { taskPathTable ->
            taskPathTable.filterValues { it.ancestor != id && it.descendant != id } +
                    getDecrementedPaths(id)
        }
        _taskCompletionTable.update { taskCompletionTable ->
            taskCompletionTable.filterValues { it.taskId != id }
        }
    }

    fun insertTaskCompletion(taskCompletion: TaskCompletion) {
        val newCompletion = taskCompletion.copy(id = ++taskCompletionIdGen)
        _taskCompletionTable.update { it + (newCompletion.id to newCompletion) }
    }

    fun insertChain(parentId: Long, childId: Long) {
        _taskPathTable.update { it + createTaskPathChain(parentId = parentId, childId = childId) }
    }

    fun connectTaskPath(parentId: Long, childId: Long) {
        _taskPathTable.update { taskPathTable ->
            val connectingPath = taskPathTable.values.firstOrNull {
                it.ancestor == childId && it.descendant == parentId ||
                        it.ancestor == parentId && it.descendant == childId
            }

            when (connectingPath?.ancestor) {
                null -> {
                    taskPathTable + if (getTaskParentId(childId) != null) {
                        val ancestors =
                            taskPathTable.values
                                .filter { it.descendant == childId }
                                .map { it.ancestor }
                                .toSet()
                        val descendants =
                            taskPathTable.values
                                .filter { it.ancestor == childId }
                                .map { it.descendant }
                                .toSet()
                        taskPathTable.values
                            .filter {
                                it.ancestor in ancestors &&
                                        it.descendant in descendants &&
                                        it.depth > 0
                            }
                            .map { it.copy(depth = it.depth + 1) }
                            .associateBy { it.id }
                    } else {
                        emptyMap()
                    } + createTaskPathChain(parentId = parentId, childId = childId)
                }

                childId -> {
                    taskPathTable.filterValues {
                        (it.ancestor != childId && it.descendant != childId) || it.depth == 0
                    } +
                            getDecrementedPaths(childId) +
                            createTaskPathChain(parentId = parentId, childId = childId)
                }

                else -> taskPathTable
            }
        }
    }

    fun deleteTaskPathAncestors(descendant: Long) {
        _taskPathTable.update { taskPathTable ->
            val ancestors =
                taskPathTable.values
                    .filter { it.descendant == descendant && it.depth > 0 }
                    .map { it.ancestor }
                    .toSet()
            val descendants =
                taskPathTable.values
                    .filter { it.ancestor == descendant }
                    .map { it.descendant }
                    .toSet()
            taskPathTable.filterValues {
                it.ancestor !in ancestors || it.descendant !in descendants
            }
        }
    }

    private fun getDecrementedPaths(taskId: Long): Map<Long, TaskPath> {
        val parentId = getTaskParentId(taskId)
        val childIds = taskPathTable.value.values
            .filter { it.ancestor == taskId && it.depth == 1 }
            .map { it.descendant }
            .toSet()
        return if (parentId != null && childIds.isNotEmpty()) {
            val ancestors = taskPathTable.value.values
                .filter { it.descendant == parentId }
                .map { it.ancestor }
                .toSet()
            val descendants = taskPathTable.value.values
                .filter { it.ancestor in childIds }
                .map { it.descendant }
                .toSet()
            taskPathTable.value.values
                .filter { it.ancestor in ancestors && it.descendant in descendants }
                .map { it.copy(depth = it.depth - 1) }
                .associateBy { it.id }
        } else {
            emptyMap()
        }
    }

    private fun createTaskPathChain(parentId: Long, childId: Long) =
        taskPathTable.value.values
            .filter { it.descendant == parentId }
            .flatMap { ancestorPath ->
                taskPathTable.value.values
                    .filter { it.ancestor == childId }
                    .map { descendantPath ->
                        TaskPath(
                            id = ++taskPathIdGen,
                            ancestor = ancestorPath.ancestor,
                            descendant = descendantPath.descendant,
                            depth = ancestorPath.depth + descendantPath.depth + 1,
                        )
                    }
            }
            .associateBy { it.id }
}
