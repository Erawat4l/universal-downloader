package com.erawat.arc

/** The mobile UI mirrors the capabilities exposed by the repository's dl.py CLI. */
object RepoCapabilities {
    val sources = listOf(
        "Direct files",
        "YouTube and supported video platforms",
        "Telegram media",
        "SelfStudys PDFs",
        "StudiesToday PDFs",
        "Studocu documents",
        "NetMirror movies and series",
    )

    val qualities = listOf("Best", "2160p", "1440p", "1080p", "720p", "480p", "360p", "240p", "144p")
    val actions = listOf("Download", "Copy direct link", "Stream in VLC", "Stream and download")
    val audioFormats = listOf("MP3", "M4A")
}
