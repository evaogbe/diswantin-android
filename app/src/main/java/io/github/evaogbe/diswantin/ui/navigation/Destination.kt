package io.github.evaogbe.diswantin.ui.navigation

sealed interface Destination {
    val route: String

    data object CurrentActivity : Destination {
        override val route = "currentActivity"
    }

    data object SearchResults : Destination {
        override val route = "searchResults"
    }

    data class ActivityDetail(private val id: Long) : Destination {
        override val route = "activity/$id"

        companion object : Destination {
            const val ID_KEY = "id"

            override val route = "activity/{$ID_KEY}"
        }
    }
}
