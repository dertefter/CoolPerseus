package com.dertefter.coolperseus.data.model

sealed class DeviceAction {
    object None : DeviceAction()
    object OpenFrontCamera : DeviceAction()
    object OpenCamera : DeviceAction()
    object Back : DeviceAction()
    object Minimize : DeviceAction()
    data class CustomIntent(val action: String) : DeviceAction()

    fun toStringRepresentation(): String {
        return when (this) {
            None -> "none"
            OpenFrontCamera -> "open_front_camera"
            OpenCamera -> "open_camera"
            Back -> "back"
            Minimize -> "minimize"
            is CustomIntent -> "custom_intent:$action"
        }
    }

    companion object {
        fun fromString(value: String?): DeviceAction {
            if (value == null) return None
            return when {
                value == "none" -> None
                value == "open_front_camera" -> OpenFrontCamera
                value == "open_camera" -> OpenCamera
                value == "back" -> Back
                value == "minimize" -> Minimize
                value.startsWith("custom_intent:") -> CustomIntent(value.removePrefix("custom_intent:"))
                else -> None
            }
        }
    }
}
