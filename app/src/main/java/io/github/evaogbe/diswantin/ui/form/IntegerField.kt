package io.github.evaogbe.diswantin.ui.form

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.text.isDigitsOnly
import kotlinx.coroutines.flow.collectLatest

@Composable
fun OutlinedIntegerField(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val state = rememberTextFieldState(initialText = if (value == 0) "" else value.toString())

    LaunchedEffect(value) {
        if (value != 0 && state.text.toString().toIntOrNull() != value) {
            state.setTextAndPlaceCursorAtEnd(value.toString())
        }
    }

    LaunchedEffect(state, onValueChange) {
        snapshotFlow { state.text.toString() }.collectLatest {
            onValueChange(it.toIntOrNull() ?: 0)
        }
    }

    OutlinedTextField(
        state = state,
        modifier = modifier,
        inputTransformation = InputTransformation {
            if (!asCharSequence().isDigitsOnly()) {
                revertAllChanges()
            }
        },
        keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Number),
        lineLimits = TextFieldLineLimits.SingleLine,
    )
}
