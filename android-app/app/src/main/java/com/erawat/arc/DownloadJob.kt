package com.erawat.arc

import org.json.JSONObject

data class DownloadJob(
    val id: String,
    val title: String,
    val source: String,
    val detail: String,
    val status: String,
    val path: String = "",
    val progress: Float = 0f,
    val downloaded: String = "0 MB",
    val total: String = "--",
    val speed: String = "--",
    val eta: String = "--",
    val poster: String = "",
    val url: String = "",
    val headers: Map<String, String> = emptyMap(),
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("title", title)
        obj.put("source", source)
        obj.put("detail", detail)
        obj.put("status", status)
        obj.put("path", path)
        obj.put("progress", progress.toDouble())
        obj.put("downloaded", downloaded)
        obj.put("total", total)
        obj.put("speed", speed)
        obj.put("eta", eta)
        obj.put("poster", poster)
        obj.put("url", url)
        val h = JSONObject()
        headers.forEach { (k, v) -> h.put(k, v) }
        obj.put("headers", h)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): DownloadJob {
            val hMap = mutableMapOf<String, String>()
            val hObj = obj.optJSONObject("headers")
            hObj?.keys()?.forEach { k -> hMap[k] = hObj.optString(k) }
            return DownloadJob(
                id = obj.optString("id"),
                title = obj.optString("title"),
                source = obj.optString("source"),
                detail = obj.optString("detail"),
                status = obj.optString("status"),
                path = obj.optString("path"),
                progress = obj.optDouble("progress", 0.0).toFloat(),
                downloaded = obj.optString("downloaded", "0 MB"),
                total = obj.optString("total", "--"),
                speed = obj.optString("speed", "--"),
                eta = obj.optString("eta", "--"),
                poster = obj.optString("poster"),
                url = obj.optString("url"),
                headers = hMap
            )
        }
    }
}
