package com.dertefter.coolperseus.action_selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ActionSelectionRoute(
    onNavigateBack: () -> Unit = {},
    viewModel: ActionSelectionViewModel = hiltViewModel(),
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ActionSelectionScreen(
        uiState = uiState,
        onEvent = { event ->
            if (event is ActionSelectionEvent.OnNavigateBack) {
                onNavigateBack()
            } else {
                viewModel.onEvent(event)
            }
        }
    )
}
