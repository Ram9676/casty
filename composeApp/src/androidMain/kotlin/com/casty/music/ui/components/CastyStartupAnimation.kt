package com.casty.music.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.casty.music.R
import com.casty.music.ui.theme.CastyTheme
import kotlinx.coroutines.delay

@Composable
fun CastyStartupAnimation(
    visible: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    var entered by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(420)
        entered = false
        delay(160)
        onFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "CastyStartupLoop")
    val beat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "StartupBeat",
    )

    val screenAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = if (entered) 120 else 160, easing = FastOutSlowInEasing),
        label = "StartupScreenAlpha",
    )
    val logoScale by animateFloatAsState(
        targetValue = if (entered) 1f + beat * 0.03f else 1.05f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "StartupLogoScale",
    )
    val wordmarkAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 220, delayMillis = if (entered) 60 else 0, easing = FastOutSlowInEasing),
        label = "StartupWordmarkAlpha",
    )
    val wordmarkOffset by animateFloatAsState(
        targetValue = if (entered) 0f else -8f,
        animationSpec = tween(durationMillis = 220, delayMillis = if (entered) 60 else 0, easing = FastOutSlowInEasing),
        label = "StartupWordmarkOffset",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(screenAlpha)
            .background(CastyTheme.colors.systemCanvas),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Clean, modern, premium Casty logo animation (simple, elegant, and calm)
            Image(
                painter = painterResource(id = R.drawable.casty_logo),
                contentDescription = "Casty",
                modifier = Modifier
                    .size(108.dp)
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                    },
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Casty",
                style = CastyTheme.typography.displayMedium.copy(
                    color = CastyTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                ),
                modifier = Modifier
                    .alpha(wordmarkAlpha)
                    .offset(y = wordmarkOffset.dp),
            )
        }
    }
}
