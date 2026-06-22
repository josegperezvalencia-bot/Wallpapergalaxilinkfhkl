package com.example.service

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.room.Room
import com.example.data.WallpaperDatabase
import com.example.data.WallpaperEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

class ParallaxWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return ParallaxEngine()
    }

    inner class ParallaxEngine : Engine(), SensorEventListener {

        private val handler = Handler(Looper.getMainLooper())
        private val drawRunnable = Runnable { drawFrame() }
        private var visible = false

        // Physics variables for smooth tilt response
        private var sensorManager: SensorManager? = null
        private var accelerometer: Sensor? = null
        private var tiltX = 0f
        private var tiltY = 0f
        private var targetTiltX = 0f
        private var targetTiltY = 0f
        private val smoothingFactor = 0.15f

        // Database wallpaper instance
        private var activeWallpaper: WallpaperEntity? = null
        private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

        // Image files cache loader
        private var bgBitmap: Bitmap? = null
        private var mgBitmap: Bitmap? = null
        private var fgBitmap: Bitmap? = null

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            loadActiveWallpaper()
        }

        private fun loadActiveWallpaper() {
            coroutineScope.launch {
                try {
                    val db = Room.databaseBuilder(
                        applicationContext,
                        WallpaperDatabase::class.java,
                        "wallpaper_4d_database"
                    ).build()
                    val papers = db.wallpaperDao().getAllWallpapers().firstOrNull()
                    if (!papers.isNullOrEmpty()) {
                        activeWallpaper = papers.first() // load the most recently saved wallpaper
                        cacheLayerBitmaps()
                    } else {
                        // Fallback default setup
                        activeWallpaper = WallpaperEntity(
                            title = "Predeterminado 4D",
                            bgPresetKey = "space",
                            mgPresetKey = "space",
                            fgPresetKey = "space"
                        )
                    }
                    db.close()
                } catch (e: Exception) {
                    Log.e("ParallaxWallpaperService", "Error loading live wallpaper configuration", e)
                }
            }
        }

        private fun cacheLayerBitmaps() {
            val config = activeWallpaper ?: return
            
            // Decodes local custom images to bitmaps if active
            bgBitmap = config.bgPath?.let { path -> decodeFileToBitmap(path) }
            mgBitmap = config.mgPath?.let { path -> decodeFileToBitmap(path) }
            fgBitmap = config.fgPath?.let { path -> decodeFileToBitmap(path) }
        }

        private fun decodeFileToBitmap(path: String): Bitmap? {
            val file = File(path)
            if (!file.exists()) return null
            return try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (e: Exception) {
                Log.e("ParallaxWallpaperService", "Failed to decode bitmap from $path", e)
                null
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
                loadActiveWallpaper() // Reload latest config upon screen awake
                handler.post(drawRunnable)
            } else {
                sensorManager?.unregisterListener(this)
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunnable)
            sensorManager?.unregisterListener(this)
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
            
            val rawAx = event.values[0]
            val rawAy = event.values[1]
            
            // Map tilt coordinates to fit confortable ranges
            targetTiltX = -rawAx * 2.2f
            targetTiltY = rawAy * 2.2f
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    // Update smooth motion calculations first
                    tiltX += (targetTiltX - tiltX) * smoothingFactor
                    tiltY += (targetTiltY - tiltY) * smoothingFactor

                    val wallpaper = activeWallpaper ?: WallpaperEntity(title = "Default")
                    
                    // 1. DRAW BACKGROUND
                    val bgShiftX = tiltX * wallpaper.bgDepth * 0.4f
                    val bgShiftY = tiltY * wallpaper.bgDepth * 0.4f
                    canvas.drawColor(Color.BLACK) // background color clearing
                    
                    canvas.save()
                    canvas.translate(bgShiftX, bgShiftY)
                    bgBitmap?.let {
                        drawCenteredScaledBitmap(canvas, it, wallpaper.bgScale)
                    } ?: run {
                        drawProceduralBackground(canvas, wallpaper.bgPresetKey)
                    }
                    canvas.restore()

                    // 2. DRAW MIDGROUND
                    val mgShiftX = tiltX * wallpaper.mgDepth * 0.4f
                    val mgShiftY = tiltY * wallpaper.mgDepth * 0.4f
                    
                    canvas.save()
                    canvas.translate(mgShiftX, mgShiftY)
                    mgBitmap?.let {
                        drawCenteredScaledBitmap(canvas, it, wallpaper.mgScale, wallpaper.mgCropShape)
                    } ?: run {
                        drawProceduralMidground(canvas, wallpaper.mgPresetKey)
                    }
                    canvas.restore()

                    // 3. DRAW FOREGROUND
                    val fgShiftX = tiltX * wallpaper.fgDepth * 0.4f
                    val fgShiftY = tiltY * wallpaper.fgDepth * 0.4f
                    
                    canvas.save()
                    canvas.translate(fgShiftX, fgShiftY)
                    fgBitmap?.let {
                        drawCenteredScaledBitmap(canvas, it, wallpaper.fgScale, wallpaper.fgCropShape)
                    } ?: run {
                        drawProceduralForeground(canvas, wallpaper.fgPresetKey)
                    }
                    canvas.restore()
                }
            } catch (e: Exception) {
                Log.e("ParallaxWallpaperService", "Error during frame render", e)
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        // ignore error
                    }
                }
            }

            if (visible) {
                handler.postDelayed(drawRunnable, 16) // tick target at 60fps
            }
        }

        private fun drawCenteredScaledBitmap(
            canvas: Canvas, 
            bitmap: Bitmap, 
            scale: Float,
            cropShape: String = "NONE"
        ) {
            val cWidth = canvas.width.toFloat()
            val cHeight = canvas.height.toFloat()
            
            val scaleFactor = (cWidth / bitmap.width).coerceAtLeast(cHeight / bitmap.height) * scale
            val finalWidth = bitmap.width * scaleFactor
            val finalHeight = bitmap.height * scaleFactor
            
            val left = (cWidth - finalWidth) / 2
            val top = (cHeight - finalHeight) / 2
            val rect = RectF(left, top, left + finalWidth, top + finalHeight)
            
            if (cropShape == "CIRCLE") {
                canvas.save()
                val path = Path()
                path.addCircle(cWidth / 2f, cHeight / 2f, (cWidth * 0.35f), Path.Direction.CW)
                canvas.clipPath(path)
            } else if (cropShape == "ROUNDED_RECT") {
                canvas.save()
                val path = Path()
                path.addRoundRect(
                    RectF(cWidth * 0.15f, cHeight * 0.25f, cWidth * 0.85f, cHeight * 0.75f),
                    64f, 64f, Path.Direction.CW
                )
                canvas.clipPath(path)
            }
            
            // Draw
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(bitmap, null, rect, paint)
            
            if (cropShape != "NONE") {
                canvas.restore()
            }
        }

        private fun drawProceduralBackground(canvas: Canvas, presetKey: String) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            val paint = Paint()
            
            if (presetKey == "space") {
                val radialColors = intArrayOf(Color.parseColor("#1B0B36"), Color.parseColor("#030107"))
                paint.shader = RadialGradient(w / 2, h / 2, w * 1.5f, radialColors, null, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, w, h, paint)
                
                // Static stars representation
                paint.shader = null
                paint.color = Color.WHITE
                for (i in 0..40) {
                    val starX = (i * 73) % w
                    val starY = (i * 127) % h
                    canvas.drawCircle(starX, starY, 3f, paint)
                }
            } else if (presetKey == "cyberpunk") {
                val linearColors = intArrayOf(Color.parseColor("#0D031A"), Color.parseColor("#330033"))
                paint.shader = LinearGradient(0f, 0f, 0f, h, linearColors, null, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, w, h, paint)
                
                // Horizon line and grid
                paint.shader = null
                paint.color = Color.parseColor("#FF00CC")
                canvas.drawLine(0f, h * 0.75f, w, h * 0.75f, paint)
            } else {
                paint.color = Color.parseColor("#11141E")
                canvas.drawRect(0f, 0f, w, h, paint)
            }
        }

        private fun drawProceduralMidground(canvas: Canvas, presetKey: String) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            val paint = Paint()
            paint.isAntiAlias = true
            
            if (presetKey == "space") {
                // Large planet with rings centered
                val grad = LinearGradient(
                    w * 0.35f, h * 0.45f, w * 0.65f, h * 0.55f,
                    Color.parseColor("#7B2CBF"), Color.parseColor("#240046"), Shader.TileMode.CLAMP
                )
                paint.shader = grad
                canvas.drawCircle(w * 0.5f, h * 0.5f, w * 0.22f, paint)
                
                // Ring drawing outline
                paint.shader = null
                paint.color = Color.parseColor("#FF007A")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 14f
                val oval = RectF(w * 0.2f, h * 0.47f, w * 0.8f, h * 0.53f)
                canvas.drawArc(oval, -15f, 210f, false, paint)
            } else if (presetKey == "cyberpunk") {
                paint.color = Color.parseColor("#FF007A")
                canvas.drawCircle(w * 0.5f, h * 0.45f, w * 0.16f, paint)
                
                paint.color = Color.parseColor("#0D031A")
                paint.style = Paint.Style.FILL
                for (i in 0..5) {
                    canvas.drawRect(w * 0.3f, h * 0.42f + (i * 20), w * 0.7f, h * 0.42f + (i * 20) + 6, paint)
                }
            } else {
                // Simple modern ring design
                paint.color = Color.parseColor("#00FFCC")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 6f
                canvas.drawCircle(w * 0.5f, h * 0.5f, w * 0.2f, paint)
            }
        }

        private fun drawProceduralForeground(canvas: Canvas, presetKey: String) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            val paint = Paint()
            paint.isAntiAlias = true
            
            if (presetKey == "space") {
                // Central floating geometric crystal satellite
                paint.color = Color.parseColor("#E2EAFC")
                paint.style = Paint.Style.FILL
                canvas.drawRect(w * 0.46f, h * 0.46f, w * 0.54f, h * 0.54f, paint)
                
                paint.color = Color.parseColor("#00FFFF")
                canvas.drawCircle(w * 0.5f, h * 0.5f, 10f, paint)
            } else if (presetKey == "cyberpunk") {
                // Neon futuristic speeder silhouette
                paint.color = Color.parseColor("#120324")
                val path = Path()
                path.moveTo(w * 0.35f, h * 0.5f)
                path.lineTo(w * 0.65f, h * 0.5f)
                path.lineTo(w * 0.6f, h * 0.54f)
                path.lineTo(w * 0.4f, h * 0.54f)
                path.close()
                canvas.drawPath(path, paint)
            } else {
                // Accent overlapping double triangles
                val path = Path()
                path.moveTo(w * 0.5f, h * 0.42f)
                path.lineTo(w * 0.58f, h * 0.54f)
                path.lineTo(w * 0.42f, h * 0.54f)
                path.close()
                
                paint.color = Color.WHITE
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                canvas.drawPath(path, paint)
            }
        }
    }
}
