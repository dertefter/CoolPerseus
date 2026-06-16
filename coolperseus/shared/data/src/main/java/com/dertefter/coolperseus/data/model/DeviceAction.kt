package com.dertefter.coolperseus.data.model

sealed class DeviceAction {
    object None : DeviceAction()
    data class LaunchIntent(val action: String) : DeviceAction()

    fun toStringRepresentation(): String {
        return when (this) {
            None -> "none"
            is LaunchIntent -> action
        }
    }

    companion object {
        fun fromString(value: String?): DeviceAction {
            if (value == null || value == "none") return None
            return when {
                value.startsWith("custom_intent:") -> LaunchIntent(value.removePrefix("custom_intent:"))
                else -> LaunchIntent(value)
            }
        }
    }
}
