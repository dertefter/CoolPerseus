package com.dertefter.shared.data.model

sealed class SoundType {
    object Jianghu : SoundType()
    object Jixie : SoundType()
    object Keji : SoundType()
    object Lingdong : SoundType()
    object Zippo : SoundType()
    data class Custom(val openSoundPath: String, val closeSoundPath: String) : SoundType()
}