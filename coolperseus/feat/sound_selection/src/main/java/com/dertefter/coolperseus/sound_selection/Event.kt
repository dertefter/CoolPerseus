package com.dertefter.coolperseus.sound_selection

sealed class Event {

    object OnNavigateBack : Event()

    data class OnSelectSound(val sound: String?) : Event()

}
