package com.dertefter.coolperseus.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeRoute(
    navigateToActions: () -> Unit = {},
    navigateToAnimations: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onEvent = { event -> viewModel.onEvent(event) },
        navigateToActions = navigateToActions,
        navigateToAnimations = navigateToAnimations,
    )
}
