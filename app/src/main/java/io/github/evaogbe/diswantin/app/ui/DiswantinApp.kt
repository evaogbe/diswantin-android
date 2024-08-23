package io.github.evaogbe.diswantin.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.evaogbe.diswantin.activity.ui.ActivityDetailScreen
import io.github.evaogbe.diswantin.activity.ui.ActivityFormScreen
import io.github.evaogbe.diswantin.activity.ui.ActivitySearchScreen
import io.github.evaogbe.diswantin.activity.ui.AdviceScreen
import io.github.evaogbe.diswantin.activity.ui.CurrentActivityScreen
import io.github.evaogbe.diswantin.ui.navigation.Destination

@Composable
fun DiswantinApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destination.CurrentActivity.route,
    ) {
        composable(Destination.CurrentActivity.route) {
            CurrentActivityScreen(
                onNavigateToSearch = {
                    navController.navigate(route = Destination.SearchResults.route)
                },
                onAddActivity = {
                    navController.navigate(route = Destination.NewActivityForm.route)
                },
                onEditActivity = {
                    navController.navigate(route = Destination.EditActivityForm(it).route)
                },
                onAdviceClick = {
                    navController.navigate(route = Destination.Advice.route)
                },
            )
        }
        composable(Destination.SearchResults.route) {
            ActivitySearchScreen(
                onBackClick = navController::popBackStack,
                onSelectSearchResult = {
                    navController.navigate(route = Destination.ActivityDetail(it).route)
                })
        }
        composable(Destination.NewActivityForm.route) {
            ActivityFormScreen(onPopBackStack = navController::popBackStack)
        }
        composable(
            Destination.EditActivityForm.route,
            arguments = listOf(navArgument(Destination.EditActivityForm.ID_KEY) {
                type = NavType.LongType
            })
        ) {
            ActivityFormScreen(onPopBackStack = navController::popBackStack)
        }
        composable(Destination.Advice.route) {
            AdviceScreen(onClose = navController::popBackStack)
        }
        composable(
            Destination.ActivityDetail.route,
            arguments = listOf(navArgument(Destination.ActivityDetail.ID_KEY) {
                type = NavType.LongType
            })
        ) {
            ActivityDetailScreen(
                onPopBackStack = navController::popBackStack,
                onEditActivity = {
                    navController.navigate(route = Destination.EditActivityForm(it).route)
                }
            )
        }
    }
}
