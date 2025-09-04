package io.github.evaogbe.diswantin.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.advice.AdviceRoute
import io.github.evaogbe.diswantin.task.ui.CurrentTaskRoute
import io.github.evaogbe.diswantin.task.ui.TagListRoute
import io.github.evaogbe.diswantin.task.ui.TaskSearchRoute
import io.github.evaogbe.diswantin.ui.navigation.BottomBarRoute

enum class BottomBarDestination(
    val route: BottomBarRoute,
    @param:StringRes val titleId: Int,
    @param:DrawableRes val iconId: Int,
    val restoreState: Boolean,
) {
    CurrentTask(
        CurrentTaskRoute,
        R.string.current_task_title,
        R.drawable.baseline_task_alt_24,
        true
    ),
    Advice(AdviceRoute, R.string.advice_title, R.drawable.psychiatry_24px, true),
    Search(
        TaskSearchRoute,
        R.string.task_search_title_bottom_bar,
        R.drawable.outline_search_24,
        false
    ),
    TagList(TagListRoute, R.string.tag_list_title, R.drawable.baseline_label_24, true)
}
