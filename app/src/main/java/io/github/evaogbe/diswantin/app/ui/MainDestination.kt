package io.github.evaogbe.diswantin.app.ui

import io.github.evaogbe.diswantin.ui.navigation.NavArguments

sealed interface MainDestination {
    val route: String

    data class TaskDetail(private val id: Long) : MainDestination {
        override val route = "task/$id"

        companion object : MainDestination {
            override val route = "task/{${NavArguments.ID_KEY}}"
        }
    }

    data object NewTaskForm : MainDestination {
        override val route = "taskForm/new"

        data class Main(private val name: String?) : MainDestination {
            override val route = name?.let {
                "taskForm/new/main?${NavArguments.NAME_KEY}=$it"
            } ?: "taskForm/new/main"

            companion object : MainDestination {
                override val route =
                    "taskForm/new/main?${NavArguments.NAME_KEY}={${NavArguments.NAME_KEY}}"
            }
        }

        data object Recurrence : MainDestination {
            override val route = "taskForm/new/recurrence"
        }
    }

    data object EditTaskForm : MainDestination {
        override val route = "taskForm/edit"

        data class Main(private val id: Long) : MainDestination {
            override val route = "taskForm/edit/main/$id"

            companion object : MainDestination {
                override val route = "taskForm/edit/main/{${NavArguments.ID_KEY}}"
            }
        }

        data object Recurrence : MainDestination {
            override val route = "taskForm/edit/recurrence"
        }
    }

    data class TaskCategoryDetail(private val id: Long) : MainDestination {
        override val route = "taskCategory/$id"

        companion object : MainDestination {
            override val route = "taskCategory/{${NavArguments.ID_KEY}}"
        }
    }

    data class NewTaskCategoryForm(private val name: String?) : MainDestination {
        override val route = name?.let {
            "taskCategoryForm/new?${NavArguments.NAME_KEY}=$it"
        } ?: "taskCategoryForm/new"

        companion object : MainDestination {
            override val route =
                "taskCategoryForm/new?${NavArguments.NAME_KEY}={${NavArguments.NAME_KEY}}"
        }
    }

    data class EditTaskCategoryForm(private val id: Long) : MainDestination {
        override val route = "taskCategoryForm/edit/$id"

        companion object : MainDestination {
            override val route = "taskCategoryForm/edit/{${NavArguments.ID_KEY}}"
        }
    }

    data object TaskSearch : MainDestination {
        override val route = "taskSearch"
    }
}
