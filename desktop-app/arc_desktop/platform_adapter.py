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


def bundled_bin_dirs() -> list[Path]:
    """Candidate folders holding binaries shipped inside the frozen bundle."""
    dirs: list[Path] = []
    meipass = getattr(sys, "_MEIPASS", None)
    if meipass:
        dirs.append(Path(meipass) / "bin")
        dirs.append(Path(meipass))
    if getattr(sys, "frozen", False):
        exe_dir = Path(sys.executable).resolve().parent
        dirs.append(exe_dir / "bin")
        dirs.append(exe_dir)
    return dirs


def which(name: str) -> str | None:
    """Locate an executable across bundled bin/, PATH, and Windows install folders."""
    exe = f"{name}.exe" if IS_WINDOWS and not name.lower().endswith(".exe") else name
    for folder in bundled_bin_dirs():
        cand = folder / exe
        if cand.is_file():
            return str(cand)

    found = shutil.which(name)
    if found:
        return found
    if not IS_WINDOWS:
        return None

    local = os.environ.get("LOCALAPPDATA", "")
    userprofile = os.environ.get("USERPROFILE", "")
    programdata = os.environ.get("ProgramData", r"C:\ProgramData")
    base_name = name.lower().removesuffix(".exe")
    candidates: list[Path] = []
    if local:
        candidates += [
            Path(local) / "Microsoft" / "WinGet" / "Links" / exe,
            Path(local) / "Programs" / base_name / "bin" / exe,
            Path(local) / "Programs" / base_name / exe,
        ]
    if userprofile:
        candidates += [
            Path(userprofile) / "scoop" / "shims" / exe,
            Path(userprofile) / "scoop" / "apps" / base_name / "current" / "bin" / exe,
            Path(userprofile) / "scoop" / "apps" / base_name / "current" / exe,
        ]
    choco_pkg_names = {
        "aria2c": ("aria2", "aria2c"),
        "ffprobe": ("ffmpeg", "ffprobe"),
    }.get(base_name, (base_name,))
    for choco_name in choco_pkg_names:
        choco_pkg = Path(programdata) / "chocolatey" / "lib" / choco_name
        if choco_pkg.is_dir():
            for match in choco_pkg.glob(f"**/{exe}"):
                candidates.append(match)
    candidates.append(Path(programdata) / "chocolatey" / "bin" / exe)

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
        try:
            if candidate.is_file() and candidate.stat().st_size > 100_000:
                return str(candidate)
        except OSError:
            continue
    for candidate in candidates:
        if candidate.is_file():
            return str(candidate)
    return None


def register_binary_paths_on_env() -> dict[str, str]:
    """Ensure bundled or system aria2c/ffmpeg are discoverable via PATH & dl hooks."""
    found: dict[str, str] = {}
    extra_dirs: list[str] = []
    for name in ("aria2c", "ffmpeg", "ffprobe", "yt-dlp", "node"):
        path = which(name)
        if path:
            found[name] = path
            parent = str(Path(path).resolve().parent)
            if parent not in extra_dirs:
                extra_dirs.append(parent)
    if extra_dirs:
        cur_path = os.environ.get("PATH", "")
        cur_parts = cur_path.split(os.pathsep) if cur_path else []
        missing = [d for d in extra_dirs if d not in cur_parts]
        if missing:
            os.environ["PATH"] = os.pathsep.join(missing + cur_parts)
    if "aria2c" in found:
        os.environ["ARIA2C_PATH"] = found["aria2c"]
    return found


def detect_accelerators() -> dict:
    """Report which optional helpers are installed."""
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


def play_stream(
    proxy_url: str,
    direct_url: str = "",
    title: str = "",
    audio_url: str = "",
    headers: dict | None = None,
    prefer_vlc: bool = True,
) -> tuple[bool, str]:
    """Hand a stream to VLC when requested/installed, otherwise default handler."""
    target = proxy_url or direct_url
    if not target:
        return False, "No stream URL to play."
    vlc = find_vlc() if prefer_vlc else None
    try:
        if vlc:
            command = [vlc]
            if title:
                command += [f"--meta-title={title}"]
            if audio_url and audio_url != target:
                command += [f"--input-slave={audio_url}"]
            if headers and "127.0.0.1" not in target:
                ua = headers.get("User-Agent") or headers.get("user-agent")
                ref = headers.get("Referer") or headers.get("referer")
                if ua:
                    command += [f"--http-user-agent={ua}"]
                if ref:
                    command += [f"--http-referrer={ref}"]
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


def extract_browser_youtube_cookies(browser: str) -> tuple[bool, str, str]:
    """Read YouTube/Google cookies from a local browser in Netscape cookie text format."""
    name = (browser or "").strip().lower()
    if name not in ("chrome", "edge", "firefox", "brave", "opera"):
        return False, "", f"Unsupported browser: {browser}"
    try:
        from yt_dlp.cookies import extract_cookies_from_browser
        jar = extract_cookies_from_browser(name)
        lines = ["# Netscape HTTP Cookie File"]
        count = 0
        for cookie in jar:
            dom = str(getattr(cookie, "domain", "") or "")
            if not any(h in dom.lower() for h in ("youtube.com", "google.com", "youtu.be", "ytimg.com")):
                continue
            include_sub = "TRUE" if dom.startswith(".") else "FALSE"
            path = str(getattr(cookie, "path", "/") or "/")
            secure = "TRUE" if getattr(cookie, "secure", False) else "FALSE"
            expires = str(int(getattr(cookie, "expires", 0) or 0))
            cname = str(getattr(cookie, "name", "") or "")
            cval = str(getattr(cookie, "value", "") or "")
            if cname:
                lines.append("\t".join([dom, include_sub, path, secure, expires, cname, cval]))
                count += 1
        if count == 0:
            return False, "", f"No YouTube/Google session cookies found in {name.title()}."
        return True, "\n".join(lines), f"Imported {count} cookies from {name.title()}."
    except Exception as error:
        return False, "", f"Could not read {name.title()} cookies: {error}"


def _ffmpeg_mux(video: str, audio: str, output: str) -> bool:
    """Stream-copy video+audio into one file. Same job as Android's MediaMuxer."""
    ffmpeg = which("ffmpeg")
    if not ffmpeg or not (os.path.exists(video) and os.path.exists(audio)):
        return False
    flags = getattr(subprocess, "CREATE_NO_WINDOW", 0) if IS_WINDOWS else 0
    cmds = [
        [ffmpeg, "-y", "-i", video, "-i", audio,
         "-c", "copy", "-movflags", "+faststart", output],
        [ffmpeg, "-y", "-i", video, "-i", audio,
         "-c:v", "copy", "-c:a", "aac", "-b:a", "192k",
         "-movflags", "+faststart", output],
    ]
    Path(output).parent.mkdir(parents=True, exist_ok=True)
    for cmd in cmds:
        try:
            subprocess.run(cmd, check=True, capture_output=True, creationflags=flags)
            if os.path.exists(output) and os.path.getsize(output) > 0:
                return True
        except Exception:
            try:
                if os.path.exists(output) and os.path.getsize(output) == 0:
                    os.remove(output)
            except OSError:
                pass
    return False


def install_desktop_muxer() -> bool:
    """Register a desktop stand-in for the APK's com.erawat.arc.EmbeddedEngine."""
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
    """Patch Android-only side effects out of an imported dl.py."""
    applied: dict[str, str] = {}

    found_bins = register_binary_paths_on_env()
    if "aria2c" in found_bins:
        try:
            dl_module.set_aria2c_path(found_bins["aria2c"])
            applied["aria2c"] = found_bins["aria2c"]
        except Exception:
            pass
    if "ffmpeg" in found_bins:
        applied["ffmpeg"] = found_bins["ffmpeg"]

    def _no_sync(file_path):
        return file_path

    dl_module.sync_to_sdcard_if_needed = _no_sync
    applied["sync_to_sdcard_if_needed"] = "disabled (Android /sdcard copy)"

    def _no_adm_link(file_path):
        return None

    dl_module.print_adm_link = _no_adm_link
    dl_module.start_local_http_server = lambda: None
    applied["print_adm_link"] = "disabled (no v1.mp4 hardlinks, no :8080 server)"

    # Fix Windows local drive paths (e.g. C:\...) in dl.py's stream proxy.
    orig_start_proxy = getattr(dl_module, "start_local_stream_proxy", None)
    if callable(orig_start_proxy):
        import urllib.parse

        def _desktop_start_proxy(target_url, custom_headers=None, port=8765):
            raw = str(target_url or "").strip()
            clean = raw[7:] if raw.startswith("file://") else raw
            if clean and os.path.isabs(clean) and os.path.isfile(clean) and not clean.startswith("/"):
                orig_start_proxy("")
                active_port = getattr(dl_module, "LOCAL_PROXY_PORT", port) or port
                sub_name = os.path.basename(clean) or "stream.mp4"
                return f"http://127.0.0.1:{active_port}/local/{urllib.parse.quote(sub_name)}?path={urllib.parse.quote(clean)}"
            return orig_start_proxy(target_url, custom_headers=custom_headers, port=port)

        dl_module.start_local_stream_proxy = _desktop_start_proxy
        applied["start_local_stream_proxy"] = "Windows drive-path local proxy enabled"

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
    applied["launch_player"] = "desktop ARC Player / VLC / default player"

    if install_desktop_muxer():
        applied["com.erawat.arc.EmbeddedEngine"] = "ffmpeg muxer registered"

    return applied
