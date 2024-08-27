package io.github.evaogbe.diswantin.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.evaogbe.diswantin.task.ui.AdviceScreen
import io.github.evaogbe.diswantin.task.ui.CurrentTaskScreen
import io.github.evaogbe.diswantin.task.ui.TaskDetailScreen
import io.github.evaogbe.diswantin.task.ui.TaskFormScreen
import io.github.evaogbe.diswantin.task.ui.TaskSearchScreen
import io.github.evaogbe.diswantin.ui.navigation.Destination

@Composable
fun DiswantinApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destination.CurrentTask.route,
    ) {
        composable(Destination.CurrentTask.route) {
            CurrentTaskScreen(
                onNavigateToSearch = {
                    navController.navigate(route = Destination.TaskSearch.route)
                },
                onAddTask = {
                    navController.navigate(route = Destination.NewTaskForm.route)
                },
                onEditTask = {
                    navController.navigate(route = Destination.EditTaskForm(it).route)
                },
                onAdviceClick = {
                    navController.navigate(route = Destination.Advice.route)
                },
            )
        }
        composable(Destination.TaskSearch.route) {
            TaskSearchScreen(
                onBackClick = navController::popBackStack,
                onSelectSearchResult = {
                    navController.navigate(route = Destination.TaskDetail(it).route)
                })
        }
        composable(Destination.NewTaskForm.route) {
            TaskFormScreen(onPopBackStack = navController::popBackStack)
        }
        composable(
            Destination.EditTaskForm.route,
            arguments = listOf(navArgument(Destination.EditTaskForm.ID_KEY) {
                type = NavType.LongType
            })
        ) {
            TaskFormScreen(onPopBackStack = navController::popBackStack)
        }
        composable(Destination.Advice.route) {
            AdviceScreen(onClose = navController::popBackStack)
        }
        composable(
            Destination.TaskDetail.route,
            arguments = listOf(navArgument(Destination.TaskDetail.ID_KEY) {
                type = NavType.LongType
            })
        ) { backStackEntry ->
            TaskDetailScreen(
                onPopBackStack = navController::popBackStack,
                onEditTask = {
                    navController.navigate(route = Destination.EditTaskForm(it).route)
                },
                onSelectChainItem = { id ->
                    if (
                        backStackEntry.arguments?.getLong(Destination.TaskDetail.ID_KEY) != id
                    ) {
                        navController.navigate(route = Destination.TaskDetail(id).route)
                    }
                }
            )
        }
    }
}
