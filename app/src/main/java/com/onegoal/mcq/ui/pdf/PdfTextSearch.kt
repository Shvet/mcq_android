package com.onegoal.mcq.ui.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Full-text search with PdfBox-Android (the platform PdfRenderer cannot extract text
 * before Android 15). Page texts are extracted once per file and reused.
 */
class PdfTextSearch {
    private val pageTexts = mutableMapOf<String, List<String>>()

    /** Returns the 0-based indexes of pages containing [query] (case-insensitive). */
    suspend fun search(file: File, query: String): List<Int> = withContext(Dispatchers.Default) {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return@withContext emptyList()
        textsFor(file).mapIndexedNotNull { index, text -> index.takeIf { text.contains(needle) } }
    }

    private suspend fun textsFor(file: File): List<String> {
        pageTexts[file.path]?.let { return it }
        val texts = withContext(Dispatchers.IO) {
            PDDocument.load(file).use { document ->
                val stripper = PDFTextStripper()
                (1..document.numberOfPages).map { page ->
                    coroutineContext.ensureActive()
                    stripper.startPage = page
                    stripper.endPage = page
                    stripper.getText(document).lowercase()
                }
            }
        }
        pageTexts[file.path] = texts
        return texts
    }
}
