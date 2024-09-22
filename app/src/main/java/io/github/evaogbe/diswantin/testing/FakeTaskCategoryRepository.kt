package io.github.evaogbe.diswantin.testing

import io.github.evaogbe.diswantin.task.data.EditTaskCategoryForm
import io.github.evaogbe.diswantin.task.data.NewTaskCategoryForm
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.task.data.TaskCategoryRepository
import io.github.evaogbe.diswantin.task.data.TaskCategoryWithTaskItems
import io.github.evaogbe.diswantin.task.data.TaskCategoryWithTasks
import io.github.evaogbe.diswantin.task.data.TaskItem
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class FakeTaskCategoryRepository(private val db: FakeDatabase = FakeDatabase()) :
    TaskCategoryRepository {
    val taskCategories
        get() = db.taskCategoryTable.value.values

    override val categoriesStream =
        db.taskCategoryTable.map { it.values.sortedBy(TaskCategory::name) }

    override fun getCategoryWithTasksById(id: Long) =
        combine(db.taskCategoryTable, db.taskTable) { taskCategories, tasks ->
            TaskCategoryWithTasks(
                checkNotNull(taskCategories[id]),
                tasks.values.filter { it.categoryId == id }.sortedBy(Task::name),
            )
        }

    override fun getCategoryWithTaskItemsById(id: Long) =
        combine(
            db.taskCategoryTable,
            db.taskTable,
            db.taskCompletionTable,
            db.taskRecurrenceTable,
        ) { taskCategories, tasks, taskCompletions, taskRecurrences ->
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
        val taskCategory =
            db.insertTaskCategory(form.newCategory, form.newTaskIds.toSet())
        return TaskCategoryWithTasks(
            taskCategory,
            form.tasks.map { it.copy(categoryId = taskCategory.id) },
        )
    }

    override suspend fun update(form: EditTaskCategoryForm): TaskCategoryWithTasks {
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
        db.deleteTaskCategory(category.id)
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
