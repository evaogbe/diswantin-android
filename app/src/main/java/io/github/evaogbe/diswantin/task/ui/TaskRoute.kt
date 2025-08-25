package io.github.evaogbe.diswantin.task.ui

import io.github.evaogbe.diswantin.ui.navigation.BottomBarRoute
import kotlinx.serialization.Serializable

@Serializable
object AdviceRoute : BottomBarRoute

@Serializable
object CurrentTaskRoute : BottomBarRoute

@Serializable
object TagListRoute : BottomBarRoute

@Serializable
data class TaskDetailRoute(val id: Long)

@Serializable
data object TaskFormRoute {
    @Serializable
    data class Main(val id: Long? = null, val name: String? = null) {
        companion object {
            fun new(name: String?) = Main(name = name)

            fun edit(id: Long) = Main(id = id)
        }
    }

    @Serializable
    data object Recurrence
}

@Serializable
data class TagDetailRoute(val id: Long)

@Serializable
object TaskSearchRoute
