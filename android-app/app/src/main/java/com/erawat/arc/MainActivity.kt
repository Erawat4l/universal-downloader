package com.erawat.arc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val ArcBlack = Color(0xFF050505)
private val ArcSurface = Color(0xFF111111)
private val ArcRaised = Color(0xFF191919)
private val ArcWhite = Color(0xFFF7F7F7)
private val ArcMuted = Color(0xFFB0B0B0)
private val ArcSubtle = Color(0xFF777777)

private enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    ACTIVITY("Activity", Icons.Outlined.FileDownload),
    LIBRARY("Library", Icons.Outlined.Folder),
    SETTINGS("Settings", Icons.Outlined.Settings),
}

private data class AudioChoice(val language: String, val url: String)
private data class SearchHit(
    val title: String,
    val source: String,
    val type: String,
    val year: String,
    val providers: String,
    val audio: List<AudioChoice>,
    val url: String = "",
    val poster: String = "",
)

private data class SeasonInfo(
    val season: Int,
    val episodes: Int,
)

private data class PlaylistEntry(
    val id: String,
    val title: String,
    val url: String,
    val thumbnail: String,
    val duration: Long = 0,
)

private data class MediaTarget(
    val title: String,
    val source: String,
    val detail: String,
    val size: String,
    val description: String,
    val availableQualities: List<String> = emptyList(),
    val poster: String = "",
    val streamHeaders: Map<String, String> = emptyMap(),
    val isSeries: Boolean = false,
    val seasons: List<SeasonInfo> = emptyList(),
    val isPlaylist: Boolean = false,
    val playlistEntries: List<PlaylistEntry> = emptyList(),
    val selectedSeason: Int = 1,
    val selectedEpisode: Int = 1,
)

private data class DownloadJob(
    val id: String,
    val title: String,
    val source: String,
    val detail: String,
    val status: String,
    val path: String = "",
    val progress: Float = 0f,
    val downloaded: String = "0 MB",
    val total: String = "--",
    val speed: String = "--",
    val eta: String = "--",
    val poster: String = "",
    val url: String = "",
    val headers: Map<String, String> = emptyMap(),
)

private data class EngineResult(
    val jobId: String,
    val type: String,
    val response: JSONObject,
)

private data class CliTrace(
    val action: String,
    val target: String,
    val durationSeconds: Float,
    val steps: List<String>,
    val stdoutLogs: String,
    val status: String,
    val isError: Boolean = false,
)

private data class PlaybackInfo(
    val url: String,
    val title: String,
    val headers: Map<String, String> = emptyMap(),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ArcTheme { ArcApp() } }
    }
}

@Composable
private fun ArcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = ArcWhite,
            onPrimary = ArcBlack,
            secondary = ArcMuted,
            onSecondary = ArcBlack,
            background = ArcBlack,
            onBackground = ArcWhite,
            surface = ArcSurface,
            onSurface = ArcWhite,
            surfaceVariant = ArcRaised,
            onSurfaceVariant = ArcMuted,
            outline = Color(0xFF404040),
            outlineVariant = Color(0xFF2B2B2B),
        ),
        content = content,
    )
}

@Composable
private fun ArcApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val preferences = remember { context.getSharedPreferences("arc_settings", Context.MODE_PRIVATE) }

    val defaultDownloadPath = remember {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (publicDownloads != null && (publicDownloads.exists() || publicDownloads.mkdirs())) {
            publicDownloads.absolutePath
        } else {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
                ?: context.filesDir.resolve("downloads").absolutePath
        }
    }
    var downloadPath by rememberSaveable {
        mutableStateOf(preferences.getString("download_path", defaultDownloadPath) ?: defaultDownloadPath)
    }
    var showPathDialog by rememberSaveable { mutableStateOf(false) }

    var defaultQuality by rememberSaveable {
        mutableStateOf(preferences.getString("default_quality", "1080p") ?: "1080p")
    }
    var selectedQuality by rememberSaveable { mutableStateOf(defaultQuality) }
    var showQualityDialog by rememberSaveable { mutableStateOf(false) }

    var tabName by rememberSaveable { mutableStateOf(AppTab.HOME.name) }
    var overlay by rememberSaveable { mutableStateOf("") }
    var input by rememberSaveable { mutableStateOf("") }
    var audioOnly by rememberSaveable { mutableStateOf(false) }

    var searchLoading by rememberSaveable { mutableStateOf(false) }
    var resolveLoading by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var resolvedUrl by rememberSaveable { mutableStateOf("") }
    var target by remember { mutableStateOf(MediaTarget("", "", "", "", "")) }
    var hits by remember { mutableStateOf(emptyList<SearchHit>()) }
    var jobs by remember { mutableStateOf(emptyList<DownloadJob>()) }
    var update by remember { mutableStateOf(UpdateInfo()) }
    var notifications by rememberSaveable { mutableStateOf(true) }
    var telegramApiId by rememberSaveable { mutableStateOf(preferences.getString("telegram_api_id", "").orEmpty()) }
    var telegramApiHash by rememberSaveable { mutableStateOf(preferences.getString("telegram_api_hash", "").orEmpty()) }
    var telegramSession by rememberSaveable { mutableStateOf(preferences.getString("telegram_session", "").orEmpty()) }
    var showTelegramSetup by rememberSaveable { mutableStateOf(false) }
    var infoTitle by rememberSaveable { mutableStateOf("") }
    var infoBody by rememberSaveable { mutableStateOf("") }

    var currentTrace by remember { mutableStateOf<CliTrace?>(null) }
    var isTraceExpanded by rememberSaveable { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableFloatStateOf(0f) }
    var liveStepText by remember { mutableStateOf("") }

    var playingMedia by remember { mutableStateOf<PlaybackInfo?>(null) }
    var showShareMenuForJob by remember { mutableStateOf<DownloadJob?>(null) }

    // History of past queries and analyzes
    var historyItems by remember {
        val raw = preferences.getString("history_items", "[]") ?: "[]"
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val str = arr.optString(i)
                if (str.isNotBlank()) list.add(str)
            }
        } catch (_: Exception) {}
        mutableStateOf(list.toList())
    }

    val tab = AppTab.entries.firstOrNull { it.name == tabName } ?: AppTab.HOME

    fun message(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }

    fun showInfo(title: String, body: String) {
        infoTitle = title
        infoBody = body
    }

    fun copyToClipboard(text: String, label: String = "Copied") {
        clipboard.setText(AnnotatedString(text))
        message("$label copied to clipboard.")
    }

    fun saveHistory(entry: String) {
        val updated = (listOf(entry) + historyItems.filter { it != entry }).take(15)
        historyItems = updated
        val arr = JSONArray()
        updated.forEach { arr.put(it) }
        preferences.edit().putString("history_items", arr.toString()).apply()
    }

    fun clearHistory() {
        historyItems = emptyList()
        preferences.edit().remove("history_items").apply()
        message("History cleared.")
    }

    LaunchedEffect(searchLoading, resolveLoading) {
        if (searchLoading || resolveLoading) {
            elapsedSeconds = 0f
            val isSearch = searchLoading
            val searchSteps = listOf(
                "Initializing media catalog engine...",
                "Querying content index & active mirrors...",
                "Scanning provider stream manifests...",
                "Extracting resolution profiles & audio...",
                "Formatting media results...",
            )
            val resolveSteps = listOf(
                "Initializing media resolver...",
                "Analyzing stream endpoints...",
                "Extracting manifest and headers...",
                "Verifying playable direct link...",
            )
            val steps = if (isSearch) searchSteps else resolveSteps
            var idx = 0
            val startTime = System.currentTimeMillis()
            while (searchLoading || resolveLoading) {
                elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000f
                liveStepText = steps[idx % steps.size]
                delay(300)
                idx++
            }
        }
    }

    // Live download progress polling loop from embedded engine
    LaunchedEffect(jobs) {
        while (jobs.any { it.status == "Downloading" }) {
            delay(400)
            val progressObj = withContext(Dispatchers.IO) {
                EmbeddedEngine.getProgress()
            }
            if (progressObj.length() > 0) {
                jobs = jobs.map { job ->
                    val p = progressObj.optJSONObject(job.id)
                    if (p != null) {
                        job.copy(
                            progress = p.optDouble("percent", job.progress.toDouble()).toFloat(),
                            downloaded = p.optString("downloaded_str", job.downloaded),
                            total = p.optString("total_str", job.total),
                            speed = p.optString("speed", job.speed),
                            eta = p.optString("eta", job.eta),
                            status = p.optString("status", job.status),
                            path = p.optString("path", job.path).ifEmpty { job.path },
                        )
                    } else job
                }
            }
        }
    }

    fun openFile(filePath: String) {
        if (filePath.isBlank()) return
        val file = File(filePath)
        if (!file.exists()) {
            message("File not found on device: $filePath")
            return
        }
        try {
            val uri: Uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            } catch (_: Exception) {
                Uri.fromFile(file)
            }
            val ext = file.extension.lowercase()
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: when {
                ext in listOf("mp4", "mkv", "webm", "avi", "mov", "ts") -> "video/*"
                ext in listOf("mp3", "m4a", "aac", "flac", "ogg", "wav") -> "audio/*"
                ext == "pdf" -> "application/pdf"
                else -> "*/*"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            message("Could not open media: ${e.message}")
        }
    }

    fun openStreamInVlc(streamUrl: String, headers: Map<String, String> = emptyMap()) {
        try {
            val uri = Uri.parse(streamUrl)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                headers.forEach { (k, v) -> putExtra(k, v) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Play stream with"))
        } catch (e: Exception) {
            message("Could not open stream: ${e.message}")
        }
    }

    fun shareFile(filePath: String) {
        if (filePath.isBlank()) return
        val file = File(filePath)
        if (!file.exists()) {
            message("File not found on device.")
            return
        }
        try {
            val uri: Uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            } catch (_: Exception) {
                Uri.fromFile(file)
            }
            val ext = file.extension.lowercase()
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share media file"))
        } catch (e: Exception) {
            message("Could not share file: ${e.message}")
        }
    }

    fun shareText(text: String, title: String = "Share Link") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            message("Could not share: ${e.message}")
        }
    }

    fun handleEngineResult(result: EngineResult) {
        val response = result.response
        val isOk = response.optBoolean("ok", false)
        val logs = response.optString("logs", "").trim()
        val error = response.optString("error", "The media engine encountered an issue.")

        val duration = elapsedSeconds.coerceAtLeast(0.4f)
        val actionLabel = when (result.type) {
            "search" -> "Search: \"$query\""
            "resolve" -> "Resolve media"
            "download" -> "Download media"
            else -> result.type
        }

        if (!isOk) {
            currentTrace = CliTrace(
                action = actionLabel,
                target = if (result.type == "search") query else input,
                durationSeconds = duration,
                steps = listOf(
                    "Media engine initialized",
                    "Executed task: ${result.type}",
                    "[ERROR] Execution failed: $error",
                ),
                stdoutLogs = logs,
                status = "Failed",
                isError = true,
            )
            message(error)
            if (result.type == "resolve") resolveLoading = false
            if (result.type == "search") searchLoading = false
            if (result.type == "download") jobs = jobs.map { if (it.id == result.jobId) it.copy(status = "Failed") else it }
            return
        }

        val data = response.optJSONObject("result")
        when (result.type) {
            "search" -> {
                val resultList = mutableListOf<SearchHit>()
                val array = data?.optJSONArray("results")
                if (array != null) {
                    for (index in 0 until array.length()) {
                        val item = array.optJSONObject(index) ?: continue
                        val audios = mutableListOf<AudioChoice>()
                        val audioArray = item.optJSONArray("audios")
                        if (audioArray != null) {
                            for (audioIndex in 0 until audioArray.length()) {
                                val audio = audioArray.optJSONObject(audioIndex) ?: continue
                                audios += AudioChoice(audio.optString("language", "Default"), audio.optString("url"))
                            }
                        }
                        resultList += SearchHit(
                            title = item.optString("title", "Untitled"),
                            source = item.optString("source", "Source"),
                            type = item.optString("type", "MEDIA"),
                            year = item.optString("year", ""),
                            providers = item.optJSONArray("providers")?.let { providers ->
                                (0 until providers.length()).joinToString(" · ") { providerIndex -> providers.optString(providerIndex) }
                            } ?: "",
                            audio = audios,
                            url = item.optString("url", ""),
                            poster = item.optString("poster", ""),
                        )
                    }
                }
                hits = resultList
                searchLoading = false

                currentTrace = CliTrace(
                    action = actionLabel,
                    target = query,
                    durationSeconds = duration,
                    steps = listOf(
                        "Media engine activated",
                        "Search query: \"$query\"",
                        "Catalogs queried and releases analyzed",
                        "Grouped audio tracks and sources",
                        "[OK] Found ${resultList.size} titles ready",
                    ),
                    stdoutLogs = logs,
                    status = "Completed",
                    isError = false,
                )
            }
            "resolve" -> {
                val isSeries = data?.optBoolean("is_series", false) ?: false
                val isPlaylist = data?.optBoolean("is_playlist", false) ?: false
                val url = data?.optString("url").orEmpty()
                resolvedUrl = url

                val qualitiesList = mutableListOf<String>()
                val qArray = data?.optJSONArray("qualities")
                if (qArray != null) {
                    for (i in 0 until qArray.length()) {
                        val q = qArray.optString(i)
                        if (q.isNotBlank()) qualitiesList.add(q)
                    }
                }

                val headersMap = mutableMapOf<String, String>()
                val hObj = data?.optJSONObject("stream_headers") ?: data?.optJSONObject("headers")
                if (hObj != null) {
                    val keys = hObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        headersMap[k] = hObj.optString(k)
                    }
                }

                val seasonsList = mutableListOf<SeasonInfo>()
                val sArray = data?.optJSONArray("seasons")
                if (sArray != null) {
                    for (i in 0 until sArray.length()) {
                        val sObj = sArray.optJSONObject(i) ?: continue
                        seasonsList.add(
                            SeasonInfo(
                                season = sObj.optInt("season", i + 1),
                                episodes = sObj.optInt("episodes", 1)
                            )
                        )
                    }
                }

                val playlistEntriesList = mutableListOf<PlaylistEntry>()
                val pArray = data?.optJSONArray("entries")
                if (pArray != null) {
                    for (i in 0 until pArray.length()) {
                        val pObj = pArray.optJSONObject(i) ?: continue
                        playlistEntriesList.add(
                            PlaylistEntry(
                                id = pObj.optString("id", ""),
                                title = pObj.optString("title", "Video ${i + 1}"),
                                url = pObj.optString("url", ""),
                                thumbnail = pObj.optString("thumbnail", ""),
                                duration = pObj.optLong("duration", 0),
                            )
                        )
                    }
                }

                val resolvedTitle = data?.optString("title").takeUnless { it.isNullOrBlank() } ?: target.title
                val resolvedPoster = data?.optString("poster").takeUnless { it.isNullOrBlank() } ?: target.poster
                val resolvedSource = data?.optString("source").takeUnless { it.isNullOrBlank() } ?: target.source

                target = target.copy(
                    title = resolvedTitle,
                    source = resolvedSource,
                    detail = "${selectedQuality}${if (audioOnly) " · Audio" else " · Video"}",
                    availableQualities = qualitiesList,
                    poster = resolvedPoster,
                    streamHeaders = headersMap,
                    isSeries = isSeries,
                    seasons = seasonsList,
                    isPlaylist = isPlaylist,
                    playlistEntries = playlistEntriesList,
                )
                if (qualitiesList.isNotEmpty() && !qualitiesList.contains(selectedQuality)) {
                    selectedQuality = qualitiesList.first()
                }

                resolveLoading = false
                overlay = "analysis"

                currentTrace = CliTrace(
                    action = actionLabel,
                    target = input,
                    durationSeconds = duration,
                    steps = listOf(
                        "Media engine activated",
                        "Target resolved: $resolvedTitle",
                        "Stream endpoint and CDN headers verified",
                        "[OK] Direct playable stream ready",
                    ),
                    stdoutLogs = logs,
                    status = "Completed",
                    isError = false,
                )
            }
            "download" -> {
                val path = data?.optString("path").orEmpty()
                jobs = jobs.map { if (it.id == result.jobId) it.copy(status = "Complete", path = path, progress = 100f) else it }
                message("Download complete.")
            }
        }
    }

    fun submit(payload: JSONObject, type: String): String {
        val jobId = java.util.UUID.randomUUID().toString()
        payload.put("job_id", jobId)
        scope.launch {
            val response = withContext(Dispatchers.IO) {
                runCatching { EmbeddedEngine.execute(context, payload) }
                    .getOrElse { error ->
                        JSONObject().apply {
                            put("ok", false)
                            put("error", error.message ?: "The engine could not start.")
                        }
                    }
            }
            handleEngineResult(EngineResult(jobId, type, response))
        }
        return jobId
    }

    fun openHome() {
        tabName = AppTab.HOME.name
        overlay = ""
        searchLoading = false
        resolveLoading = false
    }

    fun analyze(value: String = input, titleOverride: String? = null, posterOverride: String? = null) {
        val clean = value.trim()
        if (clean.isEmpty()) {
            message("Paste a link or enter a search.")
            return
        }
        input = clean
        saveHistory(titleOverride ?: clean)
        if (defaultQuality != "Ask each time") {
            selectedQuality = defaultQuality
        }

        val isDirectUrl = clean.startsWith("http://") || clean.startsWith("https://")
        if (isDirectUrl) {
            resolveLoading = true
            target = MediaTarget(
                title = titleOverride ?: (clean.split("/").lastOrNull { it.isNotBlank() } ?: "Media stream"),
                source = "Analyzing...",
                detail = "${selectedQuality}${if (audioOnly) " · Audio" else " · Video"}",
                size = "Detecting",
                description = clean,
                poster = posterOverride.orEmpty(),
            )
            submit(
                JSONObject().apply {
                    put("action", "resolve")
                    put("url", clean)
                    put("quality", selectedQuality)
                },
                "resolve"
            )
        } else {
            query = clean
            searchLoading = true
            hits = emptyList()
            overlay = "search"
            submit(
                JSONObject().apply {
                    put("action", "search")
                    put("query", clean)
                    put("platform", "netmirror")
                },
                "search"
            )
        }
    }

    fun resolveEpisode(season: Int, episode: Int) {
        resolveLoading = true
        target = target.copy(selectedSeason = season, selectedEpisode = episode)
        submit(
            JSONObject().apply {
                put("action", "resolve")
                put("url", input)
                put("season", season)
                put("episode", episode)
                put("quality", selectedQuality)
            },
            "resolve"
        )
    }

    fun startDownload(targetUrl: String = resolvedUrl.ifEmpty { input }, customTitle: String = target.title) {
        val job = DownloadJob(
            id = java.util.UUID.randomUUID().toString(),
            title = customTitle,
            source = target.source,
            detail = "${selectedQuality}${if (audioOnly) " · Audio" else " · Video"}",
            status = "Downloading",
            poster = target.poster,
            url = targetUrl,
            headers = target.streamHeaders,
        )
        jobs = listOf(job) + jobs
        message("Download queued: $customTitle")

        val payload = JSONObject().apply {
            put("action", "download")
            put("url", targetUrl)
            put("quality", selectedQuality)
            put("target_dir", downloadPath)
            put("job_id", job.id)
            if (target.isSeries) {
                put("season", target.selectedSeason)
                put("episode", target.selectedEpisode)
            }
            if (telegramSession.isNotBlank()) {
                put("telegram", JSONObject().apply {
                    put("api_id", telegramApiId)
                    put("api_hash", telegramApiHash)
                    put("session", telegramSession)
                })
            }
        }
        scope.launch {
            val response = withContext(Dispatchers.IO) {
                runCatching { EmbeddedEngine.execute(context, payload) }
                    .getOrElse { error ->
                        JSONObject().apply {
                            put("ok", false)
                            put("error", error.message ?: "The engine could not start.")
                        }
                    }
            }
            handleEngineResult(EngineResult(job.id, "download", response))
        }
        tabName = AppTab.ACTIVITY.name
        overlay = ""
    }

    fun cancelJob(jobId: String) {
        EmbeddedEngine.cancel(jobId)
        jobs = jobs.map { if (it.id == jobId) it.copy(status = "Cancelled") else it }
        message("Download cancelled.")
    }

    fun deleteJob(jobId: String, removeFile: Boolean = false) {
        val job = jobs.firstOrNull { it.id == jobId }
        if (job != null && removeFile && job.path.isNotBlank()) {
            runCatching { File(job.path).delete() }
        }
        jobs = jobs.filterNot { it.id == jobId }
        message("Item removed.")
    }

    Scaffold(
        containerColor = ArcBlack,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (overlay.isEmpty() && playingMedia == null) {
                NavigationBar(containerColor = ArcRaised) {
                    AppTab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tabName = item.name },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                overlay == "search" -> {
                    SearchOverlay(
                        query = query,
                        loading = searchLoading,
                        elapsedSeconds = elapsedSeconds,
                        hits = hits,
                        onClose = { overlay = "" },
                        onSelect = { hit ->
                            input = hit.url.ifEmpty { query }
                            target = MediaTarget(
                                title = hit.title,
                                source = hit.source,
                                detail = "${hit.year} · ${hit.type}",
                                size = "Resolving",
                                description = hit.providers,
                                poster = hit.poster,
                            )
                            analyze(hit.url.ifEmpty { query }, titleOverride = hit.title, posterOverride = hit.poster)
                        },
                        trace = currentTrace,
                        isTraceExpanded = isTraceExpanded,
                        onToggleTrace = { isTraceExpanded = !isTraceExpanded },
                    )
                }
                overlay == "analysis" -> {
                    AnalysisOverlay(
                        target = target,
                        loading = resolveLoading,
                        elapsedSeconds = elapsedSeconds,
                        resolvedUrl = resolvedUrl,
                        selectedQuality = selectedQuality,
                        onSelectQuality = { selectedQuality = it },
                        onClose = { overlay = "" },
                        onPlayInApp = {
                            if (resolvedUrl.isNotBlank()) {
                                playingMedia = PlaybackInfo(resolvedUrl, target.title, target.streamHeaders)
                            } else {
                                message("Stream link not ready yet.")
                            }
                        },
                        onPlayExternal = {
                            if (resolvedUrl.isNotBlank()) {
                                openStreamInVlc(resolvedUrl, target.streamHeaders)
                            } else {
                                message("Stream link not ready yet.")
                            }
                        },
                        onDownload = { startDownload() },
                        onSelectEpisode = { season, episode ->
                            resolveEpisode(season, episode)
                        },
                        onSelectPlaylistItem = { entry ->
                            input = entry.url
                            analyze(entry.url, titleOverride = entry.title, posterOverride = entry.thumbnail)
                        },
                        onCopy = { copyToClipboard(resolvedUrl.ifEmpty { input }, "Stream link") },
                        onShare = {
                            if (resolvedUrl.isNotBlank()) {
                                shareText(resolvedUrl, "Share Stream URL")
                            } else {
                                shareText(input, "Share Link")
                            }
                        },
                        trace = currentTrace,
                        isTraceExpanded = isTraceExpanded,
                        onToggleTrace = { isTraceExpanded = !isTraceExpanded },
                    )
                }
                else -> {
                    when (tab) {
                        AppTab.HOME -> HomeScreen(
                            input = input,
                            onInputChange = { input = it },
                            onPaste = {
                                clipboard.getText()?.text?.let {
                                    input = it
                                    message("Pasted from clipboard.")
                                }
                            },
                            onAnalyze = { analyze() },
                            onOpenHistoryItem = { item ->
                                input = item
                                analyze(item)
                            },
                            historyItems = historyItems,
                            onClearHistory = { clearHistory() },
                            audioOnly = audioOnly,
                            onToggleAudio = { audioOnly = !audioOnly },
                            defaultQuality = defaultQuality,
                            onPickQuality = { showQualityDialog = true },
                            downloadPath = downloadPath,
                            onPickPath = { showPathDialog = true },
                            onSelectPreset = { preset ->
                                input = preset
                                analyze(preset)
                            },
                        )
                        AppTab.ACTIVITY -> ActivityScreen(
                            jobs = jobs,
                            onPlayInApp = { job ->
                                if (job.path.isNotBlank() && File(job.path).exists()) {
                                    playingMedia = PlaybackInfo(job.path, job.title)
                                } else if (job.url.isNotBlank()) {
                                    playingMedia = PlaybackInfo(job.url, job.title, job.headers)
                                } else {
                                    message("Media not available.")
                                }
                            },
                            onOpenExternal = { job ->
                                if (job.path.isNotBlank() && File(job.path).exists()) {
                                    openFile(job.path)
                                } else if (job.url.isNotBlank()) {
                                    openStreamInVlc(job.url, job.headers)
                                }
                            },
                            onCancelJob = { cancelJob(it) },
                            onDeleteJob = { id, removeFile -> deleteJob(id, removeFile) },
                            onShareJob = { job -> showShareMenuForJob = job },
                        )
                        AppTab.LIBRARY -> LibraryScreen(
                            downloadPath = downloadPath,
                            onOpenFile = { openFile(it) },
                            onShareFile = { shareFile(it) },
                            onPlayInApp = { path ->
                                playingMedia = PlaybackInfo(path, File(path).name)
                            },
                        )
                        AppTab.SETTINGS -> SettingsScreen(
                            downloadPath = downloadPath,
                            onPickPath = { showPathDialog = true },
                            defaultQuality = defaultQuality,
                            onPickQuality = { showQualityDialog = true },
                            notifications = notifications,
                            onToggleNotifications = { notifications = !notifications },
                            telegramSession = telegramSession,
                            onOpenTelegramSetup = { showTelegramSetup = true },
                            onOpenPath = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(downloadPath), "resource/folder")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open Folder"))
                                } catch (_: Exception) {
                                    message("Saved path: $downloadPath")
                                }
                            },
                        )
                    }
                }
            }

            // In-app embedded Media3 video player
            playingMedia?.let { playback ->
                InAppVideoPlayerDialog(
                    info = playback,
                    onDismiss = { playingMedia = null },
                    onOpenExternal = {
                        openStreamInVlc(playback.url, playback.headers)
                    },
                )
            }

            // Share options sheet
            showShareMenuForJob?.let { job ->
                Dialog(onDismissRequest = { showShareMenuForJob = null }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ArcRaised,
                        border = BorderStroke(1.dp, Color(0xFF333333)),
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Share: ${job.title}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ArcWhite,
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            if (job.path.isNotBlank() && File(job.path).exists()) {
                                OutlinedButton(
                                    onClick = {
                                        val p = job.path
                                        showShareMenuForJob = null
                                        shareFile(p)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Share Video File")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (job.url.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val u = job.url
                                        showShareMenuForJob = null
                                        shareText(u, "Share Stream URL")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Share Direct Stream Link")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (job.path.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        copyToClipboard(job.path, "File path")
                                        showShareMenuForJob = null
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Copy Storage Path")
                                }
                            }
                        }
                    }
                }
            }

            // Default Quality Selection Dialog
            if (showQualityDialog) {
                val qualityOptions = listOf("2160p (4K)", "1440p (2K)", "1080p (Full HD)", "720p (HD)", "480p (SD)", "Best Available", "Ask each time")
                Dialog(onDismissRequest = { showQualityDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ArcRaised,
                        border = BorderStroke(1.dp, Color(0xFF333333)),
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Select Default Quality",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ArcWhite,
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            qualityOptions.forEach { opt ->
                                val cleanVal = opt.split(" ").first()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            defaultQuality = cleanVal
                                            preferences.edit().putString("default_quality", cleanVal).apply()
                                            showQualityDialog = false
                                            message("Default quality set to $opt")
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = opt,
                                        color = if (defaultQuality == cleanVal) ArcWhite else ArcMuted,
                                        fontWeight = if (defaultQuality == cleanVal) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (defaultQuality == cleanVal) {
                                        Icon(Icons.Outlined.Check, contentDescription = null, tint = ArcWhite)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Save Path Dialog
            if (showPathDialog) {
                var tempPath by remember { mutableStateOf(downloadPath) }
                Dialog(onDismissRequest = { showPathDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ArcRaised,
                        border = BorderStroke(1.dp, Color(0xFF333333)),
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Download Directory",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ArcWhite,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = tempPath,
                                onValueChange = { tempPath = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("Storage Path") },
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showPathDialog = false }) { Text("Cancel") }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        downloadPath = tempPath
                                        preferences.edit().putString("download_path", tempPath).apply()
                                        showPathDialog = false
                                        message("Download directory saved.")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ArcWhite, contentColor = ArcBlack),
                                ) {
                                    Text("Save")
                                }
                            }
                        }
                    }
                }
            }

            // Telegram Setup Dialog
            if (showTelegramSetup) {
                Dialog(onDismissRequest = { showTelegramSetup = false }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ArcRaised,
                        border = BorderStroke(1.dp, Color(0xFF333333)),
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Telegram Session Setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = telegramApiId,
                                onValueChange = { telegramApiId = it },
                                label = { Text("API ID") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = telegramApiHash,
                                onValueChange = { telegramApiHash = it },
                                label = { Text("API HASH") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = telegramSession,
                                onValueChange = { telegramSession = it },
                                label = { Text("String Session") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { showTelegramSetup = false }) { Text("Cancel") }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        preferences.edit()
                                            .putString("telegram_api_id", telegramApiId)
                                            .putString("telegram_api_hash", telegramApiHash)
                                            .putString("telegram_session", telegramSession)
                                            .apply()
                                        showTelegramSetup = false
                                        message("Telegram credentials saved.")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ArcWhite, contentColor = ArcBlack),
                                ) {
                                    Text("Save")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InAppVideoPlayerDialog(
    info: PlaybackInfo,
    onDismiss: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    val context = LocalContext.current
    val exoPlayer = remember(info.url) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .setAllowCrossProtocolRedirects(true)
        if (info.headers.isNotEmpty()) {
            httpDataSourceFactory.setDefaultRequestProperties(info.headers)
        }
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                val mediaItem = MediaItem.fromUri(Uri.parse(info.url))
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = true
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        this.player = exoPlayer
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Top control bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color(0x88000000), CircleShape),
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
                }

                Text(
                    text = info.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                )

                Button(
                    onClick = onOpenExternal,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xAA222222), contentColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("VLC", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    input: String,
    onInputChange: (String) -> Unit,
    onPaste: () -> Unit,
    onAnalyze: () -> Unit,
    onOpenHistoryItem: (String) -> Unit,
    historyItems: List<String>,
    onClearHistory: () -> Unit,
    audioOnly: Boolean,
    onToggleAudio: () -> Unit,
    defaultQuality: String,
    onPickQuality: () -> Unit,
    downloadPath: String,
    onPickPath: () -> Unit,
    onSelectPreset: (String) -> Unit,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
    ) {
        Text(
            text = "ARC",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = ArcWhite,
        )
        Text(
            text = "Universal Media & Stream Engine",
            style = MaterialTheme.typography.bodyMedium,
            color = ArcMuted,
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Search Input Card
        Card(
            colors = CardDefaults.cardColors(containerColor = ArcRaised),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF222222)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search movies, TV shows, or paste URL...") },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (input.isNotBlank()) {
                                IconButton(onClick = { onInputChange("") }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear")
                                }
                            }
                            IconButton(onClick = onPaste) {
                                Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AssistChip(
                        onClick = onPickQuality,
                        label = { Text(defaultQuality) },
                        leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    )
                    Button(
                        onClick = onAnalyze,
                        colors = ButtonDefaults.buttonColors(containerColor = ArcWhite, contentColor = ArcBlack),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Search / Resolve", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Recent History Section
        if (historyItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.History, contentDescription = null, tint = ArcMuted, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recent Searches & Targets",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ArcWhite,
                    )
                }
                TextButton(onClick = onClearHistory) {
                    Text("Clear", color = ArcMuted, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ArcRaised, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp)),
            ) {
                historyItems.take(6).forEachIndexed { idx, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenHistoryItem(item) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = ArcSubtle, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = item,
                            color = ArcWhite,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (idx < historyItems.take(6).lastIndex) {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222222)))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Supported Sources",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = ArcWhite,
        )
        Spacer(modifier = Modifier.height(8.dp))
        val sources = listOf("NetMirror (Movies & TV Series)", "YouTube Videos & Mixes", "Telegram Media", "Direct Video Streams", "Educational Documents")
        sources.forEach { src ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = ArcSubtle, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = src, color = ArcMuted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SearchOverlay(
    query: String,
    loading: Boolean,
    elapsedSeconds: Float,
    hits: List<SearchHit>,
    onClose: () -> Unit,
    onSelect: (SearchHit) -> Unit,
    trace: CliTrace?,
    isTraceExpanded: Boolean,
    onToggleTrace: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ArcBlack)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = ArcWhite)
            }
            Text(
                text = "Search: \"$query\"",
                fontWeight = FontWeight.Bold,
                color = ArcWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )
        }

        if (loading) {
            Spacer(modifier = Modifier.height(30.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.8f), color = ArcWhite)
                Spacer(modifier = Modifier.height(14.dp))
                Text("Searching mirrors & catalogs... (${String.format("%.1fs", elapsedSeconds)})", color = ArcMuted, fontSize = 13.sp)
            }
        } else {
            if (hits.isEmpty()) {
                Spacer(modifier = Modifier.height(40.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = ArcSubtle, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No matching titles found.", color = ArcMuted)
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text("${hits.size} Results", color = ArcSubtle, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(hits) { hit ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ArcRaised),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF222222)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { onSelect(hit) },
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (hit.poster.isNotBlank()) {
                                    AsyncImage(
                                        model = hit.poster,
                                        contentDescription = hit.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp, 80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF222222)),
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = hit.title,
                                        fontWeight = FontWeight.Bold,
                                        color = ArcWhite,
                                        fontSize = 15.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${hit.year} · ${hit.type} · ${hit.source}",
                                        color = ArcMuted,
                                        fontSize = 12.sp,
                                    )
                                    if (hit.audio.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val audios = hit.audio.joinToString(", ") { it.language }
                                        Text(
                                            text = "Audio: $audios",
                                            color = ArcSubtle,
                                            fontSize = 11.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Trace Card
            trace?.let { t ->
                TraceCard(trace = t, isExpanded = isTraceExpanded, onToggle = onToggleTrace)
            }
        }
    }
}

@Composable
private fun AnalysisOverlay(
    target: MediaTarget,
    loading: Boolean,
    elapsedSeconds: Float,
    resolvedUrl: String,
    selectedQuality: String,
    onSelectQuality: (String) -> Unit,
    onClose: () -> Unit,
    onPlayInApp: () -> Unit,
    onPlayExternal: () -> Unit,
    onDownload: () -> Unit,
    onSelectEpisode: (Int, Int) -> Unit,
    onSelectPlaylistItem: (PlaylistEntry) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    trace: CliTrace?,
    isTraceExpanded: Boolean,
    onToggleTrace: () -> Unit,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ArcBlack)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = ArcWhite)
            }
            Text(
                text = target.title,
                fontWeight = FontWeight.Bold,
                color = ArcWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )
            IconButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, contentDescription = "Share", tint = ArcWhite)
            }
        }

        if (loading) {
            Spacer(modifier = Modifier.height(30.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.8f), color = ArcWhite)
                Spacer(modifier = Modifier.height(14.dp))
                Text("Resolving stream endpoint... (${String.format("%.1fs", elapsedSeconds)})", color = ArcMuted, fontSize = 13.sp)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scroll),
            ) {
                // Header with poster and title
                Card(
                    colors = CardDefaults.cardColors(containerColor = ArcRaised),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF222222)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (target.poster.isNotBlank()) {
                                AsyncImage(
                                    model = target.poster,
                                    contentDescription = target.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(72.dp, 104.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF222222)),
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = target.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ArcWhite,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = target.source,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ArcMuted,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(selectedQuality, fontSize = 11.sp) },
                                    )
                                    if (target.isSeries) {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("TV Series", fontSize = 11.sp) },
                                        )
                                    }
                                }
                            }
                        }

                        // Quality options selector
                        if (target.availableQualities.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Available Qualities", fontSize = 12.sp, color = ArcSubtle, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(target.availableQualities) { q ->
                                    FilterChip(
                                        selected = selectedQuality == q,
                                        onClick = { onSelectQuality(q) },
                                        label = { Text(q) },
                                    )
                                }
                            }
                        }
                    }
                }

                // TV Series Seasons & Episodes Picker
                if (target.isSeries && target.seasons.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select Season & Episode", fontWeight = FontWeight.Bold, color = ArcWhite, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    var activeSeasonTab by remember { mutableStateOf(target.selectedSeason) }

                    // Season chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(target.seasons) { s ->
                            FilterChip(
                                selected = activeSeasonTab == s.season,
                                onClick = { activeSeasonTab = s.season },
                                label = { Text("Season ${s.season}") },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    val activeSeason = target.seasons.firstOrNull { it.season == activeSeasonTab } ?: target.seasons.first()
                    // Episode buttons row
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ArcRaised, RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                    ) {
                        Text("Season ${activeSeason.season} Episodes (${activeSeason.episodes} total)", fontSize = 12.sp, color = ArcMuted)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items((1..activeSeason.episodes).toList()) { epNum ->
                                val isSelected = target.selectedSeason == activeSeason.season && target.selectedEpisode == epNum
                                Button(
                                    onClick = { onSelectEpisode(activeSeason.season, epNum) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) ArcWhite else Color(0xFF2A2A2A),
                                        contentColor = if (isSelected) ArcBlack else ArcWhite,
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text("Ep $epNum", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // YouTube Playlist Picker
                if (target.isPlaylist && target.playlistEntries.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Playlist Videos (${target.playlistEntries.size})", fontWeight = FontWeight.Bold, color = ArcWhite, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ArcRaised, RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                    ) {
                        target.playlistEntries.forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectPlaylistItem(entry) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (entry.thumbnail.isNotBlank()) {
                                    AsyncImage(
                                        model = entry.thumbnail,
                                        contentDescription = entry.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(60.dp, 40.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF222222)),
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Text(
                                    text = entry.title,
                                    fontSize = 13.sp,
                                    color = ArcWhite,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onPlayInApp,
                        colors = ButtonDefaults.buttonColors(containerColor = ArcWhite, contentColor = ArcBlack),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play in App", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onDownload,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A), contentColor = ArcWhite),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onPlayExternal,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play in VLC")
                    }
                    OutlinedButton(
                        onClick = onCopy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Link")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                trace?.let { t ->
                    TraceCard(trace = t, isExpanded = isTraceExpanded, onToggle = onToggleTrace)
                }
            }
        }
    }
}

@Composable
private fun ActivityScreen(
    jobs: List<DownloadJob>,
    onPlayInApp: (DownloadJob) -> Unit,
    onOpenExternal: (DownloadJob) -> Unit,
    onCancelJob: (String) -> Unit,
    onDeleteJob: (String, Boolean) -> Unit,
    onShareJob: (DownloadJob) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Activity",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = ArcWhite,
        )
        Text(
            text = "Active and completed downloads",
            style = MaterialTheme.typography.bodyMedium,
            color = ArcMuted,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (jobs.isEmpty()) {
            Spacer(modifier = Modifier.height(50.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null, tint = ArcSubtle, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No downloads yet.", color = ArcMuted)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(jobs) { job ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ArcRaised),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF222222)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (job.poster.isNotBlank()) {
                                    AsyncImage(
                                        model = job.poster,
                                        contentDescription = job.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(48.dp, 64.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF222222)),
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = job.title,
                                        fontWeight = FontWeight.Bold,
                                        color = ArcWhite,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${job.detail} · ${job.source}",
                                        color = ArcMuted,
                                        fontSize = 11.sp,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            when (job.status) {
                                                "Downloading" -> Color(0xFF1E3A5F)
                                                "Complete" -> Color(0xFF1E4620)
                                                else -> Color(0xFF3A1E1E)
                                            },
                                            RoundedCornerShape(8.dp),
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = job.status,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ArcWhite,
                                    )
                                }
                            }

                            // Animated progress bar if downloading
                            if (job.status == "Downloading") {
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { (job.progress / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = ArcWhite,
                                    trackColor = Color(0xFF262626),
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "${String.format("%.1f", job.progress)}% | ${job.downloaded}/${job.total}",
                                        fontSize = 11.sp,
                                        color = ArcMuted,
                                    )
                                    Text(
                                        text = "${job.speed} | ETA ${job.eta}",
                                        fontSize = 11.sp,
                                        color = ArcMuted,
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { onCancelJob(job.id) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    ) {
                                        Icon(Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Cancel", fontSize = 11.sp)
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Button(
                                        onClick = { onPlayInApp(job) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ArcWhite, contentColor = ArcBlack),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    ) {
                                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Play", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onOpenExternal(job) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    ) {
                                        Text("VLC", fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { onShareJob(job) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    ) {
                                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { onDeleteJob(job.id, true) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = ArcSubtle)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    downloadPath: String,
    onOpenFile: (String) -> Unit,
    onShareFile: (String) -> Unit,
    onPlayInApp: (String) -> Unit,
) {
    val dir = remember(downloadPath) { File(downloadPath) }
    var files by remember(downloadPath) {
        val list = if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.filter { it.isFile && !it.name.startsWith(".") }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else emptyList()
        mutableStateOf(list)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = ArcWhite,
        )
        Text(
            text = downloadPath,
            style = MaterialTheme.typography.bodySmall,
            color = ArcMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (files.isEmpty()) {
            Spacer(modifier = Modifier.height(50.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Outlined.Folder, contentDescription = null, tint = ArcSubtle, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No downloaded files found in this folder.", color = ArcMuted)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(files) { file ->
                    val sizeMb = file.length() / (1024 * 1024)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ArcRaised),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF222222)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onPlayInApp(file.absolutePath) },
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (file.extension.lowercase() in listOf("mp4", "mkv", "webm", "avi", "mov")) Icons.Outlined.Movie else Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = ArcWhite,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    fontWeight = FontWeight.Bold,
                                    color = ArcWhite,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${sizeMb}MB · ${file.extension.uppercase()}",
                                    color = ArcMuted,
                                    fontSize = 11.sp,
                                )
                            }
                            IconButton(onClick = { onOpenFile(file.absolutePath) }) {
                                Icon(Icons.Outlined.OpenInNew, contentDescription = "Open", tint = ArcMuted, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onShareFile(file.absolutePath) }) {
                                Icon(Icons.Outlined.Share, contentDescription = "Share", tint = ArcMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    downloadPath: String,
    onPickPath: () -> Unit,
    defaultQuality: String,
    onPickQuality: () -> Unit,
    notifications: Boolean,
    onToggleNotifications: () -> Unit,
    telegramSession: String,
    onOpenTelegramSetup: () -> Unit,
    onOpenPath: () -> Unit,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = ArcWhite,
        )
        Text(
            text = "App preferences & media engine",
            style = MaterialTheme.typography.bodyMedium,
            color = ArcMuted,
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Save Location
        Card(
            colors = CardDefaults.cardColors(containerColor = ArcRaised),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF222222)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPickPath() },
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Folder, contentDescription = null, tint = ArcWhite)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Save Location", fontWeight = FontWeight.Bold, color = ArcWhite, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(downloadPath, color = ArcMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Outlined.Edit, contentDescription = "Change", tint = ArcMuted)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Default Quality
        Card(
            colors = CardDefaults.cardColors(containerColor = ArcRaised),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF222222)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPickQuality() },
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = null, tint = ArcWhite)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Default Quality", fontWeight = FontWeight.Bold, color = ArcWhite, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(defaultQuality, color = ArcMuted, fontSize = 12.sp)
                }
                Icon(Icons.Outlined.Edit, contentDescription = "Change", tint = ArcMuted)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Telegram Auth
        Card(
            colors = CardDefaults.cardColors(containerColor = ArcRaised),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF222222)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenTelegramSetup() },
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Security, contentDescription = null, tint = ArcWhite)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Telegram Integration", fontWeight = FontWeight.Bold, color = ArcWhite, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        if (telegramSession.isNotBlank()) "Connected with session" else "Tap to configure (optional)",
                        color = ArcMuted,
                        fontSize = 12.sp,
                    )
                }
                Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = ArcMuted)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Notifications
        Card(
            colors = CardDefaults.cardColors(containerColor = ArcRaised),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF222222)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.NotificationsNone, contentDescription = null, tint = ArcWhite)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Completion Notifications", fontWeight = FontWeight.Bold, color = ArcWhite, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Notify when downloads finish", color = ArcMuted, fontSize = 12.sp)
                }
                Switch(checked = notifications, onCheckedChange = { onToggleNotifications() })
            }
        }
    }
}

@Composable
private fun TraceCard(
    trace: CliTrace,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ArcRaised),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF2B2B2B)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trace.action,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ArcWhite,
                    )
                    Text(
                        text = "${trace.status} in ${String.format("%.1fs", trace.durationSeconds)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (trace.isError) Color(0xFFFF6B6B) else Color(0xFF6BFF8B),
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = ArcMuted,
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    trace.steps.forEach { step ->
                        Text(
                            text = "> $step",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (step.startsWith("[ERROR]")) Color(0xFFFF6B6B) else ArcMuted,
                            modifier = Modifier.padding(vertical = 1.dp),
                        )
                    }
                    if (trace.stdoutLogs.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Log Output:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ArcSubtle,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = Color(0xFF0A0A0A),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = trace.stdoutLogs,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFCCCCCC),
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
