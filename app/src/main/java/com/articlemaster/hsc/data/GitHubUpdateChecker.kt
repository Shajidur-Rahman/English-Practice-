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
    const val FALLBACK_VERSION_URL = "https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/main/version.json"
    const val RELEASES_PAGE_URL = "https://github.com/$REPO_OWNER/$REPO_NAME/releases"
    const val DIRECT_APK_URL = "https://github.com/$REPO_OWNER/$REPO_NAME/raw/main/download/EnglishPractice.apk"

    fun checkLatestRelease(currentVersion: String): UpdateCheckResult {
        val releaseRes = fetchUrl(LATEST_RELEASE_API, acceptHeader = "application/vnd.github+json", currentVersion)
        if (releaseRes.isSuccess) {
            val (code, body) = releaseRes.getOrThrow()
            if (code in 200..299 && body.isNotBlank()) {
                val parsed = parseReleaseResponse(body, currentVersion)
                if (parsed is UpdateCheckResult.UpdateAvailable || parsed is UpdateCheckResult.UpToDate) {
                    return parsed
                }
            }
        }

        // Fallback to raw/main/version.json so updates work immediately even before a GitHub Release tag is published
        val fallbackRes = fetchUrl(FALLBACK_VERSION_URL, acceptHeader = "application/json", currentVersion)
        if (fallbackRes.isSuccess) {
            val (code, body) = fallbackRes.getOrThrow()
            if (code in 200..299 && body.isNotBlank()) {
                return parseVersionManifestResponse(body, currentVersion)
            }
            if (code == 404) return UpdateCheckResult.NoReleasePublished
            return UpdateCheckResult.Failed("HTTP $code")
        }

        val err = fallbackRes.exceptionOrNull() ?: releaseRes.exceptionOrNull()
        return UpdateCheckResult.Failed(err?.message ?: "Network unavailable")
    }

    private fun fetchUrl(urlString: String, acceptHeader: String, currentVersion: String): Result<Pair<Int, String>> {
        return runCatching {
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("Accept", acceptHeader)
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("User-Agent", "EnglishPractice-Android/$currentVersion")
            }
            try {
                val code = conn.responseCode
                val body = if (code in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else ""
                code to body
            } finally {
                conn.disconnect()
            }
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

    internal fun parseVersionManifestResponse(rawJson: String, currentVersion: String): UpdateCheckResult {
        val root = JSONObject(rawJson)
        val tag = root.optString("version").trim()
        val title = root.optString("title", "English Practice $tag")
        val notes = root.optString("notes", "")
        val url = root.optString("apkUrl", DIRECT_APK_URL)
        val date = root.optString("date", "")
        return evaluateRelease(
            tag = tag,
            title = title,
            releaseNotes = notes,
            htmlUrl = RELEASES_PAGE_URL,
            apkDownloadUrl = url,
            publishedAt = date,
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
