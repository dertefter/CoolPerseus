package com.dertefter.coolperseus.animation_selection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dertefter.shared.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnimationSelectionViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val availableAnimations = listOf(
        "edges_glow",
        "pixel_glow",
        null
    )

    val uiState: StateFlow<AnimationSelectionUiState> = combine(
        settingsRepository.selectedAnimation,
        settingsRepository.edgesGlowSettings,
        settingsRepository.pixelGlowSettings,
        MutableStateFlow(availableAnimations)
    ) { selectedAnimation, edgesGlowSettings, pixelGlowSettings, animations ->
        AnimationSelectionUiState(
            selectedAnimation = selectedAnimation,
            animations = animations,
            edgesGlowSettings = edgesGlowSettings,
            pixelGlowSettings = pixelGlowSettings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnimationSelectionUiState(null, emptyList())
    )

    fun onEvent(event: AnimationSelectionEvent) {
        when (event) {
            is AnimationSelectionEvent.OnSelectAnimation -> {
                viewModelScope.launch {
                    settingsRepository.setSelectedAnimation(event.animation)
                }
            }
            is AnimationSelectionEvent.OnUpdateEdgesGlowSettings -> {
                viewModelScope.launch {
                    settingsRepository.setEdgesGlowSettings(event.settings)
                }
            }
            AnimationSelectionEvent.OnResetEdgesGlowSettings -> {
                viewModelScope.launch {
                    settingsRepository.resetEdgesGlowSettings()
                }
            }
            is AnimationSelectionEvent.OnUpdatePixelGlowSettings -> {
                viewModelScope.launch {
                    settingsRepository.setPixelGlowSettings(event.settings)
                }
            }
            AnimationSelectionEvent.OnResetPixelGlowSettings -> {
                viewModelScope.launch {
                    settingsRepository.resetPixelGlowSettings()
                }
            }
            AnimationSelectionEvent.OnNavigateBack -> {
                // Handled by navigator
            }
        }
    }
}
