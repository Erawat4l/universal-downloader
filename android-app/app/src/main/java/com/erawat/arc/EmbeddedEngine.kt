package com.erawat.arc

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONObject

/**
 * Runs the repository's real Python downloader inside this APK.
 * No external terminal, shell, or companion app is required.
 */
object EmbeddedEngine {
    fun execute(context: Context, request: JSONObject): JSONObject {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context.applicationContext))
        }
        val module = Python.getInstance().getModule("dl")
        val rawResponse = module.callAttr("run_app_json_request_json", request.toString())
        return JSONObject(rawResponse.toString())
    }

    fun getProgress(jobId: String? = null): JSONObject {
        if (!Python.isStarted()) return JSONObject()
        return try {
            val module = Python.getInstance().getModule("dl")
            val raw = module.callAttr("get_download_progress_json", jobId ?: "")
            JSONObject(raw.toString())
        } catch (_: Exception) {
            JSONObject()
        }
    }

    fun cancel(jobId: String) {
        if (!Python.isStarted()) return
        try {
            val module = Python.getInstance().getModule("dl")
            module.callAttr("cancel_download", jobId)
        } catch (_: Exception) {}
    }
}
