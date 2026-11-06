package com.onegoal.mcq.util

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** PNGs written to the cache folder that the FileProvider shares (xml/file_paths.xml). */
object ScreenshotFiles {
    suspend fun save(context: Context, image: ImageBitmap, prefix: String = "Screenshot"): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "screenshots").apply { mkdirs() }
        // Keep only the latest few screenshots around.
        directory.listFiles()?.sortedByDescending { it.lastModified() }?.drop(5)?.forEach { it.delete() }
        val file = File(directory, "${prefix}_${System.currentTimeMillis()}.png")
        file.outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        file
    }
}
