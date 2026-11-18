package com.dertefter.shared.slider_animations

data class PixelGlowSettings(
    val cornerRadius: Float = DEFAULT_CORNER_RADIUS,
    val glowRadius: Float = DEFAULT_GLOW_RADIUS,
    val glowPower: Float = DEFAULT_GLOW_POWER,
    val pulseSpeed: Float = DEFAULT_PULSE_SPEED,
    val pulseAmount: Float = DEFAULT_PULSE_AMOUNT,
    val verticalTop: Float = DEFAULT_VERTICAL_TOP,
    val verticalBottom: Float = DEFAULT_VERTICAL_BOTTOM,
    val glowStrength: Float = DEFAULT_GLOW_STRENGTH,
    val dotCell: Float = DEFAULT_DOT_CELL,
    val dotSize: Float = DEFAULT_DOT_SIZE,
    val dotDensity: Float = DEFAULT_DOT_DENSITY,
    val dotFlicker: Float = DEFAULT_DOT_FLICKER,
) {
    companion object {

        const val STORAGE_VERSION = 1

        const val DEFAULT_CORNER_RADIUS = 0.04f
        const val DEFAULT_GLOW_RADIUS = 2.0f
        const val DEFAULT_GLOW_POWER = 500.0f
        const val DEFAULT_PULSE_SPEED = 2.0f
        const val DEFAULT_PULSE_AMOUNT = 0.2f
        const val DEFAULT_VERTICAL_TOP = 0.2f
        const val DEFAULT_VERTICAL_BOTTOM = 1.4f
        const val DEFAULT_GLOW_STRENGTH = 0.8f
        const val DEFAULT_DOT_CELL = 0.002f
        const val DEFAULT_DOT_SIZE = 0.25f
        const val DEFAULT_DOT_DENSITY = 0.15f
        const val DEFAULT_DOT_FLICKER = 5.5f
    }
}
