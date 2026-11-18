package com.dertefter.shared.slider_animations

data class EdgesGlowSettings(
    val cornerRadius: Float = DEFAULT_CORNER_RADIUS,
    val glowRadius: Float = DEFAULT_GLOW_RADIUS,
    val glowPower: Float = DEFAULT_GLOW_POWER,
    val pulseSpeed: Float = DEFAULT_PULSE_SPEED,
    val pulseAmount: Float = DEFAULT_PULSE_AMOUNT,
    val maxVerticalTop: Float = DEFAULT_MAX_VERTICAL_TOP,
    val maxVerticalBottom: Float = DEFAULT_MAX_VERTICAL_BOTTOM,
) {
    companion object {
        const val STORAGE_VERSION = 2

        const val DEFAULT_CORNER_RADIUS = 0.04f
        const val DEFAULT_GLOW_RADIUS = 0.36f
        const val DEFAULT_GLOW_POWER = 500.0f
        const val DEFAULT_PULSE_SPEED = 2.0f
        const val DEFAULT_PULSE_AMOUNT = 0.0f
        const val DEFAULT_MAX_VERTICAL_TOP = 0.85f
        const val DEFAULT_MAX_VERTICAL_BOTTOM = 1.03f
    }
}
