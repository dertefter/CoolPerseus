package com.dertefter.coolperseus.action_selection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dertefter.coolperseus.data.model.DeviceAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSelectionScreen(
    uiState: UiState = UiState(),
    onEvent: (Event) -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showActionPickerFor by remember { mutableStateOf<String?>(null) }

    if (showActionPickerFor != null) {
        ActionPickerDialog(
            onDismiss = { showActionPickerFor = null },
            onActionSelected = { action ->
                when (showActionPickerFor) {
                    "slider_up" -> onEvent(Event.OnSelectActionSliderUp(action))
                    "slider_down" -> onEvent(Event.OnSelectActionSliderDown(action))
                    "ai_button" -> onEvent(Event.OnSelectActionAiButton(action))
                }
                showActionPickerFor = null
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(text = "Действия")
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(Event.OnNavigateBack) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = innerPadding
        ) {

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TitleValueCard(
                        modifier = Modifier.clickable { showActionPickerFor = "slider_down" },
                        title = "При открытии слайдера",
                        value = uiState.actionSliderDown.toFriendlyString()
                    )
                    TitleValueCard(
                        modifier = Modifier.clickable { showActionPickerFor = "slider_up" },
                        title = "При закрытии слайдера",
                        value = uiState.actionSliderUp.toFriendlyString()
                    )
                    TitleValueCard(
                        modifier = Modifier.clickable { showActionPickerFor = "ai_button" },
                        title = "При нажатии на AI-кнопку",
                        value = uiState.actionAiButton.toFriendlyString()
                    )
                }
            }

        }
    }
}

@Composable
fun ActionPickerDialog(
    onDismiss: () -> Unit,
    onActionSelected: (DeviceAction) -> Unit
) {
    val actions = listOf(
        DeviceAction.None,
        DeviceAction.LaunchIntent("open_front_camera"),
        DeviceAction.LaunchIntent("open_camera"),
        DeviceAction.LaunchIntent("")
    )

    var customIntentAction by remember { mutableStateOf("") }
    var showingCustomInput by remember { mutableStateOf(false) }

    if (showingCustomInput) {
        AlertDialog(
            onDismissRequest = { showingCustomInput = false },
            title = { Text("Введите Action") },
            text = {
                OutlinedTextField(
                    value = customIntentAction,
                    onValueChange = { customIntentAction = it },
                    label = { Text("android.intent.action...") }
                )
            },
            confirmButton = {
                TextButton(onClick = { onActionSelected(DeviceAction.LaunchIntent(customIntentAction)) }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showingCustomInput = false }) {
                    Text("Отмена")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Выберите действие") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions.forEach { action ->
                        Text(
                            text = action.toFriendlyString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (action is DeviceAction.LaunchIntent && action.action.isEmpty()) {
                                        showingCustomInput = true
                                    } else {
                                        onActionSelected(action)
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

fun DeviceAction.toFriendlyString(): String {
    return when (this) {
        DeviceAction.None -> "Ничего не делать"
        is DeviceAction.LaunchIntent -> {
            when (action) {
                "open_front_camera" -> "Открыть фронтальную камеру"
                "open_camera" -> "Открыть основную камеру"
                else -> if (action.isEmpty()) "Кастомный Intent" else "Intent: $action"
            }
        }
    }
}

@Preview
@Composable
fun ActionSelectionScreenPreview() {
    ActionSelectionScreen()
}

