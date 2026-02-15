package com.example.android_musicplayerwithvj

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VJCanvas(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "VJ")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (!isPlaying) return@Canvas

        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 4 * scale

        rotate(rotation, center) {
            for (i in 0 until 12) {
                val angle = i * (360f / 12)
                val x = center.x + radius * cos(Math.toRadians(angle.toDouble())).toFloat()
                val y = center.y + radius * sin(Math.toRadians(angle.toDouble())).toFloat()
                
                drawCircle(
                    color = Color.Cyan.copy(alpha = 0.6f),
                    radius = 40f * scale,
                    center = Offset(x, y)
                )
                
                drawLine(
                    color = Color.Magenta.copy(alpha = 0.4f),
                    start = center,
                    end = Offset(x, y),
                    strokeWidth = 5f
                )
            }
        }
    }
}