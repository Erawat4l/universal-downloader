"""Application entry point for Arc Desktop.

    python -m arc_desktop            normal start
    python -m arc_desktop --selftest build the whole UI headless, then exit 0/1

`--selftest` exists so CI can prove the GUI still constructs after any change,
without a display: it loads the real engine, applies the desktop adapter, builds
every page, and exercises the non-network code paths.
"""

from __future__ import annotations

import argparse
import os
import sys
import time
import traceback
from pathlib import Path
from typing import Optional

from . import __version__
from .engine_bridge import EngineBridge, EngineError
from .store import Store


def _bootstrap_paths() -> None:
    """Make `arc_desktop` importable from a source checkout or a frozen exe."""
    if getattr(sys, "frozen", False):
        return
    here = Path(__file__).resolve().parent.parent
    if str(here) not in sys.path:
        sys.path.insert(0, str(here))


def _qt_high_dpi() -> None:
    os.environ.setdefault("QT_ENABLE_HIGHDPI_SCALING", "1")
    os.environ.setdefault("QT_AUTO_SCREEN_SCALE_FACTOR", "1")


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        prog="arc-desktop", description="Arc - Universal Downloader for Windows")
    parser.add_argument("--selftest", action="store_true",
                        help="build the UI headless and exit (used by CI)")
    parser.add_argument("--engine", default=None,
                        help="path to dl.py (default: auto-detect)")
    parser.add_argument("--download-dir", default=None,
                        help="override the download folder")
    parser.add_argument("--version", action="version", version=f"Arc Desktop {__version__}")
    return parser.parse_args(argv)


def build_window(args: argparse.Namespace):
    """Create QApplication, load the engine and construct MainWindow."""
    _qt_high_dpi()
    from PySide6.QtCore import Qt
    from PySide6.QtGui import QIcon
    from PySide6.QtWidgets import QApplication, QMessageBox

    from . import theme
    from .ui.main_window import MainWindow

    application = QApplication.instance() or QApplication(sys.argv[:1])
    application.setApplicationName("Arc")
    application.setApplicationDisplayName("Arc \u2014 Universal Downloader")
    application.setOrganizationName("Erawat")
    application.setDesktopFileName("arc-desktop")
    application.setWindowIcon(QIcon(_icon_path()))
    application.setStyleSheet(theme.build_stylesheet())

    store = Store()
    if args.download_dir:
        store.set("download_path", args.download_dir)

    bridge = EngineBridge(Path(args.engine) if args.engine else None)
    window = MainWindow(bridge, store)
    if not bridge.ready:
        QMessageBox.warning(window, "Engine unavailable", bridge.load_error or "dl.py not found.")
    return application, window


def _icon_path() -> str:
    for candidate in (Path(__file__).resolve().parent.parent.parent / "desktop-app" / "assets" / "arc.ico",
                      Path(__file__).resolve().parent.parent / "assets" / "arc.ico",
                      Path(getattr(sys, "_MEIPASS", ".")) / "assets" / "arc.ico"):
        if candidate.is_file():
            return str(candidate)
    return ""


def run_selftest(args: argparse.Namespace) -> int:
    """Construct everything headless and report. Exit 0 only on success."""
    os.environ.setdefault("QT_QPA_PLATFORM", "offscreen")
    started = time.time()
    problems: list[str] = []
    try:
        application, window = build_window(args)
    except EngineError as error:
        print(f"[SELFTEST][FAIL] engine: {error}")
        return 1
    except Exception:
        print("[SELFTEST][FAIL] window construction:")
        traceback.print_exc()
        return 1

    bridge = window.bridge
    environment = bridge.environment()
    print(f"[SELFTEST] engine        : {environment.get('engine_path')}")
    print(f"[SELFTEST] engine load   : {environment.get('engine_load_seconds')}s")
    print(f"[SELFTEST] python        : {environment.get('python')}")
    print(f"[SELFTEST] aria2c        : {'yes' if environment.get('aria2c') else 'no'}")
    print(f"[SELFTEST] ffmpeg        : {'yes' if environment.get('ffmpeg') else 'no'}")
    print(f"[SELFTEST] download dir  : {environment.get('default_download_dir')}")
    for name, action in (environment.get("adapter") or {}).items():
        print(f"[SELFTEST] adapter       : {name} -> {action}")

    if not bridge.ready:
        problems.append(bridge.load_error or "engine did not load")

    # Exercise every page and the code paths that do not need the network.
    try:
        for index in range(len(window.nav_buttons)):
            window._select_nav(index)
            application.processEvents()

        window.pages.setCurrentWidget(window.search_page)
        window.search_page.set_results("selftest", [
            {"title": "Selftest Movie", "source": "Test", "type": "movie", "year": "2024",
             "providers": ["Test"], "poster": "", "url": "https://example.invalid/movie/1",
             "audios": [{"language": "Hindi", "url": "https://example.invalid/a"},
                        {"language": "English", "url": "https://example.invalid/b"}]},
            {"title": "Selftest Show", "source": "Test", "type": "series", "year": "",
             "providers": [], "poster": "", "url": "https://example.invalid/tv/2", "audios": []},
        ])
        application.processEvents()

        window.target = window._normalize_target({
            "title": "Selftest Show S01E02", "source": "Test", "is_series": True,
            "qualities": ["1080p (Full HD)", "480p (SD)", "2160p (4K UHD)", "Best Available"],
            "quality_urls": {"1080p (Full HD)": "https://example.invalid/1080"},
            "seasons": [{"season": 1, "episodes": 4}, {"season": 2, "episodes": 2}],
            "url": "https://example.invalid/tv/2", "stream_url": "https://example.invalid/s",
            "can_stream": True, "media_type": "video", "stream_headers": {"Referer": "x"},
        })
        window._apply_target(window.target)
        application.processEvents()

        qualities = window.target.get("available_qualities") or []
        if qualities and qualities[0] != "2160p (4K UHD)":
            problems.append(f"quality ranking wrong: {qualities}")

        payload = window._download_payload()
        for key in ("action", "url", "quality", "audio_only", "target_dir", "cache_dir", "title"):
            if key not in payload:
                problems.append(f"download payload missing '{key}'")
        if payload.get("audio_only") is not False:
            problems.append("audio_only should be False for a video download")
        if "youtube" in str(payload.get("url", "")).lower():
            problems.append("unexpected youtube url in selftest payload")

        window.jobs["selftest-job"] = {
            "id": "selftest-job", "title": "Selftest", "detail": "1080p \u00b7 Video",
            "source": "Test", "poster": "", "status": "Downloading", "progress": 12.5,
            "speed": "1.2MB/s", "eta": "40s", "downloaded": "12 MB", "total": "96 MB",
            "path": "", "started_at": time.time(),
        }
        window._render_jobs()
        application.processEvents()
        card = window.activity_page.card_for("selftest-job")
        if card is None:
            problems.append("activity card was not created")
        else:
            if "12.5%" not in card.percent_label.text():
                problems.append(f"progress text wrong: {card.percent_label.text()}")
            if not card.pause_button.isVisible() or card.resume_button.isVisible():
                problems.append("pause/resume visibility wrong for Downloading")
        window.jobs.pop("selftest-job", None)
        window._render_jobs()

        store = window.store
        before = len(store.history())
        store.add_history({"title": "Selftest File", "path": str(Path(__file__) / "x.mp4"),
                           "source": "Test", "detail": "1080p"})
        if len(store.history(only_existing=False)) != before + 1:
            problems.append("history append failed")
        store.remove_history(str(Path(__file__) / "x.mp4"))

        application.processEvents()
    except Exception:
        problems.append("UI exercise raised:\n" + traceback.format_exc())

    elapsed = time.time() - started
    if problems:
        for problem in problems:
            print(f"[SELFTEST][FAIL] {problem}")
        window.close()
        return 1

    print(f"[SELFTEST][OK] window built, all pages exercised in {elapsed:.2f}s")
    window.close()
    return 0


def main(argv: Optional[list[str]] = None) -> int:
    _bootstrap_paths()
    args = parse_args(list(sys.argv[1:] if argv is None else argv))
    if args.selftest:
        return run_selftest(args)
    try:
        application, window = build_window(args)
    except EngineError as error:
        print(f"[Arc] {error}", file=sys.stderr)
        try:
            from PySide6.QtWidgets import QApplication, QMessageBox
            fallback = QApplication.instance() or QApplication(sys.argv[:1])
            box = QMessageBox()
            box.setIcon(QMessageBox.Critical)
            box.setWindowTitle("Arc could not start")
            box.setText("The download engine (dl.py) could not be loaded.")
            box.setInformativeText(str(error))
            box.exec()
        except Exception:
            pass
        return 2
    window.show()
    return application.exec()



if __name__ == "__main__":
    raise SystemExit(main())
