package com.onegoal.mcq.data.pdf

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/** Downloads PDFs once and keeps them in the app's files dir so they open offline afterwards. */
class PdfFileCache(context: Context, private val client: OkHttpClient) {
    private val directory = File(context.filesDir, "pdfs").apply { mkdirs() }

    suspend fun get(url: String): File = withContext(Dispatchers.IO) {
        val target = File(directory, fileNameFor(url))
        if (target.exists() && target.length() > 0) return@withContext target

        val request = Request.Builder().url(url).header("Accept", "application/pdf").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")
            val body = response.body ?: throw IOException("Empty body for $url")
            val partial = File(directory, target.name + ".part")
            partial.outputStream().use { output -> body.byteStream().copyTo(output) }
            if (!partial.renameTo(target)) throw IOException("Could not save $url")
        }
        target
    }

    private fun fileNameFor(url: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(url.toByteArray())
        return digest.joinToString("") { "%02x".format(it) } + ".pdf"
    }
}
