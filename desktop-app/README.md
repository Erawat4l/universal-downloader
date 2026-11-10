# Arc Desktop — the Windows app

The same downloader as the **Arc** APK, as a native Windows program. It is a thin
Qt front end around the repository's `dl.py` engine — nothing here forks or
copies the engine, so a fix to `dl.py` improves the CLI, the APK and the `.exe`
at the same time.

```
CLI      dl.py .......................... you type commands
APK      dl.py <- Chaquopy <- Kotlin ..... android-app/
EXE      dl.py <- EngineBridge <- PySide6 . desktop-app/   (this folder)
```

---

## Quickest path to Arc.exe

On your Windows laptop, from a checkout of this repository:

```bat
cd desktop-app
build.bat
```

That creates `.venv`, installs dependencies, runs a headless UI self-test, and
freeezes `dist\Arc.exe`. Double-click it to run.

Prefer to try it before building?

```bat
cd desktop-app
run-from-source.bat
```

### Requirements

| Needed | Notes |
|---|---|
| Windows 10 or 11 | 64-bit |
| Python 3.9 – 3.13 | from python.org, **"Add python.exe to PATH"** ticked |
| ~10 min | first build downloads Qt (~200 MB) |

Optional but strongly recommended — these are what make the `.exe` faster than
the APK:

```bat
winget install aria2.aria2
winget install Gyan.FFmpeg
```

---

## Why the exe is faster than the APK

Same engine, same Python, same CDNs — but the Android build cannot ship native
helper binaries. `android-app/app/build.gradle.kts` installs only
`requests`, `beautifulsoup4`, `yt-dlp`, `pyaes` and `pyrogram`. So on Android
`shutil.which("aria2c")` is `None` and **every** download falls through to
`download_file_stream()`: one TCP connection, pure Python, 512 KB chunks.

On Windows the same code path picks up the tools that are actually installed:

| | APK | Arc.exe |
|---|---|---|
| Download connections | 1 (Python loop) | **16** (`aria2c -x16 -s16`) |
| Video + audio merge | Kotlin `MediaMuxer` | **ffmpeg** (`-c copy`) |
| NetMirror AES-GCM streams | ✗ no crypto backend | ✓ `pycryptodomex` |
| Instant streaming | proxy → external player | proxy → **VLC** |
| Restart-safe Library | ✗ held in `remember{}` | ✓ JSON on disk |

Extraction and resolve speed is unchanged — that part is network-bound and runs
the same Python either way. Settings → Accelerators shows exactly what your
machine has, and how many connections you are getting.

---

## What it does

**Home** — paste a link or type a title. A link resolves directly; words search
the catalog. Supports YouTube and 1000+ sites, NetMirror movies/series, Telegram
(including private channels with your own session), magnet links, MediaFire /
Terabox / Drive, and study PDFs.

**Activity** — live percent, speed, ETA and bytes for each job, polled every
400 ms (the same cadence as the APK). Pause, Resume, Cancel, Dismiss. A job shows
"Fetching" with an indeterminate bar until the first bytes land, so a slow CDN
handshake does not look frozen.

**Library** — finished files, persisted across restarts, plus a scan of your
download folder so files the CLI already fetched show up too. Play, open folder,
delete.

**Settings** — save location, default video/audio quality, notifications,
VLC preference, Telegram session, accelerator status, GitHub update check.

**Execution trace** — every search and resolve shows a card with elapsed time,
the steps taken, and the engine's own log output (already sanitized by
`sanitize_app_logs()`, which strips internal hostnames). Copy it when reporting a
problem.

---

## How it is put together

```
desktop-app/
├── arc_desktop/
│   ├── app.py                entry point; --selftest builds the UI headless
│   ├── engine_bridge.py      loads dl.py in-process, serialised call queue
│   ├── platform_adapter.py   Android-only side effects -> Windows equivalents
│   ├── store.py              settings + download history as JSON in %APPDATA%
│   ├── theme.py              the APK's monochrome design tokens as Qt QSS
│   └── ui/
│       ├── widgets.py        Card, ChipBar, ThinkingCard, JobCard, AsyncImage
│       ├── pages.py          Home, Search, Analysis, Activity, Library, Settings
│       └── main_window.py    state, job queue, 400 ms poll
├── arc.spec                  PyInstaller recipe (bundles dl.py + igl.py)
├── build_exe.py              prerequisite checks -> selftest -> freeze
├── build.bat                 one-click Windows build
├── run-from-source.bat       run without building
└── requirements.txt
```

### The engine is loaded, never copied

`engine_bridge.py` finds `dl.py` (repo root, `engine/` inside the frozen bundle,
or `ARC_ENGINE_PATH`), imports it with `importlib.util.spec_from_file_location`,
and calls the same machine interface the APK uses:

```python
bridge.request({"action": "resolve", "url": url, "quality": "best"})
```

Actions: `capabilities`, `search`, `resolve`, `download`, `progress`, `cancel`,
`telegram_check`, `igl`, `igl_resolve`, `stream_proxy`.

Two things the bridge adds on top:

* **Serialised calls.** `run_app_json_request` captures `sys.stdout` per thread
  (see `inherit_capture` in `dl.py`), and the bridge still holds a lock so two
  actions can never interleave their logs.
* **Desktop adaptation** (`platform_adapter.apply`) — it patches three
  Android-only behaviours instead of forking the engine:

  | dl.py function | On Android | On Windows |
  |---|---|---|
  | `sync_to_sdcard_if_needed` | `adb push` to `/sdcard/Download` | disabled |
  | `print_adm_link` | HTTP server on `:8080` + `v1.mp4` hardlinks | disabled |
  | `launch_player` | `adb` / `am start` | VLC or the default player |

  It also registers a `com.erawat.arc.EmbeddedEngine` module backed by ffmpeg, so
  the engine's `EmbeddedEngine.mux(video, audio, out)` call merges HD video and
  audio on desktop exactly as `MediaMuxer` does on Android.

---

## Build options

```bat
python build_exe.py                    REM one-file dist\Arc.exe (default)
python build_exe.py --onedir           REM dist\Arc\ folder: starts seconds faster
python build_exe.py --run-selftest     REM prove the UI builds before freezing
python build_exe.py --skip-checks      REM just freeze
python -m arc_desktop --selftest       REM headless UI check on its own
```

**One file vs one directory.** `Arc.exe` alone is easiest to share, but PyInstaller
unpacks it to a temp folder on every launch (~3–6 s on an SSD). The `--onedir`
build starts immediately — zip the whole `dist\Arc` folder to distribute it.

CI publishes both. See `.github/workflows/build-windows-exe.yml`.

---

## Notes and honest limits

* **Pause on a subprocess download** uses `SIGSTOP` on POSIX. Windows cannot
  suspend a child process that way, so pausing an `aria2c`/yt-dlp download is
  reported as unsupported rather than faked. In-process downloads (the Python
  chunk loop) pause normally.
* **`aria2c` bypasses progress hooks.** yt-dlp fires no progress callbacks when an
  external downloader runs, so on that path the bar updates only at start and end.
  Uninstall aria2c if you prefer a smooth bar over raw speed.
* **Magnet links need aria2c.** Without it the engine says so and points at
  `winget install aria2.aria2` instead of failing silently.
* **The DDL bypasser** (`MediaFire`, `Terabox`, Drive) is an optional external
  module the engine looks for at `~/Projects/Link-Bypasser-Bot`. It is not part of
  this repository, so those links resolve only if you have it installed.
* **Antivirus.** Unsigned PyInstaller builds sometimes trigger SmartScreen.
  Click "More info" → "Run anyway", or build with `--onedir` (fewer heuristics
  fire on a folder than on a packed single exe). UPX is disabled in `arc.spec`
  for the same reason.
* Download only what you have the right to keep.

---

## See also

* [`../README.md`](../README.md) — the engine, the CLI, and how the three front ends fit together
* [`../android-app/README.md`](../android-app/README.md) — building the APK
* [`../TERMUX.md`](../TERMUX.md) — building the APK on a tablet
