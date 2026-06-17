package com.dertefter.coolperseus.action_selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun TitleValueCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String? = null,
    onClick: () -> Unit = {}
){

    val valueColor by animateColorAsState(
        if (value.isNullOrEmpty()){
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.primary
        }
    )

    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp)
            .fillMaxWidth()
    ){
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = value ?: stringResource(R.string.nothing_to_do),
            style = MaterialTheme.typography.labelLarge,
            color = valueColor
        )
    }
}

@Composable
@Preview
fun TitleValueCardPrev(){
    TitleValueCard(Modifier, "ddddd", null)
}