package com.dertefter.coolperseus.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dertefter.coolperseus.design.R
import com.dertefter.coolperseus.design.theme.CoolPerseusTheme
import com.materialkolor.ktx.harmonize

@Composable
fun SoundCard(
    modifier: Modifier = Modifier,
    sound: String?,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
){
    val title = when (sound){
        "jianghu" -> stringResource(R.string.jianghu)
        "jixie" -> stringResource(R.string.jixie)
        "keji" -> stringResource(R.string.keji)
        "lingdong" -> stringResource(R.string.lingdong)
        "zippo" -> stringResource(R.string.zippo)
        null -> stringResource(R.string.no_sound)
        else -> sound
    }

    val sourceColor = when (sound){
        "jianghu" -> colorResource(R.color.jianghu)
        "jixie" -> colorResource(R.color.jixie)
        "keji" -> colorResource(R.color.keji)
        "lingdong" -> colorResource(R.color.lingdong)
        "zippo" -> colorResource(R.color.zippo)
        else -> MaterialTheme.colorScheme.primary
    }

    val imageRes = when (sound){
        "jianghu" -> R.drawable.slide_jianghu_open
        "jixie" -> R.drawable.slide_jixie_open
        "keji" -> R.drawable.slide_keji_open
        "lingdong" -> R.drawable.slide_lingdong_open
        "zippo" -> R.drawable.slide_zippo_open
        else -> null
    }

    val padding by animateDpAsState(
        targetValue = if (selected) 8.dp else 0.dp,
        label = "padding"
    )

    val borderColor = sourceColor.harmonize(MaterialTheme.colorScheme.primary)

    val borderColorAnimated by animateColorAsState(
        targetValue = if (selected) borderColor else borderColor.copy(alpha = 0f),
        label = "borderColor"
    )

    val gradColor = sourceColor.harmonize(MaterialTheme.colorScheme.primary, true)
    val textColor = sourceColor.harmonize(MaterialTheme.colorScheme.primaryFixed, true)

    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .border(
                color = borderColorAnimated,
                width = 4.dp,
                shape = shape
            )
            .padding(padding)
            .clip(RoundedCornerShape(24.dp - padding))
    ){
        imageRes?.let { imageRes ->
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        if (sound == null){
           Icon(
               painter = painterResource(id = R.drawable.ic_volume_off),
               tint = MaterialTheme.colorScheme.primary,
               contentDescription = stringResource(R.string.no_sound),
               modifier = Modifier.size(48.dp).align(Alignment.Center)
           )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            gradColor
                        )
                    )
                )
        )

        Text(
            text = title,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            style = MaterialTheme.typography.titleLarge,
            color = textColor

        )

    }
}

@Preview(showBackground = true)
@Composable
fun SoundCardPreview() {
    CoolPerseusTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SoundCard(
                modifier = Modifier.size(200.dp, 220.dp),
                sound = null,
                selected = false
            )
        }
    }
}

