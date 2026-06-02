package com.casty.music.backend.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Size
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * God-level BitmapLoader for Media3.
 * 
 * Purpose: Make lock screen controls + notification always show beautiful artwork.
 * - Uses Coil3 (same as the rest of the app)
 * - Aggressively optimized sizes (prevents memory bloat / glitches)
 * - Hardware bitmap disabled for notification / session compatibility
 * - Robust error handling (never crashes playback)
 */
@UnstableApi
class CastyCoilBitmapLoader(
    private val context: Context
) : BitmapLoader {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun supportsMimeType(mimeType: String): Boolean = true

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        scope.launch {
            try {
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                future.set(bitmap)
            } catch (t: Throwable) {
                future.setException(t)
            }
        }
        return future
    }

    override fun loadBitmap(uri: android.net.Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        scope.launch {
            try {
                val bitmap = loadWithCoil(uri.toString())
                if (bitmap != null) {
                    future.set(bitmap)
                } else {
                    future.setException(IllegalStateException("Failed to load artwork"))
                }
            } catch (t: Throwable) {
                future.setException(t)
            }
        }
        return future
    }

    private suspend fun loadWithCoil(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val imageLoader = SingletonImageLoader.get(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(Size(1024, 1024)) // Good balance for lock screen / notification
                .allowHardware(false)   // Mandatory for MediaSession bitmaps
                .build()

            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                // Convert to Android Bitmap safely using asDrawable KTX extension
                val drawable = result.image.asDrawable(context.resources)
                if (drawable is android.graphics.drawable.BitmapDrawable) {
                    return@withContext drawable.bitmap
                } else {
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        drawable.intrinsicWidth.coerceAtLeast(1),
                        drawable.intrinsicHeight.coerceAtLeast(1),
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    val canvas = android.graphics.Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    return@withContext bitmap
                }
            }
            null
        } catch (_: Throwable) {
            null
        }
    }
}