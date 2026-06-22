package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class WallpaperRepository(private val context: Context) {
    
    private val database: WallpaperDatabase by lazy {
        androidx.room.Room.databaseBuilder(
            context.applicationContext,
            WallpaperDatabase::class.java,
            "wallpaper_4d_database"
        ).build()
    }
    
    private val wallpaperDao = database.wallpaperDao()
    
    val allWallpapers: Flow<List<WallpaperEntity>> = wallpaperDao.getAllWallpapers()
    
    suspend fun getWallpaperById(id: Long): WallpaperEntity? = withContext(Dispatchers.IO) {
        wallpaperDao.getWallpaperById(id)
    }
    
    suspend fun insertWallpaper(wallpaper: WallpaperEntity): Long = withContext(Dispatchers.IO) {
        wallpaperDao.insertWallpaper(wallpaper)
    }
    
    suspend fun deleteWallpaper(wallpaper: WallpaperEntity) = withContext(Dispatchers.IO) {
        wallpaperDao.deleteWallpaper(wallpaper)
    }
    
    suspend fun deleteWallpaperById(id: Long) = withContext(Dispatchers.IO) {
        wallpaperDao.deleteWallpaperById(id)
    }
    
    /**
     * Copies selected local images to internal storage so the app always preserves
     * read access to them even after devices reboot.
     */
    suspend fun saveImageToInternalStorage(uriStr: String): String = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriStr)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext ""
            
            // Create a dedicated directory
            val wallpaperDir = File(context.filesDir, "layers_images")
            if (!wallpaperDir.exists()) {
                wallpaperDir.mkdirs()
            }
            
            val fileName = "layer_${UUID.randomUUID()}.png"
            val targetFile = File(wallpaperDir, fileName)
            
            FileOutputStream(targetFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            
            Log.d("WallpaperRepository", "Successfully copied $uri to ${targetFile.absolutePath}")
            targetFile.absolutePath
        } catch (e: Exception) {
            Log.e("WallpaperRepository", "Failed to copy image to internal storage", e)
            ""
        }
    }
}
