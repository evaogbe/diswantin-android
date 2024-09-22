package io.github.evaogbe.diswantin.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
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
    query: String,
    onQueryChange: (String) -> Unit,
    label: @Composable () -> Unit,
    onSearch: (String) -> Unit,
    options: ImmutableList<T>,
    formatOption: (T) -> String,
    onSelectOption: (T) -> Unit,
    autoFocus: Boolean,
    modifier: Modifier = Modifier,
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = query, selection = TextRange(query.length)))
    }
    var expanded by remember(options) { mutableStateOf(options.isNotEmpty()) }
    val focusRequester = remember { FocusRequester() }
    val currentQuery by rememberUpdatedState(query)

    LaunchedEffect(query) {
        textFieldValue = textFieldValue.copy(text = query)
    }

    LaunchedEffect(onSearch) {
        snapshotFlow { currentQuery }
            .debounce(150.milliseconds)
            .distinctUntilChanged()
            .collectLatest {
                onSearch(it)
            }
    }

    AutocompleteField(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        query = textFieldValue,
        onQueryChange = {
            textFieldValue = it
            onQueryChange(it.text)
        },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> AutocompleteField(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    label: @Composable () -> Unit,
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
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable),
            label = label,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query.text) }),
            singleLine = true,
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )

        if (options.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = formatOption(option)) },
                        onClick = {
                            onSelectOption(option)
                            onExpandedChange(false)
                        }
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
            AutocompleteField(
                expanded = true,
                onExpandedChange = {},
                query = TextFieldValue("Br"),
                onQueryChange = {},
                focusRequester = FocusRequester(),
                label = { Text("Task before") },
                onSearch = {},
                options = persistentListOf("Brush teeth", "Brush hair", "Eat breakfast"),
                formatOption = { it },
                onSelectOption = {}
            )
        }
    }
}
