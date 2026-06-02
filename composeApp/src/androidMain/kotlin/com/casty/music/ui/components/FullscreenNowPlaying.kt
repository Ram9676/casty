package com.casty.music.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.casty.music.ui.components.HapticType
import com.casty.music.ui.components.castyClickable
import com.casty.music.ui.theme.CastyTheme.shapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.casty.music.data.cleanTitle
import com.casty.music.ui.components.CastyRealTimeVisualizer
import com.casty.music.ui.components.CastyVisualizer
import com.casty.music.ui.theme.CastyTheme
import com.casty.music.viewmodel.CastyPlayerViewModel
import com.casty.music.viewmodel.PlayerEvent

@Composable
fun FullscreenNowPlaying(
    viewModel: CastyPlayerViewModel,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSong = uiState.currentSong ?: return

    var showQueueSheet by remember { mutableStateOf(false) }
    var showLyricsPanel by remember { mutableStateOf(false) }
    var showTrackOptions by remember { mutableStateOf(false) }

    // Breathing scale animation for album art when playing
    val infiniteTransition = rememberInfiniteTransition(label = "ArtBreathing")
    val artScale by if (uiState.isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ScaleValue"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, excludeFromSystemGesture = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Blurred Art Background
            CastyArtwork(
                model = currentSong.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                artworkSizePx = 720,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(40.dp)
                    .scale(1.2f),
                shape = RoundedCornerShape(0.dp)
            )

            // Scrim to darken the blurred background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Content Column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.castyClickable(
                            onClick = onDismiss,
                            hapticType = HapticType.Tick
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = CastyTheme.colors.textPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Casty branding - subtle premium touch
                        Text(
                            text = "CASTY",
                            style = CastyTheme.typography.labelSmall.copy(
                                color = CastyTheme.colors.accentPink,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        )
                        Text(
                            text = "PLAYING FROM PLAYLIST",
                            style = CastyTheme.typography.labelSmall.copy(
                                color = CastyTheme.colors.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = currentSong.artistsText.orEmpty(),
                            style = CastyTheme.typography.bodyMedium.copy(
                                color = CastyTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    IconButton(
                        onClick = { showTrackOptions = true },
                        modifier = Modifier.castyClickable(
                            onClick = { showTrackOptions = true },
                            hapticType = HapticType.Tick
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = CastyTheme.colors.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Album Art Box - Fully adaptive for every phone, foldable, tablet
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val artSize = (maxWidth * 0.72f).coerceIn(220.dp, 340.dp)  // Smart adaptive sizing
                    // Advanced premium crop + Casty pink glow for god-tier album art edges and interaction feel
                    Box(
                        modifier = Modifier
                            .size(artSize + 20.dp)
                            .graphicsLayer {
                                scaleX = artScale
                                scaleY = artScale
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Pink glow edge effect for advanced "future crop" luxury
                        Box(
                            modifier = Modifier
                                .size(artSize + 30.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            CastyTheme.colors.accentPink.copy(alpha = 0.25f),
                                            Color.Transparent
                                        ),
                                        radius = (artSize.value / 2) + 15
                                    )
                                )
                                .clip(CastyTheme.shapes.softLarge)
                        )
                        CastyArtwork(
                            model = currentSong.thumbnailUrl,
                            contentDescription = "Album Art",
                            contentScale = ContentScale.Crop,
                            artworkSizePx = 720,
                            modifier = Modifier
                                .size(artSize)
                                .clip(CastyTheme.shapes.premiumCard),  // Asymmetric advanced crop for depth
                            shape = CastyTheme.shapes.premiumCard
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Premium Casty Visualizers (world-class experience)
                // 1. Beautiful procedural version (always works)
                CastyVisualizer(
                    isPlaying = uiState.isPlaying,
                    intensity = if (uiState.isPlaying) 1.05f else 0.7f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                )

                // Real-time visualizer temporarily disabled to fix immediate crash after logo.
                // We will re-enable it with more testing.
                // if (uiState.currentMediaItem != null) {
                //     val audioSessionId by playerViewModel.audioSessionId.collectAsStateWithLifecycle()
                //     if (audioSessionId != 0) {
                //         CastyRealTimeVisualizer(...)
                //     }
                // }

                Spacer(modifier = Modifier.height(12.dp))

                // Track Info & Heart Like Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong.cleanTitle(),
                            style = CastyTheme.typography.displaySmall.copy(
                                color = CastyTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.isHighQuality) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            CastyTheme.colors.elevation3,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "HQ",
                                        style = CastyTheme.typography.labelSmall.copy(
                                            color = CastyTheme.colors.accentPink,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = uiState.playbackError ?: currentSong.artistsText.orEmpty(),
                                style = CastyTheme.typography.bodyLarge.copy(color = CastyTheme.colors.textSecondary),
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.onEvent(PlayerEvent.ToggleLike) }
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (uiState.isLiked) com.casty.music.R.drawable.heart else com.casty.music.R.drawable.heart_outline
                            ),
                            contentDescription = "Like",
                            tint = if (uiState.isLiked) CastyTheme.colors.accentPink else CastyTheme.colors.textSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Playback Controls + Slider
                PlayerControls(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Utilities Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lyrics Toggle Button
                    IconButton(onClick = { showLyricsPanel = true }) {
                        Icon(
                            painter = painterResource(id = com.casty.music.R.drawable.text),
                            contentDescription = "Lyrics",
                            tint = CastyTheme.colors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Queue List
                    IconButton(onClick = { showQueueSheet = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Queue",
                            tint = CastyTheme.colors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Sub Sheets
        QueueBottomSheet(
            visible = showQueueSheet,
            onDismiss = { showQueueSheet = false }
        )

        TrackOptionsSheet(
            visible = showTrackOptions,
            song = currentSong,
            onDismiss = { showTrackOptions = false }
        )

        if (showLyricsPanel) {
            Popup(
                onDismissRequest = { showLyricsPanel = false },
                properties = PopupProperties(focusable = true)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(0.9f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lyrics",
                                style = CastyTheme.typography.titleLarge.copy(color = CastyTheme.colors.textPrimary)
                            )
                            Text(
                                text = "Close",
                                style = CastyTheme.typography.bodyLarge.copy(color = CastyTheme.colors.textSecondary),
                                modifier = Modifier.clickable { showLyricsPanel = false }
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            LyricsPanel()
                        }
                    }
                }
            }
        }
    }
}
