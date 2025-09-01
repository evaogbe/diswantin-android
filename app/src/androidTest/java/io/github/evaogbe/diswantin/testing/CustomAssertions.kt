package io.github.evaogbe.diswantin.testing

import assertk.Assert
import assertk.all
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import io.github.evaogbe.diswantin.ui.snackbar.SnackbarState

fun Assert<SnackbarState>.matchesSnackbar(message: String, actionLabel: String? = null) {
    all {
        prop(SnackbarState::message).isEqualTo(message)
        prop(SnackbarState::actionLabel).isEqualTo(actionLabel)
    }
}
