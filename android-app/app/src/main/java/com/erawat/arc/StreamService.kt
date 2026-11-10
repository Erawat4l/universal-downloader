package com.erawat.arc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import org.json.JSONObject

/**
 * Foreground Service that keeps the local Python streaming proxy alive in the background
 * when VLC or an external player takes the foreground.
 */
class StreamService : Service() {
    companion object {
        private const val CHANNEL_ID = "arc_stream_channel"
        private const val NOTIF_ID = 8765
        const val ACTION_START = "com.erawat.arc.STREAM_START"
        const val ACTION_STOP = "com.erawat.arc.STREAM_STOP"

        fun start(context: Context, url: String, headers: Map<String, String>) {
            val intent = Intent(context, StreamService::class.java).apply {
                action = ACTION_START
                putExtra("url", url)
                val hObj = JSONObject()
                headers.forEach { (k, v) -> hObj.put(k, v) }
                putExtra("headers", hObj.toString())
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, StreamService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val url = intent?.getStringExtra("url").orEmpty()
        val headersStr = intent?.getStringExtra("headers").orEmpty()
        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_ID, notification)
        }

        // Ensure the local stream proxy is active
        if (url.isNotBlank()) {
            Thread {
                runCatching {
                    val pPayload = JSONObject().apply {
                        put("action", "stream_proxy")
                        put("url", url)
                        if (headersStr.isNotBlank()) {
                            put("headers", JSONObject(headersStr))
                        }
                    }
                    EmbeddedEngine.execute(applicationContext, pPayload)
                }
            }.start()
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ARC Media Stream",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Streaming proxy for VLC & External Media Players"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, StreamService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ARC Media Streaming")
            .setContentText("Relaying direct stream to external player")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(openPending)
            .addAction(0, "Stop Stream", stopPending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
