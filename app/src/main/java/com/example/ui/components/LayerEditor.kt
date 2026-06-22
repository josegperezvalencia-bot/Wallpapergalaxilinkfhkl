package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WallpaperEntity
import com.example.ui.WallpaperViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayerEditor(
    viewModel: WallpaperViewModel,
    modifier: Modifier = Modifier
) {
    val wallpaper by viewModel.editingWallpaper.collectAsState()
    val selectedLayer by viewModel.selectedLayer.collectAsState()
    val isSimulating by viewModel.isSimulatorEnabled.collectAsState()
    val context = LocalContext.current

    // Set up Photo Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.assignLocalImageToActiveLayer(it.toString())
        }
    }

    // Get current parameters depending on active edited layer
    val (currentScale, currentDepth, currentShape, currentPreset) = when (selectedLayer) {
        "BG" -> quadruple(wallpaper.bgScale, wallpaper.bgDepth, "NONE", wallpaper.bgPresetKey)
        "MG" -> quadruple(wallpaper.mgScale, wallpaper.mgDepth, wallpaper.mgCropShape, wallpaper.mgPresetKey)
        else -> quadruple(wallpaper.fgScale, wallpaper.fgDepth, wallpaper.fgCropShape, wallpaper.fgPresetKey)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("layer_editor_card"),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF13111C).copy(alpha = 0.94f)
        ),
        border = BorderStroke(1.dp, Color(0xFF2C2543).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Drag handle decorator
            Box(
                modifier = Modifier
                    .size(48.dp, 4.dp)
                    .background(Color(0xFF4C3E75), CircleShape)
                    .align(Alignment.CenterHorizontally)
            )

            // Dynamic Title / Title Input Edit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                var isEditingTitle by remember { mutableStateOf(false) }
                var tempTitle by remember(wallpaper.title) { mutableStateOf(wallpaper.title) }

                if (isEditingTitle) {
                    OutlinedTextField(
                        value = tempTitle,
                        onValueChange = { tempTitle = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wallpaper_title_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF9F7FF4),
                            unfocusedBorderColor = Color(0xFF4C3E75)
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (tempTitle.isNotBlank()) {
                                        viewModel.updateTitle(tempTitle)
                                    }
                                    isEditingTitle = false
                                }
                            ) {
                                Text("OK", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                } else {
                    Text(
                        text = wallpaper.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isEditingTitle = true }
                    )
                }

                // Simulate device tilt toggle button
                Button(
                    onClick = { viewModel.setSimulatorEnabled(!isSimulating) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSimulating) Color(0xFF1F1B35) else Color(0xFFFF007A).copy(alpha = 0.15f),
                        contentColor = if (isSimulating) Color(0xFFB5A7DF) else Color(0xFFFF45A3)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, if (isSimulating) Color(0xFF4C3E75) else Color(0xFFFF007A))
                ) {
                    Text(
                        text = if (isSimulating) "Simulación: On" else "Gravedad: On",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Segmented active layer selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1A30), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    "BG" to "Fondo",
                    "MG" to "Medio",
                    "FG" to "Frente"
                ).forEach { (code, label) ->
                    val isActive = selectedLayer == code
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isActive) Color(0xFF6B4EE0) else Color.Transparent)
                            .clickable { viewModel.selectLayer(code) }
                            .padding(vertical = 10.dp)
                            .testTag("layer_tab_$code"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isActive) Color.White else Color(0xFF9E95C5),
                            fontSize = 13.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Option Selector Title
            Text(
                text = "Recorte & Texturas (${when(selectedLayer) { "BG" -> "Fondo" "MG" -> "Capa Media" else -> "Capa Frontal" }})",
                color = Color(0xFFA59ABB),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            // Presets and Upload button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Outer Space preset selector
                OutlinedButton(
                    onClick = { viewModel.updateLayerPreset("space") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (currentPreset == "space") Color(0xFF00FFCC) else Color.White
                    ),
                    border = BorderStroke(1.dp, if (currentPreset == "space") Color(0xFF00FFCC) else Color(0xFF4C3E75)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cósmico", fontSize = 11.sp)
                }

                // Cyberpunk preset selector
                OutlinedButton(
                    onClick = { viewModel.updateLayerPreset("cyberpunk") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (currentPreset == "cyberpunk") Color(0xFF00FFCC) else Color.White
                    ),
                    border = BorderStroke(1.dp, if (currentPreset == "cyberpunk") Color(0xFF00FFCC) else Color(0xFF4C3E75)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cyberpunk", fontSize = 11.sp)
                }

                // Mystical forest selector
                OutlinedButton(
                    onClick = { viewModel.updateLayerPreset("forest") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (currentPreset == "forest") Color(0xFF00FFCC) else Color.White
                    ),
                    border = BorderStroke(1.dp, if (currentPreset == "forest") Color(0xFF00FFCC) else Color(0xFF4C3E75)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Mystic", fontSize = 11.sp)
                }

                // Gallery Image Picker Button
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentPreset == "custom") Color(0xFF6B4EE0) else Color(0xFF383152)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Foto", fontSize = 11.sp, color = Color.White)
                }
            }

            // Crops selection (Solo visible para capas media y frente)
            if (selectedLayer != "BG") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Molde de Recorte (Mascara)",
                        color = Color(0xFFA59ABB),
                        fontSize = 11.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "NONE" to "Ninguno",
                            "CIRCLE" to "Circular",
                            "ROUNDED_RECT" to "Esquinas"
                        ).forEach { (shapeCode, shapeLabel) ->
                            val isSelected = currentShape == shapeCode
                            OutlinedCard(
                                onClick = { viewModel.updateLayerCropShape(shapeCode) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("crop_shape_$shapeCode"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFFFF007A) else Color(0xFF2C2543)),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (isSelected) Color(0xFFFF007A).copy(alpha = 0.12f) else Color.Transparent
                                )
                            ) {
                                Text(
                                    text = shapeLabel,
                                    color = if (isSelected) Color.White else Color(0xFF9E95C5),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Slider: Movimiento de profundidad (Parallax Depth Multiplier)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Factor de Profundidad 4D",
                        color = Color(0xFFA59ABB),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${currentDepth.roundToInt()}%",
                        color = Color(0xFF00FFCC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = currentDepth,
                    onValueChange = { viewModel.updateLayerDepth(it) },
                    valueRange = -75f..75f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFF6B4EE0),
                        inactiveTrackColor = Color(0xFF241F3C),
                        thumbColor = Color(0xFF00FFCC)
                    ),
                    modifier = Modifier.testTag("depth_slider")
                )
            }

            // Slider: Zoom / Scale
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Escala de Capa",
                        color = Color(0xFFA59ABB),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "%.2fx".format(currentScale),
                        color = Color(0xFF9F7FF4),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = currentScale,
                    onValueChange = { viewModel.updateLayerScale(it) },
                    valueRange = 0.6f..2.2f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFF6B4EE0),
                        inactiveTrackColor = Color(0xFF241F3C),
                        thumbColor = Color(0xFF9F7FF4)
                    ),
                    modifier = Modifier.testTag("scale_slider")
                )
            }

            // Row for Reset layer gestures / Save Workspace Favorites
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.resetActiveLayerOffsets() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reset_layer_button"),
                    border = BorderStroke(1.dp, Color(0xFF4C3E75)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFB5A7DF)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Reajustar Capa")
                }

                Button(
                    onClick = {
                        viewModel.saveCurrentWallpaper()
                        // Provide snappy alert feedback
                        androidx.compose.material3.SnackbarHostState() // internally handled or simple toast
                        android.widget.Toast.makeText(context, "Fondo 4D guardado en favoritos local!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("save_wallpaper_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00FFCC),
                        contentColor = Color(0xFF05010B)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Guardar Favorito", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Pair & Quadruple helper data holders
data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

fun <A, B, C, D> quadruple(a: A, b: B, c: C, d: D): Quadruple<A, B, C, D> = Quadruple(a, b, c, d)
