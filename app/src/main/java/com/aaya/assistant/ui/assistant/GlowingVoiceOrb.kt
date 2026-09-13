package com.aaya.assistant.ui.assistant

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aaya.assistant.ui.theme.ElectricViolet
import com.aaya.assistant.ui.theme.NeonCyan
import com.aaya.assistant.ui.theme.RadiantPurple

@Composable
fun GlowingVoiceOrb(
    isListening: Boolean,
    audioLevelRms: Float = 0f,
    size: Dp = 160.dp,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbGlow")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    // Dynamic audio level responsiveness
    val audioMultiplier = if (isListening) (1f + (audioLevelRms.coerceIn(0f, 15f) / 20f)) else 1f

    Box(
        modifier = Modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2, size.toPx() / 2)
            val baseRadius = (size.toPx() / 2.5f) * pulseScale * audioMultiplier

            // 1. Outer ambient glow ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = if (isListening) 0.45f else 0.2f),
                        ElectricViolet.copy(alpha = if (isListening) 0.25f else 0.1f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.35f
                ),
                radius = baseRadius * 1.35f,
                center = center
            )

            // 2. Middle rotating neural wave ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(NeonCyan, RadiantPurple, ElectricViolet, NeonCyan),
                    center = center
                ),
                radius = baseRadius * 1.05f,
                center = center,
                style = Stroke(width = if (isListening) 3.5.dp.toPx() else 2.dp.toPx())
            )

            // 3. Inner core celestial sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        NeonCyan,
                        ElectricViolet
                    ),
                    center = center,
                    radius = baseRadius * 0.75f
                ),
                radius = baseRadius * 0.75f,
                center = center
            )
        }
    }
}
