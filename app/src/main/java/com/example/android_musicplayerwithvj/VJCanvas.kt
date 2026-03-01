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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import kotlin.math.*

data class AuraParticle(
    val x: Float, val y: Float,
    val vx: Float, val vy: Float,
    val life: Float, val hue: Float, val sz: Float
)

@Composable
fun VJCanvas(
    isPlaying: Boolean,
    fftData: FloatArray,
    waveData: ByteArray,
    style: VJStyle,
    colorMode: VJColorMode = VJColorMode.COLORFUL,
    singleColor: Color = Color.Cyan,
    artworkUri: Uri? = null,
    zoomOnKickEnabled: Boolean = true,
    trackPeakLow: Float = 0.15f,
    trackPeakAll: Float = 0.15f,
    uiScale: Float = 1.0f,
    effectScale: Float = 1.0f,
    liquidFlipped: Boolean = false,
    fireworkSensitivity: Float = 1.3f
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

    // --- Robust Beat / Kick Detection ---
    // Use the first few FFT bins as bass energy (kick lives around 40-120Hz)
    val bassEnergy = if (fftData.size > 3) (fftData[1] + fftData[2] + fftData[3]) / 3f else 0f

    // Low-pass filter: slowly track the baseline energy level
    var energyBaseline by remember { mutableFloatStateOf(0f) }
    val baselineAlpha = 0.15f  // how quickly the baseline adapts (lower = slower)
    energyBaseline = energyBaseline * (1f - baselineAlpha) + bassEnergy * baselineAlpha

    // A kick is detected when current energy spikes clearly above the running baseline
    val kickThreshold = 1.6f   // must be this many times above baseline
    val minEnergy    = 0.04f   // ignore silence / near-silence
    val isKick = bassEnergy > energyBaseline * kickThreshold && bassEnergy > minEnergy

    // Gate the kick: require a minimum interval between beats (prevent re-triggering)
    var lastKickTime by remember { mutableLongStateOf(0L) }
    val currentTimeMs = System.currentTimeMillis()
    val gatedKick = isKick && (currentTimeMs - lastKickTime) > 180L  // min 180ms between kicks

    // Update lastKickTime when a gated kick fires
    if (gatedKick) lastKickTime = currentTimeMs

    // Zoom target: snap to zoom-in on kick, otherwise idle at 1f
    // animateFloatAsState with a spring handles the natural decay back to 1f
    val zoomTarget = if (style == VJStyle.FLOWER && zoomOnKickEnabled && gatedKick) 1.07f else 1f

    val animatedZoom by animateFloatAsState(
        targetValue = zoomTarget,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 2000f),
        label = "FlowerZoom"
    )


    // ---- Spectral centroid for timbre-based color (AURA_HEAT) ----
    val spectralCentroid = if (fftData.size > 2) {
        var ws = 0f; var tw = 0f
        for (i in 1 until fftData.size) { ws += i * fftData[i]; tw += fftData[i] }
        if (tw > 0f) (ws / tw / fftData.size).coerceIn(0f, 1f) else 0f
    } else 0f
    val timbreHue = spectralCentroid * 270f

    // ---- Particle state + coroutine-based transient detection + spawning ----
    var particleCx by remember { mutableFloatStateOf(0f) }
    var particleCy by remember { mutableFloatStateOf(0f) }
    var auraParticles by remember { mutableStateOf(listOf<AuraParticle>()) }

    // Always-current refs so the LaunchedEffect reads latest values without restarting
    val fftRef          = rememberUpdatedState(fftData)
    val playingRef      = rememberUpdatedState(isPlaying)
    val styleRef        = rememberUpdatedState(style)
    val effectRef       = rememberUpdatedState(effectScale)
    val sensitivityRef  = rememberUpdatedState(fireworkSensitivity)
    val timbreRef       = rememberUpdatedState(timbreHue)
    val cxRef           = rememberUpdatedState(particleCx)
    val cyRef           = rememberUpdatedState(particleCy)

    LaunchedEffect(Unit) {
        val rand = kotlin.random.Random.Default
        var hiBaselineLoc = 0f
        var lastTransientMsLoc = 0L

        while (true) {
            kotlinx.coroutines.delay(16)

            // 1. Physics update (every frame, all styles)
            auraParticles = auraParticles.mapNotNull { p ->
                if (p.life <= 0.02f) null
                else p.copy(
                    x = p.x + p.vx,
                    y = p.y + p.vy,
                    vy = p.vy + 0.28f,
                    vx = p.vx * 0.97f,
                    life = p.life - 0.018f
                )
            }

            if (!playingRef.value || styleRef.value != VJStyle.AURA_HEAT) continue

            // 2. Transient detection
            val fft = fftRef.value
            if (fft.size < 4) continue
            val hiS = (fft.size * 0.15f).toInt().coerceAtLeast(1)
            val hiE = (fft.size * 0.85f).toInt().coerceAtMost(fft.size)
            val hiEn = if (hiE > hiS) {
                var sum = 0f
                for (i in hiS until hiE) sum += fft.getOrElse(i) { 0f }
                sum / (hiE - hiS)
            } else 0f

            hiBaselineLoc = hiBaselineLoc * 0.92f + hiEn * 0.08f

            val threshold   = sensitivityRef.value
            val nowMs       = System.currentTimeMillis()
            val isT         = hiEn > hiBaselineLoc * threshold && hiEn > 0.005f
            val gated       = isT && (nowMs - lastTransientMsLoc) > 100L

            if (!gated) continue
            lastTransientMsLoc = nowMs

            // 3. Spawn burst
            val cx  = cxRef.value
            val cy  = cyRef.value
            if (cx <= 0f && cy <= 0f) continue
            val eff = effectRef.value.coerceIn(0.5f, 3f)
            val tHue = timbreRef.value
            val burst = (0 until 28).map { i ->
                val angle = (i.toFloat() / 28f) * 2f * PI.toFloat() + rand.nextFloat() * 0.5f
                val speed = 6f + rand.nextFloat() * 16f * eff
                AuraParticle(
                    x = cx, y = cy,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 3f,
                    life = 0.75f + rand.nextFloat() * 0.25f,
                    hue  = (tHue + rand.nextFloat() * 60f - 30f).mod(360f),
                    sz   = 5f + rand.nextFloat() * 9f
                )
            }
            auraParticles = (auraParticles + burst).takeLast(300)
        }
    }


    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val maxDimPx = min(widthPx, heightPx)
        val cx = widthPx / 2f
        val cy = heightPx / 2f
        // Update center for the particle coroutine
        particleCx = cx
        particleCy = cy

        // Common base radius (70% scale), further scaled by uiScale
        val commonBaseR = (maxDimPx / 3.2f) * 0.7f * uiScale


        // 1. First, draw artwork (Bottom layer)
        if ((style == VJStyle.LIQUID || style == VJStyle.FLOWER) && artworkUri != null) {
            val artworkScale = if (style == VJStyle.FLOWER) animatedZoom else 1f
            // artwork size matches commonBaseR exactly
            val artworkSize = with(density) { (commonBaseR * 2.0f * artworkScale).toDp() } 
            AsyncImage(
                model = artworkUri,
                contentDescription = null,
                modifier = Modifier
                    .size(artworkSize)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        // 2. Then, draw Canvas (Front layer)
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (!isPlaying) return@Canvas

            val center = Offset(size.width / 2f, size.height / 2f)
            val currentScale = if (style == VJStyle.FLOWER) animatedZoom else 1f

            scale(scale = currentScale, pivot = center) {
                when (style) {
                    VJStyle.FLOWER -> {
                        // FLOWER: Expands outward with volume (same as LIQUID)
                        val path = Path()
                        val points = 128
                        val radiusData = FloatArray(points)
                        for (i in 0 until points) {
                            val waveIdx = (i * (waveData.size / points)) % (waveData.size.takeIf { it > 0 } ?: 1)
                            val rawAmp = (waveData.getOrElse(waveIdx) { 0 }.toFloat() / 128f)
                            val normalizedAmp = (rawAmp / max(0.15f, trackPeakAll))
                            val gate = 0.25f
                            val cleanedAmp = if (abs(normalizedAmp) < gate) 0f else (normalizedAmp - sign(normalizedAmp) * gate)
                            
                            val dynamicRadius = 260f * 0.7f * effectScale * atan(1.8f * (abs(cleanedAmp) + 0.005f))
                            radiusData[i] = (commonBaseR * 0.98f) + dynamicRadius
                        }
                        for (i in 0 until points) {
                            val angle1 = (i.toFloat() / points) * 2.0 * PI
                            val angle2 = ((i + 1).toFloat() / points) * 2.0 * PI
                            val midAngle = (angle1 + angle2) / 2.0
                            val r1 = radiusData[i]
                            val r2 = radiusData[(i + 1) % points]
                            
                            val x1 = center.x + r1 * cos(angle1).toFloat()
                            val y1 = center.y + r1 * sin(angle1).toFloat()
                            val xMid = center.x + ((r1 + r2) / 2.0f) * cos(midAngle).toFloat()
                            val yMid = center.y + ((r1 + r2) / 2.0f) * sin(midAngle).toFloat()
                            
                            if (i == 0) path.moveTo(x1, y1)
                            path.quadraticBezierTo(x1, y1, xMid, yMid)
                        }
                        path.close()

                        // Use EvenOdd fill to keep center clear and show image
                        val finalPath = Path()
                        finalPath.fillType = PathFillType.EvenOdd
                        finalPath.addPath(path)
                        val circleRect = Rect(center.x - commonBaseR * 0.98f, center.y - commonBaseR * 0.98f, center.x + commonBaseR * 0.98f, center.y + commonBaseR * 0.98f)
                        finalPath.addOval(circleRect)
                        
                        drawPath(finalPath, if (colorMode == VJColorMode.COLORFUL) Color.Magenta else singleColor)
                        drawCircle(if (colorMode == VJColorMode.COLORFUL) Color.Magenta else singleColor, radius = commonBaseR, style = Stroke(width = 2f))
                    }
                    VJStyle.LIQUID -> {
                        // LIQUID: Trap Nation / Avee Player Style (Sharp, Symmetric, FFT-based)
                        val pointsCount = 64 // Use 64 points per half-circle
                        val radiusData = FloatArray(pointsCount)
                        
                        // Process FFT data to get sharp spikes
                        for (i in 0 until pointsCount) {
                            // Map FFT indices to emphasize bass (lower frequencies)
                            // We use a non-linear mapping to grab more bass and fewer highs
                            val fftIdx = (i.toFloat() / pointsCount).pow(1.5f) * (fftData.size / 2)
                            val idx = fftIdx.toInt().coerceIn(0, fftData.size - 1)
                            
                            val rawMag = fftData.getOrElse(idx) { 0f }
                            
                            // Emphasize the spikes
                            val spike = rawMag * 150f * effectScale
                            radiusData[i] = commonBaseR * 0.98f + spike
                        }

                        // We create multiple layers for glow effect
                        val layers = 3
                        val baseColor = if (colorMode == VJColorMode.COLORFUL) Color.Cyan else singleColor
                        
                        for (layer in 0 until layers) {
                            val path = Path()
                            val alpha = 1f - (layer * 0.3f) // Dimmer behind
                            val scaleLayer = 1f + (layer * 0.05f) // Slightly larger behind
                            val blurWidth = if (layer == 0) 2f else 6f * layer // Sharper in front, blurred behind
                            
                            // Draw full circle symmetrically
                            // Go from 6 o'clock (PI/2) to 12 o'clock (-PI/2) for left side
                            // Then from 12 o'clock (-PI/2) back to 6 o'clock (PI/2) for right side
                            for (side in 0..1) {
                                for (i in 0 until pointsCount) {
                                    // Smooth out the bottom and top connections
                                    val smoothFactor = if (i < 3) (i / 3f) else if (i > pointsCount - 4) ((pointsCount - 1 - i) / 3f) else 1f
                                    
                                    val r = commonBaseR * 0.98f + (radiusData[i] - commonBaseR * 0.98f) * smoothFactor * scaleLayer
                                    
                                    val angleOffset = if (liquidFlipped) -PI / 2.0 else PI / 2.0 // Top or Bottom
                                    // Map i to angle: 0 to PI
                                    val progressAngle = (i.toFloat() / (pointsCount - 1)) * PI
                                    val angle = if (side == 0) angleOffset + progressAngle else angleOffset - progressAngle
                                    
                                    val x = center.x + r * cos(angle).toFloat()
                                    val y = center.y + r * sin(angle).toFloat()
                                    
                                    if (side == 0 && i == 0) {
                                        path.moveTo(x, y)
                                    } else {
                                        path.lineTo(x, y) // Use lineTo for sharp spikes
                                    }
                                }
                            }
                            path.close()
                            
                            // Inner cut-out to keep the center clean
                            val finalPath = Path()
                            finalPath.fillType = PathFillType.EvenOdd
                            finalPath.addPath(path)
                            val innerR = commonBaseR * 0.98f - (if (layer == 0) 0f else 2f)
                            val circleRect = Rect(center.x - innerR, center.y - innerR, center.x + innerR, center.y + innerR)
                            finalPath.addOval(circleRect)
                            
                            drawPath(finalPath, baseColor.copy(alpha = alpha))
                            
                            if (layer == 0) {
                                // Draw strong center border only for the top layer
                                drawCircle(baseColor, radius = commonBaseR * 0.98f, style = Stroke(width = blurWidth))
                            }
                        }
                    }
                    VJStyle.NEON_WAVES -> {
                        val colors = if (colorMode == VJColorMode.COLORFUL) listOf(Color.Cyan, Color.Green, Color.Magenta) else listOf(singleColor, singleColor, singleColor)
                        colors.forEachIndexed { index, color ->
                            val path = Path()
                            val phase = (time + (index * 2f)).toDouble()
                            for (i in 0 until 100) {
                                val x = (i.toFloat() / 100f) * size.width
                                val waveVal = sin((i.toDouble() / 10.0) + phase).toFloat()
                                val audioVal = (waveData.getOrElse((i * (waveData.size / 100)) % waveData.size) { 0 }.toInt()) / 128f
                                val y = center.y + (waveVal * 100f) + (audioVal * 150f * effectScale * (index + 1) * 0.5f)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(path, color.copy(alpha = 0.6f), style = Stroke(width = 12f, cap = StrokeCap.Round))
                        }
                    }
                    VJStyle.AURA_HEAT -> {
                        val avgFft = if (fftData.isNotEmpty()) fftData.average().toFloat() else 0f

                        // --- Background aura blobs (timbre-colored) ---
                        val auraColor = if (colorMode == VJColorMode.COLORFUL)
                            Color.hsv(timbreHue, 0.85f, 1f)
                        else singleColor

                        for (i in 0 until 5) {
                            val angle = (time * (i + 1) * 0.18f + i * 72.0).toDouble()
                            val orbitR = 90f + i * 30f
                            val blobR  = (size.minDimension / 4.5f) * (1f + avgFft * 2.5f * effectScale)
                            val blobHue = if (colorMode == VJColorMode.COLORFUL)
                                (timbreHue + i * 54f).mod(360f) else null
                            val blobColor = if (blobHue != null) Color.hsv(blobHue, 0.9f, 1f) else singleColor
                            val offset = Offset(
                                center.x + cos(angle).toFloat() * orbitR * avgFft * 2f,
                                center.y + sin(angle).toFloat() * orbitR * avgFft * 2f
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(blobColor.copy(alpha = 0.55f), Color.Transparent),
                                    center = offset, radius = blobR
                                ),
                                radius = blobR, center = offset
                            )
                        }

                        // --- Central pulsing ring (timbre color, kick reactive) ---
                        val ringR = commonBaseR * (0.6f + avgFft * effectScale)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.Transparent, auraColor.copy(alpha = 0.4f), Color.Transparent),
                                center = center, radius = ringR
                            ),
                            radius = ringR, center = center
                        )

                        // --- Firework particles ---
                        auraParticles.forEach { p ->
                            val pColor = if (colorMode == VJColorMode.COLORFUL)
                                Color.hsv(p.hue, 1f, 1f).copy(alpha = p.life)
                            else singleColor.copy(alpha = p.life)
                            drawCircle(color = pColor, radius = p.sz * p.life, center = Offset(p.x, p.y))
                        }
                    }
                    VJStyle.SPEKTRO -> {
                        val barWidth = size.width / 32f
                        for (i in 0 until 32) {
                            val magnitude = fftData.getOrElse(i) { 0f }
                            val color = if (colorMode == VJColorMode.COLORFUL) Color.hsv((i.toFloat() / 32f) * 360f, 0.7f, 1f) else singleColor
                            val h = magnitude * size.height * 0.4f * effectScale
                            drawRect(color = color, topLeft = Offset(i * barWidth, center.y - h / 2f), size = androidx.compose.ui.geometry.Size(barWidth - 1f, h))
                        }
                    }
                    VJStyle.ALCHEMY -> {
                        rotate(rotation) {
                            val path = Path()
                            for (i in 0 until 64) {
                                val angle = (i.toFloat() / 64f) * 2.0 * PI
                                val r = (size.minDimension / 5f) + (fftData.getOrElse(i) { 0f } * 180f * effectScale)
                                val x = center.x + (r * cos(angle)).toFloat()
                                val y = center.y + (r * sin(angle)).toFloat()
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            path.close()
                            drawPath(path, if (colorMode == VJColorMode.COLORFUL) Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Red), center) else SolidColor(singleColor), style = Stroke(width = 4f))
                        }
                    }
                    VJStyle.SPIKE -> {
                        for (i in 0 until 32) {
                            val angle = (i.toFloat() / 32) * 360f
                            val len = 60f + fftData.getOrElse(i) { 0f } * 350f * effectScale
                            rotate(angle + rotation, center) { drawLine(if (colorMode == VJColorMode.COLORFUL) Color.hsv(angle, 0.8f, 1f) else singleColor, Offset(center.x + 40f, center.y), Offset(center.x + len, center.y), strokeWidth = 4f) }
                        }
                    }
                    VJStyle.BARS -> {
                        val bw = size.width / 32f
                        for (i in 0 until 32) {
                            val bh = fftData.getOrElse(i) { 0f } * size.height * 0.5f * effectScale
                            drawRect(if (colorMode == VJColorMode.COLORFUL) Color.hsv((i.toFloat() / 32) * 360f, 0.6f, 1f).copy(alpha = 0.7f) else singleColor.copy(alpha = 0.7f), Offset(i * bw, size.height - bh), androidx.compose.ui.geometry.Size(bw - 2f, bh))
                        }
                    }
                }
            }
        }
    }
}
