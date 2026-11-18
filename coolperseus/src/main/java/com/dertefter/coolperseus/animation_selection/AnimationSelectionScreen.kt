package com.dertefter.coolperseus.animation_selection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dertefter.coolperseus.CoolPerseusTheme
import com.dertefter.coolperseus.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@Composable
fun AnimationSelectionScreen(
    uiState: AnimationSelectionUiState,
    onEvent: (AnimationSelectionEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val animations = uiState.animations
    val selectedAnimation = uiState.selectedAnimation
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = animations.indexOf(selectedAnimation).coerceAtLeast(0),
        pageCount = { animations.size }
    )
    val currentSelected by rememberUpdatedState(selectedAnimation)

    LaunchedEffect(selectedAnimation, animations.size) {
        val targetIndex = animations.indexOf(selectedAnimation)
        if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetIndex)
        }
    }

    LaunchedEffect(pagerState, animations) {
        if (animations.isEmpty()) return@LaunchedEffect
        snapshotFlow { pagerState.isScrollInProgress }
            .distinctUntilChanged()
            .drop(1)
            .collect { scrolling ->
                if (scrolling) return@collect
                val page = pagerState.settledPage
                if (page !in animations.indices) return@collect
                val animation = animations[page]
                if (animation != currentSelected) {
                    onEvent(AnimationSelectionEvent.OnSelectAnimation(animation))
                }
            }
    }

    val isInPreview = LocalInspectionMode.current

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(text = stringResource(R.string.animation_selection_title))
                        },
                        navigationIcon = {
                            IconButton(onClick = { onEvent(AnimationSelectionEvent.OnNavigateBack) }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_back),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    )
                    if (animations.isNotEmpty()) {
                        PrimaryScrollableTabRow(
                            edgePadding = 8.dp,
                            selectedTabIndex = pagerState.currentPage.coerceIn(animations.indices)
                        ) {
                            animations.forEachIndexed { index, animation ->
                                Tab(
                                    selected = pagerState.currentPage == index,
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                    text = {
                                        Text(
                                            text = when (animation) {
                                                "edges_glow" -> stringResource(R.string.edges_glow)
                                                "pixel_glow" -> stringResource(R.string.pixel_glow)
                                                null -> stringResource(R.string.no_animation)
                                                else -> animation
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { page ->
                when (val animation = animations.getOrNull(page)) {
                    "edges_glow" -> EdgesGlowSettingsCard(
                        settings = uiState.edgesGlowSettings,
                        onSettingsChange = { settings ->
                            onEvent(AnimationSelectionEvent.OnUpdateEdgesGlowSettings(settings))
                        },
                        onReset = { onEvent(AnimationSelectionEvent.OnResetEdgesGlowSettings) }
                    )

                    "pixel_glow" -> PixelGlowSettingsCard(
                        settings = uiState.pixelGlowSettings,
                        onSettingsChange = { settings ->
                            onEvent(AnimationSelectionEvent.OnUpdatePixelGlowSettings(settings))
                        },
                        onReset = { onEvent(AnimationSelectionEvent.OnResetPixelGlowSettings) }
                    )

                    else -> AnimationTitlePage(
                        title = animation ?: stringResource(R.string.no_animation)
                    )
                }
            }
        }

        if (!isInPreview && selectedAnimation != null) {
            AnimationPreview(
                animation = selectedAnimation,
                modifier = Modifier.fillMaxSize(),
                edgesGlowSettings = uiState.edgesGlowSettings,
                pixelGlowSettings = uiState.pixelGlowSettings
            )
        }
    }
}

@Composable
private fun AnimationTitlePage(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AnimationSelectionScreenPreview() {
    CoolPerseusTheme {
        AnimationSelectionScreen(
            uiState = AnimationSelectionUiState(
                selectedAnimation = "edges_glow",
                animations = listOf("edges_glow", "pixel_glow", null)
            ),
            onEvent = {}
        )
    }
}
