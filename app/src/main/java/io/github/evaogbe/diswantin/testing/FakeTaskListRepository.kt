package io.github.evaogbe.diswantin.testing

import io.github.evaogbe.diswantin.task.data.EditTaskListForm
import io.github.evaogbe.diswantin.task.data.NewTaskListForm
import io.github.evaogbe.diswantin.task.data.TaskItem
import io.github.evaogbe.diswantin.task.data.TaskList
import io.github.evaogbe.diswantin.task.data.TaskListRepository
import io.github.evaogbe.diswantin.task.data.TaskListWithTaskItems
import io.github.evaogbe.diswantin.task.data.TaskListWithTasks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty0

class FakeTaskListRepository(private val db: FakeDatabase = FakeDatabase()) : TaskListRepository {
    private val throwingMethods = MutableStateFlow(setOf<KFunction<*>>())

    val taskLists
        get() = db.taskListTable.value.values

    override val taskListsStream: Flow<List<TaskList>> =
        combine(throwingMethods, db.taskListTable) { throwingMethods, taskLists ->
            if (::taskListsStream::get in throwingMethods) {
                throw RuntimeException("Test")
            }

            taskLists.values.sortedBy { it.name }
        }

    override fun getTaskListWithTasksById(id: Long): Flow<TaskListWithTasks> =
        combine(
            throwingMethods,
            db.taskListTable,
            db.taskTable,
            db.taskPathTable,
        ) { throwingMethods, taskLists, tasks, taskPaths ->
            if (::getTaskListWithTasksById in throwingMethods) {
                throw RuntimeException("Test")
            }

            TaskListWithTasks(
                checkNotNull(taskLists[id]),
                tasks.values
                    .filter { it.listId == id }
                    .sortedByDescending { task ->
                        taskPaths.values.filter { it.ancestor == task.id }.maxOf { it.depth }
                    },
            )
        }

    override fun getTaskListWithTaskItemsById(id: Long): Flow<TaskListWithTaskItems?> =
        combine(
            throwingMethods,
            db.taskListTable,
            db.taskTable,
            db.taskPathTable,
            db.taskCompletionTable,
        ) { throwingMethods, taskLists, tasks, taskPaths, taskCompletions ->
            if (::getTaskListWithTaskItemsById in throwingMethods) {
                throw RuntimeException("Test")
            }

            taskLists[id]?.let { taskList ->
                TaskListWithTaskItems(
                    taskList,
                    tasks.values.filter { it.listId == id }.map { task ->
                        TaskItem(
                            id = task.id,
                            name = task.name,
                            recurring = task.recurring,
                            doneAt = taskCompletions.values
                                .filter { it.taskId == task.id }
                                .maxOfOrNull { it.doneAt },
                        )
                    }.sortedByDescending { task ->
                        taskPaths.values.filter { it.ancestor == task.id }.maxOf { it.depth }
                    }
                )
            }
        }

    override suspend fun create(form: NewTaskListForm): TaskListWithTasks {
        if (::create in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        val taskList = db.insertTaskList(form.newTaskList, form.newTaskIds.toSet(), form.taskPaths)
        return TaskListWithTasks(taskList, form.tasks.map { it.copy(listId = taskList.id) })
    }

    override suspend fun update(form: EditTaskListForm): TaskListWithTasks {
        if (::update in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        db.updateTaskList(
            taskList = form.updatedTaskList,
            taskIdsToInsert = form.taskIdsToInsert,
            taskPathsToInsert = form.taskPathsToInsert,
            taskIdsToRemove = form.taskIdsToRemove,
            taskPathTaskIdsToRemove = form.taskPathTaskIdsToRemove.toSet(),
        )
        return TaskListWithTasks(
            form.updatedTaskList,
            form.tasks.map { it.copy(listId = form.updatedTaskList.id) },
        )
    }

    override suspend fun delete(taskList: TaskList) {
        if (this::delete in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        db.deleteTaskList(taskList.id)
    }

    fun setThrows(property: KProperty0<*>, shouldThrow: Boolean) {
        setThrows(property::get, shouldThrow)
    }

    fun setThrows(method: KFunction<*>, shouldThrow: Boolean) {
        if (shouldThrow) {
            throwingMethods.update { it + method }
        } else {
            throwingMethods.update { it - method }
        }
    }

    companion object {
        fun withTaskLists(vararg taskListsWithTasks: TaskListWithTasks) =
            withTaskLists(taskListsWithTasks.toSet())

        fun withTaskLists(taskListsWithTasks: Iterable<TaskListWithTasks>): FakeTaskListRepository {
            val db = FakeDatabase()
            taskListsWithTasks.forEach { taskListWithTasks ->
                taskListWithTasks.tasks.forEach(db::insertTask)
                db.insertTaskList(taskListWithTasks.taskList, taskListWithTasks.tasks.map { it.id })
            }
            return FakeTaskListRepository(db)
        }
    }
}
