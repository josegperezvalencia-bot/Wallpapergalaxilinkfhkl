package com.example

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.ParallaxWallpaperService
import com.example.ui.WallpaperViewModel
import com.example.ui.components.GalleryPanel
import com.example.ui.components.LayerEditor
import com.example.ui.components.WallpaperCanvas
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: WallpaperViewModel = viewModel()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF07050E)
                ) {
                    WallpaperCreatorScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun WallpaperCreatorScreen(viewModel: WallpaperViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_screen_container")
    ) {
        // --- 1. FULLSCREEN INTERACTIVE 4D PARALLAX READY CANVAS BACKGROUND ---
        WallpaperCanvas(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize(),
            isEditMode = true
        )

        // Glassmorphic top gradient shadow to protect status-bar contrast
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF07050E).copy(alpha = 0.85f), Color.Transparent)
                    )
                )
        )

        // --- 2. FOREGROUND FLOATING CONTROLS ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Elegant Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WALLPAPERS 4D",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.8.sp
                    )
                    Text(
                        text = "Profundidad Parallax Giroscópica",
                        color = Color(0xFF9E95C5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Call to action: "Establecer como Fondo Real" Live Wallpaper Settings Intent launcher
                Button(
                    onClick = {
                        try {
                            // First, save current creation automatically so it loads in the service
                            viewModel.saveCurrentWallpaper()
                            
                            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                putExtra(
                                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                    ComponentName(context, ParallaxWallpaperService::class.java)
                                )
                            }
                            context.startActivity(intent)
                            Toast.makeText(context, "Fondo guardado. ¡Establécelo ahora!", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Servicio no soportado en este emulador. Guardado en galeria.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.testTag("apply_live_wallpaper_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6B4EE0)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF9F7FF4).copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Aplicar",
                        tint = Color(0xFF00FFCC),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Aplicar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Minimal Tab Switches
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .background(Color(0xFF13111C).copy(alpha = 0.8f), RoundedCornerShape(50))
                    .padding(4.dp)
                    .align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.Center
            ) {
                // Tab: Creator
                TabPill(
                    label = "Diseñar",
                    icon = Icons.Default.Build,
                    isSelected = currentTab == "creator",
                    onClick = { viewModel.selectTab("creator") },
                    modifier = Modifier.testTag("tab_creator")
                )
                // Tab: Gallery
                TabPill(
                    label = "Mi Galería",
                    icon = Icons.Default.Favorite,
                    isSelected = currentTab == "gallery",
                    onClick = { viewModel.selectTab("gallery") },
                    modifier = Modifier.testTag("tab_gallery")
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Tab contents representation switching with sliding visibility animations
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (currentTab == "creator") {
                    LayerEditor(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.65f)
                            .testTag("gallery_panel_container"),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0B0912).copy(alpha = 0.95f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF2C2543).copy(alpha = 0.5f))
                    ) {
                        GalleryPanel(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) Color(0xFF6B4EE0) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFFA59ABB),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = if (isSelected) Color.White else Color(0xFFA59ABB),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
