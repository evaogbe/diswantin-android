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
        tailsOnly: Boolean,
        excludeChainFor: Long?
    ): Flow<List<Task>> =
        combine(throwingMethods, tasksTable, taskPaths) { throwingMethods, tasks, taskPaths ->
            if (::search in throwingMethods) {
                throw RuntimeException("Test")
            }

            val excludeIds = if (tailsOnly) {
                taskPaths.filter { it.depth > 0 }
                    .map { it.ancestor }
                    .toSet() + taskPaths.filter { it.ancestor == excludeChainFor }
                    .map { it.descendant }
            } else {
                emptySet()
            }
            tasks.values.filter {
                it.name.contains(query, ignoreCase = true) && it.id !in excludeIds
            }
        }

    override fun getChain(id: Long): Flow<List<Task>> =
        combine(throwingMethods, tasksTable, taskPaths) { throwingMethods, tasks, taskPaths ->
            if (::getChain in throwingMethods) {
                throw RuntimeException("Test")
            }

            val head = taskPaths.filter { it.descendant == id }.maxByOrNull { it.depth }
            taskPaths.filter { it.ancestor == head?.ancestor }
                .sortedByDescending { it.depth }
                .mapNotNull { tasks[it.descendant] }
        }

    override fun getParent(id: Long): Flow<Task?> =
        combine(throwingMethods, tasksTable, taskPaths) { throwingMethods, tasks, taskPaths ->
            if (::getParent in throwingMethods) {
                throw RuntimeException("Test")
            }

            taskPaths.firstOrNull { it.descendant == id && it.depth == 1 }?.let {
                tasks[it.ancestor]
            }
        }

    override fun hasTasks(excludeChainFor: Long?): Flow<Boolean> =
        combine(throwingMethods, tasksTable, taskPaths) { throwingMethods, tasks, taskPaths ->
            if (::hasTasks in throwingMethods) {
                throw RuntimeException("Test")
            }

            if (excludeChainFor == null) {
                tasks.isNotEmpty()
            } else {
                val chainIds = taskPaths.filter {
                    it.descendant == excludeChainFor
                }.map {
                    it.ancestor
                }.toSet() + taskPaths.filter {
                    it.ancestor == excludeChainFor
                }.map {
                    it.descendant
                }
                tasks.values.any { it.id !in chainIds }
            }
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
        taskPaths.update { paths ->
            paths + singletonPath + paths.filter {
                it.descendant == form.prevTaskId
            }.map {
                TaskPath(
                    id = ++taskPathIdGen,
                    ancestor = it.id,
                    descendant = task.id,
                    depth = it.depth + 1
                )
            }
        }
        return task
    }

    override suspend fun update(form: EditTaskForm) {
        if (::update in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        val task = form.updatedTask
        tasksTable.update { it + (task.id to task) }

        if (form.parentId != form.oldParentId) {
            taskPaths.update { paths ->
                val pathsToRemove = form.oldParentId?.let { oldParentId ->
                    val ancestors =
                        paths.filter { it.descendant == oldParentId }.map { it.ancestor }.toSet()
                    val descendants =
                        paths.filter { it.ancestor == task.id }.map { it.descendant }.toSet()
                    paths.filter { it.ancestor in ancestors && it.descendant in descendants }
                        .toSet()
                } ?: emptySet()
                val pathsToAdd = form.parentId?.let { parentId ->
                    paths.filter {
                        it.descendant == parentId
                    }.map {
                        TaskPath(
                            id = ++taskPathIdGen,
                            ancestor = it.id,
                            descendant = task.id,
                            depth = it.depth + 1
                        )
                    } + paths.filter {
                        it.descendant == parentId
                    }.flatMap { p1 ->
                        paths.filter {
                            it.ancestor == task.id && it.depth > 0
                        }.map { p2 ->
                            TaskPath(
                                id = ++taskPathIdGen,
                                ancestor = p1.ancestor,
                                descendant = p2.descendant,
                                depth = p1.depth + p2.depth + 1,
                            )
                        }
                    }
                } ?: emptySet()
                paths - pathsToRemove + pathsToAdd
            }
        }
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
