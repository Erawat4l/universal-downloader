package com.erawat.arc

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer

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

    fun pause(jobId: String) {
        if (!Python.isStarted()) return
        try {
            val module = Python.getInstance().getModule("dl")
            module.callAttr("pause_download", jobId)
        } catch (_: Exception) {}
    }

    fun resume(jobId: String) {
        if (!Python.isStarted()) return
        try {
            val module = Python.getInstance().getModule("dl")
            module.callAttr("resume_download", jobId)
        } catch (_: Exception) {}
    }

    fun mux(videoPath: String, audioPath: String, outputPath: String): Boolean {
        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        return try {
            val vFile = File(videoPath)
            val aFile = File(audioPath)
            if (!vFile.exists() || !aFile.exists()) return false

            val outFile = File(outputPath)
            outFile.parentFile?.mkdirs()
            if (outFile.exists()) outFile.delete()

            videoExtractor = MediaExtractor().apply { setDataSource(videoPath) }
            audioExtractor = MediaExtractor().apply { setDataSource(audioPath) }

            val outFormat = if (outputPath.endsWith(".webm", ignoreCase = true)) {
                MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
            } else {
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            }
            muxer = MediaMuxer(outputPath, outFormat)

            var videoTrackIndex = -1
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoExtractor.selectTrack(i)
                    videoTrackIndex = muxer.addTrack(format)
                    break
                }
            }

            var audioTrackIndex = -1
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioExtractor.selectTrack(i)
                    audioTrackIndex = muxer.addTrack(format)
                    break
                }
            }

            if (videoTrackIndex == -1 || audioTrackIndex == -1) {
                return false
            }

            muxer.start()
            val maxBufferSize = 2 * 1024 * 1024
            val videoBuffer = ByteBuffer.allocateDirect(maxBufferSize)
            val audioBuffer = ByteBuffer.allocateDirect(maxBufferSize)
            val videoBufferInfo = MediaCodec.BufferInfo()
            val audioBufferInfo = MediaCodec.BufferInfo()

            var hasVideo = true
            var hasAudio = true

            var videoSize = videoExtractor.readSampleData(videoBuffer, 0)
            if (videoSize < 0) hasVideo = false
            var audioSize = audioExtractor.readSampleData(audioBuffer, 0)
            if (audioSize < 0) hasAudio = false

            while (hasVideo || hasAudio) {
                if (hasVideo && (!hasAudio || videoExtractor.sampleTime <= audioExtractor.sampleTime)) {
                    videoBufferInfo.offset = 0
                    videoBufferInfo.size = videoSize
                    videoBufferInfo.presentationTimeUs = videoExtractor.sampleTime
                    videoBufferInfo.flags = videoExtractor.sampleFlags
                    muxer.writeSampleData(videoTrackIndex, videoBuffer, videoBufferInfo)
                    videoExtractor.advance()
                    videoSize = videoExtractor.readSampleData(videoBuffer, 0)
                    if (videoSize < 0) hasVideo = false
                } else if (hasAudio) {
                    audioBufferInfo.offset = 0
                    audioBufferInfo.size = audioSize
                    audioBufferInfo.presentationTimeUs = audioExtractor.sampleTime
                    audioBufferInfo.flags = audioExtractor.sampleFlags
                    muxer.writeSampleData(audioTrackIndex, audioBuffer, audioBufferInfo)
                    audioExtractor.advance()
                    audioSize = audioExtractor.readSampleData(audioBuffer, 0)
                    if (audioSize < 0) hasAudio = false
                }
            }

            true
        } catch (_: Exception) {
            false
        } finally {
            try { muxer?.stop() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
            try { videoExtractor?.release() } catch (_: Exception) {}
            try { audioExtractor?.release() } catch (_: Exception) {}
        }
    }

    fun getStreamProxyUrl(context: Context, url: String, headers: Map<String, String> = emptyMap()): String {
        return try {
            val req = JSONObject().apply {
                put("action", "stream_proxy")
                put("url", url)
                if (headers.isNotEmpty()) {
                    val hObj = JSONObject()
                    headers.forEach { (k, v) -> hObj.put(k, v) }
                    put("headers", hObj)
                }
            }
            val res = execute(context, req)
            if (res.optBoolean("ok", false)) {
                val pUrl = res.optJSONObject("result")?.optString("proxy_url")
                    ?: res.optString("proxy_url")
                if (!pUrl.isNullOrBlank()) pUrl else url
            } else {
                url
            }
        } catch (_: Exception) {
            url
        }
    }
}
