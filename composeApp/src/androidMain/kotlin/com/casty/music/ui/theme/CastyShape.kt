package com.casty.music.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
class CastyShapes(
    val extraSmall: RoundedCornerShape = RoundedCornerShape(4.dp),
    val small: RoundedCornerShape = RoundedCornerShape(8.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(12.dp),
    val large: RoundedCornerShape = RoundedCornerShape(24.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(500.dp),
    val circle: RoundedCornerShape = RoundedCornerShape(50),
    // God-level advanced crops for future premium feel (soft organic edges, asymmetric for depth)
    val softLarge: RoundedCornerShape = RoundedCornerShape(28.dp),
    val premiumCard: RoundedCornerShape = RoundedCornerShape(
        topStart = 20.dp, topEnd = 20.dp, bottomStart = 28.dp, bottomEnd = 28.dp // Asymmetric for "alive" depth
    ),
    val superPill: RoundedCornerShape = RoundedCornerShape(999.dp)
)

object CastyMotion {
    // Easing curves matching premium UI transitions
    val StandardEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f) // Swift deceleration
    val EmphasizedEasing: Easing = CubicBezierEasing(0.3f, 0.0f, 0.0f, 1.0f)

    // Duration constants
    const val ShortDuration = 150
    const val MediumDuration = 300
    const val LongDuration = 500
}

val LocalCastyShapes = staticCompositionLocalOf { CastyShapes() }
