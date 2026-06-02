package com.casty.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.casty.music.ui.components.castyHeroPress
import com.casty.music.ui.theme.CastyTheme
import com.casty.music.viewmodel.PlayerEvent
import com.casty.music.viewmodel.PlayerUiState
import java.util.Locale

@Composable
fun PlayerControls(
    uiState: PlayerUiState,
    onEvent: (PlayerEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberCastyHaptics()
    var sliderValue by remember(uiState.progressMs) { mutableFloatStateOf(uiState.progressMs.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress Slider
        val duration = uiState.durationMs.toFloat().coerceAtLeast(1f)
        
        Slider(
            value = if (isDragging) sliderValue else uiState.progressMs.toFloat(),
            onValueChange = {
                isDragging = true
                sliderValue = it
                haptic.textureScrub()
            },
            onValueChangeFinished = {
                isDragging = false
                onEvent(PlayerEvent.SeekTo(sliderValue.toLong()))
            },
            valueRange = 0f..duration,
            colors = SliderDefaults.colors(
                thumbColor = CastyTheme.colors.textPrimary,
                activeTrackColor = CastyTheme.colors.textPrimary,
                inactiveTrackColor = CastyTheme.colors.elevation3
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Time labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val currentPos = if (isDragging) sliderValue.toLong() else uiState.progressMs
            Text(
                text = formatTime(currentPos),
                style = CastyTheme.typography.bodySmall.copy(color = CastyTheme.colors.textSecondary)
            )
            Text(
                text = formatTime(uiState.durationMs),
                style = CastyTheme.typography.bodySmall.copy(color = CastyTheme.colors.textSecondary)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Playback buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(
                onClick = {
                    haptic.confirm()
                    onEvent(PlayerEvent.ToggleShuffle)
                }
            ) {
                Icon(
                    painter = painterResource(id = com.casty.music.R.drawable.shuffle),
                    contentDescription = "Shuffle",
                    tint = if (uiState.isShuffleEnabled) CastyTheme.colors.accentPink else CastyTheme.colors.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Skip Back
            IconButton(
                onClick = {
                    haptic.tick()
                    onEvent(PlayerEvent.SkipPrev)
                }
            ) {
                Icon(
                    painter = painterResource(id = com.casty.music.R.drawable.play_skip_back),
                    contentDescription = "Previous",
                    tint = CastyTheme.colors.textPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Premium Hero Play/Pause — god-level delightful press
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .castyHeroPress(
                        onClick = { onEvent(PlayerEvent.PlayPause) }
                    )
                    .clip(CircleShape)
                    .background(CastyTheme.colors.textPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = if (uiState.isPlaying) com.casty.music.R.drawable.pause else com.casty.music.R.drawable.play
                    ),
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Skip Forward
            IconButton(
                onClick = {
                    haptic.tick()
                    onEvent(PlayerEvent.SkipNext)
                }
            ) {
                Icon(
                    painter = painterResource(id = com.casty.music.R.drawable.play_skip_forward),
                    contentDescription = "Next",
                    tint = CastyTheme.colors.textPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Repeat
            IconButton(
                onClick = {
                    haptic.confirm()
                    // Cycle repeat mode: 0 (none) -> 2 (all) -> 1 (one) -> 0 (none)
                    val nextMode = when (uiState.repeatMode) {
                        0 -> 2 // Repeat all
                        2 -> 1 // Repeat one
                        else -> 0 // None
                    }
                    onEvent(PlayerEvent.SetRepeatMode(nextMode))
                }
            ) {
                Icon(
                    painter = painterResource(
                        id = if (uiState.repeatMode == 1) com.casty.music.R.drawable.repeatone else com.casty.music.R.drawable.repeat
                    ),
                    contentDescription = "Repeat",
                    tint = if (uiState.repeatMode != 0) CastyTheme.colors.accentPink else CastyTheme.colors.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
}
