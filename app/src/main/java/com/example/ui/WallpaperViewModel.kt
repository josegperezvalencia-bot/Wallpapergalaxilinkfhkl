package com.example.ui

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.WallpaperEntity
import com.example.data.WallpaperRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

class WallpaperViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val repository = WallpaperRepository(application)

    // List of saved designs
    val savedWallpapers: StateFlow<List<WallpaperEntity>> = repository.allWallpapers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current design in the creator/editor
    private val _editingWallpaper = MutableStateFlow(
        WallpaperEntity(
            title = "Mi Creación 4D",
            bgPresetKey = "space",
            bgDepth = -20f,
            bgScale = 1.25f,
            mgPresetKey = "space",
            mgDepth = 15f,
            mgScale = 1.0f,
            fgPresetKey = "space",
            fgDepth = 45f,
            fgScale = 1.0f
        )
    )
    val editingWallpaper: StateFlow<WallpaperEntity> = _editingWallpaper.asStateFlow()

    // Screen tabs: "creator" or "my_wallpapers"
    private val _currentTab = MutableStateFlow("creator")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Selected layer mode for gestures & edit sliders: "BG", "MG", "FG"
    private val _selectedLayer = MutableStateFlow("MG")
    val selectedLayer: StateFlow<String> = _selectedLayer.asStateFlow()

    // Smooth filtered tilt values used directly by rendering Canvas
    private val _tiltX = MutableStateFlow(0f)
    val tiltX: StateFlow<Float> = _tiltX.asStateFlow()

    private val _tiltY = MutableStateFlow(0f)
    val tiltY: StateFlow<Float> = _tiltY.asStateFlow()

    // Simulator to rotate wallpaper automatically in an orbit
    private val _isSimulatorEnabled = MutableStateFlow(true)
    val isSimulatorEnabled: StateFlow<Boolean> = _isSimulatorEnabled.asStateFlow()

    // Sensor Manager
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Smoothing filter targets
    private var targetTiltX = 0f
    private var targetTiltY = 0f
    private val smoothingFactor = 0.15f // Low pass filter coefficient

    private var simulatorJob: Job? = null
    private var sensorActive = false

    init {
        startSimulator()
        startSensorMonitoring()
    }

    private fun startSensorMonitoring() {
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            sensorActive = true
            Log.d("WallpaperViewModel", "Registered accelerometer sensor listener.")
        } else {
            Log.w("WallpaperViewModel", "SensorManager or Accelerometer not available.")
        }
    }

    private fun startSimulator() {
        simulatorJob?.cancel()
        simulatorJob = viewModelScope.launch {
            var angle = 0f
            while (true) {
                if (_isSimulatorEnabled.value) {
                    // Update angle for circular tilting orbit simulation
                    angle += 0.03f
                    // Range of horizontal and vertical tilt
                    val simX = sin(angle) * 12f
                    val simY = cos(angle * 0.7f) * 12f
                    
                    // If sensor is not active or device is flat, blend simulation
                    if (!sensorActive) {
                        targetTiltX = simX
                        targetTiltY = simY
                    } else {
                        // If sensor is active, blend some slight simulation if requested
                        // but prioritize sensor below
                    }
                }
                
                // Low pass filter tick
                _tiltX.value += (targetTiltX - _tiltX.value) * smoothingFactor
                _tiltY.value += (targetTiltY - _tiltY.value) * smoothingFactor
                
                delay(16) // ~60fps rendering frame tick
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val rawAx = event.values[0] // X axis acceleration
            val rawAy = event.values[1] // Y axis acceleration
            
            // Adjust calculations to map acceleration to comfortable tilt offsets (-15 to 15 deg)
            // Negative X because screen tilts opposite to acceleration direction
            val pitch = -rawAx * 2.2f
            val roll = rawAy * 2.2f
            
            if (!_isSimulatorEnabled.value) {
                targetTiltX = pitch.coerceIn(-25f, 25f)
                targetTiltY = roll.coerceIn(-25f, 25f)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun setSimulatorEnabled(enabled: Boolean) {
        _isSimulatorEnabled.value = enabled
        if (enabled && !sensorActive) {
            // Re-trigger simulator if we had stopped it
        }
    }

    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    fun selectLayer(layer: String) {
        _selectedLayer.value = layer
    }

    // Set a custom tile title
    fun updateTitle(newTitle: String) {
        _editingWallpaper.value = _editingWallpaper.value.copy(title = newTitle)
    }

    // Update active layer's parameters dynamically
    fun updateLayerScale(scale: Float) {
        val current = _editingWallpaper.value
        _editingWallpaper.value = when (_selectedLayer.value) {
            "BG" -> current.copy(bgScale = scale.coerceIn(0.5f, 3f))
            "MG" -> current.copy(mgScale = scale.coerceIn(0.5f, 3f))
            else -> current.copy(fgScale = scale.coerceIn(0.5f, 3f))
        }
    }

    fun updateLayerDepth(depth: Float) {
        val current = _editingWallpaper.value
        _editingWallpaper.value = when (_selectedLayer.value) {
            "BG" -> current.copy(bgDepth = depth.coerceIn(-80f, 80f))
            "MG" -> current.copy(mgDepth = depth.coerceIn(-80f, 80f))
            else -> current.copy(fgDepth = depth.coerceIn(-80f, 80f))
        }
    }

    fun updateLayerOffset(dx: Float, dy: Float) {
        val current = _editingWallpaper.value
        _editingWallpaper.value = when (_selectedLayer.value) {
            "BG" -> current.copy(bgOffsetX = current.bgOffsetX + dx, bgOffsetY = current.bgOffsetY + dy)
            "MG" -> current.copy(mgOffsetX = current.mgOffsetX + dx, mgOffsetY = current.mgOffsetY + dy)
            else -> current.copy(fgOffsetX = current.fgOffsetX + dx, fgOffsetY = current.fgOffsetY + dy)
        }
    }

    fun updateLayerPreset(presetKey: String) {
        val current = _editingWallpaper.value
        _editingWallpaper.value = when (_selectedLayer.value) {
            "BG" -> current.copy(bgPresetKey = presetKey, bgPath = null)
            "MG" -> current.copy(mgPresetKey = presetKey, mgPath = null)
            else -> current.copy(fgPresetKey = presetKey, fgPath = null)
        }
    }

    fun updateLayerCropShape(shape: String) {
        val current = _editingWallpaper.value
        _editingWallpaper.value = when (_selectedLayer.value) {
            "BG" -> current // bg usually doesn't need crops
            "MG" -> current.copy(mgCropShape = shape)
            else -> current.copy(fgCropShape = shape)
        }
    }

    // Assign custom local image URI to active layer
    fun assignLocalImageToActiveLayer(uriStr: String) {
        viewModelScope.launch {
            val copiedPath = repository.saveImageToInternalStorage(uriStr)
            if (copiedPath.isNotEmpty()) {
                val current = _editingWallpaper.value
                _editingWallpaper.value = when (_selectedLayer.value) {
                    "BG" -> current.copy(bgPath = copiedPath, bgPresetKey = "custom")
                    "MG" -> current.copy(mgPath = copiedPath, mgPresetKey = "custom")
                    else -> current.copy(fgPath = copiedPath, fgPresetKey = "custom")
                }
                Log.d("WallpaperViewModel", "Assigned internal copy $copiedPath to ${_selectedLayer.value}")
            }
        }
    }

    fun resetActiveLayerOffsets() {
        val current = _editingWallpaper.value
        _editingWallpaper.value = when (_selectedLayer.value) {
            "BG" -> current.copy(bgOffsetX = 0f, bgOffsetY = 0f, bgScale = 1.25f, bgDepth = -20f)
            "MG" -> current.copy(mgOffsetX = 0f, mgOffsetY = 0f, mgScale = 1.00f, mgDepth = 15f)
            else -> current.copy(fgOffsetX = 0f, fgOffsetY = 0f, fgScale = 1.00f, fgDepth = 45f)
        }
    }

    // Load full wallpaper entity into editor
    fun loadWallpaperIntoEditor(wallpaper: WallpaperEntity) {
        _editingWallpaper.value = wallpaper
        _currentTab.value = "creator"
    }

    // Database Actions
    fun saveCurrentWallpaper() {
        viewModelScope.launch {
            val current = _editingWallpaper.value
            val savedId = repository.insertWallpaper(current)
            // Update current with generated ID so edits modify it instead of creating copies
            _editingWallpaper.value = current.copy(id = savedId)
            Log.d("WallpaperViewModel", "Saved wallpaper with ID: $savedId")
        }
    }

    fun deleteWallpaper(wallpaper: WallpaperEntity) {
        viewModelScope.launch {
            repository.deleteWallpaper(wallpaper)
            // If we deleted the active editing wallpaper, reset it
            if (_editingWallpaper.value.id == wallpaper.id) {
                _editingWallpaper.value = _editingWallpaper.value.copy(id = 0)
            }
        }
    }

    fun deleteWallpaperById(id: Long) {
        viewModelScope.launch {
            repository.deleteWallpaperById(id)
            if (_editingWallpaper.value.id == id) {
                _editingWallpaper.value = _editingWallpaper.value.copy(id = 0)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        simulatorJob?.cancel()
        if (sensorActive) {
            sensorManager?.unregisterListener(this)
        }
    }
}
