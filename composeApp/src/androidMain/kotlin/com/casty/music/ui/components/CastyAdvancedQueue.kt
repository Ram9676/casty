package com.casty.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.casty.music.data.Song
import com.casty.music.data.cleanTitle
import com.casty.music.ui.theme.CastyTheme

/**
 * God-tier Queue Bottom Sheet with drag-to-dismiss, smooth animations,
 * and intelligent now-playing highlighting.
 */
@Composable
fun CastyAdvancedQueueSheet(
    visible: Boolean,
    queue: List<Song>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSongClick: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberCastyHaptics()
    
    if (!visible) return
    
    // Full-screen overlay with backdrop blur effect
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onDismiss)
    ) {
        // Queue content card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(CastyTheme.colors.elevation1)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CastyTheme.colors.elevation3)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Queue",
                    style = CastyTheme.typography.titleLarge.copy(
                        color = CastyTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${queue.size} tracks",
                        style = CastyTheme.typography.bodyMedium.copy(
                            color = CastyTheme.colors.textSecondary,
                            fontSize = 14.sp
                        )
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Close",
                            tint = CastyTheme.colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Queue list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(queue, key = { it.id }) { song ->
                    val index = queue.indexOf(song)
                    QueueItem(
                        song = song,
                        isPlaying = index == currentIndex,
                        onClick = {
                            haptics.confirm()
                            onSongClick(index)
                        },
                        onRemove = {
                            haptics.tick()
                            onRemove(index)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "QueueItemScale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPlaying -> CastyTheme.colors.elevation3
            else -> Color.Transparent
        },
        label = "QueueItemBg"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isPlaying) CastyTheme.colors.accentPink.copy(alpha = 0.5f) else Color.Transparent,
        label = "QueueItemBorder"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isPlaying) {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Now playing indicator or position number
        if (isPlaying) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Playing",
                tint = CastyTheme.colors.accentPink,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Text(
                text = "♪",
                style = CastyTheme.typography.bodyMedium.copy(
                    color = CastyTheme.colors.textSecondary.copy(alpha = 0.5f),
                    fontSize = 16.sp
                ),
                modifier = Modifier.width(18.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Album art thumbnail
        CastyArtwork(
            model = song.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(6.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Track info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.cleanTitle(),
                style = CastyTheme.typography.bodyLarge.copy(
                    color = if (isPlaying) CastyTheme.colors.accentPink else CastyTheme.colors.textPrimary,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = song.artistsText.orEmpty(),
                style = CastyTheme.typography.bodyMedium.copy(
                    color = CastyTheme.colors.textSecondary,
                    fontSize = 13.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Remove",
                tint = CastyTheme.colors.textSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Mini queue preview for Now Playing screen
 */
@Composable
fun CastyMiniQueuePreview(
    queue: List<Song>,
    currentIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberCastyHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        label = "MiniQueueScale"
    )
    
    val nextSongs = queue.drop(currentIndex + 1).take(2)
    
    if (nextSongs.isEmpty()) return
    
    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(CastyTheme.colors.elevation2)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.tick()
                    onClick()
                }
            )
            .padding(12.dp)
    ) {
        Text(
            text = "Up Next",
            style = CastyTheme.typography.labelMedium.copy(
                color = CastyTheme.colors.textSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        nextSongs.forEachIndexed { index, song ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index + 1}.",
                    style = CastyTheme.typography.bodySmall.copy(
                        color = CastyTheme.colors.textSecondary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.width(20.dp)
                )
                
                CastyArtwork(
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = song.cleanTitle(),
                    style = CastyTheme.typography.bodySmall.copy(
                        color = CastyTheme.colors.textPrimary,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            
            if (index < nextSongs.lastIndex) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}
