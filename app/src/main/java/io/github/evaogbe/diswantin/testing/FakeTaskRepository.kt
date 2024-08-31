package io.github.evaogbe.diswantin.testing

import io.github.evaogbe.diswantin.task.data.EditTaskForm
import io.github.evaogbe.diswantin.task.data.NewTaskForm
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.task.data.TaskWithTaskList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import java.time.Instant
import kotlin.reflect.KFunction

class FakeTaskRepository(private val db: FakeDatabase = FakeDatabase()) : TaskRepository {
    private val throwingMethods = MutableStateFlow(setOf<String>())

    val tasks
        get() = db.taskTable.value.values

    override fun getCurrentTask(scheduledBefore: Instant, doneBefore: Instant): Flow<Task?> =
        combine(
            throwingMethods,
            db.taskTable,
            db.taskPathTable
        ) { throwingMethods, tasks, taskPaths ->
            if (::getCurrentTask.name in throwingMethods) {
                throw RuntimeException("Test")
            }

            tasks.values.sortedWith(
                compareBy(nullsLast(), Task::scheduledAt)
                    .thenComparing(Task::deadline, nullsLast())
                    .thenComparing(Task::createdAt)
                    .thenComparing(Task::id)
            ).mapNotNull { task ->
                taskPaths.values
                    .filter { path ->
                        path.descendant == task.id && tasks[path.ancestor]?.let {
                            it.doneAt == null || it.recurring && it.doneAt < doneBefore
                        } == true
                    }
                    .maxByOrNull { it.depth }
                    ?.let { tasks[it.ancestor] }
            }.firstOrNull { task ->
                task.scheduledAt?.let { it <= scheduledBefore } != false
            }
        }

    override fun getById(id: Long): Flow<Task?> =
        combine(throwingMethods, db.taskTable) { throwingMethods, tasks ->
            if (::getById.name in throwingMethods) {
                throw RuntimeException("Test")
            }

            tasks[id]
        }

    override fun getTaskWithTaskListById(id: Long): Flow<TaskWithTaskList?> =
        combine(
            throwingMethods,
            db.taskListTable,
            db.taskTable,
        ) { throwingMethods, taskLists, tasks ->
            if (::getTaskWithTaskListById.name in throwingMethods) {
                throw RuntimeException("Test")
            }

            tasks[id]?.let { task ->
                TaskWithTaskList(
                    id = task.id,
                    name = task.name,
                    deadline = task.deadline,
                    scheduledAt = task.scheduledAt,
                    doneAt = task.doneAt,
                    recurring = task.recurring,
                    listId = task.listId,
                    listName = task.listId?.let { taskLists[it] }?.name,
                )
            }
        }

    override fun search(
        query: String,
        singletonsOnly: Boolean
    ): Flow<List<Task>> =
        combine(throwingMethods, db.taskTable) { throwingMethods, tasks ->
            if (::search.name in throwingMethods) {
                throw RuntimeException("Test")
            }

            if (singletonsOnly) {
                tasks.values.filter {
                    it.name.contains(query, ignoreCase = true) && it.listId == null
                }
            } else {
                tasks.values.filter {
                    it.name.contains(query, ignoreCase = true)
                }
            }
        }

    override suspend fun create(form: NewTaskForm): Task {
        if (::create.name in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        return db.insertTask(form.newTask)
    }

    override suspend fun update(form: EditTaskForm): Task {
        if (object {}.javaClass.enclosingMethod?.name in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        db.updateTask(form.updatedTask)
        return form.updatedTask
    }

    override suspend fun update(task: Task) {
        if (object {}.javaClass.enclosingMethod?.name in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        db.updateTask(task)
    }

    override suspend fun delete(id: Long) {
        if (::delete.name in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        db.deleteTask(id)
    }

    fun setThrows(method: KFunction<*>, shouldThrow: Boolean) {
        setThrows(method.name, shouldThrow)
    }

    fun setThrows(methodName: String, shouldThrow: Boolean) {
        if (shouldThrow) {
            throwingMethods.update { it + methodName }
        } else {
            throwingMethods.update { it - methodName }
        }
    }

    companion object {
        fun withTasks(vararg initialTasks: Task) = withTasks(initialTasks.toSet())

        fun withTasks(initialTasks: Iterable<Task>): FakeTaskRepository {
            val db = FakeDatabase()
            initialTasks.forEach(db::insertTask)
            return FakeTaskRepository(db)
        }
    }
}
