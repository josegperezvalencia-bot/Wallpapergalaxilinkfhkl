package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WallpaperEntity
import com.example.ui.WallpaperViewModel

@Composable
fun GalleryPanel(
    viewModel: WallpaperViewModel,
    modifier: Modifier = Modifier
) {
    val savedList by viewModel.savedWallpapers.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FONDOS GUARDADOS",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
            
            Text(
                text = "${savedList.size} Diseños",
                color = Color(0xFF00FFCC),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (savedList.isEmpty()) {
            // High-fidelity empty state card as recommended by design guidelines
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF13111C), RoundedCornerShape(24.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Paleta vacía",
                        tint = Color(0xFF4C3E75),
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("empty_gallery_icon")
                    )
                    
                    Text(
                        text = "Aún no tienes fondos 4D",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Diseña tus propios fondos combinando fotos locales y recortes en 3D. ¡Guárdalos para verlos en esta galería!",
                        color = Color(0xFFA59ABB),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = { viewModel.selectTab("creator") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6B4EE0)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Crear Fondo Ahora")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .testTag("saved_wallpapers_grid"),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(savedList, key = { it.id }) { wallpaper ->
                    WallpaperCard(
                        wallpaper = wallpaper,
                        onSelect = { viewModel.loadWallpaperIntoEditor(it) },
                        onDelete = { viewModel.deleteWallpaper(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun WallpaperCard(
    wallpaper: WallpaperEntity,
    onSelect: (WallpaperEntity) -> Unit,
    onDelete: (WallpaperEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onSelect(wallpaper) }
            .testTag("wallpaper_mini_card_${wallpaper.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1B182B)
        ),
        border = BorderStroke(1.dp, Color(0xFF2C2543))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Layer preview representation (Minimalist abstract stacked colors representing the layers)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Capas icon",
                        tint = Color(0xFF9F7FF4),
                        modifier = Modifier.size(20.dp)
                    )
                    
                    // Delete Button
                    IconButton(
                        onClick = { onDelete(wallpaper) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .testTag("delete_wallpaper_${wallpaper.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Borrar wallpaper",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Title & Attributes
                Column {
                    Text(
                        text = wallpaper.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Capas: BG=${wallpaper.bgPresetKey} / FG=${wallpaper.fgPresetKey}",
                        color = Color(0xFFA59ABB),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
