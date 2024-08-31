package io.github.evaogbe.diswantin.ui.navigation

sealed interface Destination {
    val route: String

    data object Home : Destination {
        override val route = "home"
    }

    data object TaskSearch : Destination {
        override val route = "taskSearch"
    }

    data object NewTaskForm : Destination {
        override val route = "taskForm"
    }

    data class EditTaskForm(private val id: Long) : Destination {
        override val route = "taskForm/$id"

        companion object : Destination {
            const val ID_KEY = "id"

            override val route = "taskForm/{$ID_KEY}"
        }
    }

    data object Advice : Destination {
        override val route = "advice"
    }

    data class TaskDetail(private val id: Long) : Destination {
        override val route = "task/$id"

        companion object : Destination {
            const val ID_KEY = "id"

            override val route = "task/{$ID_KEY}"
        }
    }

    data object NewTaskListForm : Destination {
        override val route = "taskListForm"
    }

    data class TaskListDetail(private val id: Long) : Destination {
        override val route = "taskList/$id"

        companion object : Destination {
            const val ID_KEY = "id"

            override val route = "taskList/{$ID_KEY}"
        }
    }
}
