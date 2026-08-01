package com.dertefter.coolperseus.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.gigamole.composefadingedges.FadingEdgesGravity
import com.gigamole.composefadingedges.verticalFadingEdges

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WidgetHostScreen(
    addedWidgetIds: SnapshotStateList<Int>,
    widgetHeights: Map<Int, Int>,
    showPicker: Boolean,
    onShowPickerChange: (Boolean) -> Unit,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    onAddAppWidget: (AppWidgetProviderInfo) -> Unit,
    onHeightChange: (Int, Int) -> Unit,
    packageManager: PackageManager
) {
    val listState = rememberLazyListState()
    var editingWidgetId by remember { mutableStateOf<Int?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onShowPickerChange(true) }
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Add Widget")
            }
        },
        topBar = {
            LargeFlexibleTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        "Widgets",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = { /* do something */ }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Localized description"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize()
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .verticalFadingEdges(
                    length = 100.dp
                )
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = paddingValues.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = paddingValues.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding() + 100.dp
            )
        ) {

            itemsIndexed(addedWidgetIds, key = { _, id -> id }) { index, widgetId ->
                val isEditing = widgetId == editingWidgetId

                val itemModifier = if (isEditing) {
                    Modifier.zIndex(1f)
                } else {
                    Modifier.animateItem()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(itemModifier)
                ) {

                    WidgetViewContainer(
                        widgetId = widgetId,
                        storedHeight = widgetHeights[widgetId],
                        onHeightChange = { onHeightChange(widgetId, it) },
                        appWidgetHost = appWidgetHost,
                        appWidgetManager = appWidgetManager,
                        isEditing = isEditing,
                        isFirst = index == 0,
                        isLast = index == addedWidgetIds.size - 1,
                        onMoveUp = {
                            if (index > 0) {
                                addedWidgetIds.add(index - 1, addedWidgetIds.removeAt(index))
                            }
                        },
                        onMoveDown = {
                            if (index < addedWidgetIds.size - 1) {
                                addedWidgetIds.add(index + 1, addedWidgetIds.removeAt(index))
                            }
                        },
                        onDelete = {
                            addedWidgetIds.remove(widgetId)
                            editingWidgetId = null
                        },
                        onConfirm = {
                            editingWidgetId = null
                        },
                        onLongClick = {
                            editingWidgetId = if (editingWidgetId == widgetId) null else widgetId
                        }
                    )
                }
            }
        }

        if (showPicker) {
            WidgetPickerList(
                appWidgetManager = appWidgetManager,
                packageManager = packageManager,
                onDismiss = { onShowPickerChange(false) },
                onSelect = { provider ->
                    onShowPickerChange(false)
                    onAddAppWidget(provider)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WidgetHostScreenPreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // This is a partial preview as we can't easily mock AppWidgetManager
            Text("Widget Host Screen Preview (Empty because of system dependencies)", Modifier.align(Alignment.Center))
        }
    }
}
