package com.dertefter.coolperseus.action_selection

import com.dertefter.coolperseus.data.model.DeviceAction

sealed class Event {
    object OnNavigateBack : Event()
    data class OnSelectActionSliderUp(val action: DeviceAction) : Event()
    data class OnSelectActionSliderDown(val action: DeviceAction) : Event()
    data class OnSelectActionAiButton(val action: DeviceAction) : Event()
}
