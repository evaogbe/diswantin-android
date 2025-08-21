package io.github.evaogbe.diswantin.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.R

enum class BottomBarDestination(
    val route: String,
    @param:StringRes val titleId: Int,
    @param:DrawableRes val iconId: Int,
) {
    CurrentTask("currentTask", R.string.current_task_title, R.drawable.baseline_task_alt_24),
    Advice("advice", R.string.advice_title, R.drawable.psychiatry_24px),
    TaskCategoryList(
        "taskCategoryList",
        R.string.task_category_list_title,
        R.drawable.baseline_list_alt_24,
    ),
}
