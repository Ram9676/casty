package com.casty.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.casty.music.ui.theme.CastyTheme
import kotlin.math.sin

/**
 * Premium Casty Visualizer - beautiful, smooth, pink-themed reactive bars.
 * High-quality Compose implementation with organic movement and glow.
 * Designed to feel expensive and alive, matching the overall Casty premium experience.
 */
@Composable
fun CastyVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    intensity: Float = 1f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val pink = CastyTheme.colors.accentPink

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val barCount = 36
        val barWidth = width / (barCount * 1.75f)
        val spacing = barWidth * 0.35f

        for (i in 0 until barCount) {
            val x = (i + 0.5f) * (width / barCount)

            // Layered sine waves for rich, organic, premium movement
            val wave1 = sin((i * 0.65f) + phase * 1.8f)
            val wave2 = sin((i * 1.35f) + phase * 2.4f) * 0.6f
            val wave3 = sin((i * 0.4f) + phase * 1.1f) * 0.4f

            val combined = (wave1 + wave2 + wave3) * 0.5f

            val base = if (isPlaying) 0.22f else 0.06f
            val amp = base + (combined * 0.48f * intensity)

            val barHeight = (height * amp.coerceIn(0.04f, 0.96f))

            // Soft pink glow + core bar for luxurious feel
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        pink.copy(alpha = 0.35f),
                        pink.copy(alpha = 0.95f),
                        pink.copy(alpha = 0.35f)
                    )
                ),
                start = Offset(x, centerY - barHeight / 2f),
                end = Offset(x, centerY + barHeight / 2f),
                strokeWidth = barWidth * 1.15f,
                cap = StrokeCap.Round
            )

            // Crisp inner core
            drawLine(
                color = pink.copy(alpha = 0.95f),
                start = Offset(x, centerY - barHeight / 2.2f),
                end = Offset(x, centerY + barHeight / 2.2f),
                strokeWidth = barWidth * 0.55f,
                cap = StrokeCap.Round
            )
        }
    }
}