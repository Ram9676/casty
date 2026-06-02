package com.casty.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.casty.music.data.cleanTitle
import com.casty.music.ui.theme.CastyTheme
import com.casty.music.viewmodel.CastyPlayerViewModel
import com.casty.music.viewmodel.PlayerEvent
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayer(
    viewModel: CastyPlayerViewModel,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSong = uiState.currentSong

    if (currentSong == null) return

    val haptics = rememberCastyHaptics()
    var offsetX by remember { mutableStateOf(0f) }
    
    // Magical pulsing background animation
    val infiniteTransition = rememberInfiniteTransition(label = "miniPlayerPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    // Progress bar animation
    val animatedProgress by animateFloatAsState(
        targetValue = if (uiState.durationMs > 0) {
            uiState.progressMs.toFloat() / uiState.durationMs.toFloat()
        } else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(72.dp)
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetX > 150) {
                            haptics.gestureStart()
                            viewModel.onEvent(PlayerEvent.SkipPrev)
                        } else if (offsetX < -150) {
                            haptics.gestureStart()
                            viewModel.onEvent(PlayerEvent.SkipNext)
                        }
                        offsetX = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                    }
                )
            }
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        CastyTheme.colors.elevation2.copy(alpha = 0.9f),
                        CastyTheme.colors.elevation2.copy(alpha = 0.7f)
                    )
                )
            )
            .castyClickable(
                onClick = { 
                    haptics.confirm()
                    onExpand() 
                },
                hapticType = HapticType.Tick
            )
    ) {
        // Animated background glow when playing
        if (uiState.isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CastyTheme.colors.accentPink.copy(alpha = pulseAlpha * 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated Album Art with rotation when playing
            Box {
                CastyArtwork(
                    model = currentSong.thumbnailUrl,
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                )
                
                // Playing indicator ring
                if (uiState.isPlaying) {
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 3000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rotation"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.sweepGradient(
                                    colors = listOf(
                                        CastyTheme.colors.accentPink,
                                        CastyTheme.colors.accentBlue,
                                        CastyTheme.colors.accentPink
                                    )
                                )
                            )
                            .rotate(rotation)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title & Artist with marquee effect
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "Casty • ${currentSong.cleanTitle()}",
                    style = CastyTheme.typography.bodyLarge.copy(
                        color = CastyTheme.colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
                Text(
                    text = uiState.playbackError ?: currentSong.artistsText.orEmpty(),
                    style = CastyTheme.typography.bodyMedium.copy(
                        color = if (uiState.playbackError == null) {
                            CastyTheme.colors.textSecondary
                        } else {
                            CastyTheme.colors.accentPink
                        },
                        fontSize = 13.sp
                    ),
                    maxLines = 1
                )
            }

            // Like Heart Button with scale animation
            var isLikedAnimated by remember { mutableStateOf(uiState.isLiked) }
            IconButton(
                onClick = {
                    haptics.confirm()
                    isLikedAnimated = !isLikedAnimated
                    viewModel.onEvent(PlayerEvent.ToggleLike)
                }
            ) {
                Icon(
                    painter = painterResource(
                        id = if (uiState.isLiked) com.casty.music.R.drawable.heart else com.casty.music.R.drawable.heart_outline
                    ),
                    contentDescription = "Like",
                    tint = if (uiState.isLiked) CastyTheme.colors.accentPink else CastyTheme.colors.textSecondary,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Play / Pause Button with morphing animation
            IconButton(
                onClick = {
                    haptics.confirm()
                    viewModel.onEvent(PlayerEvent.PlayPause)
                }
            ) {
                Icon(
                    painter = painterResource(
                        id = if (uiState.isPlaying) com.casty.music.R.drawable.pause else com.casty.music.R.drawable.play
                    ),
                    contentDescription = "Play/Pause",
                    tint = CastyTheme.colors.textPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Animated progress bar at the bottom with gradient
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(CastyTheme.colors.elevation1)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                CastyTheme.colors.accentBlue,
                                CastyTheme.colors.accentPink,
                                CastyTheme.colors.accentPurple
                            )
                        )
                    )
            )
        }
    }
}
