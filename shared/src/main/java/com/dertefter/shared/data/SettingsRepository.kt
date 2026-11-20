package com.dertefter.shared.data

import com.dertefter.shared.data.model.DeviceAction
import com.dertefter.shared.data.model.SoundType
import com.dertefter.shared.slider_animations.EdgesGlowSettings
import com.dertefter.shared.slider_animations.PixelGlowSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val soundType: Flow<SoundType?>

    val customSounds: Flow<List<SoundType>>

    val actionSliderUp: Flow<DeviceAction>
    val actionSliderDown: Flow<DeviceAction>
    val actionAiButton: Flow<DeviceAction>

    val selectedAnimation: Flow<String?>

    val edgesGlowSettings: Flow<EdgesGlowSettings>

    val pixelGlowSettings: Flow<PixelGlowSettings>

    fun getSelectedAnimationSync(): String?

    fun getEdgesGlowSettingsSync(): EdgesGlowSettings

    fun getPixelGlowSettingsSync(): PixelGlowSettings

    suspend fun setSelectedSound(sound: SoundType?)

    suspend fun addCustom(soundType: SoundType)

    suspend fun removeCustom(soundType: SoundType)

    suspend fun replaceCustomSounds(sounds: List<SoundType>)

    suspend fun setActionSliderUp(action: DeviceAction)
    suspend fun setActionSliderDown(action: DeviceAction)
    suspend fun setActionAiButton(action: DeviceAction)

    suspend fun setSelectedAnimation(animation: String?)

    suspend fun setEdgesGlowSettings(settings: EdgesGlowSettings)

    suspend fun resetEdgesGlowSettings()

    suspend fun setPixelGlowSettings(settings: PixelGlowSettings)

    suspend fun resetPixelGlowSettings()

}
