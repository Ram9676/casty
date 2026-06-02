package com.casty.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.casty.music.ui.theme.CastyTheme

/**
 * Ultra-premium shimmer skeleton loader matching Spotify / high-end apps.
 * Smooth, subtle, hardware-accelerated.
 */
@Composable
fun CastySkeleton(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val infinite = rememberInfiniteTransition(label = "skeleton")
    val translateAnim by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerColors = listOf(
        CastyTheme.colors.elevation3.copy(alpha = 0.55f),
        CastyTheme.colors.elevation2.copy(alpha = 0.9f),
        CastyTheme.colors.elevation3.copy(alpha = 0.55f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnim - 300f, y = 0f),
        end = Offset(x = translateAnim, y = 0f)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

/** Convenience row skeleton for track list items (Spotify style) */
@Composable
fun TrackSkeletonRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        CastySkeleton(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(8.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            CastySkeleton(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp),
                shape = RoundedCornerShape(4.dp)
            )
            Spacer(Modifier.height(6.dp))
            CastySkeleton(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp),
                shape = RoundedCornerShape(4.dp)
            )
        }
        CastySkeleton(
            modifier = Modifier.size(18.dp),
            shape = RoundedCornerShape(50)
        )
    }
}

/** Grid / card skeleton for home shelves, albums etc. */
@Composable
fun ContentCardSkeleton(
    modifier: Modifier = Modifier,
    width: Dp = 140.dp,
    height: Dp = 140.dp
) {
    Column(modifier = modifier.width(width)) {
        CastySkeleton(
            modifier = Modifier
                .size(width)
                .aspectRatio(1f),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(10.dp))
        CastySkeleton(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(14.dp),
            shape = RoundedCornerShape(4.dp)
        )
        Spacer(Modifier.height(4.dp))
        CastySkeleton(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(11.dp),
            shape = RoundedCornerShape(4.dp)
        )
    }
}

/** Full screen list loading state (used in Playlist / Search results) */
@Composable
fun SkeletonTrackList(count: Int = 8, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        repeat(count) {
            TrackSkeletonRow()
            if (it < count - 1) Spacer(Modifier.height(2.dp))
        }
    }
}