package com.erawat.arc

import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Bridges the Android UI to the existing Python CLI when Termux is installed.
 * The APK stays useful without Termux; in that case the UI reports the setup step.
 */
object TermuxBridge {
    private const val TERMUX_PACKAGE = "com.termux"
    private const val PYTHON_PATH = "/data/data/com.termux/files/usr/bin/python"
    private const val CLI_PATH = "/data/data/com.termux/files/home/universal-downloader/dl.py"
    private const val WORKING_DIRECTORY = "/data/data/com.termux/files/home/universal-downloader"

    fun startDownload(context: Context, url: String, quality: String): Boolean {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false

        val command = arrayOf(CLI_PATH, url, "--quality", quality)
        val intent = Intent().apply {
            action = "com.termux.RUN_COMMAND"
            component = ComponentName(TERMUX_PACKAGE, "com.termux.app.RunCommandService")
            putExtra("com.termux.RUN_COMMAND_PATH", PYTHON_PATH)
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", command)
            putExtra("com.termux.RUN_COMMAND_WORKDIR", WORKING_DIRECTORY)
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
        }

        return try {
            context.startService(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
