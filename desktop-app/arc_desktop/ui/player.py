"""Built-in ARC Cinema Media Player dialog using QtMultimedia (FFmpeg backend).

Mirrors MainActivity.kt's ArcPlayerScreen:
- Handles local files, progressive streams, HLS .m3u8 (via proxy), and split
  YouTube DASH (video_url + audio_url) using synced primary + slave players.
- Quality switcher preserving exact playback position (resumeSeekMs).
- Audio track switcher (instant direct swap or engine re-resolve) preserving position.
- YouTube build 79 stability: never auto-switch quality on buffer/error; keeps spinner
  until loaded or changed manually.
- Non-YouTube proxy/fallback recovery for referer-locked CDNs.
- Playback speed (0.5x - 2.0x), volume/mute, aspect-ratio mode, +/-10s seek, fullscreen.
"""

from __future__ import annotations

import bisect
import html
import re
import threading
import urllib.request
from pathlib import Path
from typing import Any, Callable, Optional

from PySide6.QtCore import Qt, QTimer, QUrl, Signal
from PySide6.QtGui import QKeySequence, QShortcut
from PySide6.QtMultimedia import QAudioOutput, QMediaPlayer
from PySide6.QtMultimediaWidgets import QVideoWidget
from PySide6.QtWidgets import (
    QComboBox, QDialog, QFileDialog, QHBoxLayout, QLabel, QPushButton, QSlider,
    QVBoxLayout, QWidget,
)

from .. import theme
from ..models import resolve_track_for_quality


_TIMECODE_RE = re.compile(
    r"(?:(\d{1,2}):)?(\d{1,2}):(\d{2})[,.](\d{1,3})\s*-->\s*(?:(\d{1,2}):)?(\d{1,2}):(\d{2})[,.](\d{1,3})"
)
_TAG_RE = re.compile(r"<[^>]+>|\{\\[^}]+\}")


def _to_ms(h: Optional[str], m: str, s: str, ms: str) -> int:
    hours = int(h or 0)
    minutes = int(m)
    seconds = int(s)
    millis = int(ms.ljust(3, "0")[:3])
    return ((hours * 3600) + (minutes * 60) + seconds) * 1000 + millis


def _parse_subtitles_text(raw: str) -> list[tuple[int, int, str]]:
    """Parse SRT or WebVTT content into sorted (start_ms, end_ms, text) cues."""
    if not raw:
        return []
    text = raw.lstrip("\ufeff").replace("\r\n", "\n").replace("\r", "\n")
    cues: list[tuple[int, int, str]] = []
    blocks = re.split(r"\n{2,}", text.strip())
    for block in blocks:
        lines = [ln.strip() for ln in block.split("\n") if ln.strip()]
        if not lines:
            continue
        match = None
        body_start = 0
        for idx, line in enumerate(lines[:3]):
            m = _TIMECODE_RE.search(line)
            if m:
                match = m
                body_start = idx + 1
                break
        if not match or body_start >= len(lines):
            continue
        start_ms = _to_ms(match.group(1), match.group(2), match.group(3), match.group(4))
        end_ms = _to_ms(match.group(5), match.group(6), match.group(7), match.group(8))
        cue_body = "\n".join(lines[body_start:])
        cue_body = html.unescape(_TAG_RE.sub("", cue_body)).strip()
        if cue_body and end_ms >= start_ms:
            cues.append((start_ms, end_ms, cue_body))
    cues.sort(key=lambda c: (c[0], c[1]))
    return cues


def _format_ms(ms: int) -> str:
    if ms <= 0:
        return "0:00"
    total_sec = ms // 1000
    h = total_sec // 3600
    m = (total_sec % 3600) // 60
    s = total_sec % 60
    if h > 0:
        return f"{h}:{m:02d}:{s:02d}"
    return f"{m}:{s:02d}"


class _PlayerSignals(QWidget):
    proxy_ready = Signal(int, str)
    audio_resolved = Signal(int, dict, str)
    subtitles_loaded = Signal(int, str, object)


class ArcPlayerDialog(QDialog):
    """Built-in in-app Cinema Player dialog for Windows."""

    def __init__(self, bridge, target: dict, parent: Optional[QWidget] = None) -> None:
        super().__init__(parent)
        self.bridge = bridge
        self.target = dict(target or {})
        self.setWindowTitle(f"ARC Player — {self.target.get('title') or 'Stream'}")
        self.resize(1040, 620)
        self.setMinimumSize(640, 400)
        self.setStyleSheet(f"QDialog {{ background-color: {theme.ARC_BLACK}; }}")

        self._signals = _PlayerSignals(self)
        self._signals.proxy_ready.connect(self._on_proxy_ready)
        self._signals.audio_resolved.connect(self._on_audio_resolved)
        self._signals.subtitles_loaded.connect(self._on_subtitles_loaded)

        self._selected_quality = str(self.target.get("current_quality") or "Best Available")
        self._selected_audio_lang = str(self.target.get("selected_audio_lang") or "")
        self._selected_sub_lang = str(self.target.get("selected_sub_lang") or "")
        self._override_audio_url = ""
        self._using_fallback = False
        self._lazy_proxy_url = ""
        self._load_generation = 0
        self._sub_generation = 0
        self._resume_seek_ms = 0
        self._user_scrubbing = False
        self._duration_ms = 0
        self._has_audio_slave = False
        self._speed = 1.0
        self._closing = False
        self._sub_cues: list[tuple[int, int, str]] = []
        self._sub_starts: list[int] = []
        self._last_cue_text = ""

        self._selectable_qualities = self._compute_qualities()
        self._audio_options = self._compute_audio_options()
        self._subtitle_options = self._compute_subtitle_options()
        if not self._selected_audio_lang and self._audio_options:
            self._selected_audio_lang = self._audio_options[0]["language"]

        self._audio_output = QAudioOutput(self)
        self._audio_output.setVolume(1.0)
        self._slave_audio_output = QAudioOutput(self)
        self._slave_audio_output.setVolume(1.0)
        self._muted_video_audio = QAudioOutput(self)
        self._muted_video_audio.setMuted(True)

        self._player = QMediaPlayer(self)
        self._player.setAudioOutput(self._audio_output)

        self._slave_audio = QMediaPlayer(self)
        self._slave_audio.setAudioOutput(self._slave_audio_output)

        self._build_ui()
        self._player.setVideoOutput(self.video_widget)
        self._wire_player_events()
        self._wire_shortcuts()

        self._sync_timer = QTimer(self)
        self._sync_timer.setInterval(250)
        self._sync_timer.timeout.connect(self._sync_slave_tick)
        self._sync_timer.start()

        self._stall_timer = QTimer(self)
        self._stall_timer.setSingleShot(True)
        self._stall_timer.timeout.connect(self._on_stall_timeout)

        self._load_active_source()
        if self._selected_sub_lang and self._selected_sub_lang.lower() != "off":
            self._activate_subtitle_by_key(self._selected_sub_lang)

    def _compute_subtitle_options(self) -> list[dict]:
        """Collect uploaded/manual subtitle tracks and sibling local .srt/.vtt files.

        Never exposes auto-generated captions (auto=True) because they fail to load.
        """
        tracks: list[dict] = []
        seen_urls: set[str] = set()
        for t in (self.target.get("subtitle_tracks") or []):
            if not isinstance(t, dict) or t.get("auto"):
                continue
            url = str(t.get("url") or "").strip()
            lang = str(t.get("lang") or "").strip()
            label = str(t.get("label") or lang or "Subtitles").strip()
            if not url or url in seen_urls:
                continue
            seen_urls.add(url)
            tracks.append({"lang": lang or label, "label": label, "url": url, "is_local": False})

        local_path = str(self.target.get("download_file_path") or "").strip()
        if local_path:
            lp = Path(local_path)
            if lp.parent.is_dir():
                stem = lp.stem
                for cand in sorted(lp.parent.iterdir()):
                    if not cand.is_file():
                        continue
                    if cand.suffix.lower() not in (".srt", ".vtt"):
                        continue
                    if not cand.stem.startswith(stem):
                        continue
                    curl = str(cand)
                    if curl in seen_urls:
                        continue
                    seen_urls.add(curl)
                    suffix_part = cand.stem[len(stem):].lstrip(".-_ ")
                    lbl = f"Local ({suffix_part.upper()})" if suffix_part else f"Local {cand.suffix.upper().lstrip('.')}"
                    tracks.append({"lang": suffix_part or "local", "label": lbl, "url": curl, "is_local": True})
        return tracks

    def _compute_qualities(self) -> list[str]:
        raw = [str(q) for q in (self.target.get("available_qualities") or []) if str(q).strip()]
        if not raw or self.target.get("is_audio_only"):
            return raw
        video_only = [q for q in raw if "audio" not in q.lower()]
        return video_only or raw

    def _compute_audio_options(self) -> list[dict]:
        combined: list[dict] = []
        seen: set[str] = set()
        for t in (self.target.get("audio_tracks") or []):
            lang = str(t.get("language", "")).strip()
            if lang and lang.lower() not in seen:
                seen.add(lang.lower())
                combined.append({"language": lang, "url": str(t.get("url", ""))})
        for lang_key, mapped_url in (self.target.get("audio_track_urls") or {}).items():
            k = str(lang_key).strip()
            if k and k.lower() not in seen:
                seen.add(k.lower())
                combined.append({"language": k, "url": str(self.target.get("target_url") or mapped_url)})
        return combined

    def _build_ui(self) -> None:
        root = QVBoxLayout(self)
        root.setContentsMargins(0, 0, 0, 0)
        root.setSpacing(0)

        # Top HUD bar
        top_bar = QWidget()
        top_bar.setStyleSheet(
            f"background-color: {theme.ARC_SURFACE}; border-bottom: 1px solid {theme.ARC_BORDER};"
        )
        top_layout = QHBoxLayout(top_bar)
        top_layout.setContentsMargins(14, 10, 14, 10)
        top_layout.setSpacing(10)

        self.title_label = QLabel(str(self.target.get("title") or "ARC Player"))
        self.title_label.setStyleSheet(f"color: {theme.ARC_WHITE}; font-size: 14px; font-weight: 700;")
        top_layout.addWidget(self.title_label, 1)

        self.status_badge = QLabel("Buffering...")
        self.status_badge.setStyleSheet(
            f"background-color: {theme.ARC_RAISED}; color: {theme.ARC_MUTED};"
            f" border: 1px solid {theme.ARC_BORDER_STRONG}; border-radius: 10px;"
            f" padding: 4px 10px; font-size: 11px; font-weight: 600;"
        )
        top_layout.addWidget(self.status_badge)

        self.aspect_btn = QPushButton("Fit")
        self.aspect_btn.setObjectName("Chip")
        self.aspect_btn.setCursor(Qt.PointingHandCursor)
        self.aspect_btn.clicked.connect(self._toggle_aspect)
        top_layout.addWidget(self.aspect_btn)

        self.fullscreen_btn = QPushButton("Fullscreen")
        self.fullscreen_btn.setObjectName("Chip")
        self.fullscreen_btn.setCursor(Qt.PointingHandCursor)
        self.fullscreen_btn.clicked.connect(self._toggle_fullscreen)
        top_layout.addWidget(self.fullscreen_btn)

        root.addWidget(top_bar)

        # Video viewport (or centered icon for audio-only)
        viewport = QWidget()
        viewport.setStyleSheet(f"background-color: #000000;")
        vp_layout = QVBoxLayout(viewport)
        vp_layout.setContentsMargins(0, 0, 0, 0)
        vp_layout.setSpacing(0)

        self.video_widget = QVideoWidget()
        self.video_widget.setAspectRatioMode(Qt.KeepAspectRatio)
        vp_layout.addWidget(self.video_widget, 1)

        if self.target.get("is_audio_only"):
            audio_banner = QLabel(f"Audio Stream\n{self.target.get('title') or 'Playing audio'}")
            audio_banner.setAlignment(Qt.AlignCenter)
            audio_banner.setWordWrap(True)
            audio_banner.setStyleSheet(
                f"color: {theme.ARC_WHITE}; background-color: #080808;"
                f" font-size: 16px; font-weight: 700; padding: 24px;"
            )
            vp_layout.addWidget(audio_banner, 1)
            self.video_widget.setVisible(False)

        self.sub_overlay = QLabel(viewport)
        self.sub_overlay.setAlignment(Qt.AlignCenter)
        self.sub_overlay.setWordWrap(True)
        self.sub_overlay.setVisible(False)
        self.sub_overlay.setAttribute(Qt.WA_TransparentForMouseEvents, True)
        self.sub_overlay.setStyleSheet(
            f"background-color: rgba(5, 5, 5, 200); color: {theme.ARC_WHITE};"
            f" border: 1px solid {theme.ARC_BORDER_SUBTLE}; border-radius: 10px;"
            f" padding: 6px 14px; font-size: 16px; font-weight: 700;"
        )
        self._viewport = viewport

        self.error_banner = QWidget()
        self.error_banner.setVisible(False)
        self.error_banner.setStyleSheet(
            f"background-color: #201111; border-top: 1px solid #442222;"
        )
        err_layout = QHBoxLayout(self.error_banner)
        err_layout.setContentsMargins(16, 10, 16, 10)
        self.error_label = QLabel("")
        self.error_label.setStyleSheet(f"color: {theme.ARC_ERROR}; font-size: 12px; font-weight: 600;")
        retry_btn = QPushButton("Retry")
        retry_btn.setObjectName("Chip")
        retry_btn.setCursor(Qt.PointingHandCursor)
        retry_btn.clicked.connect(self._retry_playback)
        err_layout.addWidget(self.error_label, 1)
        err_layout.addWidget(retry_btn)
        vp_layout.addWidget(self.error_banner)

        root.addWidget(viewport, 1)

        # Bottom controls panel
        controls = QWidget()
        controls.setStyleSheet(
            f"background-color: {theme.ARC_SURFACE}; border-top: 1px solid {theme.ARC_BORDER};"
        )
        ctrl_root = QVBoxLayout(controls)
        ctrl_root.setContentsMargins(16, 10, 16, 12)
        ctrl_root.setSpacing(8)

        # Timeline row
        time_row = QHBoxLayout()
        time_row.setSpacing(10)
        self.pos_label = QLabel("0:00")
        self.pos_label.setStyleSheet(f"color: {theme.ARC_MUTED}; font-family: {theme.MONO_FAMILY}; font-size: 11px;")
        self.seek_slider = QSlider(Qt.Horizontal)
        self.seek_slider.setRange(0, 0)
        self.seek_slider.setCursor(Qt.PointingHandCursor)
        self.seek_slider.sliderPressed.connect(self._on_scrub_start)
        self.seek_slider.sliderReleased.connect(self._on_scrub_end)
        self.seek_slider.sliderMoved.connect(self._on_scrub_moved)
        self.dur_label = QLabel("0:00")
        self.dur_label.setStyleSheet(f"color: {theme.ARC_MUTED}; font-family: {theme.MONO_FAMILY}; font-size: 11px;")
        time_row.addWidget(self.pos_label)
        time_row.addWidget(self.seek_slider, 1)
        time_row.addWidget(self.dur_label)
        ctrl_root.addLayout(time_row)

        # Transport + tracks row
        bar = QHBoxLayout()
        bar.setSpacing(8)

        self.rewind_btn = QPushButton("-10s")
        self.rewind_btn.setObjectName("Ghost")
        self.rewind_btn.setCursor(Qt.PointingHandCursor)
        self.rewind_btn.clicked.connect(lambda: self._seek_relative(-10_000))

        self.play_btn = QPushButton("Pause")
        self.play_btn.setObjectName("Primary")
        self.play_btn.setCursor(Qt.PointingHandCursor)
        self.play_btn.setMinimumWidth(84)
        self.play_btn.clicked.connect(self._toggle_play)

        self.fwd_btn = QPushButton("+10s")
        self.fwd_btn.setObjectName("Ghost")
        self.fwd_btn.setCursor(Qt.PointingHandCursor)
        self.fwd_btn.clicked.connect(lambda: self._seek_relative(10_000))

        bar.addWidget(self.rewind_btn)
        bar.addWidget(self.play_btn)
        bar.addWidget(self.fwd_btn)

        bar.addSpacing(8)
        self.mute_btn = QPushButton("Vol")
        self.mute_btn.setObjectName("Ghost")
        self.mute_btn.setCursor(Qt.PointingHandCursor)
        self.mute_btn.clicked.connect(self._toggle_mute)
        self.vol_slider = QSlider(Qt.Horizontal)
        self.vol_slider.setRange(0, 100)
        self.vol_slider.setValue(100)
        self.vol_slider.setFixedWidth(90)
        self.vol_slider.valueChanged.connect(self._on_volume_changed)
        bar.addWidget(self.mute_btn)
        bar.addWidget(self.vol_slider)

        bar.addStretch(1)

        # Speed selector
        self.speed_combo = QComboBox()
        self.speed_combo.addItems(["0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x"])
        self.speed_combo.setCurrentText("1.0x")
        self.speed_combo.currentTextChanged.connect(self._on_speed_changed)
        bar.addWidget(self.speed_combo)

        # Quality selector
        self.quality_combo = QComboBox()
        q_items = self._selectable_qualities or ["Original"]
        self.quality_combo.addItems(q_items)
        if self._selected_quality in q_items:
            self.quality_combo.setCurrentText(self._selected_quality)
        self.quality_combo.setVisible(bool(self._selectable_qualities))
        self.quality_combo.currentTextChanged.connect(self._on_quality_changed)
        bar.addWidget(self.quality_combo)

        # Audio track selector
        self.audio_combo = QComboBox()
        a_items = [a["language"] for a in self._audio_options]
        if a_items:
            self.audio_combo.addItems(a_items)
            if self._selected_audio_lang in a_items:
                self.audio_combo.setCurrentText(self._selected_audio_lang)
        self.audio_combo.setVisible(len(a_items) > 1)
        self.audio_combo.currentTextChanged.connect(self._on_audio_changed)
        bar.addWidget(self.audio_combo)

        # Subtitles selector (uploaded/manual + local .srt/.vtt)
        self.sub_combo = QComboBox()
        self._populate_sub_combo()
        self.sub_combo.setVisible(not bool(self.target.get("is_audio_only")))
        self.sub_combo.currentIndexChanged.connect(self._on_sub_index_changed)
        bar.addWidget(self.sub_combo)

        ctrl_root.addLayout(bar)
        root.addWidget(controls)

    def _wire_player_events(self) -> None:
        self._player.positionChanged.connect(self._on_position_changed)
        self._player.durationChanged.connect(self._on_duration_changed)
        self._player.playbackStateChanged.connect(self._on_playback_state_changed)
        self._player.mediaStatusChanged.connect(self._on_media_status_changed)
        self._player.errorOccurred.connect(self._on_player_error)

    def _wire_shortcuts(self) -> None:
        QShortcut(QKeySequence(Qt.Key_Space), self, activated=self._toggle_play)
        QShortcut(QKeySequence(Qt.Key_Left), self, activated=lambda: self._seek_relative(-10_000))
        QShortcut(QKeySequence(Qt.Key_Right), self, activated=lambda: self._seek_relative(10_000))
        QShortcut(QKeySequence(Qt.Key_F), self, activated=self._toggle_fullscreen)
        QShortcut(QKeySequence(Qt.Key_M), self, activated=self._toggle_mute)
        QShortcut(QKeySequence(Qt.Key_Up), self, activated=lambda: self.vol_slider.setValue(min(100, self.vol_slider.value() + 10)))
        QShortcut(QKeySequence(Qt.Key_Down), self, activated=lambda: self.vol_slider.setValue(max(0, self.vol_slider.value() - 10)))

    # -------------------------------------------------------- active track
    def _active_track(self) -> dict:
        qt = self.target.get("quality_tracks") or {}
        qu = self.target.get("quality_urls") or {}
        fb_spec = {
            "url": str(self.target.get("url") or ""),
            "video_url": str(self.target.get("video_url") or ""),
            "audio_url": str(self.target.get("audio_url") or ""),
            "fallback_url": str(self.target.get("fallback_url") or ""),
            "video_format": str(self.target.get("video_format") or ""),
            "audio_format": str(self.target.get("audio_format") or ""),
            "fallback_format": str(self.target.get("fallback_format") or ""),
        }
        spec = resolve_track_for_quality(
            self._selected_quality,
            qt,
            qu,
            str(self.target.get("url") or ""),
            fb_spec,
        )
        if self._override_audio_url and (spec.get("video_url") or self.target.get("is_audio_only")):
            spec["audio_url"] = self._override_audio_url
            if self.target.get("is_audio_only"):
                spec["url"] = self._override_audio_url
        return spec

    def _is_youtube(self, active: dict) -> bool:
        t_url = str(self.target.get("target_url") or "")
        primary = str(active.get("url") or self.target.get("url") or "")
        v_url = str(active.get("video_url") or "")
        return any("youtu" in s.lower() or "googlevideo.com" in s.lower() for s in (t_url, primary, v_url))

    def _load_active_source(self) -> None:
        self._load_generation += 1
        gen = self._load_generation
        self.error_banner.setVisible(False)
        self.status_badge.setText("Loading...")

        active = self._active_track()
        primary_url = str(active.get("url") or self.target.get("url") or "")
        fallback_url = self._lazy_proxy_url or str(
            active.get("fallback_url")
            or (primary_url if active.get("video_url") and primary_url != active.get("video_url") else "")
        )

        # If local file, play cleanly without proxy
        local_path = str(self.target.get("download_file_path") or "")
        if local_path and Path(local_path).is_file():
            self._play_single_url(QUrl.fromLocalFile(local_path))
            return

        has_split = (
            not self._using_fallback
            and not self.target.get("is_audio_only")
            and bool(active.get("video_url"))
            and bool(active.get("audio_url"))
        )
        if has_split:
            self._play_split_urls(str(active["video_url"]), str(active["audio_url"]))
            return

        single = (fallback_url or primary_url) if self._using_fallback else primary_url
        if not single:
            self._show_error("No playable stream URL.")
            return

        headers = dict(self.target.get("headers") or {})
        needs_proxy = (
            single.startswith(("http://", "https://"))
            and "127.0.0.1" not in single
            and (
                bool(headers)
                or "hakunaymatata" in single.lower()
                or "mzfi.me" in single.lower()
                or "netmirror" in single.lower()
            )
            and "googlevideo.com" not in single.lower()
        )
        if needs_proxy and self.bridge and self.bridge.ready:
            self.status_badge.setText("Starting proxy...")
            self.bridge.submit(
                {"action": "stream_proxy", "url": single, "headers": headers},
                lambda res, g=gen, orig=single: self._signals.proxy_ready.emit(
                    g, str(((res.get("result") or {}).get("proxy_url") or orig))
                ),
            )
            return

        self._play_single_url(QUrl(single))

    def _on_proxy_ready(self, gen: int, resolved_url: str) -> None:
        if self._closing or gen != self._load_generation:
            return
        self._play_single_url(QUrl(resolved_url))

    def _play_single_url(self, qurl: QUrl) -> None:
        self._has_audio_slave = False
        self._slave_audio.stop()
        self._slave_audio.setSource(QUrl())
        self._player.setAudioOutput(self._audio_output)
        self._player.setSource(qurl)
        self._player.setPlaybackRate(self._speed)
        if self._resume_seek_ms > 0:
            QTimer.singleShot(250, lambda pos=self._resume_seek_ms: self._apply_resume_seek(pos))
        self._player.play()
        self._arm_stall_watchdog()

    def _play_split_urls(self, video_url: str, audio_url: str) -> None:
        self._has_audio_slave = True
        self._player.setAudioOutput(self._muted_video_audio)
        self._slave_audio.setAudioOutput(self._slave_audio_output)
        self._player.setSource(QUrl(video_url))
        self._slave_audio.setSource(QUrl(audio_url))
        self._player.setPlaybackRate(self._speed)
        self._slave_audio.setPlaybackRate(self._speed)
        if self._resume_seek_ms > 0:
            QTimer.singleShot(300, lambda pos=self._resume_seek_ms: self._apply_resume_seek(pos))
        self._player.play()
        self._slave_audio.play()
        self._arm_stall_watchdog()

    def _apply_resume_seek(self, pos: int) -> None:
        if pos > 0:
            self._resume_seek_ms = 0
            self._player.setPosition(pos)
            if self._has_audio_slave:
                self._slave_audio.setPosition(pos)

    def _arm_stall_watchdog(self) -> None:
        active = self._active_track()
        if self._is_youtube(active) or self._using_fallback or self.target.get("is_audio_only"):
            self._stall_timer.stop()
            return
        self._stall_timer.start(7000)

    def _on_stall_timeout(self) -> None:
        if self._closing or self._player.position() > 0 or self.error_banner.isVisible():
            return
        active = self._active_track()
        if not self._is_youtube(active) and not self._using_fallback and (active.get("fallback_url") or self._lazy_proxy_url):
            self._using_fallback = True
            self._load_active_source()

    # -------------------------------------------------------- Qt callbacks
    def _on_position_changed(self, pos: int) -> None:
        if pos > 0:
            self._stall_timer.stop()
        if not self._user_scrubbing:
            self.seek_slider.setValue(pos)
            self.pos_label.setText(_format_ms(pos))
        self._update_subtitle_cue(pos)

    def _on_duration_changed(self, dur: int) -> None:
        self._duration_ms = max(0, dur)
        self.seek_slider.setRange(0, self._duration_ms)
        self.dur_label.setText(_format_ms(self._duration_ms))

    def _on_playback_state_changed(self, state: QMediaPlayer.PlaybackState) -> None:
        playing = state == QMediaPlayer.PlayingState
        self.play_btn.setText("Pause" if playing else "Play")
        if self._has_audio_slave:
            if playing and self._slave_audio.playbackState() != QMediaPlayer.PlayingState:
                self._slave_audio.play()
            elif not playing and self._slave_audio.playbackState() == QMediaPlayer.PlayingState:
                self._slave_audio.pause()

    def _on_media_status_changed(self, status: QMediaPlayer.MediaStatus) -> None:
        if status in (QMediaPlayer.LoadingMedia, QMediaPlayer.BufferingMedia, QMediaPlayer.StalledMedia):
            self.status_badge.setText(f"Buffering ({self._selected_quality.split(' (')[0]})")
            if self._has_audio_slave and status == QMediaPlayer.BufferingMedia:
                self._slave_audio.pause()
        elif status in (QMediaPlayer.BufferedMedia, QMediaPlayer.LoadedMedia):
            self._stall_timer.stop()
            self.status_badge.setText(self._selected_quality.split(" (")[0])
            if self._has_audio_slave and self._player.playbackState() == QMediaPlayer.PlayingState:
                self._slave_audio.play()
        elif status == QMediaPlayer.EndOfMedia:
            self.status_badge.setText("Ended")
            self.play_btn.setText("Play")

    def _on_player_error(self, _error, error_string: str) -> None:
        active = self._active_track()
        if self._is_youtube(active):
            # Never auto-switch YouTube quality on error; keep buffering badge until changed manually.
            self.status_badge.setText(f"Buffering ({self._selected_quality.split(' (')[0]})...")
            return
        if not self._using_fallback and (active.get("fallback_url") or self._lazy_proxy_url):
            self._using_fallback = True
            self._load_active_source()
            return
        self._show_error(error_string or "Playback unavailable")

    def _sync_slave_tick(self) -> None:
        if not self._has_audio_slave or self._closing:
            return
        if self._player.playbackState() != QMediaPlayer.PlayingState:
            return
        diff = abs(self._slave_audio.position() - self._player.position())
        if diff > 160:
            self._slave_audio.setPosition(self._player.position())

    # -------------------------------------------------------- UI actions
    def _toggle_play(self) -> None:
        if self._player.playbackState() == QMediaPlayer.PlayingState:
            self._player.pause()
            if self._has_audio_slave:
                self._slave_audio.pause()
        else:
            if self._player.mediaStatus() == QMediaPlayer.EndOfMedia:
                self._player.setPosition(0)
                if self._has_audio_slave:
                    self._slave_audio.setPosition(0)
            self._player.play()
            if self._has_audio_slave:
                self._slave_audio.play()

    def _seek_relative(self, delta_ms: int) -> None:
        pos = max(0, self._player.position() + delta_ms)
        if self._duration_ms > 0:
            pos = min(pos, self._duration_ms)
        self._player.setPosition(pos)
        if self._has_audio_slave:
            self._slave_audio.setPosition(pos)

    def _on_scrub_start(self) -> None:
        self._user_scrubbing = True

    def _on_scrub_moved(self, pos: int) -> None:
        self.pos_label.setText(_format_ms(pos))

    def _on_scrub_end(self) -> None:
        pos = self.seek_slider.value()
        self._user_scrubbing = False
        self._player.setPosition(pos)
        if self._has_audio_slave:
            self._slave_audio.setPosition(pos)

    def _on_volume_changed(self, value: int) -> None:
        vol = max(0.0, min(1.0, value / 100.0))
        self._audio_output.setVolume(vol)
        self._slave_audio_output.setVolume(vol)
        if value > 0 and self._audio_output.isMuted():
            self._audio_output.setMuted(False)
            self._slave_audio_output.setMuted(False)
            self.mute_btn.setText("Vol")

    def _toggle_mute(self) -> None:
        muted = not self._audio_output.isMuted()
        self._audio_output.setMuted(muted)
        self._slave_audio_output.setMuted(muted)
        self.mute_btn.setText("Muted" if muted else "Vol")

    def _on_speed_changed(self, text: str) -> None:
        try:
            rate = float(text.rstrip("x"))
        except ValueError:
            rate = 1.0
        self._speed = rate
        self._player.setPlaybackRate(rate)
        if self._has_audio_slave:
            self._slave_audio.setPlaybackRate(rate)

    def _on_quality_changed(self, q_opt: str) -> None:
        if not q_opt or q_opt == self._selected_quality:
            return
        self._using_fallback = False
        self._lazy_proxy_url = ""
        self._resume_seek_ms = max(0, self._player.position())
        self._selected_quality = q_opt
        self._load_active_source()

    def _on_audio_changed(self, lang: str) -> None:
        if not lang or lang == self._selected_audio_lang:
            return
        cur_pos = max(0, self._player.position())
        direct_urls = self.target.get("audio_track_urls") or {}
        matched_url = next((v for k, v in direct_urls.items() if str(k).lower() == lang.lower()), "")
        active = self._active_track()
        if matched_url and (active.get("video_url") or self.target.get("is_audio_only")):
            self._resume_seek_ms = cur_pos
            self._selected_audio_lang = lang
            self._override_audio_url = str(matched_url)
            self._using_fallback = False
            self._load_active_source()
            return

        choice = next((a for a in self._audio_options if a["language"].lower() == lang.lower()), None)
        resolve_url = str((choice or {}).get("url") or self.target.get("target_url") or "")
        if not resolve_url or not self.bridge or not self.bridge.ready:
            return
        self.status_badge.setText(f"Switching audio: {lang}...")
        gen = self._load_generation
        self._resume_seek_ms = cur_pos
        payload = {
            "action": "resolve",
            "url": resolve_url,
            "audio_lang": lang,
            "quality": "Best Available" if "ask" in self._selected_quality.lower() else self._selected_quality,
        }
        if self.target.get("is_series"):
            payload["season"] = int(self.target.get("selected_season") or 1)
            payload["episode"] = int(self.target.get("selected_episode") or 1)
        self.bridge.submit(payload, lambda res, g=gen, lg=lang: self._signals.audio_resolved.emit(g, res, lg))

    def _on_audio_resolved(self, gen: int, res: dict, lang: str) -> None:
        if self._closing or gen != self._load_generation:
            return
        if not res.get("ok"):
            self._show_error(str(res.get("error") or f"Could not switch audio to {lang}"))
            return
        data = dict(res.get("result") or {})
        qu = dict(data.get("quality_urls") or {})
        qt = dict(data.get("quality_tracks") or {})
        direct = str(data.get("direct_url") or data.get("stream_url") or data.get("url") or "")
        spec = resolve_track_for_quality(self._selected_quality, qt, qu, direct, data)
        self._override_audio_url = ""
        self._using_fallback = False
        self._selected_audio_lang = lang
        if direct:
            self.target["url"] = spec.get("url") or direct
        self.target["video_url"] = spec.get("video_url", "")
        self.target["audio_url"] = spec.get("audio_url", "")
        self.target["fallback_url"] = spec.get("fallback_url", "")
        if qt:
            self.target["quality_tracks"] = qt
        if qu:
            self.target["quality_urls"] = qu
        if data.get("stream_headers") or data.get("headers"):
            self.target["headers"] = data.get("stream_headers") or data.get("headers")
        self._load_active_source()

    def _toggle_aspect(self) -> None:
        if self.video_widget.aspectRatioMode() == Qt.KeepAspectRatio:
            self.video_widget.setAspectRatioMode(Qt.KeepAspectRatioByExpanding)
            self.aspect_btn.setText("Crop")
        else:
            self.video_widget.setAspectRatioMode(Qt.KeepAspectRatio)
            self.aspect_btn.setText("Fit")

    def _toggle_fullscreen(self) -> None:
        if self.isFullScreen():
            self.showNormal()
            self.fullscreen_btn.setText("Fullscreen")
        else:
            self.showFullScreen()
            self.fullscreen_btn.setText("Exit Fullscreen")

    def _retry_playback(self) -> None:
        self._using_fallback = False
        self._load_active_source()

    def _show_error(self, msg: str) -> None:
        self.error_label.setText(msg)
        self.error_banner.setVisible(True)
        self.status_badge.setText("Error")

    # -------------------------------------------------------- subtitles
    def _populate_sub_combo(self, select_label: str = "") -> None:
        self.sub_combo.blockSignals(True)
        self.sub_combo.clear()
        self.sub_combo.addItem("CC: Off", "")
        target_idx = 0
        for opt in self._subtitle_options:
            lbl = opt["label"]
            self.sub_combo.addItem(f"CC: {lbl}", opt["url"])
            if select_label and (
                lbl.lower() == select_label.lower()
                or opt["lang"].lower() == select_label.lower()
            ):
                target_idx = self.sub_combo.count() - 1
        self.sub_combo.addItem("Load .srt/.vtt...", "__pick_file__")
        self.sub_combo.setCurrentIndex(target_idx)
        self.sub_combo.blockSignals(False)

    def _activate_subtitle_by_key(self, key: str) -> None:
        for idx, opt in enumerate(self._subtitle_options, start=1):
            if opt["label"].lower() == key.lower() or opt["lang"].lower() == key.lower():
                self.sub_combo.blockSignals(True)
                self.sub_combo.setCurrentIndex(idx)
                self.sub_combo.blockSignals(False)
                self._load_subtitle_source(opt["url"], opt["label"])
                return

    def _on_sub_index_changed(self, index: int) -> None:
        if index < 0:
            return
        data = str(self.sub_combo.itemData(index) or "")
        if not data:
            self._sub_generation += 1
            self._sub_cues = []
            self._sub_starts = []
            self._last_cue_text = ""
            self.sub_overlay.setVisible(False)
            return
        if data == "__pick_file__":
            path, _ = QFileDialog.getOpenFileName(
                self, "Load Subtitle File", "", "Subtitle Files (*.srt *.vtt);;All Files (*)"
            )
            if path:
                lbl = Path(path).name
                self._subtitle_options.append(
                    {"lang": "local", "label": lbl, "url": path, "is_local": True}
                )
                self._populate_sub_combo(select_label=lbl)
                self._load_subtitle_source(path, lbl)
            else:
                self.sub_combo.blockSignals(True)
                self.sub_combo.setCurrentIndex(0)
                self.sub_combo.blockSignals(False)
            return
        lbl = self.sub_combo.itemText(index).removeprefix("CC: ").strip()
        self._load_subtitle_source(data, lbl)

    def _load_subtitle_source(self, source_url: str, label: str) -> None:
        self._sub_generation += 1
        gen = self._sub_generation
        p = Path(source_url)
        if p.is_file():
            try:
                raw = p.read_text(encoding="utf-8", errors="replace")
            except Exception:
                raw = ""
            self._on_subtitles_loaded(gen, label, _parse_subtitles_text(raw))
            return

        headers = dict(self.target.get("headers") or {})
        self.status_badge.setText(f"Loading CC: {label}...")

        def _worker() -> None:
            cues: list[tuple[int, int, str]] = []
            try:
                req = urllib.request.Request(
                    source_url,
                    headers={"User-Agent": headers.get("User-Agent", "Mozilla/5.0"), **headers},
                )
                with urllib.request.urlopen(req, timeout=15) as resp:
                    raw = resp.read().decode("utf-8", errors="replace")
                cues = _parse_subtitles_text(raw)
            except Exception:
                cues = []
            self._signals.subtitles_loaded.emit(gen, label, cues)

        threading.Thread(target=_worker, daemon=True).start()

    def _on_subtitles_loaded(self, gen: int, label: str, cues_obj: object) -> None:
        if self._closing or gen != self._sub_generation:
            return
        cues = list(cues_obj) if isinstance(cues_obj, list) else []
        self._sub_cues = cues
        self._sub_starts = [c[0] for c in cues]
        self._last_cue_text = ""
        if cues:
            self.status_badge.setText(f"{self._selected_quality.split(' (')[0]} · CC: {label}")
            self._update_subtitle_cue(self._player.position())
        else:
            self.sub_overlay.setVisible(False)
            self.status_badge.setText("Subtitle unavailable")

    def _update_subtitle_cue(self, pos_ms: int) -> None:
        if not self._sub_cues:
            if self.sub_overlay.isVisible():
                self.sub_overlay.setVisible(False)
            return
        idx = bisect.bisect_right(self._sub_starts, pos_ms) - 1
        cue_text = ""
        while idx >= 0:
            start_ms, end_ms, txt = self._sub_cues[idx]
            if start_ms <= pos_ms <= end_ms:
                cue_text = txt
                break
            if pos_ms - start_ms > 15_000:
                break
            idx -= 1
        if cue_text == self._last_cue_text and self.sub_overlay.isVisible() == bool(cue_text):
            return
        self._last_cue_text = cue_text
        if not cue_text:
            self.sub_overlay.setVisible(False)
            return
        self.sub_overlay.setText(cue_text)
        self._reposition_subtitle_overlay()
        self.sub_overlay.setVisible(True)
        self.sub_overlay.raise_()

    def _reposition_subtitle_overlay(self) -> None:
        vp = getattr(self, "_viewport", None)
        if vp is None or not self.sub_overlay.text():
            return
        max_w = max(240, int(vp.width() * 0.78))
        self.sub_overlay.setMaximumWidth(max_w)
        self.sub_overlay.adjustSize()
        w = min(max_w, self.sub_overlay.sizeHint().width() + 8)
        h = self.sub_overlay.sizeHint().height()
        x = max(12, (vp.width() - w) // 2)
        y = max(12, vp.height() - h - 22)
        self.sub_overlay.setGeometry(x, y, w, h)

    def resizeEvent(self, event) -> None:  # noqa: N802
        super().resizeEvent(event)
        if self.sub_overlay.isVisible():
            self._reposition_subtitle_overlay()

    def keyPressEvent(self, event) -> None:  # noqa: N802
        if event.key() == Qt.Key_Escape and self.isFullScreen():
            self._toggle_fullscreen()
            return
        super().keyPressEvent(event)

    def closeEvent(self, event) -> None:  # noqa: N802
        self._closing = True
        self._sync_timer.stop()
        self._stall_timer.stop()
        self._player.stop()
        self._slave_audio.stop()
        super().closeEvent(event)
