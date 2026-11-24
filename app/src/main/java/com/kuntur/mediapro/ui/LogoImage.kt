package com.kuntur.mediapro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage

private val Navy = Color(0xFF0A1628)
private val Gold = Color(0xFFE8B923)

@Composable
fun LogoImage(
    logoUrl: String?,
    category: String,
    size: Dp = 48.dp,
    corner: Dp = 10.dp
) {
    val shape = RoundedCornerShape(corner)
    val fallbackIcon = when (category.lowercase()) {
        "radio" -> Icons.Default.Radio
        "tv" -> Icons.Default.Tv
        else -> Icons.Default.Movie
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(Navy)
            .border(1.dp, Gold.copy(alpha = 0.35f), shape),
        contentAlignment = Alignment.Center
    ) {
        if (!logoUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(shape),
                loading = {
                    Icon(Icons.Default.PlayArrow, null, tint = Gold.copy(0.5f))
                },
                error = {
                    Icon(fallbackIcon, null, tint = Gold)
                }
            )
        } else {
            Icon(fallbackIcon, null, tint = Gold)
        }
    }
}
