package com.casty.music.ui.components

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.casty.music.ui.theme.CastyTheme
import com.casty.music.R

@Composable
fun CastyArtwork(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    contentScale: ContentScale = ContentScale.Crop,
    artworkSizePx: Int = 256,
    fallbackIconRes: Int = R.drawable.musical_notes
) {
    val context = LocalContext.current
    val normalizedModel = remember(model, artworkSizePx) {
        model.normalizedArtworkModel(artworkSizePx)
    }
    val imageRequest: ImageRequest? = remember(context, normalizedModel) {
        normalizedModel?.let {
            ImageRequest.Builder(context)
                .data(it)
                .size(artworkSizePx, artworkSizePx)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .build()
        }
    }
    var showFallback by remember(normalizedModel) { mutableStateOf(normalizedModel == null) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(CastyTheme.colors.elevation2),
        contentAlignment = Alignment.Center
    ) {
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = contentDescription,
                contentScale = contentScale,
                onSuccess = { showFallback = false },
                onError = { showFallback = true },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!showFallback && imageRequest != null && artworkSizePx >= 320) {
            ClassicMeshArtworkOverlay(modifier = Modifier.fillMaxSize())
        }

        if (showFallback) {
            Icon(
                painter = painterResource(id = fallbackIconRes),
                contentDescription = contentDescription,
                tint = CastyTheme.colors.textSecondary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

private fun Any?.normalizedArtworkModel(sizePx: Int): Any? = when (this) {
    null -> null
    is Uri -> toString().normalizedArtworkUrl(sizePx)
    is String -> normalizedArtworkUrl(sizePx)
    else -> this
}

private fun String.normalizedArtworkUrl(sizePx: Int): String? {
    val cleaned = trim().takeIf {
        it.isNotEmpty() && !it.equals("null", ignoreCase = true)
    } ?: return null

    val withScheme = when {
        cleaned.startsWith("//") -> "https:$cleaned"
        cleaned.startsWith("http://") -> cleaned.replaceFirst("http://", "https://")
        else -> cleaned
    }

    return when {
        withScheme.contains("googleusercontent.com") || withScheme.contains("ggpht.com") ->
            withScheme.resizeGoogleArtwork(sizePx)
        withScheme.contains("i.ytimg.com") ->
            withScheme.resizeYouTubeArtwork(sizePx)
        else -> withScheme
    }
}

private fun String.resizeGoogleArtwork(sizePx: Int): String {
    val size = sizePx.coerceIn(256, 1440)
    val explicitSize = Regex("w\\d+-h\\d+")
    if (contains(explicitSize)) {
        return replace(explicitSize, "w$size-h$size")
    }

    val baseUrl = split("=w", "=s", "=h", limit = 2).first()
    return if (contains("=w") && contains("-h")) {
        "$baseUrl=w$size-h$size-p-l90-rj"
    } else if (contains("yt3.ggpht.com")) {
        "$baseUrl=s$size-c-k-c0x00ffffff-no-rj"
    } else {
        "$baseUrl=w$size-h$size-p-l90-rj"
    }
}

private fun String.resizeYouTubeArtwork(sizePx: Int): String {
    // maxresdefault.jpg is missing for many videos and causes blank art; prefer hqdefault.
    if (!contains("ytimg.com")) return this
    return when {
        sizePx >= 480 -> {
            replace("mqdefault.jpg", "hqdefault.jpg")
                .replace("sddefault.jpg", "hqdefault.jpg")
                .replace("default.jpg", "hqdefault.jpg")
        }
        else -> this
    }
}

@Composable
private fun ClassicMeshArtworkOverlay(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    androidx.compose.ui.graphics.Color.Transparent,
                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.08f),
                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.2f),
                )
            )
        )

        val spacing = (size.minDimension / 18f).coerceIn(8f, 18f)
        val radius = (spacing / 7f).coerceIn(1.2f, 3.2f)
        val dotColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f)
        var rowIndex = 0
        var y = spacing * 0.65f

        while (y < size.height) {
            val horizontalOffset = if (rowIndex % 2 == 0) spacing * 0.45f else 0f
            var x = horizontalOffset
            while (x < size.width) {
                drawCircle(
                    color = dotColor,
                    radius = radius,
                    center = Offset(x, y),
                )
                x += spacing
            }
            y += spacing
            rowIndex += 1
        }
    }
}
