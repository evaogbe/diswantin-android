package io.github.evaogbe.diswantin.task.data

data class EditTaskListForm(
    private val name: String,
    val tasks: List<Task>,
    private val existingTaskListWithTasks: TaskListWithTasks,
) {
    val taskIdsToRemove: List<Long>

    val taskIdsToInsert: List<Long>

    val taskPathTaskIdsToRemove: List<Long>

    val taskPathsToInsert: List<TaskPath>

    init {
        require(name.isNotBlank()) { "Name must be present" }

        val oldTaskIds = existingTaskListWithTasks.tasks.map { it.id }
        val newTaskIds = tasks.map { it.id }
        taskIdsToRemove = oldTaskIds.filterNot { it in newTaskIds }
        taskIdsToInsert = newTaskIds.filterNot { it in oldTaskIds }
        val taskPathTaskIdsToKeep =
            oldTaskIds.zip(newTaskIds)
                .filter { (oldId, newId) -> oldId == newId }
                .map { (oldId) -> oldId }
                .toSet()
        taskPathTaskIdsToRemove = oldTaskIds.filterNot { it in taskPathTaskIdsToKeep }
        taskPathsToInsert =
            newTaskIds.flatMapIndexed { i, ancestor ->
                newTaskIds.drop(i + 1).mapIndexed { j, descendant ->
                    TaskPath(ancestor = ancestor, descendant = descendant, depth = j + 1)
                }
            }
                .filterNot {
                    it.ancestor in taskPathTaskIdsToKeep && it.descendant in taskPathTaskIdsToKeep
                }
    }

    val updatedTaskList = existingTaskListWithTasks.taskList.copy(name = name.trim())
}
