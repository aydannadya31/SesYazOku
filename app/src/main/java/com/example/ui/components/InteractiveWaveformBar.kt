package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TranscriptSegmentEntity
import java.util.Locale
import kotlin.math.sin

@Composable
fun InteractiveWaveformBar(
    currentPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    segments: List<TranscriptSegmentEntity> = emptyList(),
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveDuration = if (durationMs > 0) durationMs else 1L
    val progressFraction = (currentPositionMs.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)

    // Primary and secondary colors from Material 3
    val playedColor = MaterialTheme.colorScheme.primary
    val unplayedColor = MaterialTheme.colorScheme.surfaceVariant
    val activeSegmentColor = MaterialTheme.colorScheme.tertiary

    val currentSec = currentPositionMs / 1000
    val totalSec = effectiveDuration / 1000

    // Currently speaking segment
    val activeSegment = segments.find { currentPositionMs in it.startMs..it.endMs }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Top status row: Active speaker and karaoke indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (activeSegment != null) "🎙️ ${activeSegment.speakerLabel} konuşuyor" else if (isPlaying) "Oynatılıyor..." else "Duraklatıldı",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (activeSegment != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = String.format(
                    Locale.getDefault(),
                    "%02d:%02d / %02d:%02d",
                    currentSec / 60, currentSec % 60,
                    totalSec / 60, totalSec % 60
                ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Waveform Canvas (Clickable & Scrubbable)
        val barCount = 42
        val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "wave_progress")

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .pointerInput(effectiveDuration) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        val targetMs = (ratio * effectiveDuration).toLong()
                        onSeek(targetMs)
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barWidth = (canvasWidth / barCount) * 0.65f
            val spacing = (canvasWidth / barCount) * 0.35f

            for (i in 0 until barCount) {
                val barFraction = i.toFloat() / barCount
                val barTimeMs = (barFraction * effectiveDuration).toLong()

                // Generate pleasant harmonic wave heights based on speaker segments
                val segmentAtBar = segments.find { barTimeMs in it.startMs..it.endMs }
                val baseHeight = if (segmentAtBar != null) {
                    0.45f + 0.5f * kotlin.math.abs(sin(i * 0.45f)).toFloat()
                } else {
                    0.15f + 0.25f * kotlin.math.abs(sin(i * 0.35f)).toFloat()
                }

                val barH = (canvasHeight * baseHeight).coerceAtLeast(6f)
                val x = i * (barWidth + spacing)
                val y = (canvasHeight - barH) / 2f

                val isPlayed = barFraction <= animatedProgress
                val isCurrentActiveSpeaker = activeSegment != null && segmentAtBar?.id == activeSegment.id

                val color = when {
                    isCurrentActiveSpeaker && isPlayed -> activeSegmentColor
                    isPlayed -> playedColor
                    isCurrentActiveSpeaker -> activeSegmentColor.copy(alpha = 0.5f)
                    else -> unplayedColor
                }

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
            }

            // Playhead scrubber line
            val playheadX = animatedProgress * canvasWidth
            drawLine(
                color = playedColor,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, canvasHeight),
                strokeWidth = 3f
            )
            drawCircle(
                color = playedColor,
                radius = 5f,
                center = Offset(playheadX, canvasHeight / 2)
            )
        }
    }
}
