package io.github.evaogbe.diswantin.testing

import io.github.evaogbe.diswantin.task.data.EditTaskListForm
import io.github.evaogbe.diswantin.task.data.NewTaskListForm
import io.github.evaogbe.diswantin.task.data.TaskList
import io.github.evaogbe.diswantin.task.data.TaskListRepository
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

    override fun getById(id: Long): Flow<TaskListWithTasks?> =
        combine(
            throwingMethods,
            db.taskListTable,
            db.taskTable
        ) { throwingMethods, taskLists, tasks ->
            if (::getById in throwingMethods) {
                throw RuntimeException("Test")
            }

            taskLists[id]?.let { taskList ->
                TaskListWithTasks(
                    taskList,
                    tasks.values.filter { it.listId == id },
                )
            }
        }

    override suspend fun create(form: NewTaskListForm): TaskListWithTasks {
        if (::create in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        return db.insertTaskList(form.newTaskListWithTasks, form.taskPaths)
    }

    override suspend fun update(form: EditTaskListForm): TaskListWithTasks {
        if (::update in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        db.updateTaskList(
            taskList = form.updatedTaskListWithTasks.taskList,
            taskIdsToInsert = form.taskIdsToInsert,
            taskPathsToInsert = form.taskPathsToInsert,
            taskIdsToRemove = form.taskIdsToRemove,
            taskPathTaskIdsToRemove = form.taskPathTaskIdsToRemove.toSet(),
        )
        return form.updatedTaskListWithTasks
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
            taskListsWithTasks.forEach(db::insertTaskList)
            return FakeTaskListRepository(db)
        }
    }
}
