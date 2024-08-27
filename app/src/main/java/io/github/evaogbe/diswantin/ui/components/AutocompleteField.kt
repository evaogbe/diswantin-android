package io.github.evaogbe.diswantin.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@Composable
fun <T : Any> AutocompleteField(
    label: @Composable () -> Unit,
    onSearch: (String) -> Unit,
    selectedOption: T?,
    options: List<T>,
    formatOption: (T) -> String,
    onSelectOption: (T?) -> Unit,
) {
    var expanded by remember(options) { mutableStateOf(options.isNotEmpty()) }
    var query by remember(selectedOption) {
        mutableStateOf(selectedOption?.let(formatOption) ?: "")
    }
    val currentQuery by rememberUpdatedState(query)

    LaunchedEffect(onSearch) {
        snapshotFlow { currentQuery }
            .debounce(150.milliseconds)
            .distinctUntilChanged()
            .collectLatest { query ->
                onSearch(query)
            }
    }

    AutocompleteField(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        query = query,
        onQueryChange = { query = it },
        onBlur = {
            query = selectedOption?.let(formatOption) ?: ""
            expanded = false
        },
        label = label,
        onSearch = onSearch,
        options = options,
        formatOption = formatOption,
        onSelectOption = onSelectOption,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> AutocompleteField(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    onBlur: () -> Unit,
    label: @Composable () -> Unit,
    onSearch: (String) -> Unit,
    options: List<T>,
    formatOption: (T) -> String,
    onSelectOption: (T?) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                if (!it || options.isNotEmpty()) {
                    onExpandedChange(it)
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    onQueryChange(it)
                    if (options.isNotEmpty()) {
                        onExpandedChange(true)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (!it.hasFocus) {
                            onBlur()
                        }
                    }
                    .menuAnchor(),
                label = label,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                singleLine = true,
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
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

        if (query.isNotBlank()) {
            Spacer(Modifier.size(SpaceSm))
            IconButton(
                onClick = { onSelectOption(null) },
                modifier = Modifier.padding(top = 4.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = colorScheme.surfaceVariant,
                    contentColor = colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(R.string.clear_button)
                )
            }
        }
    }
}

@DevicePreviews
@Composable
private fun AutocompleteFieldPreview() {
    DiswantinTheme {
        Surface {
            AutocompleteField(
                expanded = true,
                onExpandedChange = {},
                query = "Br",
                onQueryChange = {},
                onBlur = {},
                label = { Text("Task before") },
                onSearch = {},
                options = listOf("Brush teeth", "Brush hair", "Eat breakfast"),
                formatOption = { it },
                onSelectOption = {}
            )
        }
    }
}
