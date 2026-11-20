package com.dertefter.coolperseus.home

import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.dertefter.coolperseus.CoolPerseusTheme
import com.dertefter.coolperseus.sounds.SoundPackStore
import com.dertefter.shared.data.model.SoundType
import com.materialkolor.ktx.harmonize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SoundCard(
    modifier: Modifier = Modifier,
    sound: SoundType?,
    selected: Boolean = false,
    onClick: (() -> Unit) = {},
    onDelete: (() -> Unit)? = null,
    left: Float = 0f,
) {

    val labelVisible = left < 10f

    val title = sound?.name ?: stringResource(com.dertefter.coolperseus.R.string.no_sound)
    val sourceColor = MaterialTheme.colorScheme.primary
    val cover = rememberCoverBitmap(sound)

    val textBgColor = sourceColor.harmonize(MaterialTheme.colorScheme.tertiary, true)
    val textColor = sourceColor.harmonize(MaterialTheme.colorScheme.onTertiary, true)

    val selectedIconBgColor = sourceColor.harmonize(MaterialTheme.colorScheme.primary, true)
    val selectedIconColor = sourceColor.harmonize(MaterialTheme.colorScheme.onPrimary, true)

    val selectedIconSize by animateDpAsState(
        if (labelVisible && selected) 72.dp else 32.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
    )

    val selectedCookieDegree by animateIntAsState(
        if (selected) 180 else 0,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        cover?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        if (sound == null) {
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
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .background(textColor.copy(alpha = 0.64f))
                    .fillMaxSize()
            ) {
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

        if (sound != null && onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f))
            ) {
                Icon(
                    painter = painterResource(id = com.dertefter.coolperseus.R.drawable.ic_delete),
                    contentDescription = stringResource(com.dertefter.coolperseus.R.string.delete_sound_pack),
                    tint = MaterialTheme.colorScheme.error
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

@Composable
private fun rememberCoverBitmap(sound: SoundType?): ImageBitmap? {
    val context = LocalContext.current
    val source = sound?.imagePath
    var bitmap by remember(source) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(source) {
        bitmap = if (source.isNullOrEmpty()) {
            null
        } else {
            withContext(Dispatchers.IO) { decodeCover(context, source) }
        }
    }

    return bitmap
}

private fun decodeCover(context: Context, source: String, maxSide: Int = 720): ImageBitmap? =
    runCatching {
        val local = SoundPackStore.localFile(context, source)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openCoverStream(context, local, source)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSide)
            }
            openCoverStream(context, local, source)
                ?.use { BitmapFactory.decodeStream(it, null, options) }
                ?.asImageBitmap()
        }
    }.getOrNull()

private fun openCoverStream(context: Context, local: java.io.File?, source: String): InputStream? =
    local?.let { runCatching { it.inputStream() }.getOrNull() }
        ?: if (source.startsWith("content://")) {
            runCatching { context.contentResolver.openInputStream(source.toUri()) }.getOrNull()
        } else {
            null
        }

private fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
    var sample = 1
    while (width / sample > maxSide || height / sample > maxSide) sample *= 2
    return sample
}

@Composable
fun AddSoundCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    left: Float = 0f
) {
    val labelVisible = left < 10f

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Icon(
            painter = painterResource(id = com.dertefter.coolperseus.R.drawable.ic_import_soundpack),
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = stringResource(com.dertefter.coolperseus.R.string.add_sound_pack),
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
        )

        AnimatedVisibility(
            visible = labelVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .padding(horizontal = left.dp)
                .align(Alignment.BottomStart)
        ) {
            Text(
                text = stringResource(com.dertefter.coolperseus.R.string.add_sound_pack),
                modifier = Modifier
                    .padding(14.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(
                        horizontal = 10.dp,
                        vertical = 4.dp
                    ),
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Preview(
    showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES,
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
                sound = SoundType(
                    id = "pack_1",
                    name = "Warrior",
                    openSounds = listOf(""),
                    closeSounds = listOf("")
                ),
            )
            SoundCard(
                modifier = Modifier
                    .height(200.dp)
                    .weight(1f),
                sound = SoundType(
                    id = "pack_2",
                    name = "Lighter",
                    openSounds = listOf(""),
                    closeSounds = listOf("")
                ),
                selected = true,
            )
        }
    }
}
