package com.casty.music.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.casty.music.R

val SatoshiFontFamily = FontFamily(
    Font(R.font.satoshi_w300, FontWeight.Light),
    Font(R.font.satoshi_w400, FontWeight.Normal),
    Font(R.font.satoshi_w500, FontWeight.Medium),
    Font(R.font.satoshi_w700, FontWeight.Bold)
)

private val defaultPlatformStyle = PlatformTextStyle(includeFontPadding = false)
private val defaultLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

@Immutable
class CastyTypography(
    val displayLarge: TextStyle = TextStyle(
        fontFamily = SatoshiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp,
        platformStyle = defaultPlatformStyle,
        lineHeightStyle = defaultLineHeightStyle
    ),
    val displayMedium: TextStyle = TextStyle(
        fontFamily = SatoshiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
        platformStyle = defaultPlatformStyle,
        lineHeightStyle = defaultLineHeightStyle
    ),
    val displaySmall: TextStyle = TextStyle(
        fontFamily = SatoshiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
        platformStyle = defaultPlatformStyle,
        lineHeightStyle = defaultLineHeightStyle
    ),
    val titleLarge: TextStyle = TextStyle(
        fontFamily = SatoshiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
        platformStyle = defaultPlatformStyle,
        lineHeightStyle = defaultLineHeightStyle
    ),
    val titleMedium: TextStyle = TextStyle(
        fontFamily = SatoshiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        platformStyle = defaultPlatformStyle,
        lineHeightStyle = defaultLineHeightStyle
    ),
    val titleSmall: TextStyle = TextStyle(
        fontFamily = SatoshiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        platformStyle = defaultPlatformStyle,
        lineHeightStyle = defaultLineHeightStyle
    ),
    val bodyLarge: TextStyle = TextStyle(
        fontFamily = SatoshiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        platformStyle = defaultPlatformStyle,
        lineHeightStyle = defaultLineHeightStyle
    ),
    val bodyMedium: TextStyle = TextStyle(
        fontFamily = SatoshiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        platformStyle = defaultPlatformStyle,
        lineHeightStyle = defaultLineHeightStyle
    ),
    val bodySmall: TextStyle = TextStyle(
        fontFamily = SatoshiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
        platformStyle = defaultPlatformStyle,
        lineHeightStyle = defaultLineHeightStyle
    ),
    val labelLarge: TextStyle = TextStyle(
        fontFamily = SatoshiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
        platformStyle = defaultPlatformStyle,
        lineHeightStyle = defaultLineHeightStyle
    ),
    val labelMedium: TextStyle = TextStyle(
        fontFamily = SatoshiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
        platformStyle = defaultPlatformStyle,
        lineHeightStyle = defaultLineHeightStyle
    ),
    val labelSmall: TextStyle = TextStyle(
        fontFamily = SatoshiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp,
        platformStyle = defaultPlatformStyle,
        lineHeightStyle = defaultLineHeightStyle
    )
)

val LocalCastyTypography = staticCompositionLocalOf { CastyTypography() }
