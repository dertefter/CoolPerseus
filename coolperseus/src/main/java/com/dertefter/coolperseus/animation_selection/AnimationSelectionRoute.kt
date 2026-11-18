package com.dertefter.coolperseus.animation_selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AnimationSelectionRoute(
    onNavigateBack: () -> Unit = {},
    viewModel: AnimationSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AnimationSelectionScreen(
        uiState = uiState,
        onEvent = { event ->
            if (event is AnimationSelectionEvent.OnNavigateBack) {
                onNavigateBack()
            } else {
                viewModel.onEvent(event)
            }
        }
    )
}
