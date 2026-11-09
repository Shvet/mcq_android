package com.onegoal.mcq.data.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import java.io.File
import java.io.IOException

/** A PDF needs a password: none was given ([wrongPassword] false) or the given one is wrong. */
class PdfPasswordException(val wrongPassword: Boolean) : IOException(
    if (wrongPassword) "Wrong PDF password" else "PDF password required",
)

/**
 * Opens password-protected PDFs. The platform PdfRenderer only takes a password from
 * Android 15, so PdfBox decrypts the file once into an unlocked copy next to it, which
 * then renders, searches and works offline like any other PDF.
 */
object PdfUnlocker {

    /** Returns [source] itself when it is not encrypted, otherwise its unlocked copy. */
    fun unlock(source: File, password: String): File {
        val target = File(source.parentFile, source.nameWithoutExtension + ".unlocked.pdf")
        if (target.exists() && target.length() > 0) return target

        try {
            PDDocument.load(source, password).use { document ->
                if (!document.isEncrypted) return source
                document.isAllSecurityToBeRemoved = true
                val partial = File(target.path + ".part")
                document.save(partial)
                if (!partial.renameTo(target)) throw IOException("Could not save unlocked PDF")
            }
        } catch (e: InvalidPasswordException) {
            throw PdfPasswordException(wrongPassword = true)
        }
        return target
    }
}
