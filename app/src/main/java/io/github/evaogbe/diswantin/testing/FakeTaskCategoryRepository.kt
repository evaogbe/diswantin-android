package io.github.evaogbe.diswantin.testing

import io.github.evaogbe.diswantin.task.data.EditTaskCategoryForm
import io.github.evaogbe.diswantin.task.data.NewTaskCategoryForm
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.task.data.TaskCategoryRepository
import io.github.evaogbe.diswantin.task.data.TaskCategoryWithTaskItems
import io.github.evaogbe.diswantin.task.data.TaskCategoryWithTasks
import io.github.evaogbe.diswantin.task.data.TaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty0

class FakeTaskCategoryRepository(private val db: FakeDatabase = FakeDatabase()) :
    TaskCategoryRepository {
    private val throwingMethods = MutableStateFlow(setOf<KFunction<*>>())

    val taskCategories
        get() = db.taskCategoryTable.value.values

    override val categoriesStream: Flow<List<TaskCategory>> =
        combine(throwingMethods, db.taskCategoryTable) { throwingMethods, taskCategories ->
            if (::categoriesStream::get in throwingMethods) {
                throw RuntimeException("Test")
            }

            taskCategories.values.sortedBy(TaskCategory::name)
        }

    override fun getCategoryWithTasksById(id: Long): Flow<TaskCategoryWithTasks> =
        combine(
            throwingMethods,
            db.taskCategoryTable,
            db.taskTable,
        ) { throwingMethods, taskCategories, tasks ->
            if (::getCategoryWithTasksById in throwingMethods) {
                throw RuntimeException("Test")
            }

            TaskCategoryWithTasks(
                checkNotNull(taskCategories[id]),
                tasks.values.filter { it.categoryId == id }.sortedBy(Task::name),
            )
        }

    override fun getCategoryWithTaskItemsById(id: Long): Flow<TaskCategoryWithTaskItems?> =
        combine(
            throwingMethods,
            db.taskCategoryTable,
            db.taskTable,
            db.taskCompletionTable,
            db.taskRecurrenceTable,
        ) { throwingMethods, taskCategories, tasks, taskCompletions, taskRecurrences ->
            if (::getCategoryWithTaskItemsById in throwingMethods) {
                throw RuntimeException("Test")
            }

            taskCategories[id]?.let { taskCategory ->
                TaskCategoryWithTaskItems(
                    taskCategory,
                    tasks.values.filter { it.categoryId == id }.map { task ->
                        TaskItem(
                            id = task.id,
                            name = task.name,
                            recurring = taskRecurrences.values.any { it.taskId == task.id },
                            doneAt = taskCompletions.values
                                .filter { it.taskId == task.id }
                                .maxOfOrNull { it.doneAt },
                        )
                    }.sortedWith(compareBy(TaskItem::doneAt).thenComparing(TaskItem::name))
                )
            }
        }

    override suspend fun create(form: NewTaskCategoryForm): TaskCategoryWithTasks {
        if (::create in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        val taskCategory =
            db.insertTaskCategory(form.newCategory, form.newTaskIds.toSet())
        return TaskCategoryWithTasks(
            taskCategory,
            form.tasks.map { it.copy(categoryId = taskCategory.id) },
        )
    }

    override suspend fun update(form: EditTaskCategoryForm): TaskCategoryWithTasks {
        if (::update in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        db.updateTaskCategory(
            taskCategory = form.updatedCategory,
            taskIdsToInsert = form.taskIdsToInsert,
            taskIdsToRemove = form.taskIdsToRemove,
        )
        return TaskCategoryWithTasks(
            form.updatedCategory,
            form.tasks.map { it.copy(categoryId = form.updatedCategory.id) },
        )
    }

    override suspend fun delete(category: TaskCategory) {
        if (this::delete in throwingMethods.value) {
            throw RuntimeException("Test")
        }

        db.deleteTaskCategory(category.id)
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
        fun withCategories(vararg taskCategoriesWithTasks: TaskCategoryWithTasks) =
            withCategories(taskCategoriesWithTasks.toSet())

        fun withCategories(
            taskCategoriesWithTasks: Iterable<TaskCategoryWithTasks>,
        ): FakeTaskCategoryRepository {
            val db = FakeDatabase()
            taskCategoriesWithTasks.forEach { taskCategoryWithTasks ->
                taskCategoryWithTasks.tasks.forEach(db::insertTask)
                db.insertTaskCategory(
                    taskCategory = taskCategoryWithTasks.category,
                    taskIds = taskCategoryWithTasks.tasks.map { it.id }.toSet(),
                )
            }
            return FakeTaskCategoryRepository(db)
        }
    }
}
