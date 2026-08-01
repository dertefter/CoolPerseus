package com.dertefter.coolperseus.overlay

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.media.MediaPlayer
import android.os.Bundle
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dertefter.coolperseus.data.model.SoundType

@Composable
fun AnimationOverlayRoute(
    type: String,
    onAnimationFinished: () -> Unit,
    viewModel: AnimationOverlayViewModel = hiltViewModel(),
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager
) {
    val context = LocalContext.current
    val soundType by viewModel.soundType.collectAsState(initial = null)
    val widgets by viewModel.widgets.collectAsState(initial = emptyList())
    val percent = remember { Animatable(0f) }

    val isOpen = type == "slider_up"

    LaunchedEffect(soundType) {
        if (soundType != null) {
            val soundRes = if (isOpen) {
                when (soundType) {
                    is SoundType.Keji -> R.raw.slide_keji_close
                    is SoundType.Zippo -> R.raw.slide_zippo_close
                    is SoundType.Jianghu -> R.raw.slide_jianghu_close
                    is SoundType.Lingdong -> R.raw.slide_lingdong_close
                    is SoundType.Jixie -> R.raw.slide_jiguan_close
                    else -> null
                }
            } else {
                when (soundType) {
                    is SoundType.Keji -> R.raw.slide_keji_open
                    is SoundType.Zippo -> R.raw.slide_zippo_open
                    is SoundType.Jianghu -> R.raw.slide_jianghu_open
                    is SoundType.Lingdong -> R.raw.slide_lingdong_open
                    is SoundType.Jixie -> R.raw.slide_jiguan_open
                    else -> null
                }
            }

            val mediaPlayer = if (soundRes != null) {
                MediaPlayer.create(context, soundRes)
            } else {
                val currentSoundType = soundType
                if (currentSoundType is SoundType.Custom) {
                    val path =
                        if (isOpen) currentSoundType.closeSoundPath else currentSoundType.openSoundPath
                    if (path.isNotEmpty()) {
                        try {
                            MediaPlayer.create(context, path.toUri())
                        } catch (_: Exception) {
                            null
                        }
                    } else null
                } else null
            }

            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener {
                it.release()
            }
        }

        percent.animateTo(
            targetValue = 1f,
            animationSpec = tween(600, easing = LinearEasing)
        )
        onAnimationFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        EdgesGlow(percent = percent.value, closed = isOpen)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            widgets.forEach { widgetId ->
                val appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(widgetId)
                if (appWidgetProviderInfo != null) {
                    val minHeight = if (appWidgetProviderInfo.minResizeHeight > 0) appWidgetProviderInfo.minResizeHeight else 100
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(minHeight.dp)
                            .padding(vertical = 8.dp),
                        factory = { ctx ->
                            appWidgetHost.createView(ctx, widgetId, appWidgetProviderInfo)
                        },
                        update = { view ->
                            view.post {
                                val displayMetrics = view.resources.displayMetrics
                                val widthDp = (view.width / displayMetrics.density).toInt()
                                val actualHeightDp = (view.height / displayMetrics.density).toInt()

                                if (widthDp > 0 && actualHeightDp > 0) {
                                    val options = Bundle().apply {
                                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
                                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
                                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, actualHeightDp)
                                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, actualHeightDp)
                                    }
                                    view.updateAppWidgetOptions(options)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
