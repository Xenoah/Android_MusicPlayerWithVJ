package com.example.android_musicplayerwithvj

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.math.*

@Composable
fun VJCanvas(
    isPlaying: Boolean,
    fftData: FloatArray,
    waveData: ByteArray,
    style: VJStyle,
    colorMode: VJColorMode = VJColorMode.COLORFUL,
    singleColor: Color = Color.Cyan,
    artworkUri: Uri? = null,
    zoomOnKickEnabled: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VJ")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(animation = tween(50000, easing = LinearEasing)),
        label = "Time"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(30000, easing = LinearEasing)),
        label = "Rotation"
    )

    // --- Advanced Beat/Kick Detection ---
    val rawKick = if (fftData.size > 1) fftData[1] else 0f
    
    // rollingMaxLow monitors overall volume to avoid overreacting in quiet parts
    var rollingMaxLow by remember { mutableStateOf(0.5f) }
    LaunchedEffect(rawKick) {
        rollingMaxLow = max(0.2f, rollingMaxLow * 0.99f + rawKick * 0.01f)
        if (rawKick > rollingMaxLow) rollingMaxLow = rawKick
    }
    
    val normalizedKick = (rawKick / rollingMaxLow).coerceIn(0f, 1f)
    
    var lastBeatTime by remember { mutableStateOf(0L) }
    var beatConfidence by remember { mutableStateOf(0f) }
    val currentTime = System.currentTimeMillis()
    
    // Attack detection
    val isPeak = normalizedKick > 0.85f && normalizedKick > (beatConfidence * 0.8f)
    
    LaunchedEffect(isPeak) {
        if (isPeak) {
            val delta = currentTime - lastBeatTime
            // 4/4 rhythm range (Approx 75-200 BPM)
            if (delta in 300..800) {
                beatConfidence = min(1f, beatConfidence + 0.25f)
            } else {
                beatConfidence = max(0f, beatConfidence - 0.15f)
            }
            lastBeatTime = currentTime
        }
    }

    // Sharper and rhythmic zoom for Liquid style only
    val zoomValue = if (style == VJStyle.LIQUID && zoomOnKickEnabled) {
        val intensity = if (isPeak) 0.08f * (0.4f + beatConfidence * 0.6f) else 0f
        1f + intensity.coerceAtMost(0.1f)
    } else 1f

    val animatedZoom by animateFloatAsState(
        targetValue = zoomValue,
        animationSpec = spring(
            dampingRatio = 0.3f, // Sharp bounce
            stiffness = 2000f    // Very fast response
        ),
        label = "LiquidRhythmicZoom"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val maxDimPx = minOf(widthPx, heightPx)

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (!isPlaying) return@Canvas

            val center = Offset(size.width / 2, size.height / 2)
            val currentScale = if (style == VJStyle.LIQUID) animatedZoom else 1f

            scale(scale = currentScale, pivot = center) {
                when (style) {
                    VJStyle.NEON_WAVES -> {
                        val colors = if (colorMode == VJColorMode.COLORFUL) {
                            listOf(Color.Cyan, Color.Green, Color.Magenta)
                        } else {
                            listOf(singleColor, singleColor, singleColor)
                        }
                        
                        colors.forEachIndexed { index, color ->
                            val path = Path()
                            val phase = time + (index * 2f)
                            val points = 100
                            for (i in 0 until points) {
                                val x = (i.toFloat() / points) * size.width
                                val waveVal = sin((i.toFloat() / 10f) + phase).toFloat()
                                val audioVal = (waveData.getOrElse((i * (waveData.size / points)) % waveData.size) { 0 }.toInt()) / 128f
                                val y = center.y + (waveVal * 100f) + (audioVal * 150f * (index + 1) * 0.5f)
                                
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                color = color.copy(alpha = 0.6f),
                                style = Stroke(width = 12f, cap = StrokeCap.Round)
                            )
                        }
                    }
                    VJStyle.AURA_HEAT -> {
                        val avgFft = if (fftData.isNotEmpty()) fftData.average().toFloat() else 0f
                        val colors = if (colorMode == VJColorMode.COLORFUL) {
                            listOf(Color.Blue, Color.Magenta, Color.Red, Color.Yellow)
                        } else {
                            listOf(singleColor, singleColor, singleColor, singleColor)
                        }
                        
                        for (i in 0 until 5) {
                            val angle = time * (i + 1) * 0.2f
                            val r = (size.minDimension / 3) * (1f + avgFft * 2f)
                            val offset = Offset(
                                center.x + cos(angle.toDouble()).toFloat() * 200f * avgFft,
                                center.y + sin(angle.toDouble()).toFloat() * 200f * avgFft
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(colors[i % colors.size].copy(alpha = 0.7f), Color.Transparent),
                                    center = offset,
                                    radius = r
                                ),
                                radius = r,
                                center = offset
                            )
                        }
                    }
                    VJStyle.LIQUID -> {
                        val path = Path()
                        val points = 128
                        val baseR = (size.minDimension / 4.5f).coerceAtLeast(120f)
                        val avgFft = if (fftData.isNotEmpty()) fftData.average().toFloat() else 0f
                        val radiusData = FloatArray(points)
                        
                        for (i in 0 until points) {
                            val waveIdx = (i * (waveData.size / points)) % (waveData.size.takeIf { it > 0 } ?: 1)
                            val rawAmp = (waveData.getOrElse(waveIdx) { 0 }.toFloat() / 128f)
                            
                            // 1. Aggressive noise gate for stability
                            val gate = 0.15f
                            val cleanedAmp = if (abs(rawAmp) < gate) 0f else (rawAmp - sign(rawAmp) * gate)
                            
                            // 2. High peak sensitivity + atan soft-limit
                            // Ensure very thin ring remains in silence (0.01f)
                            val dynamicRadius = 220f * atan(2.5f * (abs(cleanedAmp) + 0.01f) + avgFft * 2.5f)
                            radiusData[i] = baseR + dynamicRadius
                        }
                        
                        for (i in 0 until points) {
                            val angle1 = (i.toFloat() / points) * 2 * PI
                            val angle2 = ((i + 1).toFloat() / points) * 2 * PI
                            val midAngle = (angle1 + angle2) / 2
                            val r1 = radiusData[i]
                            val r2 = radiusData[(i + 1) % points]
                            
                            val x1 = (center.x + r1 * cos(angle1)).toFloat()
                            val y1 = (center.y + r1 * sin(angle1)).toFloat()
                            val xMid = (center.x + (r1 + r2) / 2 * cos(midAngle)).toFloat()
                            val yMid = (center.y + (r1 + r2) / 2 * sin(midAngle)).toFloat()
                            
                            if (i == 0) path.moveTo(x1, y1)
                            path.quadraticBezierTo(x1, y1, xMid, yMid)
                        }
                        path.close()
                        
                        val color = if (colorMode == VJColorMode.COLORFUL) Color.Cyan else singleColor
                        drawPath(path, color)
                        drawCircle(Color.Black, radius = baseR * 0.85f, center = center)
                    }
                    VJStyle.SPEKTRO -> {
                        val numBands = 32
                        val barWidth = size.width / numBands
                        for (i in 0 until numBands) {
                            val magnitude = fftData.getOrElse(i) { 0f }
                            val color = if (colorMode == VJColorMode.COLORFUL) {
                                Color.hsv((i.toFloat() / numBands) * 360f, 0.7f, 1f)
                            } else {
                                singleColor
                            }
                            val h = magnitude * size.height * 0.4f
                            drawRect(color = color, topLeft = Offset(i * barWidth, center.y - h / 2), size = androidx.compose.ui.geometry.Size(barWidth - 1f, h))
                        }
                    }
                    VJStyle.ALCHEMY -> {
                        rotate(rotation) {
                            val path = Path()
                            for (i in 0 until 64) {
                                val angle = (i.toFloat() / 64) * 2 * PI
                                val r = (size.minDimension / 5) + (fftData.getOrElse(i) { 0f } * 180f)
                                val x = (center.x + r * cos(angle)).toFloat(); val y = (center.y + r * sin(angle)).toFloat()
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            path.close()
                            val brush = if (colorMode == VJColorMode.COLORFUL) {
                                Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Red), center)
                            } else {
                                SolidColor(singleColor)
                            }
                            drawPath(path, brush, style = Stroke(width = 4f))
                        }
                    }
                    VJStyle.SPIKE -> {
                        for (i in 0 until 32) {
                            val angle = (i.toFloat() / 32) * 360f
                            val len = 60f + fftData.getOrElse(i) { 0f } * 350f
                            val color = if (colorMode == VJColorMode.COLORFUL) Color.hsv(angle, 0.8f, 1f) else singleColor
                            rotate(angle + rotation, center) { drawLine(color, Offset(center.x + 40f, center.y), Offset(center.x + len, center.y), strokeWidth = 4f) }
                        }
                    }
                    VJStyle.BARS -> {
                        val bw = size.width / 32
                        for (i in 0 until 32) {
                            val bh = fftData.getOrElse(i) { 0f } * size.height * 0.5f
                            val color = if (colorMode == VJColorMode.COLORFUL) Color.hsv((i.toFloat() / 32) * 360f, 0.6f, 1f) else singleColor
                            drawRect(color.copy(alpha = 0.7f), Offset(i * bw, size.height - bh), androidx.compose.ui.geometry.Size(bw - 2f, bh))
                        }
                    }
                }
            }
        }

        if (style == VJStyle.LIQUID && artworkUri != null) {
            val baseR = (maxDimPx / 4.5f).coerceAtLeast(with(density) { 120.dp.toPx() })
            val artworkSize = with(density) { (baseR * 1.6f * animatedZoom).toDp() }

            AsyncImage(
                model = artworkUri,
                contentDescription = null,
                modifier = Modifier
                    .size(artworkSize)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}
