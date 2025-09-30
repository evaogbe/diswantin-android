package io.github.evaogbe.diswantin.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.advice.AdviceRoute
import io.github.evaogbe.diswantin.task.ui.CurrentTaskRoute
import io.github.evaogbe.diswantin.task.ui.TagListRoute

enum class BottomBarDestination(
    val route: Any,
    @param:StringRes val titleId: Int,
    @param:DrawableRes val iconId: Int,
) {
    CurrentTask(CurrentTaskRoute, R.string.current_task_title, R.drawable.baseline_task_alt_24),
    Advice(AdviceRoute, R.string.advice_title, R.drawable.psychiatry_24px),
    TagList(TagListRoute, R.string.tag_list_title, R.drawable.baseline_label_24),
}
