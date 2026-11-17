package com.erawat.arc

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object DownloadManager {
    private const val PREFS_NAME = "arc_download_manager"
    private const val KEY_JOBS = "saved_jobs"

    private val _jobsState = mutableStateOf<List<DownloadJob>>(emptyList())
    val jobsState: State<List<DownloadJob>> = _jobsState

    private var isInitialized = false

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        loadJobs(context)
    }

    @Synchronized
    fun getJobs(): List<DownloadJob> = _jobsState.value

    @Synchronized
    fun addJob(context: Context, job: DownloadJob) {
        val current = _jobsState.value.filterNot { it.id == job.id }
        _jobsState.value = listOf(job) + current
        saveJobs(context)
    }

    @Synchronized
    fun updateJob(context: Context?, jobId: String, transform: (DownloadJob) -> DownloadJob) {
        val current = _jobsState.value
        val updated = current.map { if (it.id == jobId) transform(it) else it }
        _jobsState.value = updated
        context?.let { saveJobs(it) }
    }

    @Synchronized
    fun updateProgress(progressObj: JSONObject) {
        if (progressObj.length() == 0) return
        val current = _jobsState.value
        var changed = false
        val updated = current.map { job ->
            val p = progressObj.optJSONObject(job.id)
            if (p != null) {
                changed = true
                val newStatus = p.optString("status", job.status)
                val effectiveStatus = when {
                    job.status == "Complete" || job.status == "Cancelled" -> job.status
                    newStatus == "Cancelled" || job.status == "Cancelled" -> "Cancelled"
                    newStatus == "Paused" || job.status == "Paused" -> "Paused"
                    newStatus == "Complete" && p.optString("path").isNotBlank() -> "Complete"
                    newStatus == "Muxing" -> "Muxing"
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
                    path = path
                )
            } else {
                job
            }
        }
        if (changed) {
            _jobsState.value = updated
        }
    }

    @Synchronized
    fun markComplete(context: Context, jobId: String, path: String) {
        updateJob(context, jobId) { it.copy(status = "Complete", path = path, progress = 100f, speed = "--", eta = "--") }
    }

    @Synchronized
    fun markFailed(context: Context, jobId: String, error: String) {
        updateJob(context, jobId) { it.copy(status = "Failed", speed = "--", eta = "--") }
    }

    @Synchronized
    fun markCancelled(context: Context, jobId: String) {
        updateJob(context, jobId) { it.copy(status = "Cancelled", speed = "--", eta = "--") }
    }

    @Synchronized
    fun markPaused(context: Context, jobId: String) {
        updateJob(context, jobId) { it.copy(status = "Paused", speed = "--", eta = "--") }
    }

    @Synchronized
    fun markResumed(context: Context, jobId: String) {
        updateJob(context, jobId) { it.copy(status = "Downloading") }
    }

    @Synchronized
    fun removeJob(context: Context, jobId: String, removeFile: Boolean) {
        val job = _jobsState.value.firstOrNull { it.id == jobId }
        if (job != null && removeFile && job.path.isNotBlank()) {
            runCatching { File(job.path).delete() }
        }
        _jobsState.value = _jobsState.value.filterNot { it.id == jobId }
        saveJobs(context)
    }

    @Synchronized
    fun saveJobs(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val arr = JSONArray()
            _jobsState.value.forEach { job ->
                arr.put(job.toJson())
            }
            prefs.edit().putString(KEY_JOBS, arr.toString()).apply()
        } catch (_: Exception) {}
    }

    @Synchronized
    private fun loadJobs(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_JOBS, null) ?: return
            val arr = JSONArray(raw)
            val list = mutableListOf<DownloadJob>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val job = DownloadJob.fromJson(obj)
                val fixedJob = if (job.status == "Downloading") {
                    job.copy(status = "Paused", speed = "--")
                } else job
                list.add(fixedJob)
            }
            _jobsState.value = list
        } catch (_: Exception) {}
    }
}
