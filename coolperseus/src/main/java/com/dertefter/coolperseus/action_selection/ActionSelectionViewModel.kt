package com.dertefter.coolperseus.action_selection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dertefter.shared.data.SettingsRepository
import com.dertefter.shared.data.model.DeviceAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UiState(
    val actionSliderUp: DeviceAction = DeviceAction.None,
    val actionSliderDown: DeviceAction = DeviceAction.None,
    val actionAiButton: DeviceAction = DeviceAction.None
)

@HiltViewModel
class ActionSelectionViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<UiState> = combine(
        settingsRepository.actionSliderUp,
        settingsRepository.actionSliderDown,
        settingsRepository.actionAiButton
    ) { sliderUp, sliderDown, aiButton ->
        UiState(
            actionSliderUp = sliderUp,
            actionSliderDown = sliderDown,
            actionAiButton = aiButton
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState()
    )

    fun onEvent(actionSelectionEvent: ActionSelectionEvent) {
        when (actionSelectionEvent) {
            is ActionSelectionEvent.OnSelectActionSliderUp -> {
                viewModelScope.launch {
                    settingsRepository.setActionSliderUp(actionSelectionEvent.action)
                }
            }
            is ActionSelectionEvent.OnSelectActionSliderDown -> {
                viewModelScope.launch {
                    settingsRepository.setActionSliderDown(actionSelectionEvent.action)
                }
            }
            is ActionSelectionEvent.OnSelectActionAiButton -> {
                viewModelScope.launch {
                    settingsRepository.setActionAiButton(actionSelectionEvent.action)
                }
            }
            ActionSelectionEvent.OnNavigateBack -> {
                // Handled in Route
            }
        }
    }
}
