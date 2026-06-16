package com.dertefter.coolperseus.sound_selection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dertefter.coolperseus.data.model.SoundType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.dertefter.coolperseus.data.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class SoundSelectionViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val availableSounds = listOf(
        "keji",
        "jixie",
        "zippo",
        "jianghu",
        "lingdong"
    )

    val uiState: StateFlow<SoundSelectionUiState> = combine(
        settingsRepository.soundType,
        MutableStateFlow(availableSounds)
    ) { soundType, sounds ->
        SoundSelectionUiState(
            selectedSound = when (soundType) {
                is SoundType.Keji -> "keji"
                is SoundType.Jixie -> "jixie"
                is SoundType.Zippo -> "zippo"
                is SoundType.Jianghu -> "jianghu"
                is SoundType.Lingdong -> "lingdong"
                else -> ""
            },
            sounds = sounds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SoundSelectionUiState("", emptyList())
    )

    fun onEvent(event: Event) {
        when (event) {
            is Event.OnSelectSound -> {
                viewModelScope.launch {
                    val soundType = when (event.sound) {
                        "keji" -> SoundType.Keji
                        "jixie" -> SoundType.Jixie
                        "zippo" -> SoundType.Zippo
                        "jianghu" -> SoundType.Jianghu
                        "lingdong" -> SoundType.Lingdong
                        else -> null
                    }
                    soundType?.let {
                        settingsRepository.setSelectedSound(it)
                    }
                }
            }
            Event.OnNavigateBack -> {
                // Handled by the activity/navigator
            }
        }
    }
}
