package com.dertefter.coolperseus.animation_selection

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.dertefter.coolperseus.CoolPerseusTheme
import com.dertefter.coolperseus.R
import com.dertefter.shared.slider_animations.EdgesGlowSettings
import java.util.Locale

@Composable
fun EdgesGlowSettingsCard(
    settings: EdgesGlowSettings,
    onSettingsChange: (EdgesGlowSettings) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimationSettingsCard(
        title = stringResource(R.string.edges_glow),
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
            valueRange = 0.05f..2.0f,
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
            title = stringResource(R.string.edges_glow_max_vertical_top),
            value = settings.maxVerticalTop,
            valueRange = 0.0f..3.0f,
            onValueChange = { onSettingsChange(settings.copy(maxVerticalTop = it)) }
        )

        ShaderSettingSlider(
            title = stringResource(R.string.edges_glow_max_vertical_bottom),
            value = settings.maxVerticalBottom,
            valueRange = 0.0f..3.0f,
            onValueChange = { onSettingsChange(settings.copy(maxVerticalBottom = it)) }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EdgesGlowSettingsCardPreview() {
    CoolPerseusTheme {
        EdgesGlowSettingsCard(
            settings = EdgesGlowSettings(),
            onSettingsChange = {},
            onReset = {}
        )
    }
}
