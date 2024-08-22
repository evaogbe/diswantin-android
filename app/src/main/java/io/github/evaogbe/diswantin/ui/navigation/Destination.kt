package io.github.evaogbe.diswantin.ui.navigation

sealed interface Destination {
    val route: String

    data object CurrentActivity : Destination {
        override val route = "currentActivity"
    }

    data object SearchResults : Destination {
        override val route = "searchResults"
    }

    data object NewActivityForm : Destination {
        override val route = "activityForm"
    }

    data class EditActivityForm(private val id: Long) : Destination {
        override val route = "activityForm/$id"

        companion object : Destination {
            const val ID_KEY = "id"

            override val route = "activityForm/{$ID_KEY}"
        }
    }

    data object Advice : Destination {
        override val route = "advice"
    }
}
