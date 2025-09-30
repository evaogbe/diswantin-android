package io.github.evaogbe.diswantin.ui.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@Composable
fun <T : Any> AutocompleteField(
    query: TextFieldState,
    label: @Composable TextFieldLabelScope.() -> Unit,
    onSearch: (String) -> Unit,
    options: ImmutableList<T>,
    formatOption: (T) -> String,
    onSelectOption: (T) -> Unit,
    autoFocus: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(options) { mutableStateOf(options.isNotEmpty()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(query, onSearch) {
        snapshotFlow { query.text.toString() }.debounce(150.milliseconds).distinctUntilChanged()
            .collectLatest {
                onSearch(it)
            }
    }

    AutocompleteField(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        query = query,
        focusRequester = focusRequester,
        label = label,
        onSearch = onSearch,
        options = options,
        formatOption = formatOption,
        onSelectOption = onSelectOption,
        modifier = modifier,
    )

    if (autoFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

const val AutocompleteMenuItemTestTag = "AutocompleteMenuItemTestTag"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> AutocompleteField(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    query: TextFieldState,
    focusRequester: FocusRequester,
    label: @Composable TextFieldLabelScope.() -> Unit,
    onSearch: (String) -> Unit,
    options: ImmutableList<T>,
    formatOption: (T) -> String,
    onSelectOption: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
    ) {
        OutlinedTextField(
            state = query,
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            label = label,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            onKeyboardAction = { performDefaultAction ->
                onSearch(query.text.toString())
                performDefaultAction()
            },
            lineLimits = TextFieldLineLimits.SingleLine,
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )

        if (options.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = formatOption(option)) },
                        onClick = {
                            onSelectOption(option)
                            onExpandedChange(false)
                        },
                        modifier = Modifier.testTag(AutocompleteMenuItemTestTag),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun AutocompleteFieldPreview() {
    DiswantinTheme {
        Surface {
            val focusRequester = remember { FocusRequester() }

            AutocompleteField(
                expanded = true,
                onExpandedChange = {},
                query = TextFieldState(initialText = "Br"),
                focusRequester = focusRequester,
                label = { Text(text = "Task before") },
                onSearch = {},
                options = persistentListOf("Brush teeth", "Brush hair", "Eat breakfast"),
                formatOption = { it },
                onSelectOption = {},
            )
        }
    }
}
