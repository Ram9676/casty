package com.casty.music.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Stable
class CastyColors(
    systemCanvas: Color,
    elevation1: Color,
    elevation2: Color,
    elevation3: Color,
    elevation4: Color,
    borderInactive: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentPink: Color,
    pinkHover: Color,
    pinkPressed: Color,
    verifiedBlue: Color
) {
    var systemCanvas by mutableStateOf(systemCanvas)
        private set
    var elevation1 by mutableStateOf(elevation1)
        private set
    var elevation2 by mutableStateOf(elevation2)
        private set
    var elevation3 by mutableStateOf(elevation3)
        private set
    var elevation4 by mutableStateOf(elevation4)
        private set
    var borderInactive by mutableStateOf(borderInactive)
        private set
    var textPrimary by mutableStateOf(textPrimary)
        private set
    var textSecondary by mutableStateOf(textSecondary)
        private set
    var accentPink by mutableStateOf(accentPink)
        private set
    var pinkHover by mutableStateOf(pinkHover)
        private set
    var pinkPressed by mutableStateOf(pinkPressed)
        private set
    var verifiedBlue by mutableStateOf(verifiedBlue)
        private set

    fun copy(
        systemCanvas: Color = this.systemCanvas,
        elevation1: Color = this.elevation1,
        elevation2: Color = this.elevation2,
        elevation3: Color = this.elevation3,
        elevation4: Color = this.elevation4,
        borderInactive: Color = this.borderInactive,
        textPrimary: Color = this.textPrimary,
        textSecondary: Color = this.textSecondary,
        accentPink: Color = this.accentPink,
        pinkHover: Color = this.pinkHover,
        pinkPressed: Color = this.pinkPressed,
        verifiedBlue: Color = this.verifiedBlue
    ) = CastyColors(
        systemCanvas, elevation1, elevation2, elevation3, elevation4,
        borderInactive, textPrimary, textSecondary, accentPink,
        pinkHover, pinkPressed, verifiedBlue
    )

    fun updateColorsFrom(other: CastyColors) {
        systemCanvas = other.systemCanvas
        elevation1 = other.elevation1
        elevation2 = other.elevation2
        elevation3 = other.elevation3
        elevation4 = other.elevation4
        borderInactive = other.borderInactive
        textPrimary = other.textPrimary
        textSecondary = other.textSecondary
        accentPink = other.accentPink
        pinkHover = other.pinkHover
        pinkPressed = other.pinkPressed
        verifiedBlue = other.verifiedBlue
    }
}

val DarkColorPalette = CastyColors(
    systemCanvas = Color(0xFF000000),      // AMOLED True Black
    elevation1 = Color(0xFF121212),        // Dark Grey surface
    elevation2 = Color(0xFF181818),        // Slightly lighter surface
    elevation3 = Color(0xFF282828),        // Medium Grey surface
    elevation4 = Color(0xFF3E3E3E),        // Light Grey surface
    borderInactive = Color(0xFF292929),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB3B3B3),
    accentPink = Color(0xFFFF2D92),        // Casty Premium Pink (bold, vibrant, future branding)
    pinkHover = Color(0xFFFF5BA8),
    pinkPressed = Color(0xFFE91E63),
    verifiedBlue = Color(0xFF2E77D0)
)



val LocalCastyColors = staticCompositionLocalOf { DarkColorPalette }

object CastyTheme {
    val colors: CastyColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCastyColors.current

    val typography: CastyTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalCastyTypography.current

    val shapes: CastyShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalCastyShapes.current
}

@Composable
fun CastyTheme(
    colors: CastyColors = DarkColorPalette,
    typography: CastyTypography = CastyTypography(),
    shapes: CastyShapes = CastyShapes(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalCastyColors provides colors,
        LocalCastyTypography provides typography,
        LocalCastyShapes provides shapes,
        content = content
    )
}

