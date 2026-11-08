package com.erawat.arc

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.flow.MutableSharedFlow
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID

/** Real bridge to the repository's Python CLI through Termux:API's RUN_COMMAND service. */
data class TermuxResult(
    val jobId: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
)

object TermuxBridge {
    private const val TERMUX_PACKAGE = "com.termux"
    private const val PYTHON_PATH = "/data/data/com.termux/files/usr/bin/python"
    private const val CLI_PATH = "/data/data/com.termux/files/home/universal-downloader/dl.py"
    private const val WORKING_DIRECTORY = "/data/data/com.termux/files/home/universal-downloader"
    const val EXTRA_JOB_ID = "arc.termux.job_id"

    val results = MutableSharedFlow<TermuxResult>(extraBufferCapacity = 24)

    fun startJsonJob(context: Context, payload: JSONObject): String? {
        val jobId = UUID.randomUUID().toString()
        val encoded = Base64.encodeToString(
            payload.toString().toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP,
        )
        val callback = Intent(context, TermuxResultReceiver::class.java).apply {
            putExtra(EXTRA_JOB_ID, jobId)
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            jobId.hashCode(),
            callback,
            pendingFlags,
        )
        val command = arrayOf(CLI_PATH, "--app-json-b64", encoded)
        val intent = Intent().apply {
            action = "com.termux.RUN_COMMAND"
            component = ComponentName(TERMUX_PACKAGE, "com.termux.app.RunCommandService")
            putExtra("com.termux.RUN_COMMAND_PATH", PYTHON_PATH)
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", command)
            putExtra("com.termux.RUN_COMMAND_WORKDIR", WORKING_DIRECTORY)
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", pendingIntent)
        }
        return try {
            context.startService(intent)
            jobId
        } catch (_: Exception) {
            null
        }
    }

    fun startDownload(context: Context, url: String, quality: String): String? {
        return startJsonJob(context, JSONObject().apply {
            put("action", "download")
            put("url", url)
            put("quality", quality)
        })
    }

    fun startResolve(context: Context, url: String, quality: String): String? {
        return startJsonJob(context, JSONObject().apply {
            put("action", "resolve")
            put("url", url)
            put("quality", quality)
        })
    }

    fun startSearch(context: Context, query: String, platform: String = "netmirror"): String? {
        return startJsonJob(context, JSONObject().apply {
            put("action", "search")
            put("query", query)
            put("platform", platform)
        })
    }
}

class TermuxResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val jobId = intent.getStringExtra(TermuxBridge.EXTRA_JOB_ID).orEmpty()
        val stdout = intent.getStringExtra("com.termux.RUN_COMMAND_RESULT")
            ?: intent.getStringExtra("com.termux.RUN_COMMAND_RESULT_STDOUT")
            ?: intent.getStringExtra("stdout")
            ?: ""
        val stderr = intent.getStringExtra("com.termux.RUN_COMMAND_RESULT_STDERR")
            ?: intent.getStringExtra("stderr")
            ?: ""
        val exitCode = intent.getIntExtra("com.termux.RUN_COMMAND_RESULT_EXIT_CODE", -1)
        TermuxBridge.results.tryEmit(TermuxResult(jobId, stdout, stderr, exitCode))
    }
}
