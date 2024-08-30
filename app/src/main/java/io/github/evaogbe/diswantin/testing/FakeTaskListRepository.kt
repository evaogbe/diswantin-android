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

class FakeTaskListRepository(initialTaskLists: List<TaskList>) : TaskListRepository {
    constructor(vararg initialTaskLists: TaskList) : this(initialTaskLists.toList())

    private val throwingMethods = MutableStateFlow(setOf<KFunction<*>>())

    private var taskListIdGen = initialTaskLists.maxOfOrNull { it.id } ?: 0L

    private val taskListsState = MutableStateFlow(initialTaskLists)

    val taskLists
        get() = taskListsState.value

    override val taskListsStream: Flow<List<TaskList>> =
        combine(throwingMethods, taskListsState) { throwingMethods, taskLists ->
            if (::taskListsStream::get in throwingMethods) {
                throw RuntimeException("Test")
            }

            taskLists.sortedBy { it.name }
        }

    override suspend fun create(form: NewTaskListForm): TaskListWithTasks {
        if (::create in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        val taskList = form.newTaskListWithTasks.taskList.copy(id = ++taskListIdGen)
        taskListsState.update { it + taskList }
        return TaskListWithTasks(
            taskList,
            form.newTaskListWithTasks.tasks.map { it.copy(listId = taskList.id) },
        )
    }

    override suspend fun update(form: EditTaskListForm): TaskListWithTasks {
        if (::update in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        taskListsState.update { taskLists ->
            taskLists.map {
                if (it.id == form.updatedTaskListWithTasks.taskList.id) {
                    form.updatedTaskListWithTasks.taskList
                } else {
                    it
                }
            }
        }
        return form.updatedTaskListWithTasks
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
}
