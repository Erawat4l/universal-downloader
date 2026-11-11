#!/usr/bin/env python3
"""Build Arc.exe on Windows.

    python build_exe.py                 # one-file Arc.exe (default)
    python build_exe.py --onedir        # folder build: starts several seconds faster
    python build_exe.py --run-selftest  # also build the GUI headless first
    python build_exe.py --skip-checks   # trust the environment, just build

The script is deliberately boring: verify prerequisites, prove the UI still
constructs, freeze it, then report the exact output path and size. Every check
prints what it found so a failed build says why instead of dumping a traceback.
"""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent
DIST = HERE / "dist"
BUILD = HERE / "build"

REQUIRED_MODULES = ["PySide6", "requests", "bs4", "yt_dlp"]
OPTIONAL_MODULES = {
    "pyrogram": "Telegram private channels",
    "tgcrypto": "faster Telegram crypto",
    "Cryptodome": "NetMirror AES-GCM stream decryption",
    "cryptography": "NetMirror AES-GCM (fallback backend)",
}
OPTIONAL_BINARIES = {
    "aria2c": "16-connection downloads (the APK is stuck on 1)",
    "ffmpeg": "merging separate HD video + audio tracks",
}


def log(message: str = "") -> None:
    print(message, flush=True)


def run(command: list[str], **kwargs) -> subprocess.CompletedProcess:
    log("  $ " + " ".join(str(part) for part in command))
    return subprocess.run([str(part) for part in command], **kwargs)


def check_python() -> bool:
    version = sys.version_info
    log(f"[1/5] Python {version.major}.{version.minor}.{version.micro} at {sys.executable}")
    if sys.platform != "win32":
        log("      WARNING: PyInstaller cannot cross-compile. Run this on Windows to get an .exe.")
    if version < (3, 9):
        log("      FAIL: Python 3.9 or newer is required.")
        return False
    return True


def check_modules(install: bool) -> bool:
    log("[2/5] Python packages")
    missing = []
    for name in REQUIRED_MODULES:
        found = _module_version(name)
        if found:
            log(f"      ok   {name} {found}")
        else:
            log(f"      MISS {name}")
            missing.append(name)
    for name, purpose in OPTIONAL_MODULES.items():
        found = _module_version(name)
        log(f"      {'ok  ' if found else 'opt '} {name} {found or '-'}  ({purpose})")

    if missing and install:
        log(f"      installing missing packages: {', '.join(missing)}")
        result = run([sys.executable, "-m", "pip", "install", "-r", HERE / "requirements.txt"])
        if result.returncode != 0:
            log("      FAIL: pip install did not succeed.")
            return False
        missing = [name for name in REQUIRED_MODULES if not _module_version(name)]

    if missing:
        log(f"      FAIL: missing {', '.join(missing)}.")
        log(f"      Fix:  python -m pip install -r {HERE / 'requirements.txt'}")
        return False
    return True


def _module_version(name: str) -> str:
    try:
        module = __import__(name)
    except Exception:
        return ""
    for attribute in ("__version__", "VERSION", "version"):
        value = getattr(module, attribute, None)
        if isinstance(value, str):
            return value
        if isinstance(value, tuple):
            return ".".join(str(part) for part in value)
    return "installed"


def check_binaries() -> None:
    log("[3/5] Optional accelerators (not required, but they make downloads faster)")
    for name, purpose in OPTIONAL_BINARIES.items():
        path = shutil.which(name)
        log(f"      {'ok  ' if path else 'opt '} {name:8} {path or 'not on PATH'}  ({purpose})")
    if not shutil.which("aria2c") or not shutil.which("ffmpeg"):
        log("      tip: winget install aria2.aria2 ; winget install Gyan.FFmpeg")


def check_engine() -> bool:
    log("[4/5] Engine files that get bundled")
    ok = True
    for name in ("dl.py", "igl.py"):
        path = REPO / name
        if path.is_file():
            size = path.stat().st_size
            lines = sum(1 for _ in path.open(encoding="utf-8", errors="replace"))
            log(f"      ok   {name} ({lines:,} lines, {size / 1024:.0f} KB)")
        else:
            log(f"      FAIL {name} not found at {path}")
            ok = False
    if not ok:
        log("      The .exe bundles the repository's engine. Run this from a full checkout.")
    return ok


def run_selftest() -> bool:
    log("      building the GUI headless to prove it still constructs...")
    environment = dict(os.environ)
    environment["QT_QPA_PLATFORM"] = "offscreen"
    environment["PYTHONPATH"] = os.pathsep.join(
        [str(HERE), environment.get("PYTHONPATH", "")]).rstrip(os.pathsep)
    result = subprocess.run(
        [sys.executable, "-m", "arc_desktop", "--selftest"],
        cwd=str(HERE), env=environment, capture_output=True, text=True, timeout=300,
    )
    for line in (result.stdout or "").splitlines():
        log(f"      | {line}")
    for line in (result.stderr or "").splitlines()[-25:]:
        log(f"      ! {line}")
    if result.returncode != 0:
        log("      FAIL: the self-test did not pass, so the frozen build would be broken too.")
        return False
    log("      ok   self-test passed")
    return True


def build(onefile: bool, clean: bool) -> int:
    log(f"[5/5] Freezing with PyInstaller ({'one file' if onefile else 'one directory'})")
    try:
        import PyInstaller  # noqa: F401
    except ImportError:
        log("      PyInstaller is not installed. Installing it now...")
        if run([sys.executable, "-m", "pip", "install", "pyinstaller>=6.6"]).returncode != 0:
            log("      FAIL: could not install PyInstaller.")
            return 1

    if clean:
        for folder in (DIST, BUILD):
            if folder.exists():
                shutil.rmtree(folder, ignore_errors=True)
                log(f"      removed stale {folder.name}/")

    environment = dict(os.environ)
    environment["ARC_ONEFILE"] = "1" if onefile else "0"
    command = [sys.executable, "-m", "PyInstaller", str(HERE / "arc.spec"),
               "--noconfirm", "--distpath", str(DIST), "--workpath", str(BUILD)]
    log("  $ " + " ".join(command))
    result = subprocess.run(command, cwd=str(HERE), env=environment)
    return result.returncode


def report(onefile: bool) -> bool:
    log("")
    log("=" * 68)
    if onefile:
        artifact = DIST / "Arc.exe"
        if artifact.is_file():
            log(f"  BUILD OK   {artifact}")
            log(f"             {artifact.stat().st_size / (1024 * 1024):.1f} MB, single file,")
            log(f"             double-click to run. First launch unpacks to temp, so it")
            log(f"             starts a few seconds slower than the directory build.")
        else:
            log("  BUILD FAILED: dist/Arc.exe was not produced.")
            return False
    else:
        folder = DIST / "Arc"
        artifact = folder / "Arc.exe"
        if artifact.is_file():
            total = sum(f.stat().st_size for f in folder.rglob("*") if f.is_file())
            log(f"  BUILD OK   {artifact}")
            log(f"             {total / (1024 * 1024):.1f} MB across {len(list(folder.rglob('*')))} entries.")
            log(f"             Ship the whole Arc folder (zip it). Starts fastest.")
        else:
            log("  BUILD FAILED: dist/Arc/Arc.exe was not produced.")
            return False
    log("=" * 68)
    log("")
    log("  Install aria2c and ffmpeg next to it (or on PATH) for full speed:")
    log("    winget install aria2.aria2")
    log("    winget install Gyan.FFmpeg")
    log("")
    return True


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Build Arc.exe for Windows")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--onefile", action="store_true", default=True,
                      help="single Arc.exe (default)")
    mode.add_argument("--onedir", dest="onefile", action="store_false",
                      help="folder build; starts faster, ship as a zip")
    parser.add_argument("--no-install", action="store_true",
                        help="do not pip install missing packages")
    parser.add_argument("--run-selftest", action="store_true",
                        help="build the GUI headless before freezing (recommended)")
    parser.add_argument("--skip-checks", action="store_true",
                        help="skip prerequisite checks and freeze immediately")
    parser.add_argument("--no-clean", action="store_true",
                        help="keep the previous build/ and dist/ folders")
    args = parser.parse_args(argv)

    started = time.time()
    log("")
    log("Arc Desktop - Windows build")
    log(f"repository : {REPO}")
    log("")

    if not args.skip_checks:
        if not check_python():
            return 1
        if not check_modules(install=not args.no_install):
            return 1
        check_binaries()
        if not check_engine():
            return 1
        if args.run_selftest and not run_selftest():
            return 1
    else:
        log("[checks skipped]")

    code = build(args.onefile, clean=not args.no_clean)
    if code != 0:
        log("")
        log(f"PyInstaller exited with code {code}.")
        return code

    if not report(args.onefile):
        return 1
    log(f"  total time: {time.time() - started:.0f}s")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
