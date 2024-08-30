package io.github.evaogbe.diswantin.testing

import io.github.evaogbe.diswantin.task.data.EditTaskForm
import io.github.evaogbe.diswantin.task.data.NewTaskForm
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskPath
import io.github.evaogbe.diswantin.task.data.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import java.time.Instant
import kotlin.reflect.KFunction

class FakeTaskRepository(initialTasks: Collection<Task>) : TaskRepository {
    constructor(vararg initialTasks: Task) : this(initialTasks.toSet())

    private val throwingMethods = MutableStateFlow(setOf<KFunction<*>>())

    private var taskIdGen = initialTasks.maxOfOrNull { it.id } ?: 0L

    private var taskPathIdGen = 0L

    private val tasksTable = MutableStateFlow(initialTasks.associateBy { it.id })

    private val taskPaths = MutableStateFlow(initialTasks.map { task ->
        TaskPath(
            id = ++taskPathIdGen,
            ancestor = task.id,
            descendant = task.id,
            depth = 0,
        )
    }.toSet())

    val tasks
        get() = tasksTable.value.values

    override fun getCurrentTask(scheduledBefore: Instant): Flow<Task?> =
        combine(throwingMethods, tasksTable, taskPaths) { throwingMethods, tasks, taskPaths ->
            if (::getCurrentTask in throwingMethods) {
                throw RuntimeException("Test")
            }

            tasks.values.sortedWith(
                compareBy(nullsLast(), Task::scheduledAt)
                    .thenComparing(Task::dueAt, nullsLast())
                    .thenComparing(Task::createdAt)
                    .thenComparing(Task::id)
            ).mapNotNull { task ->
                val path = taskPaths.filter { it.descendant == task.id }.maxBy { it.depth }
                tasks[path.ancestor]
            }.firstOrNull { task ->
                task.scheduledAt?.let { it <= scheduledBefore } != false
            }
        }

    override fun getById(id: Long): Flow<Task?> =
        combine(throwingMethods, tasksTable) { throwingMethods, tasks ->
            if (::getById in throwingMethods) {
                throw RuntimeException("Test")
            }

            tasks[id]
        }

    override fun search(
        query: String,
        singletonsOnly: Boolean
    ): Flow<List<Task>> =
        combine(throwingMethods, tasksTable) { throwingMethods, tasks ->
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

    override fun getTaskListItems(id: Long): Flow<List<Task>> =
        combine(throwingMethods, tasksTable, taskPaths) { throwingMethods, tasks, taskPaths ->
            if (::getTaskListItems in throwingMethods) {
                throw RuntimeException("Test")
            }

            val head = taskPaths.filter { it.descendant == id }.maxByOrNull { it.depth }
            taskPaths.filter { it.ancestor == head?.ancestor }
                .sortedByDescending { it.depth }
                .mapNotNull { tasks[it.descendant] }
        }

    override suspend fun create(form: NewTaskForm): Task {
        if (::create in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        val task = form.newTask.copy(id = ++taskIdGen)
        val singletonPath = TaskPath(
            id = ++taskPathIdGen,
            ancestor = task.id,
            descendant = task.id,
            depth = 0,
        )
        tasksTable.update { it + (task.id to task) }
        taskPaths.update { it + singletonPath }
        return task
    }

    override suspend fun update(form: EditTaskForm): Task {
        if (::update in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        val task = form.updatedTask
        tasksTable.update { it + (task.id to task) }
        return task
    }

    override suspend fun remove(id: Long) {
        if (::remove in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        taskPaths.update { paths ->
            val parentId = paths.firstOrNull { it.descendant == id && it.depth == 1 }?.let {
                tasksTable.value[it.ancestor]
            }?.id
            val childId = paths.firstOrNull { it.ancestor == id && it.depth == 1 }?.let {
                tasksTable.value[it.descendant]
            }?.id
            val chainPaths = if (parentId != null && childId != null) {
                val ancestors =
                    paths.filter { it.descendant == parentId }.map { it.ancestor }.toSet()
                val descendants =
                    paths.filter { it.ancestor == childId }.map { it.descendant }.toSet()
                paths.filter { it.ancestor in ancestors && it.descendant in descendants }.toSet()
            } else {
                emptySet()
            }
            paths - paths.filter { it.ancestor == id || it.descendant == id }.toSet() -
                    chainPaths +
                    chainPaths.map { it.copy(depth = it.depth - 1) }
        }
        tasksTable.update { it - id }
    }

    fun setThrows(method: KFunction<*>, shouldThrow: Boolean) {
        if (shouldThrow) {
            throwingMethods.update { it + method }
        } else {
            throwingMethods.update { it - method }
        }
    }
}
