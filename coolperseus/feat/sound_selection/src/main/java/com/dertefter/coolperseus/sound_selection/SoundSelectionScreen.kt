package com.dertefter.coolperseus.sound_selection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dertefter.coolperseus.design.components.SoundCard
import androidx.compose.ui.tooling.preview.Preview
import com.dertefter.coolperseus.design.theme.CoolPerseusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundSelectionScreen(
    uiState: SoundSelectionUiState,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(text = "Звук слайдера")
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(Event.OnNavigateBack) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = innerPadding
        ) {
            items(uiState.sounds) { sound ->
                SoundCard(
                    sound = sound,
                    selected = uiState.selectedSound == sound,
                    onClick = {
                        onEvent(Event.OnSelectSound(sound))
                    },
                    modifier = Modifier
                        .height(220.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SoundSelectionScreenPreview() {
    CoolPerseusTheme {
        SoundSelectionScreen(
            uiState = SoundSelectionUiState(
                selectedSound = "keji",
                sounds = listOf(null, "keji", "jixie", "zippo", "jianghu", "lingdong")
            ),
            onEvent = {}
        )
    }
}

