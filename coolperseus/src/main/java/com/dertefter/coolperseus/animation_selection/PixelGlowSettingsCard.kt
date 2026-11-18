package com.dertefter.coolperseus.animation_selection

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.dertefter.coolperseus.CoolPerseusTheme
import com.dertefter.coolperseus.R
import com.dertefter.shared.slider_animations.PixelGlowSettings
import java.util.Locale

@Composable
fun PixelGlowSettingsCard(
    settings: PixelGlowSettings,
    onSettingsChange: (PixelGlowSettings) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimationSettingsCard(
        title = stringResource(R.string.pixel_glow),
        onReset = onReset,
        modifier = modifier
    ) {
        ShaderSettingSlider(
            title = stringResource(R.string.shader_corner_radius),
            value = settings.cornerRadius,
            valueRange = 0.0f..0.3f,
            onValueChange = { onSettingsChange(settings.copy(cornerRadius = it)) }
        )

        ShaderSettingSlider(
            title = stringResource(R.string.shader_glow_radius),
            value = settings.glowRadius,
            valueRange = 0.05f..5.0f,
            onValueChange = { onSettingsChange(settings.copy(glowRadius = it)) }
        )

        ShaderSettingSlider(
            title = stringResource(R.string.shader_glow_power),
            value = settings.glowPower,
            valueRange = 1.0f..1000.0f,
            formatValue = { String.format(Locale.US, "%.0f", it) },
            onValueChange = { onSettingsChange(settings.copy(glowPower = it)) }
        )

        ShaderSettingSlider(
            title = stringResource(R.string.shader_pulse_speed),
            value = settings.pulseSpeed,
            valueRange = 0.0f..10.0f,
            formatValue = { String.format(Locale.US, "%.1f", it) },
            onValueChange = { onSettingsChange(settings.copy(pulseSpeed = it)) }
        )

        ShaderSettingSlider(
            title = stringResource(R.string.shader_pulse_amount),
            value = settings.pulseAmount,
            valueRange = 0.0f..1.0f,
            onValueChange = { onSettingsChange(settings.copy(pulseAmount = it)) }
        )

        ShaderSettingSlider(
            title = stringResource(R.string.pixel_glow_vertical_top),
            value = settings.verticalTop,
            valueRange = 0.0f..2.0f,
            onValueChange = { onSettingsChange(settings.copy(verticalTop = it)) }
        )

        ShaderSettingSlider(
            title = stringResource(R.string.pixel_glow_vertical_bottom),
            value = settings.verticalBottom,
            valueRange = 0.0f..2.0f,
            onValueChange = { onSettingsChange(settings.copy(verticalBottom = it)) }
        )

        ShaderSettingSlider(
            title = stringResource(R.string.pixel_glow_glow_strength),
            value = settings.glowStrength,
            valueRange = 0.0f..2.0f,
            onValueChange = { onSettingsChange(settings.copy(glowStrength = it)) }
        )

        ShaderSettingSlider(
            title = stringResource(R.string.pixel_glow_dot_cell),
            value = settings.dotCell,
            valueRange = 0.0005f..0.02f,
            formatValue = { String.format(Locale.US, "%.4f", it) },
            onValueChange = { onSettingsChange(settings.copy(dotCell = it)) }
        )

        ShaderSettingSlider(
            title = stringResource(R.string.pixel_glow_dot_size),
            value = settings.dotSize,
            valueRange = 0.0f..0.5f,
            onValueChange = { onSettingsChange(settings.copy(dotSize = it)) }
        )

        ShaderSettingSlider(
            title = stringResource(R.string.pixel_glow_dot_density),
            value = settings.dotDensity,
            valueRange = 0.0f..1.0f,
            onValueChange = { onSettingsChange(settings.copy(dotDensity = it)) }
        )

        ShaderSettingSlider(
            title = stringResource(R.string.pixel_glow_dot_flicker),
            value = settings.dotFlicker,
            valueRange = 0.0f..20.0f,
            formatValue = { String.format(Locale.US, "%.1f", it) },
            onValueChange = { onSettingsChange(settings.copy(dotFlicker = it)) }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PixelGlowSettingsCardPreview() {
    CoolPerseusTheme {
        PixelGlowSettingsCard(
            settings = PixelGlowSettings(),
            onSettingsChange = {},
            onReset = {}
        )
    }
}
