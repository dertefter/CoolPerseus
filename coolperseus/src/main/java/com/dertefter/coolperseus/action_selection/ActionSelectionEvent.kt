package com.dertefter.coolperseus.action_selection

import com.dertefter.shared.data.model.DeviceAction

sealed class ActionSelectionEvent {
    object OnNavigateBack : ActionSelectionEvent()
    data class OnSelectActionSliderUp(val action: DeviceAction) : ActionSelectionEvent()
    data class OnSelectActionSliderDown(val action: DeviceAction) : ActionSelectionEvent()
    data class OnSelectActionAiButton(val action: DeviceAction) : ActionSelectionEvent()
}
