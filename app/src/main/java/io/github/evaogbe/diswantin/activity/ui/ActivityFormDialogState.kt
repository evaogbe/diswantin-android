package io.github.evaogbe.diswantin.activity.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Stable
class ActivityFormDialogState(name: String, mode: Mode) {
    var mode by mutableStateOf(mode)
        private set

    var nameInput by mutableStateOf(name)
        private set

    fun openDialog(name: String? = null) {
        nameInput = name ?: ""
        mode = if (name == null) Mode.OpenForNew else Mode.OpenForEdit
    }

    fun closeDialog() {
        mode = Mode.Closed
    }

    fun updateNameInput(value: String) {
        nameInput = value
    }

    companion object {
        val Saver = run {
            val nameKey = "Name"
            val modeKey = "Mode"
            mapSaver(
                save = { mapOf(nameKey to it.nameInput, modeKey to it.mode) },
                restore = { ActivityFormDialogState(it[nameKey] as String, it[modeKey] as Mode) }
            )
        }
    }

    enum class Mode {
        Closed, OpenForNew, OpenForEdit
    }
}

@Composable
fun rememberActivityFormDialogState() =
    rememberSaveable(saver = ActivityFormDialogState.Saver) {
        ActivityFormDialogState("", ActivityFormDialogState.Mode.Closed)
    }
