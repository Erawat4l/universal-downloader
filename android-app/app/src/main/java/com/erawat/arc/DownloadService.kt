package com.erawat.arc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class DownloadService : Service() {

    companion object {
        const val CHANNEL_ACTIVE_ID = "arc_download_channel_active"
        const val CHANNEL_COMPLETE_ID = "arc_downloads"
        const val NOTIF_ID = 8888

        const val ACTION_START_DOWNLOAD = "com.erawat.arc.START_DOWNLOAD"
        const val ACTION_PAUSE_DOWNLOAD = "com.erawat.arc.PAUSE_DOWNLOAD"
        const val ACTION_RESUME_DOWNLOAD = "com.erawat.arc.RESUME_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.erawat.arc.CANCEL_DOWNLOAD"
        const val ACTION_STOP_SERVICE = "com.erawat.arc.STOP_SERVICE"

        const val EXTRA_JOB_ID = "job_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_PAYLOAD = "payload"

        fun startDownload(context: Context, job: DownloadJob, payload: JSONObject) {
            DownloadManager.init(context)
            DownloadManager.addJob(context, job)

            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_JOB_ID, job.id)
                putExtra(EXTRA_TITLE, job.title)
                putExtra(EXTRA_PAYLOAD, payload.toString())
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun pause(context: Context, jobId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_PAUSE_DOWNLOAD
                putExtra(EXTRA_JOB_ID, jobId)
            }
            try { context.startService(intent) } catch (_: Exception) {}
        }

        fun resume(context: Context, jobId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_RESUME_DOWNLOAD
                putExtra(EXTRA_JOB_ID, jobId)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun cancel(context: Context, jobId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra(EXTRA_JOB_ID, jobId)
            }
            try { context.startService(intent) } catch (_: Exception) {}
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private val activeJobIds = ConcurrentHashMap.newKeySet<String>()
    private var pollingJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        DownloadManager.init(applicationContext)
        createNotificationChannels()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START_DOWNLOAD -> {
                val jobId = intent.getStringExtra(EXTRA_JOB_ID).orEmpty()
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
                val payloadStr = intent.getStringExtra(EXTRA_PAYLOAD).orEmpty()

                if (jobId.isNotBlank() && payloadStr.isNotBlank()) {
                    activeJobIds.add(jobId)
                    ensureForeground(title.ifBlank { "Download" }, 0, "Starting...")
                    acquireWakeLock()
                    startPollingProgress()

                    serviceScope.launch {
                        executeDownload(jobId, title, payloadStr)
                    }
                }
            }
            ACTION_PAUSE_DOWNLOAD -> {
                val jobId = intent.getStringExtra(EXTRA_JOB_ID).orEmpty()
                if (jobId.isNotBlank()) {
                    EmbeddedEngine.pause(jobId)
                    DownloadManager.markPaused(applicationContext, jobId)
                    activeJobIds.remove(jobId)
                    checkIfAllDone()
                }
            }
            ACTION_RESUME_DOWNLOAD -> {
                val jobId = intent.getStringExtra(EXTRA_JOB_ID).orEmpty()
                if (jobId.isNotBlank()) {
                    activeJobIds.add(jobId)
                    EmbeddedEngine.resume(jobId)
                    DownloadManager.markResumed(applicationContext, jobId)
                    acquireWakeLock()
                    val job = DownloadManager.getJobs().firstOrNull { it.id == jobId }
                    ensureForeground(job?.title ?: "Download", job?.progress?.toInt() ?: 0, "Resuming...")
                    startPollingProgress()
                }
            }
            ACTION_CANCEL_DOWNLOAD -> {
                val jobId = intent.getStringExtra(EXTRA_JOB_ID).orEmpty()
                if (jobId.isNotBlank()) {
                    EmbeddedEngine.cancel(jobId)
                    DownloadManager.markCancelled(applicationContext, jobId)
                    activeJobIds.remove(jobId)
                    checkIfAllDone()
                }
            }
            ACTION_STOP_SERVICE -> {
                cleanupAndStop()
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun executeDownload(jobId: String, title: String, payloadStr: String) {
        val response = withContext(Dispatchers.IO) {
            runCatching {
                val payload = JSONObject(payloadStr)
                EmbeddedEngine.execute(applicationContext, payload)
            }.getOrElse { error ->
                JSONObject().apply {
                    put("ok", false)
                    put("error", error.message ?: "The engine could not start.")
                }
            }
        }

        val isOk = response.optBoolean("ok", false)
        val data = response.optJSONObject("result")
        val isCancelled = data?.optBoolean("cancelled", false) == true ||
                data?.optString("status") == "Cancelled" ||
                response.optBoolean("cancelled", false)
        var path = data?.optString("path").orEmpty()
        if (path.isBlank()) {
            path = response.optString("path").orEmpty()
        }

        val existingJob = DownloadManager.getJobs().firstOrNull { it.id == jobId }
        val checkPath = if (path.isNotBlank()) path else existingJob?.path.orEmpty()
        val fileActuallyExists = checkPath.isNotBlank() && runCatching {
            val f = File(checkPath)
            f.exists() && f.length() > 0
        }.getOrDefault(false)

        if (fileActuallyExists) {
            DownloadManager.markComplete(applicationContext, jobId, checkPath)
            postCompletionNotification(title.ifBlank { "Download" }, checkPath)
        } else if (!isOk || isCancelled || path.isBlank()) {
            if (isCancelled) {
                DownloadManager.markCancelled(applicationContext, jobId)
            } else {
                val errMsg = response.optString("error", "Download failed.")
                val engineLogs = response.optString("logs", "").ifBlank { errMsg }
                DownloadManager.markFailed(applicationContext, jobId, errMsg, engineLogs)
            }
        } else {
            DownloadManager.markComplete(applicationContext, jobId, path)
            postCompletionNotification(title.ifBlank { "Download" }, path)
        }

        activeJobIds.remove(jobId)
        checkIfAllDone()
    }

    private fun startPollingProgress() {
        if (pollingJob?.isActive == true) return
        pollingJob = serviceScope.launch {
            while (isActive && activeJobIds.isNotEmpty()) {
                delay(400)
                val progressObj = withContext(Dispatchers.IO) {
                    EmbeddedEngine.getProgress()
                }
                if (progressObj.length() > 0) {
                    DownloadManager.updateProgress(progressObj)

                    val jobs = DownloadManager.getJobs()
                    val activeJob = jobs.firstOrNull { it.id in activeJobIds && (it.status == "Downloading" || it.status == "Muxing") }
                    if (activeJob != null) {
                        val pct = activeJob.progress.toInt().coerceIn(0, 100)
                        val text = if (activeJob.status == "Muxing") {
                            "Muxing media: ${pct}%"
                        } else {
                            "${activeJob.downloaded} / ${activeJob.total} · ${activeJob.speed}"
                        }
                        updateOngoingNotification(activeJob.title, pct, text)
                    }
                }
            }
        }
    }

    private fun checkIfAllDone() {
        if (activeJobIds.isEmpty()) {
            pollingJob?.cancel()
            pollingJob = null
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ARC:DownloadWakeLock")
        }
        wakeLock?.let {
            if (!it.isHeld) {
                it.acquire(6 * 60 * 60 * 1000L)
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                try { it.release() } catch (_: Exception) {}
            }
        }
    }

    private fun ensureForeground(title: String, progress: Int, text: String) {
        val notification = buildOngoingNotification(title, progress, text)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIF_ID, notification)
            }
        } catch (_: Exception) {}
    }

    private fun updateOngoingNotification(title: String, progress: Int, text: String) {
        val notification = buildOngoingNotification(title, progress, text)
        val nm = getSystemService(NotificationManager::class.java)
        nm?.notify(NOTIF_ID, notification)
    }

    private fun buildOngoingNotification(title: String, progress: Int, text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ACTIVE_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun postCompletionNotification(title: String, filePath: String) {
        val prefs = getSharedPreferences("arc_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notifications_enabled", true)) return

        try {
            val pendingIntent = if (filePath.isNotBlank()) {
                val file = File(filePath)
                val uri: Uri = try {
                    FileProvider.getUriForFile(this, "$packageName.provider", file)
                } catch (_: Exception) {
                    Uri.fromFile(file)
                }
                val openIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, if (filePath.endsWith(".mp3") || filePath.endsWith(".m4a")) "audio/*" else "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                PendingIntent.getActivity(
                    this,
                    (System.currentTimeMillis() % 10000).toInt(),
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else null

            val notif = NotificationCompat.Builder(this, CHANNEL_COMPLETE_ID)
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

            val nm = getSystemService(NotificationManager::class.java)
            nm?.notify((System.currentTimeMillis() % 100000).toInt(), notif)
        } catch (_: Exception) {}
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java) ?: return

            val activeChannel = NotificationChannel(
                CHANNEL_ACTIVE_ID,
                "Active Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live background download progress"
                setShowBadge(false)
            }
            nm.createNotificationChannel(activeChannel)

            val completeChannel = NotificationChannel(
                CHANNEL_COMPLETE_ID,
                "ARC Downloads",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Download completion alerts"
                enableVibration(true)
            }
            nm.createNotificationChannel(completeChannel)
        }
    }

    private fun cleanupAndStop() {
        pollingJob?.cancel()
        serviceScope.cancel()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupAndStop()
    }
}
