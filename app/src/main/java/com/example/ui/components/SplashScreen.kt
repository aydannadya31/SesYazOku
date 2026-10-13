package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow_transition")

    // Rotating angle for the glowing aura
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulsing breathing scale for the aura
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Shimmer glow alpha
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    LaunchedEffect(Unit) {
        delay(2500) // Show rotating aura effect then transition smoothly
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Deep Midnight Navy
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Rotating Glowing Aura Canvas around the text
        Box(
            modifier = Modifier
                .size(310.dp)
                .scale(pulseScale),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotationAngle)
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2.3f

                // Outer multi-colored glowing sweep ring
                val sweepColors = listOf(
                    Color(0xFF38BDF8).copy(alpha = glowAlpha), // Cyan
                    Color(0xFF818CF8).copy(alpha = glowAlpha * 0.9f), // Indigo
                    Color(0xFFA855F7).copy(alpha = glowAlpha), // Purple
                    Color(0xFFEC4899).copy(alpha = glowAlpha * 0.85f), // Pink
                    Color(0xFFF59E0B).copy(alpha = glowAlpha), // Amber gold
                    Color(0xFF10B981).copy(alpha = glowAlpha * 0.9f), // Emerald
                    Color(0xFF38BDF8).copy(alpha = glowAlpha)
                )

                val sweepBrush = Brush.sweepGradient(
                    colors = sweepColors,
                    center = center
                )

                // Soft outer aura stroke
                drawCircle(
                    brush = sweepBrush,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 18f, cap = StrokeCap.Round)
                )

                // Thin vibrant inner accent ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f * glowAlpha),
                    radius = radius - 8f,
                    center = center,
                    style = Stroke(width = 3f)
                )
            }

            // Central "SesYazOku" Typography (NO LOGO, strictly text as requested)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "SesYazOku",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Ses • Transkript • Döküman",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
