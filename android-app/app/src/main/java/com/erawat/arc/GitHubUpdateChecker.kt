package com.erawat.arc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** APK updater: source commits and Windows releases are deliberately ignored. */
data class UpdateInfo(
    val loading: Boolean = true,
    val appUpdateAvailable: Boolean = false,
    val localCommit: String = BuildConfig.SOURCE_COMMIT,
    val releaseTag: String = "",
    val releaseUrl: String = "",
    val apkUrl: String = "",
    val error: String = "",
)

object GitHubUpdateChecker {
    private const val OWNER = "Erawat4l"
    private const val REPOSITORY = "universal-downloader"
    private const val API_ROOT = "https://api.github.com/repos/$OWNER/$REPOSITORY"

    suspend fun check(localCommit: String = BuildConfig.SOURCE_COMMIT): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val release = getOptionalJson("$API_ROOT/releases/latest")
            val releaseTag = release?.optString("tag_name").orEmpty()
            val releaseUrl = release?.optString("html_url").orEmpty()
            var apkUrl = ""
            val assets = release?.optJSONArray("assets")
            if (assets != null) {
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }

            val localVersion = BuildConfig.VERSION_NAME.substringBefore("-").removePrefix("v")
            val releaseVersion = releaseTag.removePrefix("v").substringBefore("-")
            UpdateInfo(
                loading = false,
                appUpdateAvailable = apkUrl.isNotBlank() && isNewerVersion(releaseVersion, localVersion),
                localCommit = localCommit,
                releaseTag = releaseTag,
                releaseUrl = releaseUrl,
                apkUrl = apkUrl,
            )
        } catch (error: Exception) {
            UpdateInfo(
                loading = false,
                localCommit = localCommit,
                error = error.message ?: "GitHub is unavailable.",
            )
        }
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        if (remote.isBlank() || local.isBlank()) return false
        val rParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val lParts = local.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(rParts.size, lParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val l = lParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    private fun getOptionalJson(url: String): JSONObject? = try {
        getJson(url)
    } catch (error: HttpStatusException) {
        if (error.statusCode == HttpURLConnection.HTTP_NOT_FOUND) null else throw error
    }

    private fun getJson(urlString: String): JSONObject {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Arc-Android")
        }
        return try {
            val status = connection.responseCode
            if (status !in 200..299) throw HttpStatusException(status)
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private class HttpStatusException(val statusCode: Int) : IOException("HTTP $statusCode")
}
