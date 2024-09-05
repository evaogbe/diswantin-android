package io.github.evaogbe.diswantin.app.ui

import io.github.evaogbe.diswantin.ui.navigation.NavArguments

sealed interface TopLevelDestination {
    val route: String

    data class TaskDetail(private val id: Long) : TopLevelDestination {
        override val route = "task/$id"

        companion object : TopLevelDestination {
            override val route = "task/{${NavArguments.ID_KEY}}"
        }
    }

    data class NewTaskForm(private val name: String?) : TopLevelDestination {
        override val route = name?.let { "taskForm?${NavArguments.NAME_KEY}=$it" } ?: "taskForm"

        companion object : TopLevelDestination {
            override val route = "taskForm?${NavArguments.NAME_KEY}={${NavArguments.NAME_KEY}}"
        }
    }

    data class EditTaskForm(private val id: Long) : TopLevelDestination {
        override val route = "taskForm/$id"

        companion object : TopLevelDestination {
            override val route = "taskForm/{${NavArguments.ID_KEY}}"
        }
    }

    data class TaskCategoryDetail(private val id: Long) : TopLevelDestination {
        override val route = "taskCategory/$id"

        companion object : TopLevelDestination {
            override val route = "taskCategory/{${NavArguments.ID_KEY}}"
        }
    }

    data class NewTaskCategoryForm(private val name: String?) : TopLevelDestination {
        override val route = name?.let {
            "taskCategoryForm?${NavArguments.NAME_KEY}=$name"
        } ?: "taskCategoryForm"

        companion object : TopLevelDestination {
            override val route =
                "taskCategoryForm?${NavArguments.NAME_KEY}={${NavArguments.NAME_KEY}}"
        }
    }

    data class EditTaskCategoryForm(private val id: Long) : TopLevelDestination {
        override val route = "taskCategoryForm/$id"

        companion object : TopLevelDestination {
            override val route = "taskCategoryForm/{${NavArguments.ID_KEY}}"
        }
    }

    data object TaskSearch : TopLevelDestination {
        override val route = "taskSearch"
    }
}
