package io.github.evaogbe.diswantin.task.data

data class EditTaskListForm(
    private val name: String,
    private val tasks: List<Task>,
    private val taskListWithTasks: TaskListWithTasks,
) {
    val taskPathTaskIdsToRemove: List<Long>

    val taskPathsToInsert: List<TaskPath>

    init {
        require(name.isNotBlank()) { "Name must be present" }
        val taskPathTaskIdsToKeep =
            taskListWithTasks.tasks.zip(tasks)
                .filter { (oldTask, newTask) -> oldTask == newTask }
                .map { (oldTask) -> oldTask.id }
                .toSet()
        taskPathTaskIdsToRemove =
            taskListWithTasks.tasks.filterNot { it.id in taskPathTaskIdsToKeep }.map { it.id }
        taskPathsToInsert =
            tasks.flatMapIndexed { i, ancestor ->
                tasks.drop(i + 1).mapIndexed { j, descendant ->
                    TaskPath(ancestor = ancestor.id, descendant = descendant.id, depth = j + 1)
                }
            }
                .filterNot {
                    it.ancestor in taskPathTaskIdsToKeep && it.descendant in taskPathTaskIdsToKeep
                }
    }

    val updatedTaskListWithTasks = TaskListWithTasks(
        taskListWithTasks.taskList.copy(name = name.trim()),
        tasks.map { it.copy(listId = taskListWithTasks.taskList.id) },
    )

    val taskIdsToRemove = taskListWithTasks.tasks.filterNot { it in tasks }.map { it.id }

    val taskIdsToInsert = tasks.filterNot { it in taskListWithTasks.tasks }.map { it.id }
}
