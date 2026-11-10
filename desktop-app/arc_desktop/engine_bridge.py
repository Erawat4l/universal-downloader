"""In-process bridge to the repository's dl.py engine.

This is the desktop twin of `android-app/.../EmbeddedEngine.kt`: it loads the
same engine module, calls the same `run_app_json_request` machine interface, and
polls the same `get_download_progress_json` for live progress.

Two deliberate differences from the APK:

* Every engine call is serialised behind a lock. `dl.run_app_json_request` uses
  `contextlib.redirect_stdout`, which is process-global, so two concurrent
  actions would capture each other's logs. The APK does not guard this.
* The module is Qt-free. The UI layer subscribes with plain callbacks, which
  keeps the engine testable headless and on CI without a display.
"""

from __future__ import annotations

import importlib.util
import json
import os
import sys
import threading
import time
import traceback
import uuid
from pathlib import Path
from typing import Any, Callable, Optional

from . import platform_adapter

ENGINE_MODULE_NAME = "dl"


def _candidate_engine_paths() -> list[Path]:
    """Every place dl.py can legitimately live, best first (auto-discovery)."""
    candidates: list[Path] = []

    # PyInstaller onefile/onedir unpacks bundled data under sys._MEIPASS.
    meipass = getattr(sys, "_MEIPASS", None)
    if meipass:
        base = Path(meipass)
        candidates += [base / "engine" / "dl.py", base / "dl.py"]

    if getattr(sys, "frozen", False):
        exe_dir = Path(sys.executable).resolve().parent
        candidates += [exe_dir / "engine" / "dl.py", exe_dir / "dl.py",
                       exe_dir.parent / "dl.py", exe_dir.parent / "engine" / "dl.py"]

    here = Path(__file__).resolve()
    # .../desktop-app/arc_desktop/engine_bridge.py -> parents[2] is the repo root
    candidates += [
        here.parent / "engine" / "dl.py",
        here.parent.parent / "engine" / "dl.py",
        here.parent.parent / "dl.py",
        here.parents[2] / "dl.py" if len(here.parents) > 2 else here.parent / "dl.py",
    ]

    seen: set[Path] = set()
    unique: list[Path] = []
    for candidate in candidates:
        try:
            resolved = candidate.resolve()
        except Exception:
            continue
        if resolved in seen:
            continue
        seen.add(resolved)
        unique.append(resolved)
    return unique


def find_engine() -> Optional[Path]:
    """Locate dl.py.

    ARC_ENGINE_PATH is authoritative: if it is set but wrong, that is an error
    worth reporting. Silently falling back to auto-discovery would hide a typo
    and freeze the wrong engine into a build.
    """
    override = (os.environ.get("ARC_ENGINE_PATH") or "").strip()
    if override:
        candidate = Path(override).expanduser()
        if candidate.is_file():
            return candidate.resolve()
        raise EngineError(
            f"ARC_ENGINE_PATH points at {candidate}, which is not a file. "
            "Fix the path, or unset it to let Arc find dl.py automatically."
        )
    for candidate in _candidate_engine_paths():
        if candidate.is_file():
            return candidate
    return None


class EngineError(RuntimeError):
    """The engine could not be located or imported."""


class EngineBridge:
    """Loads dl.py once and exposes it as a small, typed, thread-safe API."""

    def __init__(self, engine_path: Optional[Path] = None) -> None:
        self._lock = threading.RLock()
        self._call_lock = threading.Lock()
        self._workers: list[threading.Thread] = []
        self.path: Optional[Path] = None
        self.dl: Any = None
        self.adapter_report: dict[str, str] = {}
        self.load_error: str = ""
        self._loaded_at: float = 0.0
        self.load(engine_path)

    # ------------------------------------------------------------------ setup
    def load(self, engine_path: Optional[Path] = None) -> None:
        with self._lock:
            if self.dl is not None and engine_path is None:
                return
            path = Path(engine_path) if engine_path else find_engine()
            if path is None or not path.is_file():
                self.load_error = (
                    "Could not find dl.py. Set ARC_ENGINE_PATH to its location, or run "
                    "this app from inside the repository."
                )
                raise EngineError(self.load_error)

            self.path = path
            # dl.py resolves `import igl` from its own directory.
            engine_dir = str(path.parent)
            if engine_dir not in sys.path:
                sys.path.insert(0, engine_dir)

            started = time.time()
            spec = importlib.util.spec_from_file_location(ENGINE_MODULE_NAME, str(path))
            if spec is None or spec.loader is None:
                self.load_error = f"Could not build an import spec for {path}"
                raise EngineError(self.load_error)
            module = importlib.util.module_from_spec(spec)
            sys.modules[ENGINE_MODULE_NAME] = module
            try:
                spec.loader.exec_module(module)
            except Exception as error:
                sys.modules.pop(ENGINE_MODULE_NAME, None)
                self.load_error = f"dl.py failed to import: {error}\n{traceback.format_exc()}"
                raise EngineError(self.load_error) from error

            # Windows has no /sdcard, so point the engine at a real folder before
            # any handler reads DEFAULT_DIR.
            download_dir = platform_adapter.default_download_dir()
            try:
                module.DEFAULT_DIR = download_dir
            except Exception:
                pass

            self.adapter_report = platform_adapter.apply(module)
            self.dl = module
            self._loaded_at = time.time() - started
            self.load_error = ""

    @property
    def ready(self) -> bool:
        return self.dl is not None

    @property
    def load_seconds(self) -> float:
        return self._loaded_at

    # --------------------------------------------------------------- requests
    def request(self, payload: dict) -> dict:
        """Run one engine action and return its raw response dict.

        Never raises for engine-side failures: dl.py already converts them into
        {"ok": False, "error": ..., "logs": ...} so the UI can show the real
        message instead of a generic one.
        """
        if self.dl is None:
            return {"ok": False, "error": self.load_error or "Engine is not loaded."}
        # redirect_stdout inside run_app_json_request is process-global.
        with self._call_lock:
            try:
                result = self.dl.run_app_json_request(payload)
                if isinstance(result, dict):
                    return result
                return {"ok": False, "error": f"Engine returned {type(result).__name__}, expected dict."}
            except Exception as error:
                return {"ok": False, "error": f"{type(error).__name__}: {error}",
                        "logs": traceback.format_exc(limit=6)}

    def submit(self, payload: dict, on_done: Callable[[dict], None],
               job_id: Optional[str] = None) -> str:
        """Run an action on a worker thread; on_done fires with the response.

        The callback is invoked on the worker thread. Qt callers must hop back to
        the GUI thread themselves (the UI layer does this with a signal).
        """
        job_id = job_id or str(uuid.uuid4())
        payload = dict(payload)
        payload.setdefault("job_id", job_id)

        def run() -> None:
            response = self.request(payload)
            response.setdefault("job_id", job_id)
            try:
                on_done(response)
            except Exception:
                traceback.print_exc()

        worker = threading.Thread(target=run, name=f"arc-engine-{job_id[:8]}", daemon=True)
        with self._lock:
            self._workers = [t for t in self._workers if t.is_alive()]
            self._workers.append(worker)
        worker.start()
        return job_id

    # -------------------------------------------------------------- shortcuts
    def capabilities(self) -> dict:
        return self.request({"action": "capabilities"})

    def search(self, query: str, platform: str = "netmirror") -> dict:
        return self.request({"action": "search", "query": query, "platform": platform})

    def resolve(self, url: str, quality: str = "best",
                season: Optional[int] = None, episode: Optional[int] = None) -> dict:
        payload: dict[str, Any] = {"action": "resolve", "url": url, "quality": quality}
        if season is not None:
            payload["season"] = season
        if episode is not None:
            payload["episode"] = episode
        return self.request(payload)

    def download(self, payload: dict) -> dict:
        body = dict(payload)
        body["action"] = "download"
        return self.request(body)

    def stream_proxy(self, url: str, headers: Optional[dict] = None) -> str:
        response = self.request({"action": "stream_proxy", "url": url, "headers": headers or {}})
        if response.get("ok"):
            result = response.get("result") or {}
            proxy = result.get("proxy_url") or response.get("proxy_url")
            if proxy:
                return str(proxy)
        return url

    def telegram_check(self, api_id: str, api_hash: str, session: str) -> dict:
        return self.request({"action": "telegram_check", "api_id": api_id,
                             "api_hash": api_hash, "session": session})

    def igl_catalog(self) -> dict:
        return self.request({"action": "igl"})

    # ----------------------------------------------------------- job control
    def progress(self, job_id: Optional[str] = None) -> dict:
        """Snapshot of live progress. Cheap: no stdout redirection involved."""
        if self.dl is None:
            return {}
        try:
            raw = self.dl.get_download_progress_json(job_id or "")
            data = json.loads(raw) if raw else {}
            return data if isinstance(data, dict) else {}
        except Exception:
            return {}

    def cancel(self, job_id: str) -> None:
        self._job_call("cancel_download", job_id)

    def pause(self, job_id: str) -> None:
        self._job_call("pause_download", job_id)

    def resume(self, job_id: str) -> None:
        self._job_call("resume_download", job_id)

    def _job_call(self, name: str, job_id: str) -> None:
        if self.dl is None or not job_id:
            return
        try:
            getattr(self.dl, name)(job_id)
        except Exception:
            pass

    # ----------------------------------------------------------- diagnostics
    def environment(self) -> dict:
        """What this runtime can actually do - shown in Settings."""
        accelerators = platform_adapter.detect_accelerators()
        engine_version = ""
        if self.dl is not None:
            try:
                caps = self.dl.app_cli_capabilities()
                engine_version = str(caps.get("version", ""))
            except Exception:
                engine_version = ""
        return {
            "engine_path": str(self.path) if self.path else "",
            "engine_ready": self.ready,
            "engine_load_seconds": round(self._loaded_at, 3),
            "engine_version": engine_version,
            "python": sys.version.split()[0],
            "frozen": bool(getattr(sys, "frozen", False)),
            "platform": sys.platform,
            "accelerators": accelerators,
            "aria2c": bool(accelerators.get("aria2c")),
            "ffmpeg": bool(accelerators.get("ffmpeg")),
            "vlc": platform_adapter.find_vlc() or "",
            "adapter": dict(self.adapter_report),
            "parallel_connections": 16 if accelerators.get("aria2c") else 1,
            "default_download_dir": platform_adapter.default_download_dir(),
        }

    def shutdown(self) -> None:
        with self._lock:
            self._workers.clear()
