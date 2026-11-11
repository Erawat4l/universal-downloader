package com.erawat.arc

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.net.URLEncoder
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    val targetUrl: String = "",
    val directStreamUrl: String = "",
    val proxyUrl: String = "",
    val availableQualities: List<String> = emptyList(),
    val qualityUrls: Map<String, String> = emptyMap(),
    val poster: String = "",
    val streamHeaders: Map<String, String> = emptyMap(),
    val isSeries: Boolean = false,
    val seasons: List<SeasonInfo> = emptyList(),
    val isPlaylist: Boolean = false,
    val playlistEntries: List<PlaylistEntry> = emptyList(),
    val selectedSeason: Int = 1,
    val selectedEpisode: Int = 1,
    val canStream: Boolean = true,
    val mediaType: String = "video",
    val audioTracks: List<AudioChoice> = emptyList(),
    val selectedAudioLang: String = "",
    val year: String = "",
    val mediaCategory: String = "",
)

private fun extractYear(title: String, yearProp: String = ""): String {
    val cleanProp = yearProp.trim()
    if (cleanProp.length == 4 && cleanProp.all { it.isDigit() }) {
        return cleanProp
    }
    val match = Regex("""\b(19\d\d|20\d\d)\b""").find(title)
    return match?.groupValues?.get(1).orEmpty()
}

private fun cleanShowTitle(raw: String): String {
    return raw
        .replace(Regex("""[\s\-_]+S\d+(-S\d+)?(\s*E\d+)?.*""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""[\s\-_]+Season\s*\d+(-S\d+)?.*""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""[\s\-_]+Complete\s*Series.*""", RegexOption.IGNORE_CASE), "")
        .trim()
        .ifBlank { raw }
}

private fun sortQualitiesDescending(rawList: List<String>): List<String> {
    if (rawList.isEmpty()) return listOf("Best Available")
    val order = mapOf(
        "4k" to 4000,
        "2160p" to 2160,
        "1440p" to 1440,
        "1080p" to 1080,
        "720p" to 720,
        "480p" to 480,
        "360p" to 360,
        "240p" to 240,
        "auto" to 50,
        "best" to 10
    )
    fun rank(q: String): Int {
        val lower = q.lowercase()
        val numMatch = Regex("""\b(\d{3,4})p?\b""").find(lower)
            ?: Regex("""(\d{3,4})p""").find(lower)
        if (numMatch != null) {
            val num = numMatch.groupValues[1].toIntOrNull()
            if (num != null) return num
        }
        for ((k, v) in order) {
            if (lower.contains(k)) return v
        }
        return 0
    }
    return rawList.distinct().sortedWith(compareByDescending { rank(it) })
}

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

class MainActivity : ComponentActivity() {
    private val incomingUrl = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            ArcTheme {
                ArcApp(
                    sharedUrl = incomingUrl.value,
                    onSharedUrlConsumed = { incomingUrl.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND == action && type != null && type.startsWith("text/")) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) {
                incomingUrl.value = extractUrlOrText(text)
            }
        } else if (Intent.ACTION_VIEW == action) {
            val dataStr = intent.dataString
            if (!dataStr.isNullOrBlank()) {
                incomingUrl.value = dataStr
            }
        }
    }

    private fun extractUrlOrText(raw: String): String {
        val regex = Regex("""https?://[^\s]+""")
        val match = regex.find(raw)
        return match?.value ?: raw.trim()
    }

    override fun onDestroy() {
        StreamService.stop(this)
        super.onDestroy()
    }
}

@Composable
private fun ArcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = ArcWhite,
            onPrimary = ArcBlack,
            primaryContainer = Color(0xFF262626),
            onPrimaryContainer = ArcWhite,
            secondary = ArcWhite,
            onSecondary = ArcBlack,
            secondaryContainer = Color(0xFF262626),
            onSecondaryContainer = ArcWhite,
            tertiary = ArcWhite,
            onTertiary = ArcBlack,
            tertiaryContainer = Color(0xFF222222),
            onTertiaryContainer = ArcWhite,
            background = ArcBlack,
            onBackground = ArcWhite,
            surface = ArcSurface,
            onSurface = ArcWhite,
            surfaceVariant = ArcRaised,
            onSurfaceVariant = ArcWhite,
            outline = Color(0xFF404040),
            outlineVariant = Color(0xFF2B2B2B),
        ),
        content = content,
    )
}

@Composable
private fun arcFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = ArcSurface,
    labelColor = ArcMuted,
    iconColor = ArcMuted,
    selectedContainerColor = ArcWhite,
    selectedLabelColor = ArcBlack,
    selectedLeadingIconColor = ArcBlack,
    disabledContainerColor = ArcBlack,
    disabledLabelColor = ArcSubtle,
)

@Composable
private fun arcFilterChipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = Color(0xFF2E2E2E),
    selectedBorderColor = ArcWhite,
    borderWidth = 1.dp,
    selectedBorderWidth = 1.dp,
)

@Composable
private fun arcTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ArcWhite,
    unfocusedBorderColor = Color(0xFF333333),
    focusedLabelColor = ArcWhite,
    unfocusedLabelColor = ArcMuted,
    cursorColor = ArcWhite,
    focusedLeadingIconColor = ArcWhite,
    unfocusedLeadingIconColor = ArcMuted,
    focusedTrailingIconColor = ArcWhite,
    unfocusedTrailingIconColor = ArcMuted,
)


@Composable
private fun ArcApp(
    sharedUrl: String? = null,
    onSharedUrlConsumed: () -> Unit = {},
) {
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

    var defaultVideoQuality by rememberSaveable {
        mutableStateOf(preferences.getString("default_video_quality", "Ask each time") ?: "Ask each time")
    }
    var defaultAudioQuality by rememberSaveable {
        mutableStateOf(preferences.getString("default_audio_quality", "Ask each time") ?: "Ask each time")
    }
    var selectedQuality by rememberSaveable { mutableStateOf("Best Available") }
    var showQualityDialog by rememberSaveable { mutableStateOf(false) }

    var tabName by rememberSaveable { mutableStateOf(AppTab.HOME.name) }
    var overlay by rememberSaveable { mutableStateOf("") }
    var homeOverlay by rememberSaveable { mutableStateOf("") }
    var input by rememberSaveable { mutableStateOf("") }
    var audioOnly by rememberSaveable { mutableStateOf(false) }

    var searchLoading by rememberSaveable { mutableStateOf(false) }
    var resolveLoading by rememberSaveable { mutableStateOf(false) }
    var streamLoading by rememberSaveable { mutableStateOf(false) }
    var episodeLoading by rememberSaveable { mutableStateOf(false) }
    var episodeResolveJob by remember { mutableStateOf<Job?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var resolvedUrl by rememberSaveable { mutableStateOf("") }
    var target by remember { mutableStateOf(MediaTarget("", "", "", "", "")) }
    var hits by remember { mutableStateOf(emptyList<SearchHit>()) }
    var jobs by remember { mutableStateOf(emptyList<DownloadJob>()) }
    var cancelledJobIds by remember { mutableStateOf(setOf<String>()) }
    val activeCancelledIds = remember { java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>()) }
    var update by remember { mutableStateOf(UpdateInfo()) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var updateProgress by remember { mutableFloatStateOf(0f) }
    var updateStatusText by remember { mutableStateOf("") }
    var notifications by rememberSaveable { mutableStateOf(preferences.getBoolean("notifications_enabled", true)) }
    var telegramApiId by rememberSaveable { mutableStateOf(preferences.getString("telegram_api_id", "") ?: "") }
    var telegramApiHash by rememberSaveable { mutableStateOf(preferences.getString("telegram_api_hash", "") ?: "") }
    var telegramSession by rememberSaveable { mutableStateOf(preferences.getString("telegram_session", "") ?: "") }
    var showTelegramSetup by rememberSaveable { mutableStateOf(false) }

    var currentTrace by remember { mutableStateOf<CliTrace?>(null) }
    var isTraceExpanded by rememberSaveable { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableFloatStateOf(0f) }
    var liveStepText by remember { mutableStateOf("") }

    var showDeleteConfirmJobId by remember { mutableStateOf<String?>(null) }
    var showSeriesDownloadDialog by remember { mutableStateOf(false) }

    val tab = AppTab.entries.firstOrNull { it.name == tabName } ?: AppTab.HOME

    var toastText by remember { mutableStateOf<String?>(null) }
    var isToastVisible by remember { mutableStateOf(false) }
    var toastJob by remember { mutableStateOf<Job?>(null) }

    fun message(text: String) {
        toastJob?.cancel()
        toastJob = scope.launch {
            toastText = text
            isToastVisible = true
            delay(2000)
            isToastVisible = false
            delay(700)
            toastText = null
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            notifications = isGranted
            preferences.edit().putBoolean("notifications_enabled", isGranted).apply()
            if (isGranted) {
                message("Notifications enabled.")
            } else {
                message("Notification permission denied in system settings.")
            }
        }
    )

    fun installApkFile(ctx: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", file)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(installIntent)
        } catch (e: Exception) {
            message("Failed to open package installer: ${e.message}")
        }
    }

    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            val apkFile = File(context.cacheDir, "arc_update.apk")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (context.packageManager.canRequestPackageInstalls()) {
                    if (apkFile.exists() && apkFile.length() > 0) {
                        installApkFile(context, apkFile)
                    }
                } else {
                    message("Permission to install updates was not granted.")
                }
            } else {
                if (apkFile.exists() && apkFile.length() > 0) {
                    installApkFile(context, apkFile)
                }
            }
        }
    )

    fun startUpdateDownloadAndInstall(targetApkUrl: String) {
        val apkUrl = targetApkUrl.ifBlank { "https://github.com/Erawat4l/universal-downloader/releases/download/v0.1.0/ARC.apk" }
        val apkFile = File(context.cacheDir, "arc_update.apk")

        if (apkFile.exists() && apkFile.length() > 5 * 1024 * 1024 && !isDownloadingUpdate) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                message("Please grant permission to install updates")
                val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                installPermissionLauncher.launch(permIntent)
            } else {
                installApkFile(context, apkFile)
            }
            return
        }

        if (isDownloadingUpdate) return
        isDownloadingUpdate = true
        updateProgress = 0f
        updateStatusText = "Connecting..."

        scope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    var currentUrl = apkUrl
                    var redirectCount = 0
                    var connection: HttpURLConnection
                    while (true) {
                        val url = URL(currentUrl)
                        connection = url.openConnection() as HttpURLConnection
                        connection.instanceFollowRedirects = false
                        connection.connectTimeout = 15000
                        connection.readTimeout = 15000
                        connection.setRequestProperty("User-Agent", "Arc-Updater/1.0")
                        val status = connection.responseCode
                        if (status in 300..399) {
                            val newLoc = connection.getHeaderField("Location") ?: break
                            currentUrl = try { URL(url, newLoc).toString() } catch (_: Exception) { newLoc }
                            redirectCount++
                            if (redirectCount > 10) break
                        } else {
                            break
                        }
                    }

                    if (connection.responseCode !in 200..299) {
                        throw java.io.IOException("HTTP ${connection.responseCode}")
                    }

                    val totalLength = connection.contentLengthLong
                    if (apkFile.exists()) apkFile.delete()

                    connection.inputStream.use { input ->
                        apkFile.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var bytesCopied = 0L
                            var lastReportedTime = System.currentTimeMillis()
                            var lastReportedBytes = 0L
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                bytesCopied += read
                                val now = System.currentTimeMillis()
                                if (now - lastReportedTime >= 250 || bytesCopied - lastReportedBytes >= 512 * 1024) {
                                    lastReportedTime = now
                                    lastReportedBytes = bytesCopied
                                    if (totalLength > 0) {
                                        val progress = bytesCopied.toFloat() / totalLength
                                        withContext(Dispatchers.Main) {
                                            updateProgress = progress
                                            val mbCopied = bytesCopied / (1024f * 1024f)
                                            val mbTotal = totalLength / (1024f * 1024f)
                                            updateStatusText = "Downloading: ${(progress * 100).toInt()}% (%.1f / %.1f MB)".format(mbCopied, mbTotal)
                                        }
                                    } else {
                                        val mbCopied = bytesCopied / (1024f * 1024f)
                                        withContext(Dispatchers.Main) {
                                            updateProgress = -1f
                                            updateStatusText = "Downloading: %.1f MB".format(mbCopied)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    true
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        updateStatusText = "Failed: ${e.message}"
                        message("Download failed: ${e.message}")
                    }
                    false
                }
            }

            isDownloadingUpdate = false
            if (success && apkFile.exists() && apkFile.length() > 0) {
                updateStatusText = "Update downloaded. Ready to install."
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                    message("Please grant permission to install updates")
                    val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    installPermissionLauncher.launch(permIntent)
                } else {
                    installApkFile(context, apkFile)
                }
            }
        }
    }

    fun copyToClipboard(text: String, label: String = "Copied") {
        clipboard.setText(AnnotatedString(text))
        message("$label copied to clipboard.")
    }

    fun postNotification(title: String, filePath: String) {
        if (!notifications) return
        try {
            val channelId = "arc_downloads"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "ARC Downloads", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Download completion alerts"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
            val pendingIntent = if (filePath.isNotBlank()) {
                val file = File(filePath)
                val uri = try {
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                } catch (_: Exception) {
                    Uri.fromFile(file)
                }
                val openIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                PendingIntent.getActivity(
                    context,
                    0,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else null

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Download Complete")
                .setContentText(title)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .apply {
                    if (pendingIntent != null) {
                        setContentIntent(pendingIntent)
                    }
                }
                .build()
            notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
        } catch (_: Exception) {}
    }

    LaunchedEffect(Unit) {
        scope.launch {
            update = GitHubUpdateChecker.check()
        }
        if (notifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(searchLoading, resolveLoading) {
        if (searchLoading || resolveLoading) {
            elapsedSeconds = 0f
            val baseText = if (searchLoading) "Searching media catalog" else "Resolving media stream"
            val startTime = System.currentTimeMillis()
            var dotCount = 0
            while (searchLoading || resolveLoading) {
                elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000f
                val dots = ".".repeat(1 + (dotCount % 3))
                liveStepText = "$baseText$dots"
                delay(350)
                dotCount++
            }
        }
    }

    // Live download progress polling loop from embedded engine
    LaunchedEffect(jobs) {
        while (jobs.any { it.status == "Downloading" || it.status == "Paused" }) {
            delay(400)
            val progressObj = withContext(Dispatchers.IO) {
                EmbeddedEngine.getProgress()
            }
            if (progressObj.length() > 0) {
                jobs = jobs.map { job ->
                    val p = progressObj.optJSONObject(job.id)
                    if (p != null) {
                        val newStatus = p.optString("status", job.status)
                        val effectiveStatus = when {
                            job.status == "Complete" || job.status == "Cancelled" -> job.status
                            job.id in cancelledJobIds -> "Cancelled"
                            newStatus == "Cancelled" || job.status == "Cancelled" -> "Cancelled"
                            newStatus == "Paused" || job.status == "Paused" -> "Paused"
                            newStatus == "Complete" && p.optString("path").isNotBlank() -> "Complete"
                            else -> "Downloading"
                        }
                        val path = p.optString("path", job.path).ifEmpty { job.path }
                        job.copy(
                            progress = p.optDouble("percent", job.progress.toDouble()).toFloat(),
                            downloaded = p.optString("downloaded_str", job.downloaded),
                            total = p.optString("total_str", job.total),
                            speed = if (effectiveStatus == "Paused") "--" else p.optString("speed", job.speed),
                            eta = if (effectiveStatus == "Paused") "--" else p.optString("eta", job.eta),
                            status = effectiveStatus,
                            path = path,
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
            context.startActivity(Intent.createChooser(intent, "Open media"))
        } catch (e: Exception) {
            message("Could not open media: ${e.message}")
        }
    }

    fun openStreamExternal(streamUrl: String, headers: Map<String, String> = emptyMap(), title: String = "") {
        try {
            if (streamUrl.isBlank() || streamUrl.contains("youtube.com") || streamUrl.contains("youtu.be")) {
                message("Resolving stream URL...")
                return
            }

            val isDirectPlayable = streamUrl.contains("googlevideo.com") ||
                                  streamUrl.startsWith("file://") ||
                                  streamUrl.startsWith("/") ||
                                  (!streamUrl.contains("hakunaymatata") && !streamUrl.contains("mzfi.me"))

            var playUrl = streamUrl
            if (streamUrl.contains("127.0.0.1")) {
                StreamService.start(context, streamUrl, headers)
            } else if (!isDirectPlayable) {
                StreamService.start(context, streamUrl, headers)
                val pPayload = JSONObject().apply {
                    put("action", "stream_proxy")
                    put("url", streamUrl)
                    val hObj = JSONObject()
                    headers.forEach { (k, v) -> hObj.put(k, v) }
                    put("headers", hObj)
                }
                val pRes = runCatching { EmbeddedEngine.execute(context, pPayload) }.getOrNull()
                val px = pRes?.optJSONObject("result")?.optString("proxy_url")
                if (!px.isNullOrBlank()) {
                    playUrl = px
                }
            }
            val uri = Uri.parse(playUrl)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                putExtra("title", title.ifBlank { "Media Stream" })
                putExtra("from_start", true)
                putExtra("position", 0L)
                headers.forEach { (k, v) -> putExtra(k, v) }
                if (headers.isNotEmpty()) {
                    val headersArray = headers.map { "${it.key}: ${it.value}" }.toTypedArray()
                    putExtra("headers", headersArray)
                    val bundle = Bundle()
                    headers.forEach { (k, v) -> bundle.putString(k, v) }
                    putExtra("android.media.intent.extra.HTTP_HEADERS", bundle)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val vlcIntent = Intent(intent).setPackage("org.videolan.vlc")
            val isVlcAvailable = try {
                context.packageManager.resolveActivity(vlcIntent, PackageManager.MATCH_DEFAULT_ONLY) != null ||
                context.packageManager.getLaunchIntentForPackage("org.videolan.vlc") != null
            } catch (_: Exception) { false }

            if (isVlcAvailable) {
                context.startActivity(vlcIntent)
            } else {
                context.startActivity(Intent.createChooser(intent, "Play stream with"))
            }
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
                val resolvedDirectUrl = data?.optString("direct_url").takeUnless { it.isNullOrBlank() } ?: data?.optString("url").orEmpty()
                val resolvedProxyUrl = data?.optString("proxy_url").orEmpty()
                val resolvedStreamUrl = resolvedProxyUrl.ifBlank { data?.optString("stream_url").takeUnless { it.isNullOrBlank() } ?: resolvedDirectUrl }
                resolvedUrl = resolvedStreamUrl

                val qualitiesList = mutableListOf<String>()
                val qArray = data?.optJSONArray("qualities")
                if (qArray != null) {
                    for (i in 0 until qArray.length()) {
                        val q = qArray.optString(i)
                        if (q.isNotBlank()) qualitiesList.add(q)
                    }
                }

                val qUrlMap = mutableMapOf<String, String>()
                val qUrlsObj = data?.optJSONObject("quality_urls")
                if (qUrlsObj != null) {
                    val qKeys = qUrlsObj.keys()
                    while (qKeys.hasNext()) {
                        val qk = qKeys.next()
                        qUrlMap[qk] = qUrlsObj.optString(qk)
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
                val rawSource = data?.optString("source").takeUnless { it.isNullOrBlank() } ?: (if (isSeries) "TV Series" else (if (isPlaylist) "Playlist" else "Media"))
                val canStream = data?.optBoolean("can_stream", true) ?: true
                val mediaType = data?.optString("media_type", if (canStream) "video" else "file") ?: (if (canStream) "video" else "file")
                val resolvedYear = data?.optString("year").takeUnless { it.isNullOrBlank() } ?: extractYear(resolvedTitle, target.year)
                val isExplicitMovie = resolvedTitle.contains("Movie", ignoreCase = true) ||
                    target.title.contains("Movie", ignoreCase = true) ||
                    rawSource.contains("Movie", ignoreCase = true) ||
                    target.mediaCategory.contains("MOVIE", ignoreCase = true)
                val isSeriesResolved = !isExplicitMovie && (seasonsList.isNotEmpty() || target.seasons.isNotEmpty()) && (isSeries || target.isSeries)
                val resolvedCategory = when {
                    isExplicitMovie -> "MOVIE"
                    isSeriesResolved -> "TV SERIES"
                    rawSource.contains("YouTube", ignoreCase = true) || target.targetUrl.contains("youtu", ignoreCase = true) -> "YOUTUBE"
                    rawSource.contains("Movie", ignoreCase = true) || rawSource.equals("NetMirror", ignoreCase = true) -> "MOVIE"
                    !canStream || mediaType == "file" -> "FILE"
                    target.mediaCategory.isNotBlank() -> target.mediaCategory
                    else -> "MOVIE"
                }

                val sortedQualities = sortQualitiesDescending(qualitiesList)
                val defaultPref = if (audioOnly) defaultAudioQuality else defaultVideoQuality
                val initialQuality = if (defaultPref != "Ask each time" && sortedQualities.contains(defaultPref)) {
                    defaultPref
                } else if (sortedQualities.isNotEmpty()) {
                    sortedQualities.first()
                } else {
                    "Best Available"
                }
                selectedQuality = initialQuality

                val initialMappedUrl = qUrlMap[initialQuality]
                val finalDirectUrl = if (!initialMappedUrl.isNullOrBlank()) initialMappedUrl else resolvedDirectUrl

                target = target.copy(
                    title = resolvedTitle,
                    source = rawSource,
                    detail = if (!canStream || mediaType == "file") "Direct File" else (if (isSeriesResolved) "Season ${data?.optInt("season", target.selectedSeason) ?: target.selectedSeason} · Episode ${data?.optInt("episode", target.selectedEpisode) ?: target.selectedEpisode}" else (if (audioOnly) "Audio Stream" else "")),
                    directStreamUrl = finalDirectUrl,
                    proxyUrl = resolvedProxyUrl,
                    targetUrl = data?.optString("target_url").takeUnless { it.isNullOrBlank() } ?: target.targetUrl,
                    availableQualities = sortedQualities,
                    qualityUrls = if (qUrlMap.isNotEmpty()) qUrlMap else target.qualityUrls,
                    poster = resolvedPoster,
                    streamHeaders = headersMap,
                    isSeries = isSeriesResolved,
                    seasons = seasonsList.ifEmpty { target.seasons },
                    isPlaylist = isPlaylist,
                    playlistEntries = playlistEntriesList,
                    selectedSeason = data?.optInt("season", target.selectedSeason) ?: target.selectedSeason,
                    selectedEpisode = data?.optInt("episode", target.selectedEpisode) ?: target.selectedEpisode,
                    canStream = canStream,
                    mediaType = mediaType,
                    year = resolvedYear,
                    mediaCategory = resolvedCategory,
                )

                resolveLoading = false
                overlay = "analysis"
                homeOverlay = "analysis"

                currentTrace = CliTrace(
                    action = actionLabel,
                    target = resolvedTitle,
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
                val isCancelled = data?.optBoolean("cancelled", false) == true ||
                    data?.optString("status") == "Cancelled" ||
                    response.optBoolean("cancelled", false) ||
                    result.jobId in cancelledJobIds ||
                    activeCancelledIds.contains(result.jobId)
                val path = data?.optString("path").orEmpty()
                val job = jobs.firstOrNull { it.id == result.jobId }

                val isJobCancelled = isCancelled || (job?.status == "Cancelled") || activeCancelledIds.contains(result.jobId)

                if (!isOk || isJobCancelled || path.isBlank()) {
                    if (isJobCancelled) {
                        jobs = jobs.map { if (it.id == result.jobId) it.copy(status = "Cancelled") else it }
                        message("Download cancelled.")
                    } else if (job != null) {
                        val errMsg = response.optString("error", error.ifBlank { "Download failed." })
                        jobs = jobs.map { if (it.id == result.jobId) it.copy(status = "Failed") else it }
                        message("Download failed: $errMsg")
                    }
                    return
                }

                val jobTitle = job?.title?.ifBlank { "Download" } ?: "Download"
                jobs = jobs.map { if (it.id == result.jobId) it.copy(status = "Complete", path = path, progress = 100f) else it }
                message("Download complete: $jobTitle")
                postNotification(jobTitle, path)
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
        homeOverlay = ""
        searchLoading = false
        resolveLoading = false
        selectedQuality = "Best Available"
        audioOnly = false
        target = MediaTarget("", "", "", "", "")
    }

    fun analyze(
        value: String = input,
        titleOverride: String? = null,
        posterOverride: String? = null,
        season: Int? = null,
        episode: Int? = null,
        audioChoices: List<AudioChoice> = emptyList(),
        selectedAudioLang: String = "",
        mediaCategoryOverride: String? = null,
        yearOverride: String? = null,
    ) {
        val clean = value.trim()
        if (clean.isEmpty()) {
            message("Paste a link or enter a search.")
            return
        }
        currentTrace = null
        liveStepText = ""
        elapsedSeconds = 0f

        val isDirectUrl = clean.startsWith("http://", ignoreCase = true) ||
            clean.startsWith("https://", ignoreCase = true) ||
            clean.startsWith("igl:", ignoreCase = true) ||
            clean.startsWith("t.me/", ignoreCase = true) ||
            clean.startsWith("telegram.me/", ignoreCase = true) ||
            clean.startsWith("magnet:", ignoreCase = true)
        if (isDirectUrl) {
            resolveLoading = true
            val isYouTubeClean = clean.contains("youtu", ignoreCase = true)
            val isSameShow = !isYouTubeClean && (clean.equals(target.targetUrl, ignoreCase = true) ||
                             (target.title.isNotBlank() && titleOverride != null && cleanShowTitle(target.title).equals(cleanShowTitle(titleOverride), ignoreCase = true)))

            if (!isSameShow) {
                selectedQuality = "Best Available"
                audioOnly = false
            }

            val titleToUse = when {
                titleOverride != null -> titleOverride
                isYouTubeClean -> "YouTube Video"
                else -> clean.split("/").lastOrNull { it.isNotBlank() } ?: "Media stream"
            }
            val isMovieTitle = titleToUse.contains("Movie", ignoreCase = true) || mediaCategoryOverride?.contains("MOVIE", ignoreCase = true) == true
            val detectedSeries = !isMovieTitle && ((mediaCategoryOverride?.contains("SERIES", ignoreCase = true) == true) ||
                                 (mediaCategoryOverride?.contains("TV", ignoreCase = true) == true) ||
                                 Regex("""\b(S\d+|Season\s*\d+)\b""", RegexOption.IGNORE_CASE).containsMatchIn(titleToUse))
            val detectedCategory = when {
                isMovieTitle -> "MOVIE"
                detectedSeries -> "TV SERIES"
                clean.contains("youtu", ignoreCase = true) -> "YOUTUBE"
                clean.startsWith("igl:", ignoreCase = true) -> "IGL EPISODE"
                clean.startsWith("magnet:", ignoreCase = true) -> "TORRENT"
                else -> "MOVIE"
            }
            val detectedYear = yearOverride ?: extractYear(titleToUse)

            target = if (isSameShow) {
                target.copy(
                    targetUrl = clean,
                    directStreamUrl = "",
                    selectedSeason = season ?: target.selectedSeason,
                    selectedEpisode = episode ?: target.selectedEpisode,
                    audioTracks = if (audioChoices.isNotEmpty()) audioChoices else target.audioTracks,
                    selectedAudioLang = if (selectedAudioLang.isNotBlank()) selectedAudioLang else target.selectedAudioLang,
                )
            } else {
                MediaTarget(
                    title = titleToUse,
                    source = if (detectedSeries) "TV Series" else (if (clean.contains("youtu", ignoreCase = true)) "YouTube" else "Movie"),
                    detail = if (detectedSeries) "Season ${season ?: 1} · Episode ${episode ?: 1}" else "Analyzing media...",
                    size = "Detecting",
                    description = clean,
                    targetUrl = clean,
                    directStreamUrl = "",
                    proxyUrl = "",
                    availableQualities = emptyList(),
                    qualityUrls = emptyMap(),
                    poster = posterOverride.orEmpty(),
                    streamHeaders = emptyMap(),
                    isSeries = detectedSeries,
                    seasons = emptyList(),
                    isPlaylist = false,
                    playlistEntries = emptyList(),
                    selectedSeason = season ?: 1,
                    selectedEpisode = episode ?: 1,
                    canStream = true,
                    mediaType = "video",
                    audioTracks = audioChoices,
                    selectedAudioLang = selectedAudioLang,
                    year = detectedYear,
                    mediaCategory = detectedCategory,
                )
            }
            overlay = "analysis"
            homeOverlay = "analysis"
            submit(
                JSONObject().apply {
                    put("action", "resolve")
                    put("url", clean)
                    put("quality", if (selectedQuality.contains("Ask", ignoreCase = true)) "Best" else selectedQuality)
                    if (season != null) put("season", season)
                    if (episode != null) put("episode", episode)
                },
                "resolve"
            )
        } else {
            query = clean
            searchLoading = true
            hits = emptyList()
            selectedQuality = "Best Available"
            audioOnly = false
            target = MediaTarget("", "", "", "", "")
            overlay = "search"
            homeOverlay = "search"
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

    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            input = sharedUrl
            tabName = AppTab.HOME.name
            overlay = ""
            analyze(sharedUrl)
            onSharedUrlConsumed()
        }
    }

    fun startDownload(
        customTitle: String = target.title,
        seasonOverride: Int? = null,
        episodeOverride: Int? = null,
        directUrlOverride: String? = null,
        downloadScope: String = "episode",
        audioOnlyOverride: Boolean? = null
    ) {
        val sNum = seasonOverride ?: target.selectedSeason
        val epNum = episodeOverride ?: target.selectedEpisode
        val isAudio = audioOnlyOverride ?: audioOnly
        val baseTitle = cleanShowTitle(target.title)
        val finalTitle = when {
            target.isSeries && downloadScope == "series" -> "$baseTitle Complete Series"
            target.isSeries && downloadScope == "season" -> "$baseTitle Season $sNum"
            target.isSeries && downloadScope == "episode" -> "$baseTitle S${sNum}E${epNum}"
            else -> customTitle
        }

        val targetEngineUrl = when {
            directUrlOverride != null -> directUrlOverride
            target.isSeries -> target.targetUrl.ifBlank { input }
            target.targetUrl.isNotBlank() -> target.targetUrl
            else -> input.ifBlank { resolvedUrl }
        }

        val job = DownloadJob(
            id = java.util.UUID.randomUUID().toString(),
            title = finalTitle,
            source = target.source.ifBlank { "Stream" },
            detail = "${if (isAudio) "Audio" else selectedQuality}${if (isAudio) " · Audio" else " · Video"}",
            status = "Downloading",
            speed = "Connecting...",
            downloaded = "Starting...",
            poster = target.poster,
            url = targetEngineUrl,
            headers = target.streamHeaders,
        )
        jobs = listOf(job) + jobs
        message("Download queued: $finalTitle")
        tabName = AppTab.ACTIVITY.name

        val payload = JSONObject().apply {
            put("action", "download")
            put("url", targetEngineUrl)
            put("scope", downloadScope)
            val isBatchScope = downloadScope == "season" || downloadScope == "series"
            if (!isBatchScope) {
                val qMapped = target.qualityUrls[selectedQuality]
                val directToUse = if (!qMapped.isNullOrBlank()) qMapped else target.directStreamUrl.ifBlank { resolvedUrl }
                if (directToUse.isNotBlank() && !directToUse.contains("127.0.0.1") &&
                    !directToUse.contains("youtube.com") && !directToUse.contains("youtu.be") &&
                    !directToUse.contains("googlevideo.com") &&
                    !targetEngineUrl.contains("youtube.com") && !targetEngineUrl.contains("youtu.be") &&
                    directToUse != targetEngineUrl) {
                    if (!target.isSeries || (sNum == target.selectedSeason && epNum == target.selectedEpisode)) {
                        put("stream_url", directToUse)
                    }
                }
            }
            put("title", finalTitle)
            put("quality", if (isAudio) "audio" else if (selectedQuality.contains("Ask", ignoreCase = true)) "Best" else selectedQuality)
            put("audio_only", isAudio)
            put("target_dir", downloadPath)
            put("cache_dir", context.cacheDir.absolutePath)
            put("job_id", job.id)
            if (target.streamHeaders.isNotEmpty()) {
                val hObj = JSONObject()
                target.streamHeaders.forEach { (k, v) -> hObj.put(k, v) }
                put("headers", hObj)
            }
            if (target.isSeries) {
                put("season", sNum)
                put("episode", epNum)
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
    }

    fun pauseJob(jobId: String) {
        EmbeddedEngine.pause(jobId)
        jobs = jobs.map { if (it.id == jobId) it.copy(status = "Paused") else it }
        message("Download paused.")
    }

    fun resumeJob(jobId: String) {
        EmbeddedEngine.resume(jobId)
        jobs = jobs.map { if (it.id == jobId) it.copy(status = "Downloading") else it }
        message("Download resumed.")
    }

    fun cancelJob(jobId: String) {
        activeCancelledIds.add(jobId)
        cancelledJobIds = cancelledJobIds + jobId
        EmbeddedEngine.cancel(jobId)
        jobs = jobs.map { if (it.id == jobId) it.copy(status = "Cancelled") else it }
        message("Download cancelled.")
    }

    fun deleteJob(jobId: String, removeFile: Boolean = false) {
        activeCancelledIds.add(jobId)
        cancelledJobIds = cancelledJobIds + jobId
        EmbeddedEngine.cancel(jobId)
        val job = jobs.firstOrNull { it.id == jobId }
        if (job != null && removeFile && job.path.isNotBlank()) {
            runCatching { File(job.path).delete() }
        }
        jobs = jobs.filterNot { it.id == jobId }
        message("Media file removed.")
    }

    Scaffold(
        containerColor = ArcBlack,
        snackbarHost = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 12.dp)
                    .imePadding(),
                contentAlignment = Alignment.BottomStart
            ) {
                AnimatedVisibility(
                    visible = isToastVisible && !toastText.isNullOrBlank(),
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 650, easing = LinearOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(durationMillis = 600)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(durationMillis = 600)
                    ) + fadeOut(animationSpec = tween(durationMillis = 550)),
                    modifier = Modifier.wrapContentWidth(Alignment.Start)
                ) {
                    val currentMsg = toastText.orEmpty()
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xF2141414),
                        border = BorderStroke(1.dp, Color(0xFF2C2C2C)),
                        shadowElevation = 8.dp,
                        modifier = Modifier.wrapContentWidth(Alignment.Start),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(ArcWhite)
                            )
                            Text(
                                text = currentMsg,
                                color = ArcWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.2.sp,
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = ArcSurface) {
                AppTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = {
                            if (item == AppTab.HOME) {
                                if (tab == AppTab.HOME) {
                                    openHome()
                                } else {
                                    tabName = AppTab.HOME.name
                                    if (overlay.isBlank() && homeOverlay.isNotBlank()) {
                                        overlay = homeOverlay
                                    }
                                }
                            } else {
                                tabName = item.name
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ArcBlack,
                            selectedTextColor = ArcWhite,
                            indicatorColor = ArcWhite,
                            unselectedIconColor = ArcMuted,
                            unselectedTextColor = ArcMuted,
                        ),
                    )
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
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "TabContentAnimation"
            ) { currentTab ->
                when (currentTab) {
                    AppTab.HOME -> {
                        AnimatedContent(
                            targetState = overlay,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(200)) + slideInHorizontally { it / 8 })
                                    .togetherWith(fadeOut(animationSpec = tween(160)) + slideOutHorizontally { -it / 8 })
                            },
                            label = "OverlayContentAnimation"
                        ) { currentOverlay ->
                            when (currentOverlay) {
                                "search" -> SearchScreen(
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
                                        val targetUrl = audio.url.ifEmpty { hit.url }
                                        selectedQuality = "Best Available"
                                        audioOnly = false
                                        analyze(
                                            value = targetUrl,
                                            titleOverride = hit.title,
                                            posterOverride = hit.poster,
                                            audioChoices = hit.audio,
                                            selectedAudioLang = audio.language,
                                            mediaCategoryOverride = if (hit.type.contains("SERIES", ignoreCase = true) || hit.type.contains("TV", ignoreCase = true)) "TV SERIES" else "MOVIE",
                                            yearOverride = hit.year,
                                        )
                                    },
                                )
                                "analysis" -> AnalysisScreen(
                                    target = target,
                                    loading = resolveLoading,
                                    streamLoading = streamLoading,
                                    episodeLoading = episodeLoading,
                                    trace = currentTrace,
                                    elapsedSeconds = elapsedSeconds,
                                    liveStep = liveStepText,
                                    isExpanded = isTraceExpanded,
                                    onToggleExpand = { isTraceExpanded = !isTraceExpanded },
                                    onCopyLogs = { copyToClipboard(currentTrace?.stdoutLogs.orEmpty(), "Process logs") },
                                    quality = selectedQuality,
                                    audioOnly = audioOnly,
                                    onBack = {
                                        if (hits.isNotEmpty()) {
                                            overlay = "search"
                                            homeOverlay = "search"
                                        } else {
                                            openHome()
                                        }
                                    },
                                    onQuality = { q ->
                                        selectedQuality = q
                                        val mappedUrl = target.qualityUrls[q]
                                        target = target.copy(
                                            directStreamUrl = if (!mappedUrl.isNullOrBlank()) mappedUrl else target.directStreamUrl,
                                        )
                                    },
                                    onAudioOnly = { aOnly ->
                                        audioOnly = aOnly
                                        val activeQ = if (aOnly) "Best Audio (320kbps)" else (target.availableQualities.firstOrNull() ?: "Best Available")
                                        selectedQuality = activeQ
                                        val mappedUrl = target.qualityUrls[activeQ]
                                        target = target.copy(
                                            directStreamUrl = if (!mappedUrl.isNullOrBlank()) mappedUrl else target.directStreamUrl,
                                        )
                                    },
                                    onSelectAudio = { audio ->
                                        val newUrl = audio.url.ifEmpty { target.targetUrl }
                                        analyze(
                                            value = newUrl,
                                            titleOverride = target.title,
                                            posterOverride = target.poster,
                                            season = target.selectedSeason,
                                            episode = target.selectedEpisode,
                                            audioChoices = target.audioTracks,
                                            selectedAudioLang = audio.language,
                                            mediaCategoryOverride = target.mediaCategory,
                                            yearOverride = target.year,
                                        )
                                    },
                                    onDownload = {
                                        if (target.isSeries) {
                                            showSeriesDownloadDialog = true
                                        } else {
                                            startDownload()
                                        }
                                    },
                                    onStream = {
                                        val displayTitle = if (target.isSeries) {
                                            "${cleanShowTitle(target.title)} S${target.selectedSeason}E${target.selectedEpisode}"
                                        } else {
                                            target.title
                                        }
                                        val qMapped = target.qualityUrls[selectedQuality]
                                        val directCandidate = if (!qMapped.isNullOrBlank()) qMapped else target.directStreamUrl.ifBlank { target.proxyUrl }
                                        val isValidDirectStream = directCandidate.isNotBlank() &&
                                            !directCandidate.contains("youtube.com") &&
                                            !directCandidate.contains("youtu.be")

                                        if (isValidDirectStream && !resolveLoading) {
                                            streamLoading = true
                                            openStreamExternal(directCandidate, target.streamHeaders, displayTitle)
                                            scope.launch {
                                                kotlinx.coroutines.delay(1200)
                                                streamLoading = false
                                            }
                                        } else {
                                            streamLoading = true
                                            scope.launch {
                                                val sPayload = JSONObject().apply {
                                                    put("action", "resolve")
                                                    put("url", target.targetUrl.ifBlank { input })
                                                    if (target.isSeries) {
                                                        put("season", target.selectedSeason)
                                                        put("episode", target.selectedEpisode)
                                                    }
                                                    put("quality", if (selectedQuality.contains("Ask", ignoreCase = true)) "Best" else selectedQuality)
                                                }
                                                val res = withContext(Dispatchers.IO) {
                                                    runCatching { EmbeddedEngine.execute(context, sPayload) }.getOrNull()
                                                }
                                                val data = res?.optJSONObject("result")
                                                val qUrlMap = mutableMapOf<String, String>()
                                                data?.optJSONObject("quality_urls")?.let { qObj ->
                                                    qObj.keys().forEach { k -> qUrlMap[k] = qObj.optString(k) }
                                                }
                                                val directUrl = qUrlMap[selectedQuality]
                                                    ?: data?.optString("direct_url").takeUnless { it.isNullOrBlank() }
                                                    ?: data?.optString("stream_url").takeUnless { it.isNullOrBlank() }
                                                    ?: data?.optString("url").orEmpty()
                                                val proxyUrl = data?.optString("proxy_url").orEmpty()
                                                val hObj = data?.optJSONObject("stream_headers") ?: data?.optJSONObject("headers")
                                                val hMap = mutableMapOf<String, String>()
                                                hObj?.keys()?.forEach { k -> hMap[k] = hObj.optString(k) }
                                                val playUrl = directUrl.ifBlank { proxyUrl }
                                                if (playUrl.isNotBlank() && !playUrl.contains("youtube.com") && !playUrl.contains("youtu.be")) {
                                                    target = target.copy(
                                                        directStreamUrl = directUrl,
                                                        proxyUrl = proxyUrl,
                                                        streamHeaders = if (hMap.isNotEmpty()) hMap else target.streamHeaders
                                                    )
                                                    openStreamExternal(playUrl, if (hMap.isNotEmpty()) hMap else target.streamHeaders, displayTitle)
                                                } else {
                                                    message("Could not resolve stream for $displayTitle")
                                                }
                                                streamLoading = false
                                            }
                                        }
                                    },
                                    onSelectSeason = { s ->
                                        episodeResolveJob?.cancel()
                                        episodeLoading = true
                                        target = target.copy(
                                            selectedSeason = s,
                                            selectedEpisode = 1,
                                            directStreamUrl = "",
                                            availableQualities = emptyList(),
                                            qualityUrls = emptyMap()
                                        )
                                        episodeResolveJob = scope.launch {
                                            val sPayload = JSONObject().apply {
                                                put("action", "resolve")
                                                put("url", target.targetUrl.ifBlank { input })
                                                put("season", s)
                                                put("episode", 1)
                                                put("quality", selectedQuality)
                                            }
                                            val res = withContext(Dispatchers.IO) {
                                                runCatching { EmbeddedEngine.execute(context, sPayload) }.getOrNull()
                                            }
                                            val data = res?.optJSONObject("result")
                                            val directUrl = data?.optString("direct_url").takeUnless { it.isNullOrBlank() } ?: data?.optString("url").orEmpty()
                                            val proxyUrl = data?.optString("proxy_url").orEmpty()
                                            val qUrlMap = mutableMapOf<String, String>()
                                            data?.optJSONObject("quality_urls")?.let { qObj ->
                                                qObj.keys().forEach { k -> qUrlMap[k] = qObj.optString(k) }
                                            }
                                            val qList = mutableListOf<String>()
                                            data?.optJSONArray("qualities")?.let { qArr ->
                                                for (i in 0 until qArr.length()) {
                                                    val q = qArr.optString(i)
                                                    if (q.isNotBlank()) qList.add(q)
                                                }
                                            }
                                            val sortedQ = if (qList.isNotEmpty()) sortQualitiesDescending(qList) else emptyList()
                                            val activeQ = if (sortedQ.contains(selectedQuality)) selectedQuality else (sortedQ.firstOrNull() ?: "Best Available")
                                            selectedQuality = activeQ
                                            val finalDirect = qUrlMap[activeQ] ?: directUrl
                                            val hObj = data?.optJSONObject("stream_headers") ?: data?.optJSONObject("headers")
                                            val hMap = mutableMapOf<String, String>()
                                            hObj?.keys()?.forEach { k -> hMap[k] = hObj.optString(k) }
                                            target = target.copy(
                                                directStreamUrl = finalDirect,
                                                proxyUrl = proxyUrl,
                                                availableQualities = sortedQ,
                                                qualityUrls = qUrlMap,
                                                streamHeaders = if (hMap.isNotEmpty()) hMap else target.streamHeaders
                                            )
                                            episodeLoading = false
                                        }
                                    },
                                    onSelectEpisode = { ep ->
                                        episodeResolveJob?.cancel()
                                        episodeLoading = true
                                        target = target.copy(
                                            selectedEpisode = ep,
                                            directStreamUrl = "",
                                            availableQualities = emptyList(),
                                            qualityUrls = emptyMap()
                                        )
                                        episodeResolveJob = scope.launch {
                                            val sPayload = JSONObject().apply {
                                                put("action", "resolve")
                                                put("url", target.targetUrl.ifBlank { input })
                                                put("season", target.selectedSeason)
                                                put("episode", ep)
                                                put("quality", selectedQuality)
                                            }
                                            val res = withContext(Dispatchers.IO) {
                                                runCatching { EmbeddedEngine.execute(context, sPayload) }.getOrNull()
                                            }
                                            val data = res?.optJSONObject("result")
                                            val directUrl = data?.optString("direct_url").takeUnless { it.isNullOrBlank() } ?: data?.optString("url").orEmpty()
                                            val proxyUrl = data?.optString("proxy_url").orEmpty()
                                            val qUrlMap = mutableMapOf<String, String>()
                                            data?.optJSONObject("quality_urls")?.let { qObj ->
                                                qObj.keys().forEach { k -> qUrlMap[k] = qObj.optString(k) }
                                            }
                                            val qList = mutableListOf<String>()
                                            data?.optJSONArray("qualities")?.let { qArr ->
                                                for (i in 0 until qArr.length()) {
                                                    val q = qArr.optString(i)
                                                    if (q.isNotBlank()) qList.add(q)
                                                }
                                            }
                                            val sortedQ = if (qList.isNotEmpty()) sortQualitiesDescending(qList) else emptyList()
                                            val activeQ = if (sortedQ.contains(selectedQuality)) selectedQuality else (sortedQ.firstOrNull() ?: "Best Available")
                                            selectedQuality = activeQ
                                            val finalDirect = qUrlMap[activeQ] ?: directUrl
                                            val hObj = data?.optJSONObject("stream_headers") ?: data?.optJSONObject("headers")
                                            val hMap = mutableMapOf<String, String>()
                                            hObj?.keys()?.forEach { k -> hMap[k] = hObj.optString(k) }
                                            target = target.copy(
                                                directStreamUrl = finalDirect,
                                                proxyUrl = proxyUrl,
                                                availableQualities = sortedQ,
                                                qualityUrls = qUrlMap,
                                                streamHeaders = if (hMap.isNotEmpty()) hMap else target.streamHeaders
                                            )
                                            episodeLoading = false
                                        }
                                    },
                                    onSelectPlaylistItem = { entry ->
                                        analyze(entry.url, titleOverride = entry.title, posterOverride = entry.thumbnail)
                                    },
                                )
                                else -> HomeScreen(
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
                                    jobs = jobs.filter { it.status == "Complete" },
                                    onOpen = { openFile(it) },
                                    onShare = { shareFile(it) },
                                )
                            }
                        }
                    }
                    AppTab.ACTIVITY -> ActivityScreen(
                        jobs = jobs.filter { it.status in listOf("Downloading", "Queued", "Paused", "Failed") },
                        onCancelJob = { cancelJob(it) },
                        onPauseJob = { pauseJob(it) },
                        onResumeJob = { resumeJob(it) },
                        onDismissJob = { id -> jobs = jobs.filterNot { it.id == id } },
                    )
                    AppTab.LIBRARY -> LibraryScreen(
                        jobs = jobs.filter { it.status == "Complete" },
                        onOpen = { openFile(it) },
                        onShare = { shareFile(it) },
                        onDeletePrompt = { id -> showDeleteConfirmJobId = id },
                    )
                    AppTab.SETTINGS -> SettingsScreen(
                        downloadPath = downloadPath,
                        onOpenPathDialog = { showPathDialog = true },
                        defaultVideoQuality = defaultVideoQuality,
                        defaultAudioQuality = defaultAudioQuality,
                        onOpenQualityDialog = { showQualityDialog = true },
                        notifications = notifications,
                        onToggleNotifications = { enabled ->
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    notifications = true
                                    preferences.edit().putBoolean("notifications_enabled", true).apply()
                                    message("Notifications enabled.")
                                }
                            } else {
                                notifications = false
                                preferences.edit().putBoolean("notifications_enabled", false).apply()
                                message("Notifications disabled.")
                            }
                        },
                        onTelegramSetup = { showTelegramSetup = true },
                        update = update,
                        isDownloadingUpdate = isDownloadingUpdate,
                        updateProgress = updateProgress,
                        updateStatusText = updateStatusText,
                        onCheckUpdates = {
                            scope.launch {
                                update = GitHubUpdateChecker.check()
                                message(if (update.appUpdateAvailable) "Update ready: ${update.newVersionName.ifBlank { "Arc" }}" else "Arc is up to date.")
                            }
                        },
                        onInstallUpdate = {
                            startUpdateDownloadAndInstall(update.apkUrl)
                        },
                    )
                }
            }
        }

            // Series Download Options Dialog
            if (showSeriesDownloadDialog) {
                AlertDialog(
                    onDismissRequest = { showSeriesDownloadDialog = false },
                    containerColor = ArcSurface,
                    titleContentColor = ArcWhite,
                    textContentColor = ArcWhite,
                    shape = RoundedCornerShape(20.dp),
                    title = { Text("Download TV Series") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Choose download scope:", color = ArcMuted, fontSize = 12.sp)
                            Button(
                                onClick = {
                                    showSeriesDownloadDialog = false
                                    startDownload(
                                        seasonOverride = target.selectedSeason,
                                        episodeOverride = target.selectedEpisode,
                                        downloadScope = "episode"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = ArcRaised, contentColor = ArcWhite),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Episode S${target.selectedSeason}E${target.selectedEpisode}")
                            }
                            Button(
                                onClick = {
                                    showSeriesDownloadDialog = false
                                    startDownload(
                                        customTitle = "${cleanShowTitle(target.title)} Season ${target.selectedSeason}",
                                        seasonOverride = target.selectedSeason,
                                        downloadScope = "season"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = ArcRaised, contentColor = ArcWhite),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Season ${target.selectedSeason} (All Episodes)")
                            }
                            Button(
                                onClick = {
                                    showSeriesDownloadDialog = false
                                    startDownload(
                                        customTitle = "${cleanShowTitle(target.title)} Complete Series",
                                        downloadScope = "series"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = ArcRaised, contentColor = ArcWhite),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Complete Series (All Seasons)")
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showSeriesDownloadDialog = false }) { Text("Cancel") }
                    }
                )
            }

            // Confirm Delete Dialog
            showDeleteConfirmJobId?.let { delId ->
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmJobId = null },
                    title = { Text("Confirm Delete", color = ArcWhite) },
                    text = { Text("Are you sure you want to delete this media file from your device?", color = ArcMuted) },
                    containerColor = ArcSurface,
                    confirmButton = {
                        Button(
                            onClick = {
                                deleteJob(delId, true)
                                showDeleteConfirmJobId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ArcWhite, contentColor = ArcBlack),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Delete", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmJobId = null }) { Text("Cancel", color = ArcMuted) }
                    }
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
                    currentVideoQuality = defaultVideoQuality,
                    currentAudioQuality = defaultAudioQuality,
                    onVideoQualitySelected = {
                        defaultVideoQuality = it
                        preferences.edit().putString("default_video_quality", it).apply()
                        message("Default video quality set to $it")
                    },
                    onAudioQualitySelected = {
                        defaultAudioQuality = it
                        preferences.edit().putString("default_audio_quality", it).apply()
                        message("Default audio quality set to $it")
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
                    Text("ARC", fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                colors = arcTextFieldColors(),
                label = { Text("Link or search") },
                leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                trailingIcon = { TextButton(onClick = onPaste) { Text("Paste") } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onAnalyze() }),
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
            val chooseHit = {
                val chosenAudio = hit.audio.firstOrNull() ?: AudioChoice("Video", hit.url)
                onSelect(hit, chosenAudio)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = ArcSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = chooseHit),
            ) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = chooseHit),
                    ) {
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
                            val srcTag = if (hit.source.isNotBlank() && !hit.source.equals("NetMirror", true)) " · ${hit.source}" else ""
                            Text("${hit.type}${if (hit.year.isNotBlank()) " · ${hit.year}" else ""}$srcTag", color = ArcMuted, fontSize = 11.sp)
                            if (hit.providers.isNotBlank()) Text(hit.providers, color = ArcSubtle, fontSize = 10.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        if (hit.audio.isEmpty()) {
                            FilterChip(
                                selected = false,
                                onClick = { onSelect(hit, AudioChoice("Video", hit.url)) },
                                label = { Text("Use result") },
                                colors = arcFilterChipColors(),
                                border = arcFilterChipBorder(false),
                            )
                        } else {
                            hit.audio.forEach { audio ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onSelect(hit, audio) },
                                    label = { Text(audio.language) },
                                    colors = arcFilterChipColors(),
                                    border = arcFilterChipBorder(false),
                                )
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
    streamLoading: Boolean = false,
    episodeLoading: Boolean = false,
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
    onSelectSeason: ((Int) -> Unit)? = null,
    onSelectEpisode: ((Int) -> Unit)? = null,
    onSelectPlaylistItem: ((PlaylistEntry) -> Unit)? = null,
    onSelectAudio: ((AudioChoice) -> Unit)? = null,
) {
    val isSeriesTarget = (
        target.isSeries ||
        target.mediaCategory.contains("SERIES", ignoreCase = true) ||
        target.mediaCategory.contains("TV", ignoreCase = true) ||
        target.source.contains("SERIES", ignoreCase = true) ||
        target.source.contains("TV", ignoreCase = true) ||
        target.targetUrl.contains("/tv/", ignoreCase = true) ||
        Regex("""\b(S\d+|Season\s*\d+)\b""", RegexOption.IGNORE_CASE).containsMatchIn(target.title)
    )

    val isYouTubeTarget = target.source.contains("YouTube", ignoreCase = true) ||
        target.targetUrl.contains("youtube.com", ignoreCase = true) ||
        target.targetUrl.contains("youtu.be", ignoreCase = true)

    val isMovieTarget = !isSeriesTarget && !isYouTubeTarget && (
        target.title.contains("Movie", ignoreCase = true) ||
        target.mediaCategory.contains("MOVIE", ignoreCase = true) ||
        target.source.contains("Movie", ignoreCase = true) ||
        target.source.equals("NetMirror", ignoreCase = true) ||
        target.targetUrl.contains("/movie/", ignoreCase = true) ||
        target.targetUrl.contains("netmirror", ignoreCase = true) ||
        target.targetUrl.contains("net52", ignoreCase = true) ||
        target.targetUrl.contains("net77", ignoreCase = true) ||
        target.targetUrl.contains("watch21", ignoreCase = true) ||
        target.targetUrl.contains("watch22", ignoreCase = true) ||
        target.canStream
    )

    val yearStr = extractYear(target.title, target.year)
    val yearBadge = if (yearStr.isNotBlank()) " · $yearStr" else ""

    val categoryBadge = when {
        isSeriesTarget -> "TV SERIES$yearBadge"
        isMovieTarget -> "MOVIE$yearBadge"
        isYouTubeTarget -> "YOUTUBE"
        target.source.isNotBlank() && !target.source.equals("Stream", ignoreCase = true) && !target.source.equals("NetMirror", ignoreCase = true) -> "${target.source.uppercase()}$yearBadge"
        target.mediaType == "file" || !target.canStream -> "FILE"
        else -> "MOVIE$yearBadge"
    }

    val headerTitle = cleanShowTitle(target.title).ifBlank { target.title.ifBlank { "Resolving link..." } }

    val displayQualities = remember(target.availableQualities, audioOnly) {
        if (audioOnly) {
            listOf("Best Audio (320kbps)", "High Audio (256kbps)", "Standard Audio (192kbps)", "Compact Audio (128kbps)")
        } else if (target.availableQualities.isNotEmpty()) {
            sortQualitiesDescending(target.availableQualities)
        } else {
            listOf("Best Available")
        }
    }
    var qualityMenuExpanded by remember { mutableStateOf(false) }

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
                        val glyphIcon = if (target.mediaType == "file" || !target.canStream) Icons.Outlined.FileDownload else Icons.Outlined.Movie
                        MediaGlyph(glyphIcon, Modifier.size(76.dp))
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = categoryBadge,
                            color = ArcMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(headerTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // TV Series season and episode selectors
        if (!loading && isSeriesTarget && target.seasons.isNotEmpty()) {
            item {
                SectionHeader("Season", "S${target.selectedSeason}")
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(target.seasons) { sInfo ->
                        val isSeasonSelected = target.selectedSeason == sInfo.season
                        FilterChip(
                            selected = isSeasonSelected,
                            onClick = { onSelectSeason?.invoke(sInfo.season) },
                            label = { Text("Season ${sInfo.season}") },
                            colors = arcFilterChipColors(),
                            border = arcFilterChipBorder(isSeasonSelected),
                        )
                    }
                }
            }
            item {
                val currentSeason = target.seasons.firstOrNull { it.season == target.selectedSeason } ?: target.seasons.first()
                SectionHeader("Episode", "E${target.selectedEpisode} of ${currentSeason.episodes}")
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((1..currentSeason.episodes).toList()) { epNum ->
                        val isEpSelected = target.selectedEpisode == epNum
                        FilterChip(
                            selected = isEpSelected,
                            onClick = { onSelectEpisode?.invoke(epNum) },
                            label = { Text("E$epNum") },
                            colors = arcFilterChipColors(),
                            border = arcFilterChipBorder(isEpSelected),
                        )
                    }
                }
            }
            if (episodeLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = ArcWhite,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Loading episode stream & qualities...", fontSize = 11.sp, color = ArcMuted)
                    }
                }
            }
        }

        // Multiple Audio Tracks selector if movie or series has multiple languages
        if (!loading && target.audioTracks.size > 1) {
            item {
                val currentLang = target.selectedAudioLang.ifBlank { target.audioTracks.first().language }
                SectionHeader("Audio Language", currentLang)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(target.audioTracks) { aChoice ->
                        val isAudioSelected = (aChoice.language == target.selectedAudioLang) || (target.selectedAudioLang.isBlank() && aChoice == target.audioTracks.first())
                        FilterChip(
                            selected = isAudioSelected,
                            onClick = {
                                if (!isAudioSelected) {
                                    onSelectAudio?.invoke(aChoice)
                                }
                            },
                            label = { Text(aChoice.language) },
                            colors = arcFilterChipColors(),
                            border = arcFilterChipBorder(isAudioSelected),
                        )
                    }
                }
            }
        }

        // YouTube playlist entries if available
        if (!loading && target.isPlaylist && target.playlistEntries.isNotEmpty()) {
            item {
                SectionHeader("Playlist Items", "${target.playlistEntries.size} Videos")
                Spacer(Modifier.height(8.dp))

                val playlistListState = rememberLazyListState()
                val stopNestedScrollConnection = remember {
                    object : NestedScrollConnection {
                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            // Consume remaining scroll delta so scrolling the playlist does not drag the parent screen
                            return available
                        }
                    }
                }

                Surface(
                    color = ArcRaised,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF222222)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    LazyColumn(
                        state = playlistListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .nestedScroll(stopNestedScrollConnection)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(target.playlistEntries) { idx, entry ->
                            Surface(
                                color = ArcSurface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectPlaylistItem?.invoke(entry) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ArcMuted,
                                        modifier = Modifier.width(26.dp)
                                    )
                                    if (entry.thumbnail.isNotBlank()) {
                                        AsyncImage(
                                            model = entry.thumbnail,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(44.dp, 32.dp)
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
                                    Icon(
                                        Icons.Outlined.PlayArrow,
                                        contentDescription = "Select Track",
                                        modifier = Modifier.size(16.dp),
                                        tint = ArcMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!loading) {
            if (target.canStream && target.mediaType != "file" && !isMovieTarget && !isSeriesTarget) {
                item {
                    SectionHeader("Stream Type")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = !audioOnly,
                            onClick = {
                                if (audioOnly) {
                                    onAudioOnly(false)
                                    onQuality("Best Available")
                                }
                            },
                            label = { Text("Video") },
                            modifier = Modifier.weight(1f),
                            colors = arcFilterChipColors(),
                            border = arcFilterChipBorder(!audioOnly),
                        )
                        FilterChip(
                            selected = audioOnly,
                            onClick = {
                                if (!audioOnly) {
                                    onAudioOnly(true)
                                    onQuality("Best Audio (320kbps)")
                                }
                            },
                            label = { Text("Audio Only") },
                            modifier = Modifier.weight(1f),
                            colors = arcFilterChipColors(),
                            border = arcFilterChipBorder(audioOnly),
                        )
                    }
                }
            }

            if (displayQualities.isNotEmpty()) {
                item {
                    SectionHeader(if (audioOnly) "Audio Quality" else "Video Quality")
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ArcSurface),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFF2B2B2B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = displayQualities.size > 1) { qualityMenuExpanded = true },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(if (audioOnly) "Bitrate Profile" else "Quality Profile", fontSize = 11.sp, color = ArcMuted)
                                    Text(quality, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ArcWhite)
                                }
                                if (displayQualities.size > 1) {
                                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = ArcWhite)
                                }
                            }
                        }
                        if (displayQualities.size > 1) {
                            DropdownMenu(
                                expanded = qualityMenuExpanded,
                                onDismissRequest = { qualityMenuExpanded = false },
                                modifier = Modifier.background(ArcSurface),
                            ) {
                                displayQualities.forEach { opt ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                opt,
                                                color = if (opt == quality) ArcWhite else ArcMuted,
                                                fontWeight = if (opt == quality) FontWeight.Bold else FontWeight.Normal,
                                            )
                                        },
                                        onClick = {
                                            onQuality(opt)
                                            qualityMenuExpanded = false
                                        },
                                        trailingIcon = {
                                            if (opt == quality) {
                                                Icon(Icons.Outlined.Check, contentDescription = null, tint = ArcWhite, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
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
                    Text(
                        if (target.isSeries) "Download $quality"
                        else if (target.isPlaylist) "Download Playlist ($quality)"
                        else if (!target.canStream || target.mediaType == "file") "Download File"
                        else "Download $quality",
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (target.canStream && target.mediaType != "file") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onStream,
                        enabled = !streamLoading,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        if (streamLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = ArcWhite,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Opening Stream...", fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            val streamLabel = if (quality.isNotBlank() && !quality.equals("Best Available", ignoreCase = true)) "Stream $quality" else "Stream"
                            Text(streamLabel, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityScreen(
    jobs: List<DownloadJob>,
    onCancelJob: (String) -> Unit,
    onPauseJob: (String) -> Unit = {},
    onResumeJob: (String) -> Unit = {},
    onDismissJob: (String) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Activity", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Active and recent downloads", color = ArcMuted, fontSize = 13.sp)
        }
        if (jobs.isEmpty()) {
            item { EmptyState("No Active Downloads", "Active downloads will appear here.") }
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
                                val actSrc = if (job.source.isNotBlank() && !job.source.equals("NetMirror", true) && !job.source.equals("Stream", true)) " · ${job.source}" else ""
                                Text(
                                    text = "${job.detail}$actSrc",
                                    color = ArcMuted,
                                    fontSize = 11.sp,
                                )
                            }
                            val isFetching = job.status == "Downloading" && job.progress <= 0.5f && (job.speed == "--" || job.speed.isBlank() || job.speed == "Connecting..." || job.downloaded == "0 MB" || job.downloaded.startsWith("Starting"))
                            val badgeText = if (isFetching) "Fetching" else job.status
                            val badgeColor = if (job.status == "Failed") Color(0xFFFF5252) else ArcWhite
                            val badgeBorder = if (job.status == "Failed") Color(0xFF882222) else Color(0xFF333333)
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF222222), RoundedCornerShape(8.dp))
                                    .border(1.dp, badgeBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        val isFetching = job.status == "Downloading" && job.progress <= 0.5f && (job.speed == "--" || job.speed.isBlank() || job.speed == "Connecting..." || job.downloaded == "0 MB" || job.downloaded.startsWith("Starting"))
                        val animatedProgress by animateFloatAsState(
                            targetValue = (job.progress / 100f).coerceIn(0f, 1f),
                            animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
                            label = "jobProgress",
                        )
                        if (isFetching) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = ArcWhite,
                                trackColor = Color(0xFF262626),
                            )
                        } else {
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (job.status == "Failed") Color(0xFFFF5252) else ArcWhite,
                                trackColor = Color(0xFF262626),
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val cleanDown = job.downloaded.replace(Regex("\\s+"), " ").trim()
                            val cleanTot = job.total.replace(Regex("\\s+"), " ").trim()
                            val progressText = when {
                                isFetching -> "Connecting & resolving stream..."
                                cleanDown.startsWith("Ep") ->
                                    "${String.format(java.util.Locale.US, "%.1f", job.progress)}% · $cleanDown ($cleanTot)"
                                cleanTot == "--" || cleanTot == "Done" || cleanTot.isBlank() ->
                                    "${String.format(java.util.Locale.US, "%.1f", job.progress)}% · $cleanDown"
                                else ->
                                    "${String.format(java.util.Locale.US, "%.1f", job.progress)}% · $cleanDown / $cleanTot"
                            }
                            Text(
                                text = progressText,
                                fontSize = 11.sp,
                                color = ArcMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val cleanEta = job.eta.removePrefix("ETA").removePrefix("eta").trim()
                            val etaDisplay = if (cleanEta.isNotBlank() && cleanEta != "--") " · ETA $cleanEta" else ""
                            val speedText = if (isFetching) "Resolving..." else "${job.speed}$etaDisplay"
                            Text(
                                text = speedText,
                                fontSize = 11.sp,
                                color = ArcMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (job.status == "Failed") {
                                OutlinedButton(
                                    onClick = { onDismissJob(job.id) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dismiss", fontSize = 11.sp)
                                }
                            } else if (job.status == "Paused") {
                                OutlinedButton(
                                    onClick = { onResumeJob(job.id) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Resume", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { onCancelJob(job.id) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Icon(Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cancel", fontSize = 11.sp)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onPauseJob(job.id) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Icon(Icons.Outlined.Pause, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pause", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { onCancelJob(job.id) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Icon(Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cancel", fontSize = 11.sp)
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
    onDeletePrompt: (String) -> Unit,
) {
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
        if (jobs.isEmpty()) {
            item { EmptyState("No Files Yet", "Completed downloads will appear here.") }
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
                                val actSrc = if (job.source.isNotBlank() && !job.source.equals("NetMirror", true) && !job.source.equals("Stream", true)) " · ${job.source}" else ""
                                Text(
                                    text = "${job.detail}$actSrc",
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
                        val checkPath = (job.path.ifBlank { job.title }).lowercase()
                        val isMedia = checkPath.endsWith(".mp4") || checkPath.endsWith(".mkv") || checkPath.endsWith(".webm") ||
                            checkPath.endsWith(".m3u8") || checkPath.endsWith(".mov") || checkPath.endsWith(".avi") ||
                            checkPath.endsWith(".mp3") || checkPath.endsWith(".m4a") || checkPath.endsWith(".flac") ||
                            checkPath.endsWith(".wav") || checkPath.endsWith(".aac") || checkPath.endsWith(".ts")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = { onOpen(job.path) },
                                colors = ButtonDefaults.buttonColors(containerColor = ArcWhite, contentColor = ArcBlack),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Icon(if (isMedia) Icons.Outlined.PlayArrow else Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isMedia) "Play" else "Open", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                onClick = { onDeletePrompt(job.id) },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = ArcMuted, modifier = Modifier.size(16.dp))
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
    defaultVideoQuality: String,
    defaultAudioQuality: String,
    onOpenQualityDialog: () -> Unit,
    notifications: Boolean,
    onToggleNotifications: (Boolean) -> Unit,
    onTelegramSetup: () -> Unit,
    update: UpdateInfo,
    isDownloadingUpdate: Boolean,
    updateProgress: Float,
    updateStatusText: String,
    onCheckUpdates: () -> Unit,
    onInstallUpdate: () -> Unit,
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
        item {
            GitHubCard(
                update = update,
                isDownloadingUpdate = isDownloadingUpdate,
                updateProgress = updateProgress,
                updateStatusText = updateStatusText,
                onCheck = onCheckUpdates,
                onInstallUpdate = onInstallUpdate,
            )
        }
        item {
            SettingsGroup("Downloads")

            Card(
                colors = CardDefaults.cardColors(containerColor = ArcSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
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
                    .clip(RoundedCornerShape(14.dp))
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
                        Text("Video: $defaultVideoQuality · Audio: $defaultAudioQuality", color = ArcMuted, fontSize = 11.sp)
                    }
                    Icon(Icons.Outlined.Edit, contentDescription = "Change", tint = ArcMuted, modifier = Modifier.size(18.dp))
                }
            }
        }

        item {
            SettingsGroup("Preferences")

            Card(
                colors = CardDefaults.cardColors(containerColor = ArcSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggleNotifications(!notifications) },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                ) {
                    MediaGlyph(Icons.Outlined.NotificationsNone, Modifier.size(36.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Notifications", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            if (notifications) "Enabled · Alerts when downloads finish" else "Disabled · Muted",
                            color = ArcMuted,
                            fontSize = 11.sp,
                        )
                    }
                    Switch(
                        checked = notifications,
                        onCheckedChange = onToggleNotifications,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ArcBlack,
                            checkedTrackColor = ArcWhite,
                            uncheckedThumbColor = ArcMuted,
                            uncheckedTrackColor = Color(0xFF222222),
                            uncheckedBorderColor = Color(0xFF333333),
                        )
                    )
                }
            }
        }

        item {
            SettingsGroup("Integrations")
            Card(
                colors = CardDefaults.cardColors(containerColor = ArcSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArcSurface,
        titleContentColor = ArcWhite,
        textContentColor = ArcWhite,
        shape = RoundedCornerShape(20.dp),
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
                            .clip(RoundedCornerShape(12.dp))
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
                            .clip(RoundedCornerShape(12.dp))
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
                    colors = arcTextFieldColors(),
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
    currentVideoQuality: String,
    currentAudioQuality: String,
    onVideoQualitySelected: (String) -> Unit,
    onAudioQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Video, 1: Audio

    val videoOptions = listOf(
        "Ask each time",
        "Best Available",
        "2160p (4K UHD)",
        "1440p (2K QHD)",
        "1080p (Full HD)",
        "720p (HD)",
        "480p (SD)",
        "360p (SD)",
    )
    val audioOptions = listOf(
        "Ask each time",
        "Best Audio (320kbps)",
        "High Audio (256kbps)",
        "Standard Audio (192kbps)",
        "Compact Audio (128kbps)",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArcSurface,
        titleContentColor = ArcWhite,
        textContentColor = ArcWhite,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text("Default Download Quality", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("Video Quality", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.weight(1f),
                        colors = arcFilterChipColors(),
                        border = arcFilterChipBorder(selectedTab == 0),
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("Audio Quality", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.weight(1f),
                        colors = arcFilterChipColors(),
                        border = arcFilterChipBorder(selectedTab == 1),
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val activeOptions = if (selectedTab == 0) videoOptions else audioOptions
                val activeCurrent = if (selectedTab == 0) currentVideoQuality else currentAudioQuality

                items(activeOptions) { option ->
                    val isSelected = activeCurrent.equals(option, ignoreCase = true)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) ArcRaised else ArcSurface,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isSelected) ArcWhite else Color(0xFF333333)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (selectedTab == 0) {
                                    onVideoQualitySelected(option)
                                } else {
                                    onAudioQualitySelected(option)
                                }
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                option,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) ArcWhite else ArcMuted,
                                fontSize = 13.sp,
                            )
                            if (isSelected) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = ArcWhite, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ArcWhite, contentColor = ArcBlack),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Done")
            }
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var id by remember { mutableStateOf(apiId) }
    var hash by remember { mutableStateOf(apiHash) }
    var sess by remember { mutableStateOf(session) }
    var checkLoading by remember { mutableStateOf(false) }
    var checkResult by remember { mutableStateOf<String?>(null) }
    var checkSuccess by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArcSurface,
        titleContentColor = ArcWhite,
        textContentColor = ArcWhite,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Telegram Session Credentials") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enter Pyrogram session credentials for Telegram media downloads.", color = ArcMuted, fontSize = 11.sp)
                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it },
                    label = { Text("API ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = arcTextFieldColors(),
                )
                OutlinedTextField(
                    value = hash,
                    onValueChange = { hash = it },
                    label = { Text("API HASH") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = arcTextFieldColors(),
                )
                OutlinedTextField(
                    value = sess,
                    onValueChange = { sess = it },
                    label = { Text("STRING_SESSION") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = arcTextFieldColors(),
                )

                if (checkResult != null) {
                    Text(
                        text = checkResult.orEmpty(),
                        color = if (checkSuccess) Color(0xFF4CAF50) else Color(0xFFFF5252),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }



                OutlinedButton(
                    onClick = {
                        checkLoading = true
                        checkResult = null
                        scope.launch {
                            var cleanSess = sess.trim()
                            val pad = cleanSess.length % 4
                            if (pad != 0) {
                                cleanSess += "=".repeat(4 - pad)
                            }
                            val payload = JSONObject().apply {
                                put("action", "telegram_check")
                                put("api_id", id.trim())
                                put("api_hash", hash.trim())
                                put("session", cleanSess)
                            }
                            val res = withContext(Dispatchers.IO) {
                                runCatching { EmbeddedEngine.execute(context, payload) }.getOrNull()
                            }
                            checkLoading = false
                            if (res != null && res.optBoolean("ok", false)) {
                                val rObj = res.optJSONObject("result")
                                val u = rObj?.optString("user", "Account Verified") ?: "Verified"
                                checkSuccess = true
                                checkResult = "Connected: $u"
                            } else {
                                checkSuccess = false
                                checkResult = "Error: ${res?.optString("error") ?: "Connection failed"}"
                            }
                        }
                    },
                    enabled = !checkLoading && id.isNotBlank() && hash.isNotBlank() && sess.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (checkLoading) "Testing connection..." else "Test Connection", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var s = sess.trim()
                    val pad = s.length % 4
                    if (pad != 0) {
                        s += "=".repeat(4 - pad)
                    }
                    onSave(id.trim(), hash.trim(), s)
                },
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
private fun GitHubCard(
    update: UpdateInfo,
    isDownloadingUpdate: Boolean,
    updateProgress: Float,
    updateStatusText: String,
    onCheck: () -> Unit,
    onInstallUpdate: () -> Unit,
) {
    val title = when {
        isDownloadingUpdate -> "Downloading update..."
        update.loading -> "Checking updates..."
        update.error.isNotBlank() -> "Update check unavailable"
        update.appUpdateAvailable -> "Update ready: ${update.newVersionName.ifBlank { "Arc" }}"
        else -> "Up to date"
    }
    val body = when {
        isDownloadingUpdate -> updateStatusText.ifBlank { "Downloading update APK into cache..." }
        update.loading -> "Checking GitHub for updates."
        update.error.isNotBlank() -> "Could not reach repository."
        update.appUpdateAvailable -> {
            if (update.commitMessage.isNotBlank()) update.commitMessage
            else if (update.releaseTag.isNotBlank()) "Release ${update.releaseTag} includes an updated APK."
            else "A new update is ready on GitHub."
        }
        else -> "Arc is up to date."
    }
    Card(colors = CardDefaults.cardColors(containerColor = ArcSurface), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                MediaGlyph(if (isDownloadingUpdate || update.appUpdateAvailable) Icons.Outlined.FileDownload else Icons.Outlined.Refresh, Modifier.size(40.dp))
                Column(Modifier.weight(1f)) {
                    Text("Updates", color = ArcMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(body, color = ArcMuted, fontSize = 11.sp)
                }
            }
            if (isDownloadingUpdate) {
                if (updateProgress >= 0f) {
                    LinearProgressIndicator(
                        progress = { updateProgress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = ArcWhite,
                        trackColor = ArcRaised,
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = ArcWhite,
                        trackColor = ArcRaised,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Universal Downloader", color = ArcSubtle, fontSize = 10.sp)
                TextButton(
                    onClick = if (update.appUpdateAvailable) onInstallUpdate else onCheck,
                    enabled = !update.loading && !isDownloadingUpdate
                ) {
                    Text(
                        when {
                            isDownloadingUpdate -> "Downloading..."
                            update.appUpdateAvailable -> "Install Update"
                            else -> "Check Now"
                        }
                    )
                }
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
            .clip(RoundedCornerShape(17.dp))
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
                    MediaGlyph(if (job.status == "Complete") Icons.Outlined.PlayArrow else Icons.Outlined.FileDownload, Modifier.size(34.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(job.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val rowSrc = if (job.source.isNotBlank() && !job.source.equals("NetMirror", true) && !job.source.equals("Stream", true)) " · ${job.source}" else ""
                    Text("${job.detail}$rowSrc", color = ArcMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    job.status,
                    color = when (job.status) {
                        "Complete" -> ArcWhite
                        "Downloading" -> ArcWhite
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
                        Text("Play", fontSize = 12.sp)
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
