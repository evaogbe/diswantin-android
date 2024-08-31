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
    private val throwingMethods = MutableStateFlow(setOf<KFunction<*>>())

    val tasks
        get() = db.taskTable.value.values

    override fun getCurrentTask(scheduledBefore: Instant): Flow<Task?> =
        combine(
            throwingMethods,
            db.taskTable,
            db.taskPathTable
        ) { throwingMethods, tasks, taskPaths ->
            if (::getCurrentTask in throwingMethods) {
                throw RuntimeException("Test")
            }

            tasks.values.sortedWith(
                compareBy(nullsLast(), Task::scheduledAt)
                    .thenComparing(Task::deadline, nullsLast())
                    .thenComparing(Task::createdAt)
                    .thenComparing(Task::id)
            ).mapNotNull { task ->
                taskPaths.values
                    .filter { it.descendant == task.id }
                    .maxByOrNull { it.depth }
                    ?.let { tasks[it.ancestor] }
            }.firstOrNull { task ->
                task.scheduledAt?.let { it <= scheduledBefore } != false
            }
        }

    override fun getById(id: Long): Flow<Task?> =
        combine(throwingMethods, db.taskTable) { throwingMethods, tasks ->
            if (::getById in throwingMethods) {
                throw RuntimeException("Test")
            }

            tasks[id]
        }

    override fun getTaskWithTaskListById(id: Long): Flow<TaskWithTaskList?> =
        combine(
            throwingMethods,
            db.taskListTable,
            db.taskTable
        ) { throwingMethods, taskLists, tasks ->
            if (::getTaskWithTaskListById in throwingMethods) {
                throw RuntimeException("Test")
            }

            tasks[id]?.let { task ->
                TaskWithTaskList(
                    id = task.id,
                    name = task.name,
                    deadline = task.deadline,
                    scheduledAt = task.scheduledAt,
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
            if (::search in throwingMethods) {
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
        if (::create in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        return db.addTask(form.newTask)
    }

    override suspend fun update(form: EditTaskForm): Task {
        if (::update in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        db.updateTask(form.updatedTask)
        return form.updatedTask
    }

    override suspend fun remove(id: Long) {
        if (::remove in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        db.removeTask(id)
    }

    fun setThrows(method: KFunction<*>, shouldThrow: Boolean) {
        if (shouldThrow) {
            throwingMethods.update { it + method }
        } else {
            throwingMethods.update { it - method }
        }
    }

    companion object {
        fun withTasks(vararg initialTasks: Task) = withTasks(initialTasks.toSet())

        fun withTasks(initialTasks: Iterable<Task>): FakeTaskRepository {
            val db = FakeDatabase()
            initialTasks.forEach(db::addTask)
            return FakeTaskRepository(db)
        }
    }
}
