package com.dertefter.coolperseus.home

import android.net.Uri
import com.dertefter.shared.data.model.SoundType

sealed class HomeEvent {

    data class OnSelectSound(val sound: SoundType?) : HomeEvent()

    data class OnImportSoundPack(val uri: Uri) : HomeEvent()

    data class OnDeleteSoundPack(val sound: SoundType) : HomeEvent()

    object OnDismissError : HomeEvent()
}
