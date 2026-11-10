import importlib.util
import json
import os
import shutil
import subprocess
import sys
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError


REPO_ROOT = Path(__file__).resolve().parent
GITHUB_OWNER = "Erawat4l"
GITHUB_REPO = "universal-downloader"
GITHUB_BRANCH = "main"
GITHUB_API = f"https://api.github.com/repos/{GITHUB_OWNER}/{GITHUB_REPO}"

CAPABILITIES = {
    "inputs": ["URL", "movie / series / video / song search"],
    "sources": [
        "direct files",
        "YouTube and supported video platforms",
        "Telegram media",
        "SelfStudys PDFs",
        "StudiesToday PDFs",
        "Studocu documents",
        "NetMirror movies and series",
    ],
    "qualities": ["best", "2160p", "1440p", "1080p", "720p", "480p", "360p", "240p", "144p"],
    "audio_formats": ["mp3", "m4a"],
    "actions": ["download", "copy direct stream link", "stream in VLC", "stream and download"],
    "resilience": ["resume", "retry", "network recovery", "progress", "speed", "ETA"],
}


def git_output(*args):
    try:
        result = subprocess.run(
            ["git", *args],
            cwd=REPO_ROOT,
            capture_output=True,
            text=True,
            timeout=8,
            check=True,
        )
        return result.stdout.strip()
    except (OSError, subprocess.SubprocessError):
        return ""


def github_json(path):
    request = Request(
        f"{GITHUB_API}{path}",
        headers={
            "Accept": "application/vnd.github+json",
            "User-Agent": "universal-downloader-ui-preview",
        },
    )
    try:
        with urlopen(request, timeout=10) as response:
            return json.loads(response.read().decode("utf-8")), None
    except HTTPError as error:
        if error.code == 404:
            return None, "not_found"
        return None, f"github_http_{error.code}"
    except (URLError, TimeoutError, OSError, json.JSONDecodeError) as error:
        return None, str(error)


def dependency_status():
    modules = {
        "requests": "requests",
        "beautifulsoup": "bs4",
        "yt_dlp": "yt_dlp",
        "pyrogram": "pyrogram",
    }
    result = {name: importlib.util.find_spec(module) is not None for name, module in modules.items()}
    result["ffmpeg"] = shutil.which("ffmpeg") is not None
    result["aria2c"] = shutil.which("aria2c") is not None
    return result


def engine_status():
    cli_path = REPO_ROOT / "dl.py"
    deps = dependency_status()
    return {
        "name": "dl.py",
        "path": str(cli_path),
        "exists": cli_path.is_file(),
        "branch": git_output("branch", "--show-current") or "unknown",
        "commit": git_output("rev-parse", "HEAD"),
        "capabilities": CAPABILITIES,
        "dependencies": deps,
        "runtime_ready": cli_path.is_file() and all(deps.values()),
        "mode": "repo-backed UI bridge",
    }


def update_status():
    local_sha = git_output("rev-parse", "HEAD")
    local_branch = git_output("branch", "--show-current") or "unknown"
    remote_commit, commit_error = github_json(f"/commits/{GITHUB_BRANCH}")
    release, release_error = github_json("/releases/latest")

    remote_sha = (remote_commit or {}).get("sha", "")
    # splitlines() on an empty string returns [], so [0] raised IndexError and
    # /api/update/check died with an empty reply whenever GitHub 404'd.
    message = (((remote_commit or {}).get("commit") or {}).get("message", "") or "").splitlines()
    message = message[0] if message else ""
    commit_url = (remote_commit or {}).get("html_url", f"https://github.com/{GITHUB_OWNER}/{GITHUB_REPO}/commits/{GITHUB_BRANCH}")

    apk_asset = None
    if release:
        for asset in release.get("assets", []):
            name = str(asset.get("name", "")).lower()
            if name.endswith(".apk"):
                apk_asset = {
                    "name": asset.get("name"),
                    "url": asset.get("browser_download_url"),
                    "size": asset.get("size"),
                }
                break

    source_update_available = bool(local_sha and remote_sha and local_sha != remote_sha)
    release_tag = (release or {}).get("tag_name")
    return {
        "repository": f"{GITHUB_OWNER}/{GITHUB_REPO}",
        "branch": GITHUB_BRANCH,
        "local_branch": local_branch,
        "local_commit": local_sha,
        "remote_commit": remote_sha,
        "remote_short_commit": remote_sha[:7],
        "commit_message": message or "No commit message available.",
        "commit_url": commit_url,
        "source_update_available": source_update_available,
        "release": {
            "tag": release_tag,
            "name": (release or {}).get("name"),
            "url": (release or {}).get("html_url"),
            "apk": apk_asset,
            "exists": bool(release),
        },
        "app_update_available": bool(apk_asset),
        "github_error": commit_error or release_error,
        "checked_at": __import__("datetime").datetime.now(__import__("datetime").timezone.utc).isoformat(),
        "update_note": (
            "A signed APK release can update the installed app. Source commits can update the CLI engine, "
            "but cannot replace an installed APK until a release asset is published."
        ),
    }


class PrototypeHandler(SimpleHTTPRequestHandler):
    """Serve the prototype at / and expose read-only repo/update status APIs."""

    def _route_root(self):
        parsed = urlsplit(self.path)
        if parsed.path in ("", "/", "/index.html"):
            self.path = "/prototype.html"

    def _send_json(self, payload, status=200):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        path = urlsplit(self.path).path
        if path == "/api/engine/status":
            self._send_json(engine_status())
            return
        if path == "/api/update/check":
            self._send_json(update_status())
            return
        self._route_root()
        super().do_GET()

    def do_POST(self):
        path = urlsplit(self.path).path
        if path == "/api/update/apply":
            status = update_status()
            if status.get("release", {}).get("apk"):
                self._send_json({
                    "ok": True,
                    "mode": "release_download",
                    "message": "A signed APK release is available. Open the release to install it.",
                    "download_url": status["release"]["apk"]["url"],
                    "release": status["release"],
                })
                return
            self._send_json({
                "ok": False,
                "mode": "source_only",
                "message": "The GitHub source is checked, but this repository has no signed APK release yet. Publish an APK release to enable one-tap app updates.",
                "source_update_available": status.get("source_update_available", False),
                "status": status,
            }, status=409)
            return
        self._send_json({"ok": False, "message": "Unknown prototype action."}, status=404)

    def do_HEAD(self):
        self._route_root()
        super().do_HEAD()

    def end_headers(self):
        # Always show the latest prototype after a UI change instead of serving a cached HTML shell.
        path = urlsplit(self.path).path
        if path in ("", "/", "/index.html") or path.endswith(".html"):
            self.send_header("Cache-Control", "no-store, max-age=0")
        super().end_headers()

    def log_message(self, format_string, *args):
        # Keep the preview log useful without echoing request bodies or credentials.
        super().log_message(format_string, *args)


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", 8001), PrototypeHandler)
    print("Downloader prototype listening on 0.0.0.0:8001", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
