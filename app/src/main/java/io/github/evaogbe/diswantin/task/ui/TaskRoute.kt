package io.github.evaogbe.diswantin.task.ui

import kotlinx.serialization.Serializable

@Serializable
object CurrentTaskRoute

@Serializable
object TagListRoute

@Serializable
object DueTodayRoute

@Serializable
data class TaskDetailRoute(val id: Long)

@Serializable
object TaskFormRoute {
    @Serializable
    data class Main(val id: Long?, val name: String?) {
        companion object {
            val Start = Main(id = null, name = null)

            fun new(name: String?) = Main(id = null, name = name)

            fun edit(id: Long) = Main(id = id, name = null)
        }
    }

    @Serializable
    object Recurrence

    @Serializable
    object TaskSearch
}

@Serializable
data class TagDetailRoute(val id: Long)

@Serializable
object TaskSearchRoute
