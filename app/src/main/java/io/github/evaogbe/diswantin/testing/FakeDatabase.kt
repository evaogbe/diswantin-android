package io.github.evaogbe.diswantin.testing

import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskList
import io.github.evaogbe.diswantin.task.data.TaskListWithTasks
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

    fun insertTaskList(taskListWithTasks: TaskListWithTasks) =
        insertTaskList(taskListWithTasks, taskListWithTasks.tasks.flatMapIndexed { i, ancestor ->
            taskListWithTasks.tasks.drop(i + 1).mapIndexed { j, descendant ->
                TaskPath(ancestor = ancestor.id, descendant = descendant.id, depth = j + 1)
            }
        })

    fun insertTaskList(
        taskListWithTasks: TaskListWithTasks,
        taskPaths: List<TaskPath>
    ): TaskListWithTasks {
        val taskList = if (taskListWithTasks.taskList.id > 0) {
            taskListWithTasks.taskList
        } else {
            taskListWithTasks.taskList.copy(id = ++taskListIdGen)
        }
        val tasks = taskListWithTasks.tasks.map { it.copy(listId = taskList.id) }
        _taskListTable.update { it + (taskList.id to taskList) }
        _taskTable.update { taskTable ->
            taskTable + tasks.associateBy { it.id }
        }
        _taskPathTable.update { taskPathTable ->
            taskPathTable + taskPaths.associateBy { it.id }
        }
        return TaskListWithTasks(taskList, tasks)
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
                (it.ancestor in taskPathTaskIdsToRemove
                        || it.descendant in taskPathTaskIdsToRemove)
                        && it.depth > 0
            } + taskPathsToInsert.associateBy { it.id }
        }
    }

    fun deleteTaskList(id: Long) {
        val tasksToUpdate = taskTable.value.values.filter { it.listId == id }
        _taskPathTable.update { taskPathTable ->
            val taskIds = tasksToUpdate.map { it.id }.toSet()
            taskPathTable.filterValues {
                (it.ancestor in taskIds || it.descendant in taskIds) && it.depth > 0
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
    }
}
