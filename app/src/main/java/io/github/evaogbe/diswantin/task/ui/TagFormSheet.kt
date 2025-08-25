package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    var nameInput by rememberSaveable { mutableStateOf(initialName) }

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        TagFormSheetLayout(
            isNew = initialName.isEmpty(),
            name = nameInput,
            onNameChange = { nameInput = it },
            onSave = {
                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                    onSave(nameInput)
                    onDismiss()
                }
            },
        )
    }
}

@Composable
fun TagFormSheetLayout(
    isNew: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
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
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.name_label)) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSave() }),
            singleLine = true,
        )
        Spacer(Modifier.size(SpaceMd))
        TextButton(
            onClick = onSave,
            modifier = Modifier.align(Alignment.End),
            enabled = name.isNotBlank(),
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
                name = "",
                onNameChange = {},
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
                name = "morning routine",
                onNameChange = {},
                onSave = {},
            )
        }
    }
}
