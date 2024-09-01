package io.github.evaogbe.diswantin.testing

import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskCompletion
import io.github.evaogbe.diswantin.task.data.TaskList
import io.github.evaogbe.diswantin.task.data.TaskPath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeDatabase {
    private var taskListIdGen = 0L

    private val _taskListTable = MutableStateFlow(emptyMap<Long, TaskList>())

    val taskListTable = _taskListTable.asStateFlow()

    private var taskIdGen = 0L

    private val _taskTable = MutableStateFlow(emptyMap<Long, Task>())

    val taskTable = _taskTable.asStateFlow()

    private var taskPathIdGen = 0L

    private val _taskPathTable = MutableStateFlow(emptyMap<Long, TaskPath>())

    val taskPathTable = _taskPathTable.asStateFlow()

    private var taskCompletionIdGen = 0L

    private val _taskCompletionTable = MutableStateFlow(emptyMap<Long, TaskCompletion>())

    val taskCompletionTable = _taskCompletionTable.asStateFlow()

    fun insertTaskList(taskList: TaskList, taskIds: List<Long>) =
        insertTaskList(taskList, taskIds.toSet(), taskIds.flatMapIndexed { i, ancestor ->
            taskIds.drop(i + 1).mapIndexed { j, descendant ->
                TaskPath(ancestor = ancestor, descendant = descendant, depth = j + 1)
            }
        })

    fun insertTaskList(
        taskList: TaskList,
        taskIds: Set<Long>,
        taskPaths: Collection<TaskPath>
    ): TaskList {
        val newTaskList = if (taskList.id > 0) {
            taskList
        } else {
            taskList.copy(id = ++taskListIdGen)
        }
        _taskListTable.update { it + (taskList.id to taskList) }
        _taskTable.update { taskTable ->
            taskTable + taskTable.values
                .filter { it.id in taskIds }
                .map { it.copy(listId = taskList.id) }
                .associateBy { it.id }
        }
        _taskPathTable.update { taskPathTable ->
            taskPathTable + taskPaths.map { it.copy(id = ++taskPathIdGen) }.associateBy { it.id }
        }
        return newTaskList
    }

    fun updateTaskList(
        taskList: TaskList,
        taskIdsToInsert: List<Long>,
        taskPathsToInsert: List<TaskPath>,
        taskIdsToRemove: List<Long>,
        taskPathTaskIdsToRemove: Set<Long>,
    ) {
        _taskListTable.update { it + (taskList.id to taskList) }
        _taskTable.update { taskTable ->
            taskTable +
                    taskIdsToRemove.mapNotNull { taskTable[it]?.copy(listId = null) }
                        .associateBy { it.id } +
                    taskIdsToInsert.mapNotNull { taskTable[it]?.copy(listId = taskList.id) }
                        .associateBy { it.id }
        }
        _taskPathTable.update { taskPathTable ->
            taskPathTable.filterValues {
                (it.ancestor !in taskPathTaskIdsToRemove
                        && it.descendant !in taskPathTaskIdsToRemove)
                        || it.depth == 0
            } + taskPathsToInsert.map { it.copy(id = ++taskPathIdGen) }.associateBy { it.id }
        }
    }

    fun deleteTaskList(id: Long) {
        val tasksToUpdate = taskTable.value.values.filter { it.listId == id }
        _taskPathTable.update { taskPathTable ->
            val taskIds = tasksToUpdate.map { it.id }.toSet()
            taskPathTable.filterValues {
                (it.ancestor !in taskIds && it.descendant !in taskIds) || it.depth == 0
            }
        }
        _taskTable.update { taskTable ->
            taskTable + tasksToUpdate.map { it.copy(listId = null) }.associateBy { it.id }
        }
        _taskListTable.update { it - id }
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

    fun deleteTask(id: Long) {
        val parentId = taskPathTable.value.values.firstOrNull {
            it.descendant == id && it.depth == 1
        }?.ancestor
        val childId = taskPathTable.value.values.firstOrNull {
            it.ancestor == id && it.depth == 1
        }?.descendant
        _taskTable.update { it - id }
        _taskPathTable.update { taskPathTable ->
            taskPathTable.filterValues { it.ancestor != id && it.descendant != id } +
                    if (parentId != null && childId != null) {
                        val ancestors = taskPathTable.values
                            .filter { it.descendant == parentId }
                            .map { it.ancestor }
                            .toSet()
                        val descendants = taskPathTable.values
                            .filter { it.ancestor == childId }
                            .map { it.descendant }
                            .toSet()
                        taskPathTable.values
                            .filter { it.ancestor in ancestors && it.descendant in descendants }
                            .map { it.copy(depth = it.depth - 1) }
                            .associateBy { it.id }
                    } else {
                        emptyMap()
                    }
        }
        _taskCompletionTable.update { taskCompletionTable ->
            taskCompletionTable.filterValues { it.taskId != id }
        }
    }

    fun insertTaskCompletion(taskCompletion: TaskCompletion) {
        val newCompletion = taskCompletion.copy(id = ++taskCompletionIdGen)
        _taskCompletionTable.update { it + (newCompletion.id to newCompletion) }
    }
}
