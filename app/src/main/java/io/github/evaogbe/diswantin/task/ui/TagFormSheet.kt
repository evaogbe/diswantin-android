package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagFormSheet(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    val nameInput = rememberTextFieldState(initialText = initialName)

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        TagFormSheetLayout(
            isNew = initialName.isEmpty(),
            name = nameInput,
            onSave = {
                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                    onSave(nameInput.text.toString())
                    onDismiss()
                }
            },
        )
    }
}

@Composable
fun TagFormSheetLayout(
    isNew: Boolean,
    name: TextFieldState,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(SpaceMd),
    ) {
        Text(
            text = if (isNew) {
                stringResource(R.string.tag_form_title_new)
            } else {
                stringResource(R.string.tag_form_title_edit)
            },
            style = typography.titleLarge,
        )
        Spacer(Modifier.size(SpaceMd))
        OutlinedTextField(
            state = name,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.name_label)) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            onKeyboardAction = { performDefaultAction ->
                onSave()
                performDefaultAction()
            },
            lineLimits = TextFieldLineLimits.SingleLine,
        )
        Spacer(Modifier.size(SpaceMd))
        TextButton(
            onClick = onSave,
            modifier = Modifier.align(Alignment.End),
            enabled = name.text.isNotBlank(),
        ) {
            Text(stringResource(R.string.save_button))
        }
    }
}

@DevicePreviews
@Composable
private fun TagFormSheetLayoutPreview_New() {
    DiswantinTheme {
        Surface {
            TagFormSheetLayout(
                isNew = true,
                name = TextFieldState(),
                onSave = {},
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TagFormSheetLayoutPreview_Edit() {
    DiswantinTheme {
        Surface {
            TagFormSheetLayout(
                isNew = false,
                name = TextFieldState(initialText = "morning routine"),
                onSave = {},
            )
        }
    }
}
