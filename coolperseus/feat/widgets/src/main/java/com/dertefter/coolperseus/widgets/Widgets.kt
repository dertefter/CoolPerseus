package com.dertefter.coolperseus.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight

// Модель виджета
data class WidgetInfo(
    val id: Int,
    val name: String,
    val width: Int = 1,  // в ячейках (1-4)
    val height: Int = 1, // в ячейках (1-4)
    var x: Int = 0,     // позиция в сетке
    var y: Int = 0,
)

// Размеры ячейки сетки
private val CELL_SIZE = 80.dp
private val CELL_SPACING = 8.dp

// Доступные размеры виджетов
private val WIDGET_SIZES = listOf(
    1 to 1, // 1x1
    1 to 2, // 1x2
    2 to 2, // 2x2
    1 to 3, // 1x3
    1 to 4, // 1x4
    2 to 3, // 2x3
    3 to 3, // 3x3
    2 to 4, // 2x4
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun Widgets(
    uiState: WidgetsUiState,
    onEvent: (WidgetsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Состояние для демонстрационных виджетов
    val demoWidgets = rememberSaveable {
        listOf(
            WidgetInfo(
                id = 1,
                name = "Погода",
                width = 2,
                height = 2,
                x = 0,
                y = 0
            ),
            WidgetInfo(
                id = 2,
                name = "Часы",
                width = 1,
                height = 2,
                x = 2,
                y = 0
            ),
            WidgetInfo(
                id = 3,
                name = "Календарь",
                width = 1,
                height = 1,
                x = 3,
                y = 0
            ),
            WidgetInfo(
                id = 4,
                name = "Музыка",
                width = 1,
                height = 3,
                x = 0,
                y = 2
            ),
        )
    }

    var widgets by rememberSaveable { mutableStateOf(demoWidgets) }
    var showAddWidgetMenu by rememberSaveable { mutableStateOf(false) }
    
    // Состояние для перетаскивания
    var draggedWidget by rememberSaveable { mutableStateOf<WidgetInfo?>(null) }
    var dragOffset by rememberSaveable { mutableStateOf(Offset.Zero) }
    
    // Состояние для изменения размера
    var resizingWidget by rememberSaveable { mutableStateOf<WidgetInfo?>(null) }
    var resizeWidth by rememberSaveable { mutableIntStateOf(1) }
    var resizeHeight by rememberSaveable { mutableIntStateOf(1) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Виджеты",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(WidgetsEvent.OnNavigateBack) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = uiState.isEditMode,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Button(
                            onClick = { onEvent(WidgetsEvent.OnEditModeChange(false)) },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Готово")
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (!uiState.isEditMode) {
                FloatingActionButton(
                    onClick = { showAddWidgetMenu = true },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(painter = painterResource(R.drawable.ic_add), contentDescription = "Добавить виджет")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {
            // Сетка виджетов
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .padding(CELL_SPACING)
            ) {
                // Отображаем сетку (4 колонки)
                val columns = 4
                
                // Отображаем все виджеты
                widgets.forEach { widget ->
                    val widgetWidth = CELL_SIZE * widget.width + CELL_SPACING * (widget.width - 1)
                    val widgetHeight = CELL_SIZE * widget.height + CELL_SPACING * (widget.height - 1)
                    
                    val xOffset = (CELL_SIZE + CELL_SPACING) * widget.x
                    val yOffset = (CELL_SIZE + CELL_SPACING) * widget.y
                    
                    Box(
                        modifier = Modifier
                            .offset(x = xOffset, y = yOffset)
                            .width(widgetWidth)
                            .height(widgetHeight)
                            .pointerInput(uiState.isEditMode, widget) {
                                if (uiState.isEditMode) {
                                    // В режиме редактирования: перетаскивание
                                    detectDragGestures(
                                        onDragStart = {
                                            draggedWidget = widget
                                            dragOffset = Offset.Zero
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount
                                            
                                            // Вычисляем новую позицию в сетке
                                            val cellSizePx = CELL_SIZE.toPx() + CELL_SPACING.toPx()
                                            val newX = widget.x + (dragOffset.x / cellSizePx).toInt()
                                            val newY = widget.y + (dragOffset.y / cellSizePx).toInt()
                                            
                                            // Проверяем границы и коллизии
                                            if (newX >= 0 && newX + widget.width <= columns &&
                                                newY >= 0 &&
                                                !widgets.any { it.id != widget.id && 
                                                    newX < it.x + it.width && 
                                                    newX + widget.width > it.x &&
                                                    newY < it.y + it.height && 
                                                    newY + widget.height > it.y }
                                            ) {
                                                widgets = widgets.map {
                                                    if (it.id == widget.id) {
                                                        it.copy(x = newX, y = newY)
                                                    } else {
                                                        it
                                                    }
                                                }
                                                draggedWidget = widget.copy(x = newX, y = newY)
                                            }
                                        },
                                        onDragEnd = {
                                            draggedWidget = null
                                            dragOffset = Offset.Zero
                                        }
                                    )
                                }
                            }
                            .pointerInput(widget) {
                                // Обработка долгого нажатия для включения режима редактирования
                                detectTapGestures(
                                    onLongPress = {
                                        if (!uiState.isEditMode) {
                                            onEvent(WidgetsEvent.OnEditModeChange(true))
                                            resizingWidget = widget
                                            resizeWidth = widget.width
                                            resizeHeight = widget.height
                                        }
                                    },
                                    onTap = {
                                        if (uiState.isEditMode) {
                                            // В режиме редактирования: выбор виджета для изменения размера
                                            resizingWidget = widget
                                            resizeWidth = widget.width
                                            resizeHeight = widget.height
                                        }
                                    }
                                )
                            }
                    ) {
                        // Контейнер виджета
                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = if (uiState.isEditMode) 2.dp else 0.dp,
                                    color = if (resizingWidget?.id == widget.id) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (uiState.isEditMode) 8.dp else 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Контент виджета (превью)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = widget.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                if (uiState.isEditMode) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                widgets = widgets.filter { it.id != widget.id }
                                                if (resizingWidget?.id == widget.id) {
                                                    resizingWidget = null
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_close),
                                                contentDescription = "Удалить",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Рамка для изменения размера (только для выбранного виджета в режиме редактирования)
                        if (uiState.isEditMode && resizingWidget?.id == widget.id) {
                            // Угловые маркеры для изменения размера
                            listOf(
                                Alignment.TopStart to { w: Int, h: Int -> (w to h) },
                                Alignment.TopEnd to { w: Int, h: Int -> (w to h) },
                                Alignment.BottomStart to { w: Int, h: Int -> (w to h) },
                                Alignment.BottomEnd to { w: Int, h: Int -> (w to h) },
                            ).forEach { (alignment, sizeFunc) ->
                                Box(
                                    modifier = Modifier
                                        .align(alignment)
                                        .size(16.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            2.dp,
                                            MaterialTheme.colorScheme.surface,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .pointerInput(widget) {
                                            detectDragGestures(
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    
                                                    val cellSizePx = CELL_SIZE.toPx()
                                                    val deltaCellsX = (dragAmount.x / cellSizePx).toInt()
                                                    val deltaCellsY = (dragAmount.y / cellSizePx).toInt()
                                                    
                                                    var newWidth = resizeWidth
                                                    var newHeight = resizeHeight
                                                    
                                                    when (alignment) {
                                                        Alignment.BottomEnd -> {
                                                            newWidth = (resizeWidth + deltaCellsX).coerceIn(1, 4)
                                                            newHeight = (resizeHeight + deltaCellsY).coerceIn(1, 4)
                                                        }
                                                        Alignment.BottomStart -> {
                                                            newWidth = (resizeWidth - deltaCellsX).coerceIn(1, 4)
                                                            newHeight = (resizeHeight + deltaCellsY).coerceIn(1, 4)
                                                        }
                                                        Alignment.TopEnd -> {
                                                            newWidth = (resizeWidth + deltaCellsX).coerceIn(1, 4)
                                                            newHeight = (resizeHeight - deltaCellsY).coerceIn(1, 4)
                                                        }
                                                        Alignment.TopStart -> {
                                                            newWidth = (resizeWidth - deltaCellsX).coerceIn(1, 4)
                                                            newHeight = (resizeHeight - deltaCellsY).coerceIn(1, 4)
                                                        }
                                                    }
                                                    
                                                    // Проверяем доступность размера
                                                    if (WIDGET_SIZES.contains(newWidth to newHeight)) {
                                                        resizeWidth = newWidth
                                                        resizeHeight = newHeight
                                                    }
                                                },
                                                onDragEnd = {
                                                    // Применяем новый размер
                                                    widgets = widgets.map {
                                                        if (it.id == widget.id) {
                                                            it.copy(width = resizeWidth, height = resizeHeight)
                                                        } else {
                                                            it
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                )
                            }
                        }
                    }
                }
                
                // Отображаем перетаскиваемый виджет поверх остальных
                draggedWidget?.let { widget ->
                    val widgetWidth = CELL_SIZE * widget.width + CELL_SPACING * (widget.width - 1)
                    val widgetHeight = CELL_SIZE * widget.height + CELL_SPACING * (widget.height - 1)
                    
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (CELL_SIZE + CELL_SPACING) * widget.x + dragOffset.x.dp,
                                y = (CELL_SIZE + CELL_SPACING) * widget.y + dragOffset.y.dp
                            )
                            .width(widgetWidth)
                            .height(widgetHeight)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    RoundedCornerShape(8.dp)
                                ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = widget.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${widget.width}x${widget.height}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Меню добавления виджета
    if (showAddWidgetMenu) {
        val availableWidgets = listOf(
            WidgetInfo(id = 5, name = "Новости", width = 2, height = 2),
            WidgetInfo(id = 6, name = "Фото", width = 1, height = 1),
            WidgetInfo(id = 7, name = "Заметки", width = 1, height = 3),
            WidgetInfo(id = 8, name = "Почта", width = 2, height = 3),
            WidgetInfo(id = 9, name = "Спорт", width = 1, height = 2),
            WidgetInfo(id = 10, name = "Финансы", width = 2, height = 1),
        )
        
        DropdownMenu(
            expanded = showAddWidgetMenu,
            onDismissRequest = { showAddWidgetMenu = false },
            modifier = Modifier.width(280.dp)
        ) {
            Text(
                text = "Добавить виджет",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .height(400.dp)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableWidgets) { widget ->
                    Card(
                        modifier = Modifier
                            .height(120.dp)
                            .clickable {
                                // Находим свободное место для нового виджета
                                val columns = 4
                                var placed = false
                                
                                for (y in 0..10) {
                                    for (x in 0 until columns) {
                                        if (x + widget.width <= columns &&
                                            !widgets.any { 
                                                x < it.x + it.width && 
                                                x + widget.width > it.x &&
                                                y < it.y + it.height && 
                                                y + widget.height > it.y 
                                            }
                                        ) {
                                            widgets = widgets + widget.copy(
                                                id = widgets.size + 1,
                                                x = x,
                                                y = y
                                            )
                                            placed = true
                                            break
                                        }
                                    }
                                    if (placed) break
                                }
                                
                                if (!placed) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Недостаточно места для виджета")
                                    }
                                }
                                
                                showAddWidgetMenu = false
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            RoundedCornerShape(12.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = widget.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${widget.width}x${widget.height}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Панель выбора размера для изменения размеров виджета
    if (uiState.isEditMode && resizingWidget != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Изменить размер: ${resizeWidth}x${resizeHeight}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(WIDGET_SIZES) { (width, height) ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(width.toFloat() / height.toFloat())
                                .background(
                                    color = if (resizeWidth == width && resizeHeight == height) 
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = if (resizeWidth == width && resizeHeight == height) 2.dp else 1.dp,
                                    color = if (resizeWidth == width && resizeHeight == height)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    resizeWidth = width
                                    resizeHeight = height
                                    
                                    // Применяем новый размер к виджету
                                    widgets = widgets.map {
                                        if (it.id == resizingWidget?.id) {
                                            it.copy(width = width, height = height)
                                        } else {
                                            it
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${width}x$height",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { resizingWidget = null }
                    ) {
                        Text("Закрыть")
                    }
                }
            }
        }
    }
}

// События для виджетов
sealed class WidgetsEvent {
    object OnNavigateBack : WidgetsEvent()
    data class OnEditModeChange(val enabled: Boolean) : WidgetsEvent()
    data class OnDragWidget(val widgetId: Int, val newX: Int, val newY: Int) : WidgetsEvent()
    data class OnResizeWidget(val widgetId: Int, val width: Int, val height: Int) : WidgetsEvent()
    data class OnDeleteWidget(val widgetId: Int) : WidgetsEvent()
    data class OnAddWidget(val widgetInfo: WidgetInfo) : WidgetsEvent()
}