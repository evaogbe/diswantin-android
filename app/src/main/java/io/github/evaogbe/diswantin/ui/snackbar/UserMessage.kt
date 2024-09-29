package io.github.evaogbe.diswantin.ui.snackbar

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

sealed interface UserMessage {
    data class String(@StringRes val resId: Int) : UserMessage

    data class Plural(@PluralsRes val resId: Int, val count: Int) : UserMessage
}
