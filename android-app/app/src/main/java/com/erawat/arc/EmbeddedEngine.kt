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

    @JvmStatic
    fun mux(videoPath: String, audioPath: String, outputPath: String): Boolean {
        return muxMultiple(videoPath, listOf(audioPath), outputPath)
    }

    @JvmStatic
    fun muxMultiple(videoPath: String, audioPaths: Array<String>, outputPath: String): Boolean {
        return muxMultiple(videoPath, audioPaths.toList(), outputPath)
    }

    @JvmStatic
    fun muxMultiple(videoPath: String, audioPaths: List<String>, outputPath: String): Boolean {
        var videoExtractor: MediaExtractor? = null
        val audioExtractors = mutableListOf<MediaExtractor>()
        var muxer: MediaMuxer? = null
        return try {
            val vFile = File(videoPath)
            if (!vFile.exists() || vFile.length() == 0L) {
                android.util.Log.e("EmbeddedEngine", "Mux video file missing or empty: $videoPath")
                return false
            }

            val validAudioFiles = audioPaths.map { File(it) }.filter { it.exists() && it.length() > 0L }
            if (validAudioFiles.isEmpty()) {
                android.util.Log.e("EmbeddedEngine", "No valid audio files to mux: $audioPaths")
                return false
            }

            val outFile = File(outputPath)
            outFile.parentFile?.mkdirs()
            if (outFile.exists()) outFile.delete()

            videoExtractor = MediaExtractor()
            videoExtractor.setDataSource(vFile.absolutePath)

            val isWebm = outputPath.endsWith(".webm", ignoreCase = true) ||
                         videoPath.endsWith(".webm", ignoreCase = true)
            val outFormat = if (isWebm) {
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
            if (videoTrackIndex == -1) {
                android.util.Log.e("EmbeddedEngine", "No video track found in: $videoPath")
                return false
            }

            val audioTrackIndices = mutableListOf<Int>()
            for (aFile in validAudioFiles) {
                val aExt = MediaExtractor()
                try {
                    aExt.setDataSource(aFile.absolutePath)
                    var trackAdded = false
                    for (i in 0 until aExt.trackCount) {
                        val format = aExt.getTrackFormat(i)
                        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                        if (mime.startsWith("audio/")) {
                            try {
                                val tIdx = muxer.addTrack(format)
                                aExt.selectTrack(i)
                                audioTrackIndices.add(tIdx)
                                audioExtractors.add(aExt)
                                trackAdded = true
                                break
                            } catch (te: Exception) {
                                android.util.Log.w("EmbeddedEngine", "Skip incompatible audio track format: $format", te)
                            }
                        }
                    }
                    if (!trackAdded) {
                        try { aExt.release() } catch (_: Exception) {}
                    }
                } catch (e: Exception) {
                    android.util.Log.w("EmbeddedEngine", "Failed to add audio track from: ${aFile.absolutePath}", e)
                    try { aExt.release() } catch (_: Exception) {}
                }
            }

            if (audioTrackIndices.isEmpty()) {
                android.util.Log.e("EmbeddedEngine", "No audio tracks could be added from: $audioPaths")
                return false
            }

            muxer.start()
            val maxBufferSize = 16 * 1024 * 1024
            val buffer = ByteBuffer.allocateDirect(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            class TrackSource(val extractor: MediaExtractor, val muxerTrackIndex: Int) {
                var isDone = false
                var lastTimestampUs: Long = -1L
                fun sampleTime(): Long {
                    if (isDone) return Long.MAX_VALUE
                    val t = extractor.sampleTime
                    return if (t < 0) Long.MAX_VALUE else t
                }
            }

            val allTracks = mutableListOf<TrackSource>()
            allTracks.add(TrackSource(videoExtractor, videoTrackIndex))
            for (i in audioExtractors.indices) {
                allTracks.add(TrackSource(audioExtractors[i], audioTrackIndices[i]))
            }

            while (true) {
                val nextSource = allTracks.filter { !it.isDone }.minByOrNull { it.sampleTime() }
                if (nextSource == null || nextSource.sampleTime() == Long.MAX_VALUE) break

                val sampleSize = nextSource.extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) {
                    nextSource.isDone = true
                    continue
                }

                var pts = nextSource.extractor.sampleTime
                if (pts < 0) {
                    nextSource.isDone = true
                    continue
                }
                if (pts <= nextSource.lastTimestampUs) {
                    pts = nextSource.lastTimestampUs + 1000L
                }
                nextSource.lastTimestampUs = pts

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = pts
                bufferInfo.flags = nextSource.extractor.sampleFlags
                try {
                    muxer.writeSampleData(nextSource.muxerTrackIndex, buffer, bufferInfo)
                } catch (we: Exception) {
                    android.util.Log.w("EmbeddedEngine", "writeSampleData warning for track ${nextSource.muxerTrackIndex}: ${we.message}")
                }
                nextSource.extractor.advance()
            }

            muxer.stop()
            val success = outFile.exists() && outFile.length() > 0
            if (!success) {
                android.util.Log.e("EmbeddedEngine", "Muxer finished but destination file missing or empty: $outputPath")
            }
            success
        } catch (e: Exception) {
            android.util.Log.e("EmbeddedEngine", "Native muxMultiple failed with exception", e)
            false
        } finally {
            try { muxer?.release() } catch (_: Exception) {}
            try { videoExtractor?.release() } catch (_: Exception) {}
            for (a in audioExtractors) {
                try { a.release() } catch (_: Exception) {}
            }
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
