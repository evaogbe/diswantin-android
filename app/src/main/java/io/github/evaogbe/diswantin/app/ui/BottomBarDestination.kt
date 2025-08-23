package io.github.evaogbe.diswantin.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.ui.AdviceRoute
import io.github.evaogbe.diswantin.task.ui.CurrentTaskRoute
import io.github.evaogbe.diswantin.task.ui.TaskCategoryListRoute
import io.github.evaogbe.diswantin.ui.navigation.BottomBarRoute

enum class BottomBarDestination(
    val route: BottomBarRoute,
    @param:StringRes val titleId: Int,
    @param:DrawableRes val iconId: Int,
) {
    CurrentTask(CurrentTaskRoute, R.string.current_task_title, R.drawable.baseline_task_alt_24),
    Advice(AdviceRoute, R.string.advice_title, R.drawable.psychiatry_24px),
    TaskCategoryList(
        TaskCategoryListRoute,
        R.string.task_category_list_title,
        R.drawable.baseline_list_alt_24,
    );

    companion object {
        val routeNames = entries.map { it.route::class.qualifiedName }
    }
}
