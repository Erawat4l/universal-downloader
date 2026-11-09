package com.erawat.arc

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Terminal
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

    var currentTrace by remember { mutableStateOf<CliTrace?>(null) }
    var isTraceExpanded by rememberSaveable { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableFloatStateOf(0f) }
    var liveStepText by remember { mutableStateOf("") }

    var playingMedia by remember { mutableStateOf<PlaybackInfo?>(null) }

    val tab = AppTab.entries.firstOrNull { it.name == tabName } ?: AppTab.HOME

    fun message(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }

    fun copyToClipboard(text: String, label: String = "Copied") {
        clipboard.setText(AnnotatedString(text))
        message("$label copied to clipboard.")
    }

    LaunchedEffect(Unit) {
        scope.launch {
            update = GitHubUpdateChecker.check()
        }
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
            overlay = "analysis"
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
            title = customTitle.ifBlank { target.title.ifBlank { "Media Download" } },
            source = target.source.ifBlank { "Stream" },
            detail = "${selectedQuality}${if (audioOnly) " · Audio" else " · Video"}",
            status = "Downloading",
            poster = target.poster,
            url = targetUrl,
            headers = target.streamHeaders,
        )
        jobs = listOf(job) + jobs
        message("Download started: ${job.title}")

        val payload = JSONObject().apply {
            put("action", "download")
            put("url", input)
            put("stream_url", resolvedUrl)
            put("title", job.title)
            put("quality", selectedQuality)
            put("target_dir", downloadPath)
            put("job_id", job.id)
            if (target.streamHeaders.isNotEmpty()) {
                val hObj = JSONObject()
                target.streamHeaders.forEach { (k, v) -> hObj.put(k, v) }
                put("headers", hObj)
            }
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
                NavigationBar(containerColor = ArcSurface) {
                    AppTab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = {
                                tabName = item.name
                                overlay = ""
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            color = ArcBlack,
        ) {
            when {
                overlay == "search" -> SearchScreen(
                    query = query,
                    loading = searchLoading,
                    hits = hits,
                    trace = currentTrace,
                    elapsedSeconds = elapsedSeconds,
                    liveStep = liveStepText,
                    isExpanded = isTraceExpanded,
                    onToggleExpand = { isTraceExpanded = !isTraceExpanded },
                    onCopyLogs = { copyToClipboard(currentTrace?.stdoutLogs.orEmpty(), "Process logs") },
                    onBack = ::openHome,
                    onSelect = { hit, audio ->
                        input = audio.url.ifEmpty { hit.url }
                        analyze(audio.url.ifEmpty { hit.url }, titleOverride = hit.title, posterOverride = hit.poster)
                    },
                )
                overlay == "analysis" -> AnalysisScreen(
                    target = target,
                    loading = resolveLoading,
                    trace = currentTrace,
                    elapsedSeconds = elapsedSeconds,
                    liveStep = liveStepText,
                    isExpanded = isTraceExpanded,
                    onToggleExpand = { isTraceExpanded = !isTraceExpanded },
                    onCopyLogs = { copyToClipboard(currentTrace?.stdoutLogs.orEmpty(), "Process logs") },
                    quality = selectedQuality,
                    audioOnly = audioOnly,
                    onBack = ::openHome,
                    onQuality = { selectedQuality = it },
                    onAudioOnly = { audioOnly = it },
                    onDownload = { startDownload() },
                    onStream = {
                        if (resolvedUrl.isNotBlank()) {
                            playingMedia = PlaybackInfo(resolvedUrl, target.title, target.streamHeaders)
                        } else {
                            message("Stream link not ready yet.")
                        }
                    },
                    onCopyLink = { copyToClipboard(resolvedUrl.ifEmpty { input }, "Stream link") },
                    onSelectSeason = { s -> resolveEpisode(s, 1) },
                    onSelectEpisode = { s, e -> resolveEpisode(s, e) },
                    onSelectPlaylistItem = { entry ->
                        input = entry.url
                        analyze(entry.url, titleOverride = entry.title, posterOverride = entry.thumbnail)
                    },
                )
                else -> when (tab) {
                    AppTab.HOME -> HomeScreen(
                        input = input,
                        onInput = { input = it },
                        onAnalyze = { analyze() },
                        onPaste = {
                            clipboard.getText()?.text?.let {
                                input = it
                                message("Pasted from clipboard.")
                            }
                        },
                        onLibrary = { tabName = AppTab.LIBRARY.name },
                        jobs = jobs,
                        onOpen = { openFile(it) },
                        onShare = { shareFile(it) },
                    )
                    AppTab.ACTIVITY -> ActivityScreen(
                        jobs = jobs,
                        onOpen = { openFile(it) },
                        onShare = { shareFile(it) },
                        onPlayInApp = { job ->
                            if (job.path.isNotBlank() && File(job.path).exists()) {
                                playingMedia = PlaybackInfo(job.path, job.title)
                            } else if (job.url.isNotBlank()) {
                                playingMedia = PlaybackInfo(job.url, job.title, job.headers)
                            } else {
                                message("Media file not ready.")
                            }
                        },
                        onCancelJob = { cancelJob(it) },
                        onDeleteJob = { id, removeFile -> deleteJob(id, removeFile) },
                    )
                    AppTab.LIBRARY -> LibraryScreen(
                        jobs = jobs,
                        onOpen = { openFile(it) },
                        onShare = { shareFile(it) },
                        onPlayInApp = { job ->
                            if (job.path.isNotBlank() && File(job.path).exists()) {
                                playingMedia = PlaybackInfo(job.path, job.title)
                            } else {
                                openFile(job.path)
                            }
                        },
                        onDeleteJob = { id, removeFile -> deleteJob(id, removeFile) },
                    )
                    AppTab.SETTINGS -> SettingsScreen(
                        downloadPath = downloadPath,
                        onOpenPathDialog = { showPathDialog = true },
                        defaultQuality = defaultQuality,
                        onOpenQualityDialog = { showQualityDialog = true },
                        notifications = notifications,
                        onToggleNotifications = { notifications = it },
                        onTelegramSetup = { showTelegramSetup = true },
                        update = update,
                        onCheckUpdates = {
                            scope.launch {
                                update = GitHubUpdateChecker.check()
                                message(if (update.appUpdateAvailable) "Update available: ${update.releaseTag}" else "App is up to date.")
                            }
                        },
                        onOpenRelease = {
                            val url = update.releaseUrl.ifBlank { "https://github.com/Erawat4l/universal-downloader/releases" }
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        },
                    )
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

            // Save Path Dialog
            if (showPathDialog) {
                SaveLocationDialog(
                    currentPath = downloadPath,
                    onPathSelected = {
                        downloadPath = it
                        preferences.edit().putString("download_path", it).apply()
                        showPathDialog = false
                        message("Save location updated.")
                    },
                    onDismiss = { showPathDialog = false },
                )
            }

            // Quality Dialog
            if (showQualityDialog) {
                DefaultQualityDialog(
                    currentQuality = defaultQuality,
                    onQualitySelected = {
                        defaultQuality = it
                        preferences.edit().putString("default_quality", it).apply()
                        showQualityDialog = false
                        message("Default quality set to $it")
                    },
                    onDismiss = { showQualityDialog = false },
                )
            }

            // Telegram Setup Dialog
            if (showTelegramSetup) {
                TelegramSetupDialog(
                    apiId = telegramApiId,
                    apiHash = telegramApiHash,
                    session = telegramSession,
                    onSave = { id, hash, sess ->
                        telegramApiId = id
                        telegramApiHash = hash
                        telegramSession = sess
                        preferences.edit()
                            .putString("telegram_api_id", id)
                            .putString("telegram_api_hash", hash)
                            .putString("telegram_session", sess)
                            .apply()
                        showTelegramSetup = false
                        message("Telegram session saved.")
                    },
                    onDismiss = { showTelegramSetup = false },
                )
            }
        }
    }
}

@Composable
private fun ClaudeThinkingCard(
    isThinking: Boolean,
    trace: CliTrace?,
    elapsedSeconds: Float,
    liveStep: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCopyLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isThinking && trace == null) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "alpha",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ArcSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isThinking) Color(0xFF383838) else Color(0xFF222222)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggleExpand)
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (isThinking) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1E1E)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Terminal,
                            contentDescription = null,
                            tint = ArcWhite.copy(alpha = alphaAnim),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (trace?.isError == true) Color(0xFF381515) else Color(0xFF1B2E1E)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (trace?.isError == true) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = if (trace?.isError == true) Color(0xFFFF5252) else Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (isThinking) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Processing...",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = ArcWhite,
                            )
                            Text(
                                String.format("[%.1fs]", elapsedSeconds),
                                fontSize = 11.sp,
                                color = ArcMuted,
                            )
                        }
                        if (liveStep.isNotBlank()) {
                            Text(
                                liveStep,
                                fontSize = 11.sp,
                                color = ArcSubtle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else if (trace != null) {
                        Text(
                            "Completed in ${String.format("%.1fs", trace.durationSeconds)} · ${trace.steps.size} steps",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = ArcWhite,
                        )
                        Text(
                            trace.action,
                            fontSize = 11.sp,
                            color = ArcMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        if (isExpanded) "Hide" else "Details",
                        fontSize = 11.sp,
                        color = ArcMuted,
                        fontWeight = FontWeight.Medium,
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = ArcMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF070707))
                        .border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isThinking) Color(0xFFFFB300) else Color(0xFF4CAF50)))
                            Text(
                                "Execution Process Trace",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = ArcMuted,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        if (trace?.stdoutLogs?.isNotBlank() == true) {
                            TextButton(
                                onClick = onCopyLogs,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.height(24.dp),
                            ) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = ArcMuted)
                                Spacer(Modifier.width(4.dp))
                                Text("Copy", fontSize = 10.sp, color = ArcMuted)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        trace?.steps?.forEach { step ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    ">",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    step,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFFD4D4D4),
                                    lineHeight = 15.sp,
                                )
                            }
                        }

                        if (isThinking && liveStep.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    ">",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFFFFB300),
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "$liveStep _",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFFFFB300),
                                    lineHeight = 15.sp,
                                )
                            }
                        }

                        if (trace?.stdoutLogs?.isNotBlank() == true) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "--- Process Log Output ---",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = ArcSubtle,
                            )
                            Text(
                                trace.stdoutLogs,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color(0xFFAAAAAA),
                                lineHeight = 14.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    input: String,
    onInput: (String) -> Unit,
    onAnalyze: () -> Unit,
    onPaste: () -> Unit,
    onLibrary: () -> Unit,
    jobs: List<DownloadJob>,
    onOpen: (String) -> Unit,
    onShare: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BrandMark()
                Column {
                    Text("Arc", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Universal Downloader", color = ArcMuted, fontSize = 11.sp)
                }
                Spacer(Modifier.weight(1f))
                StatusPill("Ready")
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            Text("Universal Downloader", fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(4.dp))
            Text("Download and stream media directly on your device", color = ArcMuted, fontSize = 13.sp)
        }
        item {
            OutlinedTextField(
                value = input,
                onValueChange = onInput,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                label = { Text("Link or search") },
                leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                trailingIcon = { TextButton(onClick = onPaste) { Text("Paste") } },
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onAnalyze,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ArcWhite, contentColor = ArcBlack),
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Analyze", fontWeight = FontWeight.Bold)
            }
        }
        item { SectionHeader("Recent Downloads", "See Library", onLibrary) }
        if (jobs.isEmpty()) {
            item { EmptyState("No Downloads Yet", "Media links and downloads will appear here.") }
        } else {
            items(jobs.take(3)) { job -> JobRow(job, onOpen = onOpen, onShare = onShare) }
        }
    }
}

@Composable
private fun SearchScreen(
    query: String,
    loading: Boolean,
    hits: List<SearchHit>,
    trace: CliTrace?,
    elapsedSeconds: Float,
    liveStep: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCopyLogs: () -> Unit,
    onBack: () -> Unit,
    onSelect: (SearchHit, AudioChoice) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                Column {
                    Text("Search Results", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(query.replaceFirstChar { it.titlecase() }, color = ArcMuted, fontSize = 12.sp)
                }
            }
        }

        item {
            ClaudeThinkingCard(
                isThinking = loading,
                trace = trace,
                elapsedSeconds = elapsedSeconds,
                liveStep = liveStep,
                isExpanded = isExpanded,
                onToggleExpand = onToggleExpand,
                onCopyLogs = onCopyLogs,
            )
        }

        if (!loading && hits.isEmpty()) {
            item { EmptyState("No Results", "No matching media was found.") }
        }

        items(hits) { hit ->
            Card(colors = CardDefaults.cardColors(containerColor = ArcSurface), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hit.poster.isNotBlank()) {
                            AsyncImage(
                                model = hit.poster,
                                contentDescription = hit.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp, 60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF222222)),
                            )
                        } else {
                            MediaGlyph(Icons.Outlined.Movie, Modifier.size(44.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(hit.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${hit.type}${if (hit.year.isNotBlank()) " · ${hit.year}" else ""} · ${hit.source}", color = ArcMuted, fontSize = 11.sp)
                            if (hit.providers.isNotBlank()) Text(hit.providers, color = ArcSubtle, fontSize = 10.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        if (hit.audio.isEmpty()) {
                            FilterChip(
                                selected = false,
                                onClick = { onSelect(hit, AudioChoice("Video", hit.url)) },
                                label = { Text("Use result") },
                            )
                        } else {
                            hit.audio.forEach { audio ->
                                FilterChip(selected = false, onClick = { onSelect(hit, audio) }, label = { Text(audio.language) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalysisScreen(
    target: MediaTarget,
    loading: Boolean,
    trace: CliTrace?,
    elapsedSeconds: Float,
    liveStep: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCopyLogs: () -> Unit,
    quality: String,
    audioOnly: Boolean,
    onBack: () -> Unit,
    onQuality: (String) -> Unit,
    onAudioOnly: (Boolean) -> Unit,
    onDownload: () -> Unit,
    onStream: () -> Unit,
    onCopyLink: () -> Unit,
    onSelectSeason: ((Int) -> Unit)? = null,
    onSelectEpisode: ((Int, Int) -> Unit)? = null,
    onSelectPlaylistItem: ((PlaylistEntry) -> Unit)? = null,
) {
    val displayQualities = remember(target.availableQualities) {
        if (target.availableQualities.isNotEmpty()) {
            target.availableQualities
        } else {
            listOf("Best", "1080p", "720p", "480p")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                Column {
                    Text(if (loading) "Analyzing" else "Review", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(if (loading) "Processing media details..." else "Choose quality and action.", color = ArcMuted, fontSize = 12.sp)
                }
            }
        }

        item {
            ClaudeThinkingCard(
                isThinking = loading,
                trace = trace,
                elapsedSeconds = elapsedSeconds,
                liveStep = liveStep,
                isExpanded = isExpanded,
                onToggleExpand = onToggleExpand,
                onCopyLogs = onCopyLogs,
            )
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = ArcSurface), shape = RoundedCornerShape(22.dp)) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (target.poster.isNotBlank()) {
                        AsyncImage(
                            model = target.poster,
                            contentDescription = target.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp, 88.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF222222)),
                        )
                    } else {
                        MediaGlyph(Icons.Outlined.Movie, Modifier.size(76.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(target.source.uppercase().ifBlank { "MEDIA" }, color = ArcMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text(target.title.ifBlank { "Resolving link..." }, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        Text(target.detail, color = ArcMuted, fontSize = 11.sp)
                    }
                }
            }
        }

        // TV Series season and episode selectors if available
        if (!loading && target.isSeries && target.seasons.isNotEmpty()) {
            item {
                SectionHeader("Season", "S${target.selectedSeason}")
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(target.seasons) { sInfo ->
                        FilterChip(
                            selected = target.selectedSeason == sInfo.season,
                            onClick = { onSelectSeason?.invoke(sInfo.season) },
                            label = { Text("Season ${sInfo.season}") },
                        )
                    }
                }
            }
            item {
                val currentSeason = target.seasons.firstOrNull { it.season == target.selectedSeason } ?: target.seasons.first()
                SectionHeader("Episode", "E${target.selectedEpisode}")
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((1..currentSeason.episodes).toList()) { epNum ->
                        FilterChip(
                            selected = target.selectedEpisode == epNum,
                            onClick = { onSelectEpisode?.invoke(target.selectedSeason, epNum) },
                            label = { Text("Episode $epNum") },
                        )
                    }
                }
            }
        }

        // YouTube playlist entries if available
        if (!loading && target.isPlaylist && target.playlistEntries.isNotEmpty()) {
            item {
                SectionHeader("Playlist Items", "${target.playlistEntries.size} Videos")
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    target.playlistEntries.take(15).forEach { entry ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ArcRaised),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPlaylistItem?.invoke(entry) },
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (entry.thumbnail.isNotBlank()) {
                                    AsyncImage(
                                        model = entry.thumbnail,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(48.dp, 36.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                    )
                                }
                                Text(
                                    entry.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ArcWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp), tint = ArcMuted)
                            }
                        }
                    }
                }
            }
        }

        if (!loading) {
            item {
                SectionHeader("Quality", "$quality Selected")
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayQualities.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { option ->
                                FilterChip(
                                    selected = quality == option,
                                    onClick = { onQuality(option) },
                                    label = { Text(option) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(selected = !audioOnly, onClick = { onAudioOnly(false) }, label = { Text("Video") }, modifier = Modifier.weight(1f))
                    FilterChip(selected = audioOnly, onClick = { onAudioOnly(true) }, label = { Text("Audio Only") }, modifier = Modifier.weight(1f))
                }
            }
            item {
                Button(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ArcWhite, contentColor = ArcBlack),
                ) {
                    Icon(Icons.Outlined.FileDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Download $quality", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onStream, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(5.dp))
                        Text("Stream")
                    }
                    OutlinedButton(onClick = onCopyLink, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Spacer(Modifier.width(5.dp))
                        Text("Copy Link")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityScreen(
    jobs: List<DownloadJob>,
    onOpen: (String) -> Unit,
    onShare: (String) -> Unit,
    onPlayInApp: (DownloadJob) -> Unit,
    onCancelJob: (String) -> Unit,
    onDeleteJob: (String, Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Activity", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Active and completed downloads", color = ArcMuted, fontSize = 13.sp)
        }
        if (jobs.isEmpty()) {
            item { EmptyState("No Activity", "Downloads will appear here.") }
        } else {
            items(jobs) { job ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ArcRaised),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF222222)),
                    modifier = Modifier.fillMaxWidth(),
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
                            } else {
                                MediaGlyph(Icons.Outlined.FileDownload, Modifier.size(42.dp))
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

                        // Animated progress indicator if downloading
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
                        } else if (job.status == "Complete") {
                            Spacer(modifier = Modifier.height(10.dp))
                            if (job.path.isNotBlank()) {
                                Text(job.path, color = ArcSubtle, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
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
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedButton(
                                    onClick = { onOpen(job.path) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text("Open", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedButton(
                                    onClick = { onShare(job.path) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text("Share", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { onDeleteJob(job.id, true) },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                }
                            }
                        } else {
                            // Cancelled or Failed
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { onDeleteJob(job.id, false) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text("Dismiss", fontSize = 11.sp)
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
    jobs: List<DownloadJob>,
    onOpen: (String) -> Unit,
    onShare: (String) -> Unit,
    onPlayInApp: (DownloadJob) -> Unit,
    onDeleteJob: (String, Boolean) -> Unit,
) {
    val completed = jobs.filter { it.status == "Complete" }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Library", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Downloaded media files on device.", color = ArcMuted, fontSize = 13.sp)
        }
        if (completed.isEmpty()) {
            item { EmptyState("No Files Yet", "Completed downloads will appear here.") }
        } else {
            items(completed) { job ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ArcRaised),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF222222)),
                    modifier = Modifier.fillMaxWidth(),
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
                            } else {
                                MediaGlyph(Icons.Outlined.Movie, Modifier.size(42.dp))
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
                        }
                        if (job.path.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(job.path, color = ArcSubtle, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
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
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedButton(
                                onClick = { onOpen(job.path) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text("Open", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedButton(
                                onClick = { onShare(job.path) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text("Share", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { onDeleteJob(job.id, true) },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
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
    onOpenPathDialog: () -> Unit,
    defaultQuality: String,
    onOpenQualityDialog: () -> Unit,
    notifications: Boolean,
    onToggleNotifications: (Boolean) -> Unit,
    onTelegramSetup: () -> Unit,
    update: UpdateInfo,
    onCheckUpdates: () -> Unit,
    onOpenRelease: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Preferences and app configuration", color = ArcMuted, fontSize = 13.sp)
        }
        item { GitHubCard(update, onCheckUpdates, onOpenRelease) }
        item {
            SettingsGroup("Downloads")

            Card(
                colors = CardDefaults.cardColors(containerColor = ArcSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPathDialog),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                ) {
                    MediaGlyph(Icons.Outlined.Folder, Modifier.size(36.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Save Location", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(downloadPath, color = ArcMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Outlined.Edit, contentDescription = "Change", tint = ArcMuted, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(6.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = ArcSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenQualityDialog),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                ) {
                    MediaGlyph(Icons.Outlined.Settings, Modifier.size(36.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Default Quality", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("$defaultQuality Selected", color = ArcMuted, fontSize = 11.sp)
                    }
                    Icon(Icons.Outlined.Edit, contentDescription = "Change", tint = ArcMuted, modifier = Modifier.size(18.dp))
                }
            }
        }

        item {
            SettingsGroup("Preferences")
            SettingRow(
                title = "Notifications",
                detail = "Alerts when downloads finish",
                icon = Icons.Outlined.NotificationsNone,
            ) {
                Switch(checked = notifications, onCheckedChange = onToggleNotifications)
            }
        }

        item {
            SettingsGroup("Integrations")
            Card(
                colors = CardDefaults.cardColors(containerColor = ArcSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTelegramSetup),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                ) {
                    MediaGlyph(Icons.AutoMirrored.Outlined.Send, Modifier.size(36.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Telegram Session", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Configure Pyrogram session credentials", color = ArcMuted, fontSize = 11.sp)
                    }
                    Icon(Icons.Outlined.Edit, contentDescription = "Configure", tint = ArcMuted, modifier = Modifier.size(18.dp))
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
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("VLC", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SaveLocationDialog(
    currentPath: String,
    onPathSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val publicPath = remember {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
            ?: "/storage/emulated/0/Download"
    }
    val appPrivatePath = remember {
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
            ?: context.filesDir.resolve("downloads").absolutePath
    }
    var inputPath by remember { mutableStateOf(currentPath) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download Save Location") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Choose where downloaded media files will be saved on your device.",
                    color = ArcMuted,
                    fontSize = 12.sp,
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (inputPath == publicPath) ArcRaised else ArcSurface,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (inputPath == publicPath) ArcWhite else Color(0xFF333333)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { inputPath = publicPath },
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Outlined.Folder, contentDescription = null, tint = ArcWhite)
                            Column(Modifier.weight(1f)) {
                                Text("Public Downloads", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(publicPath, color = ArcMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (inputPath == publicPath) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = ArcWhite, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (inputPath == appPrivatePath) ArcRaised else ArcSurface,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (inputPath == appPrivatePath) ArcWhite else Color(0xFF333333)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { inputPath = appPrivatePath },
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Outlined.Folder, contentDescription = null, tint = ArcWhite)
                            Column(Modifier.weight(1f)) {
                                Text("App Storage", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(appPrivatePath, color = ArcMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (inputPath == appPrivatePath) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = ArcWhite, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = inputPath,
                    onValueChange = { inputPath = it },
                    label = { Text("Custom Path") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPathSelected(inputPath) },
                colors = ButtonDefaults.buttonColors(containerColor = ArcWhite, contentColor = ArcBlack),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DefaultQualityDialog(
    currentQuality: String,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf("Best", "1080p", "720p", "480p", "Audio Only")
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default Download Quality") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { option ->
                    val isSelected = currentQuality.equals(option, ignoreCase = true)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) ArcRaised else ArcSurface,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isSelected) ArcWhite else Color(0xFF333333)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onQualitySelected(option) },
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(option, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            if (isSelected) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = ArcWhite, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun TelegramSetupDialog(
    apiId: String,
    apiHash: String,
    session: String,
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var id by remember { mutableStateOf(apiId) }
    var hash by remember { mutableStateOf(apiHash) }
    var sess by remember { mutableStateOf(session) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Telegram Session Credentials") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enter Pyrogram session details for Telegram media downloads.", color = ArcMuted, fontSize = 11.sp)
                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it },
                    label = { Text("API ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = hash,
                    onValueChange = { hash = it },
                    label = { Text("API HASH") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = sess,
                    onValueChange = { sess = it },
                    label = { Text("STRING_SESSION") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(id.trim(), hash.trim(), sess.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = ArcWhite, contentColor = ArcBlack),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun GitHubCard(update: UpdateInfo, onCheck: () -> Unit, onOpenRelease: () -> Unit) {
    val title = when {
        update.loading -> "Checking updates..."
        update.error.isNotBlank() -> "Update check unavailable"
        update.appUpdateAvailable -> "App update ready"
        update.sourceUpdateAvailable -> "Source update found"
        else -> "Up to date"
    }
    val body = when {
        update.loading -> "Checking GitHub for updates."
        update.error.isNotBlank() -> "Could not reach repository."
        update.appUpdateAvailable -> "Release ${update.releaseTag.ifBlank { "Latest" }} includes a new APK."
        update.sourceUpdateAvailable -> "New commits available on repository."
        else -> "You are on the latest version."
    }
    Card(colors = CardDefaults.cardColors(containerColor = ArcSurface), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                MediaGlyph(if (update.appUpdateAvailable) Icons.Outlined.FileDownload else Icons.Outlined.Refresh, Modifier.size(40.dp))
                Column(Modifier.weight(1f)) {
                    Text("Updates", color = ArcMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(body, color = ArcMuted, fontSize = 11.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Universal Downloader", color = ArcSubtle, fontSize = 10.sp)
                TextButton(onClick = if (update.appUpdateAvailable) onOpenRelease else onCheck, enabled = !update.loading) { Text(if (update.appUpdateAvailable) "Download Update" else "Check Now") }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun SettingsGroup(title: String) {
    Text(title, color = ArcMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp, bottom = 3.dp))
}

@Composable
private fun SettingRow(title: String, detail: String, icon: ImageVector, control: @Composable (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        MediaGlyph(icon, Modifier.size(36.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(detail, color = ArcMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (control != null) control()
    }
}

@Composable
private fun JobRow(
    job: DownloadJob,
    onOpen: (String) -> Unit = {},
    onShare: (String) -> Unit = {},
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ArcSurface),
        shape = RoundedCornerShape(17.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = job.status == "Complete" && job.path.isNotBlank()) {
                onOpen(job.path)
            },
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (job.poster.isNotBlank()) {
                    AsyncImage(
                        model = job.poster,
                        contentDescription = job.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp, 48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF222222)),
                    )
                } else {
                    MediaGlyph(Icons.Outlined.FileDownload, Modifier.size(34.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(job.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${job.source} · ${job.detail}", color = ArcMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    job.status,
                    color = when (job.status) {
                        "Complete" -> Color(0xFF4CAF50)
                        "Downloading" -> Color(0xFF2196F3)
                        else -> Color(0xFFFF5252)
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (job.status == "Complete" && job.path.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { onOpen(job.path) }) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Open", fontSize = 12.sp)
                    }
                    TextButton(onClick = { onShare(job.path) }) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Surface(shape = CircleShape, color = ArcSurface, border = BorderStroke(1.dp, Color(0xFF333333))) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(ArcWhite))
            Text(text, color = ArcMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BrandMark() {
    Surface(modifier = Modifier.size(42.dp), shape = RoundedCornerShape(14.dp), color = ArcWhite) {
        Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.FileDownload, contentDescription = null, tint = ArcBlack) }
    }
}

@Composable
private fun MediaGlyph(icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = ArcRaised) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = ArcWhite) }
    }
}

@Composable
private fun EmptyState(title: String, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = ArcSurface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            MediaGlyph(Icons.Outlined.Link, Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(detail, color = ArcMuted, fontSize = 12.sp)
        }
    }
}
