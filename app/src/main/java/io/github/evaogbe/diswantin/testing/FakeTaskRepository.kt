package io.github.evaogbe.diswantin.testing

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import io.github.evaogbe.diswantin.task.data.CurrentTask
import io.github.evaogbe.diswantin.task.data.CurrentTaskParams
import io.github.evaogbe.diswantin.task.data.DueTask
import io.github.evaogbe.diswantin.task.data.DueTaskWithRecurrences
import io.github.evaogbe.diswantin.task.data.EditTaskForm
import io.github.evaogbe.diswantin.task.data.NewTaskForm
import io.github.evaogbe.diswantin.task.data.PathUpdateType
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskCompletion
import io.github.evaogbe.diswantin.task.data.TaskDetail
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.task.data.TaskSearchCriteria
import io.github.evaogbe.diswantin.task.data.TaskSkip
import io.github.evaogbe.diswantin.task.data.TaskSummary
import io.github.evaogbe.diswantin.task.data.TaskTag
import io.github.evaogbe.diswantin.task.data.doesRecurOnDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

private data class TaskChainInfo(
    val root: Task,
    val recurrence: TaskRecurrence?,
    val scheduledAt: ZonedDateTime,
    val deadline: ZonedDateTime,
    val overdue: Boolean,
)

class FakeTaskRepository(
    private val db: FakeDatabase = FakeDatabase(),
    private val clock: Clock = Clock.systemDefaultZone(),
) : TaskRepository {
    val tasks
        get() = db.taskTable.value.values

    override fun getCurrentTask(params: CurrentTaskParams) = combine(
        db.taskTable,
        db.taskPathTable,
        db.taskCompletionTable,
        db.taskRecurrenceTable,
        db.taskSkipTable,
    ) { tasks, taskPaths, taskCompletions, taskRecurrences, taskSkips ->
        val availableTaskIds = tasks.values.filter { task ->
            val recurrences = taskRecurrences.values.filter { it.taskId == task.id }
            val doneAt =
                taskCompletions.values.filter { it.taskId == task.id }.maxOfOrNull { it.doneAt }
            val isDone = doneAt != null && (recurrences.isEmpty() || doneAt >= params.startOfToday)
            val isSkipped =
                taskSkips.values.filter { it.taskId == task.id }.maxOfOrNull { it.skippedAt }
                    ?.let { it >= params.startOfToday } == true
            val occursToday = recurrences.isEmpty() || doesRecurOnDate(recurrences, params.today)
            !isDone && !isSkipped && occursToday
        }.map { it.id }.toSet()
        val availablePaths = taskPaths.values.filter { path ->
            path.ancestor in availableTaskIds && path.descendant in availableTaskIds
        }
        availablePaths.groupBy { it.descendant }.values.asSequence().mapNotNull { paths ->
            val leaf = paths.maxBy { it.depth }
            tasks[leaf.ancestor]
        }.filter { task ->
            val isScheduledFuture = isScheduledAtFuture(task, params.now)
            val doesStartFuture = doesStartAfterFuture(task, params.now)
            !isScheduledFuture && !doesStartFuture
        }.map { rootTask ->
            val chain = availablePaths.filter { it.ancestor == rootTask.id }.mapNotNull { path ->
                tasks[path.descendant]?.let { descendant ->
                    val recurring = taskRecurrences.values.any { it.taskId == descendant.id }
                    descendant to recurring
                }
            }
            val scheduledAt = chain.minOf { (task) ->
                dateTimePartsToZonedDateTime(
                    task.scheduledDate,
                    task.scheduledTime,
                    LocalTime.MIN,
                ) ?: OffsetDateTime.MAX.toZonedDateTime()
            }
            val deadline = chain.minOf { (task, recurring) ->
                dateTimePartsToZonedDateTime(
                    task.deadlineDate ?: if (recurring) params.today else null,
                    task.deadlineTime,
                    LocalTime.MAX,
                ) ?: OffsetDateTime.MAX.toZonedDateTime()
            }
            val anyOverdue = chain.any { (task, recurring) ->
                isOverdue(task, recurring, params.today, params.overdueTime)
            }
            val recurrence = taskRecurrences.values.firstOrNull { it.taskId == rootTask.id }
            TaskChainInfo(rootTask, recurrence, scheduledAt, deadline, anyOverdue)
        }
            .sortedWith(
                compareByDescending(TaskChainInfo::overdue).thenBy(TaskChainInfo::scheduledAt)
                    .thenBy { it.root.startAfterTime != null }.thenBy(TaskChainInfo::deadline)
                    .thenByDescending(nullsLast()) { it.recurrence?.type }
                    .thenByDescending(nullsLast()) { it.recurrence?.step }.thenBy(nullsFirst()) {
                        dateTimePartsToZonedDateTime(
                            it.root.startAfterDate,
                            it.root.startAfterTime,
                            LocalTime.MIN,
                        )
                    }.thenBy { it.root.createdAt }.thenBy { it.root.id }).firstNotNullOfOrNull {
                CurrentTask(
                    id = it.root.id,
                    name = it.root.name,
                    note = it.root.note,
                    recurring = it.recurrence != null,
                )
            }
    }

    private fun isScheduledAtFuture(task: Task, now: LocalDateTime): Boolean {
        val today = now.toLocalDate()
        val currentTime = now.toLocalTime()

        if (task.scheduledDate != null) {
            if (task.scheduledDate > today) return true
            if (task.scheduledDate < today) return false
        }

        return task.scheduledTime?.let { it > currentTime } == true
    }

    private fun doesStartAfterFuture(task: Task, now: LocalDateTime): Boolean {
        if (task.startAfterDate?.let { it > now.toLocalDate() } == true) return true
        if (task.startAfterTime?.let { it > now.toLocalTime() } == true) return true
        return false
    }

    private fun dateTimePartsToZonedDateTime(
        date: LocalDate?,
        time: LocalTime?,
        defaultTime: LocalTime,
    ) = when {
        date != null -> date.atTime(time ?: defaultTime).atZone(clock.zone)
        time != null -> ZonedDateTime.now(clock).with(time)
        else -> null
    }

    private fun isOverdue(
        task: Task,
        recurring: Boolean,
        today: LocalDate,
        overdueTime: LocalTime,
    ): Boolean {
        if (task.scheduledDate?.let { it < today } == true) {
            return true
        }

        if (task.scheduledTime?.let { it <= overdueTime } == true) {
            if (recurring) return true
            if (task.scheduledDate?.let { it == today } == true) return true
        }

        if (task.deadlineDate?.let { it < today } == true) {
            return true
        }

        if (task.deadlineTime?.let { it <= overdueTime } == true) {
            if (recurring) return true
            if (task.deadlineDate?.let { it == today } == true) return true
        }

        return false
    }

    override fun getTaskById(id: Long) = db.taskTable.map { checkNotNull(it[id]) }

    override fun getTaskDetailById(id: Long): Flow<TaskDetail?> = combine(
        db.taskTable,
        db.taskCompletionTable,
        db.taskPathTable,
        db.taskRecurrenceTable,
    ) { tasks, taskCompletions, taskPaths, taskRecurrences ->
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
                doneAt = taskCompletions.values.filter { it.taskId == task.id }
                    .maxOfOrNull { it.doneAt },
                parentId = parentId,
                parentName = parentId?.let { tasks[it] }?.name,
                parentRecurring = taskRecurrences.values.any { it.taskId == parentId },
                parentDoneAt = taskCompletions.values.filter { it.taskId == parentId }
                    .maxOfOrNull { it.doneAt },
            )
        }
    }

    override fun getTaskSummariesByTagId(tagId: Long) = combine(
        db.taskTable,
        db.taskCompletionTable,
        db.taskRecurrenceTable,
        db.taskTagTable,
    ) { tasks, taskCompletions, taskRecurrences, taskTags ->
        PagingData.from(
            taskTags.values.asSequence().filter { it.tagId == tagId }
                .mapNotNull { tasks[it.taskId] }.sortedWith(
                    compareBy<Task> { task ->
                        taskCompletions.values.any { it.taskId == task.id }
                    }.thenBy(nullsLast(), Task::scheduledDate)
                        .thenBy(nullsLast(), Task::scheduledTime).thenByDescending { task ->
                            taskRecurrences.values.any { it.taskId == task.id }
                        }.thenBy(nullsLast(), Task::deadlineDate)
                        .thenBy(nullsLast(), Task::deadlineTime)
                        .thenBy(nullsFirst(), Task::startAfterDate)
                        .thenBy(nullsFirst(), Task::startAfterTime).thenBy(Task::createdAt)
                        .thenBy(Task::id),
                ).map { task ->
                    TaskSummary(
                        id = task.id,
                        name = task.name,
                        recurring = taskRecurrences.values.any { it.taskId == task.id },
                        doneAt = taskCompletions.values.filter { it.taskId == task.id }
                            .maxOfOrNull { it.doneAt },
                    )
                }.toList(),
            LoadStates(
                refresh = LoadState.NotLoading(endOfPaginationReached = true),
                prepend = LoadState.NotLoading(endOfPaginationReached = true),
                append = LoadState.NotLoading(endOfPaginationReached = true),
            ),
        )
    }

    override fun searchTaskSummaries(criteria: TaskSearchCriteria) = combine(
        db.taskTable,
        db.taskCompletionTable,
        db.taskRecurrenceTable,
    ) { tasks, taskCompletions, taskRecurrences ->
        PagingData.from(
            tasks.values.asSequence().map { task ->
                task to taskRecurrences.values.filter { it.taskId == task.id }
            }.filter { (task, recurrences) ->
                task.name.contains(
                    criteria.name, ignoreCase = true
                ) && criteria.deadlineDateRange?.let { (start, end) ->
                    if (recurrences.isEmpty()) {
                        task.deadlineDate?.let { it in (start..end) } == true
                    } else {
                        task.deadlineTime != null && generateSequence(start) {
                            if (it < end) it.plusDays(1) else null
                        }.any { doesRecurOnDate(recurrences, it) }
                    }
                } != false && criteria.startAfterDateRange?.let { (start, end) ->
                    if (recurrences.isEmpty()) {
                        task.startAfterDate?.let { it in (start..end) } == true
                    } else {
                        task.startAfterTime != null && generateSequence(start) {
                            if (it < end) it.plusDays(1) else null
                        }.any { doesRecurOnDate(recurrences, it) }
                    }
                } != false && criteria.scheduledDateRange?.let { (start, end) ->
                    if (recurrences.isEmpty()) {
                        task.scheduledDate?.let { it in (start..end) } == true
                    } else {
                        task.scheduledTime != null && generateSequence(start) {
                            if (it < end) it.plusDays(1) else null
                        }.any { doesRecurOnDate(recurrences, it) }
                    }
                } != false && criteria.doneDateRange?.let { (start, end) ->
                    val doneStart = start.atStartOfDay(clock.zone).toInstant()
                    val doneEnd = end.atStartOfDay(clock.zone).toInstant()
                    taskCompletions.values.any {
                        it.taskId == task.id && it.doneAt in (doneStart..doneEnd)
                    }
                } != false && criteria.recurrenceDate?.let {
                    doesRecurOnDate(recurrences, it)
                } != false
            }.sortedWith(compareBy<Pair<Task, List<TaskRecurrence>>> { (_, recurrences) ->
                recurrences.isNotEmpty()
            }.thenBy(nullsLast()) { (task) -> task.scheduledDate }
                .thenBy(nullsLast()) { (task) -> task.scheduledTime }
                .thenBy(nullsLast()) { (task) -> task.deadlineDate }
                .thenBy(nullsLast()) { (task) -> task.deadlineTime }
                .thenBy(nullsFirst()) { (task) -> task.startAfterDate }
                .thenBy(nullsFirst()) { (task) -> task.startAfterTime }
                .thenByDescending(nullsFirst()) { (task) ->
                    taskCompletions.values.filter { it.taskId == task.id }.maxOfOrNull { it.doneAt }
                }.thenBy { (task) -> task.name }.thenBy { (task) -> task.id })
                .map { (task, recurrences) ->
                    TaskSummary(
                        id = task.id,
                        name = task.name,
                        recurring = recurrences.isNotEmpty(),
                        doneAt = taskCompletions.values.filter { it.taskId == task.id }
                            .maxOfOrNull { it.doneAt },
                    )
                }.toList(),
            LoadStates(
                refresh = LoadState.NotLoading(endOfPaginationReached = true),
                prepend = LoadState.NotLoading(endOfPaginationReached = true),
                append = LoadState.NotLoading(endOfPaginationReached = true),
            ),
        )
    }

    override fun getTasksDueAt(dueAt: LocalDate, zone: ZoneId) = combine(
        db.taskTable, db.taskRecurrenceTable, db.taskCompletionTable
    ) { tasks, taskRecurrences, taskCompletions ->
        PagingData.from(
            tasks.values.asSequence().map { task ->
                task to taskRecurrences.values.filter { it.taskId == task.id }
            }.filter { (task, recurrences) ->
                val isScheduled = task.scheduledDate == dueAt
                val hasDeadline = task.deadlineDate == dueAt
                val recurring = doesRecurOnDate(recurrences, dueAt)
                val isDone = if (recurring) {
                    val doneAfter = dueAt.atStartOfDay(zone).toInstant()
                    val doneBefore = dueAt.plusDays(1).atStartOfDay(zone).toInstant()
                    taskCompletions.values.any {
                        it.taskId == task.id && it.doneAt in (doneAfter..<doneBefore)
                    }
                } else {
                    taskCompletions.values.any { it.taskId == task.id }
                }
                !isDone && (isScheduled || hasDeadline || recurring)
            }.sortedWith(compareBy<Pair<Task, List<TaskRecurrence>>> { (_, recurrences) ->
                recurrences.isNotEmpty()
            }.thenByDescending(nullsFirst()) { (_, recurrences) ->
                recurrences.firstOrNull()?.type
            }.thenByDescending(nullsFirst()) { (_, recurrences) ->
                recurrences.firstOrNull()?.step
            }.thenBy(nullsLast()) { (task) -> task.scheduledDate }
                .thenBy(nullsLast()) { (task) -> task.scheduledTime }
                .thenBy(nullsLast()) { (task) -> task.deadlineDate }
                .thenBy(nullsLast()) { (task) -> task.deadlineTime }
                .thenBy(nullsFirst()) { (task) -> task.startAfterDate }
                .thenBy(nullsFirst()) { (task) -> task.startAfterTime }
                .thenBy { (task) -> task.createdAt }.thenBy { (task) -> task.id })
                .map { (task, recurrences) ->
                    DueTaskWithRecurrences(
                        task = DueTask(id = task.id, name = task.name),
                        recurrences = recurrences,
                    )
                }.toList(),
            LoadStates(
                refresh = LoadState.NotLoading(endOfPaginationReached = true),
                prepend = LoadState.NotLoading(endOfPaginationReached = true),
                append = LoadState.NotLoading(endOfPaginationReached = true),
            ),
        )
    }

    override fun getParent(id: Long) = combine(db.taskTable, db.taskPathTable) { tasks, taskPaths ->
        taskPaths.values.firstOrNull { it.descendant == id && it.depth == 1 }?.let {
            tasks[it.ancestor]
        }
    }

    override fun getChildren(id: Long) = combine(
        db.taskTable,
        db.taskPathTable,
        db.taskRecurrenceTable,
        db.taskCompletionTable,
    ) { tasks, taskPaths, taskRecurrences, taskCompletions ->
        PagingData.from(
            taskPaths.values.asSequence().filter { it.ancestor == id && it.depth == 1 }
                .mapNotNull { tasks[it.descendant] }.sortedWith(
                    compareBy(nullsLast(), Task::scheduledDate).thenBy(
                        nullsLast(), Task::scheduledTime
                    ).thenByDescending { task ->
                        taskRecurrences.values.any { it.taskId == task.id }
                    }.thenBy(nullsLast(), Task::deadlineDate)
                        .thenBy(nullsLast(), Task::deadlineTime)
                        .thenBy(nullsFirst(), Task::startAfterDate)
                        .thenBy(nullsFirst(), Task::startAfterTime).thenBy(Task::createdAt)
                        .thenBy(Task::id)
                ).map { task ->
                    TaskSummary(
                        id = task.id,
                        name = task.name,
                        recurring = taskRecurrences.values.any { it.taskId == task.id },
                        doneAt = taskCompletions.values.filter { it.taskId == task.id }
                            .maxOfOrNull { it.doneAt },
                    )
                }.toList(),
            LoadStates(
                refresh = LoadState.NotLoading(endOfPaginationReached = true),
                prepend = LoadState.NotLoading(endOfPaginationReached = true),
                append = LoadState.NotLoading(endOfPaginationReached = true),
            ),
        )
    }

    override fun getTaskRecurrencesByTaskId(taskId: Long) =
        db.taskRecurrenceTable.map { taskRecurrences ->
            taskRecurrences.values.filter { it.taskId == taskId }
                .sortedBy(TaskRecurrence::startDate)
        }

    override fun getTaskCount() = db.taskTable.map { it.size.toLong() }

    override suspend fun create(form: NewTaskForm): Task {
        val task = db.insertTask(form.newTask)
        form.tagIds.forEach { db.insertTaskTag(TaskTag(taskId = task.id, tagId = it)) }
        form.recurrences.forEach { db.insertTaskRecurrence(it.copy(taskId = task.id)) }

        if (form.parentTaskId != null) {
            db.insertChain(parentId = form.parentTaskId, childId = task.id)
        }

        return task
    }

    override suspend fun update(form: EditTaskForm): Task {
        db.updateTask(form.updatedTask)
        form.tagIdsToRemove.forEach { db.deleteTaskTag(it) }
        form.tagIdsToAdd.forEach {
            db.insertTaskTag(TaskTag(taskId = form.updatedTask.id, tagId = it))
        }
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
}
