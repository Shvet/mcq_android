package com.onegoal.mcq.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.onegoal.mcq.R
import java.io.File

/** Thin wrappers around the system intents used by the drawer and the PDF viewer. */
object Intents {
    private fun playStoreWebUrl(context: Context) = "https://play.google.com/store/apps/details?id=${context.packageName}"

    fun shareApp(context: Context, message: String?) {
        val text = listOfNotNull(message?.takeIf { it.isNotBlank() }, playStoreWebUrl(context)).joinToString("\n")
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.share_app_using)))
    }

    fun rateApp(context: Context) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        try {
            context.startActivity(market)
        } catch (_: ActivityNotFoundException) {
            openUrl(context, playStoreWebUrl(context))
        }
    }

    fun sendFeedback(context: Context, email: String?, extraText: String? = null) {
        val mail = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            if (!email.isNullOrBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.feedback_subject, context.getString(R.string.app_name)))
            if (!extraText.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, extraText)
        }
        try {
            context.startActivity(Intent.createChooser(mail, context.getString(R.string.send_email_using)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_email_app, Toast.LENGTH_SHORT).show()
        }
    }

    fun openUrl(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            // No browser installed; nothing sensible to do.
        }
    }

    fun shareImage(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.share_screenshot)))
    }

    fun shareText(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, null))
    }
}
