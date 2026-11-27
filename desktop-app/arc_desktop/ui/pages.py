"""The six pages, as pure views.

Each page owns layout and emits signals; MainWindow owns state and talks to the
engine. That split keeps this file free of threading and JSON handling.
"""

from __future__ import annotations

import re
from typing import Optional

from PySide6.QtCore import Qt, Signal

from PySide6.QtWidgets import (
    QCheckBox, QComboBox, QFileDialog, QFrame, QGridLayout, QHBoxLayout, QLabel,
    QLineEdit, QMessageBox, QPlainTextEdit, QPushButton, QScrollArea, QSizePolicy,
    QVBoxLayout, QWidget, QStackedWidget, QButtonGroup,
)

from .. import theme
from ..models import prioritize_audio_choices
from ..store import human_size
from .widgets import (
    AsyncImage, ChipBar, EmptyState, JobCard, LibraryCard, SectionHeader,
    StatusPill, ThinkingCard, card, divider, label,
)

AUDIO_QUALITIES = [
    "Best Audio (320kbps)",
    "High Audio (256kbps)",
    "Standard Audio (192kbps)",
    "Compact Audio (128kbps)",
]
VIDEO_QUALITY_DEFAULTS = [
    "Best Available", "2160p (4K UHD)", "1440p (2K QHD)", "1080p (Full HD)",
    "720p (HD)", "480p (SD)", "360p", "240p", "144p",
]


class _ScrollPage(QScrollArea):
    """Base: a full-height scroll area with the standard page padding."""

    def __init__(self, parent=None) -> None:
        super().__init__(parent)
        self.setWidgetResizable(True)
        self.setFrameShape(QFrame.NoFrame)
        self.setHorizontalScrollBarPolicy(Qt.ScrollBarAlwaysOff)
        self._host = QWidget()
        self.layout_ = QVBoxLayout(self._host)
        self.layout_.setContentsMargins(26, 24, 26, 28)
        self.layout_.setSpacing(16)
        self.layout_.setAlignment(Qt.AlignTop)
        self.setWidget(self._host)

    def add(self, widget: QWidget) -> None:
        self.layout_.addWidget(widget)

    def add_layout(self, layout) -> None:
        self.layout_.addLayout(layout)

    def add_stretch(self) -> None:
        self.layout_.addStretch(1)

    def clear_dynamic(self, start_index: int) -> None:
        while self.layout_.count() > start_index:
            item = self.layout_.takeAt(self.layout_.count() - 1)
            widget = item.widget()
            if widget is not None:
                widget.deleteLater()


# =====================================================================  HOME
class HomePage(QWidget):
    analyze_requested = Signal(str)
    pick_torrent_requested = Signal(str)
    library_requested = Signal()

    def __init__(self, parent=None) -> None:
        super().__init__(parent)
        root = QVBoxLayout(self)
        root.setContentsMargins(0, 0, 0, 0)
        root.setSpacing(0)

        scroll = _ScrollPage()
        root.addWidget(scroll)

        header, header_layout = card("horizontal", 12, 0, "Card")
        header.setObjectName("")
        header.setStyleSheet("background: transparent; border: none;")
        brand_tile = QLabel("\u21E9")
        brand_tile.setFixedSize(42, 42)
        brand_tile.setAlignment(Qt.AlignCenter)
        brand_tile.setStyleSheet(
            f"background-color: {theme.ARC_WHITE}; color: {theme.ARC_BLACK};"
            f" border-radius: 14px; font-size: 20px; font-weight: 800;"
        )
        header_layout.addWidget(brand_tile)
        title_column = QVBoxLayout()
        title_column.setContentsMargins(0, 0, 0, 0)
        title_column.setSpacing(1)
        brand = QLabel("ARC")
        brand.setObjectName("NavBrand")
        sub = QLabel("Universal Downloader for Windows")
        sub.setObjectName("NavBrandSub")
        title_column.addWidget(brand)
        title_column.addWidget(sub)
        header_layout.addLayout(title_column)
        header_layout.addStretch(1)
        self.status_pill = StatusPill("Ready")
        header_layout.addWidget(self.status_pill)
        scroll.add(header)

        hero = QVBoxLayout()
        hero.setContentsMargins(2, 6, 2, 0)
        hero.setSpacing(5)
        hero.addWidget(label("Paste a link or search", "ScreenTitle"))
        hero.addWidget(label("Download and stream media directly on your PC", "ScreenSubtitle"))
        scroll.add_layout(hero)

        input_card, input_layout = card("vertical", 10)
        self.input_field = QLineEdit()
        self.input_field.setPlaceholderText("Link or search")
        self.input_field.setClearButtonEnabled(True)
        self.input_field.returnPressed.connect(self._submit)
        input_layout.addWidget(self.input_field)

        button_row = QHBoxLayout()
        button_row.setSpacing(10)
        self.torrent_button = QPushButton(".torrent")
        self.torrent_button.setObjectName("Ghost")
        self.torrent_button.setCursor(Qt.PointingHandCursor)
        self.torrent_button.setToolTip("Open local .torrent file")
        self.torrent_button.clicked.connect(self._pick_torrent)
        self.paste_button = QPushButton("Paste")
        self.paste_button.setObjectName("Ghost")
        self.paste_button.setCursor(Qt.PointingHandCursor)
        self.paste_button.clicked.connect(self._paste)
        self.analyze_button = QPushButton("Analyze")
        self.analyze_button.setObjectName("Primary")
        self.analyze_button.setCursor(Qt.PointingHandCursor)
        self.analyze_button.clicked.connect(self._submit)
        button_row.addWidget(self.torrent_button)
        button_row.addWidget(self.paste_button)
        button_row.addWidget(self.analyze_button, 1)
        input_layout.addLayout(button_row)

        scroll.add(input_card)

        self.recent_header = SectionHeader("Recent Downloads", "See Library")
        if self.recent_header.clicked is not None:
            self.recent_header.action_button.clicked.connect(lambda: self.library_requested.emit())
        scroll.add(self.recent_header)

        self.recent_host = QVBoxLayout()
        self.recent_host.setContentsMargins(0, 0, 0, 0)
        self.recent_host.setSpacing(12)
        scroll.add_layout(self.recent_host)
        scroll.add_stretch()
        self._scroll = scroll
        self._recent_cards: list[LibraryCard] = []

    # ------------------------------------------------------------------ slots
    def set_input(self, text: str) -> None:
        self.input_field.setText(text)

    def input_text(self) -> str:
        return self.input_field.text().strip()

    def set_busy(self, busy: bool) -> None:
        self.analyze_button.setEnabled(not busy)
        self.input_field.setEnabled(not busy)
        self.status_pill.set_text("Working..." if busy else "Ready",
                                  theme.ARC_WARNING if busy else theme.ARC_MUTED)

    def set_status_pill(self, text: str, color: Optional[str] = None) -> None:
        self.status_pill.set_text(text, color or theme.ARC_MUTED)

    def set_recent(self, entries: list[dict], on_open, on_folder, on_delete) -> None:
        for old in self._recent_cards:
            old.deleteLater()
        self._recent_cards.clear()
        for entry in entries[:3]:
            card_widget = LibraryCard(entry)
            card_widget.open_clicked.connect(on_open)
            card_widget.folder_clicked.connect(on_folder)
            card_widget.delete_clicked.connect(on_delete)
            self.recent_host.addWidget(card_widget)
            self._recent_cards.append(card_widget)
        if not self._recent_cards:
            empty = EmptyState("No Downloads Yet",
                               "Finished files appear here and in the Library tab.")
            self.recent_host.addWidget(empty)
            self._recent_cards.append(empty)  # type: ignore[arg-type]

    def _submit(self) -> None:
        text = self.input_text()
        if text:
            self.analyze_requested.emit(text)

    def _paste(self) -> None:
        clipboard = QApplication_clipboard()
        if clipboard:
            self.input_field.setText(clipboard)
            self.input_field.setFocus()

    def _pick_torrent(self) -> None:
        path, _ = QFileDialog.getOpenFileName(
            self, "Select .torrent file", "", "Torrent Files (*.torrent);;All Files (*)"
        )
        if path:
            self.input_field.setText(path)
            self.pick_torrent_requested.emit(path)


def QApplication_clipboard() -> str:
    from PySide6.QtWidgets import QApplication
    application = QApplication.instance()
    if application is None:
        return ""
    return (application.clipboard().text() or "").strip()


# ===================================================================  SEARCH
class SearchPage(QWidget):
    hit_selected = Signal(dict, str, str)
    back_requested = Signal()
    copy_logs = Signal()

    def __init__(self, parent=None) -> None:
        super().__init__(parent)
        root = QVBoxLayout(self)
        root.setContentsMargins(0, 0, 0, 0)
        root.setSpacing(0)
        self._scroll = _ScrollPage()
        root.addWidget(self._scroll)

        top, top_layout = card("horizontal", 12, 0)
        top.setObjectName("")
        top.setStyleSheet("background: transparent; border: none;")
        self.back_button = QPushButton("\u2190  Back")
        self.back_button.setObjectName("Ghost")
        self.back_button.setCursor(Qt.PointingHandCursor)
        self.back_button.clicked.connect(lambda: self.back_requested.emit())
        top_layout.addWidget(self.back_button)
        column = QVBoxLayout()
        column.setContentsMargins(0, 0, 0, 0)
        column.setSpacing(2)
        self.title_label = label("Search Results", "ScreenTitle")
        self.title_label.setStyleSheet("font-size: 21px; font-weight: 800;")
        self.query_label = label("", "Meta")
        column.addWidget(self.title_label)
        column.addWidget(self.query_label)
        top_layout.addLayout(column, 1)
        self._scroll.add(top)

        self.thinking = ThinkingCard()
        self.thinking.copy_logs.connect(lambda: self.copy_logs.emit())
        self._scroll.add(self.thinking)

        self.results_host = QVBoxLayout()
        self.results_host.setContentsMargins(0, 0, 0, 0)
        self.results_host.setSpacing(13)
        self._scroll.add_layout(self.results_host)
        self._scroll.add_stretch()
        self._cards: list[QWidget] = []

    def set_query(self, query: str, loading: bool) -> None:
        self.query_label.setText(f"\u201c{query}\u201d" + ("  \u00b7  searching..." if loading else ""))
        if loading:
            self.thinking.start("Search", "Searching media catalog")
            for old in self._cards:
                old.deleteLater()
            self._cards.clear()

    def set_results(self, query: str, hits: list[dict]) -> None:
        for old in self._cards:
            old.deleteLater()
        self._cards.clear()
        self.query_label.setText(f"\u201c{query}\u201d  \u00b7  {len(hits)} result(s)")
        # Successful search results are the primary content. The trace card's
        # collapsed Details row used to occupy the top of the viewport and make
        # users think the search returned nothing; keep it only for empty/error results.
        self.thinking.setVisible(not bool(hits))
        if not hits:
            empty = EmptyState("No Results",
                               "Nothing matched. Try a shorter title, or the exact year.")
            self.results_host.addWidget(empty)
            self._cards.append(empty)
            return

        for hit in hits:
            item, item_layout = card("vertical", 9)
            row = QHBoxLayout()
            row.setContentsMargins(0, 0, 0, 0)
            row.setSpacing(11)
            poster = AsyncImage(44, 60)
            row.addWidget(poster)
            column = QVBoxLayout()
            column.setContentsMargins(0, 0, 0, 0)
            column.setSpacing(2)
            name = QLabel(str(hit.get("title", "Untitled")))
            name.setStyleSheet("font-size: 14px; font-weight: 700;")
            name.setWordWrap(True)
            hit_url = str(hit.get("url", "") or "").lower()
            hit_src = str(hit.get("source", "") or "").strip()
            hit_type = str(hit.get("type", "") or "").strip()
            is_igl = (
                hit_url.startswith("igl:")
                or hit_src.lower() == "arc"
                or "latent" in hit_src.lower()
                or "latent" in str(hit.get("title", "")).lower()
                or hit_type.upper() == "IGL"
            )
            if is_igl:
                meta_text = "ARC"
            else:
                providers = hit.get("providers") or []
                if isinstance(providers, list):
                    prov_text = " \u00b7 ".join(str(p).strip() for p in providers if str(p).strip())
                else:
                    prov_text = str(providers).strip()
                clean_src = prov_text or (
                    hit_src if hit_src and hit_src.lower() not in ("netmirror", "movie", "tv series") else hit_type
                )
                meta_bits = []
                if clean_src:
                    meta_bits.append(clean_src)
                yr = str(hit.get("year", "") or "").strip()
                if yr and yr != "0":
                    meta_bits.append(yr)
                meta_text = "  \u00b7  ".join(meta_bits)
            column.addWidget(name)
            column.addWidget(label(meta_text, "Meta"))
            column.addStretch(1)
            row.addLayout(column, 1)
            arrow = QLabel("\u203A")
            arrow.setStyleSheet(f"color: {theme.ARC_SUBTLE}; font-size: 20px;")
            row.addWidget(arrow, 0, Qt.AlignVCenter)
            item_layout.addLayout(row)

            audios = prioritize_audio_choices(list(hit.get("audios") or []))
            chips = ChipBar()
            if audios:
                chips.set_items([str(a.get("language", "Default")) for a in audios])
                chips.selected.connect(
                    lambda lang, h=hit, a_list=audios: self._pick_audio(h, a_list, lang)
                )
            else:
                chips.set_items(["Use this result"])
                chips.selected.connect(lambda _lang, h=hit: self.hit_selected.emit(h, str(h.get("url", "")), ""))
            item_layout.addWidget(chips)

            item.setCursor(Qt.PointingHandCursor)
            open_button = QPushButton("Open")
            open_button.setObjectName("Chip")
            open_button.setCursor(Qt.PointingHandCursor)
            open_button.clicked.connect(lambda _=False, h=hit, a=audios: self._first(h, a))
            item_layout.addWidget(open_button, 0, Qt.AlignRight)
            self.results_host.addWidget(item)
            self._cards.append(item)
            item.show()
            # Keep the real result visible even when a poster host is slow.
            poster.set_url(str(hit.get("poster", "")))

    def _first(self, hit: dict, audios: list) -> None:
        if audios:
            first = audios[0]
            self.hit_selected.emit(hit, str(first.get("url", "")), str(first.get("language", "")))
        else:
            self.hit_selected.emit(hit, str(hit.get("url", "")), "")

    def _pick_audio(self, hit: dict, audios: list, language: str) -> None:
        for audio in audios:
            if str(audio.get("language", "")) == language:
                self.hit_selected.emit(hit, str(audio.get("url", "")), language)
                return
        self._first(hit, audios)


# =================================================================  ANALYSIS
class AnalysisPage(QWidget):
    download_requested = Signal()
    stream_requested = Signal()
    copy_link_requested = Signal()
    back_requested = Signal()
    copy_logs = Signal()
    quality_changed = Signal(str)
    audio_only_changed = Signal(bool)
    season_selected = Signal(int)
    episode_selected = Signal(int)
    audio_track_selected = Signal(str)
    playlist_item_selected = Signal(dict)

    def __init__(self, parent=None) -> None:
        super().__init__(parent)
        root = QVBoxLayout(self)
        root.setContentsMargins(0, 0, 0, 0)
        root.setSpacing(0)
        self._scroll = _ScrollPage()
        root.addWidget(self._scroll)

        top, top_layout = card("horizontal", 12, 0)
        top.setObjectName("")
        top.setStyleSheet("background: transparent; border: none;")
        self.back_button = QPushButton("\u2190  Back")
        self.back_button.setObjectName("Ghost")
        self.back_button.setCursor(Qt.PointingHandCursor)
        self.back_button.clicked.connect(lambda: self.back_requested.emit())
        top_layout.addWidget(self.back_button)
        column = QVBoxLayout()
        column.setContentsMargins(0, 0, 0, 0)
        column.setSpacing(2)
        self.title_label = label("Review", "ScreenTitle")
        self.title_label.setStyleSheet("font-size: 21px; font-weight: 800;")
        self.subtitle_label = label("Choose quality and action.", "Meta")
        column.addWidget(self.title_label)
        column.addWidget(self.subtitle_label)
        top_layout.addLayout(column, 1)
        self._scroll.add(top)

        self.thinking = ThinkingCard()
        self.thinking.copy_logs.connect(lambda: self.copy_logs.emit())
        self._scroll.add(self.thinking)

        # -------------------------------------------------------- media header
        self.media_card, media_layout = card("horizontal", 14)
        self.poster = AsyncImage(64, 88)
        media_layout.addWidget(self.poster)
        info = QVBoxLayout()
        info.setContentsMargins(0, 0, 0, 0)
        info.setSpacing(5)
        self.badge_label = label("MEDIA", "Badge")
        self.name_label = QLabel("Resolving link...")
        self.name_label.setStyleSheet("font-size: 18px; font-weight: 800;")
        self.name_label.setWordWrap(True)
        self.detail_label = label("", "Meta")
        info.addWidget(self.badge_label)
        info.addWidget(self.name_label)
        info.addWidget(self.detail_label)
        info.addStretch(1)
        media_layout.addLayout(info, 1)
        self._scroll.add(self.media_card)

        # ------------------------------------------------------------- season
        self.season_header = SectionHeader("Season", "")
        self.season_chips = ChipBar()
        self.season_chips.selected.connect(self._on_season)
        self.episode_header = SectionHeader("Episode", "")
        self.episode_chips = ChipBar()
        self.episode_chips.selected.connect(self._on_episode)
        self.season_header.setVisible(False)
        self.season_chips.setVisible(False)
        self.episode_header.setVisible(False)
        self.episode_chips.setVisible(False)
        self._scroll.add(self.season_header)
        self._scroll.add(self.season_chips)
        self._scroll.add(self.episode_header)
        self._scroll.add(self.episode_chips)

        # -------------------------------------------------------------- audio
        self.audio_header = SectionHeader("Audio Language", "")
        self.audio_chips = ChipBar()
        self.audio_chips.selected.connect(lambda lang: self.audio_track_selected.emit(lang))
        self.audio_header.setVisible(False)
        self.audio_chips.setVisible(False)
        self._scroll.add(self.audio_header)
        self._scroll.add(self.audio_chips)

        # ----------------------------------------------------------- playlist
        self.playlist_header = SectionHeader("Playlist Items", "")
        self.playlist_host = QVBoxLayout()
        self.playlist_host.setContentsMargins(0, 0, 0, 0)
        self.playlist_host.setSpacing(6)
        self.playlist_frame, playlist_layout = card("vertical", 6, 10, "CardRaised")
        playlist_layout.addLayout(self.playlist_host)
        self.playlist_header.setVisible(False)
        self.playlist_frame.setVisible(False)
        self._scroll.add(self.playlist_header)
        self._scroll.add(self.playlist_frame)

        # ------------------------------------------------------- stream type
        self.stream_type_header = SectionHeader("Stream Type")
        type_row = QHBoxLayout()
        type_row.setSpacing(10)
        self.video_chip = QPushButton("Video")
        self.video_chip.setObjectName("Chip")
        self.video_chip.setCheckable(True)
        self.video_chip.setChecked(True)
        self.video_chip.setCursor(Qt.PointingHandCursor)
        self.audio_chip = QPushButton("Audio Only")
        self.audio_chip.setObjectName("Chip")
        self.audio_chip.setCheckable(True)
        self.audio_chip.setCursor(Qt.PointingHandCursor)
        self._type_group = QButtonGroup(self)
        self._type_group.setExclusive(True)
        self._type_group.addButton(self.video_chip)
        self._type_group.addButton(self.audio_chip)
        self.video_chip.toggled.connect(self._on_type_toggled)
        type_row.addWidget(self.video_chip, 1)
        type_row.addWidget(self.audio_chip, 1)
        self._scroll.add(self.stream_type_header)
        self._scroll.add_layout(type_row)

        # ------------------------------------------------------------ quality
        self.quality_header = SectionHeader("Video Quality")
        self.quality_combo = QComboBox()
        self.quality_combo.setCursor(Qt.PointingHandCursor)
        self.quality_combo.currentTextChanged.connect(
            lambda value: self.quality_changed.emit(value)
        )
        self.quality_size_hint = label("", "MetaDim")
        quality_box = QVBoxLayout()
        quality_box.setContentsMargins(0, 0, 0, 0)
        quality_box.setSpacing(6)
        quality_box.addWidget(self.quality_combo)
        quality_box.addWidget(self.quality_size_hint)
        self._scroll.add(self.quality_header)
        self._scroll.add_layout(quality_box)

        # ------------------------------------------------------------ actions
        self.download_button = QPushButton("Download")
        self.download_button.setObjectName("Primary")
        self.download_button.setCursor(Qt.PointingHandCursor)
        self.download_button.setFixedHeight(50)
        self.download_button.clicked.connect(lambda: self.download_requested.emit())
        action_row = QHBoxLayout()
        action_row.setSpacing(10)
        self.stream_button = QPushButton("\u25B6   Stream")
        self.stream_button.setCursor(Qt.PointingHandCursor)
        self.stream_button.setFixedHeight(46)
        self.stream_button.clicked.connect(lambda: self.stream_requested.emit())
        self.copy_button = QPushButton("Copy direct link")
        self.copy_button.setCursor(Qt.PointingHandCursor)
        self.copy_button.setFixedHeight(46)
        self.copy_button.clicked.connect(lambda: self.copy_link_requested.emit())
        action_row.addWidget(self.stream_button, 1)
        action_row.addWidget(self.copy_button, 1)
        self._scroll.add(self.download_button)
        self._scroll.add_layout(action_row)
        self._scroll.add_stretch()
        self._playlist_cards: list[QWidget] = []

    # ------------------------------------------------------------------ slots
    def set_loading(self, loading: bool) -> None:
        self.title_label.setText("Analyzing" if loading else "Review")
        self.subtitle_label.setText("Processing media details..." if loading
                                    else "Choose quality and action.")
        if loading:
            self._qualities = []
            self.thinking.start("Resolve media", "Resolving media stream")
            self.download_button.setEnabled(False)
            self.stream_button.setEnabled(False)
            self.copy_button.setEnabled(False)
            for widget in (self.season_header, self.season_chips, self.episode_header,
                           self.episode_chips, self.audio_header, self.audio_chips,
                           self.playlist_header, self.playlist_frame):
                widget.setVisible(False)
        else:
            self.download_button.setEnabled(True)
            self.stream_button.setEnabled(True)
            self.copy_button.setEnabled(True)

    def set_target(self, target: dict, quality: str, audio_only: bool) -> None:
        self.poster.set_url(str(target.get("poster", "")))
        is_series = bool(target.get("is_series"))
        can_stream = bool(target.get("can_stream", True))
        media_type = str(target.get("media_type", "video"))
        is_playlist = bool(target.get("is_playlist"))
        is_file = (media_type == "file") or (not can_stream)

        season = int(target.get("selected_season") or 1)
        episode = int(target.get("selected_episode") or 1)
        year_val = str(target.get("year") or "").strip()
        year_badge = f"  \u00b7  {year_val}" if year_val and year_val != "0" else ""
        providers_val = str(target.get("providers") or "").strip()
        target_url_l = str(target.get("target_url") or target.get("url") or "").lower()
        src_val = str(target.get("source") or "").strip()
        is_igl = target_url_l.startswith("igl:") or "latent" in src_val.lower() or "igl" in src_val.lower()
        is_yt = "youtube" in src_val.lower() or "youtu" in target_url_l
        is_torrent = media_type == "torrent" or target_url_l.startswith("magnet:") or "torrent" in src_val.lower()

        if is_igl:
            self.badge_label.setText("IGL  \u00b7  ARC")
        elif is_torrent:
            self.badge_label.setText("TORRENT")
        elif is_file:
            self.badge_label.setText("FILE")
        elif is_series:
            base = providers_val.upper() if providers_val else f"TV SERIES  \u00b7  S{season}E{episode}"
            self.badge_label.setText(f"{base}{year_badge}" if providers_val else base)
        elif is_playlist:
            self.badge_label.setText("PLAYLIST")
        elif is_yt:
            self.badge_label.setText("YOUTUBE")
        elif providers_val:
            self.badge_label.setText(f"{providers_val.upper()}{year_badge}")
        elif src_val and src_val.lower() not in ("stream", "netmirror", "movie", "direct file"):
            self.badge_label.setText(f"{src_val.upper()}{year_badge}")
        else:
            self.badge_label.setText(f"MOVIE{year_badge}")

        title = str(target.get("title", "")).strip()
        if is_series:
            self.name_label.setText(f"{_clean_show_title(title)} S{season}E{episode}")
        else:
            self.name_label.setText(title or "Resolving link...")

        self._qualities = list(target.get("available_qualities") or [])
        self._audio_only = audio_only
        self._refresh_quality_options(quality)

        if is_file:
            self.detail_label.setText("Direct file \u00b7 saved as-is")
        else:
            self.detail_label.setText(f"{quality}  \u00b7  {'Audio' if audio_only else 'Video'}")

        # seasons / episodes
        seasons = target.get("seasons") or []
        show_seasons = is_series and bool(seasons)
        self.season_header.setVisible(show_seasons)
        self.season_chips.setVisible(show_seasons)
        self.episode_header.setVisible(show_seasons)
        self.episode_chips.setVisible(show_seasons)
        if show_seasons:
            self.season_header.set_title("Season")
            self.season_chips.set_items(
                [f"S{int(s.get('season', i + 1))}" for i, s in enumerate(seasons)], f"S{season}")
            current = next((s for s in seasons if int(s.get("season", 0)) == season), seasons[0])
            try:
                count = max(1, int(current.get("episodes", 1) or 1))
            except (TypeError, ValueError):
                count = 1
            self.episode_header.set_title("Episode")
            self.episode_chips.set_items([f"E{n}" for n in range(1, count + 1)], f"E{episode}")

        # audio language choices carried over from search hit or resolve
        languages = target.get("audio_languages") or []
        show_audio = bool(languages) and (len(languages) > 1 or bool(target.get("selected_audio_lang")))
        self.audio_header.setVisible(show_audio)
        self.audio_chips.setVisible(show_audio)
        if show_audio:
            current_lang = str(target.get("selected_audio_lang") or languages[0])
            self.audio_header.set_title("Audio Language")
            self.audio_chips.set_items([str(l) for l in languages], current_lang)

        # playlist
        entries = target.get("entries") or []
        show_playlist = is_playlist and bool(entries)
        self.playlist_header.setVisible(show_playlist)
        self.playlist_frame.setVisible(show_playlist)
        if show_playlist:
            self.playlist_header.set_title(f"Playlist Items \u00b7 {len(entries)}")
            for old in self._playlist_cards:
                old.deleteLater()
            self._playlist_cards.clear()
            for index, entry in enumerate(entries[:200], 1):
                row = QFrame()
                row.setObjectName("Card")
                row_layout = QHBoxLayout(row)
                row_layout.setContentsMargins(10, 8, 10, 8)
                row_layout.setSpacing(10)
                thumb = AsyncImage(44, 32)
                thumb.set_url(str(entry.get("thumbnail", "")))
                row_layout.addWidget(thumb)
                name = QLabel(f"{index}. {entry.get('title', 'Video')}")
                name.setStyleSheet("font-size: 12px;")
                name.setWordWrap(False)
                row_layout.addWidget(name, 1)
                play = QPushButton("\u25B6")
                play.setObjectName("Ghost")
                play.setCursor(Qt.PointingHandCursor)
                play.clicked.connect(lambda _=False, e=entry: self.playlist_item_selected.emit(e))
                row_layout.addWidget(play)
                self.playlist_host.addWidget(row)
                self._playlist_cards.append(row)

        self.download_button.setText(
            f"Download Playlist \u00b7 {len(entries)} items" if is_playlist
            else ("Download Torrent" if is_torrent
                  else ("Download File" if is_file else f"Download {quality}"))
        )
        self.download_button.setEnabled(True)
        can_play_stream = can_stream and media_type != "file" and not is_torrent
        show_stream_type = can_play_stream and is_yt
        self.stream_button.setVisible(can_play_stream)
        self.copy_button.setVisible(can_play_stream)
        self.stream_type_header.setVisible(show_stream_type)
        self.video_chip.setVisible(show_stream_type)
        self.audio_chip.setVisible(show_stream_type)
        self.quality_header.setVisible(can_play_stream)
        self.quality_combo.setVisible(can_play_stream)
        self.video_chip.setChecked(not audio_only)
        self.audio_chip.setChecked(audio_only)

    def set_audio_only(self, audio_only: bool) -> None:
        self._audio_only = audio_only
        self.video_chip.blockSignals(True)
        self.audio_chip.blockSignals(True)
        self.video_chip.setChecked(not audio_only)
        self.audio_chip.setChecked(audio_only)
        self.video_chip.blockSignals(False)
        self.audio_chip.blockSignals(False)
        self._refresh_quality_options(self.quality_combo.currentText())

    def set_busy(self, busy: bool) -> None:
        self.download_button.setEnabled(not busy)
        self.stream_button.setEnabled(not busy)
        self.copy_button.setEnabled(not busy)

    def set_size_hint(self, text: str) -> None:
        self.quality_size_hint.setText(text)
        self.quality_size_hint.setVisible(bool(text))

    # --------------------------------------------------------------- internal
    def _refresh_quality_options(self, current: str) -> None:
        self.quality_combo.blockSignals(True)
        self.quality_combo.clear()
        audio_only = getattr(self, "_audio_only", False)
        if audio_only:
            options = list(AUDIO_QUALITIES)
            self.quality_header.set_title("Audio Quality")
        else:
            options = list(getattr(self, "_qualities", []) or []) or ["Best Available"]
            self.quality_header.set_title("Video Quality")
        self.quality_combo.addItems(options)
        if current in options:
            self.quality_combo.setCurrentText(current)
        self.quality_combo.blockSignals(False)

    def _on_type_toggled(self, _checked: bool) -> None:
        audio_only = self.audio_chip.isChecked()
        if audio_only == getattr(self, "_audio_only", False):
            return
        self.audio_only_changed.emit(audio_only)

    def _on_season(self, value: str) -> None:
        digits = "".join(ch for ch in value if ch.isdigit())
        if digits:
            self.season_selected.emit(int(digits))

    def _on_episode(self, value: str) -> None:
        digits = "".join(ch for ch in value if ch.isdigit())
        if digits:
            self.episode_selected.emit(int(digits))

def _clean_show_title(raw: str) -> str:
    import re
    text = re.sub(r"[\s\-_]+S\d+(-S\d+)?(\s*E\d+)?.*", "", raw or "", flags=re.IGNORECASE)
    text = re.sub(r"[\s\-_]+Season\s*\d+(-S\d+)?.*", "", text, flags=re.IGNORECASE)
    text = re.sub(r"[\s\-_]+Complete\s*Series.*", "", text, flags=re.IGNORECASE)
    return text.strip() or (raw or "")


# =================================================================  ACTIVITY
class ActivityPage(QWidget):
    pause_requested = Signal(str)
    resume_requested = Signal(str)
    cancel_requested = Signal(str)
    dismiss_requested = Signal(str)
    open_requested = Signal(str)
    copy_logs_requested = Signal(str)

    def __init__(self, parent=None) -> None:
        super().__init__(parent)
        root = QVBoxLayout(self)
        root.setContentsMargins(0, 0, 0, 0)
        self._scroll = _ScrollPage()
        root.addWidget(self._scroll)
        head = QVBoxLayout()
        head.setContentsMargins(2, 0, 2, 0)
        head.setSpacing(4)
        head.addWidget(label("Activity", "ScreenTitle"))
        head.addWidget(label("Active and recent downloads", "ScreenSubtitle"))
        self._scroll.add_layout(head)
        self.jobs_host = QVBoxLayout()
        self.jobs_host.setContentsMargins(0, 0, 0, 0)
        self.jobs_host.setSpacing(13)
        self._scroll.add_layout(self.jobs_host)
        self._scroll.add_stretch()
        self._cards: dict[str, JobCard] = {}
        self._empty: Optional[EmptyState] = None
        self._show_empty()

    def _show_empty(self) -> None:
        if self._empty is None:
            self._empty = EmptyState("No Active Downloads", "Queued downloads appear here.")
            self.jobs_host.addWidget(self._empty)
        self._empty.setVisible(True)

    def set_jobs(self, jobs: list[dict]) -> None:
        visible = [j for j in jobs if str(j.get("status")) in
                   ("Downloading", "Muxing", "Queued", "Paused", "Failed", "Cancelled")]
        live_ids = {str(j.get("id")) for j in visible}
        for job_id in list(self._cards):
            if job_id not in live_ids:
                self._cards.pop(job_id).deleteLater()
        if not visible:
            self._show_empty()
            return
        if self._empty is not None:
            self._empty.setVisible(False)
        for job in visible:
            job_id = str(job.get("id"))
            existing = self._cards.get(job_id)
            if existing is None:
                card_widget = JobCard(job)
                card_widget.pause_clicked.connect(lambda j: self.pause_requested.emit(j))
                card_widget.resume_clicked.connect(lambda j: self.resume_requested.emit(j))
                card_widget.cancel_clicked.connect(lambda j: self.cancel_requested.emit(j))
                card_widget.dismiss_clicked.connect(lambda j: self.dismiss_requested.emit(j))
                card_widget.open_clicked.connect(lambda j: self.open_requested.emit(j))
                card_widget.logs_clicked.connect(lambda j: self.copy_logs_requested.emit(j))
                self.jobs_host.insertWidget(0, card_widget)
                self._cards[job_id] = card_widget
            else:
                existing.update(job)

    def card_for(self, job_id: str) -> Optional[JobCard]:
        return self._cards.get(job_id)


# ==================================================================  LIBRARY
class LibraryPage(QWidget):
    open_requested = Signal(str)
    folder_requested = Signal(str)
    delete_requested = Signal(str)
    refresh_requested = Signal()

    def __init__(self, parent=None) -> None:
        super().__init__(parent)
        root = QVBoxLayout(self)
        root.setContentsMargins(0, 0, 0, 0)
        self._scroll = _ScrollPage()
        root.addWidget(self._scroll)

        head = QHBoxLayout()
        head.setContentsMargins(2, 0, 2, 0)
        column = QVBoxLayout()
        column.setContentsMargins(0, 0, 0, 0)
        column.setSpacing(4)
        column.addWidget(label("Library", "ScreenTitle"))
        self.subtitle = label("Downloaded media on this PC", "ScreenSubtitle")
        column.addWidget(self.subtitle)
        head.addLayout(column, 1)
        self.refresh_button = QPushButton("Refresh")
        self.refresh_button.setObjectName("Ghost")
        self.refresh_button.setCursor(Qt.PointingHandCursor)
        self.refresh_button.clicked.connect(lambda: self.refresh_requested.emit())
        head.addWidget(self.refresh_button, 0, Qt.AlignTop)
        self._scroll.add_layout(head)

        self.filter_field = QLineEdit()
        self.filter_field.setPlaceholderText("Filter by name...")
        self.filter_field.setClearButtonEnabled(True)
        self.filter_field.textChanged.connect(lambda _text: self._refilter())
        self._scroll.add(self.filter_field)

        self.items_host = QVBoxLayout()
        self.items_host.setContentsMargins(0, 0, 0, 0)
        self.items_host.setSpacing(13)
        self._scroll.add_layout(self.items_host)
        self._scroll.add_stretch()
        self._entries: list[dict] = []
        self._cards: list[LibraryCard] = []

    def set_entries(self, entries: list[dict], folder: str) -> None:
        self._entries = list(entries)
        self.subtitle.setText(f"{len(entries)} file(s)  \u00b7  {folder}")
        self._refilter()

    def _refilter(self) -> None:
        needle = self.filter_field.text().strip().lower()
        for old in self._cards:
            old.deleteLater()
        self._cards.clear()
        shown = [e for e in self._entries if not needle or needle in str(e.get("title", "")).lower()]
        if not shown:
            empty = EmptyState("No Files Yet",
                               "Completed downloads appear here, and files already in your "
                               "download folder are picked up automatically.")
            self.items_host.addWidget(empty)
            self._cards.append(empty)  # type: ignore[arg-type]
            return
        for entry in shown[:200]:
            card_widget = LibraryCard(entry)
            card_widget.refresh(entry)
            card_widget.open_clicked.connect(lambda p: self.open_requested.emit(p))
            card_widget.folder_clicked.connect(lambda p: self.folder_requested.emit(p))
            card_widget.delete_clicked.connect(lambda p: self.delete_requested.emit(p))
            self.items_host.addWidget(card_widget)
            self._cards.append(card_widget)


# =================================================================  SETTINGS
class SettingsPage(QWidget):
    browse_requested = Signal()
    video_quality_changed = Signal(str)
    audio_quality_changed = Signal(str)
    notifications_changed = Signal(bool)
    prefer_vlc_changed = Signal(bool)
    player_dialog_requested = Signal()
    telegram_auth_requested = Signal()
    telegram_logout_requested = Signal()
    telegram_test_requested = Signal(str, str, str)
    telegram_saved = Signal(str, str, str)
    google_login_requested = Signal()
    google_disconnect_requested = Signal()
    about_requested = Signal()
    scan_requested = Signal()
    install_requested = Signal(str)

    def __init__(self, parent=None) -> None:
        super().__init__(parent)
        root = QVBoxLayout(self)
        root.setContentsMargins(0, 0, 0, 0)
        self._scroll = _ScrollPage()
        root.addWidget(self._scroll)

        head = QVBoxLayout()
        head.setContentsMargins(2, 0, 2, 0)
        head.setSpacing(4)
        head.addWidget(label("Settings", "ScreenTitle"))
        head.addWidget(label("Preferences", "ScreenSubtitle"))
        self._scroll.add_layout(head)

        # ---------------------------------------------------------- downloads
        self._scroll.add(self._group("Downloads"))
        path_card, path_layout = card("vertical", 8, 14, "Card")
        path_layout.addWidget(label("Save Location", "SectionHeader"))
        self.path_label = label("", "Meta")
        self.path_label.setWordWrap(True)
        self.path_label.setTextInteractionFlags(Qt.TextSelectableByMouse)
        path_layout.addWidget(self.path_label)
        path_buttons = QHBoxLayout()
        path_buttons.setSpacing(8)
        self.browse_button = QPushButton("Change folder")
        self.browse_button.setObjectName("Ghost")
        self.browse_button.setCursor(Qt.PointingHandCursor)
        self.browse_button.clicked.connect(lambda: self.browse_requested.emit())
        self.scan_button = QPushButton("Scan folder now")
        self.scan_button.setObjectName("Ghost")
        self.scan_button.setCursor(Qt.PointingHandCursor)
        self.scan_button.clicked.connect(lambda: self.scan_requested.emit())
        path_buttons.addWidget(self.browse_button)
        path_buttons.addWidget(self.scan_button)
        path_buttons.addStretch(1)
        path_layout.addLayout(path_buttons)
        self._scroll.add(path_card)

        quality_card, quality_layout = card("vertical", 8, 14, "Card")
        quality_layout.addWidget(label("Default Quality", "SectionHeader"))
        grid = QGridLayout()
        grid.setHorizontalSpacing(10)
        grid.setVerticalSpacing(8)
        grid.addWidget(label("Video", "Meta"), 0, 0)
        self.video_quality_combo = QComboBox()
        self.video_quality_combo.addItems(["Ask each time"] + VIDEO_QUALITY_DEFAULTS)
        self.video_quality_combo.currentTextChanged.connect(
            lambda v: self.video_quality_changed.emit(v))
        grid.addWidget(self.video_quality_combo, 0, 1)
        grid.addWidget(label("Audio", "Meta"), 1, 0)
        self.audio_quality_combo = QComboBox()
        self.audio_quality_combo.addItems(["Ask each time"] + AUDIO_QUALITIES)
        self.audio_quality_combo.currentTextChanged.connect(
            lambda v: self.audio_quality_changed.emit(v))
        grid.addWidget(self.audio_quality_combo, 1, 1)
        grid.setColumnStretch(1, 1)
        quality_layout.addLayout(grid)
        self.quality_note = label("", "MetaDim")
        self.quality_note.setWordWrap(True)
        quality_layout.addWidget(self.quality_note)
        self._scroll.add(quality_card)

        # ------------------------------------------------------- accelerators
        self._scroll.add(self._group("Accelerators"))
        self.accel_card, self.accel_layout = card("vertical", 7, 14, "Card")
        self._scroll.add(self.accel_card)

        # -------------------------------------------------------- preferences
        self._scroll.add(self._group("Preferences"))
        player_card, player_layout = card("horizontal", 10, 14, "Card")
        player_col = QVBoxLayout()
        player_col.setSpacing(2)
        player_col.addWidget(label("Streaming Player", "SectionHeader"))
        self.streaming_player_label = label("ARC Player (Built-in)", "Meta")
        player_col.addWidget(self.streaming_player_label)
        player_layout.addLayout(player_col, 1)
        self.player_change_btn = QPushButton("Change")
        self.player_change_btn.setObjectName("Ghost")
        self.player_change_btn.setCursor(Qt.PointingHandCursor)
        self.player_change_btn.clicked.connect(lambda: self.player_dialog_requested.emit())
        player_layout.addWidget(self.player_change_btn)
        self._scroll.add(player_card)

        pref_card, pref_layout = card("vertical", 4, 14, "Card")
        self.notifications_check = QCheckBox("Notify me when a download finishes")
        self.notifications_check.toggled.connect(lambda v: self.notifications_changed.emit(v))
        self.vlc_check = QCheckBox("Prefer VLC when external streaming is selected")
        self.vlc_check.toggled.connect(lambda v: self.prefer_vlc_changed.emit(v))
        pref_layout.addWidget(self.notifications_check)
        pref_layout.addWidget(self.vlc_check)
        self._scroll.add(pref_card)

        # ------------------------------------------------------- integrations
        self._scroll.add(self._group("Integrations"))
        google_card, google_layout = card("horizontal", 10, 14, "Card")
        g_col = QVBoxLayout()
        g_col.setSpacing(2)
        self.google_title_lbl = label("Google Account (YouTube)", "SectionHeader")
        self.google_sub_lbl = label("Optional \u00b7 YouTube authentication", "Meta")
        g_col.addWidget(self.google_title_lbl)
        g_col.addWidget(self.google_sub_lbl)
        google_layout.addLayout(g_col, 1)
        self.google_manage_btn = QPushButton("Configure")
        self.google_manage_btn.setObjectName("Ghost")
        self.google_manage_btn.setCursor(Qt.PointingHandCursor)
        self.google_manage_btn.clicked.connect(lambda: self.google_login_requested.emit())
        self.google_disc_btn = QPushButton("Disconnect")
        self.google_disc_btn.setObjectName("Ghost")
        self.google_disc_btn.setStyleSheet(f"color: {theme.ARC_ERROR};")
        self.google_disc_btn.setCursor(Qt.PointingHandCursor)
        self.google_disc_btn.setVisible(False)
        self.google_disc_btn.clicked.connect(lambda: self.google_disconnect_requested.emit())
        google_layout.addWidget(self.google_manage_btn)
        google_layout.addWidget(self.google_disc_btn)
        self._scroll.add(google_card)

        tg_card, tg_layout = card("vertical", 9, 14, "Card")
        tg_head = QHBoxLayout()
        tg_head_col = QVBoxLayout()
        tg_head_col.setSpacing(2)
        self.tg_head_lbl = label("Telegram Session (MTProto)", "SectionHeader")
        self.tg_sub_lbl = label("Private channel access \u00b7 QR, Phone OTP or Session", "Meta")
        tg_head_col.addWidget(self.tg_head_lbl)
        tg_head_col.addWidget(self.tg_sub_lbl)
        tg_head.addLayout(tg_head_col, 1)
        self.tg_auth_btn = QPushButton("Login (QR / OTP)")
        self.tg_auth_btn.setObjectName("Chip")
        self.tg_auth_btn.setCursor(Qt.PointingHandCursor)
        self.tg_auth_btn.clicked.connect(lambda: self.telegram_auth_requested.emit())
        self.tg_logout_btn = QPushButton("Sign Out")
        self.tg_logout_btn.setObjectName("Ghost")
        self.tg_logout_btn.setStyleSheet(f"color: {theme.ARC_ERROR};")
        self.tg_logout_btn.setVisible(False)
        self.tg_logout_btn.clicked.connect(lambda: self.telegram_logout_requested.emit())
        tg_head.addWidget(self.tg_auth_btn)
        tg_head.addWidget(self.tg_logout_btn)
        tg_layout.addLayout(tg_head)

        self.tg_id = QLineEdit()
        self.tg_id.setPlaceholderText("API ID (numeric)")
        self.tg_hash = QLineEdit()
        self.tg_hash.setPlaceholderText("API hash")
        self.tg_session = QLineEdit()
        self.tg_session.setPlaceholderText("Pyrogram string session")
        self.tg_session.setEchoMode(QLineEdit.Password)
        self.reveal_check = QCheckBox("Show session")
        self.reveal_check.toggled.connect(
            lambda on: self.tg_session.setEchoMode(QLineEdit.Normal if on else QLineEdit.Password))
        for field in (self.tg_id, self.tg_hash, self.tg_session):
            tg_layout.addWidget(field)
        tg_layout.addWidget(self.reveal_check)
        self.tg_status = label("", "Meta")
        self.tg_status.setWordWrap(True)
        tg_layout.addWidget(self.tg_status)
        tg_buttons = QHBoxLayout()
        tg_buttons.setSpacing(8)
        self.tg_test_button = QPushButton("Test connection")
        self.tg_test_button.setCursor(Qt.PointingHandCursor)
        self.tg_test_button.clicked.connect(self._test_telegram)
        self.tg_save_button = QPushButton("Save")
        self.tg_save_button.setObjectName("Primary")
        self.tg_save_button.setCursor(Qt.PointingHandCursor)
        self.tg_save_button.clicked.connect(self._save_telegram)
        tg_buttons.addWidget(self.tg_test_button)
        tg_buttons.addWidget(self.tg_save_button)
        tg_buttons.addStretch(1)
        tg_layout.addLayout(tg_buttons)
        self._scroll.add(tg_card)

        # -------------------------------------------------------------- about
        self._scroll.add(self._group("About"))
        about_card, about_layout = card("horizontal", 10, 14, "Card")
        about_col = QVBoxLayout()
        about_col.setSpacing(2)
        about_col.addWidget(label("About ARC Features", "SectionHeader"))
        about_col.addWidget(label("What ARC Desktop can do", "Meta"))
        about_layout.addLayout(about_col, 1)
        self.about_btn = QPushButton("View")
        self.about_btn.setObjectName("Ghost")
        self.about_btn.setCursor(Qt.PointingHandCursor)
        self.about_btn.clicked.connect(lambda: self.about_requested.emit())
        about_layout.addWidget(self.about_btn)
        self._scroll.add(about_card)

        self._scroll.add_stretch()
        self._accel_rows: list[QWidget] = []
        self._install_buttons: dict[str, QPushButton] = {}
        self._install_status: dict[str, QLabel] = {}

    # ------------------------------------------------------------------ slots
    def _group(self, title: str) -> QWidget:
        return _group_label(title)

    def set_download_path(self, path: str) -> None:
        self.path_label.setText(path or "(not set)")

    def set_defaults(self, video: str, audio: str, notifications: bool, prefer_vlc: bool) -> None:
        for combo, value in ((self.video_quality_combo, video), (self.audio_quality_combo, audio)):
            combo.blockSignals(True)
            index = combo.findText(value)
            combo.setCurrentIndex(index if index >= 0 else 0)
            combo.blockSignals(False)
        self.notifications_check.blockSignals(True)
        self.notifications_check.setChecked(bool(notifications))
        self.notifications_check.blockSignals(False)
        self.vlc_check.blockSignals(True)
        self.vlc_check.setChecked(bool(prefer_vlc))
        self.vlc_check.blockSignals(False)

    def set_environment(self, environment: dict) -> None:
        for row in self._accel_rows:
            row.deleteLater()
        self._accel_rows.clear()
        self._install_buttons.clear()
        self._install_status.clear()

        parallel = int(environment.get("parallel_connections", 1))
        headline = QLabel(f"{parallel}\u00d7 connections")
        headline.setStyleSheet(f"font-size: 14px; font-weight: 800; color: {theme.ARC_WHITE};")
        self.accel_layout.insertWidget(0, headline)
        self._accel_rows.append(headline)

        accelerators = environment.get("accelerators", {}) or {}
        rows = [
            ("aria2c", accelerators.get("aria2c"), "16-connection downloads"),
            ("ffmpeg", accelerators.get("ffmpeg"), "HD audio/video merge"),
            ("VLC", environment.get("vlc"), "External stream player"),
        ]
        for name, path, purpose in rows:
            row = QWidget()
            layout = QHBoxLayout(row)
            layout.setContentsMargins(0, 2, 0, 2)
            layout.setSpacing(10)
            name_label = QLabel(name)
            name_label.setStyleSheet("font-size: 12px; font-weight: 700;")
            purpose_label = QLabel(purpose)
            purpose_label.setObjectName("MetaDim")
            layout.addWidget(name_label)
            layout.addWidget(purpose_label, 1)
            status = QLabel("Installed" if path else "")
            status.setObjectName("MetaDim")
            layout.addWidget(status)
            self._install_status[name.lower()] = status
            if not path:
                install = QPushButton("Install")
                install.setObjectName("Ghost")
                install.setCursor(Qt.PointingHandCursor)
                install.clicked.connect(lambda _=False, key=name.lower(): self.install_requested.emit(key))
                layout.addWidget(install)
                self._install_buttons[name.lower()] = install
            self.accel_layout.addWidget(row)
            self._accel_rows.append(row)

        self.quality_note.setText("Used automatically when available.")

    def set_installer_state(self, key: str, text: str, busy: bool = False, error: bool = False) -> None:
        button = self._install_buttons.get(key.lower())
        status = self._install_status.get(key.lower())
        if button is not None:
            button.setText("Installing…" if busy else "Install")
            button.setEnabled(not busy)
        if status is not None:
            status.setText(text)
            status.setStyleSheet(
                f"color: {theme.ARC_ERROR if error else theme.ARC_WARNING}; font-size: 10px;"
            )

    def set_streaming_player_name(self, name: str) -> None:
        self.streaming_player_label.setText(name or "ARC Player (Built-in)")

    def set_google_account(self, email: str, has_cookies: bool) -> None:
        if has_cookies:
            self.google_title_lbl.setText(email or "Google Account")
            self.google_sub_lbl.setText("Connected \u00b7 YouTube playback authenticated")
            self.google_disc_btn.setVisible(True)
        else:
            self.google_title_lbl.setText("Google Account (YouTube)")
            self.google_sub_lbl.setText("Optional \u00b7 YouTube authentication")
            self.google_disc_btn.setVisible(False)

    def set_telegram(self, api_id: str, api_hash: str, session: str, user: str = "", phone: str = "") -> None:
        self.tg_id.setText(api_id or "")
        self.tg_hash.setText(api_hash or "")
        self.tg_session.setText(session or "")
        connected = bool((session or "").strip())
        self.tg_logout_btn.setVisible(connected)
        if connected:
            who = user or phone or "Telegram User"
            self.tg_status.setText(f"Connected as {who} (Saved on this PC).")
            self.tg_status.setStyleSheet(f"color: {theme.ARC_SUCCESS}; font-size: 11px; font-weight: 600;")
        else:
            self.tg_status.setText("Not configured.")
            self.tg_status.setStyleSheet(f"color: {theme.ARC_MUTED}; font-size: 11px;")

    def set_telegram_busy(self, busy: bool) -> None:
        self.tg_test_button.setEnabled(not busy)
        self.tg_save_button.setEnabled(not busy)
        if busy:
            self.tg_status.setText("Testing connection...")
            self.tg_status.setStyleSheet(f"color: {theme.ARC_WARNING}; font-size: 11px;")

    def set_telegram_result(self, ok: bool, message: str) -> None:
        self.tg_status.setText(message)
        self.tg_status.setStyleSheet(
            f"color: {theme.ARC_SUCCESS if ok else theme.ARC_ERROR}; font-size: 11px; font-weight: 600;"
        )

    def telegram_values(self) -> tuple[str, str, str]:
        clean_sess = re.sub(r'\s+', '', self.tg_session.text())
        return self.tg_id.text().strip(), self.tg_hash.text().strip(), clean_sess

    def _test_telegram(self) -> None:
        api_id, api_hash, session = self.telegram_values()
        self.telegram_test_requested.emit(api_id, api_hash, session)

    def _save_telegram(self) -> None:
        api_id, api_hash, session = self.telegram_values()
        self.telegram_saved.emit(api_id, api_hash, session)


def _group_label(title: str) -> QWidget:
    widget = QLabel(title.upper())
    widget.setStyleSheet(
        f"color: {theme.ARC_MUTED}; font-size: 10px; font-weight: 800; letter-spacing: 0.8px;"
    )
    widget.setContentsMargins(2, 12, 0, 0)
    return widget
