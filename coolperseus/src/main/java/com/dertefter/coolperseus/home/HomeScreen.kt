package com.dertefter.coolperseus.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dertefter.coolperseus.CoolPerseusTheme
import com.dertefter.coolperseus.R
import com.dertefter.shared.data.model.SoundType
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

    val snackbarHostState = remember { SnackbarHostState() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onEvent(HomeEvent.OnImportSoundPack(it)) }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMsg ->
            snackbarHostState.showSnackbar(errorMsg)
            onEvent(HomeEvent.OnDismissError)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                val itemCount = uiState.sounds.size + 1
                val state = rememberCarouselState(initialItem = initialIndex) { itemCount }

                LaunchedEffect(uiState.selectedSound) {
                    val index = uiState.sounds.indexOf(uiState.selectedSound)
                    if (index >= 0 && state.currentItem != index) {
                        state.animateScrollToItem(index)
                    }
                }

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.app_sounds),
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { launcher.launch("*/*") }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_import_soundpack),
                                contentDescription = stringResource(R.string.add_sound_pack),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

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
                        if (index < uiState.sounds.size) {
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
                                onDelete = if (sound != null) {
                                    { onEvent(HomeEvent.OnDeleteSoundPack(sound)) }
                                } else null,
                                modifier = Modifier
                                    .maskClip(MaterialTheme.shapes.extraLarge)
                                    .fillMaxWidth(),
                            )
                        } else {
                            AddSoundCard(
                                left = carouselItemDrawInfo.maskRect.left / 4f,
                                onClick = {
                                    launcher.launch("*/*")
                                },
                                modifier = Modifier
                                    .maskClip(MaterialTheme.shapes.extraLarge)
                                    .fillMaxWidth()
                            )
                        }
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
                        content = { Text(stringResource(R.string.app_actions)) },
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
                        content = { Text(stringResource(R.string.app_animations)) },
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
                selectedSound = null,
                sounds = listOf(
                    SoundType(
                        id = "pack_1",
                        name = "Warrior",
                        openSounds = listOf(""),
                        closeSounds = listOf("")
                    ),
                    SoundType(
                        id = "pack_2",
                        name = "Lighter",
                        openSounds = listOf(""),
                        closeSounds = listOf("")
                    ),
                    null
                )
            ),
            onEvent = {},
            navigateToActions = {},
            navigateToAnimations = {}
        )
    }
}
