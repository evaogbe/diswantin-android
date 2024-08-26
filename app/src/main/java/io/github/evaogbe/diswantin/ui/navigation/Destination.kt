package io.github.evaogbe.diswantin.ui.navigation

sealed interface Destination {
    val route: String

    data object CurrentActivity : Destination {
        override val route = "currentActivity"
    }

    data object ActivitySearch : Destination {
        override val route = "activitySearch"
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

    data class ActivityDetail(private val id: Long) : Destination {
        override val route = "activity/$id"

        companion object : Destination {
            const val ID_KEY = "id"

            override val route = "activity/{$ID_KEY}"
        }
    }
}
