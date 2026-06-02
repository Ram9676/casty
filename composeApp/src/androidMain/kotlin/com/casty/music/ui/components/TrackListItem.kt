package com.casty.music.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    modifier: Modifier = Modifier
) {
    val haptics = rememberCastyHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Scale on press: 0.95f, normal: 1f
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        label = "PressScale"
    )

    Row(
        modifier = modifier
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Thumbnail/Cover Art
        CastyArtwork(
            model = song.thumbnailUrl,
            contentDescription = "Track Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp),
            shape = RoundedCornerShape(4.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Info: Title & Subtitle (artist)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.cleanTitle(),
                style = CastyTheme.typography.bodyLarge.copy(color = CastyTheme.colors.textPrimary),
                maxLines = 1
            )
            Text(
                text = song.artistsText.orEmpty(),
                style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textSecondary),
                maxLines = 1
            )
        }

        // More options menu button
        if (onMenuClick != null) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    painter = painterResource(id = com.casty.music.R.drawable.ellipsis_vertical),
                    contentDescription = "Options",
                    tint = CastyTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
