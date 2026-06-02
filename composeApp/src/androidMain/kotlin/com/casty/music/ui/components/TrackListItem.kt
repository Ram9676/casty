package com.casty.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.casty.music.data.Song
import com.casty.music.data.cleanTitle
import com.casty.music.ui.theme.CastyTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackListItem(
    song: Song,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false
) {
    val haptics = rememberCastyHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Magical scale animation on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "PressScale"
    )
    
    // Pulsing glow effect for currently playing song
    val infiniteTransition = rememberInfiniteTransition(label = "playingGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    // Gradient background for playing state
    val backgroundBrush = if (isPlaying) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF6B35F7).copy(alpha = 0.2f),
                Color(0xFFF72585).copy(alpha = 0.1f),
                Color.Transparent
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Transparent)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundBrush)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        haptics.tick()
                        onClick()
                    },
                    onLongClick = onLongClick?.let {
                        {
                            haptics.longPress()
                            it()
                        }
                    }
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated Album Art with glow
            Box {
                CastyArtwork(
                    model = song.thumbnailUrl,
                    contentDescription = "Track Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                )
                
                // Playing indicator overlay
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF6B35F7).copy(alpha = glowAlpha),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    
                    // Equalizer animation
                    Row(
                        modifier = Modifier
                            .size(56.dp)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        repeat(3) { index ->
                            val barHeight by infiniteTransition.animateFloat(
                                initialValue = 4f,
                                targetValue = 16f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(
                                        durationMillis = 400 + (index * 150),
                                        easing = EaseInOut
                                    ),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "barHeight$index"
                            )
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(barHeight.dp)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info: Title & Subtitle (artist)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.cleanTitle(),
                    style = CastyTheme.typography.bodyLarge.copy(
                        color = if (isPlaying) 
                            CastyTheme.colors.accentPink 
                        else 
                            CastyTheme.colors.textPrimary,
                        fontWeight = if (isPlaying) androidx.compose.ui.text.font.FontWeight.Bold 
                                   else androidx.compose.ui.text.font.FontWeight.Normal
                    ),
                    maxLines = 1
                )
                Text(
                    text = song.artistsText.orEmpty(),
                    style = CastyTheme.typography.bodyMedium.copy(
                        color = CastyTheme.colors.textSecondary
                    ),
                    maxLines = 1
                )
            }

            // Menu button with magnetic effect
            IconButton(
                onClick = {
                    haptics.tick()
                    onMenuClick?.invoke()
                }
            ) {
                Icon(
                    painter = painterResource(id = com.casty.music.R.drawable.more),
                    contentDescription = "More options",
                    tint = if (isPlaying) 
                        CastyTheme.colors.accentPink 
                    else 
                        CastyTheme.colors.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Bottom separator line with gradient
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            CastyTheme.colors.elevation2,
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
