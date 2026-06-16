package com.dertefter.coolperseus.data

import com.dertefter.coolperseus.data.model.DeviceAction
import com.dertefter.coolperseus.data.model.SoundType
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val soundType: Flow<SoundType?>

    val customSounds: Flow<List<SoundType>>

    val actionSliderUp: Flow<DeviceAction>
    val actionSliderDown: Flow<DeviceAction>
    val actionAiButton: Flow<DeviceAction>

    suspend fun setSelectedSound(sound: SoundType)

    suspend fun addCustom(soundType: SoundType)

    suspend fun removeCustom(soundType: SoundType)

    suspend fun setActionSliderUp(action: DeviceAction)
    suspend fun setActionSliderDown(action: DeviceAction)
    suspend fun setActionAiButton(action: DeviceAction)


}