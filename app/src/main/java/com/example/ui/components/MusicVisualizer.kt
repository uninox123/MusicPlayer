package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun LiveVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 15,
    activeColor: Color = MaterialTheme.colorScheme.primary
) {
    // Continuous animation driving the visualizer
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    val animationPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = width / (barCount * 1.5f)
        val gap = barWidth * 0.5f

        for (i in 0 until barCount) {
            // Generate standard wave-like pattern, multiplied by dynamic factor if playing
            val baseHeightFactor = if (isPlaying) {
                // Combine primary wave, secondary octave, and pseudo-random jitter
                val sineVal = sin(animationPhase + (i * 0.6f))
                val jitter = sin(animationPhase * 2.3f + i) * 0.2f
                ((sineVal + 1f) / 2f * 0.7f + 0.3f + jitter).coerceIn(0.1f, 1f)
            } else {
                // Quiet resting state
                0.1f + (sin(i * 0.5f) + 1f) * 0.05f
            }

            val barHeight = height * baseHeightFactor
            val x = i * (barWidth + gap) + gap
            val y = height - barHeight

            // Draw rounded bar using line with stroke cap or rectangular path
            drawLine(
                color = activeColor.copy(alpha = if (isPlaying) 1f else 0.5f),
                start = Offset(x + barWidth / 2, height),
                end = Offset(x + barWidth / 2, y),
                strokeWidth = barWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
fun WaveformProgressBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    barCount: Int = 40,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
) {
    // Generate static pseudo-random waveform height values that remain consistent for a song
    val waveHeights = remember {
        val random = Random(12345) // Seeded so it is stable
        List(barCount) { random.nextFloat().coerceIn(0.2f, 1.0f) }
    }

    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f

    BoxWithConstraints(modifier = modifier) {
        val width = maxWidth
        val height = maxHeight

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Calculate selected progress based on tap location
                        val clickedProgress = offset.x / size.width
                        val selectedPosition = (clickedProgress * durationMs).toLong()
                        onSeek(selectedPosition.coerceIn(0L, durationMs))
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barWidth = canvasWidth / (barCount * 1.3f)
            val gap = barWidth * 0.3f

            for (i in 0 until barCount) {
                val x = i * (barWidth + gap) + gap
                val barProgress = i.toFloat() / barCount.toFloat()
                
                // Color active vs inactive bars
                val color = if (barProgress <= progress) activeColor else inactiveColor
                
                val waveHeight = waveHeights[i] * canvasHeight
                val yStart = (canvasHeight - waveHeight) / 2
                val yEnd = yStart + waveHeight

                drawLine(
                    color = color,
                    start = Offset(x + barWidth / 2, yStart),
                    end = Offset(x + barWidth / 2, yEnd),
                    strokeWidth = barWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}
