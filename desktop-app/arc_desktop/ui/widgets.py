"""Reusable widgets: the desktop equivalents of the APK's Compose components.

Compose                     ->  Qt
Card(containerColor=Surface) ->  Card
FilterChip                   ->  Chip / ChipBar
ClaudeThinkingCard           ->  ThinkingCard
JobRow / Activity card       ->  JobCard
EmptyState                   ->  EmptyState
SectionHeader                ->  SectionHeader
AsyncImage (Coil)            ->  AsyncImage
"""

from __future__ import annotations

from typing import Iterable, Optional

from PySide6.QtCore import Qt, QUrl, Signal, QRectF
from PySide6.QtGui import QPixmap
from PySide6.QtNetwork import QNetworkAccessManager, QNetworkReply, QNetworkRequest
from PySide6.QtWidgets import (
    QFrame, QHBoxLayout, QLabel, QProgressBar, QPushButton, QScrollArea,
    QVBoxLayout, QWidget,
)

from .. import theme


# --------------------------------------------------------------------- helpers
def card(orientation: str = "vertical", spacing: int = 14, margins: int = 16,
         object_name: str = "Card") -> tuple[QFrame, "QBoxLayout"]:
    frame = QFrame()
    frame.setObjectName(object_name)
    layout = QVBoxLayout(frame) if orientation == "vertical" else QHBoxLayout(frame)
    layout.setContentsMargins(margins, margins, margins, margins)
    layout.setSpacing(spacing)
    return frame, layout


def label(text: str, object_name: str = "", word_wrap: bool = False) -> QLabel:
    widget = QLabel(text)
    if object_name:
        widget.setObjectName(object_name)
    widget.setWordWrap(word_wrap)
    widget.setTextInteractionFlags(Qt.TextSelectableByMouse)
    return widget


def divider() -> QFrame:
    line = QFrame()
    line.setObjectName("Divider")
    line.setFixedHeight(1)
    return line


class SectionHeader(QWidget):
    def __init__(self, title: str, action_text: str = "", parent=None) -> None:
        super().__init__(parent)
        layout = QHBoxLayout(self)
        layout.setContentsMargins(2, 0, 2, 0)
        self.title_label = label(title, "SectionHeader")
        layout.addWidget(self.title_label)
        layout.addStretch(1)
        self.action_button: Optional[QPushButton] = None
        if action_text:
            self.action_button = QPushButton(action_text)
            self.action_button.setObjectName("Ghost")
            self.action_button.setCursor(Qt.PointingHandCursor)
            layout.addWidget(self.action_button)

    def set_title(self, title: str) -> None:
        self.title_label.setText(title)

    @property
    def clicked(self):
        return self.action_button.clicked if self.action_button else None


class StatusPill(QWidget):
    """Small rounded status token, like the APK's badge boxes."""

    def __init__(self, text: str = "Ready", parent=None) -> None:
        super().__init__(parent)
        layout = QHBoxLayout(self)
        layout.setContentsMargins(10, 6, 12, 6)
        layout.setSpacing(7)
        self.dot = QLabel()
        self.dot.setFixedSize(6, 6)
        self.text_label = QLabel(text)
        self.text_label.setObjectName("Meta")
        layout.addWidget(self.dot)
        layout.addWidget(self.text_label)
        self.set_style(theme.ARC_MUTED)

    def set_style(self, color: str) -> None:
        self.dot.setStyleSheet(f"background-color: {color}; border-radius: 3px;")
        self.setStyleSheet(
            f"StatusPill {{ background-color: {theme.ARC_SURFACE};"
            f" border: 1px solid {theme.ARC_BORDER_STRONG}; border-radius: 11px; }}"
        )

    def set_text(self, text: str, color: Optional[str] = None) -> None:
        self.text_label.setText(text)
        if color:
            self.set_style(color)


class ChipBar(QWidget):
    """Horizontally scrollable row of selectable chips (seasons, episodes, audio)."""

    selected = Signal(str)

    def __init__(self, parent=None, wrap: bool = False) -> None:
        super().__init__(parent)
        self._buttons: list[QPushButton] = []
        self._wrap = wrap
        outer = QHBoxLayout(self)
        outer.setContentsMargins(0, 0, 0, 0)
        outer.setSpacing(0)

        if wrap:
            self._container = QWidget()
            self._layout = _ChipRowLayout(self._container, 8)
            outer.addWidget(self._container)
        else:
            self._scroll = QScrollArea()
            self._scroll.setWidgetResizable(True)
            self._scroll.setHorizontalScrollBarPolicy(Qt.ScrollBarAsNeeded)
            self._scroll.setVerticalScrollBarPolicy(Qt.ScrollBarAlwaysOff)
            self._scroll.setFixedHeight(44)
            self._container = QWidget()
            self._layout = QHBoxLayout(self._container)
            self._layout.setContentsMargins(0, 2, 0, 8)
            self._layout.setSpacing(8)
            self._scroll.setWidget(self._container)
            outer.addWidget(self._scroll)

    def set_items(self, items: Iterable[str], current: str = "", max_items: int = 400) -> None:
        while self._layout.count():
            item = self._layout.takeAt(0)
            widget = item.widget()
            if widget is not None:
                widget.deleteLater()
        self._buttons.clear()
        for index, text in enumerate(items):
            if index >= max_items:
                break
            button = QPushButton(str(text))
            button.setObjectName("Chip")
            button.setCheckable(True)
            button.setCursor(Qt.PointingHandCursor)
            button.setChecked(str(text) == str(current))
            button.clicked.connect(lambda _=False, value=str(text): self.selected.emit(value))
            self._layout.addWidget(button)
            self._buttons.append(button)
        if not self._wrap:
            self._layout.addStretch(1)

    def set_current(self, current: str) -> None:
        for button in self._buttons:
            button.setChecked(button.text() == str(current))

    def count(self) -> int:
        return len(self._buttons)


class _ChipRowLayout(QHBoxLayout):
    """Chips laid out left to right for long season/episode lists."""

    def __init__(self, parent=None, spacing: int = 8) -> None:
        super().__init__(parent)
        self.setContentsMargins(0, 0, 0, 0)
        self.setSpacing(spacing)
        self.addStretch(1)


class AsyncImage(QWidget):
    """Loads a remote poster without blocking the UI, Coil-style.

    Falls back to a monochrome glyph tile, exactly like the APK's MediaGlyph when
    a result has no poster.
    """

    ready = Signal()

    _manager: Optional[QNetworkAccessManager] = None
    _cache: dict[str, QPixmap] = {}

    def __init__(self, width: int = 64, height: int = 88, parent=None) -> None:
        super().__init__(parent)
        self._width = width
        self._height = height
        self._url = ""
        self._pixmap: Optional[QPixmap] = None
        self.setFixedSize(width, height)
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        self._label = QLabel()
        self._label.setFixedSize(width, height)
        self._label.setAlignment(Qt.AlignCenter)
        self._label.setStyleSheet(
            f"background-color: #1E1E1E; border-radius: 10px; color: {theme.ARC_SUBTLE}; font-size: 18px;"
        )
        self._label.setText("\u25B6")
        layout.addWidget(self._label)

    @classmethod
    def _network(cls) -> QNetworkAccessManager:
        if cls._manager is None:
            cls._manager = QNetworkAccessManager()
        return cls._manager

    def set_url(self, url: str) -> None:
        url = (url or "").strip()
        if url == self._url:
            return
        self._url = url
        if not url:
            self._show_placeholder()
            self.ready.emit()
            return
        cached = self._cache.get(url)
        if cached is not None and not cached.isNull():
            self._apply(cached)
            self.ready.emit()
            return
        if not url.startswith(("http://", "https://")):
            self._show_placeholder()
            self.ready.emit()
            return
        try:
            request = QNetworkRequest(QUrl(url))
            request.setAttribute(QNetworkRequest.CacheLoadControlAttribute,
                                 QNetworkRequest.PreferCache)
            reply = self._network().get(request)
            reply.finished.connect(lambda r=reply: self._finished(r))
        except Exception:
            self._show_placeholder()
            self.ready.emit()

    def _finished(self, reply: QNetworkReply) -> None:
        try:
            if reply.error() != QNetworkReply.NoError:
                self._show_placeholder()
                return
            data = reply.readAll()
            pixmap = QPixmap()
            if not pixmap.loadFromData(bytes(data)):
                self._show_placeholder()
                return
            scaled = pixmap.scaled(self._width * 2, self._height * 2,
                                   Qt.KeepAspectRatioByExpanding, Qt.SmoothTransformation)
            self._cache[self._url] = scaled
            if len(self._cache) > 120:
                for key in list(self._cache)[:40]:
                    self._cache.pop(key, None)
            self._apply(scaled)
        except Exception:
            self._show_placeholder()
        finally:
            self.ready.emit()
            reply.deleteLater()

    def _apply(self, pixmap: QPixmap) -> None:
        scaled = pixmap.scaled(self._width * 2, self._height * 2,
                               Qt.KeepAspectRatioByExpanding, Qt.SmoothTransformation)
        self._pixmap = scaled
        self.update()
        self._label.setPixmap(self._cropped())
        self._label.setStyleSheet("border-radius: 10px;")

    def _cropped(self) -> QPixmap:
        if self._pixmap is None:
            return QPixmap()
        source = self._pixmap
        x = max(0, (source.width() - self._width * 2) // 2)
        y = max(0, (source.height() - self._height * 2) // 2)
        region = QRectF(x, y, min(source.width(), self._width * 2), min(source.height(), self._height * 2))
        cropped = source.copy(region.toRect())
        return cropped.scaled(self._width, self._height, Qt.IgnoreAspectRatio, Qt.SmoothTransformation)

    def _show_placeholder(self) -> None:
        self._pixmap = None
        self._label.setPixmap(QPixmap())
        self._label.setText("\u25B6")
        self._label.setStyleSheet(
            f"background-color: #1E1E1E; border-radius: 10px; color: {theme.ARC_SUBTLE}; font-size: 18px;"
        )


class EmptyState(QFrame):
    def __init__(self, title: str, detail: str, glyph: str = "\u2197", parent=None) -> None:
        super().__init__(parent)
        self.setObjectName("Card")
        layout = QVBoxLayout(self)
        layout.setContentsMargins(24, 26, 24, 26)
        layout.setSpacing(6)
        layout.setAlignment(Qt.AlignCenter)

        tile = QLabel(glyph)
        tile.setFixedSize(52, 52)
        tile.setAlignment(Qt.AlignCenter)
        tile.setStyleSheet(
            f"background-color: {theme.ARC_RAISED}; border-radius: 14px;"
            f" color: {theme.ARC_WHITE}; font-size: 22px;"
        )
        wrapper = QHBoxLayout()
        wrapper.addStretch(1)
        wrapper.addWidget(tile)
        wrapper.addStretch(1)
        layout.addLayout(wrapper)
        layout.addSpacing(6)

        title_label = label(title, "SectionHeader")
        title_label.setAlignment(Qt.AlignCenter)
        layout.addWidget(title_label)
        detail_label = label(detail, "Meta", word_wrap=True)
        detail_label.setAlignment(Qt.AlignCenter)
        layout.addWidget(detail_label)


class ThinkingCard(QFrame):
    """The ClaudeThinkingCard HUD: elapsed seconds, steps, and the sanitised trace.

    Shows real engine output only. dl.py's sanitize_app_logs() has already
    stripped internal hostnames, and errors are surfaced verbatim.
    """

    copy_logs = Signal()

    def __init__(self, parent=None) -> None:
        super().__init__(parent)
        self.setObjectName("Card")
        self._expanded = False
        self._steps: list[str] = []
        self._logs = ""
        self._is_error = False
        self._running = False

        root = QVBoxLayout(self)
        root.setContentsMargins(14, 13, 14, 13)
        root.setSpacing(10)

        header = QWidget()
        header_layout = QHBoxLayout(header)
        header_layout.setContentsMargins(2, 2, 2, 2)
        header_layout.setSpacing(10)

        self.icon_tile = QLabel("\u25B6")
        self.icon_tile.setFixedSize(28, 28)
        self.icon_tile.setAlignment(Qt.AlignCenter)
        self.icon_tile.setStyleSheet(
            f"background-color: #1E1E1E; border-radius: 14px; color: {theme.ARC_WHITE}; font-size: 13px;"
        )
        header_layout.addWidget(self.icon_tile)

        text_column = QVBoxLayout()
        text_column.setContentsMargins(0, 0, 0, 0)
        text_column.setSpacing(2)
        self.title_label = label("Ready", "SectionHeader")
        self.title_label.setStyleSheet(f"font-size: 13px; font-weight: 700;")
        self.detail_label = label("", "Meta")
        text_column.addWidget(self.title_label)
        text_column.addWidget(self.detail_label)
        header_layout.addLayout(text_column, 1)

        self.toggle_button = QPushButton("Details")
        self.toggle_button.setObjectName("Ghost")
        self.toggle_button.setCursor(Qt.PointingHandCursor)
        self.toggle_button.clicked.connect(self.toggle)
        header_layout.addWidget(self.toggle_button)
        root.addWidget(header)

        self.body = QWidget()
        body_layout = QVBoxLayout(self.body)
        body_layout.setContentsMargins(0, 0, 0, 0)
        body_layout.setSpacing(8)

        self.log_box = QFrame()
        self.log_box.setObjectName("LogBox")
        log_layout = QVBoxLayout(self.log_box)
        log_layout.setContentsMargins(12, 11, 12, 11)
        log_layout.setSpacing(7)

        log_header = QHBoxLayout()
        dot = QLabel()
        dot.setFixedSize(8, 8)
        dot.setStyleSheet(f"background-color: {theme.ARC_SUCCESS}; border-radius: 4px;")
        self.log_dot = dot
        log_header.addWidget(dot)
        caption = QLabel("Execution Process Trace")
        caption.setStyleSheet(
            f"color: {theme.ARC_MUTED}; font-family: {theme.MONO_FAMILY};"
            f" font-size: 10px; font-weight: 700;"
        )
        log_header.addWidget(caption)
        log_header.addStretch(1)
        self.copy_button = QPushButton("Copy")
        self.copy_button.setObjectName("Ghost")
        self.copy_button.setCursor(Qt.PointingHandCursor)
        self.copy_button.clicked.connect(lambda: self.copy_logs.emit())
        log_header.addWidget(self.copy_button)
        log_layout.addLayout(log_header)

        self.steps_label = QLabel("")
        self.steps_label.setObjectName("LogStep")
        self.steps_label.setWordWrap(True)
        self.steps_label.setTextInteractionFlags(Qt.TextSelectableByMouse)
        log_layout.addWidget(self.steps_label)

        self.logs_label = QLabel("")
        self.logs_label.setObjectName("LogText")
        self.logs_label.setWordWrap(True)
        self.logs_label.setTextInteractionFlags(Qt.TextSelectableByMouse)
        self.logs_label.setMaximumHeight(260)
        log_layout.addWidget(self.logs_label)

        body_layout.addWidget(self.log_box)
        self.body.setVisible(False)
        root.addWidget(self.body)
        self.setVisible(False)

    # ------------------------------------------------------------------ state
    def start(self, action: str, live_step: str = "") -> None:
        self._running = True
        self._steps = []
        self._logs = ""
        self._is_error = False
        self.title_label.setText("Processing...")
        self.detail_label.setText(live_step or action)
        self.icon_tile.setText("\u203A")
        self.icon_tile.setStyleSheet(
            f"background-color: #1E1E1E; border-radius: 14px;"
            f" color: {theme.ARC_WARNING}; font-size: 15px; font-weight: 700;"
        )
        self.log_dot.setStyleSheet(f"background-color: {theme.ARC_WARNING}; border-radius: 4px;")
        self._render()
        self.setVisible(True)

    def tick(self, elapsed_seconds: float, live_step: str = "") -> None:
        if not self._running:
            return
        if live_step:
            self.detail_label.setText(f"{live_step}   [{elapsed_seconds:.1f}s]")
        else:
            self.detail_label.setText(f"[{elapsed_seconds:.1f}s]")

    def finish(self, action: str, duration_seconds: float, steps: list[str],
               logs: str = "", is_error: bool = False) -> None:
        self._running = False
        self._steps = list(steps or [])
        self._logs = logs or ""
        self._is_error = bool(is_error)
        accent = theme.ARC_ERROR if is_error else theme.ARC_SUCCESS
        self.title_label.setText(
            f"{'Failed' if is_error else 'Completed'} in {duration_seconds:.1f}s \u00b7 {len(self._steps)} steps"
        )
        self.detail_label.setText(action)
        self.icon_tile.setText("\u2715" if is_error else "\u2713")
        self.icon_tile.setStyleSheet(
            f"background-color: {'#381515' if is_error else '#1B2E1E'}; border-radius: 14px;"
            f" color: {accent}; font-size: 13px; font-weight: 800;"
        )
        self.log_dot.setStyleSheet(f"background-color: {accent}; border-radius: 4px;")
        self.copy_button.setVisible(bool(self._logs.strip()))
        self._render()
        self.setVisible(True)

    def toggle(self) -> None:
        self._expanded = not self._expanded
        self.body.setVisible(self._expanded)
        self.toggle_button.setText("Hide" if self._expanded else "Details")

    def _render(self) -> None:
        if self._steps:
            self.steps_label.setText("\n".join(f">  {step}" for step in self._steps))
            self.steps_label.setVisible(True)
        else:
            self.steps_label.setVisible(False)
        if self._logs.strip():
            trimmed = self._logs.strip()
            if len(trimmed) > 6000:
                trimmed = "\u2026" + trimmed[-6000:]
            self.logs_label.setText(f"--- Process Log Output ---\n{trimmed}")
            self.logs_label.setVisible(True)
        else:
            self.logs_label.setVisible(False)

    def logs(self) -> str:
        return self._logs


class JobCard(QFrame):
    """One row of the Activity tab, matching the APK's job card."""

    pause_clicked = Signal(str)
    resume_clicked = Signal(str)
    cancel_clicked = Signal(str)
    dismiss_clicked = Signal(str)
    open_clicked = Signal(str)

    def __init__(self, job: dict, parent=None) -> None:
        super().__init__(parent)
        self.setObjectName("CardRaised")
        self.job_id = str(job.get("id", ""))
        root = QVBoxLayout(self)
        root.setContentsMargins(14, 13, 14, 13)
        root.setSpacing(10)

        top = QHBoxLayout()
        top.setSpacing(12)
        self.poster = AsyncImage(48, 64)
        self.poster.set_url(str(job.get("poster", "")))
        top.addWidget(self.poster)

        column = QVBoxLayout()
        column.setContentsMargins(0, 0, 0, 0)
        column.setSpacing(3)
        self.title_label = QLabel(str(job.get("title", "Download")))
        self.title_label.setStyleSheet("font-size: 14px; font-weight: 700;")
        self.detail_label = label(str(job.get("detail", "")), "Meta")
        column.addWidget(self.title_label)
        column.addWidget(self.detail_label)
        column.addStretch(1)
        top.addLayout(column, 1)

        self.status_pill = StatusPill(str(job.get("status", "Downloading")))
        top.addWidget(self.status_pill, 0, Qt.AlignTop)
        root.addLayout(top)

        self.progress = QProgressBar()
        self.progress.setRange(0, 1000)
        self.progress.setValue(0)
        self.progress.setTextVisible(False)
        root.addWidget(self.progress)

        stats = QHBoxLayout()
        stats.setContentsMargins(0, 0, 0, 0)
        self.percent_label = label("0.0%", "Meta")
        self.speed_label = label("--", "Meta")
        stats.addWidget(self.percent_label)
        stats.addStretch(1)
        stats.addWidget(self.speed_label)
        root.addLayout(stats)

        actions = QHBoxLayout()
        actions.setContentsMargins(0, 0, 0, 0)
        actions.setSpacing(8)
        actions.addStretch(1)
        self.open_button = self._button("Open", self.open_clicked)
        self.pause_button = self._button("Pause", self.pause_clicked)
        self.resume_button = self._button("Resume", self.resume_clicked)
        self.cancel_button = self._button("Cancel", self.cancel_clicked)
        self.dismiss_button = self._button("Dismiss", self.dismiss_clicked)
        for button in (self.open_button, self.pause_button, self.resume_button,
                       self.cancel_button, self.dismiss_button):
            actions.addWidget(button)
        root.addLayout(actions)
        self.update(job)

    def _button(self, text: str, signal) -> QPushButton:
        button = QPushButton(text)
        button.setObjectName("Ghost")
        button.setCursor(Qt.PointingHandCursor)
        button.clicked.connect(lambda _=False: signal.emit(self.job_id))
        return button

    def update(self, job: dict) -> None:
        status = str(job.get("status", "Downloading"))
        try:
            percent = float(job.get("progress", 0.0))
        except (TypeError, ValueError):
            percent = 0.0
        percent = max(0.0, min(100.0, percent))
        speed = str(job.get("speed", "--"))
        eta = str(job.get("eta", "--")).replace("ETA", "").strip() or "--"
        downloaded = str(job.get("downloaded", "--"))
        total = str(job.get("total", "--"))
        connecting = (
            status == "Downloading" and percent <= 0.5
            and (speed in ("--", "", "Connecting...") or downloaded in ("0 MB", "--")
                 or downloaded.startswith("Starting"))
        )

        self.title_label.setText(str(job.get("title", "Download")))
        detail = str(job.get("detail", ""))
        source = str(job.get("source", ""))
        if source and source.lower() not in ("netmirror", "stream", "catalog"):
            detail = f"{detail} \u00b7 {source}" if detail else source
        self.detail_label.setText(detail)

        self.status_pill.set_text("Fetching" if connecting else status,
                                  theme.ARC_ERROR if status == "Failed" else theme.ARC_MUTED)
        self.progress.setObjectName("Failed" if status == "Failed" else "Progress")
        if connecting:
            self.progress.setRange(0, 0)
        else:
            self.progress.setRange(0, 1000)
            self.progress.setValue(int(percent * 10))
        self.progress.style().unpolish(self.progress)
        self.progress.style().polish(self.progress)

        if connecting:
            self.percent_label.setText("Connecting & resolving stream...")
            self.speed_label.setText("Resolving...")
        else:
            size_text = downloaded if total in ("--", "", "Done") else f"{downloaded} / {total}"
            self.percent_label.setText(f"{percent:.1f}%  \u00b7  {size_text}")
            self.speed_label.setText(speed if eta in ("--", "") else f"{speed}  \u00b7  ETA {eta}")

        done = status == "Complete" and str(job.get("path", "")).strip() != ""
        self.open_button.setVisible(done)
        self.pause_button.setVisible(status == "Downloading")
        self.resume_button.setVisible(status == "Paused")
        self.cancel_button.setVisible(status in ("Downloading", "Paused"))
        self.dismiss_button.setVisible(status in ("Failed", "Cancelled", "Complete"))


class LibraryCard(QFrame):
    """A finished file in the Library tab."""

    open_clicked = Signal(str)
    folder_clicked = Signal(str)
    delete_clicked = Signal(str)

    def __init__(self, entry: dict, parent=None) -> None:
        super().__init__(parent)
        self.setObjectName("CardRaised")
        self.path = str(entry.get("path", ""))
        root = QVBoxLayout(self)
        root.setContentsMargins(14, 13, 14, 13)
        root.setSpacing(8)

        top = QHBoxLayout()
        top.setSpacing(12)
        self.poster = AsyncImage(48, 64)
        self.poster.set_url(str(entry.get("poster", "")))
        top.addWidget(self.poster)
        column = QVBoxLayout()
        column.setContentsMargins(0, 0, 0, 0)
        column.setSpacing(3)
        self.title_label = QLabel(str(entry.get("title", "File")))
        self.title_label.setStyleSheet("font-size: 14px; font-weight: 700;")
        self.title_label.setWordWrap(False)
        self.meta_label = label(str(entry.get("detail", "")), "Meta")
        column.addWidget(self.title_label)
        column.addWidget(self.meta_label)
        column.addStretch(1)
        top.addLayout(column, 1)
        root.addLayout(top)

        self.path_label = label(self.path, "MetaDim")
        self.path_label.setTextInteractionFlags(Qt.TextSelectableByMouse)
        root.addWidget(self.path_label)

        actions = QHBoxLayout()
        actions.setContentsMargins(0, 0, 0, 0)
        actions.setSpacing(8)
        actions.addStretch(1)
        self.open_button = QPushButton("Play")
        self.open_button.setObjectName("Primary")
        self.open_button.setCursor(Qt.PointingHandCursor)
        self.open_button.setFixedHeight(32)
        self.open_button.clicked.connect(lambda: self.open_clicked.emit(self.path))
        self.folder_button = QPushButton("Folder")
        self.folder_button.setObjectName("Ghost")
        self.folder_button.setCursor(Qt.PointingHandCursor)
        self.folder_button.clicked.connect(lambda: self.folder_clicked.emit(self.path))
        self.delete_button = QPushButton("Delete")
        self.delete_button.setObjectName("Ghost")
        self.delete_button.setCursor(Qt.PointingHandCursor)
        self.delete_button.clicked.connect(lambda: self.delete_clicked.emit(self.path))
        actions.addWidget(self.open_button)
        actions.addWidget(self.folder_button)
        actions.addWidget(self.delete_button)
        root.addLayout(actions)

    def refresh(self, entry: dict) -> None:
        from ..store import human_size
        size = human_size(entry.get("size_bytes"))
        detail = str(entry.get("detail", ""))
        self.meta_label.setText(f"{detail} \u00b7 {size}" if size != "--" else detail)
        self.title_label.setText(str(entry.get("title", "File")))


class Toast(QWidget):
    """The APK's bottom-left snackbar, as a frameless overlay."""

    def __init__(self, parent: QWidget) -> None:
        super().__init__(parent)
        self.setObjectName("Toast")
        self.setAttribute(Qt.WA_TransparentForMouseEvents, True)
        self.setStyleSheet(
            f"QWidget#Toast {{ background-color: #F2141414; border: 1px solid {theme.ARC_BORDER_STRONG};"
            f" border-radius: 20px; }}"
        )
        layout = QHBoxLayout(self)
        layout.setContentsMargins(16, 10, 18, 10)
        layout.setSpacing(10)
        dot = QLabel()
        dot.setFixedSize(6, 6)
        dot.setStyleSheet(f"background-color: {theme.ARC_WHITE}; border-radius: 3px;")
        self.text_label = QLabel("")
        self.text_label.setStyleSheet(f"color: {theme.ARC_WHITE}; font-size: 12px; font-weight: 500;")
        layout.addWidget(dot)
        layout.addWidget(self.text_label)
        self.hide()

    def show_message(self, text: str) -> None:
        self.text_label.setText(text)
        self.adjustSize()
        parent = self.parentWidget()
        if parent is not None:
            self.move(18, max(12, parent.height() - self.height() - 18))
        self.raise_()
        self.show()
