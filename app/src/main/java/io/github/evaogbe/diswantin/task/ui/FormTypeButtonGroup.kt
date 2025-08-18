package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews

@Composable
fun FormTypeButtonGroup(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        SegmentedButton(
            selected = selectedIndex == 0,
            onClick = { onSelect(0) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            icon = {
                Icon(
                    painter = painterResource(R.drawable.baseline_task_alt_24),
                    contentDescription = null,
                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                )
            },
        ) {
            Text(stringResource(R.string.form_type_button_task))
        }
        SegmentedButton(
            selected = selectedIndex == 1,
            onClick = { onSelect(1) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            icon = {
                Icon(
                    painter = painterResource(R.drawable.baseline_list_alt_24),
                    contentDescription = null,
                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                )
            },
        ) {
            Text(stringResource(R.string.form_type_button_category))
        }
    }
}

@DevicePreviews
@Composable
private fun FormTypeButtonGroupPreview() {
    DiswantinTheme {
        FormTypeButtonGroup(selectedIndex = 0, onSelect = {})
    }
}
