package com.erawat.arc

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
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
)
private data class MediaTarget(
    val title: String,
    val source: String,
    val detail: String,
    val size: String,
    val description: String,
    val availableQualities: List<String> = emptyList(),
)
private data class DownloadJob(
    val id: String,
    val title: String,
    val source: String,
    val detail: String,
    val status: String,
    val path: String = "",
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
    var clipboardHelper by rememberSaveable { mutableStateOf(true) }
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

    LaunchedEffect(searchLoading, resolveLoading) {
        if (searchLoading || resolveLoading) {
            elapsedSeconds = 0f
            val isSearch = searchLoading
            val searchSteps = listOf(
                "Initializing media engine...",
                "Querying content catalogs and active mirrors...",
                "Scanning provider stream manifests...",
                "Parsing multi-language audio tracks...",
                "Extracting resolution profiles...",
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
            context.startActivity(Intent.createChooser(intent, "Share media"))
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

                target = target.copy(
                    title = data?.optString("title").takeUnless { it.isNullOrBlank() } ?: target.title,
                    source = data?.optString("source").takeUnless { it.isNullOrBlank() } ?: target.source,
                    detail = "${selectedQuality}${if (audioOnly) " · Audio" else " · Video"}",
                    availableQualities = qualitiesList,
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
                        "Target URL resolved",
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
                jobs = jobs.map { if (it.id == result.jobId) it.copy(status = "Complete", path = path) else it }
                message("Download complete.")
            }
        }
    }

    fun submit(payload: JSONObject, type: String): String {
        val jobId = java.util.UUID.randomUUID().toString()
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

    fun analyze(value: String = input) {
        val clean = value.trim()
        if (clean.isEmpty()) {
            message("Paste a link or enter a search.")
            return
        }
        input = clean
        if (defaultQuality != "Ask each time") {
            selectedQuality = defaultQuality
        }
        if (clean.startsWith("http://") || clean.startsWith("https://")) {
            resolveLoading = true
            currentTrace = null
            overlay = "analysis"
            submit(JSONObject().apply {
                put("action", "resolve")
                put("url", clean)
                put("quality", if (audioOnly) "audio" else selectedQuality)
            }, "resolve")
        } else {
            val youtubeSearch = clean.startsWith("yt:", ignoreCase = true) ||
                clean.startsWith("youtube:", ignoreCase = true)
            val searchQuery = clean.replace(Regex("^(yt|youtube):\\s*", RegexOption.IGNORE_CASE), "").trim()
            query = searchQuery
            hits = emptyList()
            searchLoading = true
            currentTrace = null
            overlay = "search"
            submit(JSONObject().apply {
                put("action", "search")
                put("query", searchQuery)
                put("platform", if (youtubeSearch) "youtube" else "netmirror")
            }, "search")
        }
    }

    fun selectSearchResult(hit: SearchHit, audio: AudioChoice) {
        val selectedUrl = audio.url.ifBlank { hit.url }
        input = selectedUrl
        query = hit.title
        target = MediaTarget(
            title = hit.title,
            source = hit.source,
            detail = "${hit.type}${if (hit.year.isNotBlank()) " · ${hit.year}" else ""} · ${audio.language}",
            size = "Size varies",
            description = "Selected media item.",
        )
        if (defaultQuality != "Ask each time") {
            selectedQuality = defaultQuality
        }
        resolveLoading = true
        currentTrace = null
        overlay = "analysis"
        submit(JSONObject().apply {
            put("action", "resolve")
            put("url", selectedUrl)
            put("quality", if (audioOnly) "audio" else selectedQuality)
        }, "resolve")
    }

    fun download() {
        if (input.isBlank()) {
            message("Analyze a link first.")
            return
        }
        try {
            File(downloadPath).mkdirs()
        } catch (_: Exception) {}

        val jobId = submit(JSONObject().apply {
            put("action", "download")
            put("url", input)
            put("quality", if (audioOnly) "audio" else selectedQuality)
            put("target_dir", downloadPath)
            put("telegram", JSONObject().apply {
                put("api_id", telegramApiId)
                put("api_hash", telegramApiHash)
                put("session", telegramSession)
            })
        }, "download")
        jobs = listOf(
            DownloadJob(jobId, target.title.ifBlank { input }, target.source.ifBlank { "Media engine" }, selectedQuality, "Downloading"),
        ) + jobs
        overlay = ""
        tabName = AppTab.ACTIVITY.name
        message("Downloading to $downloadPath")
    }

    fun stream() {
        if (resolvedUrl.isBlank()) {
            message("Resolve the stream first.")
            return
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resolvedUrl)).apply { type = "video/*" })
        } catch (_: Exception) {
            message("No compatible video player found.")
        }
    }

    fun copyResolvedLink() {
        if (resolvedUrl.isBlank()) {
            message("Resolve the stream first.")
            return
        }
        copyToClipboard(resolvedUrl, "Direct stream link")
    }

    fun paste() {
        val value = clipboard.getText()?.text.orEmpty()
        if (value.isBlank()) message("Clipboard is empty.") else input = value
    }

    LaunchedEffect(Unit) {
        update = GitHubUpdateChecker.check()
    }

    val navigate: (AppTab) -> Unit = { next -> tabName = next.name; overlay = "" }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ArcBlack,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = ArcSurface) {
                AppTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item && overlay.isEmpty(),
                        onClick = { navigate(item) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding).imePadding(), color = ArcBlack) {
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
                    onSelect = ::selectSearchResult,
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
                    onDownload = ::download,
                    onStream = ::stream,
                    onCopyLink = ::copyResolvedLink,
                )
                tab == AppTab.HOME -> HomeScreen(
                    input = input,
                    onInput = { input = it },
                    onAnalyze = { analyze() },
                    onPaste = ::paste,
                    onQuickSearch = { input = ""; message("Enter a title to search.") },
                    onTelegram = { tabName = AppTab.SETTINGS.name; overlay = ""; showTelegramSetup = true },
                    onLibrary = { navigate(AppTab.LIBRARY) },
                    jobs = jobs,
                    onOpen = ::openFile,
                    onShare = ::shareFile,
                )
                tab == AppTab.ACTIVITY -> ActivityScreen(jobs, onOpen = ::openFile, onShare = ::shareFile)
                tab == AppTab.LIBRARY -> LibraryScreen(jobs, onOpen = ::openFile, onShare = ::shareFile)
                tab == AppTab.SETTINGS -> SettingsScreen(
                    update = update,
                    notifications = notifications,
                    clipboardHelper = clipboardHelper,
                    downloadPath = downloadPath,
                    defaultQuality = defaultQuality,
                    onNotifications = { notifications = it },
                    onClipboard = { clipboardHelper = it },
                    onOpenPathDialog = { showPathDialog = true },
                    onOpenQualityDialog = { showQualityDialog = true },
                    onCheckUpdates = { scope.launch { update = UpdateInfo(); update = GitHubUpdateChecker.check() } },
                    onOpenRelease = {
                        val url = update.releaseUrl.ifBlank { update.apkUrl }
                        if (url.isNotBlank()) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        else message("No APK release is available.")
                    },
                    onInfo = ::showInfo,
                    onTelegramSetup = { showTelegramSetup = true },
                )
            }
        }
    }

    if (showPathDialog) {
        SaveLocationDialog(
            currentPath = downloadPath,
            onPathSelected = { newPath ->
                downloadPath = newPath
                preferences.edit().putString("download_path", newPath).apply()
                showPathDialog = false
                message("Save location updated.")
            },
            onDismiss = { showPathDialog = false },
        )
    }

    if (showQualityDialog) {
        DefaultQualityDialog(
            currentQuality = defaultQuality,
            onQualitySelected = { newQuality ->
                defaultQuality = newQuality
                preferences.edit().putString("default_quality", newQuality).apply()
                if (newQuality != "Ask each time") {
                    selectedQuality = newQuality
                }
                showQualityDialog = false
                message("Default quality set to $newQuality.")
            },
            onDismiss = { showQualityDialog = false },
        )
    }

    if (infoTitle.isNotBlank()) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { infoTitle = ""; infoBody = "" },
            title = { Text(infoTitle) },
            text = { Text(infoBody) },
            confirmButton = { TextButton(onClick = { infoTitle = ""; infoBody = "" }) { Text("Close") } },
        )
    }

    if (showTelegramSetup) {
        TelegramSetupDialog(
            apiId = telegramApiId,
            apiHash = telegramApiHash,
            session = telegramSession,
            onApiId = { telegramApiId = it },
            onApiHash = { telegramApiHash = it },
            onSession = { telegramSession = it },
            onDismiss = { showTelegramSetup = false },
            onSave = {
                preferences.edit()
                    .putString("telegram_api_id", telegramApiId.trim())
                    .putString("telegram_api_hash", telegramApiHash.trim())
                    .putString("telegram_session", telegramSession.trim())
                    .apply()
                showTelegramSetup = false
                message("Telegram credentials saved.")
            },
        )
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
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Custom Path") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = inputPath.trim()
                    if (clean.isNotBlank()) {
                        try {
                            File(clean).mkdirs()
                        } catch (_: Exception) {}
                        onPathSelected(clean)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ArcWhite, contentColor = ArcBlack),
            ) {
                Text("Save")
            }
        },
    )
}

@Composable
private fun DefaultQualityDialog(
    currentQuality: String,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf("Best", "2160p", "1440p", "1080p", "720p", "480p", "360p", "240p", "144p", "Ask each time")

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default Download Quality") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Choose the preferred quality pre-selected when analyzing new links or searches.",
                    color = ArcMuted,
                    fontSize = 12.sp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    options.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { opt ->
                                FilterChip(
                                    selected = currentQuality == opt,
                                    onClick = { onQualitySelected(opt) },
                                    label = { Text(opt) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun HomeScreen(
    input: String,
    onInput: (String) -> Unit,
    onAnalyze: () -> Unit,
    onPaste: () -> Unit,
    onQuickSearch: () -> Unit,
    onTelegram: () -> Unit,
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
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                QuickChip("Clipboard", Icons.Outlined.ContentPaste, onPaste)
                QuickChip("Search", Icons.Outlined.Search, onQuickSearch)
                QuickChip("Telegram", Icons.AutoMirrored.Outlined.Send, onTelegram)
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
                        MediaGlyph(Icons.Outlined.Movie, Modifier.size(44.dp))
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
                    MediaGlyph(Icons.Outlined.Movie, Modifier.size(76.dp))
                    Column(Modifier.weight(1f)) {
                        Text(target.source.uppercase().ifBlank { "MEDIA" }, color = ArcMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text(target.title.ifBlank { "Resolving link..." }, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        Text(target.detail, color = ArcMuted, fontSize = 11.sp)
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
        if (jobs.isEmpty()) item { EmptyState("No Activity", "Downloads will appear here.") }
        items(jobs) { job -> JobRow(job, onOpen = onOpen, onShare = onShare) }
    }
}

@Composable
private fun LibraryScreen(
    jobs: List<DownloadJob>,
    onOpen: (String) -> Unit,
    onShare: (String) -> Unit,
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
        if (completed.isEmpty()) item { EmptyState("No Files Yet", "Completed downloads will appear here.") }
        items(completed) { job -> JobRow(job, onOpen = onOpen, onShare = onShare) }
    }
}

@Composable
private fun SettingsScreen(
    update: UpdateInfo,
    notifications: Boolean,
    clipboardHelper: Boolean,
    downloadPath: String,
    defaultQuality: String,
    onNotifications: (Boolean) -> Unit,
    onClipboard: (Boolean) -> Unit,
    onOpenPathDialog: () -> Unit,
    onOpenQualityDialog: () -> Unit,
    onCheckUpdates: () -> Unit,
    onOpenRelease: () -> Unit,
    onInfo: (String, String) -> Unit,
    onTelegramSetup: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                    MediaGlyph(Icons.Outlined.Tune, Modifier.size(36.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Default Quality", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("$defaultQuality Selected", color = ArcMuted, fontSize = 11.sp)
                    }
                    Icon(Icons.Outlined.Edit, contentDescription = "Change", tint = ArcMuted, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(6.dp))

            SettingRow("Clipboard Helper", "Suggest pasted links", Icons.Outlined.ContentPaste, control = { Switch(checked = clipboardHelper, onCheckedChange = onClipboard) })

            SettingsGroup("Engine")
            SettingRow(
                "Standalone Engine",
                "High-performance engine runs independently inside this app",
                Icons.Outlined.Settings,
                control = { TextButton(onClick = { onInfo("Engine", "Arc bundles its media downloader directly in this APK. Searching, resolving, and downloading run standalone without external software.") }) { Text("Info") } },
            )

            SettingsGroup("Telegram")
            SettingRow(
                "Telegram Integration",
                "Configure optional API credentials for private links",
                Icons.AutoMirrored.Outlined.Send,
                control = { TextButton(onClick = onTelegramSetup) { Text("Setup") } },
            )

            SettingsGroup("Notifications")
            SettingRow("Download Notifications", "Android system notifications", Icons.Outlined.NotificationsNone, control = { Switch(checked = notifications, onCheckedChange = onNotifications) })

            SettingsGroup("About")
            SettingRow("About Arc", "Universal media downloader for Android", Icons.Outlined.Info, control = { TextButton(onClick = { onInfo("About Arc", "Arc is a standalone universal downloader for videos, movies, audio, and documents.") }) { Text("Info") } })
        }
        item { Text("Download only content you have the right to access.", color = ArcSubtle, fontSize = 11.sp) }
    }
}

@Composable
private fun TelegramSetupDialog(
    apiId: String,
    apiHash: String,
    session: String,
    onApiId: (String) -> Unit,
    onApiHash: (String) -> Unit,
    onSession: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Telegram Configuration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    "Public Telegram links work automatically via web preview. API credentials are optional and only needed for private restricted channels.",
                    color = ArcMuted,
                    fontSize = 12.sp,
                )
                OutlinedTextField(
                    value = apiId,
                    onValueChange = onApiId,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("API ID (optional)") },
                )
                OutlinedTextField(
                    value = apiHash,
                    onValueChange = onApiHash,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("API hash (optional)") },
                )
                OutlinedTextField(
                    value = session,
                    onValueChange = onSession,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3,
                    label = { Text("String session (optional)") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = { Button(onClick = onSave) { Text("Save") } },
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
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                MediaGlyph(
                    if (job.status == "Complete") Icons.Outlined.CheckCircle else Icons.Outlined.FileDownload,
                    Modifier.size(40.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(job.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${job.source} · ${job.detail}", color = ArcMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    job.status,
                    color = if (job.status == "Complete") Color(0xFF4CAF50) else ArcMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (job.path.isNotBlank()) {
                Text(job.path, color = ArcSubtle, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
private fun QuickChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        colors = AssistChipDefaults.assistChipColors(containerColor = ArcSurface),
        border = BorderStroke(1.dp, Color(0xFF303030)),
    )
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
