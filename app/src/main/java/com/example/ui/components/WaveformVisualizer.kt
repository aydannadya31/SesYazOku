package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun WaveformVisualizer(
    waveforms: List<Float>,
    isRecording: Boolean,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_pulse")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val barCount = 45
        val barSpacing = width / barCount
        val barWidth = (barSpacing * 0.55f).coerceAtLeast(3f)

        // Draw subtle center baseline
        drawLine(
            color = surfaceColor.copy(alpha = 0.6f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 2f
        )

        val dataSize = waveforms.size
        for (i in 0 until barCount) {
            val dataIndex = (dataSize - barCount + i)
            val amplitude = if (dataIndex >= 0 && dataIndex < dataSize) {
                waveforms[dataIndex]
            } else {
                0.08f
            }

            val dynamicMultiplier = if (isRecording && !isPaused) pulseAnim else 1.0f
            val barHeight = ((height * 0.85f) * amplitude * dynamicMultiplier).coerceIn(6f, height * 0.9f)
            val x = i * barSpacing + (barSpacing - barWidth) / 2f
            val y = centerY - (barHeight / 2f)

            val barBrush = Brush.verticalGradient(
                colors = listOf(
                    secondaryColor,
                    primaryColor
                ),
                startY = y,
                endY = y + barHeight
            )

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
