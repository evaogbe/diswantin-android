package io.github.evaogbe.diswantin.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.evaogbe.diswantin.task.ui.AdviceScreen
import io.github.evaogbe.diswantin.task.ui.HomeScreen
import io.github.evaogbe.diswantin.task.ui.TaskDetailScreen
import io.github.evaogbe.diswantin.task.ui.TaskFormScreen
import io.github.evaogbe.diswantin.task.ui.TaskListDetailScreen
import io.github.evaogbe.diswantin.task.ui.TaskListFormScreen
import io.github.evaogbe.diswantin.task.ui.TaskSearchScreen
import io.github.evaogbe.diswantin.ui.navigation.Destination

@Composable
fun DiswantinApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destination.Home.route,
    ) {
        composable(Destination.Home.route) {
            HomeScreen(
                onSearch = { navController.navigate(route = Destination.TaskSearch.route) },
                onEditTask = { navController.navigate(route = Destination.EditTaskForm(it).route) },
                onAddTask = {
                    navController.navigate(route = Destination.NewTaskForm(name = null).route)
                },
                onAddList = { navController.navigate(route = Destination.NewTaskListForm.route) },
                onAdviceClick = { navController.navigate(route = Destination.Advice.route) },
                onSelectTaskList = {
                    navController.navigate(route = Destination.TaskListDetail(it).route)
                },
            )
        }
        composable(Destination.TaskSearch.route) {
            TaskSearchScreen(
                onBackClick = navController::popBackStack,
                onAddTask = {
                    navController.navigate(route = Destination.NewTaskForm(name = it).route)
                },
                onSelectSearchResult = {
                    navController.navigate(route = Destination.TaskDetail(it).route)
                },
            )
        }
        composable(
            Destination.NewTaskForm.route,
            arguments = listOf(navArgument(Destination.NewTaskForm.NAME_KEY) {
                type = NavType.StringType
                nullable = true
            }),
        ) {
            TaskFormScreen(onPopBackStack = navController::popBackStack)
        }
        composable(
            Destination.EditTaskForm.route,
            arguments = listOf(navArgument(Destination.EditTaskForm.ID_KEY) {
                type = NavType.LongType
            }),
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
            }),
        ) {
            TaskDetailScreen(
                onPopBackStack = navController::popBackStack,
                onEditTask = { navController.navigate(route = Destination.EditTaskForm(it).route) },
                onSelectTaskList = {
                    navController.navigate(route = Destination.TaskListDetail(it).route)
                },
            )
        }
        composable(Destination.NewTaskListForm.route) {
            TaskListFormScreen(onPopBackStack = navController::popBackStack)
        }
        composable(
            Destination.EditTaskListForm.route,
            arguments = listOf(navArgument(Destination.EditTaskListForm.ID_KEY) {
                type = NavType.LongType
            }),
        ) {
            TaskListFormScreen(onPopBackStack = navController::popBackStack)
        }
        composable(
            Destination.TaskListDetail.route,
            arguments = listOf(navArgument(Destination.TaskListDetail.ID_KEY) {
                type = NavType.LongType
            }),
        ) {
            TaskListDetailScreen(
                onPopBackStack = navController::popBackStack,
                onEditTaskList = {
                    navController.navigate(route = Destination.EditTaskListForm(it).route)
                },
                onSelectTask = { navController.navigate(route = Destination.TaskDetail(it).route) },
            )
        }
    }
}
