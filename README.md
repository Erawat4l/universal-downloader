<div align="center">

<img src="docs/assets/banner.gif" alt="ARC — Universal Downloader: paste a link, get your media" width="100%">

# ARC · Universal Downloader

**Paste a link. Get your media. Stream it, or keep it.**

One engine (`dl.py`), two front ends: **ARC**, a native Android app that carries the whole engine inside the APK,
and **`dl`**, a command-line tool for Termux, Linux, macOS and Windows. No companion apps, no terminal, no root.

[![Python](https://img.shields.io/badge/python-3.12%20%E2%80%93%203.14-3776ab?logo=python&logoColor=white)](#install)
[![Android API](https://img.shields.io/badge/Android%20API-35-3ddc84?logo=android&logoColor=white)](#install)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285f4?logo=jetpackcompose&logoColor=white)](#project-layout)
[![Chaquopy](https://img.shields.io/badge/Chaquopy-embedded%20Python-f5f5f5)](#why-it-is-fast)
[![aria2c](https://img.shields.io/badge/aria2c-16%C3%97%20parallel-fbbf24)](#why-it-is-fast)
[![4K UHD](https://img.shields.io/badge/up%20to-2160p-0d0d0d)](#quality-ladder)
[![Platforms](https://img.shields.io/badge/platforms-Android%20%C2%B7%20Termux%20%C2%B7%20Linux%20%C2%B7%20macOS%20%C2%B7%20Windows-22d3ee)](#install)
[![License: MIT](https://img.shields.io/badge/license-MIT-34d399)](#privacy-security--license)

<img src="docs/assets/demo.gif" alt="ARC in action: type a title, analyze, pick quality and audio" width="300">

</div>

---

## Table of contents

- [Why it is fast](#why-it-is-fast)
  - [Quality ladder](#quality-ladder)
- [Demo](#demo)
- [Features](#features)
- [Install](#install)
  - [Android (direct APK)](#android-direct-apk)
  - [Termux](#termux)
  - [Linux & macOS](#linux--macos)
  - [Windows](#windows)
  - [Build the APK yourself](#build-the-apk-yourself)
- [Quick start](#quick-start)
- [Using the app](#using-the-app)
- [Command reference (`dl`)](#command-reference-dl)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)
- [Roadmap](#roadmap)
- [Project layout](#project-layout)
- [Development](#development)
- [Privacy, security & license](#privacy-security--license)

---

## Why it is fast

Most "downloaders" are a web page wrapped around somebody else's server. ARC runs the whole pipeline on your
device: resolve the stream, fix the headers the CDN insists on, then move the bytes in parallel.

<div align="center">
<img src="docs/assets/pipeline.gif" alt="Link or title, resolve, fix headers, 16 parallel connections, merge to MP4" width="100%">
</div>

| Technique | Where | Effect |
|---|---|---|
| **Direct manifest extraction** | NetMirror titles, direct-link hosts, `yt-dlp` sites | No countdowns, pop-ups or ad-walls to click through |
| **Embedded Python (Chaquopy)** | Android app | The real `dl.py` runs inside the APK, one install and no helper app |
| **16-connection `aria2c`** (`-x 16 -s 16 -k 1M`) | direct files and `yt-dlp` | Saturates the link instead of one TCP stream |
| **Local HTTP `Range` proxy** on `127.0.0.1:8765` | streaming | Playback starts at once and seeking works |
| **Per-CDN header fix** (`Referer`, `Origin`) | proxy | Streams that return `403` in a bare player play normally |
| **Native `MediaMuxer`** | Android app | Joins separate video and audio into MP4 or WebM without `ffmpeg` |

> **Scope note.** Parallel chunking applies to direct files and `yt-dlp` downloads (16 connections).
> HLS (`.m3u8`) streams use 5 connections, because segment servers punish more than that.

### Quality ladder

Qualities are always listed highest first. `-q` accepts the names in the first column.

| `-q` value | Resolution | Label |
|---|---|---|
| `2160p` / `4k` | 3840×2160 | True 4K UHD |
| `1440p` | 2560×1440 | 2K (`yt-dlp` sources) |
| `1080p` | 1920×1080 | Full HD, the usual sweet spot |
| `720p` | 1280×720 | HD |
| `480p` · `360p` | 854×480 · 640×360 | SD · Low |
| `240p` · `144p` | — | Minimum (`yt-dlp` sources) |
| `best` | highest available | Default when you do not choose |
| `audio` · `mp3` | — | Audio only |

---

## Demo

<div align="center">

| Home | Settings | Search results | Review & quality |
|:---:|:---:|:---:|:---:|
| <img src="docs/assets/screens/home.png" width="200" alt="Home"> | <img src="docs/assets/screens/settings.png" width="200" alt="Settings"> | <img src="docs/assets/screens/search.png" width="200" alt="Search results"> | <img src="docs/assets/screens/review.png" width="200" alt="Review"> |
| Paste a link or type a title. | Save location, quality, player, notifications. | Live steps while ARC finds the title. | Pick audio language and quality, then download or stream. |

</div>

The terminal side (`dl` with no arguments):

```text
=================================================================
            UNIVERSAL DOWNLOADER CLI
=================================================================
> Enter URL or Movie/Video Name (or 'q' to quit):
```

---

## Features

| Area | What you get |
|---|---|
| **App UI** | Jetpack Compose + Material 3, monochrome dark theme. Four tabs: **Home**, **Activity**, **Library**, **Settings**. Live "thinking" card with elapsed time and step progress. Real errors are shown, never fake results |
| **Sources** | NetMirror titles, `yt-dlp` sites (YouTube and 1000+ more), media-host links (MediaFire, Terabox and similar), Telegram, BitTorrent magnets, educational PDFs, the India's Got Latent catalogue |
| **Quality** | 4K down to 360p for NetMirror, `yt-dlp` formats down to 144p, audio-only MP3 or M4A |
| **TV series & audio** | Season and episode selectors, download one episode, a season or the whole series (`Series/Season 01/…`). Language tags (Hindi, Tamil, Telugu, Bengali, Indonesian) choose the matching audio track |
| **Playback** | **ARC Player** (Media3 ExoPlayer) with quality, speed and audio-track switching, or stream in VLC / any player through the local proxy |
| **Four actions** | **Download** · **Copy direct link** (for ADM / 1DM) · **Stream** · **Stream and download** |
| **Downloads** | Resumable, automatic retries, foreground service, notifications, battery-optimisation prompt so downloads never pause |
| **Telegram** | Pyrogram MTProto for public and private channel posts, with your own string session |
| **Desktop** | `Arc.exe` for Windows (PySide6 front end around the same engine) |
| **Privacy** | No telemetry, no accounts, proxy bound to `127.0.0.1` only |

---

## Install

| You want | Use |
|---|---|
| The app, on a phone | [Android (direct APK)](#android-direct-apk) |
| The CLI on the same phone | [Termux](#termux) |
| The CLI on a computer | [Linux & macOS](#linux--macos) or [Windows](#windows) |
| To build the APK yourself | [Build the APK yourself](#build-the-apk-yourself) |

### Android (direct APK)

1. Download the newest `ARC {n}.apk` from the repository's **Releases** page.
2. Open it. Android asks once to **allow installs from this source**; allow it.
3. On first launch grant **notifications** and **media access**, and the **all files** switch if you want downloads in the public `Downloads` folder.
4. Install **VLC** (`org.videolan.vlc`) for instant streaming. ARC tries VLC first and falls back to any video player.

### Termux

```bash
pkg update && pkg upgrade -y
pkg install -y python ffmpeg aria2 git
termux-setup-storage          # once, so /sdcard/Download is writable

git clone https://github.com/Erawat4l/universal-downloader.git
cd universal-downloader
pip install yt-dlp pyrogram tgcrypto requests beautifulsoup4
chmod +x dl.py

mkdir -p ~/bin && ln -sf "$PWD/dl.py" ~/bin/dl
dl                            # interactive menu
```

### Linux & macOS

```bash
# Debian / Ubuntu:  sudo apt install -y python3 python3-venv ffmpeg aria2 git
# Arch:             sudo pacman -S python ffmpeg aria2 git
# Fedora:           sudo dnf install python3 ffmpeg aria2 git
# macOS:            brew install python ffmpeg aria2 git

git clone https://github.com/Erawat4l/universal-downloader.git
cd universal-downloader
python3 -m venv .venv && . .venv/bin/activate
pip install yt-dlp pyrogram tgcrypto requests beautifulsoup4

mkdir -p ~/.local/bin
printf '#!/bin/sh\nexec "%s/.venv/bin/python" "%s/dl.py" "$@"\n' "$PWD" "$PWD" > ~/.local/bin/dl
chmod +x ~/.local/bin/dl
dl
```

The wrapper makes `dl` always run inside the virtualenv. Debian and Ubuntu refuse a global `pip install`
("externally managed environment"), which is why the virtualenv is used.

### Windows

**WSL (recommended)** behaves exactly like Linux:

```powershell
wsl --install -d Ubuntu
```

**Arc.exe** (desktop app): from a checkout, run `desktop-app\build.bat`. It creates a venv, installs the
dependencies, runs a headless UI self-test and freezes `dist\Arc.exe`.

**Native PowerShell** for the CLI:

```powershell
winget install -e --id Python.Python.3.12
winget install -e --id Gyan.FFmpeg
winget install -e --id aria2.aria2
git clone https://github.com/Erawat4l/universal-downloader.git
cd universal-downloader
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install yt-dlp pyrogram tgcrypto requests beautifulsoup4
python dl.py
```

### Build the APK yourself

Termux is only the build machine; the finished APK does not need it.

```bash
pkg install -y git python wget unzip zip aapt2 apksigner openjdk-17
git clone https://github.com/Erawat4l/universal-downloader.git
cd universal-downloader/android-app
python3 tools/build_android.py
```

The builder finds or installs the Android SDK, picks the highest supported Python for Chaquopy (3.14, then 3.13,
then 3.12), syncs the root `dl.py` into the app, runs Gradle and writes the debug APK to
`app/build/outputs/apk/debug/`. It also makes a numbered copy (`ARC 1.apk`, `ARC 2.apk`, …). The first build
downloads more than 600 MB. On Windows run `build.bat`. Step-by-step Termux notes are in [`TERMUX.md`](TERMUX.md).

---

## Quick start

1. Open **ARC** and paste a link, or type a title such as `param sundari`.
2. Tap **Analyze**. The live card shows each step while the title is found and resolved.
3. On **Review**, choose the **audio language** and **video quality**.
4. Tap **Download**, or **Stream** to watch in ARC Player straight away.
5. Finished files appear in **Activity** and **Library** with their saved path.

From a terminal the same flow is one line:

```bash
dl "https://www.youtube.com/watch?v=dQw4w9WgXcQ" -q 1080p
```

---

## Using the app

| Tab | What it does |
|---|---|
| **Home** | Paste a link or search by title, and see recent downloads |
| **Activity** | Running jobs with speed, progress, ETA and the saved path |
| **Library** | Everything you have downloaded, ready to play or share |
| **Settings** | Save location, default quality, streaming player, notifications, battery optimisation, Google account (optional, for YouTube sign-in), **About ARC Features** |

**ARC Player** controls: quality, playback speed and audio track from the settings sheet, lock controls, and seek
bar. Switching audio or quality keeps your position.

---

## Command reference (`dl`)

```text
dl                              interactive menu
dl <url | magnet | title> ...   download, or search by title
```

| Flag | Meaning |
|---|---|
| *(none)* | Interactive menu: paste a link, or type a title. Start a query with `yt:` to search YouTube |
| `-q`, `--quality <value>` | `best`, `2160p`/`4k`, `1440p`, `1080p`, `720p`, `480p`, `360p`, `240p`, `144p`, `audio`, `mp3` |
| `-d <folder>` | Output folder |
| `-o <name>` | Output file name |
| `--cookies <x>` | Cookie file or raw cookie string |

Interactive mode asks what to do with a link: `1` download (16-connection `aria2c`), `2` copy the direct link,
`3` stream, `4` stream and download.

```bash
dl -q 2160p -d ~/Videos "https://www.youtube.com/watch?v=dQw4w9WgXcQ"   # a 4K file into a folder
dl "Inception"                                                           # search by name, then pick an action
dl -q 1080p "https://www.youtube.com/playlist?list=PL..."                # a playlist
dl -q mp3 "https://youtu.be/..."                                         # audio only
dl "magnet:?xt=urn:btih:..."                                             # a torrent magnet
dl "https://t.me/c/1234567890/100"                                       # a Telegram post (needs credentials)
```

---

## Configuration

### Telegram (MTProto)

Public posts can sometimes be fetched without credentials. Private channels need your own API ID and hash from
[my.telegram.org](https://my.telegram.org) and a Pyrogram **string session**.

| Setting | Environment variable | In the app |
|---|---|---|
| API ID | `TELEGRAM_API_ID` | Settings → Telegram → Setup |
| API hash | `TELEGRAM_API_HASH` | Settings → Telegram → Setup |
| String session | `TELEGRAM_SESSION` | Settings → Telegram → Setup |

### Paths, quality and cookies

| Want | How |
|---|---|
| Download folder | `-d <folder>`. Default: `/sdcard/Download` if writable, else `~/Downloads` |
| Output file name | `-o <name>` |
| Cookies (age-gated or logged-in sites) | `--cookies <file-or-string>`, or a `cookies.txt` in `/sdcard/Download` or your home folder |
| Studocu | `STUDOCU_COOKIE` environment variable |
| Local proxy port | Fixed: `8765`, falling back to `8766`–`8768` |

Secrets are git-ignored (`*.local.json`, `.env`, `*.session`). Inside the app they live in its private preferences.

---

## Troubleshooting

<details>
<summary><b>Stream plays for a second, then <code>403</code> or <code>429</code></b></summary>

Some CDNs check that the request came from their own player page. ARC routes the stream through the local proxy,
which adds the right `Referer` and `Origin` and retries once after a `429`. Play the proxy URL
(`http://127.0.0.1:8765/stream.mp4`) that `dl` prints, not the raw CDN link.
</details>

<details>
<summary><b>The proxy port is busy</b></summary>

`dl` tries `8765`, then `8766` to `8768`. If all four are taken, free one (`ss -ltnp | grep 876`) or stop the other copy.
</details>

<details>
<summary><b>A file "downloaded" but I cannot find it (Android 11+)</b></summary>

- **Termux:** run `termux-setup-storage` once. `dl` writes to `/sdcard/Download` when it can.
- **ARC:** grant the **all files** switch, or look in `Android/data/com.erawat.arc/files/Download`. The path of each
  finished file is shown in **Activity** and **Library**.
- **No notification, or the proxy stops when VLC opens:** allow notifications for ARC (needed for the foreground service on Android 13+).
</details>

<details>
<summary><b>Termux build: <code>aapt2</code> fails</b></summary>

Gradle downloads an x86 `aapt2` that cannot run on Android. The builder already uses the Termux one, so just run
`pkg install aapt2`. If you run Gradle by hand:
`./gradlew assembleDebug -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`
</details>

<details>
<summary><b>Video without sound on the CLI</b></summary>

Separate 1080p and 4K video and audio streams are merged with FFmpeg. If `ffmpeg` is missing you get video only. Install it with your package manager.
</details>

<details>
<summary><b><code>which dl</code> points at the wrong program</b></summary>

`which -a dl` lists every `dl` on your `PATH` in the order the shell tries them. Delete the ones you do not
want, then run `hash -r` in bash or zsh.
</details>

<details>
<summary><b><code>dlopen failed: cannot locate symbol</code> on Termux</b></summary>

An old library under a new `python` package: `pkg update && pkg upgrade && pkg install --reinstall libexpat python`.
</details>

---

## Roadmap

- [x] Native Android app with the embedded engine
- [x] 16-connection downloads, local `Range` proxy, ARC Player
- [x] Multi-audio selection and TV series support
- [x] Windows desktop app
- [ ] **Subtitles** for movies and YouTube: pick a track, show it in ARC Player, save `.srt` next to the video
- [ ] Review screen: quality chips, file-size estimate on the Download button
- [ ] Home polish: quick-start hints, live status pill, Activity badge for running jobs

---

## Project layout

```text
universal-downloader/
├── dl.py                     # the engine and the `dl` CLI (single source of truth)
├── igl.py                    # India's Got Latent catalogue, streaming and downloads
├── TERMUX.md                 # on-device APK build
├── build.bat                 # Windows launcher for the APK build
├── tests/                    # engine and desktop-core tests
├── desktop-app/              # PySide6 desktop UI and PyInstaller Arc.exe build
└── android-app/
    ├── tools/build_android.py        # finds JDK/SDK/Gradle, builds, numbers ARC {n}.apk
    ├── python-vendor/pyaes/          # vendored pure-Python AES
    └── app/src/main/
        ├── AndroidManifest.xml
        ├── python/                   # dl.py (synced at build time), igl.py, tgcrypto.py
        ├── jniLibs/                  # bundled aria2c for arm64-v8a and x86_64
        └── java/com/erawat/arc/
            ├── MainActivity.kt       # Compose UI, theme, ARC Player
            ├── EmbeddedEngine.kt     # calls dl.py through Chaquopy
            ├── DownloadService.kt    # foreground download service
            ├── DownloadManager.kt    # saved job queue and state
            ├── StreamService.kt      # foreground service for the stream proxy
            └── ArcCrypto.kt          # native AES for Telegram downloads
```

The app's copy of `dl.py` is overwritten from the root file on every build, so **edit the root `dl.py`** and rebuild.

---

## Development

```bash
python -m pytest tests/                       # engine and desktop-core tests
cd android-app && python3 tools/build_android.py
cd desktop-app && build.bat                   # Windows, builds dist\Arc.exe
```

A CI workflow (`.github/workflows/build-windows-exe.yml`) builds `Arc.exe` on Windows and attaches it to a release when you push a `v*` tag.

---

## Privacy, security & license

- **Zero telemetry.** No analytics, no crash reporting, no accounts. ARC talks only to the sites you ask it to reach and to Telegram when you use it.
- **Private by default.** Downloads, history and Telegram values stay on your device. The local proxy listens on `127.0.0.1` only.
- **Credentials.** No API ID, hash, session or cookie is stored in the repository. A Telegram string session grants access to your account: treat it like a password and revoke it from Telegram's *Active sessions* if it leaks.
- **Use responsibly.** ARC fetches media you can already open in a browser, without the ads. Download only what you have the right to keep, and respect the terms of the services you use.

Released under the **MIT License**. See `LICENSE`.
