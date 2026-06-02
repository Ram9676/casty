package com.casty.music.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.casty.music.data.cleanTitle
import com.casty.music.ui.components.HapticType
import com.casty.music.ui.components.castyClickable
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(64.dp)
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
            .clip(CastyTheme.shapes.premiumCard)  // Advanced premium crop for best edge/interaction feel
            .background(CastyTheme.colors.elevation2)
            .castyClickable(
                onClick = { onExpand() },
                hapticType = HapticType.Tick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            CastyArtwork(
                model = currentSong.thumbnailUrl,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp),
                shape = RoundedCornerShape(4.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Artist
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Subtle Casty branding in the mini player for consistent identity
                Text(
                    text = "Casty • ${currentSong.cleanTitle()}",
                    style = CastyTheme.typography.bodyLarge.copy(
                        color = CastyTheme.colors.textPrimary,
                        fontSize = 14.sp
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
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
            }

            // Like Heart Button
            IconButton(
                onClick = {
                    haptics.confirm()
                    viewModel.onEvent(PlayerEvent.ToggleLike)
                }
            ) {
                Icon(
                    painter = painterResource(
                        id = if (uiState.isLiked) com.casty.music.R.drawable.heart else com.casty.music.R.drawable.heart_outline
                    ),
                    contentDescription = "Like",
                    tint = if (uiState.isLiked) CastyTheme.colors.accentPink else CastyTheme.colors.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Play / Pause Button
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
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Mini progress bar at the bottom of the dock
        val progress = if (uiState.durationMs > 0) {
            uiState.progressMs.toFloat() / uiState.durationMs.toFloat()
        } else {
            0f
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(2.dp)
                .background(CastyTheme.colors.accentPink)
                .align(Alignment.BottomStart)
        )
    }
}
