package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.WallpaperEntity
import com.example.ui.WallpaperViewModel
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun WallpaperCanvas(
    viewModel: WallpaperViewModel,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = true
) {
    val wallpaper by viewModel.editingWallpaper.collectAsState()
    val tiltX by viewModel.tiltX.collectAsState()
    val tiltY by viewModel.tiltY.collectAsState()
    val selectedLayer by viewModel.selectedLayer.collectAsState()

    // Stars & Fireflies random coordinates generated once
    val starCoords = remember { List(45) { Offset((0..1000).random().toFloat(), (0..2000).random().toFloat()) } }
    val fireflyCoords = remember { List(25) { Offset((0..1000).random().toFloat(), (1000..2000).random().toFloat()) } }
    val starSizes = remember { List(45) { (2..5).random().toFloat() } }
    
    // Gestures mapping
    val gestureModifier = if (isEditMode) {
        Modifier.pointerInput(selectedLayer) {
            detectTransformGestures { _, pan, zoom, _ ->
                if (zoom != 1f) {
                    val currentScale = when (selectedLayer) {
                        "BG" -> wallpaper.bgScale
                        "MG" -> wallpaper.mgScale
                        else -> wallpaper.fgScale
                    }
                    viewModel.updateLayerScale(currentScale * zoom)
                }
                viewModel.updateLayerOffset(pan.x / density, pan.y / density)
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .then(gestureModifier)
    ) {
        // --- 1. BACKGROUND LAYER ---
        val bgParallaxX = (tiltX * wallpaper.bgDepth * 0.4f) + wallpaper.bgOffsetX
        val bgParallaxY = (tiltY * wallpaper.bgDepth * 0.4f) + wallpaper.bgOffsetY
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = bgParallaxX
                    translationY = bgParallaxY
                    scaleX = wallpaper.bgScale
                    scaleY = wallpaper.bgScale
                }
        ) {
            if (wallpaper.bgPath != null) {
                // Render local custom photo
                AsyncImage(
                    model = File(wallpaper.bgPath),
                    contentDescription = "Fondo Personalizado",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Render procedural preset background
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawPresetBackground(wallpaper.bgPresetKey, starCoords, starSizes)
                }
            }
        }

        // --- 2. MIDGROUND LAYER ---
        val mgParallaxX = (tiltX * wallpaper.mgDepth * 0.4f) + wallpaper.mgOffsetX
        val mgParallaxY = (tiltY * wallpaper.mgDepth * 0.4f) + wallpaper.mgOffsetY
        
        val mgCropModifier = when (wallpaper.mgCropShape) {
            "CIRCLE" -> Modifier.clip(CircleShape)
            "ROUNDED_RECT" -> Modifier.clip(RoundedCornerShape(32.dp))
            else -> Modifier
        }

        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.Center)
                .graphicsLayer {
                    translationX = mgParallaxX
                    translationY = mgParallaxY
                    scaleX = wallpaper.mgScale
                    scaleY = wallpaper.mgScale
                }
                .then(mgCropModifier)
        ) {
            if (wallpaper.mgPath != null) {
                AsyncImage(
                    model = File(wallpaper.mgPath),
                    contentDescription = "Capa Media Personalizada",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawPresetMidground(wallpaper.mgPresetKey)
                }
            }
        }

        // --- 3. FOREGROUND LAYER ---
        val fgParallaxX = (tiltX * wallpaper.fgDepth * 0.4f) + wallpaper.fgOffsetX
        val fgParallaxY = (tiltY * wallpaper.fgDepth * 0.4f) + wallpaper.fgOffsetY
        
        val fgCropModifier = when (wallpaper.fgCropShape) {
            "CIRCLE" -> Modifier.clip(CircleShape)
            "ROUNDED_RECT" -> Modifier.clip(RoundedCornerShape(32.dp))
            else -> Modifier
        }

        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.Center)
                .graphicsLayer {
                    translationX = fgParallaxX
                    translationY = fgParallaxY
                    scaleX = wallpaper.fgScale
                    scaleY = wallpaper.fgScale
                }
                .then(fgCropModifier)
        ) {
            if (wallpaper.fgPath != null) {
                AsyncImage(
                    model = File(wallpaper.fgPath),
                    contentDescription = "Primer Plano Personalizado",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawPresetForeground(wallpaper.fgPresetKey, fireflyCoords)
                }
            }
        }
    }
}

// Draw internal preset backgrounds procedurally
private fun DrawScope.drawPresetBackground(presetKey: String, starCoords: List<Offset>, starSizes: List<Float>) {
    when (presetKey) {
        "space" -> {
            // Dark space gradient
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1B0B36), Color(0xFF030107)),
                    center = center,
                    radius = size.width * 1.5f
                ),
                size = size
            )
            // Stars
            starCoords.forEachIndexed { i, coord ->
                val scaleFactor = starSizes[i]
                drawCircle(
                    color = Color.White.copy(alpha = 0.75f),
                    radius = scaleFactor,
                    center = Offset(coord.x % size.width, coord.y % size.height)
                )
            }
            // Planetary glow nebula
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF007A).copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(size.width * 0.7f, size.height * 0.3f),
                    radius = size.width * 0.6f
                ),
                center = Offset(size.width * 0.7f, size.height * 0.3f),
                radius = size.width * 0.6f
            )
        }
        "cyberpunk" -> {
            // Retro wave colors
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0D031A), Color(0xFF330033)),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height)
                ),
                size = size
            )
            
            // Sunset grid horizon
            val gridY = size.height * 0.75f
            drawLine(
                color = Color(0xFFFF00CC),
                start = Offset(0f, gridY),
                end = Offset(size.width, gridY),
                strokeWidth = 3f
            )
            
            // Neon vertical guide lines
            for (i in -4..12) {
                val startX = size.width / 2 + (i * (size.width / 8))
                val endX = size.width / 2 + (i * (size.width / 4))
                drawLine(
                    color = Color(0xFF00FFFF).copy(alpha = 0.4f),
                    start = Offset(startX, gridY),
                    end = Offset(endX, size.height),
                    strokeWidth = 2f
                )
            }
        }
        "forest" -> {
            // Midnight fog gradient
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0D1E16), Color(0xFF050A0B)),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height)
                ),
                size = size
            )
            // Big luminous full moon
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE8F1F5), Color(0xFFCEEAE0).copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.35f),
                    radius = size.width * 0.4f
                ),
                center = Offset(size.width * 0.5f, size.height * 0.35f),
                radius = size.width * 0.4f
            )
            drawCircle(
                color = Color(0xFFF6FAF9).copy(alpha = 0.9f),
                center = Offset(size.width * 0.5f, size.height * 0.35f),
                radius = size.width * 0.2f
            )
        }
        else -> {
            // Clean minimal gradient background
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF1E2638), Color(0xFF11141E))
                ),
                size = size
            )
        }
    }
}

// Draw layer midgrounds procedurally
private fun DrawScope.drawPresetMidground(presetKey: String) {
    val w = size.width
    val h = size.height
    when (presetKey) {
        "space" -> {
            // Deep neon magenta ringed gas giant
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF7B2CBF), Color(0xFF240046), Color(0xFF03001C))
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.32f
            )
            
            // Planetary rings draw
            val ringPath = Path().apply {
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        left = -w * 0.15f,
                        top = h * 0.42f,
                        right = w * 1.15f,
                        bottom = h * 0.58f
                    ),
                    startAngleDegrees = -15f,
                    sweepAngleDegrees = 210f,
                    forceMoveTo = true
                )
            }
            drawPath(
                path = ringPath,
                color = Color(0xFFFF007A).copy(alpha = 0.82f),
                style = Stroke(width = w * 0.04f)
            )
            drawPath(
                path = ringPath,
                color = Color(0xFF00FFCC).copy(alpha = 0.4f),
                style = Stroke(width = w * 0.06f)
            )
        }
        "cyberpunk" -> {
            // Distant retro wireframe sun and high buildings
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFF007A), Color(0xFFFFD700))
                ),
                center = Offset(w * 0.5f, h * 0.45f),
                radius = w * 0.28f
            )
            // Cyberpunk sun lines
            for (i in 0..7) {
                val blockY = h * 0.38f + (i * (w * 0.05f))
                if (blockY < h * 0.55f) {
                    drawRect(
                        color = Color(0xFF23023D),
                        topLeft = Offset(w * 0.15f, blockY),
                        size = Size(w * 0.7f, w * 0.015f)
                    )
                }
            }
            
            // Cyberpunk building outlines
            drawRect(
                color = Color(0xFF0D031A),
                topLeft = Offset(w * 0.2f, h * 0.5f),
                size = Size(w * 0.25f, h * 0.5f)
            )
            drawRect(
                color = Color(0xFF05010B),
                topLeft = Offset(w * 0.55f, h * 0.42f),
                size = Size(w * 0.3f, h * 0.58f)
            )
            // Neon building borders
            drawLine(
                color = Color(0xFF00FFFF),
                start = Offset(w * 0.2f, h * 0.5f),
                end = Offset(w * 0.45f, h * 0.5f),
                strokeWidth = 3f
            )
            drawLine(
                color = Color(0xFFFF00AA),
                start = Offset(w * 0.55f, h * 0.42f),
                end = Offset(w * 0.85f, h * 0.42f),
                strokeWidth = 3f
            )
        }
        "forest" -> {
            // Silhouettes of beautiful branching trees
            val treePath = Path().apply {
                moveTo(w * 0.3f, h)
                lineTo(w * 0.35f, h * 0.4f) // Main trunk
                // Branches
                moveTo(w * 0.34f, h * 0.55f)
                quadraticTo(w * 0.2f, h * 0.45f, w * 0.15f, h * 0.48f)
                moveTo(w * 0.35f, h * 0.48f)
                quadraticTo(w * 0.5f, h * 0.38f, w * 0.55f, h * 0.4f)
                
                // Base
                moveTo(w * 0.3f, h)
                lineTo(w * 0.42f, h)
                close()
            }
            drawPath(
                path = treePath,
                color = Color(0xFF07110D),
                style = Stroke(width = w * 0.05f)
            )
            
            // Soft foliage blobs
            drawCircle(Color(0xFF0C1D16).copy(alpha = 0.95f), center = Offset(w * 0.18f, h * 0.46f), radius = w * 0.15f)
            drawCircle(Color(0xFF081510).copy(alpha = 0.95f), center = Offset(w * 0.54f, h * 0.38f), radius = w * 0.18f)
        }
        else -> {
            // Default elegant minimal intersecting glass rings
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.3f,
                style = Stroke(width = 6f)
            )
            drawCircle(
                color = Color(0xFF00FFCC).copy(alpha = 0.3f),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.25f,
                style = Stroke(width = 2f)
            )
        }
    }
}

// Draw layer foregrounds procedurally
private fun DrawScope.drawPresetForeground(presetKey: String, fireflyCoords: List<Offset>) {
    val w = size.width
    val h = size.height
    when (presetKey) {
        "space" -> {
            // Draw a cute minimal astronaut helmet or satellite floating elegantly
            val satCenter = Offset(w * 0.5f, h * 0.5f)
            
            // Satellite main body
            drawRoundRect(
                brush = Brush.linearGradient(colors = listOf(Color(0xFFE2EAFC), Color(0xFF819BCC))),
                topLeft = Offset(satCenter.x - w * 0.12f, satCenter.y - w * 0.12f),
                size = Size(w * 0.24f, w * 0.24f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f)
            )
            
            // Glowing visor / lens
            drawCircle(
                brush = Brush.radialGradient(colors = listOf(Color(0xFF00FFFF), Color(0xFF0055AA))),
                center = satCenter,
                radius = w * 0.07f
            )
            
            // Blue solar wings
            drawRect(
                color = Color(0xFF00B4D8).copy(alpha = 0.85f),
                topLeft = Offset(satCenter.x - w * 0.38f, satCenter.y - w * 0.04f),
                size = Size(w * 0.22f, w * 0.08f)
            )
            drawRect(
                color = Color(0xFF00B4D8).copy(alpha = 0.85f),
                topLeft = Offset(satCenter.x + w * 0.16f, satCenter.y - w * 0.04f),
                size = Size(w * 0.22f, w * 0.08f)
            )
            
            // Wing grids
            drawLine(Color.White.copy(alpha = 0.4f), Offset(satCenter.x - w * 0.27f, satCenter.y - w * 0.04f), Offset(satCenter.x - w * 0.27f, satCenter.y + w * 0.04f))
            drawLine(Color.White.copy(alpha = 0.4f), Offset(satCenter.x + w * 0.27f, satCenter.y - w * 0.04f), Offset(satCenter.x + w * 0.27f, satCenter.y + w * 0.04f))
        }
        "cyberpunk" -> {
            // Neon floating flyer / car speeder speeding across the foreground
            val carPath = Path().apply {
                moveTo(w * 0.18f, h * 0.5f)
                lineTo(w * 0.82f, h * 0.5f)
                lineTo(w * 0.75f, h * 0.58f)
                lineTo(w * 0.25f, h * 0.58f)
                close()
            }
            drawPath(
                brush = Brush.horizontalGradient(colors = listOf(Color(0xFF120324), Color(0xFF330033))),
                path = carPath
            )
            
            // Glowing cyan tail lights line
            drawLine(
                color = Color(0xFF00FFFF),
                start = Offset(w * 0.22f, h * 0.51f),
                end = Offset(w * 0.45f, h * 0.51f),
                strokeWidth = 5f
            )
            // Thruster fire pink glow
            drawLine(
                color = Color(0xFFFF007A),
                start = Offset(w * 0.14f, h * 0.53f),
                end = Offset(w * 0.22f, h * 0.53f),
                strokeWidth = 6f
            )
        }
        "forest" -> {
            // Hanging forest moss/vines creeping on boundaries + drifting firefly dots
            val vinePath = Path().apply {
                moveTo(0f, 0f)
                quadraticTo(w * 0.25f, h * 0.35f, w * 0.45f, 0f)
                moveTo(w * 0.4f, 0f)
                quadraticTo(w * 0.72f, h * 0.45f, w, 0f)
            }
            drawPath(
                path = vinePath,
                color = Color(0xFF030705),
                style = Stroke(width = w * 0.04f)
            )
            
            // Fireflies
            fireflyCoords.forEach { coord ->
                drawCircle(
                    color = Color(0xFFADFF2F).copy(alpha = 0.78f),
                    radius = 4f,
                    center = Offset(coord.x % w, coord.y % h)
                )
                drawCircle(
                    color = Color(0xFFADFF2F).copy(alpha = 0.2f),
                    radius = 12f,
                    center = Offset(coord.x % w, coord.y % h)
                )
            }
        }
        else -> {
            // Central geometric design: interactive crystal triangles with rotating depths
            val crystalPath = Path().apply {
                moveTo(w * 0.5f, h * 0.28f)
                lineTo(w * 0.65f, h * 0.58f)
                lineTo(w * 0.35f, h * 0.58f)
                close()
                
                moveTo(w * 0.5f, h * 0.72f)
                lineTo(w * 0.65f, h * 0.42f)
                lineTo(w * 0.35f, h * 0.42f)
                close()
            }
            drawPath(
                brush = Brush.verticalGradient(colors = listOf(Color(0xFF00FFCC).copy(alpha = 0.38f), Color(0xFFFF00CC).copy(alpha = 0.38f))),
                path = crystalPath
            )
            drawPath(
                path = crystalPath,
                color = Color.White.copy(alpha = 0.61f),
                style = Stroke(width = 3f)
            )
        }
    }
}
