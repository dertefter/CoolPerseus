package com.dertefter.coolperseus.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dertefter.coolperseus.R

@Composable
fun WidgetViewContainer(
    widgetId: Int,
    storedHeight: Int? = null,
    onHeightChange: (Int) -> Unit = {},
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    isEditing: Boolean = false,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onDelete: () -> Unit = {},
    onConfirm: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(widgetId)
        ?: return

    val minHeight = if (appWidgetProviderInfo.minResizeHeight > 0) appWidgetProviderInfo.minResizeHeight else 100
    var currentHeight by remember(storedHeight) { mutableFloatStateOf((storedHeight ?: minHeight).toFloat()) }
    var stableHeight by remember(storedHeight) { mutableIntStateOf(storedHeight ?: minHeight) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(currentHeight.dp),
        contentAlignment = Alignment.Center
    ) {
        WidgetViewWrapper(
            isEditing = isEditing
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize(),
                factory = { context ->
                    LongClickInterceptor(context).apply {
                        onLongClickAction = onLongClick
                        this.isEditing = isEditing
                        val hostView = appWidgetHost.createView(context, widgetId, appWidgetProviderInfo)
                        addView(hostView)
                    }
                },
                update = { interceptor ->
                    interceptor.onLongClickAction = onLongClick
                    interceptor.isEditing = isEditing
                    val hostView = interceptor.getChildAt(0) as? android.appwidget.AppWidgetHostView
                    hostView?.let { view ->
                        val displayMetrics = view.resources.displayMetrics
                        val widthDp = if (view.width > 0) (view.width / displayMetrics.density).toInt()
                        else (displayMetrics.widthPixels / displayMetrics.density).toInt()
                        val heightDp = stableHeight

                        if (widthDp > 0 && heightDp > 0) {
                            val options = Bundle().apply {
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
                            }
                            view.updateAppWidgetOptions(options)
                        }
                    }
                }
            )
        }


        AnimatedVisibility(
            visible = isEditing,
            modifier = Modifier
                .align(Alignment.BottomStart),
        ) {
            WidgetEditingActions(
                isFirst = isFirst,
                isLast = isLast,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onDelete = onDelete,
                onConfirm = onConfirm
            )
        }

        AnimatedVisibility(
            visible = isEditing,
            modifier = Modifier
                .align(Alignment.BottomEnd),
        ) {
            Icon(
                painter = painterResource(R.drawable.widget_drag_handle),
                contentDescription = "Resize",
                modifier = Modifier
                    .size(48.dp) // Увеличим область касания до стандарта 48dp
                    .padding(4.dp)
                    .pointerInput(storedHeight) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val deltaDp = with(density) { dragAmount.y.toDp().value }
                                currentHeight = (currentHeight + deltaDp).coerceAtLeast(minHeight.toFloat())
                            },
                            onDragEnd = {
                                val finalHeight = currentHeight.toInt()
                                stableHeight = finalHeight
                                onHeightChange(finalHeight)
                            }
                        )
                    },
                tint = MaterialTheme.colorScheme.primary
            )
        }

    }
}

private class LongClickInterceptor(context: Context) : FrameLayout(context) {
    var onLongClickAction: () -> Unit = {}
    var isEditing: Boolean = false

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            onLongClickAction()
        }
    })

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        if (isEditing) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }
}

@Composable
fun WidgetViewWrapper(
    isEditing: Boolean = false,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(if (isEditing) 0.95f else 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        content()
    }
}


@Composable
private fun WidgetEditingActions(
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.primaryContainer,
                CircleShape
            )
            .padding(all = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = onDelete,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onError,
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }

        if (!isFirst) {
            IconButton(
                onClick = onMoveUp,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    Icons.Rounded.ArrowUpward,
                    contentDescription = "Move Up"
                )
            }
        }

        if (!isLast) {
            IconButton(
                onClick = onMoveDown,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    Icons.Rounded.ArrowDownward,
                    contentDescription = "Move Down"
                )
            }
        }

        IconButton(
            onClick = onConfirm,
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Confirm"
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF777070)
@Composable
fun WidgetViewWrapperPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            WidgetViewWrapper(
                isEditing = true,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color.Yellow),
                    contentAlignment = Alignment.Center
                ) {
                    Text("isEditing Widget")
                }
            }
            WidgetEditingActions(
                isFirst = false,
                isLast = false,
                onMoveUp = {},
                onMoveDown = {},
                onDelete = {},
                onConfirm = {},
                modifier = Modifier.align(Alignment.BottomStart)
            )
            Icon(
                painter = painterResource(R.drawable.widget_drag_handle),
                contentDescription = "Resize",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}
