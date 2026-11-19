package com.articlemaster.hsc.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateInstaller {

    fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): Result<File> {
        return runCatching {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val outFile = File(updatesDir, "EnglishPractice-update.apk")
            if (outFile.exists()) outFile.delete()

            val conn = openFollowingRedirects(downloadUrl)
            try {
                val totalBytes = conn.contentLengthLong.takeIf { it > 0L } ?: -1L
                conn.inputStream.use { input ->
                    FileOutputStream(outFile).use { output ->
                        val buf = ByteArray(16 * 1024)
                        var copied = 0L
                        var lastReported = -1f
                        while (true) {
                            val read = input.read(buf)
                            if (read <= 0) break
                            output.write(buf, 0, read)
                            copied += read
                            if (totalBytes > 0) {
                                val frac = (copied.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                if (frac - lastReported >= 0.02f || frac >= 0.999f) {
                                    lastReported = frac
                                    onProgress(frac)
                                }
                            }
                        }
                        output.flush()
                    }
                }
            } finally {
                conn.disconnect()
            }
            outFile
        }
    }

    fun launchApkInstaller(context: Context, apkFile: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val permIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(permIntent)
            return false
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
        return true
    }

    private fun openFollowingRedirects(startUrl: String, maxHops: Int = 6): HttpURLConnection {
        var currentUrl = startUrl
        repeat(maxHops) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 10_000
                readTimeout = 20_000
                setRequestProperty("User-Agent", "EnglishPractice-Updater")
            }
            val code = conn.responseCode
            if (code in listOf(301, 302, 303, 307, 308)) {
                val loc = conn.getHeaderField("Location")
                conn.disconnect()
                if (loc.isNullOrBlank()) throw IllegalStateException("Redirect missing Location")
                currentUrl = URL(URL(currentUrl), loc).toString()
            } else if (code in 200..299) {
                return conn
            } else {
                conn.disconnect()
                throw IllegalStateException("Download failed (HTTP $code)")
            }
        }
        throw IllegalStateException("Too many redirects")
    }
}
