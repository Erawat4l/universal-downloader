#!/usr/bin/env python3
"""Tests for the Qt-free half of the Windows desktop app.

    python tests/test_desktop_core.py        (from the repository root)

These cover everything that does not need a display: engine discovery and
loading, the desktop adapter that replaces Android-only side effects, the
persistent store, and the bridge's job control. The Qt layer itself is verified
separately by `python -m arc_desktop --selftest`, which CI runs on a real
Windows runner.
"""

from __future__ import annotations

import importlib.util
import json
import os
import sys
import tempfile
import threading
import time
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
DESKTOP = REPO / "desktop-app"

PASSED: list[str] = []
FAILED: list[tuple[str, str]] = []

if str(DESKTOP) not in sys.path:
    sys.path.insert(0, str(DESKTOP))


def test(name: str):
    def decorate(fn):
        def wrapper():
            try:
                fn()
            except AssertionError as error:
                FAILED.append((name, str(error) or "assertion failed"))
                print(f"  FAIL  {name}\n        {error}")
                return
            except Exception as error:  # noqa: BLE001
                FAILED.append((name, f"{type(error).__name__}: {error}"))
                print(f"  ERROR {name}\n        {type(error).__name__}: {error}")
                return
            PASSED.append(name)
            print(f"  ok    {name}")
        wrapper.__name__ = fn.__name__
        TESTS.append(wrapper)
        return wrapper
    return decorate


TESTS: list = []


# =========================================================== engine discovery
@test("the bridge finds dl.py in a source checkout")
def t_find_engine():
    from arc_desktop.engine_bridge import find_engine, _candidate_engine_paths
    candidates = _candidate_engine_paths()
    assert candidates, "no candidate engine paths were produced"
    found = find_engine()
    assert found is not None, "dl.py was not found from a full checkout"
    assert found.name == "dl.py", found
    assert found.is_file()


@test("ARC_ENGINE_PATH overrides discovery")
def t_engine_path_override():
    from arc_desktop import engine_bridge
    original = os.environ.get("ARC_ENGINE_PATH")
    marker = Path(tempfile.mkdtemp()) / "custom-engine.py"
    marker.write_text("# not really an engine\n", encoding="utf-8")
    os.environ["ARC_ENGINE_PATH"] = str(marker)
    try:
        assert engine_bridge.find_engine() == marker.resolve(), engine_bridge.find_engine()
    finally:
        if original is None:
            os.environ.pop("ARC_ENGINE_PATH", None)
        else:
            os.environ["ARC_ENGINE_PATH"] = original


@test("a missing engine raises EngineError with an actionable message")
def t_missing_engine():
    from arc_desktop.engine_bridge import EngineBridge, EngineError
    original = os.environ.get("ARC_ENGINE_PATH")
    os.environ["ARC_ENGINE_PATH"] = str(Path(tempfile.mkdtemp()) / "nope.py")
    try:
        EngineBridge()
        raise AssertionError("expected EngineError for a missing engine")
    except EngineError as error:
        assert "ARC_ENGINE_PATH" in str(error), str(error)
    finally:
        if original is None:
            os.environ.pop("ARC_ENGINE_PATH", None)
        else:
            os.environ["ARC_ENGINE_PATH"] = original


@test("a broken engine reports the import failure instead of crashing")
def t_broken_engine():
    from arc_desktop.engine_bridge import EngineBridge, EngineError
    broken = Path(tempfile.mkdtemp()) / "dl.py"
    broken.write_text("raise RuntimeError('deliberate import failure')\n", encoding="utf-8")
    try:
        EngineBridge(broken)
        raise AssertionError("expected EngineError for a broken engine")
    except EngineError as error:
        assert "deliberate import failure" in str(error), str(error)


@test("the real engine loads in-process and answers capabilities")
def t_bridge_loads_engine():
    from arc_desktop.engine_bridge import EngineBridge
    bridge = EngineBridge()
    assert bridge.ready, bridge.load_error
    assert bridge.load_seconds < 20, f"engine took {bridge.load_seconds}s to import"
    response = bridge.capabilities()
    assert response.get("ok") is True, response
    assert "download" in response["capabilities"]["actions"]


@test("request() never raises: engine failures come back as ok=False")
def t_bridge_request_errors():
    from arc_desktop.engine_bridge import EngineBridge
    bridge = EngineBridge()
    response = bridge.request({"action": "resolve", "url": ""})
    assert response.get("ok") is False
    assert response.get("error"), response
    unknown = bridge.request({"action": "definitely-not-an-action"})
    assert unknown.get("ok") is False
    assert "Unknown app action" in str(unknown.get("error", ""))


@test("submit() runs on a worker thread and delivers the response")
def t_bridge_submit():
    from arc_desktop.engine_bridge import EngineBridge
    bridge = EngineBridge()
    received: dict = {}
    event = threading.Event()
    calling_thread = threading.get_ident()

    def on_done(response):
        received.update(response)
        received["_thread"] = threading.get_ident()
        event.set()

    job_id = bridge.submit({"action": "capabilities"}, on_done)
    assert job_id, "submit() must return a job id"
    assert event.wait(30), "the callback never fired"
    assert received.get("ok") is True, received
    assert received.get("job_id") == job_id
    assert received["_thread"] != calling_thread, "the callback ran on the caller's thread"


@test("the bridge serialises engine calls so logs cannot interleave")
def t_bridge_serialises_calls():
    from arc_desktop.engine_bridge import EngineBridge
    bridge = EngineBridge()
    results: list[dict] = []
    done = threading.Event()
    lock = threading.Lock()

    def collect(response):
        with lock:
            results.append(response)
            if len(results) == 3:
                done.set()

    for index in range(3):
        bridge.submit({"action": "resolve", "url": ""}, collect, job_id=f"serial-{index}")
    assert done.wait(40), f"only {len(results)} of 3 calls completed"
    assert all(item.get("ok") is False for item in results), results
    ids = {item.get("job_id") for item in results}
    assert ids == {f"serial-{index}" for index in range(3)}, ids


# ============================================================== job control
@test("pause/resume/cancel reach the engine and are safe with unknown ids")
def t_job_control():
    from arc_desktop.engine_bridge import EngineBridge
    bridge = EngineBridge()
    for method in ("pause", "resume", "cancel"):
        getattr(bridge, method)("no-such-job")       # must not raise
        getattr(bridge, method)("")                  # must not raise
    assert bridge.cancel("also-unknown") is None


@test("progress() returns a plain dict and never raises")
def t_progress():
    from arc_desktop.engine_bridge import EngineBridge
    bridge = EngineBridge()
    snapshot = bridge.progress()
    assert isinstance(snapshot, dict), type(snapshot)
    assert bridge.progress("unknown-job") == {}


@test("the progress snapshot shape is what the Activity tab expects")
def t_progress_shape():
    from arc_desktop.engine_bridge import EngineBridge
    bridge = EngineBridge()
    job = "shape-check"
    bridge.dl.ACTIVE_DOWNLOAD_PROGRESS[job] = {
        "percent": 42.5, "downloaded_str": "4.2 MB", "total_str": "10.0 MB",
        "speed": "1.1MB/s", "eta": "00:05", "status": "Downloading",
    }
    try:
        snapshot = bridge.progress().get(job)
        assert snapshot, "the job was not visible through the bridge"
        for key in ("percent", "downloaded_str", "total_str", "speed", "eta", "status"):
            assert key in snapshot, f"missing {key} in {snapshot}"
        assert snapshot["percent"] == 42.5
    finally:
        bridge.dl.ACTIVE_DOWNLOAD_PROGRESS.pop(job, None)


# =========================================================== platform adapter
@test("the adapter removes Android-only side effects from dl.py")
def t_adapter_patches():
    from arc_desktop import platform_adapter
    spec = importlib.util.spec_from_file_location("dl_adapter_probe", str(REPO / "dl.py"))
    module = importlib.util.module_from_spec(spec)
    sys.modules["dl_adapter_probe"] = module
    spec.loader.exec_module(module)

    report = platform_adapter.apply(module)
    assert "sync_to_sdcard_if_needed" in report, report
    assert "print_adm_link" in report, report
    assert "launch_player" in report, report

    # adb push to /sdcard is now a no-op that hands the path back unchanged.
    assert module.sync_to_sdcard_if_needed("/some/file.mp4") == "/some/file.mp4"
    # No :8080 server and no v1.mp4 hardlinks littering Downloads.
    assert module.print_adm_link("/some/file.mp4") is None
    assert module.start_local_http_server() is None
    # The player launcher must not shell out to adb on a desktop.
    import inspect
    source = inspect.getsource(module.launch_player)
    assert "adb" not in source, source


@test("the adapter registers an ffmpeg-backed EmbeddedEngine for dl.py's HD merge")
def t_desktop_muxer():
    from arc_desktop import platform_adapter
    saved = {name: sys.modules.get(name) for name in
             ("com", "com.erawat", "com.erawat.arc", "com.erawat.arc.EmbeddedEngine")}
    for name in saved:
        sys.modules.pop(name, None)
    try:
        assert platform_adapter.install_desktop_muxer() is True
        from com.erawat.arc import EmbeddedEngine
        assert callable(EmbeddedEngine.mux)
        # Missing inputs must be a clean False, never an exception.
        assert EmbeddedEngine.mux("/no/video.mp4", "/no/audio.m4a", "/no/out.mp4") is False
    finally:
        for name, module in saved.items():
            if module is None:
                sys.modules.pop(name, None)
            else:
                sys.modules[name] = module


@test("accelerator detection reports a dict for every known helper")
def t_detect_accelerators():
    from arc_desktop import platform_adapter
    found = platform_adapter.detect_accelerators()
    for name in platform_adapter.ACCELERATORS:
        assert name in found, f"{name} missing from {found}"
        assert found[name] is None or Path(found[name]).name, found[name]


@test("the default download directory exists and is writable")
def t_default_download_dir():
    from arc_desktop import platform_adapter
    folder = platform_adapter.default_download_dir()
    assert folder, "no download directory was resolved"
    assert Path(folder).is_dir(), folder
    probe = Path(folder) / ".arc-write-probe"
    probe.write_text("ok", encoding="utf-8")
    probe.unlink()


@test("open/reveal/play refuse empty paths instead of raising")
def t_path_helpers():
    from arc_desktop import platform_adapter
    assert platform_adapter.open_path("") is False
    assert platform_adapter.reveal_in_folder("") is False
    ok, message = platform_adapter.play_stream("", "")
    assert ok is False and message, message


# ==================================================================== store
@test("settings round-trip through JSON on disk")
def t_store_settings():
    from arc_desktop.store import Store
    path = Path(tempfile.mkdtemp()) / "arc.json"
    store = Store(path)
    store.set("download_path", "/tmp/downloads")
    store.set("notifications", False)
    assert path.is_file(), "settings were not written"

    reloaded = Store(path)
    assert reloaded.get("download_path") == "/tmp/downloads"
    assert reloaded.get("notifications") is False
    assert reloaded.get("default_video_quality"), "defaults were lost"


@test("a corrupt settings file cannot stop the app from starting")
def t_store_corrupt():
    from arc_desktop.store import Store, DEFAULT_SETTINGS
    path = Path(tempfile.mkdtemp()) / "arc.json"
    path.write_text("{ this is not json", encoding="utf-8")
    store = Store(path)
    for key, value in DEFAULT_SETTINGS.items():
        assert store.get(key) == value, f"{key} did not fall back to its default"


@test("history persists, de-duplicates by path, and hides deleted files")
def t_store_history():
    from arc_desktop.store import Store
    folder = Path(tempfile.mkdtemp())
    store = Store(folder / "arc.json")
    video = folder / "movie.mp4"
    video.write_bytes(b"x" * 2048)

    store.add_history({"title": "Movie", "path": str(video), "source": "Test"})
    store.add_history({"title": "Movie (again)", "path": str(video), "source": "Test"})
    entries = store.history()
    assert len(entries) == 1, f"re-adding the same path duplicated it: {entries}"
    assert entries[0]["title"] == "Movie (again)", entries[0]
    assert entries[0]["size_bytes"] == 2048, entries[0]

    video.unlink()
    assert store.history() == [], "a deleted file is still listed in the Library"
    assert len(store.history(only_existing=False)) == 1, "the record itself should survive"

    reloaded = Store(folder / "arc.json")
    assert len(reloaded.history(only_existing=False)) == 1, "history was not persisted"


@test("history is capped so the JSON file cannot grow forever")
def t_store_history_cap():
    from arc_desktop.store import Store
    folder = Path(tempfile.mkdtemp())
    store = Store(folder / "arc.json")
    store.set("max_history", 25)
    for index in range(60):
        store.add_history({"title": f"file-{index}", "path": str(folder / f"{index}.mp4")})
    assert len(store.history(only_existing=False)) <= 25, len(store.history(only_existing=False))
    newest = store.history(only_existing=False)[0]
    assert newest["title"] == "file-59", newest


@test("the folder scan finds media the CLI already downloaded, and ignores the rest")
def t_store_scan():
    from arc_desktop.store import Store
    folder = Path(tempfile.mkdtemp())
    (folder / "movie.mkv").write_bytes(b"x")
    (folder / "song.mp3").write_bytes(b"x")
    (folder / "notes.txt").write_text("ignore me", encoding="utf-8")
    (folder / "show").mkdir()
    (folder / "show" / "S01E01.mp4").write_bytes(b"x")
    (folder / ".arc-cache").mkdir()
    (folder / ".arc-cache" / ".temp_part.mp4").write_bytes(b"x")

    store = Store(folder / "arc.json")
    found = {entry["title"] for entry in store.scan_folder(str(folder))}
    assert "movie" in found and "song" in found, found
    assert "S01E01" in found, "nested media was missed"
    assert "notes" not in found, "a text file was listed as media"
    assert store.scan_folder(str(folder / "missing")) == []
    assert store.scan_folder("") == []


@test("human_size formats bytes and refuses nonsense")
def t_human_size():
    from arc_desktop.store import human_size
    assert human_size(0) == "--"
    assert human_size(None) == "--"
    assert human_size("abc") == "--"
    assert human_size(1024) == "1.0 KB"
    assert human_size(5 * 1024 * 1024) == "5.0 MB"
    assert human_size(3_900_000_000) == "3.6 GB"


@test("config_dir resolves to a real writable location")
def t_config_dir():
    from arc_desktop.store import config_dir
    folder = config_dir()
    assert folder.is_dir(), folder
    probe = folder / ".write-probe"
    probe.write_text("ok", encoding="utf-8")
    probe.unlink()


# =============================================================== environment
@test("environment() tells the truth about speed and integration")
def t_environment():
    from arc_desktop.engine_bridge import EngineBridge
    environment = EngineBridge().environment()
    for key in ("engine_path", "engine_ready", "python", "accelerators", "aria2c",
                "ffmpeg", "parallel_connections", "adapter", "default_download_dir"):
        assert key in environment, f"{key} missing from environment()"
    assert environment["engine_ready"] is True
    assert environment["parallel_connections"] == (16 if environment["aria2c"] else 1), environment
    assert isinstance(environment["adapter"], dict) and environment["adapter"]
    assert json.dumps(environment)          # must be serialisable for the UI


# ===================================================================== theme
@test("design tokens match the APK's palette")
def t_theme_tokens():
    source = (DESKTOP / "arc_desktop" / "theme.py").read_text(encoding="utf-8")
    kotlin = (REPO / "android-app" / "app" / "src" / "main" / "java" / "com" / "erawat" / "arc"
              / "MainActivity.kt")
    for token, value in (("ARC_BLACK", "050505"), ("ARC_SURFACE", "111111"),
                         ("ARC_RAISED", "191919"), ("ARC_WHITE", "F7F7F7"),
                         ("ARC_MUTED", "B0B0B0"), ("ARC_SUBTLE", "777777")):
        assert f"{token} = \"#{value}\"" in source, f"{token} drifted from #{value}"
        if kotlin.is_file():
            assert value.lower() in kotlin.read_text(encoding="utf-8").lower(), \
                f"#{value} is no longer used by the APK, so the two designs have diverged"


@test("every intra-package import in the desktop app resolves")
def t_imports_resolve():
    """Catches the ImportError class of bug without needing a Qt runtime.

    A misspelled or misplaced import only surfaces when the module is imported,
    which for the Qt layer means a display. Checking the AST instead means CI
    finds it before the headless self-test ever runs.
    """
    import ast

    root = DESKTOP / "arc_desktop"
    modules = {path.relative_to(root.parent).with_suffix("").as_posix().replace("/", "."): path
               for path in root.rglob("*.py")}
    assert modules, "no desktop modules were found"

    def top_level_names(path: Path) -> set:
        tree = ast.parse(path.read_text(encoding="utf-8"))
        names: set = set()
        for node in ast.walk(tree):
            if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef)):
                names.add(node.name)
            elif isinstance(node, ast.Assign):
                for target in node.targets:
                    if isinstance(target, ast.Name):
                        names.add(target.id)
            elif isinstance(node, ast.AnnAssign) and isinstance(node.target, ast.Name):
                names.add(node.target.id)
            elif isinstance(node, ast.Import):
                for alias in node.names:
                    names.add((alias.asname or alias.name).split(".")[0])
            elif isinstance(node, ast.ImportFrom):
                for alias in node.names:
                    names.add(alias.asname or alias.name)
        return names

    defined = {name: top_level_names(path) for name, path in modules.items()}
    problems: list[str] = []
    for module, path in sorted(modules.items()):
        tree = ast.parse(path.read_text(encoding="utf-8"))
        for node in ast.walk(tree):
            if not isinstance(node, ast.ImportFrom) or node.level == 0:
                continue
            parts = module.split(".")
            base = parts[: len(parts) - node.level] if node.level else parts
            target = ".".join(base + ([node.module] if node.module else []))
            resolved = next((c for c in (target, target + ".__init__") if c in defined), None)
            if resolved is None:
                problems.append(f"{module}: cannot resolve module '{target}'")
                continue
            package = resolved[:-9] if resolved.endswith(".__init__") else resolved
            for alias in node.names:
                if alias.name in defined[resolved]:
                    continue
                # `from .. import theme` imports a submodule, which is valid even
                # though the package __init__ never names it.
                if package and f"{package}.{alias.name}" in modules:
                    continue
                problems.append(
                    f"{module}: imports '{alias.name}' from {resolved}, which does not define it")
    assert not problems, "\n        " + "\n        ".join(problems)


@test("every PySide6 name the app imports exists in the installed Qt")
def t_qt_names_exist():
    """Verifies Qt API usage against the shipped type stubs, without importing Qt.

    Importing PySide6 needs a working platform plugin, so this reads the .pyi
    stubs the wheel ships and confirms each imported class/enum is really there.
    """
    import ast
    import re

    try:
        import PySide6
    except Exception:
        return                      # Qt not installed here; CI covers it
    stub_dir = Path(PySide6.__file__).resolve().parent
    stubs = {path.stem: path for path in stub_dir.glob("Qt*.pyi")}
    if not stubs:
        return                      # stubs stripped from this install

    available = {name: set(re.findall(r"^\s*class\s+([A-Za-z_]\w*)",
                                      path.read_text(encoding="utf-8", errors="replace"), re.M))
                 | set(re.findall(r"^\s{4}([A-Z]\w+)\s*=",
                                   path.read_text(encoding="utf-8", errors="replace"), re.M))
                 for name, path in stubs.items()}

    problems: list[str] = []
    checked = 0
    for path in sorted((DESKTOP / "arc_desktop").rglob("*.py")):
        tree = ast.parse(path.read_text(encoding="utf-8"))
        for node in ast.walk(tree):
            if not isinstance(node, ast.ImportFrom) or not node.module:
                continue
            if not node.module.startswith("PySide6"):
                continue
            stub = node.module.split(".")[-1]
            names = available.get(stub)
            if names is None:
                problems.append(f"{path.name}: no stub module for {node.module}")
                continue
            for alias in node.names:
                checked += 1
                if alias.name not in names:
                    problems.append(f"{path.name}: '{alias.name}' is not in PySide6.{stub}")
    assert checked > 20, f"only {checked} Qt imports were checked - the scan is broken"
    assert not problems, "\n        " + "\n        ".join(problems)


@test("successful search results render immediately while posters load asynchronously")
def t_search_results_visible_immediately():
    pages = (DESKTOP / "arc_desktop" / "ui" / "pages.py").read_text(encoding="utf-8")
    assert "self.thinking.setVisible(not bool(hits))" in pages
    add_card = pages.index("self.results_host.addWidget(item)", pages.index("def set_results"))
    show_card = pages.index("item.show()", add_card)
    start_poster = pages.index("poster.set_url(str(hit.get(\"poster\", \"\")))", add_card)
    assert add_card < show_card < start_poster
    assert "item.hide()" not in pages


@test("Android build keeps its checked-in engine instead of copying desktop dl.py")
def t_android_engine_stays_independent():
    builder = (REPO / "android-app" / "tools" / "build_android.py").read_text(encoding="utf-8")
    assert "Using the checked-in Android engine" in builder
    assert "shutil.copy2(PYTHON_ENGINE_SOURCE, PYTHON_ENGINE_DEST)" not in builder


# ============================================================== main
def main() -> int:
    print("")
    print("=" * 72)
    print(" Arc Desktop core tests (Qt-free)")
    print(f" desktop-app: {DESKTOP}")
    print(f" python     : {sys.version.split()[0]} on {sys.platform}")
    print("=" * 72)
    print("")
    for case in list(TESTS):
        case()
    print("")
    print("=" * 72)
    print(f" passed {len(PASSED)}   failed {len(FAILED)}   total {len(TESTS)}")
    if FAILED:
        print("")
        print(" Failures:")
        for name, reason in FAILED:
            print(f"   - {name}")
            print(f"     {reason}")
    print("=" * 72)
    print("")
    return 1 if FAILED else 0


if __name__ == "__main__":
    raise SystemExit(main())
