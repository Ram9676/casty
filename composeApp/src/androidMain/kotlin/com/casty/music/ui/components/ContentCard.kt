package com.casty.music.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.casty.music.ui.theme.CastyTheme
import com.casty.music.ui.theme.CastyTheme.shapes

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContentCard(
    title: String,
    subtitle: String,
    imageUrl: String?,
    onClick: () -> Unit,
    onPlayClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    fillParentWidth: Boolean = false,
    artworkSize: Dp = 124.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = rememberCastyHaptics()
    // God-level advanced crop using Casty premium shapes for luxurious edges on all devices
    val cardShape = CastyTheme.shapes.premiumCard
    val cardModifier = if (fillParentWidth) {
        modifier.fillMaxWidth()
    } else {
        modifier.width(140.dp)
    }
    val artworkModifier = if (fillParentWidth) {
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    } else {
        Modifier.size(artworkSize)
    }
    val clickableModifier = if (onLongClick == null) {
        cardModifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                haptics.tick()
                onClick()
            }
        )
    } else {
        cardModifier.combinedClickable(
            onClick = {
                haptics.tick()
                onClick()
            },
            onLongClick = {
                haptics.gestureStart()
                onLongClick()
            }
        )
    }

    Column(
        modifier = clickableModifier
            .clip(cardShape)
            .padding(8.dp)
    ) {
        Box(
            modifier = artworkModifier
                .clip(cardShape)
                .background(CastyTheme.colors.elevation2)
        ) {
            CastyArtwork(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                shape = cardShape
            )

            // Hover play button overlay
            if (onPlayClick != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CastyTheme.colors.accentPink)
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 8.dp, end = 8.dp)
                        .clickable {
                            haptics.confirm()
                            onPlayClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = com.casty.music.R.drawable.play),
                        contentDescription = "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = title,
            style = CastyTheme.typography.bodyLarge.copy(color = CastyTheme.colors.textPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Subtitle
        Text(
            text = subtitle,
            style = CastyTheme.typography.bodyMedium.copy(
                color = CastyTheme.colors.textSecondary,
                fontSize = 12.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
