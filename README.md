<div align="center">

<img src="docs/assets/banner.png" alt="Arc / Universal Downloader — stream bypasser, 4K downloader and offline video hub" width="100%">

# Arc / Universal Downloader

**The high-speed stream bypasser, 4K media downloader, and offline video hub — from terminal to native Android.**

One engine (`dl.py`), two front ends: the **`dl`** command-line tool for Termux, Linux, macOS and Windows,
and **Arc**, a native Android app (`com.erawat.arc`) that carries the same engine inside the APK.
No companion apps, no terminal, no root.

[![Python](https://img.shields.io/badge/python-3.12%20%E2%80%93%203.14-3776ab?logo=python&logoColor=white)](#install--setup)
[![Android API](https://img.shields.io/badge/Android%20API-35-3ddc84?logo=android&logoColor=white)](#arc-android-app)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285f4?logo=jetpackcompose&logoColor=white)](#arc-android-app)
[![Chaquopy](https://img.shields.io/badge/Chaquopy-embedded%20Python-f5f5f5)](#why-it-is-fast--unique)
[![License: MIT](https://img.shields.io/badge/license-MIT-34d399)](#privacy-security--license)
[![4K UHD](https://img.shields.io/badge/4K%20UHD-up%20to%202160p-0d0d0d)](#quality-ladder)
[![aria2c](https://img.shields.io/badge/aria2c-16%C3%97%20parallel-fbbf24)](#why-it-is-fast--unique)
[![Branch](https://img.shields.io/badge/branch-main%20only-8e8e93)](#project-architecture)

</div>

---

## Demo

| Home | Media review & quality selector | Live thinking diagnostics | Download activity |
|:---:|:---:|:---:|:---:|
| <img src="docs/assets/screens/home.png" alt="Arc home screen" width="200"> | <img src="docs/assets/screens/review.png" alt="Arc media review with 4K quality selector" width="200"> | <img src="docs/assets/screens/thinking.png" alt="Arc live thinking diagnostics card" width="200"> | <img src="docs/assets/screens/activity.png" alt="Arc download activity" width="200"> |
| Paste a link or type a title. | Pick 4K → 360p, audio track and episode. | Elapsed seconds, steps and a sanitised trace. | Speed, progress, ETA and the saved path. |

The terminal side looks like this (`dl` with no arguments):

```text
=================================================================
            UNIVERSAL DOWNLOADER CLI
=================================================================
> Enter URL or Movie/Video Name (or 'q' to quit):
```

---

## Table of contents

- [Demo](#demo)
- [Why it is fast & unique](#why-it-is-fast--unique)
  - [Quality ladder](#quality-ladder)
- [Feature breakdown](#feature-breakdown)
- [Arc Android app](#arc-android-app)
- [Install & setup](#install--setup)
  - [Android (direct APK)](#android-direct-apk)
  - [Termux (native on Android)](#termux-native-on-android)
  - [Building the APK on Termux](#building-the-apk-on-termux)
  - [Linux & macOS](#linux--macos)
  - [Windows (PowerShell & WSL)](#windows-powershell--wsl)
- [Troubleshooting & edge cases](#troubleshooting--edge-cases)
- [Configuration & credentials](#configuration--credentials)
- [CLI command reference (`dl`)](#cli-command-reference-dl)
- [Project architecture](#project-architecture)
- [Privacy, security & license](#privacy-security--license)

---

## Why it is fast & unique

Most "downloaders" are a web page wrapped around somebody else's server. This one runs the whole pipeline on
your device: resolve the stream, fix the headers the CDN insists on, then move bytes in parallel.

| What | The usual way | What Arc / `dl` does | Effect |
|---|---|---|---|
| **Getting the stream** | Open a site, wait out a countdown, dodge an ad-wall or shortener, click the real button | Extracts the CDN manifest directly (NetMirror titles, direct-link hosts, `yt-dlp` sites) | No timers, no pop-ups, nothing to click through |
| **Running Python on Android** | Install Termux, then a helper app, then grant `RUN_COMMAND` | **Chaquopy** embeds Python and the real `dl.py` in the APK | One install; the app calls the engine in-process |
| **Watching before it finishes** | Download the whole file, then play it | A local HTTP **Range** proxy on `127.0.0.1` (port `8765`, then `8766`–`8768` if busy) feeds VLC or any player | Playback starts at once and seeking works |
| **CDN checks the `Referer`** | Player gets `403`, or a `429` from hammering the origin | The proxy injects `Referer: https://mzfi.me/` for those CDNs and retries once on `429` | Streams that fail in a bare player play normally |
| **Moving the bytes** | One connection, one browser tab | `aria2c` with **16 connections** (`-x 16 -s 16 -k 1M`) | Saturates the link instead of one TCP stream |
| **Separate video + audio** | Needs an `ffmpeg` binary, which Android does not have | Android's native `MediaMuxer` joins the streams into MP4 or WebM inside the app | HD/4K with sound, no external binaries |

> **Honest scope note.** Parallel chunking applies to direct files and `yt-dlp` downloads (16 connections).
> HLS (`.m3u8`) streams are fetched with 5 connections, because segment servers punish more than that.

### Quality ladder

Qualities are always listed highest first. `-q` accepts the names in the first column.

| `-q` value | Resolution | Label | Notes |
|---|---|---|---|
| `2160p` / `4k` | 3840×2160 | True 4K UHD | Sample 4K title measured by the author: **3.4 GB** |
| `1440p` | 2560×1440 | 2K | `yt-dlp` sources |
| `1080p` | 1920×1080 | Full HD | The usual sweet spot |
| `720p` | 1280×720 | HD | |
| `480p` | 854×480 | SD | |
| `360p` | 640×360 | Low | |
| `240p`, `144p` | — | Minimum | `yt-dlp` sources |
| `best` | highest available | Auto | Default when you do not choose |
| `audio`, `mp3` | — | Audio only | MP3 or M4A |

For NetMirror titles the ladder offered is **4K → 1080p → 720p → 480p → 360p**, ordered strictly descending.
File size grows steeply at the top: a 4K file is several times the size of the same title at 1080p, so check
free space before choosing it.

---

## Feature breakdown

| Area | What you get |
|---|---|
| **Mobile app UI** | Jetpack Compose + Material 3. Four tabs: **Home**, **Activity**, **Library**, **Settings**. A monochrome design system: `ArcBlack #050505`, `ArcSurface #111111`, `ArcRaised #191919`, `ArcWhite #F7F7F7`, `ArcMuted #B0B0B0`. The `ClaudeThinkingCard` diagnostic HUD shows elapsed seconds, multi-step progress and a terminal-style trace with internal backend details stripped out. Real errors are shown, never fake results. |
| **Stream bypasser & quality engine** | Direct extraction of CDN manifests from NetMirror titles. Media-host links (MediaFire, Terabox and similar) go through the direct-link bypasser. Quality ladder from 4K down to 360p, plus `yt-dlp` formats up to 144p. Header normalisation per CDN (`Referer`, `Origin`). |
| **TV series & multi-audio** | Season and episode detection with selectors. Download one episode, a whole season, or the whole series; batch jobs land in `Series/Season 01/…`. Language tags in the title (Hindi, Tamil, Telugu, Bengali, Indonesian) choose the matching audio track, and origin country is used when a title carries one (`cn: India` → Hindi). |
| **Downloader acceleration** | `aria2c` at 16 connections for direct files and `yt-dlp`, resumable, with automatic retries. `yt-dlp` stream parser with `bestvideo+bestaudio` merging (FFmpeg on desktop, `MediaMuxer` in the app). |
| **Playback** | Four actions on every title: **Download**, **Copy direct link** (for ADM / 1DM), **Stream in VLC**, **Stream and download**. A foreground service keeps the proxy alive while an external player has focus. |
| **Telegram** | Pyrogram MTProto engine for public and private channel messages (`t.me/c/<chat>/<msg>`), authenticated with a string session. |
| **Documents** | SelfStudys, StudiesToday and Studocu PDFs. |
| **IGL catalogue** | India's Got Latent episode list with cached metadata, members-only badging, direct streaming to VLC and parallel `aria2c` downloading, through the bundled `igl.py`. |
| **Terminal suite** | `dl` (menu or arguments) and `igl`. Search by name, `yt:` prefix for YouTube search, magnet links, cookie files, custom output name and folder. |

---

## Arc Android app

`com.erawat.arc` · minSdk 26 (Android 8.0) · compile/target SDK 35.

- **Standalone.** Chaquopy bundles `dl.py`, `requests`, BeautifulSoup, `yt-dlp` and `pyrogram==2.0.106`. Search,
  resolve, stream and download all run inside the app process.
- **Updates.** Settings checks GitHub for a newer commit and for the latest release APK.
- **Permissions.** Internet, notifications, media read access, foreground service (media playback), and
  all-files access so downloads can be written to public Downloads. See [Android 14+](#android-14--scoped-storage).

---

## Install & setup

| You want | Use |
|---|---|
| The app, on a phone | [Android (direct APK)](#android-direct-apk) |
| The CLI on the same phone | [Termux](#termux-native-on-android) |
| The CLI on a computer | [Linux & macOS](#linux--macos) or [Windows](#windows-powershell--wsl) |
| To build the APK yourself | [Building the APK on Termux](#building-the-apk-on-termux) |

### Android (direct APK)

1. Download the newest `ARC {n}.apk` from the repository's **Releases** page (`{n}` is the build number, for
   example `ARC 31.apk`).
2. Open it. Android asks once to **allow installs from this source** for your browser or file manager; allow it.
3. On first launch grant **notifications** and **media access**, and the **all files** switch if you want
   downloads in public `Downloads`.
4. Install **VLC** (`org.videolan.vlc`) for instant streaming. Arc tries VLC first and falls back to any video
   player if VLC is missing.

### Termux (native on Android)

Install Termux from F-Droid or its GitHub releases, then:

```bash
pkg update && pkg upgrade -y
pkg install -y python ffmpeg aria2 git
termux-setup-storage          # once, so /sdcard/Download is writable

git clone https://github.com/Erawat4l/universal-downloader.git
cd universal-downloader
pip install yt-dlp pyrogram tgcrypto requests beautifulsoup4
chmod +x dl.py
```

The package is `aria2`; the program it installs is called `aria2c`. Put `dl` on your `PATH`:

```bash
mkdir -p ~/bin && ln -sf "$PWD/dl.py" ~/bin/dl
dl                             # interactive menu
```

If `~/bin` is not on your `PATH`, add it (`export PATH="$HOME/bin:$PATH"` in your shell profile; in fish,
`fish_add_path ~/bin`).

### Building the APK on Termux

Termux is only the build machine. The finished APK does not need Termux.

```bash
pkg install -y git python wget unzip zip aapt2 apksigner openjdk-17
git clone https://github.com/Erawat4l/universal-downloader.git
cd universal-downloader/android-app
python3 tools/build_android.py
```

The builder finds or installs the Android SDK, picks the highest supported Python (**3.14**, then 3.13, then
3.12) for Chaquopy, syncs the repository's `dl.py` into the app, runs Gradle, and writes the debug APK to
`app/build/outputs/apk/debug/`. It then makes a **numbered copy**: `ARC 1.apk`, `ARC 2.apk` and so on, one higher
than the largest number already in the build folder or `/sdcard/Download`. If `tt` is on your `PATH` the numbered
APK is sent with `tt send 2 <apk>`; if that fails, the local APK is kept.

The first build downloads more than 600 MB. Wait for the prompt to return. The full step-by-step block,
including a fish-shell version, is in [`TERMUX.md`](TERMUX.md).

### Linux & macOS

```bash
# Debian / Ubuntu / Mint
sudo apt update && sudo apt install -y python3 python3-venv ffmpeg aria2 git
# Arch / Manjaro:  sudo pacman -S python ffmpeg aria2 git
# Fedora / RHEL:   sudo dnf install python3 ffmpeg aria2 git
# macOS:           brew install python ffmpeg aria2 git

git clone https://github.com/Erawat4l/universal-downloader.git
cd universal-downloader
python3 -m venv .venv && . .venv/bin/activate
pip install yt-dlp pyrogram tgcrypto requests beautifulsoup4
chmod +x dl.py

mkdir -p ~/.local/bin
printf '#!/bin/sh\nexec "%s/.venv/bin/python" "%s/dl.py" "$@"\n' "$PWD" "$PWD" > ~/.local/bin/dl
chmod +x ~/.local/bin/dl
dl
```

The small wrapper (instead of a bare symlink) makes `dl` always run inside the virtualenv, so it finds its
packages. Debian and Ubuntu refuse a global `pip install` ("externally managed environment"), which is why the
virtualenv is used.

### Windows (PowerShell & WSL)

**WSL (recommended)** gives you the same behaviour as Linux, including `aria2c`:

```powershell
wsl --install -d Ubuntu
```

Then follow the [Linux](#linux--macos) steps inside Ubuntu.

**Native PowerShell** works too:

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

If PowerShell blocks the activation script:
`Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass`. Open a new terminal after `winget` installs so the
new `PATH` is picked up. To build the APK on Windows, run `build.bat` in the repository root: it is a thin
launcher around the same `build_android.py`.

---

## Troubleshooting & edge cases

### Termux build: `aapt2` fails ("Syntax error", "cannot execute")

Gradle downloads an x86 `aapt2` from Maven, which cannot run on Android. `build_android.py` already handles this
on Termux: it finds `aapt2` on your `PATH` and passes it to Gradle. You only need the package:

```bash
pkg install aapt2
```

If you run Gradle by hand instead of through the builder, give it the override yourself:

```bash
./gradlew assembleDebug -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

The error `Termux requires the ARM64 aapt2 package` means the package is missing.

### Python version for the build

The build needs Python 3.12, 3.13 or 3.14. Anything else stops with a message naming the supported versions.
Check with `python --version`.

### Stream plays for a second, then `403` or `429`

Some CDNs (`hakunaymatata`, `bcdn`, `okcdn` hosts) check that the request came from their own player page. A bare
player sends no such page, so the CDN refuses, and hammering it earns a `429`. `dl` and Arc route the stream
through the local proxy, which adds `Referer: https://mzfi.me/` and `Origin: https://mzfi.me` and retries once
after a `429`. To play it yourself, use the proxy URL (`http://127.0.0.1:8765/stream.mp4` or `.m3u8`) that `dl`
prints, not the raw CDN link. Copying the raw link into ADM or 1DM only works when the downloader can send the same
`Referer`; the "Copy direct link" mode prints the Referer next to the URL for that reason.

### The proxy port is busy

`dl` tries `8765`, then `8766`, `8767`, `8768`. If all four are taken it cannot start the proxy. Free the port
(`ss -ltnp | grep 876`) or stop the other copy.

### `which dl` points at the wrong program

If you installed `dl` more than once (a symlink in `~/bin`, a wrapper in `~/.local/bin`, an old copy elsewhere),
the first one on `PATH` answers. Symptoms look unrelated: an old menu, a missing option.

```bash
which -a dl            # every dl on PATH, in the order the shell tries them
type -a dl             # bash / zsh equivalent
```

Delete the ones you do not want, then make the shell forget its cache: `hash -r` in bash or zsh. In fish nothing
is needed. On Windows use `where.exe dl`.

### Android 14+ & scoped storage

Android 11 and later restrict writing to shared storage, and 14 is stricter about foreground services. If a file
"downloaded" but is nowhere in your file manager:

- **Termux:** run `termux-setup-storage` once. `dl` writes to `/sdcard/Download` when it can, and falls back to
  `~/Downloads`.
- **Arc:** grant the **all files** switch in the system settings, or accept that files go to the app's own
  Downloads folder under `Android/data/com.erawat.arc/files/Download`. The path of each finished file is shown in
  **Activity** and **Library**.
- **No notification or the proxy stops when VLC opens:** allow notifications for Arc. The foreground service that
  keeps the proxy alive needs one on Android 13 and later.

### `dlopen failed: cannot locate symbol ...` on Termux

An old library under a new `python` package. Every `pip` command fails from inside Python:

```bash
pkg update && pkg upgrade
pkg install --reinstall libexpat python
```

### Telegram: `Pyrogram is not available` / `String session is required`

The engine needs `pyrogram` and `tgcrypto` installed, and all three credentials set. See
[Configuration & credentials](#configuration--credentials).

### Merging video and audio on the CLI

Separate 1080p/4K video and audio streams are merged with FFmpeg. If `ffmpeg` is missing you get the video
without sound. Install it with your package manager.

---

## Configuration & credentials

### Telegram (MTProto)

Public channel posts can sometimes be fetched without credentials. Private channels and dependable media downloads
need your own API ID and hash from [my.telegram.org](https://my.telegram.org) and a Pyrogram **string session**.

| Setting | Environment variable | App field |
|---|---|---|
| API ID (numeric) | `TELEGRAM_API_ID` | Settings → Telegram → Setup |
| API hash | `TELEGRAM_API_HASH` | Settings → Telegram → Setup |
| String session | `TELEGRAM_SESSION` | Settings → Telegram → Setup |

```bash
export TELEGRAM_API_ID="1234567"
export TELEGRAM_API_HASH="your_api_hash"
export TELEGRAM_SESSION="your_pyrogram_string_session"
```

If the variables are not set, `dl` looks in `credentials.local.json` (next to `dl.py`, or in `assets/`) and
`~/.telegram_credentials.json`:

```json
{ "api_id": "1234567", "api_hash": "your_api_hash", "session": "your_pyrogram_string_session" }
```

These files are git-ignored (`*.local.json`, `.env`, `*.session`). In the app, the values live in its private
preferences and are handed only to the bundled engine for a Telegram download.

### Paths, quality and cookies

| Want | How |
|---|---|
| Download folder | `-d <folder>`. Default: `/sdcard/Download` if writable, else `~/Downloads`. For `igl`: `ARC_DOWNLOAD_DIR`. |
| Output file name | `-o <name>` |
| Default quality | There is no saved default. Pass `-q <value>` (see the [quality ladder](#quality-ladder)); without it you are asked, or `best` is used. |
| Cookies (age-gated or logged-in sites) | `--cookies <file-or-string>`. If you pass none, `dl` looks for `cookies.txt` or `youtube_cookies.txt` in `/sdcard/Download` and your home folder. |
| Studocu | `STUDOCU_COOKIE` environment variable |
| Local proxy port | Fixed: `8765`, falling back to `8766`–`8768`. It is not configurable. |

---

## CLI command reference (`dl`)

```text
dl                              interactive menu
dl <url | magnet | title> ...   download, or search by title
```

| Flag | Meaning |
|---|---|
| *(none)* | Interactive menu: paste a link, or type a title to search. Start a query with `yt:` to search YouTube. |
| `-i`, `--interactive`, `menu` | Open the interactive menu explicitly. |
| `-q`, `--quality <value>` | Quality: `best`, `2160p`/`4k`, `1440p`, `1080p`, `720p`, `480p`, `360p`, `240p`, `144p`, `audio`, `mp3`. |
| `-d <folder>` | Output folder. |
| `-o <name>` | Output file name. |
| `--cookies <x>`, `-c`, `--cookie` | Cookie file or raw cookie string. |
| `--app-json`, `--app-json-b64` | Machine interface used by the Android app. Not for everyday use. |

Interactive mode asks what to do with a link:

| Choice | Action |
|---|---|
| `1` | Download (multi-threaded `aria2c`, 16 connections) |
| `2` | Copy the direct stream link (for ADM / 1DM), with the Referer shown |
| `3` | Stream in VLC, nothing stored |
| `4` | Stream in VLC and download |

> "Direct link" is a menu choice, not a command-line flag.

### Examples

```bash
# A 4K file from a direct or yt-dlp source, into a chosen folder
dl -q 2160p -d ~/Videos "https://www.youtube.com/watch?v=dQw4w9WgXcQ"

# A movie by name: searches NetMirror, then offers the four actions above
dl "Inception"

# A TV series: pick the title, then the season and episode (S1E1) at the prompts.
# Download scope can be one episode, a season, or the series (Series/Season 01/...).
dl "Breaking Bad"

# A YouTube playlist, 1080p
dl -q 1080p "https://www.youtube.com/playlist?list=PL..."

# Audio only
dl -q mp3 "https://youtu.be/..."

# A torrent magnet
dl "magnet:?xt=urn:btih:..."

# A Telegram post (needs credentials)
dl "https://t.me/c/1234567890/100"

# Custom name and folder, with a cookie file
dl -o lesson.mp4 -d /sdcard/Download --cookies ~/cookies.txt "https://example.com/video"
```

### India's Got Latent (IGL)

Typing a title that mentions "igl", "latent" or "samay" in `dl` lists the catalogue. The standalone tool takes
episode numbers, search text and a few flags:

```bash
igl                  # interactive list
igl 12               # episode number 12 from the list
igl --latest         # newest episode
igl -s "episode 5"   # stream in VLC and download
igl -so latest       # stream only
igl -d 12            # download only
igl -r               # refresh the cached episode list
```

`igl.py` must be importable or live at `~/scripts/igl.py` for `dl` to find it; the APK bundles it.

---

## Project architecture

Single authoritative branch: **`main`**.

```text
universal-downloader/
├── dl.py                     # the engine and the `dl` CLI (single source of truth)
├── igl.py                    # India's Got Latent catalogue, streaming and downloads
├── TERMUX.md                 # on-device APK build, POSIX and fish versions
├── build.bat                 # Windows launcher for the APK build
├── desktop-app/              # PySide6 desktop UI & PyInstaller Windows exe build
└── android-app/
    ├── build.gradle.kts · settings.gradle.kts · gradle.properties · gradlew(.bat)
    ├── build-android.sh · build-android.bat · build.bat
    ├── tools/
    │   └── build_android.py  # finds JDK/SDK/Gradle, picks Python, builds, numbers ARC {n}.apk, sends via tt
    ├── python-vendor/
    │   └── pyaes/            # vendored pure-Python AES
    └── app/
        ├── build.gradle.kts  # Chaquopy + pip: requests, beautifulsoup4, yt-dlp, pyrogram==2.0.106
        └── src/main/
            ├── AndroidManifest.xml
            ├── python/
            │   ├── dl.py     # copy of the root dl.py (synced by the builder at build time)
            │   └── igl.py
            ├── java/com/erawat/arc/
            │   ├── MainActivity.kt          # Compose UI, theme, ClaudeThinkingCard
            │   ├── EmbeddedEngine.kt        # calls dl.py via Chaquopy; MediaMuxer A/V merge
            │   ├── StreamService.kt         # foreground service for the stream proxy
            │   ├── GitHubUpdateChecker.kt   # new commits and release APKs
            │   └── RepoCapabilities.kt      # sources, qualities and actions shown in the UI
            └── res/                         # themes, strings, file provider paths, launcher icon
```

The app's copy of `dl.py` is overwritten from the root file on every build, so **edit the root `dl.py`** and
rebuild; never edit the copy under `android-app/app/src/main/python/`.

---

## Privacy, security & license

- **Zero telemetry.** No analytics, no crash reporting, no accounts. The app and the CLI talk only to the sites
  you ask them to reach, to GitHub for the update check, and to Telegram when you use it.
- **Private by default.** Downloads, history and Telegram values stay on your device. The local proxy listens on
  `127.0.0.1` only, so nothing on your network can reach it.
- **Credentials safety.** No API ID, hash, session or cookie is stored in the repository. Keep them in
  environment variables or in the git-ignored files above. A Telegram string session grants access to your
  account: treat it like a password and revoke it from Telegram's *Active sessions* if it leaks.
- **Use responsibly.** This tool fetches media you can already open in a browser, without the ads. Download only
  what you have the right to keep, and respect the terms of the services you use.

Released under the **MIT License**. See `LICENSE`.
