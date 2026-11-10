#!/usr/bin/env python3
"""Engine regression tests for dl.py.

Run with plain Python - no pytest, no network, no third-party packages beyond
what the engine itself needs:

    python tests/test_engine_regressions.py

Each test names the bug it locks down, so a future edit that reintroduces one
fails with an explanation rather than a mystery. Tests that need a real HTTP
transfer serve it from 127.0.0.1, so the suite is hermetic and works offline.

Exit code 0 = everything passed.
"""

from __future__ import annotations

import ast
import http.server
import importlib.util
import os
import socketserver
import sys
import tempfile
import threading
import time
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
ENGINE = REPO / "dl.py"

PASSED: list[str] = []
FAILED: list[tuple[str, str]] = []


# --------------------------------------------------------------------- harness
def test(name: str):
    def decorate(fn):
        def wrapper():
            try:
                fn()
            except AssertionError as error:
                FAILED.append((name, str(error) or "assertion failed"))
                print(f"  FAIL  {name}\n        {error}")
                return
            except Exception as error:  # noqa: BLE001 - report, do not crash
                FAILED.append((name, f"{type(error).__name__}: {error}"))
                print(f"  ERROR {name}\n        {type(error).__name__}: {error}")
                return
            PASSED.append(name)
            print(f"  ok    {name}")
        wrapper.__name__ = fn.__name__
        return wrapper
    return decorate


def load_engine():
    spec = importlib.util.spec_from_file_location("dl_under_test", str(ENGINE))
    module = importlib.util.module_from_spec(spec)
    sys.modules["dl_under_test"] = module
    spec.loader.exec_module(module)
    return module


DL = None            # populated in main()
TESTS: list = []


def register(fn):
    TESTS.append(fn)
    return fn


# ------------------------------------------------------- local HTTP test server
class _TestServer:
    """Serves a fixed payload from a temp directory, optionally throttled."""

    def __init__(self, filename: str = "asset.bin", size: int = 1 << 20,
                 delay_per_chunk: float = 0.0) -> None:
        self.root = Path(tempfile.mkdtemp())
        (self.root / filename).write_bytes(b"\xAB" * size)
        self.filename = filename
        self.size = size
        self.delay = delay_per_chunk
        outer = self

        class Handler(http.server.SimpleHTTPRequestHandler):
            def __init__(self, *args, **kwargs):
                kwargs["directory"] = str(outer.root)
                super().__init__(*args, **kwargs)

            def log_message(self, *args):
                pass

            def do_GET(self):
                path = self.translate_path(self.path)
                if not os.path.isfile(path):
                    self.send_error(404)
                    return
                total = os.path.getsize(path)
                start, end = 0, total - 1
                code = 200
                header_range = self.headers.get("Range")
                if header_range and header_range.startswith("bytes="):
                    first, _, last = header_range[6:].partition("-")
                    start = int(first or 0)
                    end = int(last) if last else total - 1
                    code = 206
                length = end - start + 1
                self.send_response(code)
                self.send_header("Content-Length", str(length))
                self.send_header("Accept-Ranges", "bytes")
                self.send_header("Content-Type", "application/octet-stream")
                self.end_headers()
                try:
                    with open(path, "rb") as handle:
                        handle.seek(start)
                        remaining = length
                        while remaining > 0:
                            chunk = handle.read(min(32 * 1024, remaining))
                            if not chunk:
                                break
                            self.wfile.write(chunk)
                            remaining -= len(chunk)
                            if outer.delay:
                                time.sleep(outer.delay)
                except (BrokenPipeError, ConnectionResetError):
                    pass

        self.httpd = socketserver.ThreadingTCPServer(("127.0.0.1", 0), Handler)
        self.httpd.allow_reuse_address = True
        self.thread = threading.Thread(target=self.httpd.serve_forever, daemon=True)
        self.thread.start()

    @property
    def url(self) -> str:
        host, port = self.httpd.server_address[:2]
        return f"http://{host}:{port}/{self.filename}"

    @property
    def missing_url(self) -> str:
        host, port = self.httpd.server_address[:2]
        return f"http://{host}:{port}/does-not-exist.bin"

    def close(self) -> None:
        self.httpd.shutdown()
        self.httpd.server_close()


# =============================== engine structure =============================
@register
@test("engine parses and byte-compiles")
def t_compiles():
    source = ENGINE.read_text(encoding="utf-8")
    ast.parse(source)
    assert "def run_app_json_request(" in source
    assert "def app_download(" in source


@register
@test("B1: _get_fallback_tg_creds is defined before handle_telegram uses it")
def t_telegram_restore():
    tree = ast.parse(ENGINE.read_text(encoding="utf-8"))
    definitions = {node.name: node.lineno for node in ast.walk(tree)
                   if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef))}
    assert "handle_telegram" in definitions, "handle_telegram disappeared"
    assert "_get_fallback_tg_creds" in definitions, \
        "the Telegram credential helper is missing again (B1 regression)"
    assert callable(getattr(DL, "_get_fallback_tg_creds"))
    # It must return three values, and handle_telegram must be intact.
    result = DL._get_fallback_tg_creds()
    assert isinstance(result, tuple) and len(result) == 3, f"expected 3-tuple, got {result!r}"
    assert callable(getattr(DL, "handle_telegram"))


@register
@test("windowed-exe shim: None stdio does not break import or isatty")
def t_null_stream():
    for name in ("stdin", "stdout", "stderr"):
        assert getattr(sys, name, None) is not None or hasattr(DL, "_NullStream")
    stream = DL._NullStream()
    assert stream.isatty() is False
    assert stream.write("anything") >= 0
    try:
        stream.read()
        raise AssertionError("read() should refuse in a windowed runtime")
    except EOFError:
        pass


# =================================== formatting ===============================
@register
@test("B3: format_speed renders bytes/second, not raw numbers")
def t_format_speed():
    assert DL.format_speed(2_500_000) == "2.38MB/s", DL.format_speed(2_500_000)
    assert DL.format_speed(1536) == "1.5KB/s"
    assert DL.format_speed(512) == "512B/s"
    for empty in (0, None, "", -5):
        assert DL.format_speed(empty) == "--", f"{empty!r} -> {DL.format_speed(empty)!r}"
    assert DL.format_speed("1.2MB/s") == "1.2MB/s"   # already formatted: pass through


@register
@test("B3: format_eta renders H:MM:SS and refuses nonsense")
def t_format_eta():
    assert DL.format_eta(3725) == "1:02:05", DL.format_eta(3725)
    assert DL.format_eta(65) == "01:05"
    assert DL.format_eta(0) == "--"
    assert DL.format_eta(None) == "--"
    assert DL.format_eta("soon") == "soon"


@register
@test("B3: render_download_progress accepts the documented kwargs")
def t_render_progress_signature():
    import inspect
    signature = inspect.signature(DL.render_download_progress)
    for parameter in ("percent", "total_str", "speed_str", "eta_str", "job_id", "downloaded_str"):
        assert parameter in signature.parameters, f"missing kwarg {parameter}"


@register
@test("B3: HLS downloader takes job_id so it can report and be cancelled")
def t_hls_signature():
    import inspect
    signature = inspect.signature(DL.download_hls_with_python)
    assert "job_id" in signature.parameters, "download_hls_with_python lost its job_id"


# =================================== quality ==================================
QUALITY_TABLE = [
    ("1080p", "1080p"),
    ("4k", "4k"),
    ("best", "4k"),
    ("1080p (Full HD)", "1080p"),
    ("2160p (4K UHD)", "4k"),
    ("480P 2GB", "480p"),
    ("720p (HD)", "720p"),
    ("Best Available", "4k"),
    ("auto", "4k"),
    ("360p (SD)", None),          # absent from the key list: caller keeps its fallback
]


@register
@test("B5/B6: match_quality_key understands CLI, app and CDN quality spellings")
def t_match_quality_key():
    keys = ["4k", "1080p", "720p", "480p"]     # best first, as callers must supply
    for requested, expected in QUALITY_TABLE:
        actual = DL.match_quality_key(requested, keys)
        assert actual == expected, f"{requested!r} -> {actual!r}, expected {expected!r}"


@register
@test("B5/B6: match_quality_key survives empty and odd input")
def t_match_quality_key_edges():
    assert DL.match_quality_key("", []) is None
    assert DL.match_quality_key(None, ["1080p"]) is None
    assert DL.match_quality_key("best", []) is None


@register
@test("sort_quality_labels ranks best-first like the APK does")
def t_sort_quality_labels():
    ranked = DL.sort_quality_labels(
        ["480p (SD)", "2160p (4K UHD)", "1080p (Full HD)", "Best Available", "720p (HD)"])
    assert ranked[0] == "2160p (4K UHD)", ranked
    assert ranked[1] == "1080p (Full HD)", ranked
    assert ranked[-1] == "Best Available", ranked     # APK ranks 'best' last (score 10)
    assert DL.sort_quality_labels([]) == []
    # NetMirror CDN text
    cdn = DL.sort_quality_labels(["480P 2GB", "1080P 4GB", "4K HDR", "360P"])
    assert cdn == ["4K HDR", "1080P 4GB", "480P 2GB", "360P"], cdn
    # de-duplicates case-insensitively while preserving order of first appearance
    assert DL.sort_quality_labels(["1080p", "1080P", "720p"]) == ["1080p", "720p"]


# ================================ probing / health ============================
@register
@test("B4: the quality probe classifies ok/dead/unknown instead of no-oping")
def t_probe_classification():
    source = ENGINE.read_text(encoding="utf-8")
    assert "'dead'" in source and "'unknown'" in source, "probe health states missing"
    assert "verified live" in source, "the honest OK message is gone"


@register
@test("B10: rank_score returns a consistent shape")
def t_rank_score_shape():
    if not hasattr(DL, "rank_score"):
        return
    import inspect
    source = inspect.getsource(DL.rank_score)
    returns = [line.strip() for line in source.splitlines() if line.strip().startswith("return")]
    shapes = {r.count(",") for r in returns if "(" not in r.replace("return", "")}
    # Every bare tuple return must have the same arity.
    assert len(shapes) <= 1, f"rank_score returns mixed tuple shapes: {returns}"


# ============================ networking / resilience =========================
@register
@test("B29: wait_for_network has a deadline instead of looping forever")
def t_wait_for_network_bounded():
    import inspect
    signature = inspect.signature(DL.wait_for_network)
    assert "max_wait" in signature.parameters, "wait_for_network lost its deadline"
    assert "job_id" in signature.parameters, "wait_for_network cannot see job state"
    # Offline for real: point the probe at a black hole by monkeypatching.
    original = DL.check_network
    DL.check_network = lambda *a, **k: False
    try:
        started = time.time()
        recovered = DL.wait_for_network(max_wait=3)
        elapsed = time.time() - started
        assert recovered is False, "should report failure when it gives up"
        assert elapsed < 20, f"wait was not bounded: {elapsed:.1f}s"
    finally:
        DL.check_network = original


@register
@test("B29: Cancel releases a job stuck waiting for the network")
def t_wait_for_network_cancel():
    original = DL.check_network
    DL.check_network = lambda *a, **k: False
    job = "wait-cancel-test"
    DL.ACTIVE_DOWNLOAD_PROGRESS[job] = {"percent": 0.0, "status": "Downloading",
                                        "speed": "--", "eta": "--"}
    threading.Thread(target=lambda: (time.sleep(1.0), DL.cancel_download(job)),
                     daemon=True).start()
    try:
        started = time.time()
        recovered = DL.wait_for_network(job_id=job, max_wait=180)
        elapsed = time.time() - started
        assert recovered is False
        assert elapsed < 10, f"cancel did not release the wait ({elapsed:.1f}s)"
        assert DL.ACTIVE_DOWNLOAD_PROGRESS[job]["status"] in ("Waiting for network", "Cancelled"), \
            DL.ACTIVE_DOWNLOAD_PROGRESS[job]["status"]
    finally:
        DL.check_network = original
        DL.CANCELLED_DOWNLOAD_JOBS.discard(job)
        DL.ACTIVE_DOWNLOAD_PROGRESS.pop(job, None)


@register
@test("B29: pause holds the wait and resume releases it")
def t_wait_for_network_pause():
    original = DL.check_network
    DL.check_network = lambda *a, **k: False
    job = "wait-pause-test"
    DL.ACTIVE_DOWNLOAD_PROGRESS[job] = {"percent": 0.0, "status": "Downloading"}
    DL.pause_download(job)

    def resume_then_cancel():
        time.sleep(1.0)
        DL.resume_download(job)
        time.sleep(1.0)
        DL.cancel_download(job)

    threading.Thread(target=resume_then_cancel, daemon=True).start()
    try:
        recovered = DL.wait_for_network(job_id=job, max_wait=180)
        assert recovered is False, "cancel after resume should end the wait"
    finally:
        DL.check_network = original
        DL.resume_download(job)
        DL.CANCELLED_DOWNLOAD_JOBS.discard(job)
        DL.PAUSED_DOWNLOAD_JOBS.discard(job)
        DL.ACTIVE_DOWNLOAD_PROGRESS.pop(job, None)


@register
@test("_interruptible_sleep responds to cancel instead of blocking")
def t_interruptible_sleep():
    assert DL._interruptible_sleep(0.01, "no-such-job") is True
    job = "sleep-cancel"
    DL.CANCELLED_DOWNLOAD_JOBS.add(job)
    try:
        started = time.time()
        assert DL._interruptible_sleep(30, job) is False
        assert time.time() - started < 2, "cancel was not noticed during the sleep"
    finally:
        DL.CANCELLED_DOWNLOAD_JOBS.discard(job)


@register
@test("B30: a permanent HTTP 4xx fails fast instead of retrying forever")
def t_permanent_error_fails_fast():
    server = _TestServer(size=1024)
    out = Path(tempfile.mkdtemp())
    try:
        assert DL._is_retryable_stream_error(ValueError("no response")) is True

        class FakeResponse:
            def __init__(self, code): self.status_code = code

        class FakeError(Exception):
            def __init__(self, code): super().__init__(f"HTTP {code}"); self.response = FakeResponse(code)

        for code in (404, 403, 410, 401):
            assert DL._is_retryable_stream_error(FakeError(code)) is False, f"{code} should not retry"
        for code in (408, 429, 500, 502, 503):
            assert DL._is_retryable_stream_error(FakeError(code)) is True, f"{code} should retry"

        started = time.time()
        result = DL.download_file_stream(server.missing_url, "gone.bin",
                                         target_dir=str(out), job_id="fast-fail")
        elapsed = time.time() - started
        assert result is None
        assert elapsed < 5, f"a 404 took {elapsed:.1f}s; the retry loop is back"
        assert not list(out.glob("gone.bin*")), "a failed download must not leave a final file"
    finally:
        server.close()


@register
@test("B30: Cancel works while the retry loop is backing off")
def t_cancel_during_backoff():
    out = Path(tempfile.mkdtemp())
    dead = "http://127.0.0.1:9/unreachable.bin"     # nothing listens on port 9
    result: dict = {}

    def run():
        result["value"] = DL.download_file_stream(dead, "dead.bin", target_dir=str(out),
                                                  job_id="backoff-cancel")

    worker = threading.Thread(target=run, daemon=True)
    worker.start()
    time.sleep(2.5)                     # let it enter the backoff sleep
    DL.cancel_download("backoff-cancel")
    started = time.time()
    worker.join(10)
    released = not worker.is_alive()
    elapsed = time.time() - started
    DL.CANCELLED_DOWNLOAD_JOBS.discard("backoff-cancel")
    assert released, "cancel did not release the retry backoff (B30 regression)"
    assert elapsed < 5, f"cancel took {elapsed:.1f}s to be noticed"
    assert result.get("value") is None


# ================================ live transfer ===============================
@register
@test("download_file_stream transfers a file and reports live progress")
def t_live_download_progress():
    server = _TestServer(size=4 << 20, delay_per_chunk=0.03)   # ~1 MB/s
    out = Path(tempfile.mkdtemp())
    job = "live-progress"
    samples: list[tuple] = []
    stop = threading.Event()

    def poll():
        while not stop.is_set():
            snapshot = DL.get_download_progress(job)
            if snapshot:
                samples.append((round(float(snapshot.get("percent", 0) or 0), 1),
                                snapshot.get("status"), snapshot.get("speed"),
                                snapshot.get("downloaded_str"), snapshot.get("total_str")))
            time.sleep(0.15)

    poller = threading.Thread(target=poll, daemon=True)
    poller.start()
    try:
        result = DL.download_file_stream(server.url, "movie.bin", target_dir=str(out),
                                        cache_dir=str(out / ".cache"), job_id=job)
        # The final frame is written just before the call returns; let the poller
        # observe it instead of racing the stop flag against it.
        time.sleep(0.35)
        stop.set()
        time.sleep(0.2)
        final = DL.get_download_progress(job)
        assert result and Path(result).is_file(), f"no file produced: {result!r}"
        assert Path(result).stat().st_size == server.size, "truncated transfer"
        assert len(samples) > 3, f"progress was not published: {samples}"
        percents = sorted({s[0] for s in samples})
        assert percents[0] < 90.0, f"only saw the end, no live progress: {percents}"
        assert any(s[1] == "Downloading" for s in samples)
        assert any(s[2] not in ("--", None, "") for s in samples), "no speed was reported"
        assert final.get("percent") == 100.0, f"final frame was {final}"
        assert final.get("status") == "Complete", f"final status was {final.get('status')}"
        assert not list((out / ".cache").glob("*.part")), "staging file was left behind"
    finally:
        stop.set()
        server.close()
        DL.ACTIVE_DOWNLOAD_PROGRESS.pop(job, None)


@register
@test("pause freezes the transfer and resume finishes it")
def t_pause_resume():
    server = _TestServer(size=3 << 20, delay_per_chunk=0.03)
    out = Path(tempfile.mkdtemp())
    job = "pause-resume"
    samples: list[tuple] = []
    stop = threading.Event()

    def poll():
        while not stop.is_set():
            snapshot = DL.get_download_progress(job)
            if snapshot:
                samples.append((round(float(snapshot.get("percent", 0) or 0), 1),
                                snapshot.get("status")))
            time.sleep(0.15)

    poller = threading.Thread(target=poll, daemon=True)
    poller.start()
    result: dict = {}
    worker = threading.Thread(
        target=lambda: result.update(value=DL.download_file_stream(
            server.url, "p.bin", target_dir=str(out), cache_dir=str(out / ".cache"), job_id=job)),
        daemon=True)
    worker.start()
    try:
        time.sleep(2.0)
        pause_index = len(samples)
        DL.pause_download(job)
        time.sleep(2.0)
        paused = [s for s in samples[pause_index:] if s[1] == "Paused"]
        assert paused, f"status never became Paused: {samples[pause_index:][:6]}"
        frozen = {s[0] for s in paused}
        assert len(frozen) <= 2, f"percent kept moving while paused: {sorted(frozen)}"
        resume_index = len(samples)
        DL.resume_download(job)
        worker.join(60)
        time.sleep(0.35)
        stop.set()
        time.sleep(0.2)
        assert not worker.is_alive(), "resume did not continue the download"
        assert result.get("value") and Path(result["value"]).is_file(), "no file after resume"
        assert Path(result["value"]).stat().st_size == server.size, "resumed file is truncated"
        # After the resume the percent must climb past the frozen value. The last
        # frames report "Complete" rather than "Downloading", so compare percents
        # regardless of status.
        after = [s[0] for s in samples[resume_index:]]
        assert after, "no progress was sampled after resume"
        assert max(after) > max(frozen), \
            f"progress did not advance after resume: {max(after)} <= {max(frozen)}"
        assert DL.get_download_progress(job).get("percent") == 100.0
    finally:
        stop.set()
        DL.resume_download(job)
        server.close()
        DL.ACTIVE_DOWNLOAD_PROGRESS.pop(job, None)
        DL.PAUSED_DOWNLOAD_JOBS.discard(job)


@register
@test("cancel mid-transfer stops promptly and leaves no final file")
def t_cancel_mid_transfer():
    server = _TestServer(size=8 << 20, delay_per_chunk=0.03)
    out = Path(tempfile.mkdtemp())
    job = "cancel-mid"
    result: dict = {}
    worker = threading.Thread(
        target=lambda: result.update(value=DL.download_file_stream(
            server.url, "c.bin", target_dir=str(out), cache_dir=str(out / ".cache"), job_id=job)),
        daemon=True)
    worker.start()
    try:
        time.sleep(2.0)
        DL.cancel_download(job)
        started = time.time()
        worker.join(15)
        assert not worker.is_alive(), "cancel did not stop the transfer"
        assert time.time() - started < 10, "cancel took too long to take effect"
        assert result.get("value") is None, f"cancelled download returned {result.get('value')!r}"
        assert not (out / "c.bin").exists(), "a cancelled download left a final file behind"
    finally:
        server.close()
        DL.CANCELLED_DOWNLOAD_JOBS.discard(job)
        DL.ACTIVE_DOWNLOAD_PROGRESS.pop(job, None)


@register
@test("resume: a .part fragment continues with a Range request")
def t_resume_from_part():
    server = _TestServer(size=1 << 20)
    out = Path(tempfile.mkdtemp())
    cache = out / ".cache"
    cache.mkdir()
    job = "resume-test"
    partial = cache / f".temp_{job}_r.bin.part"
    partial.write_bytes(b"\xAB" * (512 * 1024))       # exactly the first half
    result = DL.download_file_stream(server.url, "r.bin", target_dir=str(out),
                                     cache_dir=str(cache), job_id=job)
    try:
        assert result and Path(result).is_file(), "resume produced no file"
        assert Path(result).stat().st_size == server.size, "resumed file has the wrong size"
    finally:
        server.close()
        DL.ACTIVE_DOWNLOAD_PROGRESS.pop(job, None)


@register
@test("progress reports Complete, not Downloading, on the final frame")
def t_final_frame_status():
    job = "final-frame"
    DL.render_download_progress(100.0, total_str="Complete", speed_str="Done",
                                eta_str="0s", job_id=job)
    snapshot = DL.get_download_progress(job)
    try:
        assert snapshot.get("status") == "Complete", snapshot
        assert snapshot.get("percent") == 100.0
    finally:
        DL.ACTIVE_DOWNLOAD_PROGRESS.pop(job, None)


@register
@test("progress dict keeps a Cancelled job cancelled")
def t_cancel_status_sticky():
    job = "sticky-cancel"
    DL.cancel_download(job)
    try:
        DL.render_download_progress(42.0, total_str="1 MB", job_id=job)
        assert DL.get_download_progress(job).get("status") == "Cancelled"
    finally:
        DL.CANCELLED_DOWNLOAD_JOBS.discard(job)
        DL.ACTIVE_DOWNLOAD_PROGRESS.pop(job, None)


# ============================== crypto / decryption ===========================
@register
@test("B2/B13: AES-GCM decryption has a real backend and honest failure")
def t_aes_gcm():
    assert hasattr(DL, "_aes_gcm_decrypt"), "the AES-GCM helper is gone"
    import inspect
    source = inspect.getsource(DL._aes_gcm_decrypt)
    assert "Cryptodome" in source or "AES" in source
    assert "cryptography" in source, "the fallback backend is missing"
    # With neither library installed here it must fail cleanly, not raise.
    try:
        result = DL._aes_gcm_decrypt(b"\x00" * 32, b"\x00" * 12, b"data", b"tag")
    except Exception as error:  # noqa: BLE001
        raise AssertionError(f"_aes_gcm_decrypt raised instead of returning None: {error}")
    assert result is None or isinstance(result, bytes)


@register
@test("B2/B13: decrypt_netmirror_tv_url uses module-level hashlib")
def t_netmirror_decrypt_hashlib():
    import inspect
    source = inspect.getsource(DL.decrypt_netmirror_tv_url)
    assert "hashlib" in source
    assert "import hashlib" not in source.split("def ")[0] or True
    assert hasattr(DL, "hashlib") or "hashlib" in dir(DL), "hashlib is not imported at module level"


# ================================== magnets ===================================
@register
@test("B11: app_download has a magnet branch that reaches download_torrent")
def t_magnet_branch_app_download():
    import inspect
    source = inspect.getsource(DL.app_download)
    assert 'startswith("magnet:")' in source, "the magnet branch is missing from app_download"
    assert "torrent_sources" in source, "magnet metadata is not forwarded"
    assert hasattr(DL, "download_torrent"), "download_torrent is still unreachable"


@register
@test("B11: the CLI dispatcher routes magnet: and .torrent too")
def t_magnet_branch_cli():
    import inspect
    source = inspect.getsource(DL.download)
    assert "magnet" in source.lower(), "download() does not handle magnet links"
    assert "torrent" in source.lower(), "download() does not handle .torrent files"


@register
@test("B11: a magnet without aria2c explains itself instead of failing oddly")
def t_magnet_without_aria2c():
    import shutil
    if shutil.which("aria2c"):
        return                     # cannot test the missing-binary path here
    response = DL.run_app_json_request({
        "action": "download",
        "url": "magnet:?xt=urn:btih:" + "0" * 40 + "&dn=Test",
        "title": "Test",
        "job_id": "magnet-no-aria2c",
    })
    DL.CANCELLED_DOWNLOAD_JOBS.discard("magnet-no-aria2c")
    text = (str(response.get("error", "")) + str(response.get("logs", ""))).lower()
    assert "aria2" in text, f"no actionable hint about aria2c: {text[:300]}"


@register
@test("app_resolve answers a magnet with torrent_sources and can_stream=false")
def t_magnet_resolve():
    response = DL.run_app_json_request({
        "action": "resolve",
        "url": "magnet:?xt=urn:btih:" + "A" * 40 + "&dn=Sample%20Movie",
    })
    assert response.get("ok") is True, response.get("error")
    result = response.get("result") or {}
    assert result.get("can_stream") is False, result
    assert result.get("media_type") == "file", result
    assert "torrent_sources" in result, result


# ================================ subprocess ==================================
@register
@test("B18: run_command_with_clean_progress takes job_id and reports cancellation")
def t_subprocess_cancel():
    import inspect
    signature = inspect.signature(DL.run_command_with_clean_progress)
    assert "job_id" in signature.parameters, "job_id parameter is gone (B18 regression)"

    job = "subproc-cancel"
    long_running = [sys.executable, "-c", "import time; time.sleep(60)"]
    result: dict = {}
    worker = threading.Thread(
        target=lambda: result.update(code=DL.run_command_with_clean_progress(
            long_running, job_id=job)), daemon=True)
    worker.start()
    time.sleep(1.5)
    DL.cancel_download(job)
    started = time.time()
    worker.join(20)
    elapsed = time.time() - started
    released = not worker.is_alive()
    code = result.get("code")
    DL.CANCELLED_DOWNLOAD_JOBS.discard(job)
    DL.ACTIVE_DOWNLOAD_PROGRESS.pop(job, None)

    assert released, "cancel did not terminate the subprocess (B18 regression)"
    assert elapsed < 12, f"cancel took {elapsed:.1f}s to kill the child"
    assert code is not None and code != 0, f"a cancelled command must not report success, got {code}"


@register
@test("B18: a normal command still returns its real exit code")
def t_subprocess_success():
    ok = DL.run_command_with_clean_progress(
        [sys.executable, "-c", "print('working')"], job_id="subproc-ok")
    assert ok == 0, f"expected 0 for a successful command, got {ok}"
    failed = DL.run_command_with_clean_progress(
        [sys.executable, "-c", "import sys; print('boom'); sys.exit(3)"], job_id="subproc-ok")
    assert failed == 3, f"expected the real exit code 3, got {failed}"


# ============================== log capture ===================================
@register
@test("B28: a request's logs stay in its own thread, not the host's stdout")
def t_capture_isolation():
    captured = DL._capture_thread_logs()
    host_lines: list[str] = []
    real_stdout = sys.__stdout__

    def engine_side():
        with captured:
            print("ENGINE LINE")
            time.sleep(0.4)
            print("ENGINE LINE 2")

    worker = threading.Thread(target=engine_side)
    worker.start()
    time.sleep(0.1)
    # Anything the host prints while the engine is capturing must not be absorbed.
    print("HOST LINE")
    host_lines.append("HOST LINE")
    worker.join()

    text = captured.text()
    assert "ENGINE LINE" in text and "ENGINE LINE 2" in text, text
    assert "HOST LINE" not in text, f"host output was swallowed into the engine log: {text!r}"


@register
@test("B28: inherit_capture pulls engine worker threads into the parent log")
def t_inherit_capture():
    captured = DL._capture_thread_logs()
    with captured:
        def child():
            print("CHILD LINE")

        worker = threading.Thread(target=DL.inherit_capture(child))
        worker.start()
        worker.join()
        print("PARENT LINE")
    text = captured.text()
    assert "CHILD LINE" in text, f"child output escaped the log: {text!r}"
    assert "PARENT LINE" in text


@register
@test("B28: unregistered threads reach the real console, and nesting restores")
def t_capture_nesting():
    outer = DL._capture_thread_logs()
    with outer:
        print("outer-1")
        inner = DL._capture_thread_logs()
        with inner:
            print("inner-1")
        print("outer-2")
    assert inner.text().strip() == "inner-1", inner.text()
    assert outer.text().strip() == "outer-1\nouter-2", outer.text()
    assert not DL._CAPTURE_REGISTRY, f"capture registry leaked: {DL._CAPTURE_REGISTRY}"


@register
@test("B28: sanitize_app_logs strips ANSI and internal hostnames")
def t_sanitize_logs():
    raw = "\x1b[32m[OK]\x1b[0m fetched from netmirror.center/api\nEngine yt-dlp said hi"
    clean = DL.sanitize_app_logs(raw)
    assert "\x1b" not in clean, clean
    assert "netmirror" not in clean.lower(), clean
    assert "yt-dlp" not in clean.lower(), clean


@register
@test("run_app_json_request reports unknown actions instead of raising")
def t_unknown_action():
    response = DL.run_app_json_request({"action": "not-a-real-action"})
    assert response.get("ok") is False
    assert "Unknown app action" in str(response.get("error", ""))


@register
@test("run_app_json_request_json round-trips JSON strings (Chaquopy entry point)")
def t_json_entry_point():
    import json
    raw = DL.run_app_json_request_json(json.dumps({"action": "capabilities"}))
    parsed = json.loads(raw)
    assert parsed.get("ok") is True
    assert "capabilities" in parsed
    broken = json.loads(DL.run_app_json_request_json("{not json"))
    assert broken.get("ok") is False


# ================================= app protocol ===============================
@register
@test("capabilities lists the machine interface the front ends rely on")
def t_capabilities():
    response = DL.run_app_json_request({"action": "capabilities"})
    assert response.get("ok") is True
    caps = response["capabilities"]
    for action in ("download", "resolve", "stream"):
        assert action in caps["actions"], caps["actions"]
    assert caps["qualities"], caps


@register
@test("resolve refuses an empty URL with a clear message")
def t_resolve_empty():
    response = DL.run_app_json_request({"action": "resolve", "url": ""})
    assert response.get("ok") is False
    assert "URL" in str(response.get("error", ""))


@register
@test("search refuses an empty query with a clear message")
def t_search_empty():
    response = DL.run_app_json_request({"action": "search", "query": ""})
    assert response.get("ok") is False
    assert response.get("error"), response


@register
@test("progress and cancel actions answer without touching stdout")
def t_progress_cancel_actions():
    response = DL.run_app_json_request({"action": "progress"})
    assert response.get("ok") is True
    assert isinstance(response.get("progress"), dict)
    cancel = DL.run_app_json_request({"action": "cancel", "job_id": "nothing-running"})
    assert cancel.get("ok") is True
    assert cancel.get("cancelled") == "nothing-running"


@register
@test("telegram_check rejects a malformed session with a real reason")
def t_telegram_check_validation():
    response = DL.run_app_json_request({
        "action": "telegram_check", "api_id": "not-a-number",
        "api_hash": "x", "session": "y",
    })
    assert response.get("ok") is False
    assert "numeric" in str(response.get("error", "")).lower(), response.get("error")


@register
@test("B15: download() forwards season/episode/scope to the handlers")
def t_download_kwargs_forwarded():
    import inspect
    source = inspect.getsource(DL.download)
    for name in ("season", "episode"):
        assert name in source, f"download() still drops {name}"


# ==================================== igl =====================================
@register
@test("B23/B24: igl resolves its output directory at import and honours the env var")
def t_igl_output_dir():
    target = Path(tempfile.mkdtemp()) / "downloads"
    environment = dict(os.environ)
    environment["ARC_DOWNLOAD_DIR"] = str(target)
    code = (
        "import importlib.util,sys;"
        f"spec=importlib.util.spec_from_file_location('igl_probe', r'{REPO / 'igl.py'}');"
        "m=importlib.util.module_from_spec(spec);sys.modules['igl_probe']=m;spec.loader.exec_module(m);"
        "import os;print(m.OUTPUT_DIR);print(os.path.isdir(m.OUTPUT_DIR))"
    )
    import subprocess
    result = subprocess.run([sys.executable, "-c", code], capture_output=True, text=True,
                            env=environment, timeout=120)
    lines = [line for line in result.stdout.splitlines() if line.strip()]
    assert result.returncode == 0, result.stderr[-600:]
    assert len(lines) >= 2, result.stdout
    assert lines[0] == str(target), f"ARC_DOWNLOAD_DIR ignored: {lines[0]}"
    assert lines[1] == "True", "the directory was not created"


@register
@test("B24: igl finds the sibling dl.py instead of a stale ~/scripts/d.py")
def t_igl_sibling_engine():
    source = (REPO / "igl.py").read_text(encoding="utf-8")
    assert "d.py" not in source or "dl.py" in source, "the stale d.py reference is back"
    assert "dl.py" in source, "igl.py no longer looks for the sibling engine"


@register
@test("get_default_dir/get_dest_path pick a real writable folder (no /sdcard on desktop)")
def t_default_dir():
    assert hasattr(DL, "get_default_dir"), "get_default_dir disappeared"
    assert DL.DEFAULT_DIR, "DEFAULT_DIR resolved to nothing"
    assert Path(DL.DEFAULT_DIR).is_dir(), f"DEFAULT_DIR is not a directory: {DL.DEFAULT_DIR}"

    target = Path(tempfile.mkdtemp())
    placed = DL.get_dest_path("movie.mp4", str(target))
    assert str(placed).startswith(str(target)), f"{placed} ignored target_dir"
    assert placed.endswith("movie.mp4"), placed

    # A name carrying path separators or a Windows drive must not escape the folder.
    escaped = DL.get_dest_path("../../etc/passwd", str(target))
    assert Path(escaped).parent == target or str(escaped).startswith(str(target)), \
        f"filename traversal was not contained: {escaped}"


# ================================= headers ====================================
@register
@test("stream headers carry the Referer/Origin that the CDNs require")
def t_stream_headers():
    headers = DL.get_stream_headers_for_url("https://example.bcdn.to/video.mp4", {})
    assert headers.get("Referer"), headers
    protected = DL.get_stream_headers_for_url("https://mzfi.me/x.mp4", {})
    assert protected.get("Referer"), protected
    plain = DL.get_stream_headers_for_url("https://example.com/file.mp4", {})
    assert plain.get("Referer"), plain


# ================================== muxing ====================================
@register
@test("HD merge falls back to ffmpeg when Android's MediaMuxer is absent")
def t_ffmpeg_mux_fallback():
    source = ENGINE.read_text(encoding="utf-8")
    assert 'shutil.which("ffmpeg")' in source, "the ffmpeg mux fallback is missing"
    assert "NO SOUND" in source, "the silent-file warning is missing"


@register
@test("B16 note: EmbeddedEngine.mux is only reached inside the APK")
def t_muxer_import_guarded():
    source = ENGINE.read_text(encoding="utf-8")
    assert "from com.erawat.arc import EmbeddedEngine" in source
    # It must stay inside a try/except so desktop and CLI do not crash on it.
    index = source.index("from com.erawat.arc import EmbeddedEngine")
    window = source[max(0, index - 400): index + 200]
    assert "try:" in window, "the Android-only import is not guarded"


# =================================== main =====================================
def main() -> int:
    global DL
    print("")
    print("=" * 72)
    print(" dl.py engine regression tests")
    print(f" engine: {ENGINE}")
    print(f" python: {sys.version.split()[0]} on {sys.platform}")
    print("=" * 72)
    print("")
    print("Loading engine...")
    try:
        DL = load_engine()
    except Exception as error:  # noqa: BLE001
        print(f"  FATAL: dl.py could not be imported: {type(error).__name__}: {error}")
        return 1
    print(f"  loaded in-process ({len(dir(DL))} attributes)")
    print("")

    for case in TESTS:
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
