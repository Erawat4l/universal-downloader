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
}
