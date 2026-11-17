package com.dertefter.coolperseus.animation_selection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dertefter.coolperseus.CoolPerseusTheme
import com.dertefter.coolperseus.R

@Composable
fun AnimationSelectionScreen(
    uiState: AnimationSelectionUiState,
    onEvent: (AnimationSelectionEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val animations = uiState.animations
    val pagerState = rememberPagerState(
        initialPage = animations.indexOf(uiState.selectedAnimation).coerceAtLeast(0),
        pageCount = { animations.size }
    )

    LaunchedEffect(uiState.selectedAnimation) {
        val targetIndex = animations.indexOf(uiState.selectedAnimation)
        if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val anim = animations.getOrNull(pagerState.currentPage)
        if (anim != uiState.selectedAnimation) {
            onEvent(AnimationSelectionEvent.OnSelectAnimation(anim))
        }
    }

    val currentAnimation = animations.getOrNull(pagerState.currentPage)

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                MediumFlexibleTopAppBar(
                    title = {
                        Text(text = stringResource(R.string.animation_selection_title))
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { page ->
                val title = when (val animation = animations[page]) {
                    "edges_glow" -> stringResource(R.string.edges_glow)
                    null -> stringResource(R.string.no_animation)
                    else -> animation
                }

                Box(
                    modifier = Modifier
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
        }

        if (currentAnimation == "edges_glow") {
            AnimationPreview(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnimationSelectionScreenPreview() {
    CoolPerseusTheme {
        AnimationSelectionScreen(
            uiState = AnimationSelectionUiState(
                selectedAnimation = "edges_glow",
                animations = listOf("edges_glow", null)
            ),
            onEvent = {}
        )
    }
}
