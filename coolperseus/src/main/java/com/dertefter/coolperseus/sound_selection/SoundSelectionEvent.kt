package com.dertefter.coolperseus.sound_selection

sealed class SoundSelectionEvent {

    object OnNavigateBack : SoundSelectionEvent()

    data class OnSelectSound(val sound: String?) : SoundSelectionEvent()

}
