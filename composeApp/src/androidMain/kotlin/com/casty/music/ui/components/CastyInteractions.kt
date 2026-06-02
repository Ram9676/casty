package com.casty.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role

/**
 * God-level premium interaction system for Casty.
 * 
 * Beautiful, advanced, future-feeling touches:
 * - Synced visual + haptic feedback
 * - Springy scale + subtle lift
 * - Context-aware haptics
 * - Long-press previews ready
 * - Zero jank, delightful on every touch
 */

@Composable
fun rememberCastyInteractionSource(): MutableInteractionSource {
    return remember { MutableInteractionSource() }
}

/**
 * Premium clickable with beautiful spring scale, haptic, and micro-lift.
 * Use this everywhere instead of raw .clickable for consistent god-tier feel.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.castyClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    hapticType: HapticType = HapticType.Tick,
    enabled: Boolean = true,
    role: Role? = null
): Modifier {
    val haptics = rememberCastyHaptics()
    val interactionSource = rememberCastyInteractionSource()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "premium-press-scale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            // Subtle premium lift on press
            translationY = if (isPressed) 1f else 0f
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null, // We control the feedback ourselves for premium control
            enabled = enabled,
            role = role,
            onClick = {
                when (hapticType) {
                    HapticType.Tick -> haptics.tick()
                    HapticType.Confirm -> haptics.confirm()
                    HapticType.Heavy -> haptics.heavyImpact()
                    HapticType.Success -> haptics.success()
                    HapticType.Light -> haptics.lightTick()
                    HapticType.Hero -> haptics.heroPress()
                }
                onClick()
            },
            onLongClick = onLongClick?.let {
                {
                    haptics.longPress()
                    it()
                }
            }
        )
}

enum class HapticType {
    Tick, Confirm, Heavy, Success, Light, Hero
}

/**
 * Premium IconButton wrapper with beautiful interactions.
 */
@Composable
fun CastyIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hapticType: HapticType = HapticType.Tick,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .castyClickable(
                onClick = onClick,
                hapticType = hapticType
            ),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        content()
    }
}

/**
 * Hero-level press for big album art / main play button.
 * Stronger scale + premium haptic.
 */
@Composable
fun Modifier.castyHeroPress(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier {
    val haptics = rememberCastyHaptics()
    val interactionSource = rememberCastyInteractionSource()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "hero-press"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    haptics.heroPress()
                    onClick()
                },
                onLongPress = onLongClick?.let { { haptics.longPress(); it() } }
            )
        }
}