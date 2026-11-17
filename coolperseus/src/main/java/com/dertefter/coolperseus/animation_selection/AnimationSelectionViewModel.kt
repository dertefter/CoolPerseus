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
        null
    )

    val uiState: StateFlow<AnimationSelectionUiState> = combine(
        settingsRepository.selectedAnimation,
        MutableStateFlow(availableAnimations)
    ) { selectedAnimation, animations ->
        AnimationSelectionUiState(
            selectedAnimation = selectedAnimation,
            animations = animations
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
            AnimationSelectionEvent.OnNavigateBack -> {
                // Handled by navigator
            }
        }
    }
}
