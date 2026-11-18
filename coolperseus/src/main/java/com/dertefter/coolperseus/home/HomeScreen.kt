package com.dertefter.coolperseus.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dertefter.coolperseus.CoolPerseusTheme
import com.dertefter.coolperseus.R
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    navigateToActions: () -> Unit,
    navigateToAnimations: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MediumFlexibleTopAppBar(
                title = {
                    Text(text = stringResource(R.string.app_name))
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    uriHandler.openUri("https://github.com/dertefter/CoolPerseus")
                }
            ) {
                Icon(
                    painterResource(R.drawable.ic_github_logo),
                    contentDescription = stringResource(R.string.app_name)
                )
            }
        }
    ) { contentPadding ->

        LazyColumn(
            contentPadding = contentPadding,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {

                val initialIndex = uiState.sounds.indexOf(uiState.selectedSound).let { if (it >= 0) it else 0 }
                val state = rememberCarouselState(initialItem = initialIndex) { uiState.sounds.count() }

                LaunchedEffect(uiState.selectedSound) {
                    val index = uiState.sounds.indexOf(uiState.selectedSound)
                    if (index >= 0 && state.currentItem != index) {
                        state.animateScrollToItem(index)
                    }
                }

                Column {
                    Text(
                        text = stringResource(R.string.app_sounds),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 4.dp
                        )
                    )

                    HorizontalCenteredHeroCarousel(
                        state = state,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .fillMaxWidth()
                            .height(200.dp),
                        minSmallItemWidth = 38.dp,
                        maxSmallItemWidth = 38.dp,
                        itemSpacing = 6.dp,
                    ) { index ->
                        val sound = uiState.sounds[index]
                        SoundCard(
                            sound = sound,
                            left = carouselItemDrawInfo.maskRect.left / 4f,
                            selected = uiState.selectedSound == sound,
                            onClick = {
                                coroutineScope.launch {
                                    state.animateScrollToItem(index)
                                }
                                onEvent(HomeEvent.OnSelectSound(sound))
                            },
                            modifier = Modifier
                                .maskClip(MaterialTheme.shapes.extraLarge)
                                .fillMaxWidth(),
                        )

                    }



                }


            }

            item {
                val colors =
                    ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    Text(
                        text = stringResource(R.string.app_actions),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 4.dp
                        )
                    )

                    SegmentedListItem(
                        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                        colors = colors,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.ic_mobile_hand),
                                contentDescription = null
                            )
                        },
                        content = { Text( stringResource(R.string.app_actions)) },
                        modifier = Modifier.clickable { navigateToActions() }
                    )

                    SegmentedListItem(
                        shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2),
                        colors = colors,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.ic_sparkles),
                                contentDescription = null
                            )
                        },
                        content = { Text( stringResource(R.string.app_animations)) },
                        modifier = Modifier.clickable { navigateToAnimations() }
                    )






                }

            }

        }

    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    CoolPerseusTheme {
        HomeScreen(
            uiState = HomeUiState(
                selectedSound = "keji",
                sounds = listOf("keji", "jixie", "zippo", "jianghu", "lingdong", null)
            ),
            onEvent = {},
            navigateToActions = {},
            navigateToAnimations = {}
        )
    }
}

