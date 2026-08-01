package com.dertefter.coolperseus.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun WidgetPickerList(
    appWidgetManager: AppWidgetManager,
    packageManager: PackageManager,
    onDismiss: () -> Unit,
    onSelect: (AppWidgetProviderInfo) -> Unit
) {
    val providers = remember { appWidgetManager.installedProviders }
    val groupedProviders = remember(providers) {
        providers.groupBy { it.provider.packageName }
    }
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    WidgetPickerListContent(
        groupedProviders = groupedProviders,
        expandedStates = expandedStates,
        packageManager = packageManager,
        onDismiss = onDismiss,
        onSelect = onSelect,
        onToggleExpand = { packageName ->
            expandedStates[packageName] = !(expandedStates[packageName] ?: false)
        }
    )
}

@Composable
fun WidgetPickerListContent(
    groupedProviders: Map<String, List<AppWidgetProviderInfo>>,
    expandedStates: Map<String, Boolean>,
    packageManager: PackageManager?,
    onDismiss: () -> Unit,
    onSelect: (AppWidgetProviderInfo) -> Unit,
    onToggleExpand: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите виджет") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                groupedProviders.forEach { (packageName, widgets) ->
                    val appInfo = try {
                        packageManager?.getApplicationInfo(packageName, 0)
                    } catch (_: Exception) {
                        null
                    }
                    val appLabel = packageManager?.let { appInfo?.loadLabel(it)?.toString() } ?: packageName
                    val appIcon = packageManager?.let { appInfo?.loadIcon(it) }
                    val isExpanded = expandedStates[packageName] ?: false

                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onToggleExpand(packageName)
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (appIcon != null) {
                                    val bitmap = (appIcon as? BitmapDrawable)?.bitmap
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = appLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            }
                        }
                    }

                    if (isExpanded) {
                        widgets.forEach { provider ->
                            item {
                                val widgetLabel = packageManager?.let { provider.loadLabel(it) } ?: "Widget"
                                TextButton(
                                    onClick = { onSelect(provider) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 32.dp)
                                ) {
                                    Text(
                                        text = widgetLabel,
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Preview
@Composable
fun WidgetPickerListPreview() {
    MaterialTheme {
        WidgetPickerListContent(
            groupedProviders = mapOf(
                "com.example.app" to listOf(),
                "com.android.settings" to listOf()
            ),
            expandedStates = mapOf("com.example.app" to true),
            packageManager = null,
            onDismiss = {},
            onSelect = {},
            onToggleExpand = {}
        )
    }
}
