package io.github.evaogbe.diswantin.testing

import io.github.evaogbe.diswantin.task.data.CurrentTaskParams
import io.github.evaogbe.diswantin.task.data.EditTaskForm
import io.github.evaogbe.diswantin.task.data.NewTaskForm
import io.github.evaogbe.diswantin.task.data.PathUpdateType
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskCompletion
import io.github.evaogbe.diswantin.task.data.TaskDetail
import io.github.evaogbe.diswantin.task.data.TaskItem
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.task.data.TaskSearchCriteria
import io.github.evaogbe.diswantin.task.data.TaskSkip
import io.github.evaogbe.diswantin.task.data.doesRecurOnDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class FakeTaskRepository(
    private val db: FakeDatabase = FakeDatabase(),
    private val clock: Clock = Clock.systemDefaultZone(),
) : TaskRepository {
    val tasks
        get() = db.taskTable.value.values

    override fun getCurrentTask(params: CurrentTaskParams): Flow<Task?> =
        combine(
            db.taskTable,
            db.taskPathTable,
            db.taskCompletionTable,
            db.taskRecurrenceTable,
            db.taskSkipTable,
        ) { tasks, taskPaths, taskCompletions, taskRecurrences, taskSkips ->
            tasks.values
                .sortedWith(
                    compareBy<Task, ZonedDateTime?>(nullsLast()) {
                        dateTimePartsToZonedDateTime(
                            it.scheduledDate,
                            it.scheduledTime,
                            LocalTime.MIN,
                        )
                    }
                        .thenComparing({ task ->
                            dateTimePartsToZonedDateTime(
                                task.deadlineDate,
                                task.deadlineTime,
                                LocalTime.MAX,
                            ) ?: if (taskRecurrences.values.any { it.taskId == task.id }) {
                                params.endOfToday
                            } else {
                                null
                            }
                        }, nullsLast())
                        .thenComparing { task ->
                            !taskRecurrences.values.any { it.taskId == task.id }
                        }
                        .thenComparing({
                            dateTimePartsToZonedDateTime(
                                it.startAfterDate,
                                it.startAfterTime,
                                LocalTime.MIN,
                            )
                        }, nullsFirst())
                        .thenComparing(Task::createdAt)
                        .thenComparing(Task::id)
                )
                .filter { task ->
                    val doneAt = taskCompletions.values
                        .filter { it.taskId == task.id }
                        .maxOfOrNull { it.doneAt }
                    doneAt == null ||
                            (taskRecurrences.values.any { it.taskId == task.id } &&
                                    doneAt < params.startOfToday)
                }
                .mapNotNull { descTask ->
                    taskPaths.values
                        .filter { path ->
                            val doneAt = taskCompletions.values
                                .filter { it.taskId == path.ancestor }
                                .maxOfOrNull { it.doneAt }
                            path.descendant == descTask.id && tasks[path.ancestor]?.let { ancTask ->
                                val recurrences = taskRecurrences.values.filter {
                                    it.taskId == ancTask.id
                                }
                                if (recurrences.isEmpty()) {
                                    doneAt == null
                                } else {
                                    doesRecurOnDate(recurrences, params.today) &&
                                            (doneAt == null || doneAt < params.startOfToday)
                                }
                            } == true
                        }
                        .maxByOrNull { it.depth }
                        ?.let { tasks[it.ancestor] }
                }.firstOrNull { task ->
                    task.scheduledDate?.let { it <= params.today } != false &&
                            task.scheduledTime?.let { it <= params.currentTime } != false &&
                            task.startAfterDate?.let { it <= params.today } != false &&
                            task.startAfterTime?.let { it <= params.currentTime } != false &&
                            taskSkips.values
                                .filter { it.taskId == task.id }
                                .maxOfOrNull { it.skippedAt }
                                ?.let { it < params.startOfToday } != false
                }
        }

    private fun dateTimePartsToZonedDateTime(
        date: LocalDate?,
        time: LocalTime?,
        defaultTime: LocalTime,
    ) = when {
        date != null -> date.atTime(time ?: defaultTime).atZone(ZoneId.systemDefault())
        time != null -> ZonedDateTime.now(clock).with(time)
        else -> null
    }

    override fun getById(id: Long) = db.taskTable.map { checkNotNull(it[id]) }

    override fun getTaskDetailById(id: Long): Flow<TaskDetail?> =
        combine(
            db.taskCategoryTable,
            db.taskTable,
            db.taskCompletionTable,
            db.taskPathTable,
        ) { taskCategories, tasks, taskCompletions, taskPaths ->
            val parentId =
                taskPaths.values.firstOrNull { it.descendant == id && it.depth == 1 }?.ancestor
            tasks[id]?.let { task ->
                TaskDetail(
                    id = task.id,
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = task.scheduledTime,
                    doneAt = taskCompletions.values
                        .filter { it.taskId == task.id }
                        .maxOfOrNull { it.doneAt },
                    categoryId = task.categoryId,
                    categoryName = task.categoryId?.let { taskCategories[it] }?.name,
                    parentId = parentId,
                    parentName = parentId?.let { tasks[it] }?.name,
                )
            }
        }

    override fun search(query: String) =
        db.taskTable.map { tasks ->
            tasks.values.filter { it.name.contains(query, ignoreCase = true) }
        }

    override fun searchTaskItems(criteria: TaskSearchCriteria) =
        combine(
            db.taskTable,
            db.taskCompletionTable,
            db.taskRecurrenceTable,
        ) { tasks, taskCompletions, taskRecurrences ->
            tasks.values
                .map { task ->
                    task to taskRecurrences.values.filter { it.taskId == task.id }
                }
                .filter { (task, recurrences) ->
                    task.name.contains(criteria.name, ignoreCase = true) &&
                            criteria.deadlineDateRange?.let { (start, end) ->
                                if (recurrences.isEmpty()) {
                                    task.deadlineDate?.let { it in (start..end) } == true
                                } else {
                                    task.deadlineTime != null && generateSequence(start) {
                                        if (it < end) it.plusDays(1) else null
                                    }.any { doesRecurOnDate(recurrences, it) }
                                }
                            } != false &&
                            criteria.startAfterDateRange?.let { (start, end) ->
                                if (recurrences.isEmpty()) {
                                    task.startAfterDate?.let { it in (start..end) } == true
                                } else {
                                    task.startAfterTime != null && generateSequence(start) {
                                        if (it < end) it.plusDays(1) else null
                                    }.any { doesRecurOnDate(recurrences, it) }
                                }
                            } != false &&
                            criteria.scheduledDateRange?.let { (start, end) ->
                                if (recurrences.isEmpty()) {
                                    task.scheduledDate?.let { it in (start..end) } == true
                                } else {
                                    task.scheduledTime != null && generateSequence(start) {
                                        if (it < end) it.plusDays(1) else null
                                    }.any { doesRecurOnDate(recurrences, it) }
                                }
                            } != false
                }
                .map { (task, recurrences) ->
                    TaskItem(
                        id = task.id,
                        name = task.name,
                        recurring = recurrences.isNotEmpty(),
                        doneAt = taskCompletions.values
                            .filter { it.taskId == task.id }
                            .maxOfOrNull { it.doneAt },
                    )
                }
        }

    override fun getParent(id: Long) =
        combine(db.taskTable, db.taskPathTable) { tasks, taskPaths ->
            taskPaths.values.firstOrNull { it.descendant == id && it.depth == 1 }?.let {
                tasks[it.ancestor]
            }
        }

    override fun getChildren(id: Long) =
        combine(
            db.taskTable,
            db.taskPathTable,
            db.taskRecurrenceTable,
            db.taskCompletionTable,
        ) { tasks, taskPaths, taskRecurrences, taskCompletions ->
            taskPaths.values
                .filter { it.ancestor == id && it.depth == 1 }
                .mapNotNull { tasks[it.descendant] }
                .sortedWith(
                    compareBy(nullsLast(), Task::scheduledDate)
                        .thenComparing(Task::scheduledTime, nullsLast())
                        .thenComparing { task ->
                            !taskRecurrences.values.any { it.taskId == task.id }
                        }
                        .thenComparing(Task::deadlineDate, nullsLast())
                        .thenComparing(Task::deadlineTime, nullsLast())
                        .thenComparing(Task::startAfterDate, nullsFirst())
                        .thenComparing(Task::startAfterTime, nullsFirst())
                        .thenComparing(Task::createdAt)
                        .thenComparing(Task::id)
                )
                .map { task ->
                    TaskItem(
                        id = task.id,
                        name = task.name,
                        recurring = taskRecurrences.values.any { it.taskId == task.id },
                        doneAt = taskCompletions.values
                            .filter { it.taskId == task.id }
                            .maxOfOrNull { it.doneAt },
                    )
                }
        }

    override fun getTaskRecurrencesByTaskId(taskId: Long) =
        db.taskRecurrenceTable.map { taskRecurrences ->
            taskRecurrences.values.filter { it.taskId == taskId }.sortedBy { it.start }
        }

    override fun getCount() = db.taskTable.map { it.size.toLong() }

    override fun getCompletionCount() = db.taskCompletionTable.map { it.size.toLong() }

    override suspend fun create(form: NewTaskForm): Task {
        val task = db.insertTask(form.newTask)
        form.recurrences.forEach { db.insertTaskRecurrence(it.copy(taskId = task.id)) }

        if (form.parentTaskId != null) {
            db.insertChain(parentId = form.parentTaskId, childId = task.id)
        }

        return task
    }

    override suspend fun update(form: EditTaskForm): Task {
        db.updateTask(form.updatedTask)
        form.recurrencesToRemove.forEach { db.deleteTaskRecurrence(it.id) }
        form.recurrencesToAdd.forEach(db::insertTaskRecurrence)

        when (form.parentUpdateType) {
            is PathUpdateType.Keep -> {}
            is PathUpdateType.Remove -> {
                db.deleteTaskPathAncestors(form.updatedTask.id)
            }

            is PathUpdateType.Replace -> {
                db.connectTaskPath(
                    parentId = form.parentUpdateType.id,
                    childId = form.updatedTask.id,
                )
            }
        }

        return form.updatedTask
    }

    override suspend fun delete(id: Long) {
        db.deleteTask(id)
    }

    override suspend fun markDone(id: Long) {
        db.insertTaskCompletion(TaskCompletion(taskId = id, doneAt = Instant.now(clock)))
    }

    override suspend fun unmarkDone(id: Long) {
        db.deleteLatestTaskCompletionByTaskId(id)
    }

    override suspend fun skip(id: Long) {
        db.insertTaskSkip(TaskSkip(taskId = id, skippedAt = Instant.now(clock)))
    }

    companion object {
        fun withTasks(initialTasks: Iterable<Task>): FakeTaskRepository {
            val db = FakeDatabase()
            initialTasks.forEach(db::insertTask)
            return FakeTaskRepository(db)
        }
    }
}
