package com.erawat.arc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
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
)
private data class MediaTarget(
    val title: String,
    val source: String,
    val detail: String,
    val size: String,
    val description: String,
)
private data class DownloadJob(
    val id: String,
    val title: String,
    val source: String,
    val detail: String,
    val status: String,
    val path: String = "",
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

    var tabName by rememberSaveable { mutableStateOf(AppTab.HOME.name) }
    var overlay by rememberSaveable { mutableStateOf("") }
    var input by rememberSaveable { mutableStateOf("") }
    var selectedQuality by rememberSaveable { mutableStateOf("1080p") }
    var audioOnly by rememberSaveable { mutableStateOf(false) }
    var searchLoading by rememberSaveable { mutableStateOf(false) }
    var resolveLoading by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var resolvedUrl by rememberSaveable { mutableStateOf("") }
    var target by remember {
        mutableStateOf(MediaTarget("", "", "", "", ""))
    }
    var hits by remember { mutableStateOf(emptyList<SearchHit>()) }
    var jobs by remember { mutableStateOf(emptyList<DownloadJob>()) }
    var pending by remember { mutableStateOf(emptyMap<String, String>()) }
    var update by remember { mutableStateOf(UpdateInfo()) }
    var notifications by rememberSaveable { mutableStateOf(true) }
    var clipboardHelper by rememberSaveable { mutableStateOf(true) }

    val tab = AppTab.entries.firstOrNull { it.name == tabName } ?: AppTab.HOME

    fun message(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }

    fun submit(payload: JSONObject, type: String): String? {
        val jobId = TermuxBridge.startJsonJob(context, payload)
        if (jobId == null) {
            message("Termux is not available. Install Termux and the CLI first.")
            return null
        }
        pending = pending + (jobId to type)
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
        if (clean.startsWith("http://") || clean.startsWith("https://")) {
            resolveLoading = true
            overlay = "analysis"
            submit(JSONObject().apply {
                put("action", "resolve")
                put("url", clean)
                put("quality", selectedQuality)
            }, "resolve")
        } else {
            query = clean
            hits = emptyList()
            searchLoading = true
            overlay = "search"
            submit(JSONObject().apply {
                put("action", "search")
                put("query", clean)
                put("platform", "netmirror")
            }, "search")
        }
    }

    fun selectSearchResult(hit: SearchHit, audio: AudioChoice) {
        input = audio.url
        query = hit.title
        target = MediaTarget(
            title = hit.title,
            source = hit.source,
            detail = "${hit.type}${if (hit.year.isNotBlank()) " · ${hit.year}" else ""} · ${audio.language}",
            size = "Size varies",
            description = "Real result from the CLI.",
        )
        resolveLoading = true
        overlay = "analysis"
        submit(JSONObject().apply {
            put("action", "resolve")
            put("url", audio.url)
            put("quality", selectedQuality)
        }, "resolve")
    }

    fun download() {
        if (input.isBlank()) {
            message("Analyze a real URL first.")
            return
        }
        val jobId = submit(JSONObject().apply {
            put("action", "download")
            put("url", input)
            put("quality", if (audioOnly) "audio" else selectedQuality)
        }, "download") ?: return
        jobs = listOf(
            DownloadJob(jobId, target.title.ifBlank { input }, target.source.ifBlank { "CLI" }, selectedQuality, "Running in Termux"),
        ) + jobs
        overlay = ""
        tabName = AppTab.ACTIVITY.name
        message("Sent to Termux. dl.py is running.")
    }

    fun stream() {
        if (resolvedUrl.isBlank()) {
            message("Resolve the stream first.")
            return
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resolvedUrl)).apply { type = "video/*" })
        } catch (_: Exception) {
            message("No compatible player is installed.")
        }
    }

    fun copyResolvedLink() {
        if (resolvedUrl.isBlank()) {
            message("Resolve the stream first.")
            return
        }
        clipboard.setText(AnnotatedString(resolvedUrl))
        message("Direct link copied.")
    }

    fun paste() {
        val value = clipboard.getText()?.text.orEmpty()
        if (value.isBlank()) message("Clipboard is empty.") else input = value
    }

    fun handleTermuxResult(result: TermuxResult) {
        val type = pending[result.jobId] ?: return
        pending = pending - result.jobId
        val jsonLine = result.stdout.lineSequence().lastOrNull { it.trim().startsWith("{") }
        val response = runCatching { JSONObject(jsonLine ?: "") }.getOrNull()
        if (response == null) {
            message("The CLI returned no readable result.")
            if (type == "resolve") resolveLoading = false
            if (type == "search") searchLoading = false
            return
        }
        if (!response.optBoolean("ok", false)) {
            val error = response.optString("error", "The CLI request failed.")
            message(error)
            if (type == "resolve") resolveLoading = false
            if (type == "search") searchLoading = false
            if (type == "download") jobs = jobs.map { if (it.id == result.jobId) it.copy(status = "Failed") else it }
            return
        }
        val data = response.optJSONObject("result")
        when (type) {
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
                            source = item.optString("source", "NetMirror"),
                            type = item.optString("type", "MEDIA"),
                            year = item.optString("year", ""),
                            providers = item.optJSONArray("providers")?.let { providers -> (0 until providers.length()).joinToString(" · ") { index -> providers.optString(index) } } ?: "",
                            audio = audios,
                        )
                    }
                }
                hits = resultList
                searchLoading = false
            }
            "resolve" -> {
                val url = data?.optString("url").orEmpty()
                resolvedUrl = url
                target = target.copy(
                    title = data?.optString("title").takeUnless { it.isNullOrBlank() } ?: target.title,
                    source = data?.optString("source").takeUnless { it.isNullOrBlank() } ?: target.source,
                    detail = "${selectedQuality}${if (audioOnly) " · Audio" else " · Video"}",
                )
                resolveLoading = false
                overlay = "analysis"
            }
            "download" -> {
                val path = data?.optString("path").orEmpty()
                jobs = jobs.map { if (it.id == result.jobId) it.copy(status = "Complete", path = path) else it }
                message("Download complete.")
            }
        }
    }

    LaunchedEffect(Unit) {
        update = GitHubUpdateChecker.check()
        TermuxBridge.results.collect { handleTermuxResult(it) }
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
                overlay == "search" -> SearchScreen(query, searchLoading, hits, onBack = ::openHome, onSelect = ::selectSearchResult)
                overlay == "analysis" -> AnalysisScreen(
                    target = target,
                    loading = resolveLoading,
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
                    onQuickSearch = { input = ""; message("Enter a search in the field.") },
                    onTelegram = { input = "https://t.me/" },
                    onLibrary = { navigate(AppTab.LIBRARY) },
                    jobs = jobs,
                )
                tab == AppTab.ACTIVITY -> ActivityScreen(jobs)
                tab == AppTab.LIBRARY -> LibraryScreen(jobs)
                tab == AppTab.SETTINGS -> SettingsScreen(
                    update = update,
                    notifications = notifications,
                    clipboardHelper = clipboardHelper,
                    onNotifications = { notifications = it },
                    onClipboard = { clipboardHelper = it },
                    onCheckUpdates = { scope.launch { update = UpdateInfo(); update = GitHubUpdateChecker.check() } },
                    onOpenRelease = {
                        val url = update.releaseUrl.ifBlank { update.apkUrl }
                        if (url.isNotBlank()) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        else message("No signed APK release is available.")
                    },
                    onMessage = ::message,
                )
            }
        }
    }
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
                    Text("Engine: dl.py", color = ArcMuted, fontSize = 11.sp)
                }
                Spacer(Modifier.weight(1f))
                StatusPill("Termux Bridge")
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text("Keep it close.", fontSize = 43.sp, lineHeight = 45.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
            Spacer(Modifier.height(8.dp))
            Text("Real downloads. No demo data.", color = ArcMuted, fontSize = 15.sp)
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
                QuickChip("Telegram", Icons.Outlined.Send, onTelegram)
            }
        }
        item { SectionHeader("Recent Downloads", "See Library", onLibrary) }
        if (jobs.isEmpty()) {
            item { EmptyState("No Downloads Yet", "Paste a link to start.") }
        } else {
            items(jobs.take(3)) { job -> JobRow(job) }
        }
    }
}

@Composable
private fun SearchScreen(
    query: String,
    loading: Boolean,
    hits: List<SearchHit>,
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
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") }
                Column {
                    Text("Search Results", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${query.replaceFirstChar { it.titlecase() }}", color = ArcMuted, fontSize = 12.sp)
                }
            }
        }
        if (loading) item { EmptyState("Searching…", "The CLI is finding real results.") }
        if (!loading && hits.isEmpty()) item { EmptyState("No Results", "The CLI returned no matches.") }
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
                        hit.audio.forEach { audio ->
                            FilterChip(selected = false, onClick = { onSelect(hit, audio) }, label = { Text(audio.language) })
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
    quality: String,
    audioOnly: Boolean,
    onBack: () -> Unit,
    onQuality: (String) -> Unit,
    onAudioOnly: (Boolean) -> Unit,
    onDownload: () -> Unit,
    onStream: () -> Unit,
    onCopyLink: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") }
                Column {
                    Text(if (loading) "Analyzing" else "Review", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(if (loading) "The CLI is working." else "Choose a format and action.", color = ArcMuted, fontSize = 12.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = ArcSurface), shape = RoundedCornerShape(22.dp)) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    MediaGlyph(Icons.Outlined.Movie, Modifier.size(76.dp))
                    Column(Modifier.weight(1f)) {
                        Text(target.source.uppercase().ifBlank { "CLI" }, color = ArcMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text(target.title.ifBlank { "Resolving link…" }, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        Text(target.detail, color = ArcMuted, fontSize = 11.sp)
                    }
                }
            }
        }
        if (loading) {
            item { EmptyState("Waiting For The CLI", "This screen will update when dl.py returns.") }
        } else {
            item {
                SectionHeader("Quality", "$quality Selected")
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RepoCapabilities.qualities.chunked(5).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { option ->
                                FilterChip(selected = quality == option, onClick = { onQuality(option) }, label = { Text(option) }, modifier = Modifier.weight(1f))
                            }
                            repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
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
                Button(onClick = onDownload, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = ArcWhite, contentColor = ArcBlack)) {
                    Icon(Icons.Outlined.FileDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Download $quality", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onStream, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Outlined.PlayArrow, contentDescription = null); Spacer(Modifier.width(5.dp)); Text("Stream") }
                    OutlinedButton(onClick = onCopyLink, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Outlined.Share, contentDescription = null); Spacer(Modifier.width(5.dp)); Text("Copy Link") }
                }
            }
        }
    }
}

@Composable
private fun ActivityScreen(jobs: List<DownloadJob>) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Activity", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("Live results from dl.py.", color = ArcMuted, fontSize = 13.sp)
        }
        if (jobs.isEmpty()) item { EmptyState("No Activity", "Start a real download from Home.") }
        items(jobs) { job ->
            Card(colors = CardDefaults.cardColors(containerColor = ArcSurface), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        MediaGlyph(if (job.status == "Complete") Icons.Outlined.CheckCircle else Icons.Outlined.FileDownload, Modifier.size(42.dp))
                        Column(Modifier.weight(1f)) {
                            Text(job.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${job.source} · ${job.detail}", color = ArcMuted, fontSize = 11.sp)
                        }
                        Text(job.status, color = ArcMuted, fontSize = 10.sp)
                    }
                    if (job.path.isNotBlank()) Text(job.path, color = ArcSubtle, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (job.status == "Running in Termux") Text("Open Termux to watch the CLI progress.", color = ArcMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(jobs: List<DownloadJob>) {
    val completed = jobs.filter { it.status == "Complete" }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Library", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("Files returned by the CLI.", color = ArcMuted, fontSize = 13.sp)
        }
        if (completed.isEmpty()) item { EmptyState("No Files Yet", "Completed CLI downloads will appear here.") }
        items(completed) { job -> JobRow(job) }
    }
}

@Composable
private fun SettingsScreen(
    update: UpdateInfo,
    notifications: Boolean,
    clipboardHelper: Boolean,
    onNotifications: (Boolean) -> Unit,
    onClipboard: (Boolean) -> Unit,
    onCheckUpdates: () -> Unit,
    onOpenRelease: () -> Unit,
    onMessage: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("Real engine. Simple controls.", color = ArcMuted, fontSize = 13.sp)
        }
        item { GitHubCard(update, onCheckUpdates, onOpenRelease) }
        item {
            SettingsGroup("Engine")
            SettingRow("Termux Bridge", "Runs the repository's dl.py", Icons.Outlined.Send, control = { TextButton(onClick = { onMessage("Termux must be installed with the CLI at ~/universal-downloader/dl.py.") }) { Text("Info") } })
            SettingsGroup("Downloads")
            SettingRow("Save Location", "Controlled by dl.py", Icons.Outlined.Folder, control = { TextButton(onClick = { onMessage("The CLI controls the download folder.") }) { Text("Info") } })
            SettingRow("Default Quality", "Ask each time", Icons.Outlined.Tune, control = { TextButton(onClick = { onMessage("Quality is selected before each request.") }) { Text("Edit") } })
            SettingRow("Clipboard Helper", "Suggest pasted links", Icons.Outlined.ContentPaste, control = { Switch(checked = clipboardHelper, onCheckedChange = onClipboard) })
            SettingsGroup("Notifications")
            SettingRow("Download Notifications", "Android notifications", Icons.Outlined.NotificationsNone, control = { Switch(checked = notifications, onCheckedChange = onNotifications) })
            SettingsGroup("Accounts And Privacy")
            SettingRow("Cookies And Authentication", "Passed to the CLI when configured", Icons.Outlined.Security, control = { TextButton(onClick = { onMessage("Credentials remain in Termux environment variables.") }) { Text("Info") } })
            SettingRow("About Arc", "Universal Downloader frontend", Icons.Outlined.Info, control = { Text("0.1.0", color = ArcMuted, fontSize = 12.sp) })
        }
        item { Text("Download only content you can save.", color = ArcSubtle, fontSize = 11.sp) }
    }
}

@Composable
private fun GitHubCard(update: UpdateInfo, onCheck: () -> Unit, onOpenRelease: () -> Unit) {
    val title = when {
        update.loading -> "Checking GitHub…"
        update.error.isNotBlank() -> "Update Check Unavailable"
        update.appUpdateAvailable -> "App Update Ready"
        update.sourceUpdateAvailable -> "CLI Source Update Found"
        else -> "You’re Up to Date"
    }
    val body = when {
        update.loading -> "Checking the repository."
        update.error.isNotBlank() -> "GitHub is unavailable. Try again later."
        update.appUpdateAvailable -> "Release ${update.releaseTag.ifBlank { "Latest" }} includes an APK."
        update.sourceUpdateAvailable -> "CLI changes are available. A signed APK is still needed."
        else -> "Arc and dl.py track the same repository."
    }
    Card(colors = CardDefaults.cardColors(containerColor = ArcSurface), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                MediaGlyph(if (update.appUpdateAvailable) Icons.Outlined.FileDownload else Icons.Outlined.Refresh, Modifier.size(40.dp))
                Column(Modifier.weight(1f)) {
                    Text("GitHub Sync", color = ArcMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(body, color = ArcMuted, fontSize = 11.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Erawat4l/universal-downloader", color = ArcSubtle, fontSize = 10.sp)
                TextButton(onClick = if (update.appUpdateAvailable) onOpenRelease else onCheck, enabled = !update.loading) { Text(if (update.appUpdateAvailable) "Open Release" else "Check Now") }
            }
            if (update.remoteCommit.isNotBlank()) Text("Latest: ${update.remoteCommit} · ${update.commitMessage}", color = ArcSubtle, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
            Text(detail, color = ArcMuted, fontSize = 11.sp)
        }
        if (control != null) control()
    }
}

@Composable
private fun JobRow(job: DownloadJob) {
    Card(colors = CardDefaults.cardColors(containerColor = ArcSurface), shape = RoundedCornerShape(17.dp)) {
        Row(modifier = Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            MediaGlyph(if (job.status == "Complete") Icons.Outlined.CheckCircle else Icons.Outlined.FileDownload, Modifier.size(40.dp))
            Column(Modifier.weight(1f)) {
                Text(job.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${job.source} · ${job.detail}", color = ArcMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = {}) { Icon(Icons.Outlined.MoreHoriz, contentDescription = "More") }
        }
    }
}

@Composable
private fun QuickChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) }, leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }, colors = AssistChipDefaults.assistChipColors(containerColor = ArcSurface), border = BorderStroke(1.dp, Color(0xFF303030)))
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
