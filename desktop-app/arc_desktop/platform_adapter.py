"""Adapts the shared dl.py engine to a desktop OS.

dl.py was written for Termux and then embedded in an APK, so a few code paths
assume Android. Rather than fork the engine (the repository's rule is that root
dl.py is the single source of truth), this module patches the Android-only side
effects at runtime and supplies the desktop equivalents.

Everything here is pure Python with no Qt import, so it is testable headless.
"""

from __future__ import annotations

import os
import shutil
import subprocess
import sys
import types
from pathlib import Path

IS_WINDOWS = sys.platform.startswith("win")
IS_MACOS = sys.platform == "darwin"

# Executables that make the desktop build genuinely faster than the APK.
ACCELERATORS = ("aria2c", "ffmpeg", "yt-dlp", "ffprobe", "node")


def which(name: str) -> str | None:
    """Locate an executable, also checking the usual Windows install spots."""
    found = shutil.which(name)
    if found:
        return found
    if not IS_WINDOWS:
        return None
    exe = f"{name}.exe"
    local = os.environ.get("LOCALAPPDATA", "")
    candidates = []
    if local:
        candidates += [
            Path(local) / "Microsoft" / "WinGet" / "Links" / exe,
            Path(local) / "Programs" / name / "bin" / exe,
        ]
    for base in (os.environ.get("ProgramFiles", r"C:\Program Files"),
                 os.environ.get("ProgramFiles(x86)", r"C:\Program Files (x86)")):
        if not base:
            continue
        candidates += [
            Path(base) / name / "bin" / exe,
            Path(base) / name / exe,
            Path(base) / "VideoLAN" / "VLC" / exe,
        ]
    for candidate in candidates:
        if candidate.is_file():
            return str(candidate)
    return None


def detect_accelerators() -> dict:
    """Report which optional helpers are installed.

    The APK bundles none of these, which is why every download there falls back
    to the single-connection Python loop in download_file_stream(). On Windows,
    aria2c switches the engine onto its 16-connection path and ffmpeg enables
    real video+audio merging.
    """
    return {name: which(name) for name in ACCELERATORS}


def find_vlc() -> str | None:
    if not IS_WINDOWS:
        return which("vlc")
    for base in (os.environ.get("ProgramFiles", r"C:\Program Files"),
                 os.environ.get("ProgramFiles(x86)", r"C:\Program Files (x86)")):
        if not base:
            continue
        candidate = Path(base) / "VideoLAN" / "VLC" / "vlc.exe"
        if candidate.is_file():
            return str(candidate)
    return which("vlc")


def default_download_dir() -> str:
    """The user's real Downloads folder, falling back to something writable."""
    home = Path(os.path.expanduser("~"))
    candidates = [home / "Downloads", home / "downloads"]
    if IS_WINDOWS:
        profile = os.environ.get("USERPROFILE")
        if profile:
            candidates.insert(0, Path(profile) / "Downloads")
        onedrive = os.environ.get("OneDriveConsumer") or os.environ.get("OneDrive")
        if onedrive:
            candidates.insert(0, Path(onedrive) / "Downloads")
    for candidate in candidates:
        try:
            candidate.mkdir(parents=True, exist_ok=True)
            probe = candidate / ".arc_write_test"
            probe.write_text("ok", encoding="utf-8")
            probe.unlink()
            return str(candidate)
        except Exception:
            continue
    fallback = home / "ArcDownloads"
    try:
        fallback.mkdir(parents=True, exist_ok=True)
    except Exception:
        return str(home)
    return str(fallback)


def open_path(path: str) -> bool:
    """Open a file or folder with the OS default handler."""
    if not path:
        return False
    try:
        if IS_WINDOWS:
            os.startfile(path)  # type: ignore[attr-defined]
            return True
        if IS_MACOS:
            subprocess.Popen(["open", path])
            return True
        subprocess.Popen(["xdg-open", path])
        return True
    except Exception:
        return False


def reveal_in_folder(path: str) -> bool:
    """Open the containing folder with the file selected."""
    if not path:
        return False
    try:
        if IS_WINDOWS:
            subprocess.Popen(["explorer", "/select,", os.path.normpath(path)])
            return True
        if IS_MACOS:
            subprocess.Popen(["open", "-R", path])
            return True
        subprocess.Popen(["xdg-open", str(Path(path).parent)])
        return True
    except Exception:
        return False


def play_stream(proxy_url: str, direct_url: str = "", title: str = "") -> tuple[bool, str]:
    """Hand a stream to VLC when installed, otherwise the default player.

    Always prefers the local proxy URL: the raw CDN link 403s for the hosts that
    check Referer, which is the whole reason dl.py runs a proxy.
    """
    target = proxy_url or direct_url
    if not target:
        return False, "No stream URL to play."
    vlc = find_vlc()
    try:
        if vlc:
            command = [vlc]
            if title:
                command += [f"--meta-title={title}"]
            command.append(target)
            subprocess.Popen(command, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            return True, f"Streaming in VLC: {vlc}"
        if IS_WINDOWS:
            os.startfile(target)  # type: ignore[attr-defined]
        elif IS_MACOS:
            subprocess.Popen(["open", target])
        else:
            subprocess.Popen(["xdg-open", target])
        return True, "Streaming in the default player."
    except Exception as error:
        return False, f"Could not launch a player: {error}"


def _ffmpeg_mux(video: str, audio: str, output: str) -> bool:
    """Stream-copy video+audio into one file. Same job as Android's MediaMuxer."""
    ffmpeg = which("ffmpeg")
    if not ffmpeg or not (os.path.exists(video) and os.path.exists(audio)):
        return False
    try:
        Path(output).parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(
            [ffmpeg, "-y", "-i", video, "-i", audio,
             "-c", "copy", "-movflags", "+faststart", output],
            check=True, capture_output=True,
        )
        return os.path.exists(output) and os.path.getsize(output) > 0
    except Exception:
        return False


def install_desktop_muxer() -> bool:
    """Register a desktop stand-in for the APK's com.erawat.arc.EmbeddedEngine.

    dl.py's HD path does `from com.erawat.arc import EmbeddedEngine` and calls
    `EmbeddedEngine.mux(video, audio, out)`. Inside the APK that is Kotlin and
    MediaMuxer. On a desktop that import fails, and the engine then kept the
    video track and silently discarded the audio. Providing the same module name
    backed by ffmpeg lets the unmodified engine merge properly here.
    """
    if "com.erawat.arc" in sys.modules:
        return True
    try:
        engine = types.ModuleType("com.erawat.arc.EmbeddedEngine")
        engine.mux = staticmethod(_ffmpeg_mux)  # type: ignore[attr-defined]

        arc = types.ModuleType("com.erawat.arc")
        arc.EmbeddedEngine = engine  # type: ignore[attr-defined]
        arc.__path__ = []  # type: ignore[attr-defined]

        erawat = types.ModuleType("com.erawat")
        erawat.arc = arc  # type: ignore[attr-defined]
        erawat.__path__ = []  # type: ignore[attr-defined]

        com = types.ModuleType("com")
        com.erawat = erawat  # type: ignore[attr-defined]
        com.__path__ = []  # type: ignore[attr-defined]

        sys.modules["com"] = com
        sys.modules["com.erawat"] = erawat
        sys.modules["com.erawat.arc"] = arc
        sys.modules["com.erawat.arc.EmbeddedEngine"] = engine
        return True
    except Exception:
        return False


def apply(dl_module) -> dict:
    """Patch Android-only side effects out of an imported dl.py.

    Returns a report of what was changed, which the Settings tab shows so the
    user can see exactly how this build differs from the APK.
    """
    applied: dict[str, str] = {}

    # adb push to /sdcard/Download after every file: pointless off Android, and
    # it spawned `adb devices` on each download.
    def _no_sync(file_path):
        return file_path

    dl_module.sync_to_sdcard_if_needed = _no_sync
    applied["sync_to_sdcard_if_needed"] = "disabled (Android /sdcard copy)"

    # print_adm_link() starts an HTTP server on :8080 and hardlinks the finished
    # file as v1.mp4/v2.mp4 next to it - a Termux-era ADM trick that would litter
    # a Windows Downloads folder with duplicates.
    def _no_adm_link(file_path):
        return None

    dl_module.print_adm_link = _no_adm_link
    dl_module.start_local_http_server = lambda: None
    applied["print_adm_link"] = "disabled (no v1.mp4 hardlinks, no :8080 server)"

    # launch_player() shells out to adb / Android `am start`. The desktop plays
    # through VLC or the default handler instead.
    def _desktop_launch(target_uri, player_choice="vlc", custom_headers=None, title=None):
        proxy = target_uri
        try:
            if str(target_uri).startswith(("http://", "https://")):
                proxy = dl_module.start_local_stream_proxy(target_uri, custom_headers=custom_headers)
        except Exception:
            proxy = target_uri
        ok, message = play_stream(proxy, target_uri, title or "")
        print(message)
        return ok

    dl_module.launch_player = _desktop_launch
    applied["launch_player"] = "desktop VLC / default player"

    if install_desktop_muxer():
        applied["com.erawat.arc.EmbeddedEngine"] = "ffmpeg muxer registered"

    return applied
