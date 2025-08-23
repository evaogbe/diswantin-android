package io.github.evaogbe.diswantin.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.evaogbe.diswantin.R

@Composable
fun DiscardConfirmationDialog(confirm: () -> Unit, dismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        confirmButton = {
            TextButton(onClick = confirm) {
                Text(stringResource(R.string.discard_confirmation_dialog_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = dismiss) {
                Text(stringResource(R.string.discard_confirmation_dialog_dismiss_button))
            }
        },
        title = { Text(stringResource(R.string.discard_confirmation_dialog_title)) },
        text = { Text(stringResource(R.string.discard_confirmation_dialog_text)) },
    )
}
