package com.casty.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.casty.music.ui.theme.CastyTheme
import com.casty.music.viewmodel.NowPlayingViewModel
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun QueueBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val haptics = rememberCastyHaptics()
    val scope = rememberCoroutineScope()

    var radioMessage by remember { mutableStateOf<String?>(null) }
    var playlistMessage by remember { mutableStateOf<String?>(null) }

    CastyBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .background(CastyTheme.colors.elevation1)
                .padding(horizontal = 16.dp)
        ) {
            // === PREMIUM HEADER (Spotify vibe) ===
            Column(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Queue",
                            style = CastyTheme.typography.titleLarge.copy(
                                color = CastyTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        val total = uiState.queue.size
                        val upcoming = (uiState.queue.size - uiState.currentQueueIndex - 1).coerceAtLeast(0)
                        Text(
                            text = "$total songs • $upcoming up next",
                            style = CastyTheme.typography.bodySmall.copy(color = CastyTheme.colors.textSecondary)
                        )
                    }

                    IconButton(onClick = {
                        haptics.tick()
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close queue", tint = CastyTheme.colors.textSecondary)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Quick action row (simplified for build stability + pink branding)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Shuffle",
                        color = CastyTheme.colors.accentPink,
                        modifier = Modifier
                            .clickable {
                                haptics.toggle(true)
                                viewModel.shuffleQueue()
                            }
                            .padding(8.dp)
                    )
                    Text(
                        "Radio",
                        color = CastyTheme.colors.accentPink,
                        modifier = Modifier
                            .clickable {
                                haptics.gestureStart()
                                viewModel.startSmartRadio { msg ->
                                    radioMessage = msg
                                    scope.launch {
                                        delay(2600)
                                        radioMessage = null
                                    }
                                }
                            }
                            .padding(8.dp)
                    )
                    Text(
                        "Save",
                        color = CastyTheme.colors.accentPink,
                        modifier = Modifier
                            .clickable {
                                haptics.confirm()
                                viewModel.saveQueueAsPlaylist("My Queue Mix") { msg ->
                                    playlistMessage = msg
                                    scope.launch {
                                        delay(2400)
                                        playlistMessage = null
                                    }
                                }
                            }
                            .padding(8.dp)
                    )
                    Text(
                        "Clear Up Next",
                        color = CastyTheme.colors.accentPink,
                        modifier = Modifier
                            .clickable {
                                haptics.reject()
                                viewModel.clearUpNext()
                            }
                            .padding(8.dp)
                    )
                }
            }

            AnimatedVisibility(visible = radioMessage != null) {
                RadioBanner(text = radioMessage.orEmpty()) { radioMessage = null }
            }
            AnimatedVisibility(visible = playlistMessage != null) {
                PlaylistBanner(text = playlistMessage.orEmpty()) { playlistMessage = null }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                val previousItems = uiState.queue.withIndex().filter { it.index < uiState.currentQueueIndex }
                val upcomingItems = uiState.queue.withIndex().filter { it.index > uiState.currentQueueIndex }

                // NOW PLAYING HERO
                item(key = "np-header") { SectionHeader("Now playing", 1) }

                if (uiState.currentQueueIndex in 0 until uiState.queue.size) {
                    val current = uiState.queue[uiState.currentQueueIndex]
                    item(key = "current-${current.mediaId}") {
                        NowPlayingHeroRow(item = current, onMoreClick = {})
                    }
                } else {
                    item { EmptyState("Nothing playing") }
                }

                // UP NEXT - Interactive core
                item(key = "up-header") {
                    Spacer(Modifier.height(16.dp))
                    SectionHeader("Up next", upcomingItems.size)
                }

                if (upcomingItems.isEmpty()) {
                    item(key = "up-empty") {
                        EmptyQueueState("Queue is looking fresh", "Add songs from anywhere in the app")
                    }
                } else {
                    itemsIndexed(
                        items = upcomingItems,
                        key = { _, indexed -> "up-${indexed.index}-${indexed.value.mediaId}" }
                    ) { _, indexed ->
                        val absoluteIndex = indexed.index
                        val mediaItem = indexed.value

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    haptics.reject()
                                    viewModel.removeQueueItem(absoluteIndex)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    Modifier.fillMaxSize().background(Color(0xFF3A1A1A)).padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) { Icon(Icons.Default.Delete, "Remove", tint = Color(0xFFFF6B6B)) }
                            },
                            enableDismissFromEndToStart = true,
                            enableDismissFromStartToEnd = false
                        ) {
                            PremiumQueueRow(
                                item = mediaItem,
                                index = absoluteIndex,
                                onPlay = { haptics.tick(); viewModel.playQueueItem(absoluteIndex) },
                                onRemove = { haptics.reject(); viewModel.removeQueueItem(absoluteIndex) },
                                onMoveUp = if (absoluteIndex > uiState.currentQueueIndex + 1) {
                                    { haptics.dragTick(); viewModel.reorderQueue(absoluteIndex, absoluteIndex - 1) }
                                } else null,
                                onMoveDown = if (absoluteIndex < uiState.queue.lastIndex) {
                                    { haptics.dragTick(); viewModel.reorderQueue(absoluteIndex, absoluteIndex + 1) }
                                } else null,
                                onLongPress = { haptics.longPress() }
                            )
                        }
                    }
                }

                if (previousItems.isNotEmpty()) {
                    item(key = "prev-header") {
                        Spacer(Modifier.height(20.dp))
                        SectionHeader("Previously played", previousItems.size)
                    }
                    itemsIndexed(
                        items = previousItems,
                        key = { _, indexed -> "prev-${indexed.index}-${indexed.value.mediaId}" }
                    ) { _, indexed ->
                        PreviouslyPlayedRow(item = indexed.value) {
                            haptics.tick()
                            viewModel.playQueueItem(indexed.index)
                        }
                    }
                }

                item(key = "enhance") {
                    Spacer(Modifier.height(28.dp))
                    EnhanceQueueFooter(
                        onStartRadio = {
                            haptics.gestureStart()
                            viewModel.startSmartRadio { msg ->
                                radioMessage = msg
                                scope.launch { delay(2800); radioMessage = null }
                            }
                        },
                        onAddMore = {
                            haptics.confirm()
                            radioMessage = "Open Search for more recommendations"
                            scope.launch { delay(1400); radioMessage = null }
                        }
                    )
                }
            }
        }
    }
}

// ==================== ULTRA PREMIUM SUB-COMPOSABLES ====================

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            title,
            style = CastyTheme.typography.labelLarge.copy(color = CastyTheme.colors.textSecondary, fontWeight = FontWeight.SemiBold)
        )
        Spacer(Modifier.width(8.dp))
        Surface(shape = CircleShape, color = CastyTheme.colors.elevation3, modifier = Modifier.size(18.dp)) {
            Box(contentAlignment = Alignment.Center) { Text("$count", style = CastyTheme.typography.labelSmall.copy(color = CastyTheme.colors.textSecondary)) }
        }
    }
}

@Composable
private fun ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(if (accent) CastyTheme.colors.accentPink.copy(0.15f) else CastyTheme.colors.elevation3, label = "pill")
    val fg = if (accent) CastyTheme.colors.accentPink else CastyTheme.colors.textPrimary

    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = bg, modifier = Modifier.height(36.dp)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = CastyTheme.typography.labelMedium.copy(color = fg, fontWeight = FontWeight.Medium))
        }
    }
}

@Composable
private fun NowPlayingHeroRow(item: androidx.media3.common.MediaItem, onMoreClick: () -> Unit) {
    val haptics = rememberCastyHaptics()
    Surface(shape = RoundedCornerShape(16.dp), color = CastyTheme.colors.elevation2, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                CastyArtwork(model = item.mediaMetadata.artworkUri, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)), shape = RoundedCornerShape(10.dp))
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(0.35f)), Alignment.Center) {
                    PlayingEqualizerIndicator()
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.mediaMetadata.title?.toString().orEmpty(),
                    style = CastyTheme.typography.titleMedium.copy(color = CastyTheme.colors.accentPink, fontWeight = FontWeight.SemiBold),
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(item.mediaMetadata.artist?.toString().orEmpty(),
                    style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textSecondary), maxLines = 1)
            }
            IconButton(onClick = { haptics.tick(); onMoreClick() }) {
                Icon(Icons.Default.MoreVert, "More", tint = CastyTheme.colors.textSecondary)
            }
        }
    }
}

@Composable
private fun PremiumQueueRow(
    item: androidx.media3.common.MediaItem,
    index: Int,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onLongPress: () -> Unit
) {
    val haptics = rememberCastyHaptics()
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, label = "row-press")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(CastyTheme.colors.elevation2)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onPlay() },
                    onLongPress = { pressed = true; onLongPress(); haptics.longPress(); pressed = false }
                )
            }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(28.dp).clip(CircleShape), Alignment.Center) {
            Icon(Icons.Default.Menu, "Reorder", tint = CastyTheme.colors.textSecondary.copy(0.7f), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        CastyArtwork(model = item.mediaMetadata.artworkUri, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)), shape = RoundedCornerShape(8.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.mediaMetadata.title?.toString().orEmpty(),
                style = CastyTheme.typography.bodyLarge.copy(color = CastyTheme.colors.textPrimary, fontWeight = FontWeight.Medium),
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(item.mediaMetadata.artist?.toString().orEmpty(),
                style = CastyTheme.typography.bodySmall.copy(color = CastyTheme.colors.textSecondary), maxLines = 1)
        }
        onMoveUp?.let {
            IconButton(onClick = it, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, null, tint = CastyTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
            }
        }
        onMoveDown?.let {
            IconButton(onClick = it, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, null, tint = CastyTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, null, tint = Color(0xFFFF6B6B).copy(0.9f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PreviouslyPlayedRow(item: androidx.media3.common.MediaItem, onReplay: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onReplay).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CastyArtwork(model = item.mediaMetadata.artworkUri, contentDescription = null,
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)), shape = RoundedCornerShape(6.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.mediaMetadata.title?.toString().orEmpty(), style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textSecondary), maxLines = 1)
            Text(item.mediaMetadata.artist?.toString().orEmpty(), style = CastyTheme.typography.bodySmall.copy(color = CastyTheme.colors.textSecondary.copy(0.6f)), maxLines = 1)
        }
        Icon(Icons.Default.PlayArrow, null, tint = CastyTheme.colors.textSecondary.copy(0.5f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PlayingEqualizerIndicator() {
    val infinite = rememberInfiniteTransition(label = "eq")
    val h1 by infinite.animateFloat(0.3f, 1f, infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h1")
    val h2 by infinite.animateFloat(0.6f, 0.95f, infiniteRepeatable(tween(280, easing = LinearEasing), RepeatMode.Reverse), label = "h2")
    val h3 by infinite.animateFloat(0.35f, 1f, infiniteRepeatable(tween(490, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h3")

    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.height(22.dp)) {
        listOf(h1, h2, h3).forEach { h ->
            Box(Modifier.width(3.dp).height(4.dp + (14.dp * h)).background(CastyTheme.colors.accentPink, RoundedCornerShape(50)))
        }
    }
}

@Composable
private fun RadioBanner(text: String, onDismiss: () -> Unit) {
    Surface(color = CastyTheme.colors.accentPink.copy(0.12f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("▶", color = CastyTheme.colors.accentPink)
            Spacer(Modifier.width(8.dp))
            Text(text, style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textPrimary), modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("OK", color = CastyTheme.colors.accentPink) }
        }
    }
}

@Composable
private fun PlaylistBanner(text: String, onDismiss: () -> Unit) {
    Surface(color = CastyTheme.colors.verifiedBlue.copy(0.12f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("✓", color = CastyTheme.colors.verifiedBlue)
            Spacer(Modifier.width(8.dp))
            Text(text, style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textPrimary), modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("Great", color = CastyTheme.colors.verifiedBlue) }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
        Text(text, style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textSecondary))
    }
}

@Composable
private fun EmptyQueueState(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.List, null, tint = CastyTheme.colors.textSecondary.copy(0.4f), modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = CastyTheme.typography.titleSmall.copy(color = CastyTheme.colors.textSecondary))
        Text(subtitle, style = CastyTheme.typography.bodySmall.copy(color = CastyTheme.colors.textSecondary.copy(0.7f)))
    }
}

@Composable
private fun EnhanceQueueFooter(onStartRadio: () -> Unit, onAddMore: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(CastyTheme.colors.elevation3.copy(0.6f), CastyTheme.colors.elevation2)))
            .padding(16.dp)
    ) {
        Text("Enhance your experience", style = CastyTheme.typography.labelLarge.copy(color = CastyTheme.colors.textSecondary))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onStartRadio,
                colors = ButtonDefaults.buttonColors(containerColor = CastyTheme.colors.accentPink),
                shape = RoundedCornerShape(50),
                modifier = Modifier.weight(1f)
            ) {
                Text("▶ Radio", fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(onClick = onAddMore, shape = RoundedCornerShape(50), modifier = Modifier.weight(1f)) {
                Text("Discover more")
            }
        }
        Text(
            "Radio uses YouTube Music recommendations • Seamless premium experience",
            style = CastyTheme.typography.labelSmall.copy(color = CastyTheme.colors.textSecondary.copy(0.5f)),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
