package com.onegoal.mcq.ui.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File

/**
 * Renders pages of one PDF file with the platform [PdfRenderer].
 * PdfRenderer allows only one open page at a time, so access is serialised with a mutex.
 */
class PdfDocumentRenderer private constructor(
    val file: File,
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    /** Height / width of every page, so the list can reserve space before rendering. */
    val pageAspectRatios: List<Float>,
) : Closeable {

    private val mutex = Mutex()
    private val cache = object : LruCache<String, Bitmap>(maxCacheBytes()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    @Volatile private var closed = false
    private var released = false

    val pageCount: Int get() = pageAspectRatios.size

    suspend fun render(pageIndex: Int, widthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$pageIndex@$widthPx"
        cache.get(key)?.let { return@withContext it }
        mutex.withLock {
            try {
                if (closed || pageIndex !in 0 until pageCount) return@withLock null
                renderer.openPage(pageIndex).use { page ->
                    val width = widthPx.coerceIn(1, MAX_RENDER_WIDTH)
                    val height = (width * page.height.toFloat() / page.width).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    cache.put(key, bitmap)
                    bitmap
                }
            } finally {
                if (closed) release()
            }
        }
    }

    /** Safe to call from the main thread; if a page is being rendered, it is released right after. */
    override fun close() {
        closed = true
        cache.evictAll()
        if (mutex.tryLock()) {
            try {
                release()
            } finally {
                mutex.unlock()
            }
        }
    }

    private fun release() {
        if (released) return
        released = true
        renderer.close()
        descriptor.close()
    }

    companion object {
        private const val MAX_RENDER_WIDTH = 2400

        suspend fun open(file: File): PdfDocumentRenderer = withContext(Dispatchers.IO) {
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = try {
                PdfRenderer(descriptor)
            } catch (e: Exception) {
                descriptor.close()
                throw e
            }
            val ratios = (0 until renderer.pageCount).map { index ->
                renderer.openPage(index).use { page -> page.height.toFloat() / page.width.coerceAtLeast(1) }
            }
            PdfDocumentRenderer(file, descriptor, renderer, ratios)
        }

        private fun maxCacheBytes(): Int = (Runtime.getRuntime().maxMemory() / 6).toInt()
    }
}
