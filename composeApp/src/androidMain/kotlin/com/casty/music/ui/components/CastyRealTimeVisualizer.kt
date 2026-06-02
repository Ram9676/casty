package com.casty.music.ui.components

import android.media.audiofx.Visualizer
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
 * World-class real-time pink visualizer for Casty.
 * Uses Android's Visualizer API for actual audio reactivity.
 * 
 * This is a safe, optional component. It does NOT modify any core playback logic.
 * Can be used in FullscreenNowPlaying when audioSessionId is available.
 */
@Composable
fun CastyRealTimeVisualizer(
    audioSessionId: Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val pink = CastyTheme.colors.accentPink
    var waveform by remember { mutableStateOf(FloatArray(48)) }

    // Real audio visualization using Android Visualizer (safe & performant)
    DisposableEffect(audioSessionId, isPlaying) {
        var visualizer: Visualizer? = null

        if (audioSessionId != 0 && isPlaying) {
            try {
                visualizer = Visualizer(audioSessionId).apply {
                    captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(128)
                    setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveformData: ByteArray?,
                            samplingRate: Int
                        ) {
                            waveformData?.let { data ->
                                // Convert to normalized float array for beautiful rendering
                                val newWave = FloatArray(48)
                                val step = data.size / 48
                                for (i in newWave.indices) {
                                    val index = (i * step).coerceIn(0, data.size - 1)
                                    newWave[i] = ((data[index].toInt() and 0xFF) - 128) / 128f
                                }
                                waveform = newWave
                            }
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fftData: ByteArray?,
                            samplingRate: Int
                        ) {
                            // We can add spectrum analysis here later for even more premium feel
                        }
                    }, Visualizer.getMaxCaptureRate() / 2, true, false)
                    enabled = true
                }
            } catch (e: Exception) {
                // Visualizer not available on this device or audio session — graceful fallback
            }
        }

        onDispose {
            visualizer?.release()
        }
    }

    // Beautiful pink rendering using real audio data
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val barCount = 48
        val barWidth = width / (barCount * 1.6f)

        for (i in 0 until barCount) {
            val x = (i + 0.5f) * (width / barCount)
            val raw = waveform.getOrNull(i) ?: 0f

            // Smooth organic response for premium feel
            val smoothed = raw * 0.8f + sin(i * 0.4f) * 0.1f
            val amplitude = (smoothed * 0.65f + 0.12f) * (if (isPlaying) 1f else 0.15f)

            val barHeight = height * amplitude.coerceIn(0.03f, 0.97f)

            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        pink.copy(alpha = 0.4f),
                        pink.copy(alpha = 0.98f),
                        pink.copy(alpha = 0.4f)
                    )
                ),
                start = Offset(x, centerY - barHeight / 2),
                end = Offset(x, centerY + barHeight / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}