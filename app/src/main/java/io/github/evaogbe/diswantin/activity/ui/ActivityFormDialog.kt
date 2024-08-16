package io.github.evaogbe.diswantin.activity.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.components.AutoFocusTextField
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews

@Composable
fun ActivityFormDialog(
    dismiss: () -> Unit,
    saveActivity: () -> Unit,
    title: String,
    name: String,
    onNameChange: (String) -> Unit,
    formError: String?,
) {
    AlertDialog(
        onDismissRequest = dismiss,
        confirmButton = {
            TextButton(onClick = saveActivity, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = dismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        },
        title = { Text(text = title) },
        text = {
            ActivityFormDialogContent(
                name = name,
                onNameChange = onNameChange,
                saveActivity = saveActivity,
                formError = formError
            )
        })
}

@Composable
fun ActivityFormDialogContent(
    name: String,
    onNameChange: (String) -> Unit,
    saveActivity: () -> Unit,
    formError: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (formError != null) {
            SelectionContainer {
                Text(text = formError, color = colorScheme.error, style = typography.titleMedium)
            }
            Spacer(Modifier.size(SpaceMd))
        }

        AutoFocusTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.name_label)) },
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                if (name.isNotBlank()) {
                    saveActivity()
                }
            }),
            singleLine = true
        )
    }
}

@DevicePreviews
@Composable
fun ActivityFormDialogContentPreview_WithError() {
    DiswantinTheme {
        Surface {
            ActivityFormDialogContent(
                name = "",
                onNameChange = {},
                saveActivity = {},
                formError = stringResource(R.string.activity_form_save_error_new),
                modifier = Modifier.padding(SpaceMd)
            )
        }
    }
}
