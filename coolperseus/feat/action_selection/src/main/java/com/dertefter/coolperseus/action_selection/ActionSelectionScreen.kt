package com.dertefter.coolperseus.action_selection

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dertefter.coolperseus.data.model.DeviceAction

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
            MediumFlexibleTopAppBar(
                title = {
                    Text(text = stringResource(R.string.action_selection_title))
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
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    TitleValueCard(
                        title = stringResource(R.string.action_on_slider_down),
                        value = uiState.actionSliderDown.toFriendlyString(),
                        onClick = { showActionPickerFor = "slider_down" }
                    )
                    TitleValueCard(
                        title = stringResource(R.string.action_on_slider_up),
                        value = uiState.actionSliderUp.toFriendlyString(),
                        onClick = { showActionPickerFor = "slider_up" }
                    )
                    TitleValueCard(
                        title = stringResource(R.string.action_on_ai_button),
                        value = uiState.actionAiButton.toFriendlyString(),
                        onClick = { showActionPickerFor = "ai_button" }
                    )
                }
            }

        }
    }
}



@Composable
fun DeviceAction.toFriendlyString(): String {
    val context = LocalContext.current
    return when (this) {
        DeviceAction.None -> stringResource(R.string.action_none)
        is DeviceAction.LaunchIntent -> {
            when (action) {
                "open_front_camera" -> stringResource(R.string.action_open_front_camera)
                "open_camera" -> stringResource(R.string.action_open_camera)
                Intent.ACTION_VOICE_COMMAND -> stringResource(R.string.action_assist)
                else -> if (action.isEmpty()) stringResource(R.string.action_custom_intent) else stringResource(R.string.action_intent_prefix, action)
            }
        }

        is DeviceAction.LaunchApp -> {
            val pm = context.packageManager
            val label = remember(packageName) {
                try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    packageName
                }
            }
            stringResource(R.string.action_launch_app_prefix, label)
        }
    }
}

@Preview
@Composable
fun ActionSelectionScreenPreview() {
    ActionSelectionScreen()
}

