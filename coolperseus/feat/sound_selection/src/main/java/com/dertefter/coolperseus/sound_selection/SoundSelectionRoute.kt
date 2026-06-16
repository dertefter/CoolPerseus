package com.dertefter.coolperseus.sound_selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SoundSelectionRoute(
    viewModel: SoundSelectionViewModel = hiltViewModel(),
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SoundSelectionScreen(
        uiState = uiState,
        onEvent = { event -> viewModel.onEvent(event) }
    )
}
