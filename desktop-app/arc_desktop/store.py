"""Persistent settings and download history.

The APK keeps its job list in Compose `remember { }`, so its Library is empty
after every restart even though the files are still on the device. The desktop
build stores both settings and finished downloads in a small JSON file, and
re-validates each remembered file against disk on load so the Library never
lists something that was deleted.
"""

from __future__ import annotations

import json
import os
import sys
import threading
import time
from pathlib import Path
from typing import Any, Optional

APP_DIR_NAME = "Arc"


def config_dir() -> Path:
    if sys.platform.startswith("win"):
        base = os.environ.get("APPDATA") or str(Path.home() / "AppData" / "Roaming")
    elif sys.platform == "darwin":
        base = str(Path.home() / "Library" / "Application Support")
    else:
        base = os.environ.get("XDG_CONFIG_HOME") or str(Path.home() / ".config")
    path = Path(base) / APP_DIR_NAME
    try:
        path.mkdir(parents=True, exist_ok=True)
    except Exception:
        path = Path.home() / f".{APP_DIR_NAME.lower()}"
        try:
            path.mkdir(parents=True, exist_ok=True)
        except Exception:
            pass
    return path


DEFAULT_SETTINGS: dict[str, Any] = {
    "download_path": "",
    "default_video_quality": "Ask each time",
    "default_audio_quality": "Ask each time",
    "notifications": True,
    "prefer_vlc": True,
    "open_folder_when_done": False,
    "telegram_api_id": "",
    "telegram_api_hash": "",
    "telegram_session": "",
    "max_history": 200,
}


class Store:
    """Thread-safe JSON-backed settings plus download history."""

    def __init__(self, path: Optional[Path] = None) -> None:
        self._lock = threading.RLock()
        self.path = Path(path) if path else config_dir() / "arc-desktop.json"
        self._data: dict[str, Any] = {"settings": dict(DEFAULT_SETTINGS), "history": []}
        self.load()

    # -------------------------------------------------------------- lifecycle
    def load(self) -> None:
        with self._lock:
            try:
                if self.path.is_file():
                    raw = json.loads(self.path.read_text(encoding="utf-8"))
                    if isinstance(raw, dict):
                        settings = dict(DEFAULT_SETTINGS)
                        settings.update(raw.get("settings") or {})
                        self._data["settings"] = settings
                        history = raw.get("history")
                        self._data["history"] = history if isinstance(history, list) else []
            except Exception:
                # A corrupt file must never stop the app from starting.
                self._data = {"settings": dict(DEFAULT_SETTINGS), "history": []}

    def save(self) -> bool:
        with self._lock:
            try:
                self.path.parent.mkdir(parents=True, exist_ok=True)
                tmp = self.path.with_suffix(".tmp")
                tmp.write_text(json.dumps(self._data, ensure_ascii=False, indent=2), encoding="utf-8")
                os.replace(tmp, self.path)
                return True
            except Exception:
                return False

    # --------------------------------------------------------------- settings
    def get(self, key: str, default: Any = None) -> Any:
        with self._lock:
            if key not in self._data["settings"] and default is not None:
                return default
            return self._data["settings"].get(key, DEFAULT_SETTINGS.get(key, default))

    def set(self, key: str, value: Any, persist: bool = True) -> None:
        with self._lock:
            self._data["settings"][key] = value
        if persist:
            self.save()

    def update(self, values: dict, persist: bool = True) -> None:
        with self._lock:
            self._data["settings"].update(values)
        if persist:
            self.save()

    def all_settings(self) -> dict:
        with self._lock:
            return dict(self._data["settings"])

    # ---------------------------------------------------------------- history
    def add_history(self, entry: dict) -> None:
        record = dict(entry)
        record.setdefault("finished_at", time.time())
        with self._lock:
            history = self._data["history"]
            # Replace an older record for the same path instead of duplicating.
            path = record.get("path")
            if path:
                self._data["history"] = [h for h in history if h.get("path") != path]
            self._data["history"].insert(0, record)
            limit = int(self._data["settings"].get("max_history", 200) or 200)
            self._data["history"] = self._data["history"][:max(20, limit)]
        self.save()

    def history(self, only_existing: bool = True) -> list[dict]:
        with self._lock:
            items = [dict(h) for h in self._data["history"]]
        if not only_existing:
            return items
        kept = []
        for item in items:
            path = item.get("path") or ""
            if path and Path(path).is_file():
                try:
                    item["size_bytes"] = Path(path).stat().st_size
                except Exception:
                    pass
                kept.append(item)
        return kept

    def remove_history(self, path: str) -> None:
        with self._lock:
            self._data["history"] = [h for h in self._data["history"] if h.get("path") != path]
        self.save()

    def clear_history(self) -> None:
        with self._lock:
            self._data["history"] = []
        self.save()

    def scan_folder(self, folder: str, limit: int = 200) -> list[dict]:
        """Pick up media the CLI or an older build already downloaded.

        This is what makes the Library useful on first run: it reflects the
        folder on disk rather than only what this process downloaded.
        """
        if not folder:
            return []
        root = Path(folder)
        if not root.is_dir():
            return []
        extensions = {".mp4", ".mkv", ".webm", ".mov", ".avi", ".ts", ".m4v",
                      ".mp3", ".m4a", ".flac", ".wav", ".aac", ".ogg", ".opus",
                      ".pdf", ".m3u8"}
        found: list[dict] = []
        try:
            entries = [p for p in root.rglob("*") if p.is_file() and p.suffix.lower() in extensions]
        except Exception:
            return []
        entries.sort(key=lambda p: p.stat().st_mtime if p.exists() else 0, reverse=True)
        for entry in entries[:limit]:
            try:
                stat = entry.stat()
            except Exception:
                continue
            found.append({
                "title": entry.stem,
                "path": str(entry),
                "size_bytes": stat.st_size,
                "finished_at": stat.st_mtime,
                "source": "Folder scan",
                "detail": entry.suffix.lstrip(".").upper() or "FILE",
            })
        return found


def human_size(num_bytes: Any) -> str:
    try:
        size = float(num_bytes)
    except (TypeError, ValueError):
        return "--"
    if size <= 0:
        return "--"
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if size < 1024 or unit == "TB":
            return f"{size:.1f} {unit}" if unit != "B" else f"{int(size)} B"
        size /= 1024
    return "--"
