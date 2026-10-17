package com.example.mindalike.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mindalike.R

@Composable
fun MindAlikeLogo(
    size: Dp = 100.dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.mind_alike_logo),
        contentDescription = "MindAlike Mascot",
        modifier = modifier.size(size)
    )
}