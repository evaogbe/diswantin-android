package io.github.evaogbe.diswantin.testing

import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskCompletion
import io.github.evaogbe.diswantin.task.data.TaskPath
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
import io.github.evaogbe.diswantin.task.data.TaskSkip
import io.github.evaogbe.diswantin.task.data.TaskTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeDatabase {
    private var tagIdGen = 0L

    private val _tagTable = MutableStateFlow(emptyMap<Long, Tag>())

    val tagTable = _tagTable.asStateFlow()

    private var taskIdGen = 0L

    private val _taskTable = MutableStateFlow(emptyMap<Long, Task>())

    val taskTable = _taskTable.asStateFlow()

    private var taskTagIdGen = 0L

    private val _taskTagTable = MutableStateFlow(emptyMap<Long, TaskTag>())

    val taskTagTable = _taskTagTable.asStateFlow()

    private var taskPathIdGen = 0L

    private val _taskPathTable = MutableStateFlow(emptyMap<Long, TaskPath>())

    val taskPathTable = _taskPathTable.asStateFlow()

    private var taskCompletionIdGen = 0L

    private val _taskCompletionTable = MutableStateFlow(emptyMap<Long, TaskCompletion>())

    val taskCompletionTable = _taskCompletionTable.asStateFlow()

    private var taskRecurrenceIdGen = 0L

    private val _taskRecurrenceTable = MutableStateFlow(emptyMap<Long, TaskRecurrence>())

    val taskRecurrenceTable = _taskRecurrenceTable.asStateFlow()

    private var taskSkipIdGen = 0L

    private val _taskSkipTable = MutableStateFlow(emptyMap<Long, TaskSkip>())

    val taskSkipTable = _taskSkipTable.asStateFlow()

    fun insertTag(tag: Tag, taskIds: Set<Long> = emptySet()): Tag {
        val newTag = if (tag.id > 0) tag else tag.copy(id = ++tagIdGen)
        val newTaskTags = taskIds.map {
            TaskTag(id = ++taskTagIdGen, taskId = it, tagId = newTag.id)
        }.associateBy { it.id }
        _tagTable.update { it + (newTag.id to newTag) }
        _taskTagTable.update { it + newTaskTags }
        return newTag
    }

    fun updateTag(tag: Tag, taskIdsToInsert: Set<Long>, taskIdsToRemove: Set<Long>) {
        _tagTable.update { it + (tag.id to tag) }
        _taskTagTable.update { taskTagTable ->
            val taskTagIdsToRemove =
                taskTagTable.values.filter { it.tagId == tag.id && it.taskId in taskIdsToRemove }
                    .map { it.id }
            val taskTagsToInsert = taskIdsToInsert.map {
                TaskTag(id = ++taskTagIdGen, taskId = it, tagId = tag.id)
            }.associateBy { it.id }
            taskTagTable - taskTagIdsToRemove + taskTagsToInsert
        }
    }

    fun deleteTag(id: Long) {
        _taskTagTable.update { taskTagTable ->
            taskTagTable.filterValues { it.tagId != id }
        }
        _tagTable.update { it - id }
    }

    fun insertTask(task: Task): Task {
        val newTask = if (task.id > 0) task else task.copy(id = ++taskIdGen)
        val path = TaskPath(
            id = ++taskPathIdGen,
            ancestor = newTask.id,
            descendant = newTask.id,
            depth = 0,
        )
        _taskTable.update { it + (newTask.id to newTask) }
        _taskPathTable.update { it + (path.id to path) }
        return newTask
    }

    fun updateTask(task: Task) {
        _taskTable.update { it + (task.id to task) }
    }

    fun deleteTask(id: Long) {
        _taskTable.update { it - id }
        _taskPathTable.update { taskPathTable ->
            taskPathTable.filterValues {
                it.ancestor != id && it.descendant != id
            } + getDecrementedPaths(id)
        }
        _taskCompletionTable.update { taskCompletionTable ->
            taskCompletionTable.filterValues { it.taskId != id }
        }
        _taskRecurrenceTable.update { taskRecurrenceTable ->
            taskRecurrenceTable.filterValues { it.taskId != id }
        }
        _taskSkipTable.update { taskSkipTable ->
            taskSkipTable.filterValues { it.taskId != id }
        }
    }

    fun insertTaskCompletion(taskCompletion: TaskCompletion) {
        val newCompletion = taskCompletion.copy(id = ++taskCompletionIdGen)
        _taskCompletionTable.update { it + (newCompletion.id to newCompletion) }
    }

    fun insertTaskRecurrence(taskRecurrence: TaskRecurrence) {
        val newRecurrence = taskRecurrence.copy(id = ++taskRecurrenceIdGen)
        _taskRecurrenceTable.update { it + (newRecurrence.id to newRecurrence) }
    }

    fun deleteTaskRecurrence(id: Long) {
        _taskRecurrenceTable.update { it - id }
    }

    fun insertTaskSkip(taskSkip: TaskSkip) {
        val newSkip = taskSkip.copy(id = ++taskSkipIdGen)
        _taskSkipTable.update { it + (newSkip.id to newSkip) }
    }

    fun insertTaskTag(taskTag: TaskTag) {
        val newTaskTag = taskTag.copy(id = ++taskTagIdGen)
        _taskTagTable.update { it + (newTaskTag.id to newTaskTag) }
    }

    fun deleteTaskTag(id: Long) {
        _taskTagTable.update { it - id }
    }

    fun deleteLatestTaskCompletionByTaskId(taskId: Long) {
        _taskCompletionTable.update { taskCompletionTable ->
            taskCompletionTable.values.filter { it.taskId == taskId }
                .maxByOrNull { it.doneAt }?.id?.let { taskCompletionTable - it }
                ?: taskCompletionTable
        }
    }

    fun insertChain(parentId: Long, childId: Long) {
        _taskPathTable.update { it + createTaskPathChain(parentId = parentId, childId = childId) }
    }

    fun connectTaskPath(parentId: Long, childId: Long) {
        _taskPathTable.update { taskPathTable ->
            if (taskPathTable.values.any { it.ancestor == childId && it.descendant == parentId }) {
                taskPathTable.filterValues {
                    (it.ancestor != childId && it.descendant != childId) || it.depth == 0
                } + getDecrementedPaths(childId) + createTaskPathChain(
                    parentId = parentId, childId = childId
                )
            } else {
                taskPathTable - getAncestorIds(childId) + createTaskPathChain(
                    parentId = parentId, childId = childId
                )
            }
        }
    }

    fun deleteTaskPathAncestors(descendant: Long) {
        _taskPathTable.update { taskPathTable ->
            taskPathTable - getAncestorIds(descendant)
        }
    }

    private fun getDecrementedPaths(taskId: Long): Map<Long, TaskPath> {
        val parentId = taskPathTable.value.values.firstOrNull {
            it.descendant == taskId && it.depth == 1
        }?.ancestor
        val childIds = taskPathTable.value.values.filter { it.ancestor == taskId && it.depth == 1 }
            .map { it.descendant }.toSet()
        return if (parentId != null && childIds.isNotEmpty()) {
            val ancestors =
                taskPathTable.value.values.filter { it.descendant == parentId }.map { it.ancestor }
                    .toSet()
            val descendants =
                taskPathTable.value.values.filter { it.ancestor in childIds }.map { it.descendant }
                    .toSet()
            taskPathTable.value.values.filter {
                it.ancestor in ancestors && it.descendant in descendants
            }.map { it.copy(depth = it.depth - 1) }.associateBy { it.id }
        } else {
            emptyMap()
        }
    }

    private fun createTaskPathChain(parentId: Long, childId: Long) =
        taskPathTable.value.values.filter { it.descendant == parentId }.flatMap { ancestorPath ->
            taskPathTable.value.values.filter { it.ancestor == childId }.map { descendantPath ->
                TaskPath(
                    id = ++taskPathIdGen,
                    ancestor = ancestorPath.ancestor,
                    descendant = descendantPath.descendant,
                    depth = ancestorPath.depth + descendantPath.depth + 1,
                )
            }
        }.associateBy { it.id }

    private fun getAncestorIds(descendant: Long): List<Long> {
        val ancestors =
            taskPathTable.value.values.filter { it.descendant == descendant && it.depth > 0 }
                .map { it.ancestor }.toSet()
        val descendants =
            taskPathTable.value.values.filter { it.ancestor == descendant }.map { it.descendant }
                .toSet()
        return taskPathTable.value.values.filter {
            it.ancestor in ancestors && it.descendant in descendants
        }.map { it.id }
    }
}
