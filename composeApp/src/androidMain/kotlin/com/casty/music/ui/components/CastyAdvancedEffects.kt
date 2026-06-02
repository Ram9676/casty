package com.casty.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.casty.music.ui.theme.CastyTheme
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

/**
 * God-tier Particle System for premium background effects.
 * Creates floating particles with physics-based movement for an ethereal, luxurious feel.
 * Use in Now Playing screen, onboarding, or any premium surface.
 */
@Composable
fun CastyParticleSystem(
    modifier: Modifier = Modifier,
    particleCount: Int = 60,
    baseSpeed: Float = 0.3f,
    enableInteraction: Boolean = true,
) {
    val particles = remember { mutableStateListOf<Particle>() }
    var touchPos by remember { mutableStateOf(Offset.Zero) }
    var isTouching by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // Initialize particles with random positions and properties
        repeat(particleCount) { i ->
            particles.add(
                Particle(
                    x = randomFloat(0f, 1000f),
                    y = randomFloat(0f, 1000f),
                    vx = (baseSpeed * (0.5f + Random.nextFloat())).let { if (Random.nextBoolean()) it else -it },
                    vy = (baseSpeed * 0.5f * (0.5f + Random.nextFloat())).let { if (Random.nextBoolean()) it else -it },
                    radius = randomFloat(1.5f, 4f),
                    alpha = randomFloat(0.15f, 0.4f),
                    hue = randomFloat(320f, 350f), // Pink spectrum
                    life = randomFloat(0f, 1f)
                )
            )
        }
        
        while (true) {
            particles.forEach { it.update(enableInteraction, touchPos, isTouching) }
            delay(16L) // ~60 FPS
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val scale = min(width, height) / 1000f
        
        particles.forEach { particle ->
            val x = (particle.x * scale).coerceIn(0f, width)
            val y = (particle.y * scale).coerceIn(0f, height)
            
            // Draw glow halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.hsv(particle.hue, 0.8f, 1f).copy(alpha = particle.alpha * 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(x, y),
                    radius = particle.radius * scale * 4f
                ),
                radius = particle.radius * scale * 4f,
                center = Offset(x, y)
            )
            
            // Draw core particle
            drawCircle(
                color = Color.hsv(particle.hue, 0.9f, 1f).copy(alpha = particle.alpha),
                radius = particle.radius * scale,
                center = Offset(x, y)
            )
        }
    }
}

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    var alpha: Float,
    var hue: Float,
    var life: Float
) {
    fun update(enableInteraction: Boolean, touchPos: Offset, isTouching: Boolean) {
        // Apply velocity
        x += vx
        y += vy
        
        // Boundary wrap
        if (x < 0) x = 1000f
        if (x > 1000) x = 0f
        if (y < 0) y = 1000f
        if (y > 1000) y = 0f
        
        // Interaction repulsion
        if (enableInteraction && isTouching) {
            val dx = x - touchPos.x
            val dy = y - touchPos.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < 200f) {
                val force = (200f - dist) / 200f
                vx += (dx / dist) * force * 0.5f
                vy += (dy / dist) * force * 0.5f
            }
        }
        
        // Damping
        vx *= 0.995f
        vy *= 0.995f
        
        // Life cycle
        life -= 0.0005f
        if (life <= 0f) {
            x = randomFloat(0f, 1000f)
            y = 0f
            vx = randomFloat(0.2f, 0.5f).let { if (Random.nextBoolean()) it else -it }
            vy = randomFloat(0.1f, 0.3f)
            life = 1f
        }
    }
}

/**
 * Advanced gradient mesh background with animated color shifts.
 * Creates a premium, dynamic backdrop that responds to playback state.
 */
@Composable
fun CastyGradientMesh(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    baseColor: Color = CastyTheme.colors.accentPink
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradientMesh")
    
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset1"
    )
    
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 1000f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset2"
    )
    
    val intensity = if (isPlaying) 1.2f else 0.6f
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Create multiple gradient layers for depth
        val colors1 = listOf(
            baseColor.copy(alpha = 0.15f * intensity),
            baseColor.copy(alpha = 0.08f * intensity),
            Color.Transparent
        )
        
        val colors2 = listOf(
            baseColor.copy(alpha = 0.12f * intensity),
            Color.Transparent,
            baseColor.copy(alpha = 0.06f * intensity)
        )
        
        // Diagonal gradient 1
        drawRect(
            brush = Brush.linearGradient(
                colors = colors1,
                start = Offset(offset1 * width / 1000f, 0f),
                end = Offset((offset1 - 500) * width / 1000f, height)
            )
        )
        
        // Diagonal gradient 2
        drawRect(
            brush = Brush.linearGradient(
                colors = colors2,
                start = Offset((offset2 - 500) * width / 1000f, height),
                end = Offset(offset2 * width / 1000f, 0f)
            )
        )
        
        // Radial accent
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.08f * intensity),
                    Color.Transparent
                ),
                center = Offset(width * 0.7f, height * 0.3f),
                radius = max(width, height) * 0.5f
            )
        )
    }
}

/**
 * Real-time audio waveform visualization with advanced smoothing.
 * More sophisticated than basic bars - creates flowing wave patterns.
 */
@Composable
fun CastyWaveformVisualizer(
    audioData: FloatArray,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = CastyTheme.colors.accentPink
) {
    val smoothedData = remember(audioData) { 
        applyGaussianSmooth(audioData, sigma = 1.5f)
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        
        if (!isPlaying || audioData.isEmpty()) {
            // Idle state - subtle pulse line
            drawLine(
                color = color.copy(alpha = 0.3f),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 2.dp.toPx()
            )
            return@Canvas
        }
        
        val path = Path()
        val step = width / (smoothedData.size - 1)
        
        // Build smooth waveform path
        smoothedData.forEachIndexed { index, amplitude ->
            val x = index * step
            val y = centerY + (amplitude * height * 0.4f)
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                val prevX = (index - 1) * step
                val prevY = centerY + (smoothedData[index - 1] * height * 0.4f)
                val controlX = (prevX + x) / 2f
                path.quadraticBezierTo(controlX, prevY, x, y)
            }
        }
        
        // Draw filled waveform with gradient
        val fillPath = Path().apply { addPath(path) }
        fillPath.lineTo(width, height)
        fillPath.lineTo(0f, height)
        fillPath.close()
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.6f),
                    color.copy(alpha = 0.2f),
                    Color.Transparent
                )
            )
        )
        
        // Draw outline
        drawPath(
            path = path,
            color = color.copy(alpha = 0.9f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private fun applyGaussianSmooth(data: FloatArray, sigma: Float): FloatArray {
    if (data.size < 3) return data
    
    val kernel = createGaussianKernel(sigma)
    val result = FloatArray(data.size)
    val halfSize = kernel.size / 2
    
    for (i in data.indices) {
        var sum = 0f
        var weightSum = 0f
        
        for (j in kernel.indices) {
            val dataIndex = (i - halfSize + j).coerceIn(0, data.size - 1)
            sum += data[dataIndex] * kernel[j]
            weightSum += kernel[j]
        }
        
        result[i] = sum / weightSum
    }
    
    return result
}

private fun createGaussianKernel(sigma: Float, size: Int = 5): FloatArray {
    val kernel = FloatArray(size)
    val mean = size / 2f
    val variance = sigma * sigma
    
    var sum = 0f
    for (i in kernel.indices) {
        val x = i - mean
        kernel[i] = (1f / sqrt(2f * PI.toFloat() * variance)) * exp(-(x * x) / (2f * variance))
        sum += kernel[i]
    }
    
    // Normalize
    for (i in kernel.indices) {
        kernel[i] /= sum
    }
    
    return kernel
}

private fun randomFloat(from: Float, to: Float): Float {
    return from + Random.nextFloat() * (to - from)
}
