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
                navigateToActivitySearch = {
                    navController.navigate(route = Destination.SearchResults.route)
                },
                navigateToNewActivityForm = {
                    navController.navigate(route = Destination.NewActivityForm.route)
                },
                navigateToEditActivityForm = {
                    navController.navigate(route = Destination.EditActivityForm(it).route)
                },
                navigateToAdvice = {
                    navController.navigate(route = Destination.Advice.route)
                },
            )
        }
        composable(Destination.SearchResults.route) {
            ActivitySearchScreen(
                popBackStack = navController::popBackStack,
                navigateToActivityDetail = {
                    navController.navigate(route = Destination.ActivityDetail(it).route)
                })
        }
        composable(Destination.NewActivityForm.route) {
            ActivityFormScreen(popBackStack = navController::popBackStack)
        }
        composable(
            Destination.EditActivityForm.route,
            arguments = listOf(navArgument(Destination.EditActivityForm.ID_KEY) {
                type = NavType.LongType
            })
        ) {
            ActivityFormScreen(popBackStack = navController::popBackStack)
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
                popBackStack = navController::popBackStack,
                navigateToEditActivityForm = {
                    navController.navigate(route = Destination.EditActivityForm(it).route)
                }
            )
        }
    }
}
