package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "wallpapers")
data class WallpaperEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    
    // Background Layer Config
    val bgPath: String? = null,
    val bgPresetKey: String = "space", // "space", "cyberpunk", "forest", "custom"
    val bgScale: Float = 1.15f,
    val bgDepth: Float = -20f, // moves small amplitude in opposite direction
    val bgOffsetX: Float = 0f,
    val bgOffsetY: Float = 0f,
    
    // Midground Layer Config
    val mgPath: String? = null,
    val mgPresetKey: String = "space",
    val mgScale: Float = 1.0f,
    val mgDepth: Float = 15f, // moves medium amplitude
    val mgOffsetX: Float = 0f,
    val mgOffsetY: Float = 0f,
    val mgCropShape: String = "NONE", // "NONE", "CIRCLE", "ROUNDED_RECT"
    
    // Foreground Layer Config
    val fgPath: String? = null,
    val fgPresetKey: String = "space",
    val fgScale: Float = 1.0f,
    val fgDepth: Float = 45f, // moves higher amplitude
    val fgOffsetX: Float = 0f,
    val fgOffsetY: Float = 0f,
    val fgCropShape: String = "NONE", // "NONE", "CIRCLE", "ROUNDED_RECT"
    
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface WallpaperDao {
    @Query("SELECT * FROM wallpapers ORDER BY createdAt DESC")
    fun getAllWallpapers(): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE id = :id LIMIT 1")
    suspend fun getWallpaperById(id: Long): WallpaperEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallpaper(wallpaper: WallpaperEntity): Long

    @Delete
    suspend fun deleteWallpaper(wallpaper: WallpaperEntity)

    @Query("DELETE FROM wallpapers WHERE id = :id")
    suspend fun deleteWallpaperById(id: Long)
}

@Database(entities = [WallpaperEntity::class], version = 1, exportSchema = false)
abstract class WallpaperDatabase : RoomDatabase() {
    abstract fun wallpaperDao(): WallpaperDao
}
