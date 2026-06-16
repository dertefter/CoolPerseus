package com.dertefter.coolperseus.data.model

sealed class DeviceAction {
    object None : DeviceAction()
    data class LaunchIntent(val action: String) : DeviceAction()
    data class LaunchApp(val packageName: String) : DeviceAction()

    fun toStringRepresentation(): String {
        return when (this) {
            None -> "none"
            is LaunchIntent -> "intent:$action"
            is LaunchApp -> "app:$packageName"
        }
    }

    companion object {
        fun fromString(value: String?): DeviceAction {
            if (value == null || value == "none") return None
            return when {
                value.startsWith("intent:") -> LaunchIntent(value.removePrefix("intent:"))
                value.startsWith("app:") -> LaunchApp(value.removePrefix("app:"))
                else -> LaunchIntent(value)
            }
        }
    }
}
