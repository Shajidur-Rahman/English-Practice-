package com.articlemaster.hsc.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseUpdateInfo(
    val latestTag: String,
    val title: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val htmlUrl: String,
    val publishedAt: String
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val info: ReleaseUpdateInfo) : UpdateCheckResult
    data class UpToDate(val currentVersion: String, val latestTag: String) : UpdateCheckResult
    data object NoReleasePublished : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

object GitHubUpdateChecker {
    const val REPO_OWNER = "Shajidur-Rahman"
    const val REPO_NAME = "English-Practice-"
    const val LATEST_RELEASE_API = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
    const val RELEASES_PAGE_URL = "https://github.com/$REPO_OWNER/$REPO_NAME/releases"

    fun checkLatestRelease(currentVersion: String): UpdateCheckResult {
        return try {
            val url = URL(LATEST_RELEASE_API)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "EnglishPractice-Android/$currentVersion")
            }
            try {
                val code = conn.responseCode
                if (code == 404) {
                    return UpdateCheckResult.NoReleasePublished
                }
                if (code !in 200..299) {
                    return UpdateCheckResult.Failed("GitHub HTTP $code")
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parseReleaseResponse(body, currentVersion)
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            UpdateCheckResult.Failed(e.message ?: "Network unavailable")
        }
    }

    internal fun parseReleaseResponse(rawJson: String, currentVersion: String): UpdateCheckResult {
        val root = JSONObject(rawJson)
        val tag = root.optString("tag_name").trim()
        if (tag.isEmpty()) return UpdateCheckResult.NoReleasePublished

        val htmlUrl = root.optString("html_url", RELEASES_PAGE_URL)
        val name = root.optString("name").ifBlank { "English Practice $tag" }
        val notes = root.optString("body").trim()
        val published = root.optString("published_at").take(10)

        var apkUrl: String? = null
        root.optJSONArray("assets")?.let { assets ->
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                    if (apkUrl != null) break
                }
            }
        }

        return evaluateRelease(
            tag = tag,
            title = name,
            releaseNotes = notes,
            htmlUrl = htmlUrl,
            apkDownloadUrl = apkUrl,
            publishedAt = published,
            currentVersion = currentVersion
        )
    }

    fun evaluateRelease(
        tag: String,
        title: String,
        releaseNotes: String,
        htmlUrl: String,
        apkDownloadUrl: String?,
        publishedAt: String,
        currentVersion: String
    ): UpdateCheckResult {
        val cleanTag = tag.trim()
        if (cleanTag.isEmpty()) return UpdateCheckResult.NoReleasePublished
        return if (isNewerVersion(cleanTag, currentVersion)) {
            UpdateCheckResult.UpdateAvailable(
                ReleaseUpdateInfo(
                    latestTag = cleanTag,
                    title = title.ifBlank { "English Practice $cleanTag" },
                    releaseNotes = releaseNotes.trim(),
                    downloadUrl = apkDownloadUrl?.takeIf { it.isNotBlank() } ?: htmlUrl,
                    htmlUrl = htmlUrl,
                    publishedAt = publishedAt
                )
            )
        } else {
            UpdateCheckResult.UpToDate(currentVersion = currentVersion, latestTag = cleanTag)
        }
    }

    fun isNewerVersion(latestTag: String, currentVersion: String): Boolean {
        val latestParts = parseNumericParts(latestTag)
        val currentParts = parseNumericParts(currentVersion)
        if (latestParts.isEmpty()) return false
        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    private fun parseNumericParts(raw: String): List<Int> {
        val cleaned = raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .substringBefore("+")
        return cleaned.split(".")
            .mapNotNull { seg -> seg.filter { it.isDigit() }.toIntOrNull() }
    }
}
