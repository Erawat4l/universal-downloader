package com.erawat.arc

import android.accounts.AccountManager
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
import android.os.PowerManager
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.provider.OpenableColumns
import androidx.compose.foundation.layout.statusBarsPadding
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
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AccountCircle
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
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.input.KeyboardType
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
import android.util.Log
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import android.app.Activity
import androidx.compose.material.icons.outlined.FitScreen
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

private data class StreamTrackSpec(
    val url: String = "",
    val videoUrl: String = "",
    val audioUrl: String = "",
    val fallbackUrl: String = "",
    val videoFormat: String = "",
    val audioFormat: String = "",
    val fallbackFormat: String = "",
)

private data class PlayerStreamTarget(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val title: String = "",
    val videoUrl: String = "",
    val audioUrl: String = "",
    val fallbackUrl: String = "",
    val videoFormat: String = "",
    val audioFormat: String = "",
    val fallbackFormat: String = "",
    val isAudioOnlyStream: Boolean = false,
    val downloadFilePath: String = "",
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
    val qualityTracks: Map<String, StreamTrackSpec> = emptyMap(),
    val videoUrl: String = "",
    val audioUrl: String = "",
    val fallbackUrl: String = "",
    val videoFormat: String = "",
    val audioFormat: String = "",
    val fallbackFormat: String = "",
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
    val providers: String = "",
)

private fun parseStreamTrackSpec(obj: JSONObject?): StreamTrackSpec? {
    if (obj == null) return null
    return StreamTrackSpec(
        url = obj.optString("url"),
        videoUrl = obj.optString("video_url"),
        audioUrl = obj.optString("audio_url"),
        fallbackUrl = obj.optString("fallback_url"),
        videoFormat = obj.optString("video_format"),
        audioFormat = obj.optString("audio_format"),
        fallbackFormat = obj.optString("fallback_format"),
    )
}

private fun parseQualityTracks(data: JSONObject?): Map<String, StreamTrackSpec> {
    val out = mutableMapOf<String, StreamTrackSpec>()
    data?.optJSONObject("quality_tracks")?.let { qtObj ->
        qtObj.keys().forEach { k ->
            parseStreamTrackSpec(qtObj.optJSONObject(k))?.let { spec ->
                out[k] = spec
            }
        }
    }
    return out
}

private fun resolveTrackForQuality(
    quality: String,
    qualityTracks: Map<String, StreamTrackSpec>,
    qualityUrls: Map<String, String>,
    fallbackDirectUrl: String,
    fallbackSpec: StreamTrackSpec = StreamTrackSpec(),
): StreamTrackSpec {
    qualityTracks[quality]?.let { return it }
    val qLow = quality.lowercase()
    for ((k, v) in qualityTracks) {
        val kLow = k.lowercase()
        if (qLow in kLow ||
            (qLow.startsWith("2160") && "2160" in kLow) ||
            (qLow.startsWith("1440") && "1440" in kLow) ||
            (qLow.startsWith("1080") && "1080" in kLow) ||
            (qLow.startsWith("720") && "720" in kLow) ||
            (qLow.startsWith("480") && "480" in kLow) ||
            (qLow.startsWith("360") && "360" in kLow)
        ) {
            return v
        }
    }
    val mapped = qualityUrls[quality]
    if (!mapped.isNullOrBlank()) {
        val matchesFallbackSpec = mapped == fallbackSpec.url && fallbackSpec.videoUrl.isNotBlank() && fallbackSpec.audioUrl.isNotBlank()
        return StreamTrackSpec(
            url = mapped,
            videoUrl = if (matchesFallbackSpec) fallbackSpec.videoUrl else "",
            audioUrl = if (matchesFallbackSpec) fallbackSpec.audioUrl else "",
            fallbackUrl = fallbackSpec.fallbackUrl,
            videoFormat = if (matchesFallbackSpec) fallbackSpec.videoFormat else "",
            audioFormat = if (matchesFallbackSpec) fallbackSpec.audioFormat else "",
            fallbackFormat = fallbackSpec.fallbackFormat,
        )
    }
    return if (fallbackSpec.url.isNotBlank() || (fallbackSpec.videoUrl.isNotBlank() && fallbackSpec.audioUrl.isNotBlank())) {
        fallbackSpec.copy(url = fallbackSpec.url.ifBlank { fallbackDirectUrl })
    } else {
        StreamTrackSpec(url = fallbackDirectUrl)
    }
}

private fun loadSeriesEpisode(
    seasonNum: Int,
    episodeNum: Int,
    target: MediaTarget,
    inputFallback: String,
    selectedQuality: String,
    episodeCache: MutableMap<String, MediaTarget>,
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    currentJob: Job?,
    onJobChange: (Job?) -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    onQualitySelected: (String) -> Unit,
    onTargetUpdated: (MediaTarget) -> Unit,
) {
    currentJob?.cancel()
    val cacheKey = "${target.targetUrl}_S${seasonNum}_E${episodeNum}"
    val cached = episodeCache[cacheKey]
    if (cached != null) {
        onTargetUpdated(cached.copy(selectedSeason = seasonNum, selectedEpisode = episodeNum))
        onLoadingChange(false)
        return
    }
    onLoadingChange(true)
    onTargetUpdated(target.copy(selectedSeason = seasonNum, selectedEpisode = episodeNum))
    val job = scope.launch {
        val sPayload = JSONObject().apply {
            put("action", "resolve")
            put("url", target.targetUrl.ifBlank { inputFallback })
            if (target.selectedAudioLang.isNotBlank()) {
                put("audio_lang", target.selectedAudioLang)
            }
            put("season", seasonNum)
            put("episode", episodeNum)
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
        val qTrackMap = parseQualityTracks(data)
        val topTrack = parseStreamTrackSpec(data) ?: StreamTrackSpec(url = directUrl)
        val qList = mutableListOf<String>()
        data?.optJSONArray("qualities")?.let { qArr ->
            for (i in 0 until qArr.length()) {
                val q = qArr.optString(i)
                if (q.isNotBlank()) qList.add(q)
            }
        }
        val sortedQ = if (qList.isNotEmpty()) sortQualitiesDescending(qList) else target.availableQualities
        val activeQ = if (sortedQ.contains(selectedQuality)) selectedQuality else (sortedQ.firstOrNull() ?: "Best Available")
        onQualitySelected(activeQ)
        val resolvedSpec = resolveTrackForQuality(
            quality = activeQ,
            qualityTracks = qTrackMap,
            qualityUrls = qUrlMap,
            fallbackDirectUrl = directUrl,
            fallbackSpec = topTrack,
        )
        val hObj = data?.optJSONObject("stream_headers") ?: data?.optJSONObject("headers")
        val hMap = mutableMapOf<String, String>()
        hObj?.keys()?.forEach { k -> hMap[k] = hObj.optString(k) }
        val updatedTarget = target.copy(
            selectedSeason = seasonNum,
            selectedEpisode = episodeNum,
            directStreamUrl = resolvedSpec.url.ifBlank { directUrl },
            proxyUrl = proxyUrl,
            availableQualities = if (sortedQ.isNotEmpty()) sortedQ else target.availableQualities,
            qualityUrls = if (qUrlMap.isNotEmpty()) qUrlMap else target.qualityUrls,
            qualityTracks = qTrackMap,
            videoUrl = resolvedSpec.videoUrl,
            audioUrl = resolvedSpec.audioUrl,
            fallbackUrl = resolvedSpec.fallbackUrl,
            videoFormat = resolvedSpec.videoFormat,
            audioFormat = resolvedSpec.audioFormat,
            fallbackFormat = resolvedSpec.fallbackFormat,
            streamHeaders = if (hMap.isNotEmpty()) hMap else target.streamHeaders,
        )
        onTargetUpdated(updatedTarget)
        episodeCache[cacheKey] = updatedTarget
        onLoadingChange(false)
    }
    onJobChange(job)
}

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

private data class CachedTrace(
    val trace: CliTrace?,
    val liveStep: String,
    val elapsedSeconds: Float,
)

private object AppSessionCache {
    val searchHits = mutableMapOf<String, List<SearchHit>>()
    val searchTraces = mutableMapOf<String, CachedTrace>()
    val targets = mutableMapOf<String, MediaTarget>()
    val targetTraces = mutableMapOf<String, CachedTrace>()
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun requestIgnoreBatteryOptimizations(context: Context) {
    try {
        val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        } catch (_: Exception) {}
    }
}

class MainActivity : ComponentActivity() {
    private val incomingUrl = mutableStateOf<String?>(null)
    private val incomingLocalPlay = mutableStateOf<Pair<String, String>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            ArcTheme {
                ArcApp(
                    sharedUrl = incomingUrl.value,
                    onSharedUrlConsumed = { incomingUrl.value = null },
                    localPlayRequest = incomingLocalPlay.value,
                    onLocalPlayConsumed = { incomingLocalPlay.value = null },
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
        val localFile = intent.getStringExtra("play_local_file")
        if (!localFile.isNullOrBlank()) {
            val localTitle = intent.getStringExtra("play_local_title").orEmpty()
            incomingLocalPlay.value = localFile to localTitle
            return
        }
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND == action) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) {
                incomingUrl.value = extractUrlOrText(text)
            } else {
                val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (streamUri != null) {
                    incomingUrl.value = resolveAndCopyUri(streamUri)
                }
            }
        } else if (Intent.ACTION_VIEW == action) {
            val uri = intent.data
            if (uri != null) {
                if (uri.scheme == "magnet") {
                    incomingUrl.value = uri.toString()
                } else if (uri.scheme in listOf("content", "file")) {
                    incomingUrl.value = resolveAndCopyUri(uri)
                } else {
                    val dataStr = intent.dataString
                    if (!dataStr.isNullOrBlank()) {
                        incomingUrl.value = dataStr
                    }
                }
            }
        }
    }

    private fun resolveAndCopyUri(uri: Uri): String {
        return try {
            var fileName = "download.torrent"
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIdx != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIdx) ?: "download.torrent"
                }
            }
            val torrentsDir = File(cacheDir, "torrents").apply { mkdirs() }
            val destFile = File(torrentsDir, fileName)
            contentResolver.openInputStream(uri)?.use { inStream ->
                destFile.outputStream().use { outStream ->
                    inStream.copyTo(outStream)
                }
            }
            destFile.absolutePath
        } catch (_: Exception) {
            uri.toString()
        }
    }

    private fun extractUrlOrText(raw: String): String {
        val regex = Regex("""https?://[^\s]+""")
        val match = regex.find(raw)
        return match?.value ?: raw.trim()
    }

    companion object {
        var onResumeCallback: (() -> Unit)? = null
    }

    override fun onResume() {
        super.onResume()
        StreamService.stop(this)
        onResumeCallback?.invoke()
    }

    override fun onDestroy() {
        StreamService.stop(this)
        onResumeCallback = null
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

private fun formatUserFriendlyError(rawError: String?): String {
    if (rawError.isNullOrBlank()) return "An unexpected issue occurred. Please try again."
    var err = rawError.trim()

    // If multiline Python traceback, extract the final meaningful error line
    if (err.contains("Traceback (most recent call last):", ignoreCase = true)) {
        val lines = err.lines().map { it.trim() }.filter { it.isNotBlank() }
        val lastLine = lines.lastOrNull { !it.startsWith("File ") && !it.startsWith("Traceback") } ?: lines.lastOrNull()
        if (!lastLine.isNullOrBlank()) {
            err = lastLine
        }
    }

    val lower = err.lowercase()

    return when {
        // Cancellation & Process Interruption
        lower.contains("cancel") || lower.contains("interrupted") || lower.contains("aborted") ->
            "Download cancelled"

        // Device Storage & Disk Full
        lower.contains("no space left") || lower.contains("enospc") || lower.contains("disk full") || lower.contains("storage full") ->
            "Not enough storage space on your device"
        lower.contains("permission denied") || lower.contains("eacces") || lower.contains("read-only file system") ->
            "Storage access permission denied"

        // Network, DNS & Socket failures
        lower.contains("timed out") || lower.contains("timeout") || lower.contains("timedout") ->
            "Connection timed out. Please check your network"
        lower.contains("no such host") || lower.contains("unable to resolve host") || lower.contains("network is unreachable") ||
        lower.contains("gaierror") || lower.contains("nodename nor servname") ->
            "No internet connection. Please check your network"
        lower.contains("econnrefused") || lower.contains("connection refused") ->
            "Server connection refused. Please try again later"
        lower.contains("econnreset") || lower.contains("connection reset") || lower.contains("broken pipe") || lower.contains("epipe") ->
            "Connection interrupted. Please retry download"
        lower.contains("certificate_verify_failed") || lower.contains("sslerror") || lower.contains("tlsv1") ->
            "Secure connection failed (SSL certificate issue)"

        // HTTP Status Codes
        lower.contains("403") || lower.contains("forbidden") || lower.contains("access denied") ->
            "Access forbidden by media server (HTTP 403)"
        lower.contains("404") || lower.contains("not found") ->
            "Media file or link was not found (HTTP 404)"
        lower.contains("410") || lower.contains("gone") ->
            "Media link has expired or been removed"
        lower.contains("rate limit") || lower.contains("rate-limiting") || lower.contains("too many requests") || lower.contains("429") ->
            "Server rate-limited. Please try again in a few moments"
        lower.contains("502") || lower.contains("503") || lower.contains("504") || lower.contains("bad gateway") || lower.contains("gateway timeout") || lower.contains("service unavailable") ->
            "Remote server is temporarily unavailable. Please retry shortly"

        // YouTube & Video Extraction
        lower.contains("sign in to confirm you’re not a bot") || lower.contains("confirm you're not a bot") || lower.contains("confirm you’re not a bot") || lower.contains("confirm you") || lower.contains("bot verification") ->
            "YouTube verification required. Connect your Google account in Settings."
        lower.contains("private video") || lower.contains("this video is private") ->
            "This video is private and cannot be accessed"
        lower.contains("members-only") || lower.contains("join this channel") ->
            "This video is restricted to channel members"
        lower.contains("age-restricted") || lower.contains("confirm your age") ->
            "Video is age-restricted and requires account authentication"
        lower.contains("not available in your country") || lower.contains("geo-restricted") || lower.contains("blocked in your country") ->
            "Media is restricted or unavailable in your region"
        lower.contains("premiere will begin in") || lower.contains("live event will begin in") ->
            "This live premiere has not started yet"
        lower.contains("copyright") || lower.contains("dmca") || lower.contains("infringement") ->
            "Media was removed due to a copyright claim"

        // Catalog & Streaming
        lower.contains("no active streaming servers") || lower.contains("offline") ->
            "No active streaming servers found for this media"
        lower.contains("unsupported url") || lower.contains("extractor error") ->
            "Unsupported or unrecognized media link"

        // Clean Fallback
        else -> {
            val cleaned = err
                .replace(Regex("""^\[\d+\s+[^\]]+\]\s*"""), "")
                .replace(Regex("""^[A-Za-z_]+Error:\s*"""), "")
                .replace(Regex("""^Exception:\s*"""), "")
                .replace(Regex("""/data/data/[^\s]+"""), "system")
                .trim()
            if (cleaned.length > 90) cleaned.take(87) + "..." else cleaned
        }
    }
}

private fun prioritizeAudioTracks(tracks: List<AudioChoice>): List<AudioChoice> {
    if (tracks.isEmpty()) return emptyList()
    val cleanTracks = tracks.filter { it.language.isNotBlank() }
    if (cleanTracks.isEmpty()) return emptyList()
    return cleanTracks.sortedWith(Comparator { a, b ->
        val langA = a.language.lowercase()
        val langB = b.language.lowercase()
        val pA = when {
            langA.contains("hindi") -> 1
            langA.contains("english") -> 2
            langA.contains("original") -> 3
            langA.contains("tamil") -> 4
            langA.contains("telugu") -> 5
            langA.contains("bengali") -> 6
            langA.contains("spanish") -> 7
            langA.contains("french") -> 8
            langA.contains("arabic") -> 9
            langA.contains("portuguese") -> 10
            langA.contains("russian") -> 11
            langA.contains("german") -> 12
            langA.contains("japanese") -> 13
            langA.contains("korean") -> 14
            langA.contains("chinese") -> 15
            else -> 20
        }
        val pB = when {
            langB.contains("hindi") -> 1
            langB.contains("english") -> 2
            langB.contains("original") -> 3
            langB.contains("tamil") -> 4
            langB.contains("telugu") -> 5
            langB.contains("bengali") -> 6
            langB.contains("spanish") -> 7
            langB.contains("french") -> 8
            langB.contains("arabic") -> 9
            langB.contains("portuguese") -> 10
            langB.contains("russian") -> 11
            langB.contains("german") -> 12
            langB.contains("japanese") -> 13
            langB.contains("korean") -> 14
            langB.contains("chinese") -> 15
            else -> 20
        }
        if (pA != pB) pA.compareTo(pB) else langA.compareTo(langB)
    })
}


@Composable
private fun ArcApp(
    sharedUrl: String? = null,
    onSharedUrlConsumed: () -> Unit = {},
    localPlayRequest: Pair<String, String>? = null,
    onLocalPlayConsumed: () -> Unit = {},
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

    var streamingPlayer by rememberSaveable {
        mutableStateOf(preferences.getString("streaming_player_package", "arc_player") ?: "arc_player")
    }
    var streamingPlayerName by rememberSaveable {
        mutableStateOf(preferences.getString("streaming_player_name", "ARC Player") ?: "ARC Player")
    }
    var showPlayerDialog by rememberSaveable { mutableStateOf(false) }
    var inAppPlayerTarget by remember { mutableStateOf<PlayerStreamTarget?>(null) }

    var tabName by rememberSaveable { mutableStateOf(AppTab.HOME.name) }
    var overlay by rememberSaveable { mutableStateOf("") }
    var homeOverlay by rememberSaveable { mutableStateOf("") }
    var navigationSource by rememberSaveable { mutableStateOf("") }
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
    LaunchedEffect(Unit) {
        DownloadManager.init(context)
    }
    val jobs by DownloadManager.jobsState
    var cancelledJobIds by remember { mutableStateOf(setOf<String>()) }
    val activeCancelledIds = remember { java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>()) }
    var notifications by rememberSaveable { mutableStateOf(preferences.getBoolean("notifications_enabled", true)) }
    var googleAccountEmail by rememberSaveable { mutableStateOf(preferences.getString("google_account_email", "") ?: "") }
    var googleCookies by rememberSaveable { mutableStateOf(preferences.getString("google_cookies", "") ?: "") }
    var showGoogleInfoDialog by rememberSaveable { mutableStateOf(false) }
    var showGoogleLoginDialog by rememberSaveable { mutableStateOf(false) }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var isBatteryExempt by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    val hasSeenOnboarding = preferences.getBoolean("has_seen_onboarding_permissions", false)
    var showOnboardingNotificationDialog by rememberSaveable { mutableStateOf(!hasSeenOnboarding) }
    var showOnboardingBatteryDialog by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        MainActivity.onResumeCallback = {
            isBatteryExempt = isIgnoringBatteryOptimizations(context)
        }
        onDispose {
            MainActivity.onResumeCallback = null
        }
    }
    val episodeCache = remember { mutableMapOf<String, MediaTarget>() }

    var searchTrace by remember { mutableStateOf<CliTrace?>(null) }
    var analysisTrace by remember { mutableStateOf<CliTrace?>(null) }
    var isSearchTraceExpanded by rememberSaveable { mutableStateOf(false) }
    var isAnalysisTraceExpanded by rememberSaveable { mutableStateOf(false) }
    var searchElapsedSeconds by remember { mutableFloatStateOf(0f) }
    var searchLiveStepText by remember { mutableStateOf("") }
    var analysisElapsedSeconds by remember { mutableFloatStateOf(0f) }
    var analysisLiveStepText by remember { mutableStateOf("") }
    var currentTaskJob by remember { mutableStateOf<Job?>(null) }
    var currentTaskJobId by remember { mutableStateOf<String?>(null) }

    var showDeleteConfirmJobId by remember { mutableStateOf<String?>(null) }
    var showSeriesDownloadDialog by remember { mutableStateOf(false) }
    var showAudioSelectDialog by remember { mutableStateOf(false) }
    var pendingActionAfterAudioSelect by remember { mutableStateOf<String?>(null) }
    val preloadedTargets = remember { mutableMapOf<String, MediaTarget>() }

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

    var googleLoginHint by rememberSaveable { mutableStateOf("") }

    val googleAccountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            val selectedEmail = if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
                result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)?.trim()
            } else null
            googleLoginHint = selectedEmail.orEmpty()
            showGoogleLoginDialog = true
        }
    )

    val launchGoogleLogin = {
        try {
            val pickerIntent = AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                null,
                null,
                null,
                null
            )
            googleAccountPickerLauncher.launch(pickerIntent)
        } catch (_: Exception) {
            googleLoginHint = ""
            showGoogleLoginDialog = true
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
        if (hasSeenOnboarding && notifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(searchLoading) {
        if (searchLoading) {
            searchElapsedSeconds = 0f
            val startTime = System.currentTimeMillis()
            var dotCount = 0
            while (searchLoading) {
                searchElapsedSeconds = (System.currentTimeMillis() - startTime) / 1000f
                val dots = ".".repeat(1 + (dotCount % 3))
                searchLiveStepText = "Searching media catalog$dots"
                delay(350)
                dotCount++
            }
        }
    }

    LaunchedEffect(resolveLoading) {
        if (resolveLoading) {
            analysisElapsedSeconds = 0f
            val startTime = System.currentTimeMillis()
            var dotCount = 0
            while (resolveLoading) {
                analysisElapsedSeconds = (System.currentTimeMillis() - startTime) / 1000f
                val dots = ".".repeat(1 + (dotCount % 3))
                analysisLiveStepText = "Resolving media stream$dots"
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
                DownloadManager.updateProgress(progressObj)
            }
        }
    }

    fun openFile(filePath: String, titleOverride: String = "") {
        if (filePath.isBlank()) return
        val file = File(filePath)
        if (!file.exists()) {
            message("File not found on device: $filePath")
            return
        }
        val ext = file.extension.lowercase()
        val isAudioFile = ext in listOf("mp3", "m4a", "aac", "flac", "ogg", "wav", "opus")
        val isMediaFile = isAudioFile || ext in listOf("mp4", "mkv", "webm", "avi", "mov", "ts", "m3u8")
        val displayTitle = titleOverride.ifBlank { file.nameWithoutExtension.ifBlank { file.name } }

        if (isMediaFile) {
            val chosenPlayer = streamingPlayer.ifBlank {
                preferences.getString("streaming_player_package", "arc_player") ?: "arc_player"
            }
            if (chosenPlayer == "arc_player" || chosenPlayer == "system_chooser") {
                inAppPlayerTarget = PlayerStreamTarget(
                    url = Uri.fromFile(file).toString(),
                    title = displayTitle,
                    isAudioOnlyStream = isAudioFile,
                    downloadFilePath = file.absolutePath,
                )
                return
            }
            try {
                val uri: Uri = try {
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                } catch (_: Exception) {
                    Uri.fromFile(file)
                }
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: when {
                    isAudioFile -> "audio/*"
                    else -> "video/*"
                }
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mime)
                    setPackage(chosenPlayer)
                    putExtra("title", displayTitle)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                inAppPlayerTarget = PlayerStreamTarget(
                    url = Uri.fromFile(file).toString(),
                    title = displayTitle,
                    isAudioOnlyStream = isAudioFile,
                    downloadFilePath = file.absolutePath,
                )
                return
            }
        }

        try {
            val uri: Uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            } catch (_: Exception) {
                Uri.fromFile(file)
            }
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: when {
                ext == "pdf" -> "application/pdf"
                else -> "*/*"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            message("Could not open media: ${e.message}")
        }
    }

    fun openStreamExternal(
        streamUrl: String,
        headers: Map<String, String> = emptyMap(),
        title: String = "",
        targetPackage: String = ""
    ) {
        try {
            if (streamUrl.isBlank()) {
                message("Resolving stream URL...")
                return
            }

            val cleanHeaders = headers.toMutableMap().apply {
                if (streamUrl.contains("okcdn", ignoreCase = true)) {
                    remove("Origin")
                    remove("Referer")
                }
            }

            val isDirectPlayable = streamUrl.contains("googlevideo.com") ||
                                  streamUrl.startsWith("file://") ||
                                  streamUrl.startsWith("/") ||
                                  (!streamUrl.contains("hakunaymatata") && !streamUrl.contains("mzfi.me"))

            var playUrl = streamUrl
            if (!isDirectPlayable && !streamUrl.contains("127.0.0.1")) {
                val pPayload = JSONObject().apply {
                    put("action", "stream_proxy")
                    put("url", streamUrl)
                    val hObj = JSONObject()
                    cleanHeaders.forEach { (k, v) -> hObj.put(k, v) }
                    put("headers", hObj)
                }
                val pRes = runCatching { EmbeddedEngine.execute(context, pPayload) }.getOrNull()
                val px = pRes?.optJSONObject("result")?.optString("proxy_url")
                if (!px.isNullOrBlank()) {
                    playUrl = px
                }
            }

            if (playUrl.contains("127.0.0.1")) {
                StreamService.start(context, streamUrl, cleanHeaders)
                try {
                    Thread.sleep(150)
                } catch (_: Exception) {}
            }
            val uri = Uri.parse(playUrl)
            val isM3u8 = playUrl.contains(".m3u8", ignoreCase = true) || playUrl.contains("hls_playlist", ignoreCase = true)
            val mimeType = "video/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                putExtra("title", title.ifBlank { "Media Stream" })
                putExtra("from_start", true)
                putExtra("position", 0L)
                cleanHeaders.forEach { (k, v) -> putExtra(k, v) }
                if (cleanHeaders.isNotEmpty()) {
                    val headersArray = cleanHeaders.map { "${it.key}: ${it.value}" }.toTypedArray()
                    putExtra("headers", headersArray)
                    val bundle = Bundle()
                    cleanHeaders.forEach { (k, v) -> bundle.putString(k, v) }
                    putExtra("android.media.intent.extra.HTTP_HEADERS", bundle)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (targetPackage.isNotBlank() && targetPackage != "system_chooser" && targetPackage != "arc_player") {
                try {
                    intent.setPackage(targetPackage)
                    context.startActivity(intent)
                    return
                } catch (_: Exception) {
                    intent.setPackage(null)
                }
            }

            if (targetPackage == "system_chooser") {
                context.startActivity(Intent.createChooser(intent, "Play stream with"))
                return
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

    fun playStream(
        streamUrl: String,
        headers: Map<String, String> = emptyMap(),
        title: String = "",
        trackSpec: StreamTrackSpec? = null,
        isAudioOnlyStream: Boolean = false,
    ) {
        val spec = trackSpec ?: StreamTrackSpec(url = streamUrl)
        val effUrl = streamUrl.ifBlank { spec.url }.ifBlank { spec.fallbackUrl }
        if (streamingPlayer == "arc_player") {
            inAppPlayerTarget = PlayerStreamTarget(
                url = effUrl,
                headers = headers,
                title = title,
                videoUrl = spec.videoUrl,
                audioUrl = spec.audioUrl,
                fallbackUrl = spec.fallbackUrl,
                videoFormat = spec.videoFormat,
                audioFormat = spec.audioFormat,
                fallbackFormat = spec.fallbackFormat,
                isAudioOnlyStream = isAudioOnlyStream || (spec.videoUrl.isBlank() && effUrl.isNotBlank() && spec.audioUrl == effUrl),
            )
        } else {
            openStreamExternal(effUrl, headers, title, targetPackage = streamingPlayer)
        }
    }

    LaunchedEffect(localPlayRequest) {
        val req = localPlayRequest
        if (req != null && req.first.isNotBlank()) {
            openFile(req.first, titleOverride = req.second)
            onLocalPlayConsumed()
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

        val duration = (if (result.type == "search") searchElapsedSeconds else analysisElapsedSeconds).coerceAtLeast(0.4f)
        val actionLabel = when (result.type) {
            "search" -> "Search: \"$query\""
            "resolve" -> "Resolve media"
            "download" -> "Download media"
            else -> result.type
        }

        if (!isOk) {
            val friendlyError = formatUserFriendlyError(error)
            val errTrace = CliTrace(
                action = actionLabel,
                target = if (result.type == "search") query else input,
                durationSeconds = duration,
                steps = listOf(
                    "Media engine initialized",
                    "Executed task: ${result.type}",
                    "[ERROR] Execution failed: $friendlyError",
                ),
                stdoutLogs = logs,
                status = "Failed",
                isError = true,
            )
            if (result.type == "search") {
                searchTrace = errTrace
                searchLoading = false
            } else if (result.type == "resolve") {
                analysisTrace = errTrace
                resolveLoading = false
                target = target.copy(
                    canStream = false,
                    directStreamUrl = "",
                    qualityUrls = emptyMap(),
                    availableQualities = emptyList()
                )
            }
            message(friendlyError)
            if (result.type == "download") DownloadManager.markFailed(context, result.jobId, friendlyError)
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
                        val sortedAudios = prioritizeAudioTracks(audios)
                        resultList += SearchHit(
                            title = item.optString("title", "Untitled"),
                            source = item.optString("source", "Source"),
                            type = item.optString("type", "MEDIA"),
                            year = item.optString("year", ""),
                            providers = item.optJSONArray("providers")?.let { providers ->
                                (0 until providers.length()).joinToString(" · ") { providerIndex -> providers.optString(providerIndex) }
                            } ?: "",
                            audio = sortedAudios,
                            url = item.optString("url", ""),
                            poster = item.optString("poster", ""),
                        )
                    }
                }
                hits = resultList
                searchLoading = false

                // Preload top search results in background in parallel for instant resolution
                val effPreloadCookie = googleCookies.ifBlank { preferences.getString("google_cookies", "").orEmpty() }
                resultList.take(5).forEach { hit ->
                    scope.launch(Dispatchers.IO) {
                        val sortedAudio = prioritizeAudioTracks(hit.audio)
                        val prefLang = sortedAudio.firstOrNull { it.language.contains("hindi", ignoreCase = true) }?.language
                            ?: sortedAudio.firstOrNull { it.language.contains("english", ignoreCase = true) }?.language
                            ?: sortedAudio.firstOrNull()?.language.orEmpty()
                        val preUrl = sortedAudio.firstOrNull { it.language.contains("hindi", ignoreCase = true) }?.url?.ifEmpty { hit.url }
                            ?: sortedAudio.firstOrNull { it.language.contains("english", ignoreCase = true) }?.url?.ifEmpty { hit.url }
                            ?: sortedAudio.firstOrNull()?.url?.ifEmpty { hit.url }
                            ?: hit.url
                        if (preUrl.isNotBlank() && !preloadedTargets.containsKey(preUrl)) {
                            try {
                                val prePayload = JSONObject().apply {
                                    put("action", "resolve")
                                    put("url", preUrl)
                                    put("quality", "Best Available")
                                    if (prefLang.isNotBlank()) put("audio_lang", prefLang)
                                    if (effPreloadCookie.isNotBlank()) put("cookie", effPreloadCookie)
                                }
                                val res = runCatching { EmbeddedEngine.execute(context, prePayload) }.getOrNull()
                                if (res?.optBoolean("ok", false) == true) {
                                    val rData = res.optJSONObject("result")
                                    if (rData != null) {
                                        val rTitle = rData.optString("title", hit.title)
                                        val rDirect = rData.optString("direct_url").ifBlank { rData.optString("stream_url") }
                                        val rProxy = rData.optString("proxy_url")
                                        val rPoster = rData.optString("poster", hit.poster)
                                        val isSeries = rData.optBoolean("is_series", false)
                                        val qList = mutableListOf<String>()
                                        rData.optJSONArray("qualities")?.let { qArr ->
                                            for (qi in 0 until qArr.length()) qList.add(qArr.optString(qi))
                                        }
                                        val qUrls = mutableMapOf<String, String>()
                                        rData.optJSONObject("quality_urls")?.let { qObj ->
                                            qObj.keys().forEach { k -> qUrls[k] = qObj.optString(k) }
                                        }
                                        val qTracks = parseQualityTracks(rData)
                                        val topTrack = parseStreamTrackSpec(rData) ?: StreamTrackSpec(url = rDirect)
                                        val initQ = qList.firstOrNull() ?: "Best Available"
                                        val effTrack = resolveTrackForQuality(initQ, qTracks, qUrls, rDirect, topTrack)
                                        val aList = mutableListOf<AudioChoice>()
                                        (rData.optJSONArray("audio_choices") ?: rData.optJSONArray("audios"))?.let { aArr ->
                                            for (ai in 0 until aArr.length()) {
                                                val aItem = aArr.optJSONObject(ai) ?: continue
                                                aList.add(AudioChoice(aItem.optString("language"), aItem.optString("url")))
                                            }
                                        }
                                        val seasonsList = mutableListOf<SeasonInfo>()
                                        rData.optJSONArray("seasons")?.let { sArr ->
                                            for (si in 0 until sArr.length()) {
                                                val sObj = sArr.optJSONObject(si) ?: continue
                                                seasonsList.add(SeasonInfo(sObj.optInt("season"), sObj.optInt("episodes")))
                                            }
                                        }
                                        val hMap = mutableMapOf<String, String>()
                                        (rData.optJSONObject("stream_headers") ?: rData.optJSONObject("headers"))?.let { hObj ->
                                            hObj.keys().forEach { k -> hMap[k] = hObj.optString(k) }
                                        }
                                        val resolvedTarget = MediaTarget(
                                            title = rTitle,
                                            source = rData.optString("source", hit.source),
                                            detail = if (isSeries) "TV Series" else "Movie",
                                            size = "",
                                            description = rData.optString("description", ""),
                                            targetUrl = preUrl,
                                            directStreamUrl = effTrack.url.ifBlank { rDirect },
                                            proxyUrl = rProxy,
                                            availableQualities = if (qList.isNotEmpty()) qList else listOf("Best Available"),
                                            qualityUrls = qUrls,
                                            qualityTracks = qTracks,
                                            videoUrl = effTrack.videoUrl,
                                            audioUrl = effTrack.audioUrl,
                                            fallbackUrl = effTrack.fallbackUrl,
                                            videoFormat = effTrack.videoFormat,
                                            audioFormat = effTrack.audioFormat,
                                            fallbackFormat = effTrack.fallbackFormat,
                                            poster = rPoster,
                                            streamHeaders = hMap,
                                            isSeries = isSeries,
                                            seasons = seasonsList,
                                            audioTracks = prioritizeAudioTracks(if (hit.audio.isNotEmpty()) hit.audio else if (aList.isNotEmpty()) aList else emptyList()),
                                            selectedAudioLang = prefLang,
                                            mediaCategory = if (isSeries) "TV SERIES" else "MOVIE",
                                            year = hit.year,
                                            providers = hit.providers,
                                        )
                                        withContext(Dispatchers.Main) {
                                            preloadedTargets[preUrl] = resolvedTarget
                                            preloadedTargets[hit.url] = resolvedTarget
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }

                searchTrace = CliTrace(
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
                val searchKey = query.trim()
                if (searchKey.isNotBlank()) {
                    AppSessionCache.searchHits[searchKey] = resultList
                    AppSessionCache.searchTraces[searchKey] = CachedTrace(
                        trace = searchTrace,
                        liveStep = searchLiveStepText,
                        elapsedSeconds = searchElapsedSeconds,
                    )
                }
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
                val resolvedProviders = data?.optJSONArray("providers")?.let { providers ->
                    (0 until providers.length()).joinToString(" · ") { providers.optString(it) }
                }?.takeUnless { it.isBlank() } ?: target.providers

                val audioTracksList = mutableListOf<AudioChoice>()
                val aArray = data?.optJSONArray("audio_choices") ?: data?.optJSONArray("audios")
                if (aArray != null) {
                    for (i in 0 until aArray.length()) {
                        val aObj = aArray.optJSONObject(i) ?: continue
                        val lang = aObj.optString("language", "Default")
                        val u = aObj.optString("url", target.targetUrl)
                        if (lang.isNotBlank() && audioTracksList.none { it.language.equals(lang, ignoreCase = true) }) {
                            audioTracksList.add(AudioChoice(language = lang, url = u))
                        }
                    }
                }
                for (existing in target.audioTracks) {
                    if (existing.language.isNotBlank() && audioTracksList.none { it.language.equals(existing.language, ignoreCase = true) }) {
                        audioTracksList.add(existing)
                    }
                }

                val sortedTracks = prioritizeAudioTracks(audioTracksList)
                val effectiveAudioTracks = if (sortedTracks.isNotEmpty()) sortedTracks else prioritizeAudioTracks(target.audioTracks)
                val preferredLang = target.selectedAudioLang.ifBlank {
                    effectiveAudioTracks.firstOrNull { it.language.contains("hindi", ignoreCase = true) }?.language
                        ?: effectiveAudioTracks.firstOrNull { it.language.contains("english", ignoreCase = true) }?.language
                        ?: effectiveAudioTracks.firstOrNull()?.language.orEmpty()
                }

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

                val finalCanStream = canStream
                val finalMediaType = mediaType
                val finalSource = rawSource

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

                val qTrackMap = parseQualityTracks(data)
                val topTrackSpec = parseStreamTrackSpec(data) ?: StreamTrackSpec(url = resolvedDirectUrl)
                val initialSpec = resolveTrackForQuality(
                    quality = initialQuality,
                    qualityTracks = qTrackMap,
                    qualityUrls = qUrlMap,
                    fallbackDirectUrl = resolvedDirectUrl,
                    fallbackSpec = topTrackSpec,
                )
                val finalDirectUrl = initialSpec.url.ifBlank { resolvedDirectUrl }

                target = target.copy(
                    title = resolvedTitle,
                    source = finalSource,
                    detail = if (!finalCanStream || finalMediaType == "file") "Direct File" else (if (isSeriesResolved) "Season ${data?.optInt("season", target.selectedSeason) ?: target.selectedSeason} · Episode ${data?.optInt("episode", target.selectedEpisode) ?: target.selectedEpisode}" else (if (audioOnly) "Audio Stream" else "")),
                    directStreamUrl = finalDirectUrl,
                    proxyUrl = resolvedProxyUrl,
                    targetUrl = data?.optString("target_url").takeUnless { it.isNullOrBlank() } ?: target.targetUrl,
                    availableQualities = sortedQualities,
                    qualityUrls = if (qUrlMap.isNotEmpty()) qUrlMap else target.qualityUrls,
                    qualityTracks = qTrackMap,
                    videoUrl = initialSpec.videoUrl,
                    audioUrl = initialSpec.audioUrl,
                    fallbackUrl = initialSpec.fallbackUrl,
                    videoFormat = initialSpec.videoFormat,
                    audioFormat = initialSpec.audioFormat,
                    fallbackFormat = initialSpec.fallbackFormat,
                    poster = resolvedPoster,
                    streamHeaders = headersMap,
                    isSeries = isSeriesResolved,
                    seasons = seasonsList.ifEmpty { target.seasons },
                    isPlaylist = isPlaylist,
                    playlistEntries = playlistEntriesList,
                    selectedSeason = data?.optInt("season", target.selectedSeason) ?: target.selectedSeason,
                    selectedEpisode = data?.optInt("episode", target.selectedEpisode) ?: target.selectedEpisode,
                    canStream = finalCanStream,
                    mediaType = finalMediaType,
                    audioTracks = effectiveAudioTracks,
                    selectedAudioLang = preferredLang,
                    year = resolvedYear,
                    mediaCategory = resolvedCategory,
                    providers = resolvedProviders,
                )
                val initS = target.selectedSeason
                val initEp = target.selectedEpisode
                val initKey = "${target.targetUrl}_S${initS}_E${initEp}"
                episodeCache[initKey] = target

                resolveLoading = false
                if (overlay == "analysis" || homeOverlay == "analysis") {
                    overlay = "analysis"
                    homeOverlay = "analysis"
                }

                analysisTrace = CliTrace(
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
                val targetKey = target.targetUrl.trim().ifBlank { input.trim() }
                if (targetKey.isNotBlank()) {
                    val effLang = target.selectedAudioLang.lowercase().trim()
                    if (effLang.isNotBlank()) {
                        AppSessionCache.targets["${targetKey}#${effLang}"] = target
                        AppSessionCache.targetTraces["${targetKey}#${effLang}"] = CachedTrace(
                            trace = analysisTrace,
                            liveStep = analysisLiveStepText,
                            elapsedSeconds = analysisElapsedSeconds,
                        )
                    }
                    AppSessionCache.targets[targetKey] = target
                    AppSessionCache.targetTraces[targetKey] = CachedTrace(
                        trace = analysisTrace,
                        liveStep = analysisLiveStepText,
                        elapsedSeconds = analysisElapsedSeconds,
                    )
                }
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
                        DownloadManager.markCancelled(context, result.jobId)
                        message("Download cancelled.")
                    } else if (job != null) {
                        val errMsg = response.optString("error", error.ifBlank { "Download failed." })
                        val friendlyErr = formatUserFriendlyError(errMsg)
                        DownloadManager.markFailed(context, result.jobId, friendlyErr)
                        message(friendlyErr)
                    }
                    return
                }

                val jobTitle = job?.title?.ifBlank { "Download" } ?: "Download"
                DownloadManager.markComplete(context, result.jobId, path)
                message("Download complete: $jobTitle")
                postNotification(jobTitle, path)
            }
        }
    }

    fun submit(payload: JSONObject, type: String): String {
        currentTaskJob?.cancel()
        val jobId = java.util.UUID.randomUUID().toString()
        currentTaskJobId = jobId
        payload.put("job_id", jobId)
        currentTaskJob = scope.launch {
            val response = withContext(Dispatchers.IO) {
                runCatching {
                    if (!isActive) return@withContext JSONObject().apply { put("ok", false); put("cancelled", true) }
                    EmbeddedEngine.execute(context, payload)
                }.getOrElse { error ->
                    JSONObject().apply {
                        put("ok", false)
                        put("error", error.message ?: "The engine could not start.")
                    }
                }
            }
            if (isActive && currentTaskJobId == jobId) {
                if (type == "resolve" && overlay != "analysis" && homeOverlay != "analysis") {
                    resolveLoading = false
                    return@launch
                }
                handleEngineResult(EngineResult(jobId, type, response))
            }
        }
        return jobId
    }

    fun openHome() {
        currentTaskJob?.cancel()
        currentTaskJob = null
        currentTaskJobId?.let { EmbeddedEngine.cancel(it) }
        currentTaskJobId = null
        tabName = AppTab.HOME.name
        overlay = ""
        homeOverlay = ""
        navigationSource = ""
        isSearchTraceExpanded = false
        isAnalysisTraceExpanded = false
        searchLoading = false
        resolveLoading = false
        streamLoading = false
        episodeLoading = false
        hits = emptyList()
        query = ""
        searchTrace = null
        analysisTrace = null
        analysisLiveStepText = ""
        searchLiveStepText = ""
        selectedQuality = "Best Available"
        audioOnly = false
        target = MediaTarget("", "", "", "", "")
    }

    val currentActivity = context as? ComponentActivity
    BackHandler(enabled = true) {
        when {
            inAppPlayerTarget != null -> inAppPlayerTarget = null
            showPlayerDialog -> showPlayerDialog = false
            showSeriesDownloadDialog -> showSeriesDownloadDialog = false
            showAudioSelectDialog -> {
                showAudioSelectDialog = false
                pendingActionAfterAudioSelect = null
            }
            showOnboardingNotificationDialog -> {
                showOnboardingNotificationDialog = false
                showOnboardingBatteryDialog = true
            }
            showOnboardingBatteryDialog -> {
                showOnboardingBatteryDialog = false
                preferences.edit().putBoolean("has_seen_onboarding_permissions", true).apply()
            }
            showDeleteConfirmJobId != null -> showDeleteConfirmJobId = null
            showGoogleInfoDialog -> showGoogleInfoDialog = false
            showGoogleLoginDialog -> showGoogleLoginDialog = false
            showQualityDialog -> showQualityDialog = false
            showPathDialog -> showPathDialog = false
            tab != AppTab.HOME -> {
                tabName = AppTab.HOME.name
            }
            tab == AppTab.HOME -> {
                if (resolveLoading || searchLoading) {
                    currentTaskJob?.cancel()
                    currentTaskJob = null
                    currentTaskJobId?.let { EmbeddedEngine.cancel(it) }
                    currentTaskJobId = null
                    resolveLoading = false
                    searchLoading = false
                    streamLoading = false
                    episodeLoading = false
                    isSearchTraceExpanded = false
                    isAnalysisTraceExpanded = false
                    if (navigationSource == "search" && hits.isNotEmpty() && overlay == "analysis") {
                        overlay = "search"
                        homeOverlay = "search"
                    } else {
                        openHome()
                    }
                    return@BackHandler
                }
                when {
                    overlay == "analysis" -> {
                        currentTaskJob?.cancel()
                        currentTaskJob = null
                        currentTaskJobId?.let { EmbeddedEngine.cancel(it) }
                        currentTaskJobId = null
                        resolveLoading = false
                        streamLoading = false
                        episodeLoading = false
                        isSearchTraceExpanded = false
                        isAnalysisTraceExpanded = false
                        if (navigationSource == "search" && hits.isNotEmpty()) {
                            overlay = "search"
                            homeOverlay = "search"
                        } else {
                            openHome()
                        }
                    }
                    overlay == "search" -> openHome()
                    overlay.isNotBlank() -> openHome()
                    else -> currentActivity?.finish()
                }
            }
            else -> currentActivity?.finish()
        }
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
        providersOverride: String? = null,
    ) {
        val clean = value.trim()
        if (clean.isEmpty()) {
            message("Paste a link or enter a search.")
            return
        }
        analysisTrace = null
        analysisLiveStepText = ""
        analysisElapsedSeconds = 0f
        isAnalysisTraceExpanded = false

        val isTorrent = clean.startsWith("magnet:", ignoreCase = true) ||
            clean.endsWith(".torrent", ignoreCase = true) ||
            clean.contains(".torrent?", ignoreCase = true) ||
            (clean.startsWith("/") && clean.endsWith(".torrent", ignoreCase = true))

        val isDirectUrl = clean.startsWith("http://", ignoreCase = true) ||
            clean.startsWith("https://", ignoreCase = true) ||
            clean.startsWith("igl:", ignoreCase = true) ||
            clean.startsWith("file://", ignoreCase = true) ||
            clean.startsWith("/") ||
            isTorrent
        if (isDirectUrl) {
            val effLang = selectedAudioLang.lowercase().trim()
            val cachedTarget = if (effLang.isNotBlank()) AppSessionCache.targets["${clean}#${effLang}"] else AppSessionCache.targets[clean]
            val cachedTargetTrace = if (effLang.isNotBlank()) AppSessionCache.targetTraces["${clean}#${effLang}"] else AppSessionCache.targetTraces[clean]
            if (cachedTarget != null && season == null && episode == null) {
                target = cachedTarget
                cachedTargetTrace?.let {
                    analysisTrace = it.trace
                    analysisLiveStepText = it.liveStep
                    analysisElapsedSeconds = it.elapsedSeconds
                }
                overlay = "analysis"
                homeOverlay = "analysis"
                resolveLoading = false
                return
            }
            resolveLoading = true
            val isYouTubeClean = clean.contains("youtu", ignoreCase = true)
            val isSameShow = !isYouTubeClean && (clean.equals(target.targetUrl, ignoreCase = true) ||
                             (target.title.isNotBlank() && titleOverride != null && cleanShowTitle(target.title).equals(cleanShowTitle(titleOverride), ignoreCase = true)))

            if (!isSameShow) {
                selectedQuality = "Best Available"
                audioOnly = false
                resolvedUrl = ""
            }

            val titleToUse = when {
                titleOverride != null -> titleOverride
                isYouTubeClean -> "YouTube Video"
                isTorrent -> clean.split("/").lastOrNull { it.isNotBlank() }?.removeSuffix(".torrent") ?: "Torrent Media"
                else -> clean.split("/").lastOrNull { it.isNotBlank() } ?: "Media stream"
            }
            val fileExtMatch = isTorrent || Regex("""\.(torrent|zip|aip|rar|7z|tar|gz|apk|pdf|epub|iso|dmg|exe|bin|doc|docx|xls|xlsx|ppt|pptx)$""", RegexOption.IGNORE_CASE).containsMatchIn(clean.split("?").first())
            val isDirectFile = fileExtMatch || isTorrent
            val initialCanStream = !isDirectFile
            val initialMediaType = if (isTorrent) "torrent" else if (isDirectFile) "file" else "video"

            val isMovieTitle = !isTorrent && (titleToUse.contains("Movie", ignoreCase = true) || mediaCategoryOverride?.contains("MOVIE", ignoreCase = true) == true)
            val detectedSeries = !isMovieTitle && !isTorrent && ((mediaCategoryOverride?.contains("SERIES", ignoreCase = true) == true) ||
                                 (mediaCategoryOverride?.contains("TV", ignoreCase = true) == true) ||
                                 Regex("""\b(S\d+|Season\s*\d+)\b""", RegexOption.IGNORE_CASE).containsMatchIn(titleToUse))
            val detectedCategory = when {
                isTorrent -> "TORRENT"
                isDirectFile -> "FILE"
                isMovieTitle -> "MOVIE"
                detectedSeries -> "TV SERIES"
                clean.contains("youtu", ignoreCase = true) -> "YOUTUBE"
                clean.startsWith("igl:", ignoreCase = true) -> "IGL EPISODE"
                else -> "MOVIE"
            }
            val detectedYear = yearOverride ?: extractYear(titleToUse)

            val sortedChoices = prioritizeAudioTracks(audioChoices)
            val initialSelectedLang = selectedAudioLang.ifBlank {
                sortedChoices.firstOrNull { it.language.contains("hindi", ignoreCase = true) }?.language
                    ?: sortedChoices.firstOrNull { it.language.contains("english", ignoreCase = true) }?.language
                    ?: sortedChoices.firstOrNull()?.language.orEmpty()
            }

            resolvedUrl = ""
            target = if (isSameShow) {
                target.copy(
                    targetUrl = clean,
                    directStreamUrl = "",
                    qualityUrls = emptyMap(),
                    availableQualities = emptyList(),
                    selectedSeason = season ?: target.selectedSeason,
                    selectedEpisode = episode ?: target.selectedEpisode,
                    audioTracks = if (sortedChoices.isNotEmpty()) sortedChoices else prioritizeAudioTracks(target.audioTracks),
                    selectedAudioLang = if (initialSelectedLang.isNotBlank()) initialSelectedLang else target.selectedAudioLang,
                )
            } else {
                MediaTarget(
                    title = titleToUse,
                    source = if (detectedSeries) "TV Series" else (if (clean.contains("youtu", ignoreCase = true)) "YouTube" else "Movie"),
                    detail = if (detectedSeries) "Season ${season ?: 1} · Episode ${episode ?: 1}" else (if (isDirectFile) "Direct File" else "Analyzing media..."),
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
                    canStream = initialCanStream,
                    mediaType = initialMediaType,
                    audioTracks = sortedChoices,
                    selectedAudioLang = initialSelectedLang,
                    year = detectedYear,
                    mediaCategory = detectedCategory,
                    providers = providersOverride ?: target.providers,
                )
            }
            overlay = "analysis"
            homeOverlay = "analysis"
            submit(
                JSONObject().apply {
                    put("action", "resolve")
                    put("url", clean)
                    put("quality", if (selectedQuality.contains("Ask", ignoreCase = true)) "Best" else selectedQuality)
                    if (selectedAudioLang.isNotBlank()) put("audio_lang", selectedAudioLang)
                    if (season != null) put("season", season)
                    if (episode != null) put("episode", episode)
                    val effCookie = googleCookies.ifBlank { preferences.getString("google_cookies", "").orEmpty() }
                    if (effCookie.isNotBlank()) {
                        put("cookie", effCookie)
                    }
                },
                "resolve"
            )
        } else {
            val cachedHits = AppSessionCache.searchHits[clean]
            val cachedSearchTrace = AppSessionCache.searchTraces[clean]
            if (cachedHits != null) {
                query = clean
                hits = cachedHits
                cachedSearchTrace?.let {
                    searchTrace = it.trace
                    searchLiveStepText = it.liveStep
                    searchElapsedSeconds = it.elapsedSeconds
                }
                overlay = "search"
                homeOverlay = "search"
                searchLoading = false
                return
            }
            query = clean
            searchTrace = null
            searchLiveStepText = ""
            searchElapsedSeconds = 0f
            isSearchTraceExpanded = false
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

    val torrentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var fileName = "download.torrent"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIdx) ?: "download.torrent"
                    }
                }
                val torrentsDir = File(context.cacheDir, "torrents").apply { mkdirs() }
                val destFile = File(torrentsDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { inStream ->
                    destFile.outputStream().use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                input = destFile.absolutePath
                analyze(destFile.absolutePath)
            } catch (e: Exception) {
                message("Failed to open torrent file: ${e.message}")
            }
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
        audioOnlyOverride: Boolean? = null,
        audioLangOverride: String? = null
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

        val isIglTarget = target.targetUrl.startsWith("igl:", ignoreCase = true) ||
            target.source.contains("Latent", ignoreCase = true) ||
            target.source.contains("IGL", ignoreCase = true) ||
            target.mediaCategory.contains("IGL", ignoreCase = true)
        val isFileTarget = !target.canStream || target.mediaType.equals("file", true) || target.source.equals("Direct file", true)

        val jobSource = when {
            isFileTarget -> ""
            isIglTarget -> "ARC"
            else -> target.source.ifBlank { "Stream" }
        }

        val jobDetail = when {
            isFileTarget -> "File"
            isIglTarget -> "IGL · ARC"
            isAudio -> "Audio"
            else -> "${selectedQuality} · Video"
        }

        val job = DownloadJob(
            id = java.util.UUID.randomUUID().toString(),
            title = finalTitle,
            source = jobSource,
            detail = jobDetail,
            status = "Downloading",
            speed = "Connecting...",
            downloaded = "Starting...",
            poster = target.poster,
            url = targetEngineUrl,
            headers = target.streamHeaders,
        )
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
            val activeAudioLang = audioLangOverride ?: target.selectedAudioLang
            if (activeAudioLang.isNotBlank()) {
                put("audio_lang", activeAudioLang)
            }
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
            val effGoogleCookie = googleCookies.ifBlank { preferences.getString("google_cookies", "").orEmpty() }
            if (effGoogleCookie.isNotBlank()) {
                put("cookie", effGoogleCookie)
            }
        }
        DownloadService.startDownload(context, job, payload)
        message("Download queued: $finalTitle")
        tabName = AppTab.ACTIVITY.name
    }

    fun pauseJob(jobId: String) {
        DownloadService.pause(context, jobId)
        message("Download paused.")
    }

    fun resumeJob(jobId: String) {
        DownloadService.resume(context, jobId)
        message("Download resumed.")
    }

    fun cancelJob(jobId: String) {
        activeCancelledIds.add(jobId)
        cancelledJobIds = cancelledJobIds + jobId
        DownloadService.cancel(context, jobId)
        message("Download cancelled.")
    }

    fun deleteJob(jobId: String, removeFile: Boolean = false) {
        activeCancelledIds.add(jobId)
        cancelledJobIds = cancelledJobIds + jobId
        DownloadService.cancel(context, jobId)
        DownloadManager.removeJob(context, jobId, removeFile)
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
                                    trace = searchTrace,
                                    elapsedSeconds = searchElapsedSeconds,
                                    liveStep = searchLiveStepText,
                                    isExpanded = isSearchTraceExpanded,
                                    onToggleExpand = { isSearchTraceExpanded = !isSearchTraceExpanded },
                                    onCopyLogs = { copyToClipboard(searchTrace?.stdoutLogs.orEmpty(), "Search logs") },
                                    onBack = ::openHome,
                                    onSelect = { hit, audio ->
                                        currentTaskJob?.cancel()
                                        currentTaskJob = null
                                        currentTaskJobId?.let { EmbeddedEngine.cancel(it) }
                                        currentTaskJobId = null
                                        navigationSource = "search"
                                        isAnalysisTraceExpanded = false
                                        val sortedAudios = prioritizeAudioTracks(hit.audio)
                                        val chosenLang = audio.language.ifBlank {
                                            sortedAudios.firstOrNull { it.language.contains("hindi", ignoreCase = true) }?.language
                                                ?: sortedAudios.firstOrNull { it.language.contains("english", ignoreCase = true) }?.language
                                                ?: sortedAudios.firstOrNull()?.language.orEmpty()
                                        }
                                        val targetUrl = audio.url.ifEmpty {
                                            sortedAudios.firstOrNull { it.language.contains("hindi", ignoreCase = true) }?.url
                                                ?: sortedAudios.firstOrNull { it.language.contains("english", ignoreCase = true) }?.url
                                                ?: sortedAudios.firstOrNull()?.url
                                                ?: hit.url
                                        }
                                        val pre = preloadedTargets[targetUrl] ?: preloadedTargets[hit.url]
                                        if (pre != null && pre.availableQualities.isNotEmpty() && !pre.directStreamUrl.isNullOrBlank()) {
                                            selectedQuality = pre.availableQualities.firstOrNull() ?: "Best Available"
                                            audioOnly = false
                                            target = pre.copy(
                                                audioTracks = if (sortedAudios.isNotEmpty()) sortedAudios else prioritizeAudioTracks(pre.audioTracks),
                                                selectedAudioLang = chosenLang.ifBlank { pre.selectedAudioLang }
                                            )
                                            overlay = "analysis"
                                            homeOverlay = "analysis"
                                            resolveLoading = false
                                        } else {
                                            selectedQuality = "Best Available"
                                            audioOnly = false
                                            analyze(
                                                value = targetUrl,
                                                titleOverride = hit.title,
                                                posterOverride = hit.poster,
                                                audioChoices = sortedAudios,
                                                selectedAudioLang = chosenLang,
                                                mediaCategoryOverride = if (hit.type.contains("SERIES", ignoreCase = true) || hit.type.contains("TV", ignoreCase = true)) "TV SERIES" else "MOVIE",
                                                yearOverride = hit.year,
                                                providersOverride = hit.providers,
                                            )
                                        }
                                    },
                                )
                                "analysis" -> AnalysisScreen(
                                    target = target,
                                    loading = resolveLoading,
                                    streamLoading = streamLoading,
                                    episodeLoading = episodeLoading,
                                    trace = analysisTrace,
                                    elapsedSeconds = analysisElapsedSeconds,
                                    liveStep = analysisLiveStepText,
                                    isExpanded = isAnalysisTraceExpanded,
                                    onToggleExpand = { isAnalysisTraceExpanded = !isAnalysisTraceExpanded },
                                    onCopyLogs = { copyToClipboard(analysisTrace?.stdoutLogs.orEmpty(), "Media logs") },
                                    quality = selectedQuality,
                                    audioOnly = audioOnly,
                                    onBack = {
                                        currentTaskJob?.cancel()
                                        currentTaskJob = null
                                        currentTaskJobId?.let { EmbeddedEngine.cancel(it) }
                                        currentTaskJobId = null
                                        resolveLoading = false
                                        isSearchTraceExpanded = false
                                        isAnalysisTraceExpanded = false
                                        if (hits.isNotEmpty()) {
                                            overlay = "search"
                                            homeOverlay = "search"
                                        } else {
                                            openHome()
                                        }
                                    },
                                    onQuality = { q ->
                                        selectedQuality = q
                                        val nextSpec = resolveTrackForQuality(
                                            quality = q,
                                            qualityTracks = target.qualityTracks,
                                            qualityUrls = target.qualityUrls,
                                            fallbackDirectUrl = target.directStreamUrl,
                                        )
                                        target = target.copy(
                                            directStreamUrl = nextSpec.url.ifBlank { target.directStreamUrl },
                                            videoUrl = nextSpec.videoUrl,
                                            audioUrl = nextSpec.audioUrl,
                                            fallbackUrl = nextSpec.fallbackUrl,
                                            videoFormat = nextSpec.videoFormat,
                                            audioFormat = nextSpec.audioFormat,
                                            fallbackFormat = nextSpec.fallbackFormat,
                                        )
                                    },
                                    onAudioOnly = { aOnly ->
                                        audioOnly = aOnly
                                        val activeQ = if (aOnly) "Best Audio (320kbps)" else (target.availableQualities.firstOrNull() ?: "Best Available")
                                        selectedQuality = activeQ
                                        val nextSpec = resolveTrackForQuality(
                                            quality = activeQ,
                                            qualityTracks = target.qualityTracks,
                                            qualityUrls = target.qualityUrls,
                                            fallbackDirectUrl = target.directStreamUrl,
                                        )
                                        target = target.copy(
                                            directStreamUrl = nextSpec.url.ifBlank { target.directStreamUrl },
                                            videoUrl = nextSpec.videoUrl,
                                            audioUrl = nextSpec.audioUrl,
                                            fallbackUrl = nextSpec.fallbackUrl,
                                            videoFormat = nextSpec.videoFormat,
                                            audioFormat = nextSpec.audioFormat,
                                            fallbackFormat = nextSpec.fallbackFormat,
                                        )
                                    },
                                    onSelectAudio = { audio ->
                                        currentTaskJob?.cancel()
                                        currentTaskJob = null
                                        currentTaskJobId?.let { EmbeddedEngine.cancel(it) }
                                        currentTaskJobId = null
                                        val newUrl = audio.url.ifEmpty { target.targetUrl }
                                        target = target.copy(selectedAudioLang = audio.language)
                                        analyze(
                                            value = newUrl,
                                            titleOverride = target.title,
                                            posterOverride = target.poster,
                                            season = if (target.isSeries) target.selectedSeason else null,
                                            episode = if (target.isSeries) target.selectedEpisode else null,
                                            audioChoices = target.audioTracks,
                                            selectedAudioLang = audio.language,
                                            mediaCategoryOverride = target.mediaCategory,
                                            yearOverride = target.year,
                                            providersOverride = target.providers,
                                        )
                                    },
                                    onDownload = {
                                        if (target.isSeries) {
                                            showSeriesDownloadDialog = true
                                        } else if (target.audioTracks.size > 1 && target.selectedAudioLang.isBlank()) {
                                            pendingActionAfterAudioSelect = "download"
                                            showAudioSelectDialog = true
                                        } else {
                                            startDownload(audioLangOverride = target.selectedAudioLang)
                                        }
                                    },
                                    onStream = {
                                        if (target.audioTracks.size > 1 && target.selectedAudioLang.isBlank()) {
                                            pendingActionAfterAudioSelect = "stream"
                                            showAudioSelectDialog = true
                                        } else {
                                            val displayTitle = if (target.isSeries) {
                                                "${cleanShowTitle(target.title)} S${target.selectedSeason}E${target.selectedEpisode}"
                                            } else {
                                                target.title
                                            }
                                            val currentTrack = resolveTrackForQuality(
                                                quality = selectedQuality,
                                                qualityTracks = target.qualityTracks,
                                                qualityUrls = target.qualityUrls,
                                                fallbackDirectUrl = target.directStreamUrl.ifBlank { target.proxyUrl },
                                                fallbackSpec = StreamTrackSpec(
                                                    url = target.directStreamUrl,
                                                    videoUrl = target.videoUrl,
                                                    audioUrl = target.audioUrl,
                                                    fallbackUrl = target.fallbackUrl,
                                                    videoFormat = target.videoFormat,
                                                    audioFormat = target.audioFormat,
                                                    fallbackFormat = target.fallbackFormat,
                                                ),
                                            )
                                            val directCandidate = currentTrack.url.ifBlank { target.directStreamUrl.ifBlank { target.proxyUrl } }
                                            val hasMergedTracks = currentTrack.videoUrl.isNotBlank() && currentTrack.audioUrl.isNotBlank()
                                            val isValidDirectStream = (directCandidate.isNotBlank() || hasMergedTracks) &&
                                                !directCandidate.startsWith("https://www.youtube.com/watch") &&
                                                !directCandidate.startsWith("https://youtu.be/") &&
                                                !directCandidate.startsWith("http://www.youtube.com/watch") &&
                                                !directCandidate.startsWith("http://youtu.be/")

                                            if (isValidDirectStream && !resolveLoading) {
                                                streamLoading = true
                                                playStream(
                                                    streamUrl = directCandidate,
                                                    headers = target.streamHeaders,
                                                    title = displayTitle,
                                                    trackSpec = currentTrack,
                                                    isAudioOnlyStream = audioOnly,
                                                )
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
                                                        if (target.selectedAudioLang.isNotBlank()) {
                                                            put("audio_lang", target.selectedAudioLang)
                                                        }
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
                                                    val qTrackMap = parseQualityTracks(data)
                                                    val topTrack = parseStreamTrackSpec(data) ?: StreamTrackSpec()
                                                    val directUrl = data?.optString("direct_url").takeUnless { it.isNullOrBlank() }
                                                        ?: data?.optString("stream_url").takeUnless { it.isNullOrBlank() }
                                                        ?: data?.optString("url").orEmpty()
                                                    val proxyUrl = data?.optString("proxy_url").orEmpty()
                                                    val resolvedSpec = resolveTrackForQuality(
                                                        quality = selectedQuality,
                                                        qualityTracks = qTrackMap,
                                                        qualityUrls = qUrlMap,
                                                        fallbackDirectUrl = directUrl.ifBlank { proxyUrl },
                                                        fallbackSpec = topTrack,
                                                    )
                                                    val hObj = data?.optJSONObject("stream_headers") ?: data?.optJSONObject("headers")
                                                    val hMap = mutableMapOf<String, String>()
                                                    hObj?.keys()?.forEach { k -> hMap[k] = hObj.optString(k) }
                                                    val playUrl = resolvedSpec.url.ifBlank { directUrl.ifBlank { proxyUrl } }
                                                    if (playUrl.isNotBlank() || (resolvedSpec.videoUrl.isNotBlank() && resolvedSpec.audioUrl.isNotBlank())) {
                                                        val effHeaders = if (hMap.isNotEmpty()) hMap else target.streamHeaders
                                                        target = target.copy(
                                                            directStreamUrl = playUrl,
                                                            proxyUrl = proxyUrl,
                                                            qualityUrls = if (qUrlMap.isNotEmpty()) qUrlMap else target.qualityUrls,
                                                            qualityTracks = if (qTrackMap.isNotEmpty()) qTrackMap else target.qualityTracks,
                                                            videoUrl = resolvedSpec.videoUrl,
                                                            audioUrl = resolvedSpec.audioUrl,
                                                            fallbackUrl = resolvedSpec.fallbackUrl,
                                                            videoFormat = resolvedSpec.videoFormat,
                                                            audioFormat = resolvedSpec.audioFormat,
                                                            fallbackFormat = resolvedSpec.fallbackFormat,
                                                            streamHeaders = effHeaders,
                                                        )
                                                        playStream(
                                                            streamUrl = playUrl,
                                                            headers = effHeaders,
                                                            title = displayTitle,
                                                            trackSpec = resolvedSpec,
                                                            isAudioOnlyStream = audioOnly,
                                                        )
                                                    } else {
                                                        message("Could not resolve stream for $displayTitle")
                                                    }
                                                    streamLoading = false
                                                }
                                            }
                                        }
                                    },
                                    onSelectSeason = { s ->
                                        loadSeriesEpisode(
                                            seasonNum = s,
                                            episodeNum = 1,
                                            target = target,
                                            inputFallback = input,
                                            selectedQuality = selectedQuality,
                                            episodeCache = episodeCache,
                                            context = context,
                                            scope = scope,
                                            currentJob = episodeResolveJob,
                                            onJobChange = { episodeResolveJob = it },
                                            onLoadingChange = { episodeLoading = it },
                                            onQualitySelected = { selectedQuality = it },
                                            onTargetUpdated = { target = it },
                                        )
                                    },
                                    onSelectEpisode = { ep ->
                                        loadSeriesEpisode(
                                            seasonNum = target.selectedSeason,
                                            episodeNum = ep,
                                            target = target,
                                            inputFallback = input,
                                            selectedQuality = selectedQuality,
                                            episodeCache = episodeCache,
                                            context = context,
                                            scope = scope,
                                            currentJob = episodeResolveJob,
                                            onJobChange = { episodeResolveJob = it },
                                            onLoadingChange = { episodeLoading = it },
                                            onQualitySelected = { selectedQuality = it },
                                            onTargetUpdated = { target = it },
                                        )
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
                                    onPickTorrent = {
                                        torrentPickerLauncher.launch(arrayOf("application/x-bittorrent", "application/octet-stream", "*/*"))
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
                        onDismissJob = { id -> DownloadManager.removeJob(context, id, false) },
                        onCopyLogs = { text -> copyToClipboard(text, "Error logs") },
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
                        streamingPlayerName = streamingPlayerName,
                        onOpenPlayerDialog = { showPlayerDialog = true },
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
                        googleAccountEmail = googleAccountEmail,
                        onGoogleInfo = { showGoogleInfoDialog = true },
                        onGoogleDisconnect = {
                            googleAccountEmail = ""
                            googleCookies = ""
                            preferences.edit()
                                .remove("google_account_email")
                                .remove("google_cookies")
                                .apply()
                            try {
                                CookieManager.getInstance().removeAllCookies(null)
                                CookieManager.getInstance().flush()
                            } catch (_: Exception) {}
                            message("Google account disconnected.")
                        },
                        isBatteryExempt = isBatteryExempt,
                        onRequestBatteryExemption = {
                            requestIgnoreBatteryOptimizations(context)
                            isBatteryExempt = isIgnoringBatteryOptimizations(context)
                        },
                        onOpenAboutFeatures = { showAboutDialog = true },
                    )
                }
            }
        }

            if (showAboutDialog) {
                AboutFeaturesDialog(onDismiss = { showAboutDialog = false })
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

            // Select Audio Track Dialog
            if (showAudioSelectDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showAudioSelectDialog = false
                        pendingActionAfterAudioSelect = null
                    },
                    containerColor = ArcSurface,
                    titleContentColor = ArcWhite,
                    textContentColor = ArcWhite,
                    shape = RoundedCornerShape(20.dp),
                    title = { Text("Select Audio Language") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Please select an audio language to continue:", color = ArcMuted, fontSize = 12.sp)
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(target.audioTracks) { track ->
                                    Button(
                                        onClick = {
                                            val chosenLang = track.language
                                            target = target.copy(selectedAudioLang = chosenLang)
                                            showAudioSelectDialog = false
                                            val action = pendingActionAfterAudioSelect
                                            pendingActionAfterAudioSelect = null
                                            if (action == "download") {
                                                startDownload(audioLangOverride = chosenLang)
                                            } else if (action == "stream") {
                                                val newUrl = track.url.ifEmpty { target.targetUrl }
                                                analyze(
                                                    value = newUrl,
                                                    titleOverride = target.title,
                                                    posterOverride = target.poster,
                                                    season = if (target.isSeries) target.selectedSeason else null,
                                                    episode = if (target.isSeries) target.selectedEpisode else null,
                                                    audioChoices = target.audioTracks,
                                                    selectedAudioLang = chosenLang,
                                                    mediaCategoryOverride = target.mediaCategory,
                                                    yearOverride = target.year,
                                                    providersOverride = target.providers,
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = ArcRaised, contentColor = ArcWhite),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(track.language)
                                            Text("Select", color = ArcWhite, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = {
                            showAudioSelectDialog = false
                            pendingActionAfterAudioSelect = null
                        }) {
                            Text("Cancel")
                        }
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

            // Player Selection Dialog
            if (showPlayerDialog) {
                PlayerSelectionDialog(
                    currentPlayerPackage = streamingPlayer,
                    onPlayerSelected = { pkg, name ->
                        streamingPlayer = pkg
                        streamingPlayerName = name
                        preferences.edit()
                            .putString("streaming_player_package", pkg)
                            .putString("streaming_player_name", name)
                            .apply()
                        message("Streaming player set to $name")
                    },
                    onDismiss = { showPlayerDialog = false }
                )
            }

            // Onboarding Step 1: Notifications Permission Dialog
            if (showOnboardingNotificationDialog) {
                OnboardingNotificationDialog(
                    onAllow = {
                        showOnboardingNotificationDialog = false
                        notifications = true
                        preferences.edit().putBoolean("notifications_enabled", true).apply()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        showOnboardingBatteryDialog = true
                    },
                    onCancel = {
                        showOnboardingNotificationDialog = false
                        showOnboardingBatteryDialog = true
                    }
                )
            }

            // Onboarding Step 2: Battery Optimization Dialog
            if (showOnboardingBatteryDialog) {
                OnboardingBatteryDialog(
                    onAllow = {
                        showOnboardingBatteryDialog = false
                        preferences.edit().putBoolean("has_seen_onboarding_permissions", true).apply()
                        requestIgnoreBatteryOptimizations(context)
                    },
                    onCancel = {
                        showOnboardingBatteryDialog = false
                        preferences.edit().putBoolean("has_seen_onboarding_permissions", true).apply()
                    }
                )
            }

            // Google Account Info Dialog
            if (showGoogleInfoDialog) {
                GoogleAccountInfoDialog(
                    isConnected = googleAccountEmail.isNotBlank(),
                    accountEmail = googleAccountEmail,
                    onLoginClick = {
                        showGoogleInfoDialog = false
                        launchGoogleLogin()
                    },
                    onDismiss = { showGoogleInfoDialog = false },
                )
            }

            // Google Account In-App Sign-In Dialog
            if (showGoogleLoginDialog) {
                GoogleLoginDialog(
                    loginHint = googleLoginHint,
                    onSuccess = { email, cookies ->
                        googleAccountEmail = email
                        googleCookies = cookies
                        preferences.edit()
                            .putString("google_account_email", email)
                            .putString("google_cookies", cookies)
                            .apply()
                        showGoogleLoginDialog = false
                        googleLoginHint = ""
                        message("Google account connected ($email)")
                    },
                    onDismiss = {
                        showGoogleLoginDialog = false
                        googleLoginHint = ""
                    },
                )
            }

            // In-App Video Player Overlay
            if (inAppPlayerTarget != null) {
                Dialog(
                    onDismissRequest = { inAppPlayerTarget = null },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false
                    )
                ) {
                    ArcPlayerScreen(
                        streamTarget = inAppPlayerTarget!!,
                        onClose = { inAppPlayerTarget = null }
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
    onPickTorrent: () -> Unit = {},
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
                val activeDownloadsCount = jobs.count { it.status == "Downloading" || it.status == "Muxing" }
                val pillText = if (activeDownloadsCount > 0) "Downloading $activeDownloadsCount" else "Ready"
                StatusPill(pillText)
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            Text("Universal Downloader", fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(4.dp))
            Text("Paste a link. Get your media.", color = ArcMuted, fontSize = 13.sp)
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
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onPickTorrent,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Folder,
                                contentDescription = "Add Torrent",
                                tint = ArcMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        TextButton(onClick = onPaste) { Text("Paste") }
                    }
                },
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
            item { EmptyState("Nothing here yet", "Paste a link to begin.") }
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
                val sorted = prioritizeAudioTracks(hit.audio)
                val chosenAudio = sorted.firstOrNull { it.language.contains("hindi", ignoreCase = true) }
                    ?: sorted.firstOrNull { it.language.contains("english", ignoreCase = true) }
                    ?: sorted.firstOrNull()
                    ?: AudioChoice("Video", hit.url)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                ) {
                    if (hit.poster.isNotBlank()) {
                        AsyncImage(
                            model = hit.poster,
                            contentDescription = hit.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp, 64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF222222)),
                        )
                    } else {
                        MediaGlyph(Icons.Outlined.Movie, Modifier.size(46.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(hit.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(3.dp))
                        val metaParts = if (hit.url.startsWith("igl:", ignoreCase = true) || hit.source.contains("Latent", ignoreCase = true) || hit.type.equals("IGL", ignoreCase = true)) {
                            listOf("IGL", "ARC")
                        } else {
                            buildList {
                                val provStr = hit.providers.ifBlank { if (hit.source.isNotBlank() && !hit.source.equals("NetMirror", true) && !hit.source.equals("Movie", true) && !hit.source.equals("TV Series", true)) hit.source else "" }
                                if (provStr.isNotBlank()) {
                                    add(provStr)
                                } else {
                                    add(hit.type)
                                }
                                val yr = hit.year.takeUnless { it == "0" || it.isBlank() }
                                if (yr != null) add(yr)
                            }
                        }
                        Text(metaParts.joinToString(" · "), color = ArcMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "Open",
                        tint = ArcMuted,
                        modifier = Modifier.size(18.dp),
                    )
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

    val yearStr = extractYear(target.title, target.year).takeUnless { it == "0" }.orEmpty()
    val yearBadge = if (yearStr.isNotBlank()) " · $yearStr" else ""
    val providerBadge = if (target.providers.isNotBlank()) " · ${target.providers}" else ""

    val isIglTarget = target.targetUrl.startsWith("igl:", ignoreCase = true) ||
        target.source.contains("Latent", ignoreCase = true) ||
        target.source.contains("IGL", ignoreCase = true) ||
        target.mediaCategory.contains("IGL", ignoreCase = true)
    val isFileTarget = target.mediaType == "file" || !target.canStream || target.source.equals("Direct file", true)

    val categoryBadge = when {
        isIglTarget -> "IGL · ARC"
        isFileTarget -> "FILE"
        isSeriesTarget -> if (target.providers.isNotBlank()) "${target.providers.uppercase()}$yearBadge" else "TV SERIES$yearBadge"
        isMovieTarget -> if (target.providers.isNotBlank()) "${target.providers.uppercase()}$yearBadge" else "MOVIE$yearBadge"
        isYouTubeTarget -> "YOUTUBE"
        target.source.isNotBlank() && !target.source.equals("Stream", ignoreCase = true) && !target.source.equals("NetMirror", ignoreCase = true) && !target.source.equals("Movie", ignoreCase = true) && !target.source.equals("Direct file", true) -> "${target.source.uppercase()}$yearBadge"
        else -> if (target.providers.isNotBlank()) "${target.providers.uppercase()}$yearBadge" else "MOVIE$yearBadge"
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

        // Audio Tracks selector: visible when analysis finishes alongside quality profile
        if (!loading && target.audioTracks.isNotEmpty()) {
            item {
                val sortedTracks = remember(target.audioTracks) { prioritizeAudioTracks(target.audioTracks) }
                val currentLang = target.selectedAudioLang.ifBlank {
                    sortedTracks.firstOrNull { it.language.contains("hindi", ignoreCase = true) }?.language
                        ?: sortedTracks.firstOrNull { it.language.contains("english", ignoreCase = true) }?.language
                        ?: sortedTracks.firstOrNull()?.language.orEmpty()
                }
                SectionHeader("Audio Language", currentLang)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sortedTracks) { aChoice ->
                        val isAudioSelected = aChoice.language.equals(currentLang, ignoreCase = true)
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
                        itemsIndexed(
                            items = target.playlistEntries,
                            key = { idx, entry -> if (entry.id.isNotBlank()) entry.id else "${entry.url}_$idx" },
                            contentType = { _, _ -> "playlist_entry" }
                        ) { idx, entry ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ArcSurface)
                                    .clickable { onSelectPlaylistItem?.invoke(entry) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
            val hasResolvedMedia = target.directStreamUrl.isNotBlank() ||
                target.proxyUrl.isNotBlank() ||
                target.qualityUrls.isNotEmpty() ||
                target.isPlaylist ||
                target.targetUrl.startsWith("magnet:", ignoreCase = true) ||
                target.source.contains("torrent", ignoreCase = true) ||
                target.mediaType == "file" ||
                target.mediaType == "torrent"

            if (!loading && !streamLoading && !episodeLoading && !hasResolvedMedia && target.title.isNotBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ArcSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF333333)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(36.dp),
                            )
                            Text(
                                "Stream Unavailable",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = ArcWhite,
                            )
                            Spacer(Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = onBack,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                            ) {
                                Text("Go Back", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                item {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArcWhite, contentColor = ArcBlack),
                    ) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        val dlText = when {
                            target.isSeries -> "Download $quality"
                            target.isPlaylist -> "Download Playlist ($quality)"
                            target.source.contains("torrent", ignoreCase = true) || target.mediaType == "torrent" -> "Download Torrent"
                            !target.canStream || target.mediaType == "file" || target.source.equals("Direct file", true) -> "Download File"
                            else -> "Download $quality"
                        }
                        Text(
                            dlText,
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
}

@Composable
private fun ActivityScreen(
    jobs: List<DownloadJob>,
    onCancelJob: (String) -> Unit,
    onPauseJob: (String) -> Unit = {},
    onResumeJob: (String) -> Unit = {},
    onDismissJob: (String) -> Unit = {},
    onCopyLogs: (String) -> Unit = {},
) {
    var viewLogsJob by remember { mutableStateOf<DownloadJob?>(null) }

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
            item { EmptyState("No Active Downloads", "Active downloads will appear here.", icon = Icons.Outlined.FileDownload) }
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
                                val actSrc = if (job.source.isNotBlank() &&
                                    !job.source.equals("NetMirror", true) &&
                                    !job.source.equals("Stream", true) &&
                                    !job.source.equals("Direct file", true) &&
                                    !job.source.equals("ARC", true) &&
                                    !job.detail.equals("File", true) &&
                                    !job.detail.equals("IGL · ARC", true)) " · ${job.source}" else ""
                                Text(
                                    text = "${job.detail}$actSrc",
                                    color = ArcMuted,
                                    fontSize = 11.sp,
                                )
                            }
                            val isFetching = job.status == "Downloading" && job.progress <= 0.5f && (job.speed == "--" || job.speed.isBlank() || job.speed == "Connecting..." || job.downloaded == "0 MB" || job.downloaded.startsWith("Starting"))
                            val badgeText = if (job.status == "Muxing") "Muxing ${job.progress.toInt()}%" else if (isFetching) "Fetching" else job.status
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
                                job.status == "Muxing" -> "Muxing ${String.format(java.util.Locale.US, "%.1f", job.progress)}% · Combining tracks"
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
                            val speedText = if (job.status == "Muxing") "Muxing tracks" else if (isFetching) "Resolving..." else "${job.speed}$etaDisplay"
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
                                    onClick = { viewLogsJob = job },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Logs", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
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

    if (viewLogsJob != null) {
        val currJob = viewLogsJob!!
        AlertDialog(
            onDismissRequest = { viewLogsJob = null },
            containerColor = ArcRaised,
            title = {
                Text("Error Diagnostics", fontWeight = FontWeight.Bold, color = ArcWhite, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text(currJob.title, color = ArcMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(10.dp))
                    val logContent = currJob.errorLog.ifBlank { currJob.detail.ifBlank { "No detailed diagnostic trace available." } }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .background(Color(0xFF161616), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF2E2E2E), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = logContent,
                            color = ArcWhite,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val logText = currJob.errorLog.ifBlank { currJob.detail }
                        onCopyLogs(logText)
                    }
                ) {
                    Text("Copy Logs", color = ArcWhite, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewLogsJob = null }) {
                    Text("Close", color = ArcMuted)
                }
            }
        )
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
            item { EmptyState("No Files Yet", "Completed downloads will appear here.", icon = Icons.Outlined.Folder) }
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
                                val actSrc = if (job.source.isNotBlank() &&
                                    !job.source.equals("NetMirror", true) &&
                                    !job.source.equals("Stream", true) &&
                                    !job.source.equals("Direct file", true) &&
                                    !job.source.equals("ARC", true) &&
                                    !job.detail.equals("File", true) &&
                                    !job.detail.equals("IGL · ARC", true)) " · ${job.source}" else ""
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
    streamingPlayerName: String = "ARC Player",
    onOpenPlayerDialog: () -> Unit = {},
    notifications: Boolean,
    onToggleNotifications: (Boolean) -> Unit,
    googleAccountEmail: String,
    onGoogleInfo: () -> Unit,
    onGoogleDisconnect: () -> Unit,
    isBatteryExempt: Boolean = false,
    onRequestBatteryExemption: () -> Unit = {},
    onOpenAboutFeatures: () -> Unit = {},
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

            Spacer(Modifier.height(6.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = ArcSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenPlayerDialog),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                ) {
                    MediaGlyph(Icons.Outlined.PlayArrow, Modifier.size(36.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Streaming Player", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(streamingPlayerName, color = ArcMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                            "Know when it's done",
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

            Spacer(Modifier.height(6.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = ArcSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onRequestBatteryExemption),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                ) {
                    MediaGlyph(Icons.Outlined.CheckCircle, Modifier.size(36.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Battery Optimization", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            "Unrestricted · Downloads never pause",
                            color = ArcMuted,
                            fontSize = 11.sp,
                        )
                    }
                    Switch(
                        checked = isBatteryExempt,
                        onCheckedChange = { onRequestBatteryExemption() },
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
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                ) {
                    MediaGlyph(Icons.Outlined.AccountCircle, Modifier.size(36.dp))
                    Column(Modifier.weight(1f)) {
                        if (googleAccountEmail.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(googleAccountEmail, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                )
                            }
                            Text("Connected · YouTube playback authenticated", color = ArcMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        } else {
                            Text("Google Account", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                            Text("Optional · YouTube authentication", color = ArcMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    IconButton(
                        onClick = onGoogleInfo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = "Account info",
                            tint = ArcMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (googleAccountEmail.isNotBlank()) {
                        OutlinedButton(
                            onClick = onGoogleDisconnect,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                        ) {
                            Text("Disconnect", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            SettingsGroup("About")
            Card(
                colors = CardDefaults.cardColors(containerColor = ArcSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenAboutFeatures),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                ) {
                    MediaGlyph(Icons.Outlined.Info, Modifier.size(36.dp))
                    Column(Modifier.weight(1f)) {
                        Text("About ARC Features", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("What ARC can do", color = ArcMuted, fontSize = 11.sp)
                    }
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = ArcMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AboutFeaturesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArcSurface,
        titleContentColor = ArcWhite,
        textContentColor = ArcWhite,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("About ARC Features", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Core features:", color = ArcMuted, fontSize = 12.sp)
                listOf(
                    "Universal Downloader",
                    "Direct Downloads",
                    "Multi-Audio",
                    "Resolution Control",
                    "Series & Movies",
                    "India's Got Latent",
                    "Background Downloads",
                    "File Handler",
                    "Torrent & Magnet",
                    "Engine Logs",
                ).forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(ArcWhite)
                        )
                        Text(feature, fontSize = 12.sp)
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
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun OnboardingNotificationDialog(
    onAllow: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = ArcSurface,
        titleContentColor = ArcWhite,
        textContentColor = ArcWhite,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Enable Notifications", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Text(
                "Enable notifications to know when downloads finish and track live progress.",
                color = ArcMuted,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onAllow,
                colors = ButtonDefaults.buttonColors(containerColor = ArcRaised, contentColor = ArcWhite),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Allow", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = ArcMuted)
            }
        }
    )
}

@Composable
private fun OnboardingBatteryDialog(
    onAllow: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = ArcSurface,
        titleContentColor = ArcWhite,
        textContentColor = ArcWhite,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Disable Battery Optimization", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Text(
                "Allow ARC to run unrestricted in the background so downloads never pause.",
                color = ArcMuted,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onAllow,
                colors = ButtonDefaults.buttonColors(containerColor = ArcRaised, contentColor = ArcWhite),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Allow", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = ArcMuted)
            }
        }
    )
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (inputPath.isNotBlank()) {
                                onPathSelected(inputPath)
                            }
                        }
                    ),
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

private data class InstalledPlayerApp(
    val packageName: String,
    val label: String,
)

@Composable
private fun PlayerSelectionDialog(
    currentPlayerPackage: String,
    onPlayerSelected: (pkg: String, name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val installedPlayers = remember {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse("https://example.com/video.mp4"), "video/*")
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        val knownPackages = listOf(
            "org.videolan.vlc",
            "com.mxtech.videoplayer.ad",
            "com.mxtech.videoplayer.pro",
            "is.xyz.mpv",
            "org.courville.nova",
            "com.brouken.player"
        )
        val pkgs = resolveInfos.map { it.activityInfo.packageName }.toMutableSet()
        for (kp in knownPackages) {
            try {
                pm.getPackageInfo(kp, 0)
                pkgs.add(kp)
            } catch (_: Exception) {}
        }
        pkgs.filter { it != context.packageName }.mapNotNull { pkg ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                InstalledPlayerApp(packageName = pkg, label = label)
            } catch (_: Exception) {
                null
            }
        }.sortedBy { it.label }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArcSurface,
        titleContentColor = ArcWhite,
        textContentColor = ArcWhite,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Streaming Player", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val isArcSelected = currentPlayerPackage == "arc_player"
                    PlayerOptionCard(
                        title = "ARC Player (In-App)",
                        subtitle = "Stream directly inside ARC with full playback controls",
                        icon = Icons.Outlined.PlayArrow,
                        isSelected = isArcSelected,
                        onClick = {
                            onPlayerSelected("arc_player", "ARC Player")
                            onDismiss()
                        }
                    )
                }

                item {
                    val isChooserSelected = currentPlayerPackage == "system_chooser"
                    PlayerOptionCard(
                        title = "System Chooser",
                        subtitle = "Open Android app picker every time",
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        isSelected = isChooserSelected,
                        onClick = {
                            onPlayerSelected("system_chooser", "System Chooser")
                            onDismiss()
                        }
                    )
                }

                if (installedPlayers.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Installed Player Apps",
                            color = ArcMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(installedPlayers) { pApp ->
                        val isAppSelected = currentPlayerPackage == pApp.packageName
                        PlayerOptionCard(
                            title = pApp.label,
                            subtitle = pApp.packageName,
                            icon = Icons.Outlined.Movie,
                            isSelected = isAppSelected,
                            onClick = {
                                onPlayerSelected(pApp.packageName, pApp.label)
                                onDismiss()
                            }
                        )
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
        }
    )
}

@Composable
private fun PlayerOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ArcRaised else ArcSurface,
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isSelected) ArcWhite else Color(0xFF333333)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) ArcWhite else ArcMuted,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) ArcWhite else ArcMuted,
                    fontSize = 13.sp,
                )
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = ArcMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isSelected) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = ArcWhite, modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun formatPlayerDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000L
    val hours = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hours > 0L) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, mins, secs)
    } else {
        String.format(java.util.Locale.US, "%d:%02d", mins, secs)
    }
}

private fun toPlayerMediaUri(raw: String): Uri {
    val trimmed = raw.trim()
    return if (trimmed.startsWith("/")) Uri.fromFile(File(trimmed)) else Uri.parse(trimmed)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArcPlayerScreen(
    streamTarget: PlayerStreamTarget,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window

    var isFullscreen by remember(streamTarget) { mutableStateOf(!streamTarget.isAudioOnlyStream) }
    var showControls by remember(streamTarget) { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_BUFFERING) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    var usingFallback by remember(streamTarget) { mutableStateOf(false) }
    var retryCounter by remember(streamTarget) { mutableIntStateOf(0) }
    var playerErrorText by remember(streamTarget) { mutableStateOf<String?>(null) }

    val originalOrientation = remember(activity) {
        activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    val applySystemBars: (Boolean) -> Unit = remember(activity, dialogWindow) {
        { fullscreenActive: Boolean ->
            listOfNotNull(dialogWindow, activity?.window).forEach { win ->
                val controller = WindowCompat.getInsetsController(win, win.decorView)
                if (fullscreenActive) {
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val lp = win.attributes
                        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        win.attributes = lp
                    }
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                    win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }
    }

    LaunchedEffect(isFullscreen) {
        activity?.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            originalOrientation
        }
        applySystemBars(isFullscreen)
    }

    DisposableEffect(activity, dialogWindow) {
        onDispose {
            activity?.requestedOrientation = originalOrientation
            listOfNotNull(dialogWindow, activity?.window).forEach { win ->
                WindowCompat.getInsetsController(win, win.decorView).show(WindowInsetsCompat.Type.systemBars())
                win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    val primaryUrl = streamTarget.url
    val isOkCdn = remember(streamTarget) {
        primaryUrl.contains("okcdn", ignoreCase = true) ||
            streamTarget.videoUrl.contains("okcdn", ignoreCase = true) ||
            streamTarget.fallbackUrl.contains("okcdn", ignoreCase = true)
    }
    val cleanHeaders = remember(streamTarget) {
        streamTarget.headers.toMutableMap().apply {
            if (isOkCdn) {
                remove("Origin")
                remove("Referer")
                remove("User-Agent")
                remove("user-agent")
                remove("origin")
                remove("referer")
            }
        }
    }
    val effFallbackUrl = remember(streamTarget) {
        streamTarget.fallbackUrl.ifBlank {
            if (streamTarget.videoUrl.isNotBlank() && primaryUrl.isNotBlank() && primaryUrl != streamTarget.videoUrl) {
                primaryUrl
            } else {
                ""
            }
        }
    }

    var resolvedPlayUrl by remember(streamTarget) { mutableStateOf(primaryUrl) }

    LaunchedEffect(streamTarget) {
        usingFallback = false
        playerErrorText = null
        Log.i(
            "ArcPlayer",
            "Format choice -> title='${streamTarget.title}', videoFmt='${streamTarget.videoFormat.ifBlank { "auto/muxed" }}', audioFmt='${streamTarget.audioFormat.ifBlank { "embedded" }}', fallbackFmt='${streamTarget.fallbackFormat.ifBlank { "none" }}', audioOnly=${streamTarget.isAudioOnlyStream}"
        )
        if (primaryUrl.isNotBlank()) {
            val isDirectPlayable = primaryUrl.contains("googlevideo.com") ||
                primaryUrl.startsWith("file://") ||
                primaryUrl.startsWith("/") ||
                (!primaryUrl.contains("hakunaymatata") && !primaryUrl.contains("mzfi.me"))

            if (!isDirectPlayable && !primaryUrl.contains("127.0.0.1")) {
                val pPayload = JSONObject().apply {
                    put("action", "stream_proxy")
                    put("url", primaryUrl)
                    val hObj = JSONObject()
                    cleanHeaders.forEach { (k, v) -> hObj.put(k, v) }
                    put("headers", hObj)
                }
                val pRes = withContext(Dispatchers.IO) {
                    runCatching { EmbeddedEngine.execute(context, pPayload) }.getOrNull()
                }
                val px = pRes?.optJSONObject("result")?.optString("proxy_url")
                if (!px.isNullOrBlank()) {
                    resolvedPlayUrl = px
                }
            }
        }
    }

    val dataSourceFactory = remember(context, streamTarget) {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .apply {
                if (!isOkCdn && cleanHeaders.keys.none { it.equals("user-agent", ignoreCase = true) }) {
                    setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                }
                if (cleanHeaders.isNotEmpty()) {
                    setDefaultRequestProperties(cleanHeaders)
                }
            }
        DefaultDataSource.Factory(context, httpFactory)
    }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    fun buildMediaSource(rawUrl: String, preferDirectExtractor: Boolean): MediaSource {
        val uri = toPlayerMediaUri(rawUrl)
        val item = MediaItem.fromUri(uri)
        val isHls = rawUrl.contains(".m3u8", ignoreCase = true) ||
            rawUrl.contains("manifest/hls_playlist", ignoreCase = true) ||
            rawUrl.contains("hls_playlist", ignoreCase = true)
        return when {
            isHls -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(item)
            preferDirectExtractor -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(item)
            else -> DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory).createMediaSource(item)
        }
    }

    DisposableEffect(exoPlayer, streamTarget) {
        val listener = object : Player.Listener {
            private fun inspectTracks(tag: String, tracks: Tracks) {
                var selectedVideo = false
                var selectedAudio = false
                val summaryParts = mutableListOf<String>()
                for (group in tracks.groups) {
                    val kind = when (group.type) {
                        C.TRACK_TYPE_VIDEO -> "video"
                        C.TRACK_TYPE_AUDIO -> "audio"
                        C.TRACK_TYPE_TEXT -> "text"
                        else -> "other(${group.type})"
                    }
                    if (group.type == C.TRACK_TYPE_VIDEO && group.isSelected) selectedVideo = true
                    if (group.type == C.TRACK_TYPE_AUDIO && group.isSelected) selectedAudio = true
                    for (idx in 0 until group.length) {
                        val fmt = group.getTrackFormat(idx)
                        summaryParts.add("$kind[sel=${group.isTrackSelected(idx)},sup=${group.isTrackSupported(idx)},mime=${fmt.sampleMimeType},codecs=${fmt.codecs},${fmt.width}x${fmt.height}]")
                    }
                }
                Log.i(
                    "ArcPlayer",
                    "$tag: selectedVideo=$selectedVideo selectedAudio=$selectedAudio usingFallback=$usingFallback tracks=${summaryParts.joinToString("; ")}"
                )
                val expectVideo = !streamTarget.isAudioOnlyStream
                val expectAudio = streamTarget.audioUrl.isNotBlank() || streamTarget.isAudioOnlyStream
                val missingTrack = (expectVideo && !selectedVideo) || (expectAudio && !selectedAudio)
                if (exoPlayer.playbackState == Player.STATE_READY && missingTrack) {
                    if (!usingFallback && effFallbackUrl.isNotBlank()) {
                        Log.w(
                            "ArcPlayer",
                            "Missing expected track (video=$selectedVideo audio=$selectedAudio); switching to fallback '${streamTarget.fallbackFormat}'"
                        )
                        usingFallback = true
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                inspectTracks("onTracksChanged", tracks)
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    Log.i("ArcPlayer", "Video output dimensions: ${videoSize.width}x${videoSize.height}")
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing && playbackState == Player.STATE_ENDED) {
                    showControls = true
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                val dur = exoPlayer.duration
                if (dur > 0L && dur != C.TIME_UNSET) {
                    durationMs = dur
                }
                if (state == Player.STATE_READY) {
                    inspectTracks("STATE_READY", exoPlayer.currentTracks)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("ArcPlayer", "ExoPlayer error (${error.errorCodeName}) usingFallback=$usingFallback", error)
                if (!usingFallback && effFallbackUrl.isNotBlank()) {
                    usingFallback = true
                } else {
                    playerErrorText = error.errorCodeName.ifBlank { "Playback error" }
                    showControls = true
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(streamTarget, resolvedPlayUrl, usingFallback, retryCounter) {
        val hasSplitTracks = !usingFallback &&
            !streamTarget.isAudioOnlyStream &&
            streamTarget.videoUrl.isNotBlank() &&
            streamTarget.audioUrl.isNotBlank()

        val source = runCatching {
            if (hasSplitTracks) {
                val videoSource = buildMediaSource(streamTarget.videoUrl, preferDirectExtractor = true)
                val audioSource = buildMediaSource(streamTarget.audioUrl, preferDirectExtractor = true)
                Log.i(
                    "ArcPlayer",
                    "Preparing MergingMediaSource -> video=[${streamTarget.videoFormat}] audio=[${streamTarget.audioFormat}]"
                )
                MergingMediaSource(videoSource, audioSource)
            } else {
                val singleUrl = if (usingFallback) effFallbackUrl.ifBlank { resolvedPlayUrl } else resolvedPlayUrl
                if (singleUrl.isBlank()) {
                    null
                } else {
                    Log.i(
                        "ArcPlayer",
                        "Preparing single MediaSource (usingFallback=$usingFallback) -> fmt=[${if (usingFallback) streamTarget.fallbackFormat else streamTarget.videoFormat.ifBlank { "muxed" }}]"
                    )
                    buildMediaSource(singleUrl, preferDirectExtractor = false)
                }
            }
        }.onFailure { err ->
            Log.e("ArcPlayer", "Error preparing media source", err)
            if (!usingFallback && effFallbackUrl.isNotBlank()) {
                usingFallback = true
            } else {
                playerErrorText = err.message ?: "Playback unavailable"
                showControls = true
            }
        }.getOrNull()

        if (source != null) {
            runCatching {
                playerErrorText = null
                exoPlayer.setMediaSource(source)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }.onFailure { err ->
                Log.e("ArcPlayer", "ExoPlayer prepare failed", err)
                if (!usingFallback && effFallbackUrl.isNotBlank()) {
                    usingFallback = true
                } else {
                    playerErrorText = err.message ?: "Playback error"
                    showControls = true
                }
            }
        }
    }

    LaunchedEffect(exoPlayer, isPlaying, playbackState) {
        while (isActive) {
            if (!isScrubbing) {
                val cur = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                if (cur >= 0L) positionMs = cur
                if (dur > 0L && dur != C.TIME_UNSET) durationMs = dur
            }
            delay(250L)
        }
    }

    LaunchedEffect(showControls, isPlaying, isScrubbing, interactionTick, playerErrorText) {
        if (showControls && isPlaying && !isScrubbing && playerErrorText == null) {
            delay(3000L)
            showControls = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                exoPlayer.stop()
                exoPlayer.release()
            }
        }
    }

    BackHandler(onBack = onClose)

    val rootInteraction = remember { MutableInteractionSource() }
    val controlSurfaceColor = ArcSurface.copy(alpha = 0.88f)
    val controlBorder = BorderStroke(1.dp, Color(0xFF2C2C2C))
    val displayPosMs = if (isScrubbing && durationMs > 0L) (scrubFraction * durationMs).toLong() else positionMs
    val sliderFraction = if (durationMs > 0L) {
        (if (isScrubbing) scrubFraction else (positionMs.toFloat() / durationMs.toFloat())).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArcBlack)
            .clickable(interactionSource = rootInteraction, indication = null) {
                showControls = !showControls
                if (showControls) interactionTick++
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.player = exoPlayer
                view.useController = false
                view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            },
            modifier = Modifier.fillMaxSize()
        )

        if (playbackState == Player.STATE_BUFFERING && !showControls && playerErrorText == null) {
            Surface(
                color = controlSurfaceColor,
                shape = CircleShape,
                border = controlBorder,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = ArcWhite,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(220)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
            ) {
                // Top bar: circular back, rounded title pill, circular fullscreen toggle
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        onClick = onClose,
                        color = controlSurfaceColor,
                        shape = CircleShape,
                        border = controlBorder,
                        modifier = Modifier.size(42.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Close player",
                                tint = ArcWhite,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Surface(
                        color = controlSurfaceColor,
                        shape = RoundedCornerShape(22.dp),
                        border = controlBorder,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = streamTarget.title.ifBlank { "ARC Player" },
                            color = ArcWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }

                    Surface(
                        onClick = {
                            isFullscreen = !isFullscreen
                            interactionTick++
                        },
                        color = controlSurfaceColor,
                        shape = CircleShape,
                        border = controlBorder,
                        modifier = Modifier.size(42.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                                contentDescription = if (isFullscreen) "Exit fullscreen" else "Fullscreen",
                                tint = ArcWhite,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                // Center controls or error card
                if (playerErrorText != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ArcSurface),
                        shape = RoundedCornerShape(18.dp),
                        border = controlBorder,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                "Stream unavailable",
                                color = ArcWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                playerErrorText.orEmpty(),
                                color = ArcMuted,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = onClose,
                                    border = controlBorder,
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text("Close", color = ArcWhite, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        usingFallback = false
                                        playerErrorText = null
                                        retryCounter++
                                        interactionTick++
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ArcWhite, contentColor = ArcBlack),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(26.dp),
                    ) {
                        Surface(
                            onClick = {
                                val nextPos = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(nextPos)
                                positionMs = nextPos
                                interactionTick++
                            },
                            color = controlSurfaceColor,
                            shape = CircleShape,
                            border = controlBorder,
                            modifier = Modifier.size(50.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Replay10,
                                    contentDescription = "Rewind 10 seconds",
                                    tint = ArcWhite,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    if (playbackState == Player.STATE_ENDED) {
                                        exoPlayer.seekTo(0L)
                                    }
                                    exoPlayer.play()
                                }
                                interactionTick++
                            },
                            color = ArcWhite,
                            shape = CircleShape,
                            modifier = Modifier.size(68.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (playbackState == Player.STATE_BUFFERING && !isPlaying) {
                                    CircularProgressIndicator(
                                        color = ArcBlack,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(28.dp),
                                    )
                                } else {
                                    Icon(
                                        if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = ArcBlack,
                                        modifier = Modifier.size(34.dp),
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = {
                                val cap = if (durationMs > 0L) durationMs else Long.MAX_VALUE
                                val nextPos = (exoPlayer.currentPosition + 10_000L).coerceAtMost(cap)
                                exoPlayer.seekTo(nextPos)
                                positionMs = nextPos
                                interactionTick++
                            },
                            color = controlSurfaceColor,
                            shape = CircleShape,
                            border = controlBorder,
                            modifier = Modifier.size(50.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Forward10,
                                    contentDescription = "Forward 10 seconds",
                                    tint = ArcWhite,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                    }
                }

                // Bottom bar: thicker M3 slider with time labels above navigation bar inset
                Card(
                    colors = CardDefaults.cardColors(containerColor = controlSurfaceColor),
                    shape = RoundedCornerShape(18.dp),
                    border = controlBorder,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    val sliderInteraction = remember { MutableInteractionSource() }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Slider(
                            value = sliderFraction,
                            onValueChange = { frac ->
                                isScrubbing = true
                                scrubFraction = frac.coerceIn(0f, 1f)
                                interactionTick++
                            },
                            onValueChangeFinished = {
                                if (durationMs > 0L) {
                                    val seekPos = (scrubFraction * durationMs).toLong()
                                    exoPlayer.seekTo(seekPos)
                                    positionMs = seekPos
                                }
                                isScrubbing = false
                                interactionTick++
                            },
                            valueRange = 0f..1f,
                            interactionSource = sliderInteraction,
                            thumb = {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(ArcWhite, CircleShape)
                                )
                            },
                            track = { sliderState ->
                                val activeFraction = ((sliderState.value - sliderState.valueRange.start) /
                                    (sliderState.valueRange.endInclusive - sliderState.valueRange.start).coerceAtLeast(0.0001f)).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFF3A3A3A))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(activeFraction)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(ArcWhite)
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = formatPlayerDuration(displayPosMs),
                                color = ArcWhite,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = formatPlayerDuration(durationMs),
                                color = ArcMuted,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
    if (content.isBlank()) return null
    return try {
        val hints = java.util.EnumMap<com.google.zxing.EncodeHintType, Any>(com.google.zxing.EncodeHintType::class.java).apply {
            put(com.google.zxing.EncodeHintType.MARGIN, 1)
            put(com.google.zxing.EncodeHintType.CHARACTER_SET, "UTF-8")
        }
        val bitMatrix = com.google.zxing.qrcode.QRCodeWriter().encode(
            content,
            com.google.zxing.BarcodeFormat.QR_CODE,
            sizePx,
            sizePx,
            hints
        )
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun GoogleAccountInfoDialog(
    isConnected: Boolean,
    accountEmail: String,
    onLoginClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArcSurface,
        titleContentColor = ArcWhite,
        textContentColor = ArcWhite,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.AccountCircle, contentDescription = null, tint = ArcWhite)
                Text("Sign in only if needed.", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "ARC downloads YouTube without login. Sign in only if YouTube blocks a request with a bot-check or 403 error. Your session stays on this device. Nothing is uploaded.",
                    fontSize = 12.sp,
                    color = ArcWhite,
                    lineHeight = 16.sp
                )
                Surface(
                    color = Color(0xFF1E1711),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF4A3015))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Outlined.WarningAmber,
                            contentDescription = "Warning",
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Use a secondary Google account, not your primary one.",
                            fontSize = 11.sp,
                            color = Color(0xFFFFD59E),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!isConnected) {
                Button(
                    onClick = {
                        onDismiss()
                        onLoginClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArcWhite, contentColor = ArcBlack),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Done", color = ArcWhite, fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            if (!isConnected) {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = ArcMuted, fontSize = 12.sp)
                }
            }
        }
    )
}

@Composable
private fun GoogleLoginDialog(
    loginHint: String = "",
    onSuccess: (email: String, cookies: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(18.dp),
            color = ArcSurface,
            border = BorderStroke(1.dp, Color(0xFF2A2A2A))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Google Sign-In", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ArcWhite)
                        Text("Connect YouTube for authenticated streaming", fontSize = 11.sp, color = ArcMuted)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = ArcMuted)
                    }
                }
                HorizontalDivider(color = Color(0xFF222222), thickness = 1.dp)

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ArcWhite, trackColor = ArcSurface)
                }

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                userAgentString = "Mozilla/5.0 (Linux; Android 14; Redmi Pad Pro 5G) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
                            }
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    val cookieManager = CookieManager.getInstance()
                                    val ytCookies = cookieManager.getCookie("https://www.youtube.com") ?: ""
                                    val googleCookies = cookieManager.getCookie("https://accounts.google.com") ?: ""
                                    val combinedCookies = buildString {
                                        if (ytCookies.isNotBlank()) append(ytCookies)
                                        if (googleCookies.isNotBlank()) {
                                            if (isNotEmpty() && !endsWith("; ")) append("; ")
                                            append(googleCookies)
                                        }
                                    }

                                    if (combinedCookies.contains("LOGIN_INFO") || (combinedCookies.contains("SAPISID") && combinedCookies.contains("SSID")) || combinedCookies.contains("__Secure-3PSID")) {
                                        view?.evaluateJavascript("(function() { try { var el = document.querySelector('div[data-email], [aria-label*=\"@\"]'); return el ? (el.getAttribute('data-email') || el.getAttribute('aria-label')) : ''; } catch(e) { return ''; } })()") { evalRes ->
                                            val detectedEmail = evalRes?.replace("\"", "")?.trim()?.takeIf { it.contains("@") } ?: "Google Account"
                                            val finalEmail = if (loginHint.isNotBlank()) loginHint else detectedEmail
                                            onSuccess(finalEmail, combinedCookies)
                                        }
                                    }
                                }
                            }
                            val encodedHint = if (loginHint.isNotBlank()) java.net.URLEncoder.encode(loginHint, "UTF-8") else ""
                            val startUrl = if (encodedHint.isNotBlank()) {
                                "https://accounts.google.com/ServiceLogin?service=youtube&continue=https://www.youtube.com/&login_hint=$encodedHint&Email=$encodedHint"
                            } else {
                                "https://accounts.google.com/ServiceLogin?service=youtube&continue=https://www.youtube.com/"
                            }
                            loadUrl(startUrl)
                        }
                    }
                )
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
            val ext = job.path.substringAfterLast('.', "").lowercase()
            val isMedia = ext in listOf("mp4", "mkv", "webm", "avi", "mov", "ts", "mp3", "m4a", "aac", "flac", "ogg", "wav")
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
                    MediaGlyph(if (job.status == "Complete" && isMedia) Icons.Outlined.PlayArrow else Icons.Outlined.FileDownload, Modifier.size(34.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(job.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val rowSrc = if (job.source.isNotBlank() &&
                        !job.source.equals("NetMirror", true) &&
                        !job.source.equals("Stream", true) &&
                        !job.source.equals("Direct file", true) &&
                        !job.source.equals("ARC", true) &&
                        !job.detail.equals("File", true) &&
                        !job.detail.equals("IGL · ARC", true)) " · ${job.source}" else ""
                    Text("${job.detail}$rowSrc", color = ArcMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    if (job.status == "Muxing") "Muxing ${job.progress.toInt()}%" else job.status,
                    color = when (job.status) {
                        "Complete" -> ArcWhite
                        "Downloading", "Muxing" -> ArcWhite
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
                        Icon(if (isMedia) Icons.Outlined.PlayArrow else Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isMedia) "Play" else "Open File", fontSize = 12.sp)
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
private fun EmptyState(title: String, detail: String, icon: ImageVector = Icons.Outlined.Link) {
    Card(colors = CardDefaults.cardColors(containerColor = ArcSurface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            MediaGlyph(icon, Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(detail, color = ArcMuted, fontSize = 12.sp)
        }
    }
}
