package com.erawat.arc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Update information shown in Settings. Source commits are informational; APK updates use Releases. */
data class UpdateInfo(
    val loading: Boolean = true,
    val sourceUpdateAvailable: Boolean = false,
    val appUpdateAvailable: Boolean = false,
    val newVersionName: String = "",
    val localCommit: String = BuildConfig.SOURCE_COMMIT,
    val remoteCommit: String = "",
    val commitMessage: String = "",
    val releaseTag: String = "",
    val releaseUrl: String = "",
    val apkUrl: String = "",
    val error: String = "",
)

object GitHubUpdateChecker {
    private const val OWNER = "Erawat4l"
    private const val REPOSITORY = "universal-downloader"
    private const val BRANCH = "main"
    private const val API_ROOT = "https://api.github.com/repos/$OWNER/$REPOSITORY"

    suspend fun check(localCommit: String = BuildConfig.SOURCE_COMMIT): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val commit = getJson("$API_ROOT/commits/$BRANCH")
            val remoteSha = commit.optString("sha")
            val message = commit.optJSONObject("commit")
                ?.optString("message")
                ?.lineSequence()
                ?.firstOrNull()
                .orEmpty()

            val release = getOptionalJson("$API_ROOT/releases/latest")
            val releaseTag = release?.optString("tag_name").orEmpty()
            val releaseUrl = release?.optString("html_url").orEmpty()
            var apkUrl = ""
            var bestBuildNum = -1
            val assets = release?.optJSONArray("assets")
            if (assets != null) {
                var bestApkUrl = ""
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    val name = asset.optString("name")
                    val downloadUrl = asset.optString("browser_download_url")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        val numMatch = Regex("""\b(\d+)\b""").find(name)
                        val buildNum = numMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        if (buildNum > bestBuildNum) {
                            bestBuildNum = buildNum
                            bestApkUrl = downloadUrl
                        } else if (bestApkUrl.isBlank()) {
                            bestApkUrl = downloadUrl
                        }
                    }
                }
                apkUrl = bestApkUrl
            }

            val currentVersionClean = BuildConfig.VERSION_NAME.substringBefore("-").removePrefix("Arc").removePrefix("v").trim()
            val releaseVersionClean = releaseTag.removePrefix("v").substringBefore("-").trim()
            val isNewerRelease = isNewerVersion(releaseVersionClean, currentVersionClean)

            val isSameCommit = remoteSha.startsWith(localCommit, ignoreCase = true) ||
                localCommit.startsWith(remoteSha.take(7), ignoreCase = true) ||
                localCommit.equals(remoteSha.take(localCommit.length.coerceAtMost(remoteSha.length)), ignoreCase = true)

            val hasNewCommit = remoteSha.isNotBlank() &&
                localCommit.isNotBlank() &&
                localCommit != "dev" &&
                !isSameCommit

            val currentBuild = try {
                BuildConfig.BUILD_NUMBER
            } catch (_: Exception) {
                BuildConfig.VERSION_CODE
            }
            val isNewerBuild = bestBuildNum > currentBuild && bestBuildNum > 0
            val hasUpdate = isNewerRelease || isNewerBuild

            val newVersionName = if (bestBuildNum > currentBuild && bestBuildNum > 0) {
                "Arc $bestBuildNum"
            } else if (isNewerRelease && releaseTag.isNotBlank()) {
                "Arc ${releaseTag.removePrefix("v")}"
            } else {
                ""
            }

            UpdateInfo(
                loading = false,
                sourceUpdateAvailable = hasNewCommit,
                appUpdateAvailable = hasUpdate,
                newVersionName = newVersionName,
                localCommit = localCommit,
                remoteCommit = remoteSha.take(7),
                commitMessage = message,
                releaseTag = releaseTag,
                releaseUrl = releaseUrl.ifBlank { "https://github.com/$OWNER/$REPOSITORY/releases" },
                apkUrl = apkUrl.ifBlank { "https://github.com/$OWNER/$REPOSITORY/releases/download/v0.1.0/ARC.apk" },
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
        if (rParts.isEmpty() || lParts.isEmpty()) return false
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
