package io.github.evaogbe.diswantin.ui.snackbar

class SnackbarState private constructor(
    val message: String,
    val actionLabel: String?,
    val onAction: () -> Unit,
) {
    fun withAction(label: String, onAction: () -> Unit) =
        SnackbarState(message, label, onAction)

    companion object {
        fun create(message: String) = SnackbarState(message, actionLabel = null, onAction = {})
    }
}

typealias SnackbarHandler = (SnackbarState) -> Unit
