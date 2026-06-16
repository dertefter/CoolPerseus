package com.dertefter.coolperseus.action_selection

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dertefter.coolperseus.data.model.DeviceAction
import com.dertefter.coolperseus.design.theme.CoolPerseusTheme

@Composable
fun ActionPickerDialog(
    onDismiss: () -> Unit,
    onActionSelected: (DeviceAction) -> Unit
) {
    val actions = listOf(
        DeviceAction.None,
        DeviceAction.LaunchIntent(Intent.ACTION_VOICE_COMMAND),
        DeviceAction.LaunchIntent("open_front_camera"),
        DeviceAction.LaunchIntent("open_camera"),
        DeviceAction.LaunchIntent(""),
        DeviceAction.LaunchApp(""),
    )

    var customIntentAction by remember { mutableStateOf("") }
    var showingCustomInput by remember { mutableStateOf(false) }
    var showingAppPicker by remember { mutableStateOf(false) }

    if (showingAppPicker) {
        AppPickerDialog(
            onDismiss = { showingAppPicker = false },
            onAppSelected = { packageName ->
                onActionSelected(DeviceAction.LaunchApp(packageName))
                showingAppPicker = false
            }
        )
    }  else if (showingCustomInput) {
        AlertDialog(
            onDismissRequest = { showingCustomInput = false },
            title = { Text(stringResource(R.string.action_picker_input_title)) },
            text = {
                OutlinedTextField(
                    value = customIntentAction,
                    onValueChange = { customIntentAction = it },
                    label = { Text(stringResource(R.string.action_picker_input_label)) }
                )
            },
            confirmButton = {
                TextButton(onClick = { onActionSelected(DeviceAction.LaunchIntent(customIntentAction)) }) {
                    Text(stringResource(R.string.action_picker_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showingCustomInput = false }) {
                    Text(stringResource(R.string.action_picker_cancel))
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.action_picker_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions.forEach { action ->
                        val text = when {
                            action is DeviceAction.LaunchApp && action.packageName.isEmpty() -> stringResource(R.string.action_launch_app)
                            else -> action.toFriendlyString()
                        }
                        Text(
                            text = text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when {
                                        action is DeviceAction.LaunchIntent && action.action.isEmpty() -> {
                                            showingCustomInput = true
                                        }

                                        action is DeviceAction.LaunchApp && action.packageName.isEmpty() -> {
                                            showingAppPicker = true
                                        }

                                        else -> {
                                            onActionSelected(action)
                                        }
                                    }
                                }
                                .padding(8.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ActionPickerDialogPreview() {
    CoolPerseusTheme {
        ActionPickerDialog(
            onDismiss = {},
            onActionSelected = {}
        )
    }
}
