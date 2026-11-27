"""MainWindow: state, job queue and the 400 ms progress poll.

Mirrors MainActivity.kt's behaviour - same statuses, same thinking-card timing,
same poll interval - but persists jobs so the Library survives a restart.

The engine's machine interface (run_app_json_request) returns
{"ok", "action", "result", "logs"}; it reports no duration and no steps, so both
are derived here exactly as handleEngineResult() does on Android.
"""

from __future__ import annotations

import base64
import binascii
import json
import os
import re
import sys
import time
import uuid
from pathlib import Path
from typing import Optional

from PySide6.QtCore import Qt, QTimer, Signal, QUrl, QSize, QPoint, QProcess, QPropertyAnimation, QEasingCurve
from PySide6.QtGui import QColor, QDesktopServices, QIcon, QPainter, QPen, QPixmap, QPolygon
from PySide6.QtWidgets import (
    QApplication, QFileDialog, QHBoxLayout, QLabel, QMainWindow, QMessageBox,
    QPushButton, QStackedWidget, QVBoxLayout, QWidget, QGraphicsOpacityEffect,
)

from .. import __version__, theme
from ..engine_bridge import EngineBridge
from ..models import match_quality_key, resolve_track_for_quality, sort_quality_labels
from ..store import Store, human_size
from .dialogs import (
    AboutFeaturesDialog, AudioSelectDialog, GoogleYouTubeDialog,
    SeriesDownloadDialog, StreamingPlayerDialog, TelegramLoginDialog,
)
from .pages import (
    AUDIO_QUALITIES, ActivityPage, AnalysisPage, HomePage, LibraryPage,
    SearchPage, SettingsPage, VIDEO_QUALITY_DEFAULTS, prioritize_audio_choices,
)
from .player import ArcPlayerDialog
from .widgets import Toast

POLL_INTERVAL_MS = 400          # same cadence as the APK's LaunchedEffect(jobs)
THINK_TICK_MS = 350             # same cadence as the animated thinking dots
NAV_ITEMS = [("Home", "Analyze links"), ("Activity", "Live progress"),
             ("Library", "Your files"), ("Settings", "Preferences")]
VIDEO_EXTENSIONS = {".mp4", ".mkv", ".webm", ".mov", ".avi", ".ts", ".m4v", ".mpd"}


class _WorkerSignals(QWidget):
    """Cross-thread bridge: engine callbacks arrive on worker threads."""

    done = Signal(dict, str, str)   # response, job_id, action label


def _nav_icon(name: str) -> QIcon:
    """Small outline icons matching the APK tabs; drawn locally for font parity."""
    def render(color: str) -> QPixmap:
        pixmap = QPixmap(24, 24)
        pixmap.fill(Qt.transparent)
        painter = QPainter(pixmap)
        painter.setRenderHint(QPainter.Antialiasing)
        painter.setPen(QPen(QColor(color), 2, Qt.SolidLine, Qt.RoundCap, Qt.RoundJoin))
        if name == "Home":
            painter.drawPolyline(QPolygon([QPoint(3, 11), QPoint(12, 3), QPoint(21, 11)]))
            painter.drawLine(6, 10, 6, 21)
            painter.drawLine(18, 10, 18, 21)
            painter.drawLine(6, 21, 18, 21)
            painter.drawLine(10, 21, 10, 15)
            painter.drawLine(14, 21, 14, 15)
        elif name == "Activity":
            painter.drawLine(12, 3, 12, 20)
            painter.drawLine(6, 14, 12, 21)
            painter.drawLine(18, 14, 12, 21)
        elif name == "Download":
            painter.drawLine(12, 3, 12, 15)
            painter.drawLine(7, 10, 12, 15)
            painter.drawLine(17, 10, 12, 15)
            painter.drawLine(4, 17, 4, 21)
            painter.drawLine(20, 17, 20, 21)
            painter.drawLine(4, 21, 20, 21)
        elif name == "Library":
            painter.drawRoundedRect(4, 3, 16, 18, 2, 2)
            painter.drawLine(8, 8, 16, 8)
            painter.drawLine(8, 12, 16, 12)
            painter.drawLine(8, 16, 16, 16)
        else:  # Settings cog, using vector lines instead of a missing font glyph.
            points = []
            import math
            for i in range(24):
                radius = 10 if i % 3 else 7.5
                angle = math.pi * 2 * i / 24 - math.pi / 2
                points.append(QPoint(round(12 + radius * math.cos(angle)), round(12 + radius * math.sin(angle))))
            painter.drawPolygon(QPolygon(points))
            painter.drawEllipse(QPoint(12, 12), 3, 3)
        painter.end()
        return pixmap

    icon = QIcon()
    icon.addPixmap(render(theme.ARC_MUTED), QIcon.Normal, QIcon.Off)
    icon.addPixmap(render(theme.ARC_BLACK), QIcon.Normal, QIcon.On)
    return icon


class MainWindow(QMainWindow):
    def __init__(self, bridge: EngineBridge, store: Store) -> None:
        super().__init__()
        self.bridge = bridge
        self.store = store
        self.setWindowTitle("Arc \u2014 Universal Downloader")
        self.resize(1100, 760)
        self.setMinimumSize(640, 480)

        self.jobs: dict[str, dict] = {}
        self.results: list[dict] = []
        self.target: dict = {}
        self.analysis_url: str = ""
        self.selected_quality: str = "Best Available"
        self.selected_audio_lang: str = ""
        self.audio_languages: list[str] = []
        self.audio_urls: dict[str, str] = {}
        self.audio_only: bool = False
        self.current_query: str = ""
        self.download_scope: str = "episode"
        self._think_started_at: float = 0.0
        self._installer_processes: dict[str, tuple[QProcess, list[str], str]] = {}
        self._search_cache: dict[str, list[dict]] = {}
        self._target_cache: dict[str, dict] = {}
        self._episode_cache: dict[str, dict] = {}

        self.signals = _WorkerSignals(self)
        self.signals.done.connect(self._on_engine_result)

        self._build_ui()
        self._start_polling()
        self._refresh_library()

    # -------------------------------------------------------------- structure
    def _build_ui(self) -> None:
        root = QWidget()
        root.setObjectName("RootPane")
        outer = QHBoxLayout(root)
        outer.setContentsMargins(0, 0, 0, 0)
        outer.setSpacing(0)
        outer.addWidget(self._build_nav_rail())

        self.pages = QStackedWidget()
        self.home_page = HomePage()
        self.search_page = SearchPage()
        self.analysis_page = AnalysisPage()
        self.activity_page = ActivityPage()
        self.library_page = LibraryPage()
        self.settings_page = SettingsPage()
        for page in (self.home_page, self.search_page, self.analysis_page,
                     self.activity_page, self.library_page, self.settings_page):
            self.pages.addWidget(page)
        outer.addWidget(self.pages, 1)
        self.setCentralWidget(root)

        self.status_label = QLabel("Ready")
        self.statusBar().addWidget(self.status_label)
        self.statusBar().addPermanentWidget(self._engine_status_label())

        self.toast = Toast(self)
        self._wire_signals()
        self._home_context_page = self.home_page
        self._select_nav(0)

    def _engine_status_label(self) -> QLabel:
        environment = self.bridge.environment()
        parallel = int(environment.get("parallel_connections", 1))
        text = (f"Engine ready \u00b7 {parallel}\u00d7 connections \u00b7 {environment.get('python', '')}"
                if environment.get("engine_ready") else "Engine unavailable")
        widget = QLabel(text)
        widget.setStyleSheet(f"color: {theme.ARC_MUTED}; font-size: 11px; padding-right: 10px;")
        return widget

    def _build_nav_rail(self) -> QWidget:
        rail = QWidget()
        rail.setObjectName("NavRail")
        rail.setFixedWidth(198)
        layout = QVBoxLayout(rail)
        layout.setContentsMargins(14, 18, 14, 18)
        layout.setSpacing(14)

        brand = QWidget()
        brand_layout = QHBoxLayout(brand)
        brand_layout.setContentsMargins(4, 0, 4, 0)
        brand_layout.setSpacing(10)
        tile = QLabel()
        tile.setFixedSize(34, 34)
        tile.setAlignment(Qt.AlignCenter)
        tile.setPixmap(_nav_icon("Download").pixmap(QSize(19, 19), QIcon.Normal, QIcon.On))
        tile.setStyleSheet(
            f"background-color: {theme.ARC_WHITE}; color: {theme.ARC_BLACK};"
            f" border-radius: 12px; font-size: 17px; font-weight: 800;"
        )
        brand_layout.addWidget(tile)
        column = QVBoxLayout()
        column.setContentsMargins(0, 0, 0, 0)
        column.setSpacing(0)
        name = QLabel("ARC")
        name.setObjectName("NavBrand")
        subtitle = QLabel("DESKTOP")
        subtitle.setObjectName("NavBrandSub")
        column.addWidget(name)
        column.addWidget(subtitle)
        brand_layout.addLayout(column)
        brand_layout.addStretch(1)
        layout.addWidget(brand)
        layout.addSpacing(4)

        self.nav_buttons: list[QPushButton] = []
        for index, (label_text, hint) in enumerate(NAV_ITEMS):
            button = QPushButton(label_text)
            button.setIcon(_nav_icon(label_text))
            button.setIconSize(QSize(18, 18))
            button.setObjectName("NavButton")
            button.setCheckable(True)
            button.setCursor(Qt.PointingHandCursor)
            button.setToolTip(hint)
            button.clicked.connect(lambda _=False, i=index: self._select_nav(i))
            layout.addWidget(button)
            self.nav_buttons.append(button)
        layout.addStretch(1)

        version = QLabel(f"v{__version__}")
        version.setObjectName("NavBrandSub")
        version.setContentsMargins(6, 0, 0, 0)
        layout.addWidget(version)
        return rail

    # ---------------------------------------------------------------- wiring
    def _wire_signals(self) -> None:
        home = self.home_page
        home.analyze_requested.connect(self.analyze_input)
        home.pick_torrent_requested.connect(self.analyze_input)
        home.library_requested.connect(lambda: self._select_nav(2))

        search = self.search_page
        search.hit_selected.connect(self.on_search_hit)
        search.back_requested.connect(lambda: self._select_nav(0))
        search.copy_logs.connect(self._copy_logs)

        analysis = self.analysis_page
        analysis.download_requested.connect(self.start_download)
        analysis.stream_requested.connect(self.start_stream)
        analysis.copy_link_requested.connect(self.copy_direct_link)
        analysis.back_requested.connect(self._back_from_analysis)
        analysis.copy_logs.connect(self._copy_logs)
        analysis.quality_changed.connect(self.on_quality_changed)
        analysis.audio_only_changed.connect(self.on_audio_only_changed)
        analysis.season_selected.connect(self.on_season_selected)
        analysis.episode_selected.connect(self.on_episode_selected)
        analysis.audio_track_selected.connect(self.on_audio_track_selected)
        analysis.playlist_item_selected.connect(self.on_playlist_item_selected)

        activity = self.activity_page
        activity.pause_requested.connect(self.pause_job)
        activity.resume_requested.connect(self.resume_job)
        activity.cancel_requested.connect(self.cancel_job)
        activity.dismiss_requested.connect(self.dismiss_job)
        activity.open_requested.connect(self.open_path)
        activity.copy_logs_requested.connect(self.copy_job_logs)

        library = self.library_page
        library.open_requested.connect(self.open_path)
        library.folder_requested.connect(self.reveal_path)
        library.delete_requested.connect(self.delete_file)
        library.refresh_requested.connect(self._refresh_library)

        settings = self.settings_page
        settings.browse_requested.connect(self.browse_download_folder)
        settings.video_quality_changed.connect(
            lambda value: self.store.set("default_video_quality", value))
        settings.audio_quality_changed.connect(
            lambda value: self.store.set("default_audio_quality", value))
        settings.notifications_changed.connect(
            lambda value: self.store.set("notifications", value))
        settings.prefer_vlc_changed.connect(
            lambda value: self.store.set("prefer_vlc", value))
        settings.player_dialog_requested.connect(self.open_player_dialog)
        settings.telegram_auth_requested.connect(self.open_telegram_login)
        settings.telegram_logout_requested.connect(self.logout_telegram)
        settings.telegram_saved.connect(self.save_telegram_credentials)
        settings.telegram_test_requested.connect(self.test_telegram)
        settings.google_login_requested.connect(self.open_google_auth_dialog)
        settings.google_disconnect_requested.connect(self.disconnect_google_account)
        settings.about_requested.connect(self.open_about_dialog)
        settings.scan_requested.connect(self._refresh_library)
        settings.install_requested.connect(self.install_accelerator)

        settings.set_defaults(
            str(self.store.get("default_video_quality", "Ask each time")),
            str(self.store.get("default_audio_quality", "Ask each time")),
            bool(self.store.get("notifications", True)),
            bool(self.store.get("prefer_vlc", True)),
        )
        settings.set_streaming_player_name(self._player_display_name())
        settings.set_download_path(self.download_dir())
        settings.set_telegram(
            str(self.store.get("telegram_api_id", "")),
            str(self.store.get("telegram_api_hash", "")),
            str(self.store.get("telegram_session", "")),
            str(self.store.get("telegram_user", "")),
            str(self.store.get("telegram_phone", "")),
        )
        g_email = str(self.store.get("google_account_email", "") or "")
        g_cookies = str(self.store.get("google_cookies", "") or "")
        settings.set_google_account(g_email, bool(g_cookies.strip()))
        settings.set_environment(self.bridge.environment())

    # -------------------------------------------------------------- shortcuts
    def download_dir(self) -> str:
        configured = str(self.store.get("download_path", "") or "").strip()
        if configured:
            try:
                Path(configured).mkdir(parents=True, exist_ok=True)
                return configured
            except Exception:
                pass
        from .. import platform_adapter
        fallback = platform_adapter.default_download_dir()
        self.store.set("download_path", fallback)
        return fallback

    def cache_dir(self) -> str:
        folder = Path(self.download_dir()) / ".arc-cache"
        try:
            folder.mkdir(parents=True, exist_ok=True)
            return str(folder)
        except Exception:
            return str(Path(self.download_dir()))

    def _show_page(self, destination: QWidget) -> None:
        """Switch pages with a short, restrained fade like the APK transitions."""
        if self.pages.currentWidget() is destination:
            return
        self.pages.setCurrentWidget(destination)
        effect = QGraphicsOpacityEffect(destination)
        destination.setGraphicsEffect(effect)
        effect.setOpacity(0.0)
        animation = QPropertyAnimation(effect, b"opacity", self)
        animation.setDuration(170)
        animation.setStartValue(0.0)
        animation.setEndValue(1.0)
        animation.setEasingCurve(QEasingCurve.OutCubic)
        animation.start()
        self._page_animation = animation

    def _select_nav(self, index: int) -> None:
        for position, button in enumerate(self.nav_buttons):
            button.setChecked(position == index)
        if index == 0:
            current = self.pages.currentWidget()
            if current in (self.activity_page, self.library_page, self.settings_page):
                destination = self._home_context_page
            elif current is self._home_context_page and current is not self.home_page:
                destination = self.home_page
            else:
                destination = self.home_page
            self._show_page(destination)
            self._home_context_page = destination
        elif index == 1:
            self._show_page(self.activity_page)
            self._render_jobs()
        elif index == 2:
            self._show_page(self.library_page)
            self._refresh_library()
        else:
            self._show_page(self.settings_page)
            self.settings_page.set_environment(self.bridge.environment())

    def _back_from_analysis(self) -> None:
        if self.results:
            self._show_page(self.search_page)
        else:
            self._select_nav(0)

    def set_status(self, text: str) -> None:
        self.status_label.setText(text)

    def notify(self, message: str) -> None:
        self.toast.show_message(message)
        QTimer.singleShot(3400, self.toast.hide)

    def _player_display_name(self) -> str:
        key = str(self.store.get("streaming_player", "arc_player") or "arc_player")
        return {
            "arc_player": "ARC Player (Built-in)",
            "vlc": "VLC Media Player",
            "system_chooser": "System Default Handler",
        }.get(key, "ARC Player (Built-in)")

    def _sync_home_status(self) -> None:
        running = sum(1 for j in self.jobs.values() if str(j.get("status")) in ("Downloading", "Muxing"))
        if running > 0:
            self.home_page.set_status_pill(f"Downloading {running}", theme.ARC_WARNING)
            if len(self.nav_buttons) > 1:
                self.nav_buttons[1].setText(f"Activity ({running})")
        else:
            self.home_page.set_status_pill("Ready", theme.ARC_SUCCESS)
            if len(self.nav_buttons) > 1:
                self.nav_buttons[1].setText("Activity")

    # ============================================================ ANALYZE FLOW
    def analyze_input(self, raw_input: str) -> None:
        text = (raw_input or "").strip().strip('"')
        if not text:
            return
        lower = text.lower()
        looks_like_link = (
            lower.startswith(
                ("http://", "https://", "magnet:", "ftp://", "t.me/",
                 "telegram.me/", "telegram.dog/", "igl:")
            )
            or (lower.endswith(".torrent") and os.path.isfile(text))
        )
        if not looks_like_link:
            self.start_search(text)
            return
        self.results = []
        self.start_analysis(text)

    def start_search(self, query: str) -> None:
        self._home_context_page = self.search_page
        self.current_query = query
        cached = self._search_cache.get(query.strip().lower())
        self.home_page.set_input(query)
        self._show_page(self.search_page)
        if cached is not None:
            self.results = list(cached)
            self.search_page.set_results(query, self.results)
            self.set_status(f"{len(self.results)} result(s) for \u201c{query}\u201d")
            return
        self.results = []
        self.search_page.set_query(query, True)
        self.home_page.set_status_pill("Searching...", theme.ARC_WARNING)
        self.set_status(f"Searching \u201c{query}\u201d...")
        self._begin_thinking(self.search_page.thinking, "Searching media catalog")
        self._submit({"action": "search", "query": query, "platform": "netmirror"},
                     f"Search: \u201c{query}\u201d")

    def start_analysis(self, url: str) -> None:
        self._home_context_page = self.analysis_page
        self.analysis_url = url
        self.audio_only = False
        cached = self._target_cache.get(f"{url}#{self.selected_audio_lang.lower()}") or self._target_cache.get(url)
        if cached is not None:
            self.target = dict(cached)
            self._show_page(self.analysis_page)
            self._apply_target(self.target)
            self.set_status(f"Ready: {self.target.get('title', '')}")
            return
        self.target = {}
        self._show_page(self.analysis_page)
        self.analysis_page.set_loading(True)
        self.analysis_page.name_label.setText(url if len(url) < 90 else url[:87] + "\u2026")
        self.analysis_page.detail_label.setText("Resolving...")
        self.home_page.set_status_pill("Analyzing...", theme.ARC_WARNING)
        self.set_status("Resolving media...")
        self._begin_thinking(self.analysis_page.thinking, "Resolving media stream")
        self._submit(self._resolve_payload(url, self._default_quality()), "Resolve media")

    def _resolve_payload(self, url: str, quality: str) -> dict:
        eff_q = "Best" if "ask" in (quality or "").lower() else quality
        payload: dict = {"action": "resolve", "url": url, "quality": eff_q}
        season, episode = self._season_episode_from_text(url)
        if season:
            payload["season"] = season
        if episode:
            payload["episode"] = episode
        if self.selected_audio_lang:
            payload["audio_lang"] = self.selected_audio_lang
        g_cookies = str(self.store.get("google_cookies", "") or "").strip()
        if g_cookies:
            payload["cookie"] = g_cookies
        telegram = self._telegram_config()
        if telegram:
            payload["telegram"] = telegram
        return payload

    def on_search_hit(self, hit: dict, url: str, language: str) -> None:
        sorted_audios = prioritize_audio_choices(list(hit.get("audios") or []))
        self.audio_languages = [str(a.get("language", "")) for a in sorted_audios if str(a.get("language", ""))]
        self.audio_urls = {str(a.get("language", "")): str(a.get("url", ""))
                           for a in sorted_audios if str(a.get("language", ""))}
        self.selected_audio_lang = language or (self.audio_languages[0] if self.audio_languages else "")
        chosen = url or self.audio_urls.get(self.selected_audio_lang, "") or str(hit.get("url", ""))
        if not chosen:
            self.notify("That result has no playable link.")
            return
        self._home_context_page = self.analysis_page
        self.analysis_url = chosen
        self.audio_only = False
        prov_list = hit.get("providers") or []
        prov_str = " \u00b7 ".join(str(p).strip() for p in prov_list if str(p).strip()) if isinstance(prov_list, list) else str(prov_list or "")
        yr_str = str(hit.get("year") or "").strip()
        self.target = {
            "target_url": chosen,
            "title": str(hit.get("title", "")),
            "poster": str(hit.get("poster", "")),
            "year": "" if yr_str == "0" else yr_str,
            "providers": prov_str,
            "source": str(hit.get("source") or hit.get("type") or ""),
            "audio_languages": list(self.audio_languages),
            "selected_audio_lang": self.selected_audio_lang,
        }
        cache_key = f"{chosen}#{self.selected_audio_lang.lower()}"
        cached = self._target_cache.get(cache_key) or self._target_cache.get(chosen)
        self._show_page(self.analysis_page)
        if cached is not None:
            self.target = dict(cached)
            self._apply_target(self.target)
            self.set_status(f"Ready: {self.target.get('title', '')}")
            return
        self.analysis_page.set_loading(True)
        self.analysis_page.name_label.setText(str(hit.get("title", "Resolving...")))
        self.analysis_page.poster.set_url(str(hit.get("poster", "")))
        self.home_page.set_status_pill("Analyzing...", theme.ARC_WARNING)
        self.set_status(f"Resolving \u201c{hit.get('title', '')}\u201d...")
        self._begin_thinking(self.analysis_page.thinking, "Resolving media stream")
        self._submit(self._resolve_payload(chosen, self._default_quality()), "Resolve media")

    # --------------------------------------------------------- engine plumbing
    def _submit(self, payload: dict, action_label: str, job_id: Optional[str] = None) -> str:
        payload = dict(payload)
        payload.setdefault("job_id", job_id or str(uuid.uuid4()))
        self.bridge.submit(payload,
                           lambda response, jid=payload["job_id"], lbl=action_label:
                           self.signals.done.emit(response, jid, lbl),
                           job_id=payload["job_id"])
        return str(payload["job_id"])

    def _begin_thinking(self, card, step: str) -> None:
        self._think_started_at = time.time()
        card.start("", step)

    def _elapsed(self) -> float:
        started = self._think_started_at or time.time()
        return max(0.4, time.time() - started)

    def _on_engine_result(self, response: dict, job_id: str, action_label: str) -> None:
        action = str(response.get("action", ""))
        ok = bool(response.get("ok", False))
        error = str(response.get("error", "")) or "The media engine encountered an issue."
        logs = str(response.get("logs", ""))
        duration = self._elapsed()

        # capabilities/progress/cancel answer outside the standard envelope.
        if not action:
            if "capabilities" in response:
                self.settings_page.set_environment(self.bridge.environment())
                return

        if action == "search" or action_label.startswith("Search"):
            self._finish_thinking(
                self.search_page.thinking, action_label, duration, logs,
                self._steps_for("search", ok, error,
                                [f"Catalogs queried and releases analyzed",
                                 f"[OK] Found {len(self._search_hits(response))} titles ready"],
                                [f"Media engine activated",
                                 f"Search query: \u201c{self.current_query}\u201d"]),
                not ok)
            hits = self._search_hits(response)
            self.results = hits
            self.search_page.set_results(self.current_query, hits)
            self._sync_home_status()
            if ok:
                if self.current_query.strip():
                    self._search_cache[self.current_query.strip().lower()] = list(hits)
                self.set_status(f"{len(hits)} result(s) for \u201c{self.current_query}\u201d")
            else:
                self.set_status(error)
                self.notify(error)
            return

        if action == "resolve" or action_label == "Resolve media":
            self._sync_home_status()
            if not ok:
                self._finish_thinking(
                    self.analysis_page.thinking, action_label, duration, logs,
                    self._steps_for("resolve", False, error, [], []), True)
                self.analysis_page.set_busy(False)
                self.analysis_page.name_label.setText("Could not resolve link")
                self.analysis_page.detail_label.setText(error)
                self.set_status(error)
                self.notify(error)
                return
            target = self._normalize_target(dict(response.get("result") or {}))
            self.target = target
            self._apply_target(target)
            t_key = str(target.get("target_url") or self.analysis_url).strip()
            if t_key:
                lang_key = str(target.get("selected_audio_lang") or "").lower().strip()
                if lang_key:
                    self._target_cache[f"{t_key}#{lang_key}"] = dict(target)
                self._target_cache[t_key] = dict(target)
                ep_key = f"{t_key}_S{target.get('selected_season', 1)}_E{target.get('selected_episode', 1)}"
                self._episode_cache[ep_key] = dict(target)
            self._finish_thinking(
                self.analysis_page.thinking, action_label, duration, logs,
                self._steps_for("resolve", True, "",
                                [f"Link analyzed: {target.get('source', 'Media')}",
                                 f"[OK] {len(target.get('available_qualities') or [])} quality option(s) ready",
                                 f"[OK] {target.get('title', 'Media')}"], []),
                False)
            self.set_status(f"Ready: {target.get('title', '')}")
            return

        if action == "download" or action_label.startswith("Download"):
            self._handle_download_result(job_id, response, ok, error, logs, duration)
            return

        if action == "telegram_check":
            self.settings_page.set_telegram_busy(False)
            result = dict(response.get("result") or {})
            if ok and result.get("ok"):
                self.settings_page.set_telegram_result(
                    True, "Session accepted. Private channels are unlocked.")
                self.notify("Telegram connected")
            else:
                detail = str(result.get("error", "")) or error
                self.settings_page.set_telegram_result(False, detail)
            return

        if action == "stream_proxy":
            proxy = str((response.get("result") or {}).get("proxy_url", ""))
            self._launch_player(proxy or self.analysis_url,
                                str(self.target.get("title", "")), logs)
            return

        if action == "igl":
            self.set_status("IGL catalog loaded")
            return

        self.set_status(error if not ok else f"{action_label} finished")

    @staticmethod
    def _search_hits(response: dict) -> list[dict]:
        result = response.get("result") or {}
        hits = result.get("results") or result.get("hits") or []
        return [dict(h) for h in hits] if isinstance(hits, list) else []

    @staticmethod
    def _steps_for(kind: str, ok: bool, error: str,
                   success_extra: list[str], prefix: list[str]) -> list[str]:
        """Client-side step list, mirroring handleEngineResult() on Android."""
        steps = prefix or ["Media engine initialized", f"Executed task: {kind}"]
        if not ok:
            return ["Media engine initialized", f"Executed task: {kind}",
                    f"[ERROR] Execution failed: {error}"]
        return steps + success_extra

    def _finish_thinking(self, thinking, action_label: str, duration: float,
                         logs: str, steps: list[str], is_error: bool) -> None:
        if thinking is None:
            return
        thinking.finish(action_label, duration, steps, logs, is_error)

    # --------------------------------------------------------- target mapping
    def _normalize_target(self, data: dict) -> dict:
        """Engine resolve result -> the view model AnalysisPage consumes."""
        is_series = bool(data.get("is_series"))
        is_playlist = bool(data.get("is_playlist"))
        direct_url = str(data.get("direct_url", "") or data.get("url", "") or "")
        proxy_url = str(data.get("proxy_url", "") or "")
        stream_url = str(data.get("stream_url", "") or "") or proxy_url or direct_url

        qualities = [str(q) for q in (data.get("qualities") or []) if str(q).strip()]
        qualities = self._sort_qualities(qualities)

        quality_urls = {}
        raw_urls = data.get("quality_urls") or {}
        if isinstance(raw_urls, dict):
            for key, value in raw_urls.items():
                if isinstance(value, dict):
                    quality_urls[str(key)] = str(value.get("url", ""))
                else:
                    quality_urls[str(key)] = str(value)

        headers = data.get("stream_headers") or data.get("headers") or {}
        if not isinstance(headers, dict):
            headers = {}

        seasons = []
        for index, season in enumerate(data.get("seasons") or []):
            if not isinstance(season, dict):
                continue
            try:
                seasons.append({"season": int(season.get("season", index + 1)),
                                "episodes": int(season.get("episodes", 1) or 1)})
            except (TypeError, ValueError):
                seasons.append({"season": index + 1, "episodes": 1})

        entries = [dict(e) for e in (data.get("entries") or []) if isinstance(e, dict)]

        can_stream = bool(data.get("can_stream", True))
        media_type = str(data.get("media_type", "video" if can_stream else "file"))

        previous = self.target or {}
        raw_qt = data.get("quality_tracks") or {}
        quality_tracks: dict[str, dict] = {}
        if isinstance(raw_qt, dict):
            for k, v in raw_qt.items():
                if isinstance(v, dict):
                    quality_tracks[str(k)] = dict(v)

        audio_choices_raw = data.get("audio_choices") or data.get("audios") or []
        audio_tracks_list: list[dict] = []
        seen_langs: set[str] = set()
        if isinstance(audio_choices_raw, list):
            for a_item in audio_choices_raw:
                if not isinstance(a_item, dict):
                    continue
                lang = str(a_item.get("language") or "").strip()
                if lang and lang.lower() not in seen_langs:
                    seen_langs.add(lang.lower())
                    u = str(a_item.get("url") or previous.get("target_url") or "")
                    audio_tracks_list.append({"language": lang, "url": u})
                    if u:
                        self.audio_urls[lang] = u
        for lang in (self.audio_languages or []):
            if lang and lang.lower() not in seen_langs:
                seen_langs.add(lang.lower())
                audio_tracks_list.append({"language": lang, "url": self.audio_urls.get(lang, "")})
        sorted_audio = prioritize_audio_choices(audio_tracks_list)
        audio_langs = [str(a["language"]) for a in sorted_audio]
        audio_track_urls = {str(k): str(v) for k, v in (data.get("audio_track_urls") or {}).items()} if isinstance(data.get("audio_track_urls"), dict) else {}
        audio_track_formats = {str(k): str(v) for k, v in (data.get("audio_track_formats") or {}).items()} if isinstance(data.get("audio_track_formats"), dict) else {}

        eff_audio_lang = (
            self.selected_audio_lang
            or str(data.get("selected_audio_lang") or "")
            or (audio_langs[0] if audio_langs else "")
        )
        self.audio_languages = audio_langs
        if eff_audio_lang:
            self.selected_audio_lang = eff_audio_lang

        prov_raw = data.get("providers")
        if isinstance(prov_raw, list):
            prov_str = " \u00b7 ".join(str(p).strip() for p in prov_raw if str(p).strip())
        else:
            prov_str = str(prov_raw or previous.get("providers") or "").strip()
        year_val = str(data.get("year") or previous.get("year") or "").strip()
        if year_val == "0":
            year_val = ""

        return {
            "url": str(data.get("target_url", "") or previous.get("target_url", "") or direct_url),
            "target_url": str(data.get("target_url", "") or previous.get("target_url", "")),
            "direct_stream_url": direct_url,
            "proxy_url": proxy_url,
            "stream_url": stream_url,
            "title": str(data.get("title", "") or previous.get("title", "")),
            "source": str(data.get("source", "") or ("TV Series" if is_series
                                                     else "Playlist" if is_playlist else "Media")),
            "poster": str(data.get("poster", "") or previous.get("poster", "")),
            "available_qualities": qualities,
            "quality_urls": quality_urls,
            "quality_tracks": quality_tracks,
            "video_url": str(data.get("video_url", "") or ""),
            "audio_url": str(data.get("audio_url", "") or ""),
            "fallback_url": str(data.get("fallback_url", "") or ""),
            "video_format": str(data.get("video_format", "") or ""),
            "audio_format": str(data.get("audio_format", "") or ""),
            "fallback_format": str(data.get("fallback_format", "") or ""),
            "stream_headers": {str(k): str(v) for k, v in headers.items()},
            "is_series": is_series,
            "is_playlist": is_playlist,
            "seasons": seasons,
            "selected_season": int(data.get("season") or data.get("selected_season")
                                   or previous.get("selected_season") or 1),
            "selected_episode": int(data.get("episode") or data.get("selected_episode")
                                    or previous.get("selected_episode") or 1),
            "entries": entries,
            "total_count": int(data.get("total_count") or len(entries) or 0),
            "can_stream": can_stream,
            "media_type": media_type,
            "audio_tracks": sorted_audio,
            "audio_languages": audio_langs,
            "audio_track_urls": audio_track_urls or dict(previous.get("audio_track_urls") or {}),
            "audio_track_formats": audio_track_formats or dict(previous.get("audio_track_formats") or {}),
            "selected_audio_lang": eff_audio_lang,
            "torrent_sources": data.get("torrent_sources") or {},
            "quality": str(data.get("quality", "")),
            "year": year_val,
            "providers": prov_str,
        }

    def _sort_qualities(self, qualities: list[str]) -> list[str]:
        """Rank quality options descending just like MainActivity.kt."""
        return sort_quality_labels(qualities)

    def _apply_target(self, target: dict) -> None:
        requested = self._default_quality()
        preferred = self._pick_quality(requested, target)
        self.selected_quality = preferred
        spec = resolve_track_for_quality(
            preferred,
            target.get("quality_tracks") or {},
            target.get("quality_urls") or {},
            str(target.get("direct_stream_url") or ""),
            target,
        )
        if spec.get("url"):
            target["direct_stream_url"] = spec["url"]
            if not target.get("proxy_url"):
                target["stream_url"] = spec["url"]
        for k in ("video_url", "audio_url", "fallback_url", "video_format", "audio_format", "fallback_format"):
            target[k] = spec.get(k, "") or target.get(k, "")
        self.analysis_page.set_busy(False)
        self.analysis_page.set_target(target, preferred, self.audio_only)
        self._update_location_hint()

    def _pick_quality(self, requested: str, target: dict) -> str:
        if self.audio_only:
            if requested and requested != "Ask each time":
                match = match_quality_key(requested, AUDIO_QUALITIES)
                if match:
                    return str(match)
            return AUDIO_QUALITIES[0]
        keys = [str(k) for k in (target.get("available_qualities") or [])]
        if not keys:
            return requested if requested != "Ask each time" else "Best Available"
        if requested and requested != "Ask each time":
            match = match_quality_key(requested, keys)
            if match:
                return str(match)
        return keys[0]

    def _update_location_hint(self) -> None:
        self.analysis_page.set_size_hint(f"Saves to {self.download_dir()}")

    def _default_quality(self) -> str:
        if self.audio_only:
            configured = str(self.store.get("default_audio_quality", "Ask each time"))
            if configured == "Ask each time":
                return "Ask each time"
            return configured if configured in AUDIO_QUALITIES else "Best Audio (320kbps)"
        configured = str(self.store.get("default_video_quality", "Ask each time"))
        if configured == "Ask each time":
            return "Ask each time"
        return configured if configured in VIDEO_QUALITY_DEFAULTS else "Best Available"

    # ------------------------------------------------------- analysis actions
    def on_quality_changed(self, quality: str) -> None:
        if not quality or quality == self.selected_quality:
            return
        self.selected_quality = quality
        self.target["selected_quality"] = quality
        spec = resolve_track_for_quality(
            quality,
            self.target.get("quality_tracks") or {},
            self.target.get("quality_urls") or {},
            str(self.target.get("direct_stream_url") or ""),
        )
        if spec.get("url"):
            self.target["direct_stream_url"] = spec["url"]
            if not self.target.get("proxy_url"):
                self.target["stream_url"] = spec["url"]
        for k in ("video_url", "audio_url", "fallback_url", "video_format", "audio_format", "fallback_format"):
            self.target[k] = spec.get(k, "")
        self.analysis_page.set_target(self.target, quality, self.audio_only)
        self._update_location_hint()

    def on_audio_only_changed(self, audio_only: bool) -> None:
        if audio_only == self.audio_only:
            return
        self.audio_only = audio_only
        self.analysis_page.set_audio_only(audio_only)
        preferred = self._pick_quality(self._default_quality(), self.target)
        self.selected_quality = preferred
        spec = resolve_track_for_quality(
            preferred,
            self.target.get("quality_tracks") or {},
            self.target.get("quality_urls") or {},
            str(self.target.get("direct_stream_url") or ""),
        )
        if spec.get("url"):
            self.target["direct_stream_url"] = spec["url"]
            if not self.target.get("proxy_url"):
                self.target["stream_url"] = spec["url"]
        for k in ("video_url", "audio_url", "fallback_url", "video_format", "audio_format", "fallback_format"):
            self.target[k] = spec.get(k, "")
        self.analysis_page.set_target(self.target, preferred, audio_only)
        self._update_location_hint()

    def on_season_selected(self, season: int) -> None:
        self.target["selected_season"] = season
        self.target["selected_episode"] = 1
        t_url = str(self.target.get("target_url") or self.target.get("url") or self.analysis_url)
        cached = self._episode_cache.get(f"{t_url}_S{season}_E1")
        if cached is not None:
            self.target = dict(cached)
            self._apply_target(self.target)
            return
        self._re_resolve("season", season)

    def on_episode_selected(self, episode: int) -> None:
        season = int(self.target.get("selected_season") or 1)
        self.target["selected_episode"] = episode
        t_url = str(self.target.get("target_url") or self.target.get("url") or self.analysis_url)
        cached = self._episode_cache.get(f"{t_url}_S{season}_E{episode}")
        if cached is not None:
            self.target = dict(cached)
            self._apply_target(self.target)
            return
        self._re_resolve("episode", episode)

    def on_audio_track_selected(self, language: str) -> None:
        if not language:
            return
        self.selected_audio_lang = language
        self.target["selected_audio_lang"] = language
        mapped_audio_url = str((self.target.get("audio_track_urls") or {}).get(language) or "")
        if mapped_audio_url and self.target.get("video_url"):
            self.target["audio_url"] = mapped_audio_url
            self.target["title"] = _with_language_suffix(_clean_language_suffix(self.target.get("title", "")), language)
            self.analysis_page.set_target(self.target, self.selected_quality, self.audio_only)
            self._update_location_hint()
            self.notify(f"Audio: {language}")
            return

        new_url = self.audio_urls.get(language) or str(self.target.get("target_url") or self.target.get("url") or self.analysis_url)
        if not new_url:
            return
        self.analysis_page.set_busy(True)
        self.home_page.set_status_pill("Analyzing...", theme.ARC_WARNING)
        self.set_status(f"Switching audio to {language}...")
        self._begin_thinking(self.analysis_page.thinking, f"Resolving {language} audio")
        payload = self._resolve_payload(new_url, self.selected_quality)
        payload["audio_lang"] = language
        if self.target.get("is_series"):
            payload["season"] = int(self.target.get("selected_season") or 1)
            payload["episode"] = int(self.target.get("selected_episode") or 1)
        self._submit(payload, "Resolve media")

    def _re_resolve(self, scope: str, value: int) -> None:
        url = str(self.target.get("target_url") or self.target.get("url") or self.analysis_url)
        if not url:
            return
        self.analysis_page.set_busy(True)
        self.set_status(f"Switching {scope} to {value}...")
        self._begin_thinking(self.analysis_page.thinking, f"Fetching {scope} {value}")
        payload = self._resolve_payload(url, self.selected_quality)
        for other in ("season", "episode"):
            current = self.target.get(f"selected_{other}")
            if current and other != scope:
                payload[other] = int(current)
        self._submit(payload, "Resolve media")

    def on_playlist_item_selected(self, entry: dict) -> None:
        url = str(entry.get("url", ""))
        if not url:
            return
        self.audio_languages = []
        self.audio_urls = {}
        self.selected_audio_lang = ""
        self.analysis_url = url
        self.start_analysis(url)

    # ------------------------------------------------------------- downloading
    def start_download(self) -> None:
        if not self.target:
            self.notify("Nothing to download yet.")
            return
        if self.target.get("is_series"):
            dlg = SeriesDownloadDialog(
                _clean_show_title(str(self.target.get("title") or "TV Series")),
                int(self.target.get("selected_season") or 1),
                int(self.target.get("selected_episode") or 1),
                self,
            )
            if dlg.exec() != SeriesDownloadDialog.Accepted or not dlg.chosen_scope:
                return
            self.download_scope = dlg.chosen_scope
        else:
            self.download_scope = "episode"

        tracks = self.target.get("audio_tracks") or []
        if len(tracks) > 1 and not self.selected_audio_lang:
            adlg = AudioSelectDialog(tracks, self.selected_audio_lang, self)
            if adlg.exec() != AudioSelectDialog.Accepted or not adlg.selected_track:
                return
            self.on_audio_track_selected(str(adlg.selected_track.get("language") or ""))
            return

        payload = self._download_payload()
        job = self._create_job(payload)
        self.jobs[job["id"]] = job
        payload["job_id"] = job["id"]
        self._render_jobs()
        self._select_nav(1)
        self.set_status(f"Downloading {job['title']}...")
        self.notify(f"Download queued: {job['title']}")
        self._submit(payload, f"Download: {job['title']}", job_id=job["id"])

    def _download_payload(self) -> dict:
        target = self.target
        is_audio = self.audio_only
        engine_url = str(target.get("target_url") or target.get("url") or self.analysis_url)
        quality = "audio" if is_audio else (
            "Best" if "ask" in self.selected_quality.lower() else self.selected_quality)

        custom_title = None
        if target.get("is_series"):
            s_num = int(target.get("selected_season") or 1)
            base_show = _clean_show_title(str(target.get("title") or "Series"))
            if self.download_scope == "season":
                custom_title = f"{base_show} Season {s_num}"
            elif self.download_scope == "series":
                custom_title = f"{base_show} Complete Series"

        payload: dict = {
            "action": "download",
            "url": engine_url,
            "scope": self.download_scope,
            "title": custom_title or _final_title(target, is_audio, self.selected_audio_lang),
            "quality": quality,
            "audio_only": is_audio,
            "target_dir": self.download_dir(),
            "cache_dir": self.cache_dir(),
        }
        if self.selected_audio_lang:
            payload["audio_lang"] = self.selected_audio_lang

        is_batch = self.download_scope in ("season", "series")
        if not is_batch and "youtube" not in engine_url.lower() and "youtu.be" not in engine_url.lower():
            q_mapped = str((target.get("quality_urls") or {}).get(self.selected_quality) or "")
            direct = q_mapped or str(target.get("direct_stream_url") or engine_url)
            if direct and "127.0.0.1" not in direct and direct != engine_url and "googlevideo.com" not in direct:
                payload["stream_url"] = direct

        headers = dict(target.get("stream_headers") or {})
        if headers:
            payload["headers"] = headers
        if target.get("is_series"):
            payload["season"] = int(target.get("selected_season") or 1)
            payload["episode"] = int(target.get("selected_episode") or 1)

        g_cookies = str(self.store.get("google_cookies", "") or "").strip()
        if g_cookies:
            payload["cookie"] = g_cookies

        telegram = self._telegram_config()
        if telegram:
            payload["telegram"] = telegram
        return payload

    def _telegram_config(self) -> dict:
        api_id = str(self.store.get("telegram_api_id", "") or "").strip()
        api_hash = str(self.store.get("telegram_api_hash", "") or "").strip()
        session = str(self.store.get("telegram_session", "") or "").strip()
        if session:
            return {"api_id": api_id, "api_hash": api_hash, "session": _pad_base64(session)}
        return {}

    def _create_job(self, payload: dict) -> dict:
        title = str(payload.get("title") or "Download")
        is_audio = bool(payload.get("audio_only"))
        return {
            "id": str(uuid.uuid4()),
            "title": title,
            "detail": (f"{payload.get('quality', '')} \u00b7 Audio" if is_audio
                       else f"{payload.get('quality', '')} \u00b7 Video"),
            "source": str(self.target.get("source", "")),
            "poster": str(self.target.get("poster", "")),
            "status": "Downloading",
            "progress": 0.0,
            "speed": "Connecting...",
            "eta": "--",
            "downloaded": "Starting...",
            "total": "--",
            "path": "",
            "started_at": time.time(),
        }

    def _handle_download_result(self, job_id: str, response: dict, ok: bool,
                                error: str, logs: str, duration: float) -> None:
        job = self.jobs.get(job_id)
        if job is None:
            return
        result = dict(response.get("result") or {})
        cancelled = bool(result.get("cancelled"))
        if not ok:
            job["status"] = "Failed"
            job["detail"] = error
            job["error_log"] = logs or error
            job["speed"] = "--"
            self.notify(f"Failed: {job['title']}")
            self.set_status(error)
        elif cancelled:
            job["status"] = "Cancelled"
            job["speed"] = "--"
            self.notify(f"Cancelled: {job['title']}")
        else:
            job["status"] = "Complete"
            job["path"] = str(result.get("path", ""))
            job["progress"] = 100.0
            job["speed"] = "Done"
            job["eta"] = "--"
            job["downloaded"] = "Done"
            job["total"] = "Done"
            job["quality"] = str(result.get("quality", job.get("detail", "")))
            self.store.add_history({
                "title": job.get("title", "Download"),
                "path": job["path"],
                "source": job.get("source", ""),
                "detail": job.get("detail", ""),
                "poster": job.get("poster", ""),
            })
            if self.store.get("notifications", True):
                self.notify(f"Downloaded: {Path(job['path']).name}" if job["path"]
                            else f"Downloaded: {job['title']}")
            if self.store.get("open_folder_when_done", False) and job["path"]:
                from ..platform_adapter import reveal_in_folder
                reveal_in_folder(job["path"])
            self.set_status(f"Saved {Path(job['path']).name}" if job["path"] else "Download complete")
        self._render_jobs()
        self._refresh_library()

    def start_stream(self) -> None:
        if not self.target:
            self.notify("No stream link to play.")
            return
        spec = resolve_track_for_quality(
            self.selected_quality,
            self.target.get("quality_tracks") or {},
            self.target.get("quality_urls") or {},
            str(self.target.get("direct_stream_url") or self.target.get("stream_url") or self.target.get("proxy_url") or ""),
            self.target,
        )
        eff_url = str(spec.get("url") or self.target.get("proxy_url") or self.target.get("stream_url") or "")
        if not eff_url and not (spec.get("video_url") and spec.get("audio_url")):
            self.notify("No stream link to play.")
            return

        display_title = (
            f"{_clean_show_title(str(self.target.get('title') or 'Stream'))} "
            f"S{self.target.get('selected_season', 1)}E{self.target.get('selected_episode', 1)}"
            if self.target.get("is_series")
            else str(self.target.get("title") or "Stream")
        )
        player_key = str(self.store.get("streaming_player", "arc_player") or "arc_player")
        if player_key == "arc_player":
            player_target = {
                "url": eff_url,
                "headers": dict(self.target.get("stream_headers") or {}),
                "title": display_title,
                "video_url": spec.get("video_url", ""),
                "audio_url": spec.get("audio_url", ""),
                "fallback_url": spec.get("fallback_url", ""),
                "video_format": spec.get("video_format", ""),
                "audio_format": spec.get("audio_format", ""),
                "fallback_format": spec.get("fallback_format", ""),
                "is_audio_only": self.audio_only,
                "current_quality": self.selected_quality,
                "available_qualities": list(self.target.get("available_qualities") or []),
                "quality_tracks": dict(self.target.get("quality_tracks") or {}),
                "quality_urls": dict(self.target.get("quality_urls") or {}),
                "target_url": str(self.target.get("target_url") or self.analysis_url),
                "audio_tracks": list(self.target.get("audio_tracks") or []),
                "audio_track_urls": dict(self.target.get("audio_track_urls") or {}),
                "audio_track_formats": dict(self.target.get("audio_track_formats") or {}),
                "selected_audio_lang": self.selected_audio_lang,
                "is_series": bool(self.target.get("is_series")),
                "selected_season": int(self.target.get("selected_season") or 1),
                "selected_episode": int(self.target.get("selected_episode") or 1),
            }
            dlg = ArcPlayerDialog(self.bridge, player_target, self)
            dlg.exec()
            return

        if "127.0.0.1" in eff_url or (spec.get("video_url") and spec.get("audio_url")):
            from ..platform_adapter import play_stream
            v_target = str(spec.get("video_url") or eff_url)
            a_target = str(spec.get("audio_url") or "")
            ok, msg = play_stream(
                v_target,
                v_target,
                display_title,
                audio_url=a_target,
                headers=self.target.get("stream_headers") or {},
                prefer_vlc=(player_key == "vlc"),
            )
            self.set_status(msg)
            self.notify(msg)
            return

        self.set_status("Starting local stream proxy...")
        self._begin_thinking(self.analysis_page.thinking, "Starting local proxy")
        self._submit({"action": "stream_proxy", "url": eff_url,
                      "headers": self.target.get("stream_headers") or {}}, "Stream")

    def _launch_player(self, url: str, title: str, logs: str = "") -> None:
        from ..platform_adapter import play_stream
        player_key = str(self.store.get("streaming_player", "arc_player") or "arc_player")
        prefer_vlc = bool(self.store.get("prefer_vlc", True)) or player_key == "vlc"
        ok, message = play_stream(url, url, title, prefer_vlc=prefer_vlc)
        self._finish_thinking(
            self.analysis_page.thinking, "Stream", self._elapsed(), logs,
            self._steps_for("stream_proxy", ok, message, ["Player launched"], []), not ok)
        self.set_status(message)
        self.notify(message)

    def copy_direct_link(self) -> None:
        url = str(self.target.get("stream_url", ""))
        if not url:
            self.notify("No direct link available.")
            return
        QApplication.clipboard().setText(url)
        self.notify("Direct link copied")

    # ------------------------------------------------------------ job control
    def pause_job(self, job_id: str) -> None:
        self.bridge.pause(job_id)
        self._set_job_status(job_id, "Paused")
        self.notify("Download paused")

    def resume_job(self, job_id: str) -> None:
        self.bridge.resume(job_id)
        self._set_job_status(job_id, "Downloading")
        self.notify("Download resumed")

    def cancel_job(self, job_id: str) -> None:
        self.bridge.cancel(job_id)
        self._set_job_status(job_id, "Cancelled")
        self.notify("Download cancelled")

    def dismiss_job(self, job_id: str) -> None:
        self.jobs.pop(job_id, None)
        self._render_jobs()

    def _set_job_status(self, job_id: str, status: str) -> None:
        job = self.jobs.get(job_id)
        if job:
            job["status"] = status
            job["speed"] = "--" if status in ("Paused", "Cancelled") else job.get("speed", "--")
            self._render_jobs()

    def _render_jobs(self) -> None:
        ordered = sorted(self.jobs.values(), key=lambda j: j.get("started_at", 0), reverse=True)
        self.activity_page.set_jobs(ordered)
        self._sync_home_status()
        entries = self._library_entries()
        self.library_page.set_entries(entries, self.download_dir())
        self.home_page.set_recent(entries, self.open_path, self.reveal_path, self.delete_file)

    def copy_job_logs(self, job_id: str) -> None:
        job = self.jobs.get(job_id) or {}
        text = str(job.get("error_log") or job.get("detail") or "")
        if text:
            QApplication.clipboard().setText(text)
            self.notify("Error logs copied")

    def _library_entries(self) -> list[dict]:
        entries = self.store.history()
        seen = {str(e.get("path")) for e in entries}
        for extra in self.store.scan_folder(self.download_dir()):
            if str(extra.get("path")) not in seen:
                entries.append(extra)
        entries.sort(key=lambda e: e.get("finished_at", 0) or 0, reverse=True)
        return entries

    def open_path(self, path: str) -> None:
        if not path:
            return
        from ..platform_adapter import open_path as open_with_os
        p = Path(path)
        if not p.exists():
            self.notify("That file is no longer on disk.")
            self.store.remove_history(path)
            self._refresh_library()
            return
        ext = p.suffix.lower()
        is_audio = ext in {".mp3", ".m4a", ".aac", ".flac", ".ogg", ".wav", ".opus"}
        is_media = is_audio or ext in VIDEO_EXTENSIONS
        player_key = str(self.store.get("streaming_player", "arc_player") or "arc_player")
        if is_media and player_key == "arc_player":
            dlg = ArcPlayerDialog(
                self.bridge,
                {
                    "url": str(p),
                    "title": p.stem,
                    "is_audio_only": is_audio,
                    "download_file_path": str(p),
                },
                self,
            )
            dlg.exec()
            return
        if not open_with_os(path):
            self.notify("Could not open that file.")

    def reveal_path(self, path: str) -> None:
        from ..platform_adapter import reveal_in_folder, open_path as open_with_os
        if path and Path(path).exists():
            reveal_in_folder(path)
        else:
            open_with_os(self.download_dir())

    def delete_file(self, path: str) -> None:
        if not path or not Path(path).exists():
            self.store.remove_history(path)
            self._refresh_library()
            return
        answer = QMessageBox.question(
            self, "Delete file", f"Delete this file from disk?\n\n{path}",
            QMessageBox.Yes | QMessageBox.No, QMessageBox.No,
        )
        if answer != QMessageBox.Yes:
            return
        try:
            Path(path).unlink()
            self.store.remove_history(path)
            self.notify("File deleted")
        except Exception as error:
            self.notify(f"Could not delete: {error}")
        self._refresh_library()

    # ---------------------------------------------------------------- library
    def _refresh_library(self) -> None:
        entries = self._library_entries()
        self.library_page.set_entries(entries, self.download_dir())
        self.home_page.set_recent(entries, self.open_path, self.reveal_path, self.delete_file)

    # ---------------------------------------------------------------- settings
    def browse_download_folder(self) -> None:
        folder = QFileDialog.getExistingDirectory(self, "Choose download folder", self.download_dir())
        if not folder:
            return
        self.store.set("download_path", folder)
        self.settings_page.set_download_path(folder)
        self._refresh_library()
        self.notify(f"Downloads now save to {folder}")

    def install_accelerator(self, key: str) -> None:
        packages = {
            "aria2c": ("aria2.aria2", "aria2c"),
            "ffmpeg": ("Gyan.FFmpeg", "FFmpeg"),
            "vlc": ("VideoLAN.VLC", "VLC"),
        }
        key = key.lower()
        package = packages.get(key)
        if package is None or key in self._installer_processes:
            return
        package_id, display = package
        if sys.platform != "win32":
            self.settings_page.set_installer_state(key, "Windows only", error=True)
            self.notify("Windows package installers are unavailable in Winlator/Linux.")
            return

        process = QProcess(self)
        process.setProcessChannelMode(QProcess.MergedChannels)
        output: list[str] = []
        process.readyReadStandardOutput.connect(
            lambda p=process, out=output: out.append(bytes(p.readAllStandardOutput()).decode("utf-8", "replace")))
        process.finished.connect(
            lambda code, _status, k=key, p=process: self._installer_finished(k, p, code))
        process.errorOccurred.connect(
            lambda _error, k=key, p=process: self._installer_failed(k, p))
        self._installer_processes[key] = (process, output, display)
        self.settings_page.set_installer_state(key, "Installing…", busy=True)
        process.start("winget", [
            "install", "--id", package_id, "--exact", "--silent",
            "--accept-source-agreements", "--accept-package-agreements",
        ])

    def _installer_finished(self, key: str, process: QProcess, exit_code: int) -> None:
        entry = self._installer_processes.pop(key, None)
        if entry is None:
            return
        _, output, display = entry
        environment = self.bridge.environment()
        self.settings_page.set_environment(environment)
        installed = bool(environment.get("vlc")) if key == "vlc" else bool(
            (environment.get("accelerators", {}) or {}).get(key))
        if exit_code == 0 and installed:
            self.notify(f"{display} installed.")
        else:
            detail = " ".join("".join(output).split())[-180:]
            status = "Install failed" + (f": {detail}" if detail else ".")
            self.settings_page.set_installer_state(key, status, error=True)
            self.notify(status)
        process.deleteLater()

    def _installer_failed(self, key: str, process: QProcess) -> None:
        entry = self._installer_processes.pop(key, None)
        if entry is None:
            return
        _, _output, display = entry
        self.settings_page.set_installer_state(
            key, "winget missing (Windows App Installer required)", error=True)
        self.notify(f"Could not install {display}: winget is not available in this environment.")
        process.deleteLater()

    def save_telegram_credentials(self, api_id: str, api_hash: str, session: str) -> None:
        padded = _pad_base64(session) if session else ""
        self.store.update({"telegram_api_id": api_id, "telegram_api_hash": api_hash,
                           "telegram_session": padded})
        self.settings_page.set_telegram(
            api_id, api_hash, session,
            str(self.store.get("telegram_user", "")),
            str(self.store.get("telegram_phone", "")),
        )
        self.notify("Telegram credentials saved")

    def open_player_dialog(self) -> None:
        from ..platform_adapter import find_vlc
        cur = str(self.store.get("streaming_player", "arc_player") or "arc_player")
        dlg = StreamingPlayerDialog(cur, find_vlc() or "", self)
        if dlg.exec() == StreamingPlayerDialog.Accepted:
            self.store.set("streaming_player", dlg.chosen_key)
            self.store.set("prefer_vlc", dlg.chosen_key == "vlc")
            self.settings_page.set_streaming_player_name(self._player_display_name())
            self.notify(f"Streaming player: {self._player_display_name()}")

    def open_telegram_login(self) -> None:
        dlg = TelegramLoginDialog(
            self.bridge,
            str(self.store.get("telegram_api_id", "")),
            str(self.store.get("telegram_api_hash", "")),
            str(self.store.get("telegram_session", "")),
            self,
        )
        if dlg.exec() == TelegramLoginDialog.Accepted and dlg.result_creds:
            c = dlg.result_creds
            self.store.update({
                "telegram_api_id": c.get("api_id", ""),
                "telegram_api_hash": c.get("api_hash", ""),
                "telegram_session": _pad_base64(c.get("session", "")),
                "telegram_user": c.get("user", ""),
                "telegram_phone": c.get("phone", ""),
            })
            self.settings_page.set_telegram(
                c.get("api_id", ""),
                c.get("api_hash", ""),
                c.get("session", ""),
                c.get("user", ""),
                c.get("phone", ""),
            )
            self.notify(f"Telegram connected: {c.get('user') or 'Account'}")

    def logout_telegram(self) -> None:
        api_id = str(self.store.get("telegram_api_id", ""))
        api_hash = str(self.store.get("telegram_api_hash", ""))
        sess = str(self.store.get("telegram_session", ""))
        self.bridge.submit(
            {"action": "telegram_logout", "api_id": api_id, "api_hash": api_hash, "session": sess},
            lambda _res: None,
        )
        self.store.update({
            "telegram_session": "",
            "telegram_user": "",
            "telegram_phone": "",
        })
        self.settings_page.set_telegram(api_id, api_hash, "", "", "")
        self.notify("Telegram signed out")

    def open_google_auth_dialog(self) -> None:
        dlg = GoogleYouTubeDialog(
            str(self.store.get("google_account_email", "")),
            str(self.store.get("google_cookies", "")),
            self,
        )
        if dlg.exec() == GoogleYouTubeDialog.Accepted:
            self.store.update({
                "google_account_email": dlg.result_email,
                "google_cookies": dlg.result_cookies,
            })
            has_c = bool(dlg.result_cookies.strip())
            self.settings_page.set_google_account(dlg.result_email, has_c)
            self.notify("YouTube cookies updated" if has_c else "YouTube cookies cleared")

    def disconnect_google_account(self) -> None:
        self.store.update({"google_account_email": "", "google_cookies": ""})
        self.settings_page.set_google_account("", False)
        self.notify("Google account disconnected")

    def open_about_dialog(self) -> None:
        AboutFeaturesDialog(__version__, self).exec()

    def test_telegram(self, api_id: str, api_hash: str, session: str) -> None:
        if not (api_id and api_hash and session):
            self.settings_page.set_telegram_result(
                False, "Fill in API ID, API hash and session first.")
            return
        self.settings_page.set_telegram_busy(True)
        clean_sess = re.sub(r'\s+', '', session)
        self._submit({"action": "telegram_check", "api_id": api_id, "api_hash": api_hash,
                      "session": _pad_base64(clean_sess)}, "Check Telegram session")

    # ------------------------------------------------------------------ polls
    def _start_polling(self) -> None:
        self.poll_timer = QTimer(self)
        self.poll_timer.setInterval(POLL_INTERVAL_MS)
        self.poll_timer.timeout.connect(self._poll_progress)
        self.poll_timer.start()

        self.think_timer = QTimer(self)
        self.think_timer.setInterval(THINK_TICK_MS)
        self.think_timer.timeout.connect(self._tick_thinking)
        self.think_timer.start()

    def _active_jobs(self) -> list[str]:
        return [job_id for job_id, job in self.jobs.items()
                if str(job.get("status")) in ("Downloading", "Muxing", "Queued", "Paused")]

    def _poll_progress(self) -> None:
        active = self._active_jobs()
        if not active or self.bridge.dl is None:
            return
        try:
            snapshots = self.bridge.progress()
        except Exception:
            return
        if not snapshots:
            return
        changed = False
        for job_id in active:
            job = self.jobs.get(job_id)
            if job is None:
                continue
            snapshot = snapshots.get(job_id)
            if not isinstance(snapshot, dict):
                continue
            try:
                percent = float(snapshot.get("percent", 0.0) or 0.0)
            except (TypeError, ValueError):
                percent = 0.0
            status = str(snapshot.get("status", "") or "")
            values = {
                "progress": percent,
                "speed": str(snapshot.get("speed", "--") or "--"),
                "eta": str(snapshot.get("eta", "--") or "--"),
                "downloaded": str(snapshot.get("downloaded_str", "--") or "--"),
                "total": str(snapshot.get("total_str", "--") or "--"),
            }
            path = str(snapshot.get("path", "") or "")
            if path:
                values["path"] = path
            current = {key: job.get(key) for key in values}
            if current != values:
                job.update(values)
                changed = True
            if status and status != str(job.get("status")) and str(job.get("status")) not in (
                    "Cancelled", "Failed", "Complete"):
                job["status"] = status
                changed = True
        if changed:
            self._render_jobs()

    def _tick_thinking(self) -> None:
        if not self._think_started_at:
            return
        elapsed = time.time() - self._think_started_at
        for page in (self.search_page, self.analysis_page):
            thinking = getattr(page, "thinking", None)
            if thinking is not None and thinking.isVisible():
                detail = thinking.detail_label.text()
                live_step = detail.split("   [")[0] if "   [" in detail else ""
                thinking.tick(elapsed, live_step)

    def _copy_logs(self) -> None:
        for page in (self.search_page, self.analysis_page):
            thinking = getattr(page, "thinking", None)
            if thinking is not None and thinking.isVisible():
                QApplication.clipboard().setText(thinking.logs())
                self.notify("Log output copied")
                return

    # ------------------------------------------------------------------- misc
    @staticmethod
    def _season_episode_from_text(text: str) -> tuple[Optional[int], Optional[int]]:
        import re
        season = episode = None
        match = re.search(r"[?&]season=(\d+)", text, re.IGNORECASE)
        if match:
            season = int(match.group(1))
        match = re.search(r"[?&]episode=(\d+)", text, re.IGNORECASE)
        if match:
            episode = int(match.group(1))
        if season is None:
            match = re.search(r"\bs(\d{1,2})\s*e(\d{1,3})\b", text, re.IGNORECASE)
            if match:
                season, episode = int(match.group(1)), int(match.group(2))
        return season, episode

    def closeEvent(self, event) -> None:  # noqa: N802 - Qt naming
        for job_id, job in list(self.jobs.items()):
            if str(job.get("status")) in ("Downloading", "Paused", "Queued"):
                self.bridge.cancel(job_id)
        self.poll_timer.stop()
        self.think_timer.stop()
        self.store.save()
        super().closeEvent(event)


def _final_title(target: dict, is_audio: bool, language: str) -> str:
    title = str(target.get("title") or "download").strip()
    if target.get("is_series"):
        season = int(target.get("selected_season") or 1)
        episode = int(target.get("selected_episode") or 1)
        base = _clean_show_title(title)
        title = f"{base} S{season:02d}E{episode:02d}"
    if language and language.lower() not in title.lower():
        title = f"{title} [{language}]"
    if is_audio and not title.lower().endswith((".mp3", ".m4a", ".flac", ".wav", ".opus")):
        title = f"{title}.mp3"
    return title


def _clean_show_title(raw: str) -> str:
    import re
    text = re.sub(r"[\s\-_]+S\d+(-S\d+)?(\s*E\d+)?.*", "", raw or "", flags=re.IGNORECASE)
    text = re.sub(r"[\s\-_]+Season\s*\d+(-S\d+)?.*", "", text, flags=re.IGNORECASE)
    text = re.sub(r"[\s\-_]+Complete\s*Series.*", "", text, flags=re.IGNORECASE)
    return text.strip() or (raw or "")


def _clean_language_suffix(raw: str) -> str:
    return re.sub(r"\s+\[[A-Za-z0-9 _-]+\]$", "", str(raw or "")).strip()


def _with_language_suffix(title: str, language: str) -> str:
    title = str(title or "")
    language = str(language or "")
    if not language or language.lower() in title.lower():
        return title
    return f"{title} [{language}]"


def _pad_base64(session: str) -> str:
    """Pyrogram sessions are base64; a copy/paste often drops the trailing '='."""
    session = (session or "").strip()
    if not session or len(session) % 4 == 0:
        return session
    try:
        base64.b64decode(session + "=" * (-len(session) % 4), validate=False)
        return session + "=" * (-len(session) % 4)
    except (binascii.Error, ValueError):
        return session
