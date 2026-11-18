package com.dertefter.coolperseus.home

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import com.dertefter.coolperseus.CoolPerseusTheme
import com.materialkolor.ktx.harmonize

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SoundCard(
    modifier: Modifier = Modifier,
    sound: String?,
    selected: Boolean = false,
    onClick: (() -> Unit) = {},
    left: Float = 0f,
){

    val labelVisible = left < 10f

    val title = when (sound){
        "jianghu" -> stringResource(com.dertefter.coolperseus.R.string.jianghu)
        "jixie" -> stringResource(com.dertefter.coolperseus.R.string.jixie)
        "keji" -> stringResource(com.dertefter.coolperseus.R.string.keji)
        "lingdong" -> stringResource(com.dertefter.coolperseus.R.string.lingdong)
        "zippo" -> stringResource(com.dertefter.coolperseus.R.string.zippo)
        null -> stringResource(com.dertefter.coolperseus.R.string.no_sound)
        else -> sound
    }

    val sourceColor = when (sound){
        "jianghu" -> colorResource(com.dertefter.coolperseus.R.color.jianghu)
        "jixie" -> colorResource(com.dertefter.coolperseus.R.color.jixie)
        "keji" -> colorResource(com.dertefter.coolperseus.R.color.keji)
        "lingdong" -> colorResource(com.dertefter.coolperseus.R.color.lingdong)
        "zippo" -> colorResource(com.dertefter.coolperseus.R.color.zippo)
        else -> MaterialTheme.colorScheme.primary
    }

    val imageRes = when (sound){
        "jianghu" -> com.dertefter.coolperseus.R.drawable.slide_jianghu_open
        "jixie" -> com.dertefter.coolperseus.R.drawable.slide_jixie_open
        "keji" -> com.dertefter.coolperseus.R.drawable.slide_keji_open
        "lingdong" -> com.dertefter.coolperseus.R.drawable.slide_lingdong_open
        "zippo" -> com.dertefter.coolperseus.R.drawable.slide_zippo_open
        else -> null
    }

    val textBgColor = sourceColor.harmonize(MaterialTheme.colorScheme.tertiary, true)
    val textColor = sourceColor.harmonize(MaterialTheme.colorScheme.onTertiary, true)

    val selectedIconBgColor = sourceColor.harmonize(MaterialTheme.colorScheme.primary, true)
    val selectedIconColor = sourceColor.harmonize(MaterialTheme.colorScheme.onPrimary, true)

    val selectedIconSize by animateDpAsState(
        if (labelVisible && selected) 72.dp else 32.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
    )

    val selectedCookieDegree by animateIntAsState(
        if (selected)  180 else 0,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainer)
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
               painter = painterResource(id = com.dertefter.coolperseus.R.drawable.ic_volume_off),
               tint = MaterialTheme.colorScheme.primary,
               contentDescription = stringResource(com.dertefter.coolperseus.R.string.no_sound),
               modifier = Modifier
                   .size(48.dp)
                   .align(Alignment.Center)
           )
        }

        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier

                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .background(textColor.copy(alpha = 0.64f))
                    .fillMaxSize()
            ){
                Icon(
                    painter = painterResource(id = com.dertefter.coolperseus.R.drawable.ic_check),
                    tint = selectedIconColor,
                    contentDescription = stringResource(com.dertefter.coolperseus.R.string.no_sound),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(selectedIconSize)
                        .clip(MaterialShapes.Cookie6Sided.toShape(selectedCookieDegree))
                        .background(selectedIconBgColor)
                        .padding(6.dp)

                )
            }
        }

        AnimatedVisibility(
            visible = labelVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .padding(horizontal = left.dp)
                .align(Alignment.BottomStart)
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .padding(14.dp)
                    .clip(CircleShape)
                    .background(textBgColor)
                    .padding(
                        horizontal = 10.dp,
                        vertical = 4.dp
                    ),
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = textColor
            )
        }


    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=673dp,height=841dp",
    wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE
)
@Composable
fun SoundCardPreviewDark() {
    CoolPerseusTheme {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SoundCard(
                modifier = Modifier
                    .height(200.dp)
                    .weight(1f),
                sound = null,
            )
            SoundCard(
                modifier = Modifier
                    .height(200.dp)
                    .weight(1f),
                sound = "jianghu",
            )
            SoundCard(
                modifier = Modifier
                    .height(200.dp)
                    .weight(1f),
                sound = "zippo",
                selected = true,
            )
        }
    }
}

