package com.dertefter.coolperseus.home

sealed class HomeEvent {

    data class OnSelectSound(val sound: String?) : HomeEvent()

}
