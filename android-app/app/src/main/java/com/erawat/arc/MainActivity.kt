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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

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

private data class QueueItem(
    val title: String,
    val source: String,
    val detail: String,
    val progress: Float,
    val complete: Boolean = false,
    val icon: ImageVector = Icons.Outlined.VideoLibrary,
)

private data class MediaTarget(
    val title: String,
    val source: String,
    val detail: String,
    val size: String,
    val description: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArcTheme {
                ArcApp()
            }
        }
    }
}

@Composable
private fun ArcTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
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
        error = Color(0xFFE0E0E0),
        onError = ArcBlack,
    )
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
private fun ArcApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current

    var tab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var analysisOpen by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    var selectedQuality by rememberSaveable { mutableStateOf("1080p") }
    var audioOnly by rememberSaveable { mutableStateOf(false) }
    var target by remember {
        mutableStateOf(
            MediaTarget(
                title = "Afterglow — Official Video",
                source = "YouTube",
                detail = "Video · 08:42",
                size = "412 MB",
                description = "Ready for a clean, one-tap download.",
            ),
        )
    }
    var queue by remember {
        mutableStateOf(
            listOf(
                QueueItem("Lunar Signals — Official Video", "YouTube", "1080p · MP4 · 412 MB", .62f),
                QueueItem("Physics Revision Notes", "SelfStudys", "PDF · 18.4 MB", 1f, true, Icons.Outlined.Description),
            ),
        )
    }
    var update by remember { mutableStateOf(UpdateInfo()) }
    var notifications by rememberSaveable { mutableStateOf(true) }
    var clipboardHelper by rememberSaveable { mutableStateOf(true) }

    fun message(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }

    fun openInput(value: String) {
        input = value
        analysisOpen = false
        tab = AppTab.HOME
    }

    fun analyze(value: String = input) {
        val clean = value.trim()
        if (clean.isEmpty()) {
            message("Paste a link or enter a search.")
            return
        }
        input = clean
        val isTelegram = clean.contains("t.me", ignoreCase = true) || clean.contains("telegram", ignoreCase = true)
        val isPdf = clean.contains(".pdf", ignoreCase = true)
        target = when {
            isTelegram -> MediaTarget("Telegram Media", "Telegram", "Media attachment", "Ready", "Media is ready to inspect.")
            isPdf -> MediaTarget("Study Document", "Document", "PDF", "18.4 MB", "Document is ready to save.")
            clean.startsWith("http", ignoreCase = true) -> MediaTarget("Afterglow — Official Video", "Supported Video", "Video · 08:42", "412 MB", "Ready for a clean, one-tap download.")
            else -> MediaTarget(clean.replaceFirstChar { it.titlecase() }, "Search", "Movie, series, video, or song", "Varies", "Choose a result to continue.")
        }
        analysisOpen = true
    }

    fun download() {
        val launched = TermuxBridge.startDownload(context, input, if (audioOnly) "audio" else selectedQuality)
        queue = listOf(
            QueueItem(
                title = target.title,
                source = target.source,
                detail = "$selectedQuality · ${if (audioOnly) "MP3" else "MP4"} · ${target.size}",
                progress = .02f,
                icon = if (audioOnly) Icons.Outlined.MusicNote else Icons.Outlined.VideoLibrary,
            ),
        ) + queue
        analysisOpen = false
        tab = AppTab.ACTIVITY
        message(if (launched) "Sent to Termux. dl.py is running." else "Queued. Install Termux to run dl.py.")
    }

    fun copyLink() {
        clipboard.setText(androidx.compose.ui.text.AnnotatedString("https://stream.example/arc/demo"))
        message("Direct link copied.")
    }

    fun pasteLink() {
        val clip = clipboard.getText()?.text.orEmpty()
        if (clip.isBlank()) {
            message("Nothing to paste.")
        } else {
            input = clip
            message("Link pasted.")
        }
    }

    LaunchedEffect(Unit) {
        update = GitHubUpdateChecker.check()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ArcBlack,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = ArcSurface) {
                AppTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item && !analysisOpen,
                        onClick = { tab = item; analysisOpen = false },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
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
            when {
                analysisOpen -> AnalyzeScreen(
                    target = target,
                    quality = selectedQuality,
                    audioOnly = audioOnly,
                    onBack = { analysisOpen = false },
                    onQuality = { selectedQuality = it },
                    onAudioOnly = { audioOnly = it },
                    onDownload = ::download,
                    onStream = { message("Player handoff is ready.") },
                    onCopyLink = ::copyLink,
                )
                tab == AppTab.HOME -> HomeScreen(
                    input = input,
                    onInput = { input = it },
                    onAnalyze = ::analyze,
                    onPaste = ::pasteLink,
                    onQuickSearch = { openInput("") },
                    onTelegram = { openInput("https://t.me/") },
                    onLibrary = { tab = AppTab.LIBRARY },
                    queue = queue,
                )
                tab == AppTab.ACTIVITY -> ActivityScreen(queue = queue, onPause = { message("Download paused.") }, onResume = { message("Download resumed.") })
                tab == AppTab.LIBRARY -> LibraryScreen()
                tab == AppTab.SETTINGS -> SettingsScreen(
                    update = update,
                    notifications = notifications,
                    clipboardHelper = clipboardHelper,
                    onNotifications = { notifications = it },
                    onClipboard = { clipboardHelper = it },
                    onCheckUpdates = {
                        scope.launch { update = UpdateInfo(); update = GitHubUpdateChecker.check() }
                    },
                    onOpenRelease = {
                        val release = update.releaseUrl.ifBlank { update.apkUrl }
                        if (release.isNotBlank()) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release)))
                        else message("No signed APK release is available.")
                    },
                    onMessage = ::message,
                )
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
    onQuickSearch: () -> Unit,
    onTelegram: () -> Unit,
    onLibrary: () -> Unit,
    queue: List<QueueItem>,
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
                StatusPill("Connected")
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text("Keep it close.", fontSize = 43.sp, lineHeight = 45.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
            Spacer(Modifier.height(8.dp))
            Text("Fast downloads for links, media, and files.", color = ArcMuted, fontSize = 15.sp)
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onAnalyze() }),
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
        item {
            SectionHeader("Recent Downloads", "See Library", onLibrary)
        }
        items(queue.take(3)) { item ->
            FileRow(item)
        }
    }
}

@Composable
private fun AnalyzeScreen(
    target: MediaTarget,
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
                    Text("Review", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Choose a format and action.", color = ArcMuted, fontSize = 12.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = ArcSurface), shape = RoundedCornerShape(22.dp)) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    MediaGlyph(Icons.Outlined.Movie, Modifier.size(76.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(target.source.uppercase(), color = ArcMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text(target.title, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        Text("${target.detail} · ${target.size}", color = ArcMuted, fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            SectionHeader("Quality", "${quality} selected")
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RepoCapabilities.qualities.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { option ->
                            FilterChip(
                                selected = quality == option,
                                onClick = { onQuality(option) },
                                label = { Text(option) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(selected = !audioOnly, onClick = { onAudioOnly(false) }, label = { Text("Video") }, modifier = Modifier.weight(1f))
                FilterChip(selected = audioOnly, onClick = { onAudioOnly(true) }, label = { Text("Audio only") }, modifier = Modifier.weight(1f))
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
        item {
            Text("Video and audio may be merged into one MP4.", color = ArcSubtle, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ActivityScreen(queue: List<QueueItem>, onPause: () -> Unit, onResume: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Activity", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("${queue.count { !it.complete }} active download", color = ArcMuted, fontSize = 13.sp)
        }
        items(queue) { item ->
            Card(colors = CardDefaults.cardColors(containerColor = ArcSurface), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        MediaGlyph(item.icon, Modifier.size(42.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${item.source} · ${item.detail}", color = ArcMuted, fontSize = 11.sp)
                        }
                        Text(if (item.complete) "Done" else "${(item.progress * 100).toInt()}%", color = ArcMuted, fontSize = 11.sp)
                    }
                    LinearProgressIndicator(
                        progress = item.progress,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = ArcWhite,
                        trackColor = Color(0xFF333333),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!item.complete) {
                            OutlinedButton(onClick = if (item.progress > 0f) onPause else onResume, shape = RoundedCornerShape(12.dp)) {
                                Icon(if (item.progress > 0f) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(5.dp))
                                Text(if (item.progress > 0f) "Pause" else "Resume")
                            }
                        } else {
                            OutlinedButton(onClick = {}, shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                                Spacer(Modifier.width(5.dp))
                                Text("Open")
                            }
                        }
                        IconButton(onClick = {}) { Icon(Icons.Outlined.MoreHoriz, contentDescription = "More") }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen() {
    val files = listOf(
        QueueItem("Lunar Signals — Official Video", "Video", "MP4 · 412 MB", 1f, true, Icons.Outlined.VideoLibrary),
        QueueItem("Physics Revision Notes", "SelfStudys", "PDF · 18.4 MB", 1f, true, Icons.Outlined.Description),
        QueueItem("Midnight Drive — Audio", "YouTube", "MP3 · 7.2 MB", 1f, true, Icons.Outlined.MusicNote),
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Library", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("Files you chose to keep.", color = ArcMuted, fontSize = 13.sp)
        }
        item {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(15.dp),
                placeholder = { Text("Search files") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            )
        }
        items(files) { FileRow(it) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = ArcSurface), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Storage", fontWeight = FontWeight.Bold)
                        Text("438 MB", color = ArcMuted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(progress = .09f, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = ArcWhite, trackColor = Color(0xFF333333))
                }
            }
        }
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("Simple controls. No clutter.", color = ArcMuted, fontSize = 13.sp)
        }
        item {
            GitHubCard(update, onCheckUpdates, onOpenRelease)
        }
        item {
            SettingsGroup("Appearance")
            SettingRow("Dark Theme", "Always on", Icons.Outlined.DarkMode)
            SettingRow("Notifications", "Download progress", Icons.Outlined.NotificationsNone, control = { Switch(checked = notifications, onCheckedChange = onNotifications) })
            SettingsGroup("Downloads")
            SettingRow("Save Location", "Downloads / Universal Downloader", Icons.Outlined.Folder, control = { TextButton(onClick = { onMessage("Folder picker ready for the next build.") }) { Text("Change") } })
            SettingRow("Default Quality", "Ask each time", Icons.Outlined.Tune, control = { TextButton(onClick = { onMessage("Quality picker ready.") }) { Text("Edit") } })
            SettingRow("Clipboard Helper", "Suggest pasted links", Icons.Outlined.ContentPaste, control = { Switch(checked = clipboardHelper, onCheckedChange = onClipboard) })
            SettingsGroup("Accounts And Privacy")
            SettingRow("Cookies And Authentication", "Hidden from logs", Icons.Outlined.Security, control = { TextButton(onClick = { onMessage("Secure credentials manager ready.") }) { Text("Manage") } })
            SettingRow("Telegram", "Optional Termux session", Icons.Outlined.Send, control = { TextButton(onClick = { onMessage("Telegram setup ready.") }) { Text("Set Up") } })
            SettingsGroup("About")
            SettingRow("About Arc", "Universal Downloader frontend", Icons.Outlined.Info, control = { Text("0.1.0", color = ArcMuted, fontSize = 12.sp) })
        }
        item {
            Text("Download only content you can save.", color = ArcSubtle, fontSize = 11.sp)
        }
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
                TextButton(onClick = if (update.appUpdateAvailable) onOpenRelease else onCheck, enabled = !update.loading) {
                    Text(if (update.appUpdateAvailable) "Open Release" else "Check Now")
                }
            }
            if (update.remoteCommit.isNotBlank()) {
                Text("Latest: ${update.remoteCommit} · ${update.commitMessage}", color = ArcSubtle, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
            Text(detail, color = ArcMuted, fontSize = 11.sp)
        }
        if (control != null) control()
    }
}

@Composable
private fun FileRow(item: QueueItem) {
    Card(colors = CardDefaults.cardColors(containerColor = ArcSurface), shape = RoundedCornerShape(17.dp)) {
        Row(modifier = Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            MediaGlyph(item.icon, Modifier.size(40.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.source} · ${item.detail}", color = ArcMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = {}) { Icon(Icons.Outlined.MoreHoriz, contentDescription = "More") }
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
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.FileDownload, contentDescription = null, tint = ArcBlack)
        }
    }
}

@Composable
private fun MediaGlyph(icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = ArcRaised) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = ArcWhite)
        }
    }
}
