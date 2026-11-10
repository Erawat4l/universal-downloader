# -*- mode: python ; coding: utf-8 -*-
"""PyInstaller spec for Arc Desktop.

Build with:
    pyinstaller arc.spec --noconfirm

dl.py and igl.py are bundled as data files under engine/ rather than frozen as
importable modules, because the app loads them at runtime through
importlib.util.spec_from_file_location(). That keeps the repository's rule
intact - root dl.py is the single source of truth - and means rebuilding the exe
after an engine fix needs no code change here.
"""

import os
import sys
from pathlib import Path

from PyInstaller.utils.hooks import collect_submodules, collect_data_files

HERE = Path(SPECPATH).resolve()          # desktop-app/
REPO = HERE.parent                        # repository root

# ---------------------------------------------------------------- engine files
ENGINE_DATA = []
for name in ("dl.py", "igl.py"):
    source = REPO / name
    if source.is_file():
        ENGINE_DATA.append((str(source), "engine"))
    else:
        raise SystemExit(f"arc.spec: {source} is missing. Run this from a full checkout.")

# --------------------------------------------------------------- hidden imports
hidden = [
    # Engine runtime
    "requests", "urllib3", "bs4", "json", "hashlib", "signal",
    # Media extraction: yt-dlp discovers its extractors dynamically
    "yt_dlp", "yt_dlp.utils", "yt_dlp.extractor",
    # Telegram
    "pyrogram", "pyaes", "tgcrypto",
    # NetMirror AES-GCM stream decryption (either backend is enough)
    "Cryptodome", "Cryptodome.Cipher", "Cryptodome.Cipher.AES",
    "cryptography", "cryptography.hazmat.primitives.ciphers.aead",
    # Qt: QtNetwork fetches posters, Svg is used by some Qt styles
    "PySide6.QtCore", "PySide6.QtGui", "PySide6.QtWidgets", "PySide6.QtNetwork",
]
for package in ("yt_dlp", "pyrogram"):
    try:
        hidden += collect_submodules(package)
    except Exception:
        pass
hidden = sorted(set(hidden))

datas = list(ENGINE_DATA)
for package in ("yt_dlp",):
    try:
        datas += collect_data_files(package)
    except Exception:
        pass

# Keep the exe small and predictable: nothing here is used by the app.
excludes = [
    "tkinter", "_tkinter", "matplotlib", "numpy", "pandas", "scipy", "PIL",
    "IPython", "jupyter", "notebook", "pytest", "setuptools", "pip",
    "PySide6.QtQml", "PySide6.QtQuick", "PySide6.QtQuickWidgets",
    "PySide6.QtWebEngineCore", "PySide6.QtWebEngineWidgets", "PySide6.Qt3DCore",
    "PySide6.QtMultimedia", "PySide6.QtBluetooth", "PySide6.QtNfc",
    "PySide6.QtPositioning", "PySide6.QtSensors", "PySide6.QtSerialPort",
    "PySide6.QtSql", "PySide6.QtTest", "PySide6.QtCharts", "PySide6.QtDataVisualization",
    "PySide6.QtPdf", "PySide6.QtRemoteObjects", "PySide6.QtScxml", "PySide6.QtStateMachine",
    "PySide6.QtTextToSpeech", "PySide6.QtUiTools", "PySide6.QtDesigner",
    "PySide6.QtHelp", "PySide6.QtPrintSupport",
]

icon_path = HERE / "assets" / "arc.ico"

# One file is easiest to share; one directory starts several seconds faster.
# build_exe.py sets this from its --onefile / --onedir flag (default: onefile).
ONEFILE = os.environ.get("ARC_ONEFILE", "1") != "0"

a = Analysis(
    [str(HERE / "arc_desktop" / "app.py")],
    pathex=[str(HERE), str(REPO)],
    binaries=[],
    datas=datas,
    hiddenimports=hidden,
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=excludes,
    noarchive=False,
    optimize=0,
)

pyz = PYZ(a.pure)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries if ONEFILE else [],
    a.datas if ONEFILE else [],
    [],
    name="Arc",
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=False,                       # UPX trips antivirus heuristics on Qt DLLs
    upx_exclude=[],
    runtime_tmpdir=None if not ONEFILE else None,
    console=False,                   # windowed: dl.py's _NullStream shim handles None streams
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
    icon=str(icon_path) if icon_path.is_file() else None,
    version=str(HERE / "assets" / "version_info.txt") if (HERE / "assets" / "version_info.txt").is_file() else None,
)

if not ONEFILE:
    coll = COLLECT(
        exe,
        a.binaries,
        a.datas,
        strip=False,
        upx=False,
        upx_exclude=[],
        name="Arc",
    )
