#!/usr/bin/env python3
"""
Universal Downloader CLI (d.py / dl)
Supports YouTube/Video quality selector CLI (yt-dlp), Direct Files, Link Bypassing (MediaFire, Terabox, etc.), SelfStudys, StudiesToday, Studocu.
Can sync downloads to /sdcard/Download via ADB when Android storage permissions require it.
"""

import sys
import os
import re
import subprocess
import urllib.parse
import base64
import io
import contextlib
import shutil
import requests
import html
from bs4 import BeautifulSoup
import socket
import http.server
import socketserver
import threading
import concurrent.futures
import asyncio
import warnings

warnings.filterwarnings("ignore", category=ResourceWarning)
warnings.filterwarnings("ignore", category=DeprecationWarning)
warnings.filterwarnings("ignore", message=".*unclosed.*")
warnings.filterwarnings("ignore", message=".*Task was destroyed.*")
warnings.filterwarnings("ignore", message=".*loop is closed.*")

class AnyThreadEventLoopPolicy(asyncio.DefaultEventLoopPolicy):
    def get_event_loop(self):
        try:
            return super().get_event_loop()
        except RuntimeError:
            loop = self.new_event_loop()
            self.set_event_loop(loop)
            return loop

try:
    asyncio.set_event_loop_policy(AnyThreadEventLoopPolicy())
except Exception:
    pass

# Lazy-loaded modules for instant CLI startup (0.2s launch)
_YTDLP_MODULE = None
_YTDLP_PATCHED = False

def _patch_ytdlp_muxer():
    global _YTDLP_PATCHED
    if _YTDLP_PATCHED:
        return
    try:
        from yt_dlp.postprocessor.ffmpeg import FFmpegPostProcessor, FFmpegMergerPP
        from yt_dlp.utils import prepend_extension

        FFmpegPostProcessor.available = property(lambda self: True)
        FFmpegPostProcessor.basename = property(lambda self: 'ffmpeg')

        orig_determine = FFmpegPostProcessor._determine_executables
        def custom_determine(self):
            res = orig_determine(self)
            if not res or 'ffmpeg' not in res or not shutil.which('ffmpeg'):
                res = {'ffmpeg': 'native_muxer', 'ffprobe': 'native_muxer'}
            return res
        FFmpegPostProcessor._determine_executables = custom_determine

        orig_merger_run = FFmpegMergerPP.run
        def custom_merger_run(self, info):
            if shutil.which("ffmpeg"):
                try:
                    return orig_merger_run(self, info)
                except Exception as e:
                    print(f"[WARN] FFmpeg merge failed: {e}, falling back to native muxer")
            files = info.get('__files_to_merge', [])
            if len(files) >= 2:
                target_out = info.get('filepath') or ''
                req = info.get('requested_formats', [])
                video_files = []
                audio_files = []
                for idx, f in enumerate(files):
                    f_ext = os.path.splitext(f)[1].lower()
                    fmt = req[idx] if idx < len(req) else {}
                    if fmt.get('vcodec') and fmt.get('vcodec') != 'none':
                        video_files.append(f)
                    elif f_ext in ('.m4a', '.mp3', '.aac', '.opus', '.ogg', '.flac', '.wav'):
                        audio_files.append(f)
                    elif not video_files:
                        video_files.append(f)
                    else:
                        audio_files.append(f)

                if not video_files and files:
                    video_files = [files[0]]
                    audio_files = files[1:]
                elif len(video_files) > 1:
                    audio_files.extend(video_files[1:])
                    video_files = [video_files[0]]

                v_file = video_files[0] if video_files else None
                if v_file and audio_files:
                    temp_out = prepend_extension(target_out, 'muxed')
                    if os.path.exists(temp_out):
                        try: os.remove(temp_out)
                        except Exception: pass

                    success = False
                    if len(audio_files) == 1:
                        success = mux_video_and_audio(v_file, audio_files[0], temp_out)
                    else:
                        m4a_audios = [a for a in audio_files if os.path.splitext(a)[1].lower() in ('.m4a', '.aac', '.mp4')]
                        audios_to_mux = m4a_audios if m4a_audios else audio_files
                        success = mux_multiple_audio_tracks(v_file, audios_to_mux, temp_out)
                        if not success:
                            first_audio = audios_to_mux[0] if audios_to_mux else audio_files[0]
                            success = mux_video_and_audio(v_file, first_audio, temp_out)

                    if success and os.path.exists(temp_out) and os.path.getsize(temp_out) > 0:
                        if os.path.exists(target_out):
                            try: os.remove(target_out)
                            except Exception: pass
                        os.rename(temp_out, target_out)
                        for f in files:
                            if f != target_out and os.path.exists(f):
                                try: os.remove(f)
                                except Exception: pass
                        return files, info
                    else:
                        print(f"[ERROR] Native muxer failed to mux {files}")
                        raise RuntimeError(f"Native muxer could not combine video and audio streams: {files}")
            return orig_merger_run(self, info)
        FFmpegMergerPP.run = custom_merger_run
        _YTDLP_PATCHED = True
    except Exception as e:
        print(f"[WARN] Failed to patch yt-dlp muxer: {e}")

def load_ytdlp():
    global _YTDLP_MODULE
    if _YTDLP_MODULE is None:
        try:
            import yt_dlp as _mod
            _YTDLP_MODULE = _mod
            _patch_ytdlp_muxer()
        except ImportError:
            _YTDLP_MODULE = False
    return _YTDLP_MODULE if _YTDLP_MODULE is not False else None

class _LazyYtDlp:
    def __getattr__(self, name):
        mod = load_ytdlp()
        if mod is None:
            raise ImportError("yt-dlp is not installed or available")
        return getattr(mod, name)

    def __bool__(self):
        return load_ytdlp() is not None

yt_dlp = _LazyYtDlp()
HAS_YTDLP = yt_dlp

_DIRECT_LINK_GEN = None

def get_bypasser():
    global _DIRECT_LINK_GEN
    if _DIRECT_LINK_GEN is None:
        try:
            SYS_PATH_BOT = os.path.expanduser("~/Projects/Link-Bypasser-Bot")
            if os.path.exists(SYS_PATH_BOT) and SYS_PATH_BOT not in sys.path:
                sys.path.append(SYS_PATH_BOT)
            from ddl import direct_link_generator as _dlg
            _DIRECT_LINK_GEN = _dlg
        except Exception:
            _DIRECT_LINK_GEN = False
    return _DIRECT_LINK_GEN if _DIRECT_LINK_GEN is not False else None

class _LazyBypasser:
    def __call__(self, *args, **kwargs):
        gen = get_bypasser()
        if not gen:
            raise ImportError("Direct link bypasser module is not available")
        return gen(*args, **kwargs)

    def __bool__(self):
        return get_bypasser() is not None

direct_link_generator = _LazyBypasser()
HAS_BYPASSER = direct_link_generator

HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': '*/*',
    'Accept-Language': 'en-US,en;q=0.9',
}

def is_writeable(path):
    try:
        os.makedirs(path, exist_ok=True)
        test_file = os.path.join(path, ".test_write_perm")
        with open(test_file, "w") as f:
            f.write("ok")
        os.remove(test_file)
        return True
    except Exception:
        return False

def get_default_dir():
    if is_writeable("/sdcard/Download"):
        return "/sdcard/Download"
    user_downloads = os.path.expanduser("~/Downloads")
    os.makedirs(user_downloads, exist_ok=True)
    return user_downloads

DEFAULT_DIR = get_default_dir()

def sync_to_sdcard_if_needed(file_path):
    """If saved outside /sdcard/Download, attempt pushing to /sdcard/Download via ADB if available in Termux CLI."""
    if not file_path:
        return
    clean_p = os.path.abspath(file_path)
    # Never push if already on sdcard or primary Android storage
    if clean_p.startswith("/sdcard") or clean_p.startswith("/storage/emulated/0") or "com.erawat.arc" in clean_p:
        return
    # Only for standalone CLI outside Android app
    try:
        import com.chaquo.python
        return
    except Exception:
        pass
    filename = os.path.basename(file_path)
    dest_sdcard = f"/sdcard/Download/{filename}"
    try:
        res = subprocess.run(["adb", "devices"], capture_output=True, text=True, timeout=2)
        if "device" in res.stdout and ("emulator" in res.stdout or len(res.stdout.strip().splitlines()) > 1):
            push_res = subprocess.run(["adb", "push", file_path, dest_sdcard], capture_output=True, text=True, timeout=30)
            if push_res.returncode == 0:
                print(f"[OK] Synced to: {dest_sdcard}")
    except Exception:
        pass

SERVER_STARTED = False
def start_local_http_server():
    global SERVER_STARTED
    if SERVER_STARTED:
        return
    import threading, http.server, socketserver
    def run_server():
        class QuietHandler(http.server.SimpleHTTPRequestHandler):
            def __init__(self, *args, **kwargs):
                super().__init__(*args, directory="/sdcard/Download", **kwargs)
            def log_message(self, format, *args):
                pass
        try:
            with socketserver.TCPServer(("", 8080), QuietHandler) as httpd:
                httpd.serve_forever()
        except Exception:
            pass
    t = threading.Thread(target=run_server, daemon=True)
    t.start()
    SERVER_STARTED = True

def print_adm_link(file_path):
    start_local_http_server()
    if not file_path or not os.path.exists(file_path): return
    basename = os.path.basename(file_path)
    encoded = urllib.parse.quote(basename)
    local_url = f"http://localhost:8080/{encoded}"
    my_ip = "127.0.0.1"
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("1.1.1.1", 80))
        my_ip = s.getsockname()[0]
        s.close()
    except Exception:
        pass
    lan_url = f"http://{my_ip}:8080/{encoded}"
    
    print("\n" + "="*65)
    print("  DIRECT LINK FOR ADM (ADVANCED DOWNLOAD MANAGER):")
    print("="*65)
    print(f"  Copy & Paste into ADM App: {local_url}")
    print(f"  Wi-Fi LAN Download Link:  {lan_url}")
    print("="*65 + "\n")

def get_stream_headers_for_url(url, base_headers=None):
    """Normalize required headers for stream hosts (e.g. mzfi.me referer for hakunaymatata)."""
    headers = {
        'User-Agent': HEADERS.get('User-Agent', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36')
    }
    if base_headers:
        headers.update(base_headers)
    try:
        parsed = urllib.parse.urlparse(url)
        hostname = (parsed.hostname or '').lower()
        if 'okcdn' in hostname:
            headers.pop('Origin', None)
            headers.pop('Referer', None)
        elif 'freeforall.dev' in hostname or 'indiassgetlentp' in hostname:
            headers['Referer'] = 'https://igl-8wky.freeforall.dev/'
            headers['Origin'] = 'https://igl-8wky.freeforall.dev'
        elif 'hakunaymatata.com' in hostname or 'bcdn' in hostname:
            if hostname != 'bcdnt.hakunaymatata.com':
                headers['Referer'] = 'https://mzfi.me/'
                headers['Origin'] = 'https://mzfi.me'
        elif 'Referer' not in headers:
            headers['Referer'] = 'https://netmirror.plus/'
    except Exception:
        pass
    return headers

LOCAL_STREAM_PROXY = None
LOCAL_PROXY_URL = None
LOCAL_PROXY_PORT = 8765

class ThreadedStreamProxyServer(socketserver.ThreadingMixIn, http.server.HTTPServer):
    daemon_threads = True
    allow_reuse_address = True

_PROXY_SESSION = None

def get_proxy_session():
    global _PROXY_SESSION
    if _PROXY_SESSION is None:
        _PROXY_SESSION = requests.Session()
        adapter = requests.adapters.HTTPAdapter(pool_connections=25, pool_maxsize=25, max_retries=2)
        _PROXY_SESSION.mount("http://", adapter)
        _PROXY_SESSION.mount("https://", adapter)
    return _PROXY_SESSION

LOCAL_STREAM_PROXY = None
LOCAL_PROXY_URL = None
LOCAL_PROXY_PORT = None
LOCAL_DYNAMIC_PLAYLISTS = {}

class StreamProxyRequestHandler(http.server.BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"
    target_stream_url = None
    stream_headers = {}

    def log_message(self, format, *args):
        # Keep terminal completely clean
        pass

    def do_HEAD(self):
        self.relay_request(head_only=True)

    def do_GET(self):
        self.relay_request(head_only=False)

    def relay_request(self, head_only=False):
        req_target = self.__class__.target_stream_url
        parsed_path = urllib.parse.urlparse(self.path)
        query = urllib.parse.parse_qs(parsed_path.query)

        # Handle dynamic in-memory playlists (e.g. YouTube multi-track master HLS)
        req_subpath = parsed_path.path.lstrip('/')
        if req_subpath in LOCAL_DYNAMIC_PLAYLISTS:
            content_bytes = LOCAL_DYNAMIC_PLAYLISTS[req_subpath].encode('utf-8')
            self.send_response(200)
            self.send_header('Content-Type', 'application/vnd.apple.mpegurl')
            self.send_header('Content-Length', str(len(content_bytes)))
            self.send_header('Access-Control-Allow-Origin', '*')
            self.send_header('Connection', 'close')
            self.end_headers()
            if not head_only:
                try:
                    self.wfile.write(content_bytes)
                except (BrokenPipeError, ConnectionResetError, socket.error):
                    pass
            return

        # 1. Handle local video/audio file streaming with full byte-range seek support
        if parsed_path.path == '/local' or parsed_path.path.startswith('/local') or (req_target and str(req_target).startswith(('/', 'file://'))):
            file_path = req_target or ''
            if 'path' in query:
                file_path = query['path'][0]
            if file_path.startswith('file://'):
                file_path = file_path[7:]
            file_path = urllib.parse.unquote(file_path)

            if not os.path.isfile(file_path):
                self.send_error(404, "Local file not found")
                return

            try:
                file_size = os.path.getsize(file_path)
            except OSError:
                self.send_error(500, "Cannot access local file")
                return

            ext = os.path.splitext(file_path)[1].lower()
            content_type = "video/mp4"
            if ext in ('.m3u8',): content_type = "application/vnd.apple.mpegurl"
            elif ext in ('.mkv',): content_type = "video/x-matroska"
            elif ext in ('.webm',): content_type = "video/webm"
            elif ext in ('.mp3',): content_type = "audio/mpeg"
            elif ext in ('.m4a',): content_type = "audio/mp4"
            elif ext in ('.flac',): content_type = "audio/flac"

            range_header = self.headers.get('Range')
            start = 0
            end = file_size - 1

            if range_header:
                m = re.search(r'bytes=(\d+)-(\d*)', range_header)
                if m:
                    start = int(m.group(1))
                    if m.group(2):
                        end = int(m.group(2))
                    self.send_response(206)
                    self.send_header('Content-Range', f'bytes {start}-{end}/{file_size}')
                else:
                    self.send_response(200)
            else:
                self.send_response(200)

            length = (end - start) + 1
            self.send_header('Content-Type', content_type)
            self.send_header('Content-Length', str(length))
            self.send_header('Accept-Ranges', 'bytes')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()

            if not head_only:
                try:
                    with open(file_path, 'rb') as f:
                        f.seek(start)
                        remaining = length
                        chunk_size = 131072
                        while remaining > 0:
                            read_len = min(chunk_size, remaining)
                            chunk = f.read(read_len)
                            if not chunk:
                                break
                            self.wfile.write(chunk)
                            remaining -= len(chunk)
                except (BrokenPipeError, ConnectionResetError, socket.error):
                    pass
            return

        # 2. Handle remote stream relay with connection reuse
        if not req_target:
            self.send_error(404, "No active stream")
            return

        if 'url' in query:
            req_target = query['url'][0]
        elif parsed_path.path not in ('/', '/stream', '/stream.mp4', '/stream.m3u8'):
            req_target = urllib.parse.urljoin(self.__class__.target_stream_url, self.path)

        req_headers = dict(self.__class__.stream_headers)
        range_header = self.headers.get('Range')
        if range_header:
            req_headers['Range'] = range_header

        try:
            session = get_proxy_session()
            resp = session.get(
                req_target,
                headers=req_headers,
                stream=True,
                timeout=15,
                allow_redirects=True
            )
            if resp.status_code == 429:
                time.sleep(0.35)
                resp = session.get(
                    req_target,
                    headers=req_headers,
                    stream=True,
                    timeout=15,
                    allow_redirects=True
                )
            self.send_response(resp.status_code)
            for k in ('Content-Type', 'Content-Length', 'Content-Range', 'Accept-Ranges', 'Last-Modified', 'ETag'):
                if k in resp.headers:
                    self.send_header(k, resp.headers[k])
            self.send_header('Access-Control-Allow-Origin', '*')
            self.send_header('Connection', 'keep-alive')
            self.end_headers()

            if not head_only:
                for chunk in resp.iter_content(chunk_size=131072):
                    if chunk:
                        self.wfile.write(chunk)
        except (BrokenPipeError, ConnectionResetError, socket.error):
            pass
        except Exception:
            pass

def start_local_stream_proxy(target_url, custom_headers=None, port=8765):
    """Start or update local streaming proxy to inject required stream headers for VLC / WebView playback."""
    global LOCAL_STREAM_PROXY, LOCAL_PROXY_URL, LOCAL_PROXY_PORT

    target_str = str(target_url or "").strip()
    is_local_file = False
    clean_target = target_str
    if clean_target.startswith("file://"):
        clean_target = clean_target[7:]
    if clean_target.startswith("/") and os.path.exists(clean_target):
        is_local_file = True

    if "127.0.0.1" in target_str or "localhost" in target_str:
        if LOCAL_STREAM_PROXY is None:
            for p in [port, 8766, 8767, 8768]:
                try:
                    server = ThreadedStreamProxyServer(('0.0.0.0', p), StreamProxyRequestHandler)
                    t = threading.Thread(target=server.serve_forever, daemon=True)
                    t.start()
                    LOCAL_STREAM_PROXY = server
                    LOCAL_PROXY_PORT = p
                    break
                except OSError:
                    continue
        return target_str

    if not is_local_file:
        headers = get_stream_headers_for_url(target_url, custom_headers)
        StreamProxyRequestHandler.target_stream_url = target_url
        StreamProxyRequestHandler.stream_headers = headers

    if LOCAL_STREAM_PROXY is None:
        for p in [port, 8766, 8767, 8768]:
            try:
                server = ThreadedStreamProxyServer(('0.0.0.0', p), StreamProxyRequestHandler)
                t = threading.Thread(target=server.serve_forever, daemon=True)
                t.start()
                LOCAL_STREAM_PROXY = server
                LOCAL_PROXY_PORT = p
                break
            except OSError:
                continue

    active_port = LOCAL_PROXY_PORT or port
    if is_local_file:
        sub_name = os.path.basename(clean_target) or "stream.m3u8"
        return f"http://127.0.0.1:{active_port}/local/{sub_name}?path={urllib.parse.quote(clean_target)}"

    ext = ".m3u8" if ".m3u8" in target_str.lower() else ".mp4"
    LOCAL_PROXY_URL = f"http://127.0.0.1:{active_port}/stream{ext}"
    return LOCAL_PROXY_URL

def register_dynamic_playlist(subpath, content):
    """Register dynamic HLS playlist into local proxy server and return streamable URL."""
    clean_sub = subpath.lstrip('/')
    LOCAL_DYNAMIC_PLAYLISTS[clean_sub] = content
    start_local_stream_proxy("")
    active_port = LOCAL_PROXY_PORT or 8765
    return f"http://127.0.0.1:{active_port}/{clean_sub}"

def launch_player(target_uri, player_choice="vlc", custom_headers=None, title=None):
    """Launch stream URL or video in VLC Player with local header proxying."""
    clean_env = os.environ.copy()
    clean_env.pop('LD_LIBRARY_PATH', None)

    play_uri = target_uri
    if target_uri.startswith(('http://', 'https://')):
        # Proxy through local HTTP proxy to inject required stream headers (e.g. mzfi.me referer)
        play_uri = start_local_stream_proxy(target_uri, custom_headers=custom_headers)

    import shlex

    # 1. Try launching via ADB into VLC
    try:
        res = subprocess.run(["adb", "devices"], capture_output=True, text=True, timeout=1.5)
        if "device" in res.stdout and len(res.stdout.strip().splitlines()) > 1:
            quoted_uri = shlex.quote(play_uri)
            adb_cmd = f"am start -a android.intent.action.VIEW -d {quoted_uri} -t video/*"
            if player_choice == "vlc":
                adb_cmd += " -p org.videolan.vlc"
            adb_res = subprocess.run(["adb", "shell", adb_cmd], capture_output=True, text=True, timeout=3)
            if adb_res.returncode == 0:
                print(" Streaming opened in VLC Player!")
                return True
    except Exception:
        pass

    # 2. Try launching via the local Android intent command when available
    try:
        am_cmd = ["am", "start", "-a", "android.intent.action.VIEW", "-d", play_uri, "-t", "video/*"]
        if player_choice == "vlc":
            am_cmd.extend(["-p", "org.videolan.vlc"])
        res = subprocess.run(am_cmd, capture_output=True, text=True, timeout=2, env=clean_env)
        if res.returncode == 0:
            print(" Streaming opened in VLC Player!")
            return True
    except Exception:
        pass

    # 3. Fallback to generic media player intent (never open web browser)
    try:
        am_cmd = ["am", "start", "-a", "android.intent.action.VIEW", "-d", play_uri, "-t", "video/*"]
        res = subprocess.run(am_cmd, capture_output=True, text=True, timeout=2, env=clean_env)
        if res.returncode == 0:
            print(" Streaming opened in default video player!")
            return True
    except Exception:
        pass

    # The Android UI owns media intents when this engine is embedded in the APK.
    print(f"[WARN] Could not launch media player automatically. Stream link: {play_uri}")
    return False

def save_m3u8_stream_file(url, title, target_dir=None):
    """Save the ~300KB .m3u8 stream playlist file to storage for instant VLC streaming."""
    out_dir = target_dir or DEFAULT_DIR
    os.makedirs(out_dir, exist_ok=True)
    clean_name = clean_filename(title, default_ext=".m3u8")
    if not clean_name.lower().endswith('.m3u8'):
        clean_name = os.path.splitext(clean_name)[0] + '.m3u8'
    file_path = os.path.join(out_dir, clean_name)
    headers = get_stream_headers_for_url(url)
    ref = headers.get('Referer', '')
    ua = headers.get('User-Agent', '')

    if '.m3u8' in url.lower():
        try:
            r = requests.get(url, headers=headers, stream=True, timeout=5)
            if r.status_code == 200:
                chunk = r.raw.read(1024, decode_content=True)
                if b'#EXTM3U' in chunk:
                    full_content = chunk + r.raw.read()
                    with open(file_path, 'wb') as f:
                        f.write(full_content)
                    sync_to_sdcard_if_needed(file_path)
                    print(f" Saved instant stream playlist ({len(full_content)//1024} KB) to: {file_path}")
                    return file_path
        except Exception:
            pass
    try:
        content = f"#EXTM3U\n#EXT-X-VERSION:3\n#EXTINF:-1,{title}\n"
        if ref:
            content += f"#EXTVLCOPT:http-referrer={ref}\n"
        if ua:
            content += f"#EXTVLCOPT:http-user-agent={ua}\n"
        content += f"{url}\n"
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        sync_to_sdcard_if_needed(file_path)
        print(f" Saved instant stream playlist to: {file_path}")
        return file_path
    except Exception:
        pass
    return None

def decrypt_netmirror_tv_url(encrypted_val):
    """Decrypt AES-GCM encrypted media URL from NetMirror watchbox."""
    try:
        from Cryptodome.Cipher import AES
        clean_enc = encrypted_val.replace('&encrypt=true', '').strip()
        raw = base64.b64decode(clean_enc)
        if len(raw) < 28:
            return None
        iv = raw[:12]
        tag = raw[12:28]
        ciphertext = raw[28:]
        for secret in [b'net###@@sss', b'NET_TV_SECRET_KEY']:
            try:
                key = hashlib.sha256(secret).digest()
                cipher = AES.new(key, AES.MODE_GCM, nonce=iv)
                decrypted = cipher.decrypt_and_verify(ciphertext, tag)
                return decrypted.decode('utf-8')
            except Exception:
                continue
        return None
    except Exception:
        return None

def extract_quality_tag(q_val):
    if not q_val:
        return ""
    qs = str(q_val).strip()
    m = re.search(r'(4320p|2160p|1440p|1080p|720p|480p|360p|240p|144p|4K|2K|HD|SD)', qs, re.I)
    if m:
        val = m.group(1).lower()
        return val if 'p' in val else val.upper()
    first_w = qs.split()[0]
    if first_w.lower() not in ("best", "ask", "audio", "default", "none"):
        return first_w
    return ""

def clean_filename(title, default_ext="", quality=None):
    title = re.sub(r'[\\/*?:"<>|]', '', str(title or ""))
    title = re.sub(r'[\x00-\x1f\x7f]', '', title)
    title = re.sub(r'[\s_]+', ' ', title).strip('_. ')
    base, ext = os.path.splitext(title)
    # Strip format tags like .f137, .f398, .f140-20, .part
    base = re.sub(r'[\._-]f[a-zA-Z0-9_\-]+$', '', base, flags=re.I).strip()
    # Strip YouTube IDs like [hpwAMSaB28c] or (hpwAMSaB28c)
    base = re.sub(r'\s*\[[a-zA-Z0-9_-]{11}\]$', '', base).strip()
    base = re.sub(r'\s*\([a-zA-Z0-9_-]{11}\)$', '', base).strip()
    # Strip version suffixes like _v1, v2, (v1), [v1]
    base = re.sub(r'[\s._-]+(v\d+|\bv\d+\b|f\d+|part\d+)$', '', base, flags=re.I).strip()
    base = re.sub(r'\s*[\(\[]v\d+[\)\]]$', '', base, flags=re.I).strip()

    # Enforce NAME{QUALITY} for video outputs
    q_tag = extract_quality_tag(quality)
    if q_tag and not (ext and ext.lower() in ('.mp3', '.m4a', '.aac', '.flac', '.wav', '.ogg')):
        base = re.sub(r'\s*[\(\[\{](?:4320p|2160p|1440p|1080p|720p|480p|360p|240p|144p|4K|2K|HD|SD)[\)\]\}]', '', base, flags=re.I).strip()
        base = f"{base}{{{q_tag}}}"

    if ext:
        title = base + ext
    else:
        title = base
    if not title:
        title = "downloaded_file"
    if default_ext and not os.path.splitext(title)[1]:
        title += default_ext
    return title

def _cleanup_download_cache(cache_dir, target_dir, job_id=None, keep_path=None):
    """Purge temporary fragment files, .part, .ytdl, and staging folders for this specific job only."""
    jid_str = str(job_id or "").strip()
    if not jid_str:
        return
    keep_norm = os.path.abspath(keep_path) if keep_path else None

    # Clean cache directory for this specific job
    if cache_dir and os.path.exists(cache_dir):
        try:
            for item in os.listdir(cache_dir):
                if jid_str in item:
                    item_path = os.path.join(cache_dir, item)
                    if keep_norm and os.path.abspath(item_path) == keep_norm:
                        continue
                    if os.path.isdir(item_path):
                        shutil.rmtree(item_path, ignore_errors=True)
                    else:
                        try: os.remove(item_path)
                        except Exception: pass
        except Exception:
            pass

    # Clean target directory for this specific job only
    if target_dir and os.path.exists(target_dir):
        try:
            # Clean this job's unique stage dir if present
            job_stage = os.path.join(target_dir, f".arc_stage_{jid_str}")
            if os.path.exists(job_stage) and (not keep_norm or os.path.abspath(job_stage) != keep_norm):
                shutil.rmtree(job_stage, ignore_errors=True)

            for item in os.listdir(target_dir):
                if jid_str in item and item.startswith(('.temp', '.arc_stage', 'ytdl_raw')):
                    item_path = os.path.join(target_dir, item)
                    if keep_norm and os.path.abspath(item_path) == keep_norm:
                        continue
                    if os.path.isdir(item_path):
                        shutil.rmtree(item_path, ignore_errors=True)
                    else:
                        try: os.remove(item_path)
                        except Exception: pass
        except Exception:
            pass

def get_dest_path(filename, target_dir=None):
    out_dir = target_dir or DEFAULT_DIR
    os.makedirs(out_dir, exist_ok=True)
    if os.path.isabs(filename):
        return filename
    return os.path.join(out_dir, filename)

def safe_move(src, dst):
    """Safely move a file, falling back to chunked copy without copystat if Android FAT32/sdcardfs rejects metadata."""
    if not os.path.exists(src):
        return None
    target_parent = os.path.dirname(dst)
    if target_parent:
        os.makedirs(target_parent, exist_ok=True)
    if os.path.abspath(src) == os.path.abspath(dst):
        return dst
    if os.path.exists(dst):
        try: os.remove(dst)
        except Exception: pass
    try:
        shutil.move(src, dst)
        return dst
    except Exception:
        try:
            with open(src, 'rb') as fsrc:
                with open(dst, 'wb') as fdst:
                    shutil.copyfileobj(fsrc, fdst, length=1024*1024)
            try: os.remove(src)
            except Exception: pass
            return dst
        except Exception as copy_err:
            print(f"[WARN] safe_move fallback failed: {copy_err}")
            raise

import time
import socket

def check_network():
    """Check internet connection by testing socket connections to reliable hosts."""
    for host, port in [("1.1.1.1", 53), ("8.8.8.8", 53), ("google.com", 80)]:
        try:
            s = socket.create_connection((host, port), timeout=3)
            s.close()
            return True
        except Exception:
            pass
    return False

def wait_for_network():
    """If network is down, pause and wait until connectivity is restored."""
    if check_network():
        return
    print("\n" + "!"*60)
    print(" [WARN] Network connection lost! Download paused.")
    print(" > Waiting for internet connection to recover...")
    print("!"*60, flush=True)
    
    down_start = time.time()
    while not check_network():
        time.sleep(3)
        
    down_duration = int(time.time() - down_start)
    print("\n" + "="*60)
    print(f" [OK] Network connection restored (was offline for {down_duration}s)!")
    print(" > Resuming download...")
    print("="*60, flush=True)

PROGRESS_SPINNER_FRAMES = ['⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏']
PROGRESS_SPINNER_INDEX = 0

CURRENT_DOWNLOAD_JOB_ID = None
ACTIVE_DOWNLOAD_PROGRESS = {}
CANCELLED_DOWNLOAD_JOBS = set()
_job_local = threading.local()

def set_current_job_id(job_id):
    _job_local.job_id = str(job_id) if job_id else None

def get_current_job_id():
    return getattr(_job_local, 'job_id', None) or CURRENT_DOWNLOAD_JOB_ID

def get_download_progress(job_id=None):
    if job_id:
        return ACTIVE_DOWNLOAD_PROGRESS.get(str(job_id), {})
    return ACTIVE_DOWNLOAD_PROGRESS

def get_download_progress_json(job_id=None):
    return json.dumps(get_download_progress(job_id), ensure_ascii=False)

PAUSED_DOWNLOAD_JOBS = set()
PAUSE_EVENTS = {}

def pause_download(job_id):
    if job_id:
        j_str = str(job_id)
        PAUSED_DOWNLOAD_JOBS.add(j_str)
        if j_str not in PAUSE_EVENTS:
            PAUSE_EVENTS[j_str] = threading.Event()
        PAUSE_EVENTS[j_str].clear()
        if j_str in ACTIVE_DOWNLOAD_PROGRESS:
            ACTIVE_DOWNLOAD_PROGRESS[j_str]["status"] = "Paused"
            ACTIVE_DOWNLOAD_PROGRESS[j_str]["speed"] = "--"
            ACTIVE_DOWNLOAD_PROGRESS[j_str]["eta"] = "--"
    return True

def resume_download(job_id):
    if job_id:
        j_str = str(job_id)
        PAUSED_DOWNLOAD_JOBS.discard(j_str)
        evt = PAUSE_EVENTS.get(j_str)
        if evt:
            evt.set()
        if j_str in ACTIVE_DOWNLOAD_PROGRESS:
            ACTIVE_DOWNLOAD_PROGRESS[j_str]["status"] = "Downloading"
    return True

def cancel_download(job_id):
    if job_id:
        j_str = str(job_id)
        CANCELLED_DOWNLOAD_JOBS.add(j_str)
        PAUSED_DOWNLOAD_JOBS.discard(j_str)
        evt = PAUSE_EVENTS.get(j_str)
        if evt:
            evt.set()
        if j_str in ACTIVE_DOWNLOAD_PROGRESS:
            ACTIVE_DOWNLOAD_PROGRESS[j_str]["status"] = "Cancelled"
    return True

def render_download_progress(percent, total_str="--", speed_str="--", eta_str="--", job_id=None, downloaded_str=None):
    """Render a sleek, animated single-line progress bar guaranteed not to wrap or spam."""
    global PROGRESS_SPINNER_INDEX, CURRENT_DOWNLOAD_JOB_ID, ACTIVE_DOWNLOAD_PROGRESS
    try:
        pct = max(0.0, min(100.0, float(percent)))
    except Exception:
        pct = 0.0

    spd_clean = speed_str if ('s' in speed_str.lower() or speed_str.lower() in ('--', 'done', 'complete', 'paused')) else f"{speed_str}/s"
    
    eta_val = str(eta_str or "--").strip()
    if eta_val.lower().startswith('eta'):
        eta_clean = eta_val[3:].strip()
    else:
        eta_clean = eta_val
    if not eta_clean:
        eta_clean = "--"

    jid = job_id or get_current_job_id()
    if jid:
        current_status = ACTIVE_DOWNLOAD_PROGRESS.get(str(jid), {}).get("status", "Downloading")
        if str(jid) in CANCELLED_DOWNLOAD_JOBS or current_status == "Cancelled":
            status = "Cancelled"
        elif str(jid) in PAUSED_DOWNLOAD_JOBS or current_status == "Paused":
            status = "Paused"
        elif current_status == "Complete":
            status = "Complete"
        else:
            status = "Downloading"

        if downloaded_str is not None:
            d_str = str(downloaded_str)
            t_str = str(total_str)
        elif str(total_str).startswith("Ep ") and "(" in str(total_str):
            m_ep = re.match(r"(Ep\s+\d+/\d+)\s*\((.*)\)", str(total_str))
            if m_ep:
                d_str = m_ep.group(1).strip()
                t_str = m_ep.group(2).strip()
            else:
                d_str = str(total_str)
                t_str = "--"
        elif "/" in str(total_str) and not str(total_str).startswith("Ep "):
            parts = str(total_str).split("/", 1)
            d_str = parts[0].strip()
            t_str = parts[1].strip()
        else:
            d_str = str(total_str)
            t_str = "--"

        ACTIVE_DOWNLOAD_PROGRESS[str(jid)] = {
            "percent": round(pct, 1),
            "downloaded_str": d_str,
            "total_str": t_str,
            "speed": spd_clean,
            "eta": eta_clean,
            "status": status
        }

    if not sys.stdout.isatty():
        return

    try:
        cols = shutil.get_terminal_size((70, 20)).columns
    except Exception:
        cols = 70

    spin = PROGRESS_SPINNER_FRAMES[PROGRESS_SPINNER_INDEX % len(PROGRESS_SPINNER_FRAMES)]
    PROGRESS_SPINNER_INDEX += 1

    info_plain = f" {pct:5.1f}% | {total_str} | {spd_clean} | {eta_clean}"
    overhead = len(info_plain) + 6
    bar_width = max(5, min(24, cols - overhead))

    # Fractional smooth block computation
    full_blocks = int(bar_width * (pct / 100.0))
    fraction = (bar_width * (pct / 100.0)) - full_blocks
    frac_blocks = ['', '▏', '▎', '▍', '▌', '▋', '▊', '▉']
    frac_char = frac_blocks[int(fraction * 8)] if full_blocks < bar_width else ''
    empty_blocks = max(0, bar_width - full_blocks - (1 if frac_char else 0))

    bar_colored = f"\033[38;5;45m{'█' * full_blocks}{frac_char}\033[38;5;238m{'░' * empty_blocks}\033[0m"
    line = f"\033[1;36m{spin}\033[0m [{bar_colored}] \033[1;97m{pct:5.1f}%\033[0m | \033[38;5;153m{total_str}\033[0m | \033[38;5;220m{spd_clean}\033[0m | \033[38;5;141m{eta_clean}\033[0m"

    sys.stdout.write(f"\r\033[K{line}")
    sys.stdout.flush()

ARIA_PROGRESS_REGEX = re.compile(
    r'\[#[0-9a-fA-F]+\s+([\d\.]+[A-Za-z]+)/([\d\.]+[A-Za-z]+)\((\d+)%\)\s*(?:CN:\d+\s*)?DL:([\d\.]+[A-Za-z]+)?(?:\s*ETA:([^\s\]]+))?\]'
)
YTDLP_PROGRESS_REGEX = re.compile(
    r'\[download\]\s+([\d\.]+)%\s+of\s+~?\s*([\d\.]+\s*[A-Za-z]+)\s+at\s+([\d\.]+\s*[A-Za-z]+/s)\s+ETA\s+([\d:]+)'
)

def _process_progress_line(line):
    line = line.strip()
    if not line:
        return

    # 1. Parse multi-threaded aria2c progress line
    m_aria = ARIA_PROGRESS_REGEX.search(line)
    if m_aria:
        try:
            downloaded = m_aria.group(1)
            total = m_aria.group(2)
            pct = float(m_aria.group(3))
            spd = m_aria.group(4) or "--"
            eta = m_aria.group(5) or "--"
            render_download_progress(pct, total_str=f"{downloaded}/{total}", speed_str=spd, eta_str=eta)
            return
        except Exception:
            pass

    # 2. Parse yt-dlp progress line
    m_yt = YTDLP_PROGRESS_REGEX.search(line)
    if m_yt:
        try:
            pct = float(m_yt.group(1))
            tot = m_yt.group(2).replace(' ', '')
            spd = m_yt.group(3).replace(' ', '')
            eta = m_yt.group(4)
            render_download_progress(pct, total_str=tot, speed_str=spd, eta_str=eta)
            return
        except Exception:
            pass

    # 3. Simple percentage fallback
    m_pct = re.search(r'\[download\]\s+([\d\.]+)%', line)
    if m_pct:
        try:
            pct = float(m_pct.group(1))
            render_download_progress(pct)
            return
        except Exception:
            pass

    # 4. Filter out aria2c / yt-dlp summary and separator lines to eliminate scrolling spam
    if line.startswith(('===', '---', '***', 'FILE:', 'Download Results:', '[FixupM3u8]')) or \
       line.startswith('[download] Downloading fragment') or ('frag' in line and line.startswith('[download]')) or \
       'Download Progress Summary' in line or \
       re.match(r'^\([A-Z]+\):download', line):
        return

    sys.stdout.write(f"\r\033[K{line}\n")
    sys.stdout.flush()

def run_command_with_clean_progress(cmd):
    """Run subprocess while processing \r and \n into clean single-line progress updates."""
    p = None
    try:
        p = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
            universal_newlines=True
        )
        buf = []
        while True:
            ch = p.stdout.read(1)
            if not ch:
                if buf:
                    _process_progress_line("".join(buf))
                break
            if ch in ('\r', '\n'):
                if buf:
                    _process_progress_line("".join(buf))
                    buf = []
            else:
                buf.append(ch)
        p.wait()
        if p.returncode == 0:
            render_download_progress(100.0, total_str="Complete", speed_str="Done", eta_str="0s")
        sys.stdout.write("\n")
        sys.stdout.flush()
        return p.returncode
    except KeyboardInterrupt:
        if p:
            try:
                p.terminate()
                p.wait(timeout=1.5)
            except Exception:
                try:
                    p.kill()
                except Exception:
                    pass
        sys.stdout.write("\n[WARN] Download interrupted by user.\n")
        sys.stdout.flush()
        return -1
    except Exception as e:
        print(f"[FAIL] Process execution error: {e}")
        return 1

def download_file_stream(url, filename, custom_headers=None, target_dir=None, cache_dir=None, job_id=None, batch_info=None):
    headers = get_stream_headers_for_url(url, {**HEADERS, **(custom_headers or {})})
    print(f"> Fetching: {url}")
    
    dest_path = get_dest_path(filename, target_dir)
    jid = job_id or get_current_job_id()

    # Place in-progress fragments in cache directory if provided, keeping target directory clean
    if cache_dir:
        os.makedirs(cache_dir, exist_ok=True)
        work_path = os.path.join(cache_dir, f".temp_{jid}_{filename}.part")
    else:
        work_path = dest_path + ".part"

    downloaded = 0
    total_bytes = 0
    if os.path.exists(work_path):
        try:
            downloaded = os.path.getsize(work_path)
        except Exception:
            downloaded = 0

    while True:
        try:
            req_headers = dict(headers)
            if 'okcdn' in url.lower():
                req_headers.pop('Origin', None)
                req_headers.pop('Referer', None)
            if downloaded > 0:
                req_headers['Range'] = f'bytes={downloaded}-'
            
            with requests.get(url, headers=req_headers, stream=True, timeout=30) as r:
                if r.status_code == 416: # Range Not Satisfiable (already completed)
                    break
                r.raise_for_status()
                
                content_type = r.headers.get('content-type', '').lower()
                content_disp = r.headers.get('content-disposition', '')
                
                if downloaded == 0:
                    disp_filename = None
                    if content_disp:
                        m_star = re.search(r"filename\*\s*=\s*([a-zA-Z0-9_\-]+)''([^;\r\n]+)", content_disp, re.I)
                        if m_star:
                            try:
                                disp_filename = urllib.parse.unquote(m_star.group(2), encoding=m_star.group(1))
                            except Exception:
                                disp_filename = urllib.parse.unquote(m_star.group(2))
                        if not disp_filename:
                            m_simple = re.search(r'filename\s*=\s*(?:"([^"]+)"|([^;\s\r\n]+))', content_disp, re.I)
                            if m_simple:
                                disp_filename = m_simple.group(1) or m_simple.group(2)

                    if disp_filename and (not os.path.splitext(filename)[1] or any(filename.lower().startswith(p) for p in ('download', 'video', 'stream', 'media', 'direct file', 'file'))):
                        filename = clean_filename(disp_filename)
                        dest_path = get_dest_path(filename, target_dir)
                    elif 'application/pdf' in content_type and not filename.endswith('.pdf'):
                        filename = os.path.splitext(filename)[0] + '.pdf'
                        dest_path = get_dest_path(filename, target_dir)
                    elif any(z in content_type for z in ('application/zip', 'application/x-zip-compressed')) and not filename.endswith('.zip'):
                        filename = os.path.splitext(filename)[0] + '.zip'
                        dest_path = get_dest_path(filename, target_dir)
                    elif 'application/vnd.android.package-archive' in content_type and not filename.endswith('.apk'):
                        filename = os.path.splitext(filename)[0] + '.apk'
                        dest_path = get_dest_path(filename, target_dir)
                    elif 'text/html' in content_type and not filename.endswith('.html'):
                        raise Exception("The server returned an HTML webpage instead of a direct file stream.")

                    total_length = r.headers.get('content-length')
                    total_bytes = int(total_length) if total_length and total_length.isdigit() else 0

                mode = 'ab' if downloaded > 0 and r.status_code == 206 else 'wb'
                if mode == 'wb':
                    downloaded = 0

                print(f"> Saving to: {dest_path}")
                last_time = time.time()
                last_downloaded = downloaded
                smooth_speed = 0.0
                with open(work_path, mode) as f:
                    for chunk in r.iter_content(chunk_size=524288):
                        if jid and str(jid) in CANCELLED_DOWNLOAD_JOBS:
                            print("\n[STOP] Download cancelled by user.")
                            try:
                                if os.path.exists(work_path):
                                    os.remove(work_path)
                            except Exception:
                                pass
                            return None

                        if jid and str(jid) in PAUSED_DOWNLOAD_JOBS:
                            pct_val = (downloaded / total_bytes * 100) if total_bytes > 0 else 0.0
                            if batch_info:
                                b_idx, b_tot = batch_info
                                pct_val = round(((b_idx - 1) / b_tot) * 100.0 + (pct_val / b_tot), 1)
                                tot_label = f"Ep {b_idx}/{b_tot} (Paused)"
                            else:
                                tot_label = f"{downloaded / (1024*1024):.1f}/{total_bytes / (1024*1024):.1f}MB" if total_bytes > 0 else f"{downloaded / (1024*1024):.1f}MB"
                            render_download_progress(pct_val, tot_label, "Paused", "--", job_id=jid)
                            evt = PAUSE_EVENTS.get(str(jid))
                            if evt:
                                evt.wait()
                            last_time = time.time()
                            continue

                        if chunk:
                            f.write(chunk)
                            downloaded += len(chunk)
                            now = time.time()
                            if now - last_time >= 0.4:
                                delta_time = now - last_time
                                delta_bytes = downloaded - last_downloaded
                                inst_speed = (delta_bytes / delta_time) if delta_time > 0 else 0
                                smooth_speed = inst_speed if smooth_speed == 0.0 else (0.35 * inst_speed + 0.65 * smooth_speed)
                                speed_mb = smooth_speed / (1024 * 1024)
                                speed_str = f"{speed_mb:.2f}MB/s"
                                percent = (downloaded / total_bytes * 100) if total_bytes > 0 else 0.0
                                mb_done = downloaded / (1024 * 1024)
                                mb_total = total_bytes / (1024 * 1024) if total_bytes > 0 else 0.0
                                rem_bytes = max(0, total_bytes - downloaded)
                                eta_sec = int(rem_bytes / smooth_speed) if smooth_speed > 0 else 0
                                eta_m, eta_s = divmod(eta_sec, 60)
                                eta_str = f"{eta_m:02d}:{eta_s:02d}"

                                if batch_info:
                                    b_idx, b_tot = batch_info
                                    overall_pct = round(((b_idx - 1) / b_tot) * 100.0 + (percent / b_tot), 1)
                                    b_down = f"Ep {b_idx}/{b_tot}"
                                    b_tot_str = f"{mb_done:.1f}/{mb_total:.1f} MB" if total_bytes > 0 else f"{mb_done:.1f} MB"
                                    render_download_progress(overall_pct, total_str=b_tot_str, speed_str=speed_str, eta_str=eta_str, job_id=jid, downloaded_str=b_down)
                                else:
                                    if total_bytes > 0:
                                        render_download_progress(percent, total_str=f"{mb_total:.1f} MB", speed_str=speed_str, eta_str=eta_str, job_id=jid, downloaded_str=f"{mb_done:.1f} MB")
                                    else:
                                        render_download_progress(100.0, total_str="--", speed_str=speed_str, eta_str="--", job_id=jid, downloaded_str=f"{mb_done:.1f} MB")
                                last_time = now
                                last_downloaded = downloaded
                break
        except (requests.exceptions.RequestException, socket.error) as e:
            if not check_network():
                wait_for_network()
            else:
                print(f"\n[WARN] Network hiccup: {e}. Retrying in 3s...", flush=True)
                time.sleep(3)

    if jid and str(jid) in CANCELLED_DOWNLOAD_JOBS:
        if os.path.exists(work_path):
            try: os.remove(work_path)
            except Exception: pass
        return None

    # Move from temporary work path to destination path
    if os.path.exists(work_path):
        safe_move(work_path, dest_path)

    sys.stdout.write("\n")
    sys.stdout.flush()
    if jid:
        if batch_info:
            b_idx, b_tot = batch_info
            ep_pct = round((b_idx / b_tot) * 100.0, 1)
            render_download_progress(ep_pct, total_str="Done", speed_str="Done", eta_str="--", job_id=jid, downloaded_str=f"Ep {b_idx}/{b_tot}")
            if str(jid) in ACTIVE_DOWNLOAD_PROGRESS:
                ACTIVE_DOWNLOAD_PROGRESS[str(jid)]["status"] = "Downloading"
        else:
            render_download_progress(100.0, total_str="Complete", speed_str="Done", eta_str="0s", job_id=jid, downloaded_str="Complete")

    print("[OK] Download complete!")
    sync_to_sdcard_if_needed(dest_path)
    return dest_path

def handle_bypasser(url, out_name=None, target_dir=None):
    if not HAS_BYPASSER:
        return None
    try:
        direct_url = direct_link_generator(url)
        if direct_url and isinstance(direct_url, str) and not direct_url.startswith("ERROR:") and direct_url.startswith(("http://", "https://", "//")):
            if direct_url.startswith("//"):
                direct_url = "https:" + direct_url
            print(f"> Direct link bypassed: {direct_url[:80]}...")
            parsed = urllib.parse.urlparse(direct_url)
            base = os.path.basename(parsed.path)
            filename = out_name or (base if base and '.' in base else "downloaded_file")
            return download_file_stream(direct_url, clean_filename(filename), target_dir=target_dir)
    except Exception as e:
        pass
    return None

def handle_selfstudys(url, target_dir=None):
    print("> Detected SelfStudys link...")
    res = requests.get(url, headers=HEADERS, timeout=15)
    res.raise_for_status()
    html = res.text
    soup = BeautifulSoup(html, 'html.parser')
    
    raw_title = ""
    if soup.title and soup.title.string:
        raw_title = soup.title.string.strip()
    elif soup.find('h1'):
        raw_title = soup.find('h1').text.strip()
    
    raw_title = re.sub(r'\s*\(Quick Revision Notes\)', '', raw_title, flags=re.IGNORECASE)
    raw_title = re.sub(r'\s*PDF\s*', '', raw_title, flags=re.IGNORECASE)
    
    filename = clean_filename(raw_title, default_ext=".pdf")
    
    pdf_match = re.search(r'https?://[^\s"\'\(\)]+sitepdfs[^\s"\'\(\)]+', html)
    if pdf_match:
        pdf_url = pdf_match.group(0)
        return download_file_stream(pdf_url, filename, target_dir=target_dir)
        
    df_match = re.search(r'downloadFile\([\"\'\`]\s*([^\s\"\'\`]+)\s*[\"\'\`]\)', html)
    if df_match:
        pdf_url = df_match.group(1)
        if not pdf_url.startswith('http'):
            pdf_url = urllib.parse.urljoin('https://www.selfstudys.com', pdf_url)
        return download_file_stream(pdf_url, filename, target_dir=target_dir)
        
    updf_match = re.search(r'https?://[^\s"\'\(\)]+uploads/pdf/[^\s"\'\(\)]+\.pdf', html)
    if updf_match:
        return download_file_stream(updf_match.group(0), filename, target_dir=target_dir)

    iframe = soup.find('iframe', src=re.compile(r'\.pdf'))
    if iframe:
        return download_file_stream(iframe['src'], filename, target_dir=target_dir)

    raise Exception("Could not locate PDF download link on SelfStudys page.")

def handle_studiestoday(url, target_dir=None):
    print("> Detected StudiesToday link...")
    res = requests.get(url, headers=HEADERS, timeout=15)
    res.raise_for_status()
    html = res.text
    soup = BeautifulSoup(html, 'html.parser')
    
    # Check if main page links to a viewfile.html page
    vf_link = soup.find('a', href=re.compile(r'/node/\d+/viewfile\.html'))
    if vf_link:
        view_url = urllib.parse.urljoin(url, vf_link['href'])
        res = requests.get(view_url, headers=HEADERS, timeout=15)
        res.raise_for_status()
        html = res.text
        soup = BeautifulSoup(html, 'html.parser')

    raw_title = soup.title.string.strip() if soup.title else "studiestoday_doc"
    raw_title = re.sub(r'^Views?\s+and\s+Download\s+PDF\s+file\s*:?\s*', '', raw_title, flags=re.IGNORECASE)
    raw_title = re.sub(r'^View\s+PDF\s+', '', raw_title, flags=re.IGNORECASE)
    raw_title = re.sub(r'\s*ICSE\s+Book\s+PDF$', '', raw_title, flags=re.IGNORECASE)
    
    if raw_title.lower() in ("views and download pdf file", "studiestoday_doc", ""):
        h1 = soup.find('h1')
        if h1:
            raw_title = re.sub(r'^View\s+PDF\s+', '', h1.text.strip(), flags=re.IGNORECASE)
            
    filename = clean_filename(raw_title, default_ext=".pdf")
    
    # Check 1: PDFFlip source attribute or securefile path
    sec_match = re.search(r'source=["\']([^"\']*/securefile/[^"\']+)["\']', html, re.IGNORECASE)
    if sec_match:
        pdf_url = urllib.parse.urljoin(url, sec_match.group(1))
        return download_file_stream(pdf_url, filename, target_dir=target_dir)

    sec_match2 = re.search(r'/securefile/([A-Za-z0-9+/=]+)', html)
    if sec_match2:
        pdf_url = urllib.parse.urljoin(url, sec_match2.group(0))
        return download_file_stream(pdf_url, filename, target_dir=target_dir)

    # Check 3: node ID base64 fallback from URL or html
    node_match = re.search(r'(\d{5,})', url) or re.search(r'/node/(\d+)', html)
    if node_match:
        import base64
        node_id = node_match.group(1)
        b64_id = base64.b64encode(node_id.encode()).decode()
        pdf_url = urllib.parse.urljoin(url, f"/securefile/{b64_id}")
        try:
            return download_file_stream(pdf_url, filename, target_dir=target_dir)
        except Exception:
            pass

    # Check 4: Direct href .pdf
    pdf_match = re.search(r'href=["\']([^"\']+\.pdf[^"\']*)["\']', html, re.IGNORECASE)
    if pdf_match:
        pdf_url = urllib.parse.urljoin(url, pdf_match.group(1))
        return download_file_stream(pdf_url, filename, target_dir=target_dir)
        
    # Check 5: iframe
    iframe = soup.find('iframe')
    if iframe and iframe.get('src'):
        pdf_url = urllib.parse.urljoin(url, iframe['src'])
        return download_file_stream(pdf_url, filename, target_dir=target_dir)
        
    raise Exception("Could not find PDF download link on StudiesToday page.")

class NetworkAwareLogger:
    def debug(self, msg):
        pass
    def warning(self, msg):
        msg_l = msg.lower()
        if "retrying" in msg_l or "failed to resolve" in msg_l or "unreachable" in msg_l or "broken" in msg_l:
            if not check_network():
                wait_for_network()
    def error(self, msg):
        print(f"[FAIL] {msg}", flush=True)
        msg_l = msg.lower()
        if "failed to resolve" in msg_l or "unreachable" in msg_l or "connection" in msg_l:
            if not check_network():
                wait_for_network()

def ensure_cookiefile_from_string(cookie_str, target_dir=None):
    if not cookie_str or os.path.isfile(cookie_str):
        return cookie_str
    try:
        tmp_dir = target_dir or os.path.expanduser("~/.cache")
        os.makedirs(tmp_dir, exist_ok=True)
        cookie_path = os.path.join(tmp_dir, "arc_yt_cookies.txt")
        if "# Netscape" in cookie_str or "\t" in cookie_str:
            with open(cookie_path, "w", encoding="utf-8") as f:
                f.write(cookie_str)
            return cookie_path
        lines = ["# Netscape HTTP Cookie File", "# https://curl.se/docs/http-cookies.html", ""]
        now = int(time.time()) + 86400 * 365
        for pair in cookie_str.split(";"):
            pair = pair.strip()
            if "=" in pair:
                k, v = pair.split("=", 1)
                lines.append(f".youtube.com\tTRUE\t/\tTRUE\t{now}\t{k.strip()}\t{v.strip()}")
        with open(cookie_path, "w", encoding="utf-8") as f:
            f.write("\n".join(lines) + "\n")
        return cookie_path
    except Exception as e:
        print(f"[WARN] Failed to write cookiefile: {e}")
        return None

def get_ytdlp_opts(custom_opts=None, cookie=None):
    opts = {}
    if HAS_YTDLP:
        try:
            _, _, _, default_opts = yt_dlp.parse_options([])
            opts.update(default_opts)
        except Exception:
            pass

    speed_opts = {
        'nopart': False,
        'continuedl': True,
        'noplaylist': True,
        'windowsfilenames': True,
        'socket_timeout': 30,
        'retries': 20,
        'fragment_retries': 20,
        'file_access_retries': 5,
        'concurrent_fragment_downloads': 16,
        'buffersize': 1024 * 64,
        'http_chunk_size': 10485760,
        'logger': NetworkAwareLogger(),
    }

    node_bin = shutil.which('node')
    if node_bin and os.path.exists(node_bin):
        speed_opts['js_runtimes'] = {'node': {'path': node_bin}}

    if shutil.which('aria2c'):
        speed_opts['external_downloader'] = {'default': 'aria2c'}
        speed_opts['external_downloader_args'] = {
            'default': ['-j', '16', '-s', '16', '-x', '16', '-k', '1M', '--min-split-size=1M', '--allow-overwrite=true', '--file-allocation=falloc']
        }

    opts.update(speed_opts)

    if not cookie:
        for possible_cookie in [
            "/sdcard/Download/cookies.txt",
            "/sdcard/Download/youtube_cookies.txt",
            "/storage/emulated/0/Download/cookies.txt",
            "/storage/emulated/0/Download/youtube_cookies.txt",
            os.path.expanduser("~/cookies.txt"),
            os.path.expanduser("~/youtube_cookies.txt"),
        ]:
            if os.path.isfile(possible_cookie):
                cookie = possible_cookie
                break

    if cookie:
        if os.path.isfile(cookie):
            opts['cookiefile'] = cookie
        else:
            cookie_file = ensure_cookiefile_from_string(cookie)
            if cookie_file:
                opts['cookiefile'] = cookie_file
            opts.setdefault('http_headers', {})['Cookie'] = cookie
    if custom_opts:
        opts.update(custom_opts)
    return opts


def download_hls_with_python(url, dest_path, headers=None):
    """Download an HLS stream without requiring an external yt-dlp executable."""
    if not HAS_YTDLP:
        return 1
    try:
        def progress_hook(d):
            if CURRENT_DOWNLOAD_JOB_ID in CANCELLED_DOWNLOAD_JOBS:
                raise Exception("Download cancelled by user")
            status = d.get('status')
            if status == 'downloading':
                total = d.get('total_bytes') or d.get('total_bytes_estimate') or 0
                downloaded = d.get('downloaded_bytes') or 0
                speed = d.get('speed') or 0
                eta = d.get('eta') or 0
                percent = (downloaded / total * 100) if total > 0 else 0.0
                render_download_progress(
                    percent=percent,
                    speed=format_speed(speed),
                    eta=format_eta(eta),
                    downloaded_bytes=downloaded,
                    total_bytes=total,
                )

        options = get_ytdlp_opts({
            "format": "best",
            "outtmpl": dest_path,
            "quiet": True,
            "no_warnings": True,
            "http_headers": headers or {},
            "progress_hooks": [progress_hook],
        })
        with yt_dlp.YoutubeDL(options) as ydl:
            ydl.download([url])
        return 0 if os.path.exists(dest_path) and os.path.getsize(dest_path) > 0 else 1
    except Exception as error:
        print(f"[FAIL] In-process HLS download failed: {error}")
        return 1


def prompt_video_quality(url, quality_arg=None, cookie=None):
    """Interactively or via argument select video quality / format CLI."""
    if not HAS_YTDLP:
        return 'best[ext=mp4]/best/b', False

    # Standard format map (prefers progressive single-file streams with video+audio fallback)
    format_map = {
        '1': ('best[ext=mp4]/best/bestvideo+bestaudio/best', 'Best Available (Auto - Highest Quality)'),
        '2160p': ('best[height<=2160][ext=mp4]/best[height<=2160]/bestvideo[height<=2160]+bestaudio/best', '2160p (4K)'),
        '1440p': ('best[height<=1440][ext=mp4]/best[height<=1440]/bestvideo[height<=1440]+bestaudio/best', '1440p (2K)'),
        '1080p': ('best[height<=1080][ext=mp4]/best[height<=1080]/bestvideo[height<=1080]+bestaudio/best', '1080p (Full HD)'),
        '720p': ('best[height<=720][ext=mp4]/best[height<=720]/bestvideo[height<=720]+bestaudio/best', '720p (HD)'),
        '480p': ('best[height<=480][ext=mp4]/best[height<=480]/bestvideo[height<=480]+bestaudio/best', '480p (SD)'),
        '360p': ('best[height<=360][ext=mp4]/best[height<=360]/bestvideo[height<=360]+bestaudio/best', '360p (Low)'),
        '240p': ('best[height<=240][ext=mp4]/best[height<=240]/bestvideo[height<=240]+bestaudio/best', '240p'),
        '144p': ('best[height<=144][ext=mp4]/best[height<=144]/bestvideo[height<=144]+bestaudio/best', '144p'),
        'audio': ('bestaudio/best', 'Audio Only (MP3/M4A)'),
        'mp3': ('bestaudio/best', 'Audio Only (MP3)'),
    }

    # If quality was specified via CLI flag -q / --quality or App Engine
    if quality_arg:
        q_clean = str(quality_arg).lower().strip()
        if q_clean in format_map:
            return format_map[q_clean][0], (q_clean in ('audio', 'mp3'))
        m = re.search(r'(\d{3,4})p', q_clean)
        if m:
            p_key = m.group(0)
            if p_key in format_map:
                return format_map[p_key][0], False
        if any(w in q_clean for w in ('audio', 'mp3', 'm4a', 'bitrate', 'kbps')):
            return format_map['audio'][0], True
        if any(w in q_clean for w in ('best', 'auto', 'ask', 'highest')):
            return format_map['1'][0], False
        if q_clean.isdigit():
            val = int(q_clean)
            if val in (2160, 1440, 1080, 720, 480, 360, 240, 144):
                key = f"{val}p"
                return format_map[key][0], False

    # Extract available qualities using yt-dlp
    print("> Extracting video info and available qualities...")
    title = "Video"
    available_heights = []
    try:
        ydl_opts = get_ytdlp_opts({'quiet': True, 'no_warnings': True}, cookie=cookie)
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            if not info:
                raise Exception("yt-dlp returned no video info (content may be private or members-only)")
            title = info.get('title', 'Video')
            formats = info.get('formats', []) or []
            h_set = set()
            for f in formats:
                if isinstance(f, dict):
                    h = f.get('height')
                    if h and f.get('vcodec') != 'none':
                        h_set.add(h)
            available_heights = sorted(list(h_set), reverse=True)
    except Exception as e:
        print(f"[WARN] Warning: Could not fetch format metadata ({e}). Using auto best quality.")
        return 'best[ext=mp4]/best/b', False

    # If non-interactive shell (stdin not a tty), return default
    if not sys.stdin.isatty():
        print(f" Video: {title}")
        print("> Non-interactive mode detected. Downloading in Best Available quality.")
        return format_map['1'][0], False

    print("\n" + "="*55)
    print(f"  Video: {title}")
    print("="*55)
    print(" Select Quality / Format:")
    
    options = []
    # Option 1: Best Available
    options.append(('1', format_map['1'][0], 'Best Available (Auto - Highest Quality)', False))
    
    # Map height options dynamically
    num = 2
    height_num_map = {}
    for h in available_heights:
        label = f"{h}p"
        if h == 2160: label += " (4K)"
        elif h == 1440: label += " (2K)"
        elif h == 1080: label += " (Full HD)"
        elif h == 720: label += " (HD)"
        elif h == 480: label += " (SD)"
        
        fmt_str = f"bestvideo[height<={h}]+bestaudio/best[height<={h}]/best"
        options.append((str(num), fmt_str, label, False))
        height_num_map[str(num)] = fmt_str
        height_num_map[f"{h}p"] = fmt_str
        height_num_map[str(h)] = fmt_str
        num += 1
        
    # Audio Option
    audio_num = str(num)
    options.append((audio_num, 'bestaudio/best', 'Audio Only (MP3/M4A)', True))
    
    for opt_num, _, label, _ in options:
        print(f"   [{opt_num}] {label}")
    print("="*55)

    try:
        user_input = input(f"> Select choice [1-{len(options)}] (Default: 1): ").strip()
    except (EOFError, KeyboardInterrupt):
        user_input = '1'

    if not user_input:
        user_input = '1'

    for opt_num, fmt_str, _, is_audio in options:
        if user_input == opt_num:
            return fmt_str, is_audio
            
    if user_input.lower() in height_num_map:
        return height_num_map[user_input.lower()], False

    if user_input.lower() in ('audio', 'mp3', 'm4a'):
        return 'bestaudio/best', True

    print("> Selection invalid or default picked. Downloading Best Available quality.")
    return format_map['1'][0], False

def mux_video_and_audio(video_path, audio_path, output_path):
    """Mux separate video and audio tracks into a single playable container without quality loss."""
    if not os.path.exists(video_path) or not os.path.exists(audio_path):
        return False

    temp_out = output_path
    needs_rename = False
    if output_path in (video_path, audio_path):
        temp_out = output_path + ".mux_temp.mp4"
        needs_rename = True

    muxed = False
    # 1. Try Android Native MediaMuxer via EmbeddedEngine
    try:
        from com.erawat.arc import EmbeddedEngine
        mux_fn = getattr(EmbeddedEngine, 'mux', None) or getattr(getattr(EmbeddedEngine, 'INSTANCE', None), 'mux', None)
        if mux_fn and mux_fn(str(video_path), str(audio_path), str(temp_out)):
            if os.path.exists(temp_out) and os.path.getsize(temp_out) > 0:
                muxed = True
    except Exception as e:
        print(f"[WARN] Native Android muxing: {e}")

    # 2. Try FFmpeg (Desktop / CLI / Termux)
    if not muxed and shutil.which("ffmpeg"):
        try:
            cmd = ["ffmpeg", "-y", "-i", video_path, "-i", audio_path, "-c", "copy", temp_out]
            res = subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            if res.returncode == 0 and os.path.exists(temp_out) and os.path.getsize(temp_out) > 0:
                muxed = True
        except Exception as e:
            print(f"[WARN] FFmpeg muxing: {e}")

    if muxed:
        try: os.remove(video_path)
        except Exception: pass
        try: os.remove(audio_path)
        except Exception: pass
        if needs_rename:
            if os.path.exists(output_path):
                try: os.remove(output_path)
                except Exception: pass
            os.rename(temp_out, output_path)
        return True

    if needs_rename and os.path.exists(temp_out):
        try: os.remove(temp_out)
        except Exception: pass
    return False

def mux_multiple_audio_tracks(video_path, audio_paths, output_path):
    """Mux a video track with multiple audio tracks into a single container."""
    if not os.path.exists(video_path) or not audio_paths:
        return False
    valid_audios = [p for p in audio_paths if os.path.exists(p) and os.path.getsize(p) > 0]
    if not valid_audios:
        return False

    temp_out = output_path
    needs_rename = False
    if output_path in ([video_path] + valid_audios):
        temp_out = output_path + ".mux_multi_temp.mp4"
        needs_rename = True

    muxed = False
    # 1. Try Android Native MediaMuxer via EmbeddedEngine
    try:
        from com.erawat.arc import EmbeddedEngine
        mux_fn = getattr(EmbeddedEngine, 'muxMultiple', None) or getattr(getattr(EmbeddedEngine, 'INSTANCE', None), 'muxMultiple', None)
        if mux_fn:
            # Pass as Python list which Chaquopy marshals to java.util.List
            if mux_fn(str(video_path), [str(p) for p in valid_audios], str(temp_out)):
                if os.path.exists(temp_out) and os.path.getsize(temp_out) > 0:
                    muxed = True
    except Exception as e:
        print(f"[WARN] Native Android multi-track muxing: {e}")

    # 2. Try FFmpeg (Desktop / CLI / Termux)
    if not muxed and shutil.which("ffmpeg"):
        try:
            cmd = ["ffmpeg", "-y", "-i", video_path]
            for a in valid_audios:
                cmd.extend(["-i", a])
            cmd.extend(["-map", "0:v"])
            for idx in range(len(valid_audios)):
                cmd.extend(["-map", f"{idx + 1}:a"])
            cmd.extend(["-c", "copy", temp_out])
            res = subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            if res.returncode == 0 and os.path.exists(temp_out) and os.path.getsize(temp_out) > 0:
                muxed = True
        except Exception as e:
            print(f"[WARN] FFmpeg multi-muxing: {e}")

    if muxed:
        try: os.remove(video_path)
        except Exception: pass
        for a in valid_audios:
            try: os.remove(a)
            except Exception: pass
        if needs_rename:
            if os.path.exists(output_path):
                try: os.remove(output_path)
                except Exception: pass
            os.rename(temp_out, output_path)
        return True

    if needs_rename and os.path.exists(temp_out):
        try: os.remove(temp_out)
        except Exception: pass
    return False

def handle_youtube_or_video(url, target_dir=None, quality_arg=None, cookie=None, standalone=False, job_id=None, cache_dir=None, out_name=None, audio_lang=None):
    if not HAS_YTDLP:
        print("[WARN] yt-dlp is not installed. Falling back to generic download...")
        return False

    # Sanitize YouTube watch URLs: strip radio/mix playlists that hang yt-dlp
    if "youtube.com/watch" in url or "youtu.be/" in url:
        parsed_u = urllib.parse.urlparse(url)
        q_params = urllib.parse.parse_qs(parsed_u.query)
        if "list" in q_params and (q_params["list"][0].startswith("RD") or "start_radio" in q_params):
            q_params.pop("list", None)
            q_params.pop("start_radio", None)
            q_params.pop("index", None)
            q_params.pop("pp", None)
            new_query = urllib.parse.urlencode(q_params, doseq=True)
            url = urllib.parse.urlunparse((parsed_u.scheme, parsed_u.netloc, parsed_u.path, parsed_u.params, new_query, parsed_u.fragment))

    is_playlist_req = ("/playlist" in url or "list=PL" in url or "list=OLAK5uy" in url or "list=UU" in url or "list=FL" in url or "list=RD" in url or "list=RDM" in url)
    is_playlist = is_playlist_req

    if is_playlist_req:
        q_clean = str(quality_arg or "").lower().strip()
        is_audio_only = any(w in q_clean for w in ("audio", "mp3", "m4a", "bitrate"))
        if is_audio_only:
            format_spec = "bestaudio[ext=m4a]/bestaudio/best"
        else:
            m = re.search(r"(\d{3,4})p", q_clean)
            if m:
                h = m.group(1)
                format_spec = f"bestvideo[height<={h}][ext=mp4]+bestaudio[ext=m4a]/best[height<={h}][ext=mp4]/best[height<={h}]/best"
            else:
                format_spec = "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best"
    else:
        try:
            format_spec, is_audio_only = prompt_video_quality(url, quality_arg=quality_arg, cookie=cookie)
        except Exception as e:
            print(f"[FAIL] Error extracting video metadata: {e}")
            return False

    out_dir = target_dir or DEFAULT_DIR
    os.makedirs(out_dir, exist_ok=True)
    
    jid = job_id or get_current_job_id()

    def ytdlp_progress_hook(d):
        if jid and str(jid) in CANCELLED_DOWNLOAD_JOBS:
            raise Exception("Download cancelled by user")

        if jid and str(jid) in PAUSED_DOWNLOAD_JOBS:
            render_download_progress(0.0, "Paused", "Paused", "--", job_id=jid)
            evt = PAUSE_EVENTS.get(str(jid))
            while str(jid) in PAUSED_DOWNLOAD_JOBS:
                if str(jid) in CANCELLED_DOWNLOAD_JOBS:
                    raise Exception("Download cancelled by user")
                time.sleep(0.5)

        status = d.get('status')
        info = d.get('info_dict') or {}
        p_idx = info.get('playlist_index')
        p_total = info.get('n_entries') or info.get('playlist_count')
        is_p = bool(p_total and p_total > 1)

        vcode = info.get('vcodec') or ''
        acode = info.get('acodec') or ''
        is_video_only_stream = (vcode != 'none' and acode == 'none')
        is_audio_only_stream = (vcode == 'none' and acode != 'none')

        if status == 'downloading':
            total = d.get('total_bytes') or d.get('total_bytes_estimate') or 0
            downloaded = d.get('downloaded_bytes') or 0
            speed = d.get('speed') or 0
            eta = d.get('eta') or 0
            raw_percent = (downloaded / total * 100) if total > 0 else 0.0
            mb_done = downloaded / (1024 * 1024)
            mb_total = total / (1024 * 1024) if total > 0 else 0
            speed_mb = (speed / (1024 * 1024)) if speed else 0
            spd_str = f"{speed_mb:.2f}MB/s" if speed_mb else "--"
            eta_m, eta_s = divmod(int(eta) if eta else 0, 60)
            eta_str = f"{eta_m:02d}:{eta_s:02d}" if eta else "--"
            tot_str = f"{mb_done:.1f}/{mb_total:.1f}MB" if total > 0 else f"{mb_done:.1f}MB"

            if is_p:
                cur_idx = p_idx or 1
                item_pct = raw_percent / 100.0
                overall_pct = round(((cur_idx - 1 + item_pct) / p_total) * 100, 1)
                p_label = f"[{cur_idx}/{p_total}] {tot_str}"
                render_download_progress(overall_pct, p_label, spd_str, eta_str, job_id=jid)
            else:
                if is_video_only_stream:
                    scaled_pct = round(raw_percent * 0.88, 1)
                    render_download_progress(scaled_pct, tot_str, spd_str, eta_str, job_id=jid)
                elif is_audio_only_stream:
                    scaled_pct = round(88.0 + (raw_percent * 0.10), 1)
                    render_download_progress(scaled_pct, f"Audio {tot_str}", spd_str, eta_str, job_id=jid)
                else:
                    render_download_progress(round(raw_percent, 1), tot_str, spd_str, eta_str, job_id=jid)
        elif status == 'finished':
            if is_p:
                cur_idx = p_idx or 1
                if cur_idx < p_total:
                    overall_pct = round((cur_idx / p_total) * 100, 1)
                    render_download_progress(overall_pct, f"[{cur_idx}/{p_total}] Done", "Done", "--", job_id=jid)
                else:
                    render_download_progress(100.0, "Complete", "Done", "0s", job_id=jid)
            else:
                if is_video_only_stream:
                    render_download_progress(88.0, "Audio track downloading...", "Processing", "--", job_id=jid)
                elif is_audio_only_stream:
                    render_download_progress(98.5, "Muxing media...", "Muxing", "--", job_id=jid)
                else:
                    render_download_progress(100.0, "Complete", "Done", "0s", job_id=jid)

    stage_dir = None
    if is_playlist:
        p_title = clean_filename(out_name or 'YouTube Playlist')
        playlist_dir = os.path.join(out_dir, p_title)
        os.makedirs(playlist_dir, exist_ok=True)
        out_dir = playlist_dir
        outtmpl_pattern = os.path.join(out_dir, '%(title)s.%(ext)s')
    else:
        stage_dir = os.path.join(out_dir, f".arc_stage_{jid}")
        os.makedirs(stage_dir, exist_ok=True)
        q_text = str(quality_arg or "").lower()
        q_match = re.search(r"(4320|2160|1440|1080|720|480|360|240|144)\s*p?", q_text)
        cap = int(q_match.group(1)) if q_match else 1080
        q_tag = f"{cap}p"

        if out_name and not any(out_name.lower().startswith(p) for p in ('youtube video', 'video', 'download', 'stream')):
            clean_base = clean_filename(out_name, default_ext="", quality=q_tag)
            outtmpl_pattern = os.path.join(stage_dir, f"{clean_base}.%(ext)s")
        else:
            outtmpl_pattern = os.path.join(stage_dir, f"%(title)s{{{q_tag}}}.%(ext)s")

    clean_audio_lang = str(audio_lang or "").strip().lower()
    if clean_audio_lang and clean_audio_lang not in ("default", "original", "none"):
        audio_sel = f"bestaudio[language^={clean_audio_lang[:2]}][ext=m4a]/bestaudio[format_note*={clean_audio_lang}][ext=m4a]/bestaudio[language^={clean_audio_lang[:2]}]/bestaudio[format_note*={clean_audio_lang}]/bestaudio[ext=m4a]/bestaudio"
    else:
        audio_sel = "bestaudio[ext=m4a]/bestaudio"

    if is_audio_only:
        format_spec = f"{audio_sel}/bestaudio/best"
    else:
        format_spec = (
            f"best[height<={cap}][vcodec^=avc][ext=mp4]/"
            f"best[height<={cap}][ext=mp4]/"
            f"best[height<={cap}]/"
            f"bestvideo[height<={cap}][vcodec^=avc][ext=mp4]+{audio_sel}/"
            f"bestvideo[height<={cap}][ext=mp4]+{audio_sel}/"
            f"bestvideo[height<={cap}]+{audio_sel}/"
            f"bestvideo[height<={cap}]+bestaudio/"
            f"bestvideo+{audio_sel}/"
            f"bestvideo+bestaudio/"
            f"best"
        )

    print(f"> Direct YouTube download [{format_spec if isinstance(format_spec, str) else 'audio+video'}]...")
    yt_opts_dict = {
        'format': format_spec,
        'outtmpl': outtmpl_pattern,
        'quiet': False,
        'no_warnings': True,
        'progress_hooks': [ytdlp_progress_hook],
        'http_headers': {'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15'},
        'nopart': False,
        'continuedl': True,
        'writesubtitles': False,
        'writeautomaticsub': False,
        'socket_timeout': 30,
        'retries': 20,
        'fragment_retries': 20,
        'http_chunk_size': 10485760,
        'merge_output_format': 'mp4',
    }
    ydl_opts = get_ytdlp_opts(yt_opts_dict, cookie=cookie)

    max_attempts = 5
    for attempt in range(1, max_attempts + 1):
        try:
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                info = ydl.extract_info(url, download=True)
                if not info:
                    raise Exception("yt-dlp could not extract video info. Content may be members-only or private.")
                
                # Check if result is a playlist
                if info.get('_type') == 'playlist' or 'entries' in info or is_playlist:
                    filename = out_dir
                else:
                    filename = ydl.prepare_filename(info)

                if jid and str(jid) in CANCELLED_DOWNLOAD_JOBS:
                    if filename and os.path.exists(filename):
                        try: os.remove(filename)
                        except Exception: pass
                    return None

                # Verify target file actually exists on disk!
                if not is_playlist:
                    if not filename or not os.path.exists(filename) or os.path.getsize(filename) == 0:
                        base, _ = os.path.splitext(filename or "")
                        found_path = None
                        for ext in ('.mp4', '.mkv', '.webm', '.m4a'):
                            alt = base + ext
                            if os.path.exists(alt) and os.path.getsize(alt) > 0:
                                found_path = alt
                                break
                        if found_path:
                            filename = found_path
                        else:
                            raise RuntimeError(f"Downloaded media file not found on device: {filename}")

                # Move from stage_dir to out_dir and ensure clean filename
                if stage_dir and filename and os.path.exists(filename) and os.path.isfile(filename):
                    cur_dir, cur_fn = os.path.split(filename)
                    cleaned_fn = clean_filename(cur_fn, quality=q_tag if not is_audio_only else None)
                    final_dest = os.path.join(out_dir, cleaned_fn)
                    safe_move(filename, final_dest)
                    filename = final_dest
                    shutil.rmtree(stage_dir, ignore_errors=True)
                elif filename and os.path.exists(filename) and os.path.isfile(filename):
                    cur_dir, cur_fn = os.path.split(filename)
                    cleaned_fn = clean_filename(cur_fn, quality=q_tag if not is_audio_only else None)
                    if cleaned_fn != cur_fn:
                        cleaned_path = os.path.join(cur_dir, cleaned_fn)
                        safe_move(filename, cleaned_path)
                        filename = cleaned_path

                # Purge any transient cache or fragment parts
                _cleanup_download_cache(cache_dir, out_dir, job_id=jid, keep_path=filename)
                sync_to_sdcard_if_needed(filename)
            if jid:
                render_download_progress(100.0, "Complete", "Done", "0s", job_id=jid)
            return filename
        except Exception as e:
            if jid and str(jid) in CANCELLED_DOWNLOAD_JOBS:
                if stage_dir and os.path.exists(stage_dir):
                    shutil.rmtree(stage_dir, ignore_errors=True)
                return None
            err_str = str(e)
            print(f"\n[FAIL] Download error encountered: {err_str}")

            if not check_network():
                wait_for_network()
                continue

            if "Conversion failed" in err_str or "Postprocessing" in err_str or "obu_forbidden_bit" in err_str or "Invalid data" in err_str:
                print("\n" + "!"*60)
                print(" [WARN] Postprocessing failed: Video stream fragments were corrupted due to network disconnection.")
                print(" > Cleaning corrupted partial fragments & retrying with network-resilient H.264 format...")
                print("!"*60 + "\n")
                for f in os.listdir(out_dir):
                    if ".f" in f or ".temp" in f or ".part" in f or ".ytdl" in f:
                        try:
                            os.remove(os.path.join(out_dir, f))
                            print(f"  - Cleaned partial fragment: {f}")
                        except Exception:
                            pass
                ydl_opts['format'] = 'best[vcodec!=none][acodec!=none]/best/b'
                continue

            if "Requested format is not available" in err_str:
                if ydl_opts.get('format') != 'best[vcodec!=none][acodec!=none]/best/b':
                    print("[WARN] Requested format not available. Falling back immediately to progressive stream...")
                    ydl_opts['format'] = 'best[vcodec!=none][acodec!=none]/best/b'
                    continue

            if '403' in err_str or 'Forbidden' in err_str or 'bot' in err_str.lower():
                print("[WARN] Retrying with fallback player clients...")
                ydl_opts['extractor_args'] = {'youtube': {'player_client': ['tv_embedded', 'android', 'ios']}}
                continue

            if attempt < max_attempts:
                print(f"> Retrying download (attempt {attempt}/{max_attempts})...")
                time.sleep(3)
            else:
                if stage_dir and os.path.exists(stage_dir):
                    shutil.rmtree(stage_dir, ignore_errors=True)
                print("[FAIL] Download failed after retries.")
                return False
    return False

def handle_studocu(url, out_name=None, custom_headers=None, target_dir=None):
    print("> Detected Studocu link...")
    headers = {**HEADERS, **(custom_headers or {})}
    
    parsed = urllib.parse.urlparse(url)
    qs = urllib.parse.parse_qs(parsed.query)
    
    filename = out_name
    if not filename and 'filename' in qs:
        filename = clean_filename(qs['filename'][0], default_ext=".pdf")
    elif not filename and 'documentId' in qs:
        filename = clean_filename(f"studocu_{qs['documentId'][0]}", default_ext=".pdf")
    elif not filename:
        path_parts = [p for p in parsed.path.split('/') if p]
        if path_parts:
            filename = clean_filename(path_parts[-1], default_ext=".pdf")
        else:
            filename = "studocu_document.pdf"
            
    cookie = os.environ.get("STUDOCU_COOKIE", "")
    if cookie and 'Cookie' not in headers:
        headers['Cookie'] = cookie

    try:
        return download_file_stream(url, filename, custom_headers=headers, target_dir=target_dir)
    except requests.exceptions.HTTPError as e:
        if e.response is not None and e.response.status_code in (401, 403):
            print("\n[FAIL] Access Denied (403 Forbidden) by Studocu.")
            print("> Studocu download links require active browser authentication cookies.")
            print("> Usage with Cookie:")
            print("   dl \"<URL>\" -c \"session_cookie_string\"")
            print("   or set STUDOCU_COOKIE=\"...\" in your shell.\n")
            return None
        raise e

def _get_fallback_tg_creds():
    api_id = os.environ.get("TELEGRAM_API_ID", "").strip()
    api_hash = os.environ.get("TELEGRAM_API_HASH", "").strip()
    session = os.environ.get("TELEGRAM_SESSION", "").strip()
    if not (api_id and api_hash and session):
        paths_to_check = [
            os.path.join(os.path.dirname(__file__), "credentials.local.json"),
            os.path.join(os.path.dirname(__file__), "assets", "credentials.local.json"),
            os.path.expanduser("~/.telegram_credentials.json"),
            os.path.join(os.environ.get("HOME", ""), ".telegram_credentials.json"),
            "/data/data/com.erawat.arc/files/telegram_credentials.json",
            "/data/user/0/com.erawat.arc/files/telegram_credentials.json",
        ]
        files_dir = getattr(sys, "_arc_files_dir", None)
        if files_dir:
            paths_to_check.insert(0, os.path.join(files_dir, "telegram_credentials.json"))
        for p in paths_to_check:
            if p and os.path.exists(p):
                try:
                    with open(p, "r", encoding="utf-8") as f:
                        data = json.load(f)
                        api_id = api_id or str(data.get("api_id", "")).strip()
                        api_hash = api_hash or str(data.get("api_hash", "")).strip()
                        session = session or str(data.get("session", "")).strip()
                except Exception:
                    pass
    return api_id, api_hash, session

def _save_fallback_tg_creds(api_id, api_hash, session):
    targets = [
        os.path.expanduser("~/.telegram_credentials.json"),
        os.path.join(os.environ.get("HOME", ""), ".telegram_credentials.json"),
        "/data/data/com.erawat.arc/files/telegram_credentials.json",
        "/data/user/0/com.erawat.arc/files/telegram_credentials.json",
    ]
    files_dir = getattr(sys, "_arc_files_dir", None)
    if files_dir:
        targets.insert(0, os.path.join(files_dir, "telegram_credentials.json"))
    payload = {"api_id": str(api_id), "api_hash": str(api_hash), "session": str(session)}
    for t in targets:
        try:
            d = os.path.dirname(t)
            if d and not os.path.exists(d):
                os.makedirs(d, exist_ok=True)
            with open(t, "w", encoding="utf-8") as f:
                json.dump(payload, f)
        except Exception:
            pass

def _clear_fallback_tg_creds():
    targets = [
        os.path.expanduser("~/.telegram_credentials.json"),
        os.path.join(os.environ.get("HOME", ""), ".telegram_credentials.json"),
        "/data/data/com.erawat.arc/files/telegram_credentials.json",
        "/data/user/0/com.erawat.arc/files/telegram_credentials.json",
    ]
    files_dir = getattr(sys, "_arc_files_dir", None)
    if files_dir:
        targets.insert(0, os.path.join(files_dir, "telegram_credentials.json"))
    for t in targets:
        try:
            if os.path.exists(t):
                os.remove(t)
        except Exception:
            pass

def handle_telegram(url, out_name=None, target_dir=None, telegram_config=None, job_id=None):
    jid = job_id or get_current_job_id()
    print("> Detected Telegram link...")
    parsed = urllib.parse.urlparse(url)
    path_parts = [p for p in parsed.path.split('/') if p]
    
    chat_id = None
    msg_id = None
    
    if len(path_parts) >= 2:
        if path_parts[0] == 's':
            chat_id = path_parts[1]
            if len(path_parts) >= 3 and path_parts[2].isdigit():
                msg_id = int(path_parts[2])
        elif path_parts[0] == 'c' and len(path_parts) >= 3:
            try:
                raw_c = path_parts[1].lstrip('-')
                if not raw_c.startswith("100"):
                    raw_c = f"100{raw_c}"
                chat_id = int(f"-{raw_c}")
            except ValueError:
                chat_id = path_parts[1]
            if path_parts[-1].isdigit():
                msg_id = int(path_parts[-1])
        else:
            chat_id = path_parts[0]
            if path_parts[1].isdigit():
                msg_id = int(path_parts[1])

    fb_id, fb_hash, fb_sess = _get_fallback_tg_creds()
    provided = telegram_config if isinstance(telegram_config, dict) else {}
    raw_api_id = str(provided.get("api_id") or fb_id or "30909654").strip()
    TELEGRAM_API_ID = int(re.sub(r'\D+', '', raw_api_id) or "30909654")
    TELEGRAM_API_HASH = str(provided.get("api_hash") or fb_hash or "d4b340a406c5c1ed1d7f26d44749602b").strip()
    raw_sess = str(provided.get("session") or fb_sess or "").strip()
    if raw_sess:
        missing = len(raw_sess) % 4
        if missing:
            raw_sess += '=' * (4 - missing)
    TELEGRAM_SESSION = raw_sess
    telegram_credentials_available = bool(TELEGRAM_API_ID and TELEGRAM_API_HASH and TELEGRAM_SESSION)

    try:
        if not telegram_credentials_available:
            raise RuntimeError("Telegram API credentials are not configured")
        import asyncio
        try:
            asyncio.get_event_loop()
        except RuntimeError:
            asyncio.set_event_loop(asyncio.new_event_loop())
        from pyrogram import Client
        import logging
        logging.getLogger("pyrogram").setLevel(logging.ERROR)

        async def pyrogram_dl():
            app = Client("d_py_tg_client", api_id=TELEGRAM_API_ID, api_hash=TELEGRAM_API_HASH, session_string=TELEGRAM_SESSION, in_memory=True)
            await app.start()
            try:
                def progress_cb(current, total):
                    if total > 0:
                        percent = (current / total) * 100
                        mb_done = current / (1024 * 1024)
                        mb_total = total / (1024 * 1024)
                        render_download_progress(percent, f"{mb_done:.1f}/{mb_total:.1f}MB", job_id=jid)
                    else:
                        mb_done = current / (1024 * 1024)
                        render_download_progress(100.0, f"{mb_done:.1f}MB", job_id=jid)

                # Resolve chat / username peer upfront
                resolved_chat_id = chat_id
                if isinstance(chat_id, str) and not chat_id.lstrip('-').isdigit():
                    try:
                        resolved_chat = await app.get_chat(chat_id)
                        if resolved_chat and resolved_chat.id:
                            resolved_chat_id = resolved_chat.id
                    except Exception as rc_err:
                        print(f"[DEBUG] Username get_chat notice for @{chat_id}: {rc_err}")

                # Prime peer cache if needed so private channels have their access_hash cached in memory
                if chat_id:
                    try:
                        import pyrogram.raw as raw
                        try:
                            await app.storage.get_peer_by_id(resolved_chat_id)
                        except KeyError:
                            r = await app.invoke(
                                raw.functions.messages.GetDialogs(
                                    offset_date=0,
                                    offset_id=0,
                                    offset_peer=raw.types.InputPeerEmpty(),
                                    limit=100,
                                    hash=0
                                )
                            )
                            if r and (r.chats or r.users):
                                await app.fetch_peers(r.chats + r.users)
                    except Exception as d_err:
                        print(f"[DEBUG] Telegram peer priming notice: {d_err}")

                msg = None
                if msg_id:
                    try:
                        msg = await app.get_messages(resolved_chat_id, msg_id)
                    except Exception as gm_err:
                        print(f"[WARN] Direct get_messages notice: {gm_err}")
                        try:
                            c = await app.get_chat(chat_id)
                            if c:
                                resolved_chat_id = c.id
                                msg = await app.get_messages(c.id, msg_id)
                        except Exception:
                            pass

                if msg and not getattr(msg, "empty", False) and msg.media:
                    out_dir = target_dir or DEFAULT_DIR
                    os.makedirs(out_dir, exist_ok=True)

                    # Extract real filename and extension from Telegram media metadata
                    real_fname = None
                    if msg.document and msg.document.file_name:
                        real_fname = msg.document.file_name
                    elif msg.video:
                        real_fname = msg.video.file_name or f"telegram_video_{msg_id}.mp4"
                    elif msg.audio:
                        real_fname = msg.audio.file_name or f"telegram_audio_{msg_id}.mp3"
                    elif msg.photo:
                        real_fname = f"telegram_photo_{msg_id}.jpg"
                    elif msg.voice:
                        real_fname = f"telegram_voice_{msg_id}.ogg"
                    elif msg.animation:
                        real_fname = msg.animation.file_name or f"telegram_animation_{msg_id}.mp4"

                    if real_fname:
                        if not out_name or any(gen in out_name.lower() for gen in ["telegram document", "telegram media", "telegram file", "resolved video"]) or not os.path.splitext(out_name)[1]:
                            final_fname = real_fname
                        else:
                            final_fname = out_name
                    else:
                        final_fname = out_name or f"telegram_file_{msg_id}"

                    # For video files, format name according to user rule: NAME(QUALITY).extension
                    base, ext = os.path.splitext(final_fname)
                    if ext.lower() in ('.mp4', '.mkv', '.webm', '.mov', '.avi'):
                        final_fname = f"{base}(Best){ext}"

                    target_file = os.path.join(out_dir, final_fname)
                    print(f"> Fetching Telegram media '{final_fname}' from {chat_id}/{msg_id} via Pyrogram...")
                    file_path = None
                    try:
                        file_path = await app.download_media(msg, file_name=target_file, progress=progress_cb)
                    except Exception as dl_err:
                        print(f"\n[WARN] Pyrogram download notice: {dl_err}")
                        if isinstance(target_file, str) and not target_file.endswith("/"):
                            if os.path.exists(target_file) and os.path.getsize(target_file) > 0:
                                file_path = target_file
                        else:
                            for item in os.listdir(out_dir):
                                item_p = os.path.join(out_dir, item)
                                if os.path.isfile(item_p) and os.path.getsize(item_p) > 0:
                                    file_path = item_p
                                    break
                    if file_path:
                        print(f"\n[OK] Telegram media downloaded to: {file_path}")
                        sync_to_sdcard_if_needed(file_path)
                        if jid:
                            render_download_progress(100.0, total_str="Complete", speed_str="Done", eta_str="0s", job_id=jid)
                        return file_path
                elif msg and not getattr(msg, "empty", False):
                    # Message exists but has no direct media attachment (e.g. text/update post)
                    post_text = (msg.text or msg.caption or "").strip()
                    if post_text:
                        print("\n" + "="*65)
                        print(f"  Telegram Post {chat_id}/#{msg_id} (Text Only):")
                        print("="*65)
                        print(post_text[:500])
                        print("="*65 + "\n")

                    # Scan adjacent messages for referenced media attachments (preceding 1-5, following 1-2)
                    adjacent_media = []
                    check_offsets = [-1, -2, -3, -4, -5, 1, 2]
                    for offset in check_offsets:
                        cand_id = msg_id + offset
                        if cand_id <= 0:
                            continue
                        try:
                            adj_msg = await app.get_messages(resolved_chat_id, cand_id)
                            if adj_msg and not getattr(adj_msg, "empty", False) and adj_msg.media:
                                fname = "media_file"
                                fsize = 0
                                if adj_msg.document:
                                    fname = adj_msg.document.file_name or f"document_{cand_id}"
                                    fsize = adj_msg.document.file_size
                                elif adj_msg.video:
                                    fname = adj_msg.video.file_name or f"video_{cand_id}.mp4"
                                    fsize = adj_msg.video.file_size
                                elif adj_msg.audio:
                                    fname = adj_msg.audio.file_name or f"audio_{cand_id}.mp3"
                                    fsize = adj_msg.audio.file_size
                                mb_sz = f" ({fsize / (1024*1024):.1f} MB)" if fsize else ""
                                adjacent_media.append((adj_msg, fname, mb_sz))
                        except Exception:
                            pass

                    if adjacent_media:
                        print(f"> Found {len(adjacent_media)} media attachment(s) in adjacent messages:")
                        for idx, (m, fn, sz) in enumerate(adjacent_media, 1):
                            print(f"   [{idx}] {fn}{sz} (Msg #{m.id})")

                        # Auto-download primary attachment in automated / app environment
                        chosen_msg, chosen_name, _ = adjacent_media[0]
                        out_dir = target_dir or DEFAULT_DIR
                        os.makedirs(out_dir, exist_ok=True)
                        target_file = os.path.join(out_dir, out_name or chosen_name)
                        print(f"\n> Automatically downloading primary attachment: {chosen_name}...")
                        fpath = await app.download_media(chosen_msg, file_name=target_file, progress=progress_cb)
                        if fpath:
                            print(f"\n[OK] Telegram media downloaded to: {fpath}")
                            sync_to_sdcard_if_needed(fpath)
                            if jid:
                                render_download_progress(100.0, total_str="Complete", speed_str="Done", eta_str="0s", job_id=jid)
                            return fpath
                    else:
                        summary_txt = post_text[:80] if post_text else "No text"
                        raise Exception(f"Telegram post #{msg_id} is a text announcement with no media attachments: \"{summary_txt}\"")
                else:
                    raise Exception(f"Message #{msg_id} was deleted or does not exist in Telegram channel {chat_id}.")
            finally:
                try:
                    await app.stop()
                except Exception:
                    pass
                try:
                    for task in asyncio.all_tasks():
                        if task is not asyncio.current_task():
                            task.cancel()
                except Exception:
                    pass
            return None

        try:
            loop = asyncio.get_event_loop()
            if loop.is_closed():
                loop = asyncio.new_event_loop()
                asyncio.set_event_loop(loop)
        except RuntimeError:
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
        res_path = loop.run_until_complete(pyrogram_dl())
        if res_path:
            return res_path
        if path_parts and path_parts[0] == 'c':
            raise Exception(f"Telegram channel {chat_id} did not yield a downloadable media file for message #{msg_id}.")
    except Exception as e:
        err_str = str(e)
        err_lower = err_str.lower()
        if "session_revoked" in err_lower or "auth_key_unregistered" in err_lower or "401 session_revoked" in err_lower:
            raise Exception("Telegram session has expired or was revoked. Please log in again in ARC Settings.")
        if "channel_private" in err_lower or "user_not_participant" in err_lower or "chat_admin_required" in err_lower:
            raise Exception("This Telegram channel is private and your account is not a member of it.")
        if "peer_id_invalid" in err_lower or "channel_invalid" in err_lower:
            raise Exception("Telegram private channel could not be accessed. Please ensure your connected Telegram account has joined this channel/group.")
        if path_parts and path_parts[0] == 'c':
            raise Exception(f"Failed to access private Telegram media: {err_str}")
        print(f"[WARN] Pyrogram unavailable or not configured ({e}). Using Telegram web embed fallback...")

    try:
        parsed_url = urllib.parse.urlparse(url)
        p_parts = [p for p in parsed_url.path.split('/') if p]
        chan_name = None
        m_id = None
        if len(p_parts) >= 2:
            chan_name = p_parts[1] if p_parts[0] == 's' else p_parts[0]
            m_id = p_parts[-1]
        elif chat_id and msg_id:
            chan_name = str(chat_id)
            m_id = str(msg_id)

        if chan_name and m_id:
            for embed_url in [f"https://t.me/s/{chan_name}/{m_id}", f"https://t.me/{chan_name}/{m_id}?embed=1"]:
                try:
                    res = requests.get(embed_url, headers=HEADERS, timeout=12)
                    if res.status_code == 200:
                        soup = BeautifulSoup(res.text, 'html.parser')
                        video = soup.find('video')
                        if video and video.get('src'):
                            return download_file_stream(video['src'], clean_filename(out_name or f"telegram_{chan_name}_{m_id}.mp4"), target_dir=target_dir)
                        audio = soup.find('audio')
                        if audio and audio.get('src'):
                            return download_file_stream(audio['src'], clean_filename(out_name or f"telegram_{chan_name}_{m_id}.mp3"), target_dir=target_dir)
                        doc = soup.find('a', class_=re.compile(r'tgme_widget_message_document'))
                        if doc and doc.get('href'):
                            href = doc['href']
                            if not href.startswith(('https://t.me/', 'http://t.me/')):
                                return download_file_stream(href, clean_filename(out_name or f"telegram_{chan_name}_{m_id}"), target_dir=target_dir)
                except Exception:
                    pass
    except Exception as e:
        print(f"[ERROR] Web embed fallback: {e}")

    raise Exception("Could not download Telegram link media.")

_NETMIRROR_META_CACHE = {}

def handle_netmirror(url, out_name=None, target_dir=None, direct_link_only=False, quality_arg=None, action_mode=None, target_season=None, target_episode=None, download_scope="episode", job_id=None, cache_dir=None, audio_lang=None):
    jid = job_id or get_current_job_id()
    if direct_link_only:
        action_mode = 'direct_link_only'
    if not action_mode:
        action_mode = 'stream_and_download'
    parsed = urllib.parse.urlparse(url)
    parts = [p for p in parsed.path.split('/') if p]
    
    media_type = 'movie'
    media_id = None
    
    for p in reversed(parts):
        nums = re.findall(r'\d+', p)
        if nums:
            media_id = nums[-1]
            break

    lower_parts = [p.lower() for p in parts]
    if 'tv' in lower_parts or 'series' in lower_parts or 's' in lower_parts:
        media_type = 'tv'
    elif 'movie' in lower_parts or 'm' in lower_parts:
        media_type = 'movie'
        
    if not media_id:
        raise Exception("Could not parse NetMirror media ID from URL.")

    import hmac, hashlib, time, base64

    def we(s):
        return s.encode('utf-8').decode('latin1')

    ts = int(time.time())
    secret_key = b'net###@@sss'
    msg = f'{media_id}:{ts}'.encode('utf-8')
    sig = hmac.new(secret_key, msg, hashlib.sha256).hexdigest()

    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Referer': 'https://netmirror.plus/',
        'Origin': 'https://netmirror.plus'
    }

    results = []
    if media_id in _NETMIRROR_META_CACHE:
        results = _NETMIRROR_META_CACHE[media_id].get("results", [])
        media_type = _NETMIRROR_META_CACHE[media_id].get("media_type", media_type)
    else:
        hosts_to_try = ['api2.imdb4.shop', 'api2.imdb3.shop']
        types_to_try = list(dict.fromkeys([media_type, 'tv', 'series', 'movie']))
        for h in hosts_to_try:
            for m_type in types_to_try:
                try:
                    api_url = f'https://{h}/api/{m_type}/{media_id}'
                    res = requests.get(api_url, headers=headers, timeout=5)
                    if res.status_code == 200:
                        data = res.json()
                        results = data.get('results', [])
                        if results:
                            media_type = m_type
                            _NETMIRROR_META_CACHE[media_id] = {"results": results, "media_type": media_type}
                            break
                except Exception:
                    continue
            if results:
                break
    if not results:
        raise Exception(f"No metadata found on NetMirror for ID {media_id}")
        
    meta = results[0]
    title = (meta.get('title') or meta.get('name') or f"netmirror_{media_id}").strip()
    year = meta.get('release_date', '').split(',')[-1].strip() if meta.get('release_date') else '2024'
    embed_json = meta.get('embed_json') or []
    season_list = meta.get('season') or []
    
    api_media_type = str(meta.get('media_type') or meta.get('type') or '').lower().strip()
    ep_count = sum(int(s.get('ep', 1)) for s in season_list) if season_list else (len(embed_json) if embed_json else 0)
    has_seasons = bool(season_list and (len(season_list) > 1 or int(season_list[0].get('ep', 1)) > 1 or int(season_list[0].get('se', 1)) > 1))

    if api_media_type == 'movie' or media_type == 'movie':
        is_tv_series = False
        media_type = 'movie'
    elif api_media_type == 'tv' or media_type == 'tv' or has_seasons or ep_count > 1 or bool(re.search(r'\bS\d+\b', title, re.I)):
        is_tv_series = True
        media_type = 'tv'
    else:
        is_tv_series = False

    poster = meta.get('backdrop_path') or meta.get('poster') or ''
    direct_stream_url = None
    stream_headers = {
        'User-Agent': HEADERS['User-Agent'],
        'Referer': 'https://netmirror.plus/',
        'Origin': 'https://netmirror.plus'
    }

    clean_title = (meta.get('title') or meta.get('name') or title).strip()
    show_base = re.sub(r'[\s\-_]+S\d+(-S\d+)?(\s*E\d+)?.*', '', clean_title, flags=re.I)
    show_base = re.sub(r'[\s\-_]+Season\s*\d+(-S\d+)?.*', '', show_base, flags=re.I)
    show_base = re.sub(r'[\s\-_]+Complete\s*Series.*', '', show_base, flags=re.I)
    show_base = re.sub(r'\[.*?\]', '', show_base)
    show_base = re.sub(r'\(.*?\)', '', show_base)
    show_base = re.sub(r'\s+', ' ', show_base).strip()
    if not show_base:
        show_base = clean_title

    formatted_seasons = []
    if is_tv_series:
        if season_list:
            for s in season_list:
                formatted_seasons.append({
                    "season": int(s.get('se', 1)),
                    "episodes": int(s.get('ep', 1))
                })
        elif embed_json:
            se_dict = {}
            for item in embed_json:
                se_num = int(item.get('se', 1))
                se_dict[se_num] = se_dict.get(se_num, 0) + 1
            for se_num, ep_count in sorted(se_dict.items()):
                formatted_seasons.append({
                    "season": se_num,
                    "episodes": ep_count
                })
        if not formatted_seasons:
            formatted_seasons = [{"season": 1, "episodes": 1}]

    # TV Series batch download support (season or entire series)
    if is_tv_series and action_mode == 'download_only' and download_scope in ('season', 'series'):
        print(f"\n> Starting batch series download for {download_scope}: {show_base}")
        episodes_to_download = []
        if download_scope == 'season':
            s_target = int(target_season if target_season is not None else 1)
            matching_s = [s for s in formatted_seasons if s['season'] == s_target]
            total_eps = matching_s[0]['episodes'] if matching_s else 1
            for ep_num in range(1, total_eps + 1):
                episodes_to_download.append((s_target, ep_num))
        else:  # series
            for s_info in formatted_seasons:
                s_target = s_info['season']
                for ep_num in range(1, s_info['episodes'] + 1):
                    episodes_to_download.append((s_target, ep_num))

        downloaded_paths = []
        total_items = len(episodes_to_download)
        print(f"> Total episodes to download: {total_items}")
        jid = job_id or get_current_job_id()

        base_dir = target_dir or DEFAULT_DIR
        series_dir = os.path.join(base_dir, clean_filename(show_base))
        os.makedirs(series_dir, exist_ok=True)

        prefetch_executor = concurrent.futures.ThreadPoolExecutor(max_workers=1)

        def prefetch_single_ep(s, ep):
            try:
                return handle_netmirror(
                    url,
                    action_mode='direct_link_only',
                    target_season=s,
                    target_episode=ep,
                    quality_arg=quality_arg,
                )
            except Exception:
                return None

        next_prefetch_future = prefetch_executor.submit(
            prefetch_single_ep, episodes_to_download[0][0], episodes_to_download[0][1]
        ) if episodes_to_download else None

        is_batch_cancelled = False
        for idx, (s_n, ep_n) in enumerate(episodes_to_download, 1):
            if jid and str(jid) in CANCELLED_DOWNLOAD_JOBS:
                print("\n[STOP] Batch download cancelled by user.")
                is_batch_cancelled = True
                break

            print(f"\n[{idx}/{total_items}] Preparing S{s_n:02d}E{ep_n:02d}...")
            batch_pct = round(((idx - 1) / total_items) * 100, 1)
            if jid:
                render_download_progress(batch_pct, total_str=f"Ep {idx}/{total_items}", speed_str="--", eta_str="--", job_id=jid)

            try:
                ep_res = next_prefetch_future.result(timeout=10) if next_prefetch_future else None
            except Exception:
                ep_res = None

            if idx < total_items:
                next_s, next_ep = episodes_to_download[idx]
                next_prefetch_future = prefetch_executor.submit(prefetch_single_ep, next_s, next_ep)
            else:
                next_prefetch_future = None

            if not ep_res or not ep_res.get('url'):
                ep_res = handle_netmirror(
                    url,
                    action_mode='direct_link_only',
                    target_season=s_n,
                    target_episode=ep_n,
                    quality_arg=quality_arg,
                )

            if not ep_res or not ep_res.get('url'):
                print(f"[FAIL] Could not resolve stream for S{s_n:02d}E{ep_n:02d}")
                continue

            ep_url = ep_res['url']
            ep_headers = ep_res.get('stream_headers') or {}

            season_dir = os.path.join(series_dir, f"Season {s_n:02d}")
            os.makedirs(season_dir, exist_ok=True)
            ep_clean_name = clean_filename(f"{show_base} S{s_n:02d}E{ep_n:02d}", default_ext=".mp4", quality=quality_arg)
            print(f"[{idx}/{total_items}] Downloading {ep_clean_name} into {season_dir}...")
            ep_path = download_file_stream(
                ep_url,
                ep_clean_name,
                custom_headers=ep_headers,
                target_dir=season_dir,
                cache_dir=cache_dir,
                job_id=jid,
                batch_info=(idx, total_items)
            )
            if jid and str(jid) in CANCELLED_DOWNLOAD_JOBS:
                print("\n[STOP] Batch download cancelled by user.")
                is_batch_cancelled = True
                break

            if ep_path:
                downloaded_paths.append(ep_path)

        try:
            prefetch_executor.shutdown(wait=False)
        except Exception:
            pass

        if is_batch_cancelled:
            if jid:
                ACTIVE_DOWNLOAD_PROGRESS[str(jid)] = {
                    "percent": 0.0,
                    "downloaded_str": "--",
                    "total_str": "Cancelled",
                    "speed": "--",
                    "eta": "--",
                    "status": "Cancelled",
                }
            return None

        if jid:
            render_download_progress(100.0, total_str="Complete", speed_str="Done", eta_str="0s", job_id=jid)
        return downloaded_paths[0] if downloaded_paths else None

    se_val = str(target_season if target_season is not None else 1) if is_tv_series else '0'
    ep_val = str(target_episode if target_episode is not None else 1) if is_tv_series else '0'
    tv_val = '1' if is_tv_series else ''
    display_title = f"{show_base} S{int(se_val)}E{int(ep_val)}" if is_tv_series else clean_title

    if not embed_json or is_tv_series:
        # Resolve stream via vidsync / NetMirror external & watchbox server APIs
        kind_label = f"series episode S{se_val}E{ep_val}" if is_tv_series else "movie"
        print(f"\n> Resolving direct stream for {kind_label}: {display_title}")
        
        target_lang = None
        clean_alang = str(audio_lang or "").strip().lower()
        if clean_alang and clean_alang not in ("default", "original", "none", "all"):
            if 'hi' in clean_alang or 'hin' in clean_alang: target_lang = 'hi'
            elif 'ta' in clean_alang or 'tam' in clean_alang: target_lang = 'ta'
            elif 'te' in clean_alang or 'tel' in clean_alang: target_lang = 'te'
            elif 'bn' in clean_alang or 'ben' in clean_alang: target_lang = 'bn'
            elif 'id' in clean_alang or 'ind' in clean_alang: target_lang = 'id'
            elif 'en' in clean_alang or 'eng' in clean_alang: target_lang = 'en'
            else: target_lang = clean_alang[:2]
        if not target_lang:
            title_lower = title.lower()
            if '[hindi]' in title_lower or 'hindi' in title_lower:
                target_lang = 'hi'
            elif '[tamil]' in title_lower or 'tamil' in title_lower:
                target_lang = 'ta'
            elif '[telugu]' in title_lower or 'telugu' in title_lower:
                target_lang = 'te'
            elif '[bengali]' in title_lower or 'bengali' in title_lower:
                target_lang = 'bn'
            elif '[indonesian]' in title_lower or 'indonesian' in title_lower:
                target_lang = 'id'

        tm_id = meta.get('tm_id') or media_id
        subjectid = meta.get('subjectid') or media_id
        dp = meta.get('dp') or ''

        lang_label = f"[{target_lang}]" if target_lang else "default"
        print(f"  [1/4]  Querying media metadata & audio tracks... [OK]")

        candidate_streams = []
        qualities_dict = {}

        # Step 2: Primary Indian Stream Gateways (Vidsync & Hindi nodes)
        if tm_id:
            sys.stdout.write("  [2/4]  Querying primary Indian stream nodes (Vidsync & Hindi servers)... ")
            sys.stdout.flush()
            try:
                if is_tv_series:
                    vidsync_api = f"https://vidsync.pro/api/stream?type=tv&id={tm_id}&season={se_val}&episode={ep_val}"
                else:
                    vidsync_api = f"https://vidsync.pro/api/stream?type={media_type}&id={tm_id}"
                v_headers = {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
                    'Referer': 'https://vidsync.pro/',
                    'Origin': 'https://vidsync.pro'
                }
                v_res = requests.get(vidsync_api, headers=v_headers, timeout=3.5)
                if v_res.status_code == 200:
                    v_data = v_res.json()
                    sources = v_data.get('sources', [])
                    if sources:
                        matching = [s for s in sources if s.get('audioLanguage') == target_lang] if target_lang else []
                        if not matching:
                            matching = sources
                        for s in matching:
                            q_raw = s.get('quality', 'Auto')
                            m_q = re.search(r'(4k|1080p|720p|480p|360p)', q_raw, re.I)
                            q_k = m_q.group(1).lower() if m_q else q_raw.lower()
                            u = s.get('url') or s.get('directUrl') or s.get('permanent_m3u8_url') or s.get('rawUrl')
                            if u:
                                lbl = f"{q_k.upper()} (HD)" if q_k == '720p' else (f"{q_k.upper()} (SD)" if q_k == '480p' else f"{q_k.upper()} (Full HD)" if q_k == '1080p' else q_k.upper())
                                qualities_dict[q_k] = {'label': lbl, 'url': u, 'dom': 'https://vidsync.pro', 'headers': s.get('headers')}
                                if u not in [c[0] for c in candidate_streams]:
                                    candidate_streams.append((u, 'https://vidsync.pro'))
                        print(f"[OK] Found {len(matching)} Indian source(s)")
                    else:
                        print("[FAIL] (no Indian stream sources)")
                else:
                    print(f"[FAIL] (status {v_res.status_code})")
            except Exception:
                print("[FAIL] (offline/timed out)")

        # Step 3: Multi-Server CDN manifests (Queried only after Indian nodes or as fallback)
        if tm_id:
            if candidate_streams:
                sys.stdout.write("  [3/4]  Querying multi-server CDN manifests... ")
            else:
                print("  [WARN] Primary Indian stream nodes offline or rate-limiting. Searching multi-server global CDN fallback manifests...")
                sys.stdout.write("  [3/4]  Querying multi-server global CDN fallback manifests... ")
            sys.stdout.flush()
            na_b64 = base64.b64encode(we(clean_title).encode('latin1')).decode('utf-8')
            sig_tm = hmac.new(secret_key, f'{tm_id}:{ts}'.encode('utf-8'), hashlib.sha256).hexdigest()
            wb_params = {
                'id': subjectid, 'se': se_val, 'ep': ep_val, 'dp': dp, 'na': na_b64, 'year': year,
                'tm_id': tm_id, 'ts': str(ts), 'sig': sig_tm, 'nid': media_id, 'exten': '1',
                'tv': tv_val, 'token': '', 'warp': 'false', '_': str(int(time.time() * 1000))
            }
            wb_query = urllib.parse.urlencode(wb_params)
            
            for dom in ['https://bet.watch22.shop', 'https://limit.watch22.shop', 'https://webvp.watch21.shop']:
                try:
                    wb_url = f"{dom}/play/watchbox.php?{wb_query}"
                    wb_r = requests.get(wb_url, headers=headers, timeout=4)
                    if wb_r.status_code == 200 and len(wb_r.text) > 200:
                        # 1. Extract from dl-item (contains resolution + filesize, e.g. "1080P 2GB")
                        for m_dl in re.finditer(r'<div class=[\"\']dl-item[\"\']>([^<]+)<[^>]+onclick=[\"\']myFunction_dl\([\"\']([^\"\']+)[\"\']', wb_r.text):
                            lbl = m_dl.group(1).strip()
                            u = m_dl.group(2).strip()
                            m_q = re.search(r'(4k|1080p|720p|480p|360p)', lbl, re.I)
                            q_key = m_q.group(1).lower() if m_q else lbl.lower()
                            if q_key not in qualities_dict or '.mp4' in u.lower():
                                qualities_dict[q_key] = {'label': lbl, 'url': u, 'dom': dom}
                            if u not in [c[0] for c in candidate_streams]:
                                candidate_streams.append((u, dom))

                        # 2. Extract selector items in quality_new
                        for m_sel in re.finditer(r'html\s*:\s*[\"\']([^\"\']*(?:1080|720|480|360|4k)[^\"\']*)[\"\'].*?url\s*:\s*[\"\'](https?://[^\"\'\`\s]+)[\"\']', wb_r.text, re.I | re.DOTALL):
                            lbl = m_sel.group(1).strip()
                            u = m_sel.group(2).strip()
                            m_q = re.search(r'(4k|1080p|720p|480p|360p)', lbl, re.I)
                            q_key = m_q.group(1).lower() if m_q else lbl.lower()
                            if q_key not in qualities_dict or '.mp4' in u.lower():
                                qualities_dict[q_key] = {'label': lbl, 'url': u, 'dom': dom}
                            if u not in [c[0] for c in candidate_streams]:
                                candidate_streams.append((u, dom))

                        # 3. Extract art.url
                        for m_art in re.finditer(r'art\.url\s*=\s*play_url\([\"\']([^\"\']+)[\"\']', wb_r.text):
                            u = m_art.group(1).strip()
                            if u and u.startswith(('http://', 'https://')) and u not in [c[0] for c in candidate_streams]:
                                candidate_streams.append((u, dom))

                        # 4. Extract art.tvVideoUrl
                        for m_tv in re.finditer(r'art\.tvVideoUrl\s*=\s*[\"\']([^\"\']+)[\"\']', wb_r.text):
                            raw_val = m_tv.group(1)
                            dec_url = decrypt_netmirror_tv_url(raw_val) if 'encrypt=true' in raw_val else raw_val
                            if dec_url and dec_url.startswith(('http://', 'https://')) and dec_url not in [c[0] for c in candidate_streams]:
                                candidate_streams.append((dec_url, dom))

                        # 4. Extract TvUrl
                        for m_tvurl in re.finditer(r'TvUrl\s*:\s*[\"\']([^\"\']+)[\"\']', wb_r.text):
                            raw_val = m_tvurl.group(1)
                            dec_url = decrypt_netmirror_tv_url(raw_val) if 'encrypt=true' in raw_val else raw_val
                            if dec_url and dec_url.startswith(('http://', 'https://')) and dec_url not in [c[0] for c in candidate_streams]:
                                candidate_streams.append((dec_url, dom))
                        break
                except Exception:
                    pass

            if candidate_streams:
                print(f"[OK] Found {len(candidate_streams)} stream candidate(s)")
            else:
                print("[FAIL] (no direct stream nodes found)")

            # Step 4: Validate candidate streams and prompt quality
            if candidate_streams:
                sys.stdout.write("  [4/4]  Validating media stream health... ")
                sys.stdout.flush()

                valid_qualities = {}
                q_order = {'4k': 6, '2160p': 6, '1080p': 5, '720p': 4, '480p': 3, '360p': 2, 'auto': 1}
                def _probe(item):
                    q_k, q_info = item
                    chk_h = get_stream_headers_for_url(q_info['url'])
                    try:
                        chk = requests.head(q_info['url'], headers=chk_h, timeout=2.5)
                        if chk.status_code in (200, 206, 302, 301, 429):
                            return q_k, q_info
                    except Exception:
                        pass
                    return q_k, q_info

                with concurrent.futures.ThreadPoolExecutor(max_workers=min(5, len(qualities_dict) or 1)) as ex:
                    for pres in ex.map(_probe, list(qualities_dict.items())):
                        if pres:
                            valid_qualities[pres[0]] = pres[1]

                if valid_qualities:
                    print("[OK] Verified (status 200)")
                    sorted_q = sorted(valid_qualities.keys(), key=lambda q: q_order.get(q, 0), reverse=True)
                    chosen_q_key = None
                    if quality_arg:
                        for k in sorted_q:
                            if quality_arg.lower() in k:
                                chosen_q_key = k
                                break
                    if not chosen_q_key:
                        if action_mode not in ('direct_link_only', 'direct') and len(sorted_q) > 1 and sys.stdin.isatty():
                            print("\n" + "="*65)
                            print(f" Select Video Quality for: {clean_title}")
                            print("="*65)
                            for q_idx, q_k in enumerate(sorted_q, 1):
                                q_obj = valid_qualities[q_k]
                                badge = "" if q_k in ('4k', '1080p') else ("" if q_k == '720p' else "")
                                rec = "  [Recommended]" if q_idx == 1 else ""
                                print(f"   [{q_idx}] {badge} {q_obj['label']}{rec}")
                            print("="*65)
                            try:
                                q_sel = input(f"> Select quality [1-{len(sorted_q)}] (Default: 1): ").strip()
                                if not q_sel or not q_sel.isdigit() or not (1 <= int(q_sel) <= len(sorted_q)):
                                    q_sel = '1'
                            except (KeyboardInterrupt, EOFError):
                                return None
                            chosen_q_key = sorted_q[int(q_sel) - 1]
                        else:
                            chosen_q_key = sorted_q[0]

                    chosen_info = valid_qualities[chosen_q_key]
                    direct_stream_url = chosen_info['url']
                    stream_headers = get_stream_headers_for_url(direct_stream_url, {'Referer': f"{chosen_info['dom']}/"})
                    print(f"> Selected Quality: {chosen_info['label']}")
                else:
                    for c_url, c_dom in candidate_streams:
                        chk_headers = get_stream_headers_for_url(c_url, {'Referer': f"{c_dom}/"})
                        try:
                            chk = requests.head(c_url, headers=chk_headers, timeout=3.5)
                            if chk.status_code in (200, 206, 302, 301):
                                direct_stream_url = c_url
                                stream_headers = chk_headers
                                print(f"[OK] Verified (status {chk.status_code})")
                                break
                        except Exception:
                            pass
                    if not direct_stream_url and candidate_streams:
                        direct_stream_url = candidate_streams[0][0]
                        stream_headers = get_stream_headers_for_url(direct_stream_url, {'Referer': f"{candidate_streams[0][1]}/"})
                        print("[OK] Decrypted stream ready")

        # Step 3b: Fallback to external.php
        if not direct_stream_url and tm_id:
            try:
                sig_tm = hmac.new(secret_key, f'{tm_id}:{ts}'.encode('utf-8'), hashlib.sha256).hexdigest()
                ext_headers = {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
                    'Referer': 'https://netmirror.plus/',
                    'Origin': 'https://netmirror.plus'
                }
                ext_url = f'https://webvp.watch21.shop/play/external.php?tm_id={tm_id}&nid={media_id}&tm_type={media_type}&ts={ts}&sig={sig_tm}&exten=1'
                r_ext = requests.get(ext_url, headers=ext_headers, timeout=5)
                if r_ext.status_code == 200:
                    m = re.search(r'const servers\s*=\s*(\[\s*\{.*?\}\s*\]);', r_ext.text, re.DOTALL)
                    if m:
                        import json
                        servers = json.loads(m.group(1))
                        for s in servers:
                            s_url = s.get('url', '')
                            if 'watchcinemo.php' in s_url:
                                r_c = requests.get(s_url, headers={'User-Agent': ext_headers['User-Agent'], 'Referer': ext_url}, timeout=5)
                                m_art = re.search(r'art\.tvVideoUrl\s*=\s*[\"\']([^\"\']+)[\"\']', r_c.text)
                                if m_art:
                                    val = m_art.group(1)
                                    direct_stream_url = decrypt_netmirror_tv_url(val) if 'encrypt=true' in val else val
                                    if direct_stream_url:
                                        break
                                match = re.search(r'https?://[^\s\"\'\`<>]+\.(?:r2\.dev|mp4|m3u8)[^\s\"\'\`<>]*', r_c.text)
                                if match:
                                    direct_stream_url = match.group(0)
                                    break
            except Exception:
                pass

        if not direct_stream_url:
            print("\n" + "="*65)
            print(f" [WARN] No active streaming servers found for: {clean_title}")
            print("   (CDN nodes are currently rate-limiting or offline)")
            print("="*65)
            return None

        clean_name = clean_filename(out_name or display_title, default_ext=".mp4", quality=quality_arg)

    else:
        # TV show with multiple episodes
        target_item = embed_json[0]
        if len(embed_json) > 1 and sys.stdin.isatty():
            print("\n" + "="*55)
            print(f"  NetMirror: {title}")
            print("="*55)
            print(" Available Episodes:")
            for idx, item in enumerate(embed_json, 1):
                se = item.get('se', '1')
                ep = item.get('ep', '1')
                size = item.get('size', 'N/A')
                print(f"   [{idx}] Season {se} Episode {ep} ({size})")
            print("="*55)
            try:
                ep_choice = input(f"> Select episode [1-{len(embed_json)}] (Default: 1): ").strip()
                if ep_choice.isdigit() and 1 <= int(ep_choice) <= len(embed_json):
                    target_item = embed_json[int(ep_choice) - 1]
            except (KeyboardInterrupt, EOFError):
                pass

        na_b64 = base64.b64encode(we(title).encode('latin1')).decode('utf-8')
        params = {
            'url': target_item['url'],
            'size': target_item.get('size', ''),
            'se': str(target_item.get('se', 1)),
            'ep': str(target_item.get('ep', 1)),
            'name': target_item.get('name', 'watchdrivehub'),
            'year': year,
            'na': na_b64,
            'tm_id': media_id,
            'ts': str(ts),
            'sig': sig,
            'nid': '0',
            'exten': '1',
            'tv': '',
            'token': ''
        }

        query_str = urllib.parse.urlencode(params)
        
        # Try servers in order: dv.watch22.shop -> webvp.watch21.shop -> bet.watch22.shop
        domains = ['https://dv.watch22.shop', 'https://webvp.watch21.shop', 'https://bet.watch22.shop']
        player_html = ""
        for dom in domains:
            player_url = f"{dom}/play/{target_item.get('name', 'watchdrivehub')}.php?{query_str}"
            try:
                r = requests.get(player_url, headers=headers, timeout=10)
                if r.status_code == 200 and 'art-video-player' in r.text:
                    player_html = r.text
                    break
            except Exception:
                pass

        if not player_html:
            raise Exception("Could not retrieve NetMirror player page from stream servers.")

        match = re.search(r'art\.tvVideoUrl\s*=\s*[\"\']([^\"\']+)[\"\']', player_html) or re.search(r'play_url\([\"\'\`](https?://[^\s\"\'\`]+)[\"\'\`]\)', player_html)
        if match:
            candidate = match.group(1) if match.lastindex else match.group(0)
            if 'encrypt=true' in candidate:
                dec_cand = decrypt_netmirror_tv_url(candidate)
                if dec_cand:
                    direct_stream_url = dec_cand
            else:
                direct_stream_url = candidate
        if not direct_stream_url:
            match = re.search(r'https?://[^\s\"\'\`]+\.(?:r2\.dev|mp4|m3u8)[^\s\"\'\`]*', player_html)
            if match:
                direct_stream_url = match.group(0)

        if not direct_stream_url:
            print("\n" + "="*65)
            print(f" [WARN] No active streaming servers could be retrieved for: {title}")
            print("="*65)
            return None

        clean_name = clean_filename(out_name or display_title, default_ext=".mp4", quality=quality_arg)

    if action_mode in ('direct_link_only', 'direct'):
        print(f"> Stream verified: {display_title}")
    else:
        print(f"> Stream ready: {display_title}")
        print("\n" + "="*65)
        print(" [STREAM] DIRECT PLAYABLE STREAM READY:")
        print("="*65)
        print(f" [URL] {direct_stream_url}")
        if stream_headers.get('Referer'):
            print(f" [REFERER] {stream_headers['Referer']}")
        print("="*65 + "\n")
    
    # Save .m3u8 stream shortcut ONLY if action_mode is stream or stream_and_download
    if action_mode in ('stream_only', 'stream', 'stream_and_download'):
        save_m3u8_stream_file(direct_stream_url, clean_name, target_dir=target_dir)

    if action_mode in ('direct_link_only', 'direct'):
        sorted_qualities = []
        quality_map = {}
        q_order = {'4k': 6, '2160p': 6, '1080p': 5, '720p': 4, '480p': 3, '360p': 2, '240p': 1, 'auto': 0}

        def get_q_rank(text):
            lower = str(text).lower()
            ranks = [val for k, val in q_order.items() if k in lower]
            return max(ranks) if ranks else 0

        target_dict = {}
        if 'qualities_dict' in locals() and qualities_dict:
            target_dict.update(qualities_dict)
        if 'valid_qualities' in locals() and valid_qualities:
            target_dict.update(valid_qualities)
        if target_dict:
            sorted_keys = sorted(target_dict.keys(), key=lambda k: get_q_rank(target_dict[k].get('label') or k), reverse=True)
            for q_k in sorted_keys:
                q_info = target_dict[q_k]
                lbl = q_info.get('label') or q_k.upper()
                if lbl not in sorted_qualities:
                    sorted_qualities.append(lbl)
                if q_info.get('url'):
                    quality_map[lbl] = q_info['url']

        if sorted_qualities:
            best_target_url = quality_map.get(sorted_qualities[0]) or direct_stream_url
            quality_map["Best Available"] = best_target_url
            if "Best Available" not in sorted_qualities:
                sorted_qualities.insert(0, "Best Available")
        else:
            sorted_qualities = ["Best Available"]
            quality_map["Best Available"] = direct_stream_url

        # Spin up local stream proxy to ensure VLC/external player streaming doesn't 403 on CDN
        proxy_stream_url = start_local_stream_proxy(direct_stream_url, custom_headers=stream_headers)

        audio_choices_list = []
        if 'show_base' in locals() and show_base:
            try:
                raw_peers = search_netmirror(show_base)
                g_peers = group_and_deduplicate_results(raw_peers, query=show_base)
                seen_langs = set()
                for g in g_peers:
                    for lang, a_item in g.get("audios", {}).items():
                        if lang not in seen_langs:
                            seen_langs.add(lang)
                            audio_choices_list.append({
                                "language": lang,
                                "url": a_item.get("url", ""),
                                "title": a_item.get("title", ""),
                                "id": a_item.get("id"),
                            })
            except Exception:
                pass

        def get_nm_aud_prio(c):
            l = c.get("language", "").lower()
            if "hindi" in l: return 1
            if "english" in l: return 2
            if "original" in l or "default" in l: return 3
            w_list = ["tamil", "telugu", "bengali", "spanish", "french", "arabic", "portuguese", "russian", "german", "japanese", "korean", "chinese", "indonesian"]
            for i, w in enumerate(w_list):
                if w in l: return 10 + i
            return 100

        audio_choices_list.sort(key=get_nm_aud_prio)

        return {
            "url": direct_stream_url,
            "direct_url": direct_stream_url,
            "proxy_url": proxy_stream_url,
            "target_url": url,
            "title": display_title,
            "source": "TV Series" if is_tv_series else "Movie",
            "year": year,
            "poster": poster,
            "quality": quality_arg if (quality_arg and quality_arg.lower() != 'best' and quality_arg in sorted_qualities) else (sorted_qualities[0] if sorted_qualities else "Best Available"),
            "qualities": sorted_qualities,
            "quality_urls": quality_map,
            "audios": audio_choices_list,
            "audio_choices": audio_choices_list,
            "stream_headers": stream_headers,
            "is_series": is_tv_series,
            "seasons": formatted_seasons if is_tv_series else [],
            "season": int(se_val) if is_tv_series else None,
            "episode": int(ep_val) if is_tv_series else None,
        }

    if action_mode in ('stream_only', 'stream', 'stream_and_download'):
        launch_player(direct_stream_url, player_choice="vlc", custom_headers=stream_headers, title=display_title)

    if action_mode in ('stream_only', 'stream'):
        print("\n" + "="*65)
        print("  STREAMING ACTIVE IN VLC PLAYER")
        print("="*65)
        print(f"  Media: {clean_title}")
        print(f"  Stream Proxy: {LOCAL_PROXY_URL or direct_stream_url}")
        print("  Full seek/scrub bar support enabled")
        print("="*65)
        print(" > VLC is currently playing the stream directly on your tablet.")
        print(" > Press [Enter] or [Ctrl+C] to stop stream and return to menu...")
        try:
            input()
        except (KeyboardInterrupt, EOFError):
            pass
        print("\n[OK] Streaming session ended.")
        return direct_stream_url

    # Direct download execution: support HLS m3u8 and MP4 multi-threaded download via aria2c.
    try:
        if is_tv_series:
            base_d = target_dir or DEFAULT_DIR
            season_d = os.path.join(base_d, clean_filename(show_base), f"Season {int(se_val):02d}")
            os.makedirs(season_d, exist_ok=True)
            target_dir = season_d
            if not out_name:
                clean_name = f"{show_base} S{int(se_val):02d}E{int(ep_val):02d}.mp4"

        dest_path = get_dest_path(clean_name, target_dir)
        if '.m3u8' in direct_stream_url:
            print("> Downloading HLS Stream via multi-threaded aria2c / yt-dlp...")
            if not clean_name.lower().endswith('.mp4'):
                clean_name = os.path.splitext(clean_name)[0] + '.mp4'
            dest_path = get_dest_path(clean_name, target_dir)
            cmd = [
                'yt-dlp',
                '--downloader', 'aria2c',
                '--downloader-args', 'aria2c:-x 5 -s 5 -k 1M -j 5 --min-split-size=1M --summary-interval=0 --console-log-level=warn',
                '--no-part',
                '--concurrent-fragments', '5',
                '--fragment-retries', '10',
                '--retries', '10',
                '--newline',
                '-o', dest_path,
                direct_stream_url
            ]
            for hk, hv in stream_headers.items():
                cmd.extend(['--add-header', f'{hk}:{hv}'])
            if shutil.which('yt-dlp'):
                ret = run_command_with_clean_progress(cmd)
            else:
                ret = download_hls_with_python(direct_stream_url, dest_path, stream_headers)
            if ret == -1:
                print("[WARN] Download cancelled.")
                return None
            if ret == 0 and os.path.exists(dest_path) and os.path.getsize(dest_path) > 0:
                sync_to_sdcard_if_needed(dest_path)
                dest_file = dest_path
            else:
                dest_file = download_file_stream(direct_stream_url, clean_name, custom_headers=stream_headers, target_dir=target_dir, cache_dir=cache_dir, job_id=jid)
        elif shutil.which('aria2c'):
            print(f"> Downloading via multi-threaded aria2c (16x): {clean_name}...")
            out_dir = os.path.dirname(dest_path) or DEFAULT_DIR
            out_file = os.path.basename(dest_path)
            cmd = [
                'aria2c',
                '-x', '16',
                '-s', '16',
                '-j', '16',
                '-k', '1M',
                '--min-split-size=1M',
                '--summary-interval=1',
                '--console-log-level=warn',
                '--allow-overwrite=true',
                '--auto-file-renaming=false',
                '-d', out_dir,
                '-o', out_file
            ]
            for hk, hv in stream_headers.items():
                cmd.append(f'--header={hk}: {hv}')
            cmd.append(direct_stream_url)
            ret = run_command_with_clean_progress(cmd)
            if ret == -1:
                print("[WARN] Download cancelled.")
                return None
            if ret == 0 and os.path.exists(dest_path) and os.path.getsize(dest_path) > 0:
                sync_to_sdcard_if_needed(dest_path)
                dest_file = dest_path
            else:
                dest_file = download_file_stream(direct_stream_url, clean_name, custom_headers=stream_headers, target_dir=target_dir, cache_dir=cache_dir, job_id=jid)
        else:
            dest_file = download_file_stream(direct_stream_url, clean_name, custom_headers=stream_headers, target_dir=target_dir, cache_dir=cache_dir, job_id=jid)
    finally:
        pass

    return dest_file

def handle_generic(url, out_name=None, custom_headers=None, target_dir=None):
    headers = {**HEADERS, **(custom_headers or {})}
    try:
        res = requests.head(url, headers=headers, allow_redirects=True, timeout=10)
        content_type = res.headers.get('content-type', '').lower()
        
        if 'text/html' in content_type:
            res_get = requests.get(url, headers=headers, timeout=15)
            soup = BeautifulSoup(res_get.text, 'html.parser')
            
            pdf_link = soup.find('a', href=re.compile(r'\.pdf(\?|$)', re.IGNORECASE))
            if pdf_link:
                target_url = urllib.parse.urljoin(url, pdf_link['href'])
                title = out_name or (soup.title.string.strip() if soup.title else "download")
                return download_file_stream(target_url, clean_filename(title, ".pdf"), custom_headers=custom_headers, target_dir=target_dir)
    except Exception:
        pass
            
    parsed = urllib.parse.urlparse(url)
    base = os.path.basename(parsed.path)
    if not base or '.' not in base:
        base = out_name or "downloaded_file"
    filename = clean_filename(out_name or base)
    
    return download_file_stream(url, filename, custom_headers=custom_headers, target_dir=target_dir)

import json
import threading
import concurrent.futures

class ProcessMonitor:
    def __init__(self):
        self.logs = []
        self.running = False
        self._thread = None
        self.lock = threading.Lock()
        self.status = "Idle"

    def log(self, message):
        with self.lock:
            timestamp = time.strftime("[%H:%M:%S]")
            entry = f"{timestamp} {message}"
            self.logs.append(entry)
            if len(self.logs) > 100:
                self.logs.pop(0)

    def set_status(self, status):
        with self.lock:
            self.status = status

    def start_key_listener(self):
        if not sys.stdin.isatty():
            return
        self.running = True
        self._thread = threading.Thread(target=self._listen_keys, daemon=True)
        self._thread.start()

    def stop_key_listener(self):
        self.running = False

    def _listen_keys(self):
        import select, termios, tty
        fd = sys.stdin.fileno()
        try:
            old_settings = termios.tcgetattr(fd)
        except Exception:
            return
            
        try:
            tty.setcbreak(fd)
            while self.running:
                rlist, _, _ = select.select([sys.stdin], [], [], 0.3)
                if rlist:
                    ch = sys.stdin.read(1)
                    if ch in ('\x0f', 'o', 'O'):
                        self.show_logs()
        except Exception:
            pass
        finally:
            try:
                termios.tcsetattr(fd, termios.TCSADRAIN, old_settings)
            except Exception:
                pass

    def show_logs(self):
        with self.lock:
            print("\n" + "="*60)
            print(f"  LIVE PROCESS LOGS | Status: {self.status}")
            print("="*60)
            if not self.logs:
                print(" (No process logs recorded yet)")
            else:
                for line in self.logs[-25:]:
                    print(f"  {line}")
            print("="*60 + "\n")

PROCESS_MONITOR = ProcessMonitor()

def log_unsupported_url(url, error_msg=""):
    queue_file = os.path.expanduser("~/.dl_unsupported_urls.json")
    data = []
    if os.path.exists(queue_file):
        try:
            with open(queue_file, "r") as f:
                data = json.load(f)
        except Exception:
            data = []
    
    entry = {
        "url": url,
        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
        "error": str(error_msg),
        "status": "pending_ai_fix"
    }
    data.append(entry)
    with open(queue_file, "w") as f:
        json.dump(data, f, indent=2)

    print("\n" + "!"*65)
    print(" [WARN] Unsupported or Complex Link Detected!")
    print(f" > URL logged for Antigravity AI auto-fix: {url}")
    print(" > CLI status: Running background analysis...")
    print(" > Press Ctrl+O (or type 'o') anytime to view live process logs!")
    print("!"*65 + "\n")

def download_youtube_audio(url, codec='mp3', quality='320', target_dir=None):
    out_dir = target_dir or DEFAULT_DIR
    os.makedirs(out_dir, exist_ok=True)

    print(f"> Downloading audio [{codec.upper()}]...")
    ydl_opts = get_ytdlp_opts({
        'format': 'bestaudio/best',
        'outtmpl': os.path.join(out_dir, '%(title)s.%(ext)s'),
        'quiet': False,
        'no_warnings': True,
    })

    if codec in ('mp3', 'flac', 'wav', 'm4a', 'opus'):
        ydl_opts['postprocessors'] = [{
            'key': 'FFmpegExtractAudio',
            'preferredcodec': codec,
            'preferredquality': quality if codec == 'mp3' else None,
        }]

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=True)
            if info:
                filename = ydl.prepare_filename(info)
                base, _ = os.path.splitext(filename)
                target = f"{base}.{codec}" if codec != 'best' else filename
                if os.path.exists(target):
                    filename = target
                print(f"[OK] Audio downloaded to: {filename}")
                sync_to_sdcard_if_needed(filename)
                return filename
    except Exception as e:
        print(f"[FAIL] Error downloading audio: {e}")
        return None

def search_netmirror(query):
    try:
        s = requests.Session()
        s.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Referer': 'https://mzfi.me/',
            'Origin': 'https://netmirror.center'
        })
        
        norm_q = ' '.join(query.strip().split())
        queries_to_try = []
        def add_q(val):
            c_val = ' '.join(val.strip().split())
            if c_val and c_val not in queries_to_try:
                queries_to_try.append(c_val)

        add_q(norm_q)
        if '&' in norm_q:
            add_q(norm_q.replace('&', ' and '))
            add_q(norm_q.replace('&', 'and'))
        if ' and ' in norm_q.lower():
            add_q(re.sub(r'\band\b', '&', norm_q, flags=re.I))
            add_q(re.sub(r'\band\b', ' & ', norm_q, flags=re.I))

        # Check for 'lock' -> 'locke' and common typo / spelling variations
        if re.search(r'\block\b', norm_q, re.I):
            locke_q = re.sub(r'\block\b', 'locke', norm_q, flags=re.I)
            add_q(locke_q)
            if '&' in locke_q:
                add_q(locke_q.replace('&', ' and '))
                add_q(locke_q.replace('&', 'and'))
            if ' and ' in locke_q.lower():
                add_q(re.sub(r'\band\b', '&', locke_q, flags=re.I))
                add_q(re.sub(r'\band\b', ' & ', locke_q, flags=re.I))

        cleaned = ''.join(c if c.isalnum() or c.isspace() else ' ' for c in norm_q)
        cleaned_words = ' '.join(cleaned.split())
        if cleaned_words:
            add_q(cleaned_words)

        # Title-casing and variations
        for c in list(queries_to_try):
            add_q(c.title())

        # Singular, plural, and trailing-e word variations (e.g. lock <-> locke, Son <-> Sons)
        words = norm_q.split()
        for i, w in enumerate(words):
            if len(w) > 2:
                if not w.lower().endswith('s'):
                    add_q(' '.join(words[:i] + [w + 's'] + words[i+1:]))
                elif w.lower().endswith('s'):
                    add_q(' '.join(words[:i] + [w[:-1]] + words[i+1:]))
                if not w.lower().endswith(('e', 's')):
                    add_q(' '.join(words[:i] + [w + 'e'] + words[i+1:]))

        def parse_year(rel_date):
            if not rel_date: return 0
            try:
                return int(rel_date.split(',')[-1].strip()[:4])
            except Exception:
                return 0

        def rank_score(it, q_lower):
            t_lower = it['title'].lower()
            clean_t = re.sub(r'\[.*?\]', '', t_lower).strip()
            has_brackets = bool(re.search(r'\[.*?\]', t_lower))
            is_hindi = bool(re.search(r'\[hindi|\bhindi\b|\bdual\b|\bmulti[\s\-]audio\b', t_lower))
            is_direct = (clean_t == q_lower)
            starts_with_q = clean_t.startswith(q_lower)
            
            for lang in ['hindi', 'tamil', 'telugu', 'bengali', 'malayalam', 'english', 'indonesian']:
                if lang in q_lower and lang in t_lower:
                    return (0, -it['num_year'], len(t_lower))
                    
            if is_direct and not has_brackets:
                return (1, -it['num_year'])
            if is_direct and is_hindi:
                return (2, -it['num_year'])
            if starts_with_q and not has_brackets:
                return (3, -it['num_year'])
            if starts_with_q and is_hindi:
                return (4, -it['num_year'])
            if not has_brackets:
                return (5, -it['num_year'])
            if is_hindi:
                return (6, -it['num_year'])
            if 'english' in t_lower:
                return (7, -it['num_year'])
            return (8, -it['num_year'])

        # Title-casing and common typo word replacements (e.g. "son f anachy" -> "Sons of Anarchy")
        if norm_q.title() not in queries_to_try:
            queries_to_try.append(norm_q.title())

        typo_words = []
        for w in norm_q.split():
            lw = w.lower()
            if lw == 'f': typo_words.append('of')
            elif lw == 'n': typo_words.append('and')
            elif lw == 'anachy': typo_words.append('anarchy')
            else: typo_words.append(w)
        fixed_typo_str = ' '.join(typo_words)
        if fixed_typo_str not in queries_to_try:
            queries_to_try.append(fixed_typo_str)
            queries_to_try.append(fixed_typo_str.title())

        def query_api(q):
            clean_q = urllib.parse.quote(q)
            for host in ['api2.imdb4.shop', 'api2.imdb3.shop']:
                try:
                    url = f'https://{host}/api/search2/{clean_q}?page=0'
                    r = s.get(url, timeout=3.0)
                    if r.status_code == 200:
                        data = r.json()
                        results = data.get('results', [])
                        parsed = []
                        for item in results:
                            title = html.unescape(item.get('title') or item.get('name') or 'Unknown Title').strip()
                            media_id = item.get('id')
                            m_type = item.get('type') or item.get('media_type') or 'movie'
                            rel_date = item.get('release_date') or ''
                            year = rel_date.split(',')[-1].strip() if rel_date else ''
                            num_year = parse_year(rel_date)
                            if media_id:
                                nm_url = f"https://netmirror.center/{m_type}/{media_id}"
                                poster = item.get('backdrop_path') or item.get('poster') or item.get('pic') or ''
                                parsed.append({
                                    'title': title,
                                    'type': m_type.upper() if m_type else 'MOVIE/SERIES',
                                    'id': media_id,
                                    'year': year,
                                    'num_year': num_year,
                                    'url': nm_url,
                                    'source': 'NetMirror',
                                    'poster': poster,
                                    'cn': item.get('cn') or item.get('country') or '',
                                })
                        if parsed:
                            return sorted(parsed, key=lambda x: rank_score(x, norm_q.lower()))
                except Exception:
                    continue
            return []

        for q in queries_to_try:
            res = query_api(q)
            if res:
                return res

        # Fallback 1: Wikipedia opensearch for typo-correction (e.g. "son f anachy" -> "Sons of Anarchy")
        try:
            wiki_url = f"https://en.wikipedia.org/w/api.php?action=opensearch&search={urllib.parse.quote(norm_q)}&limit=5&namespace=0&format=json"
            wiki_res = requests.get(wiki_url, headers={'User-Agent': 'universal-downloader/1.0'}, timeout=4).json()
            if len(wiki_res) > 1 and wiki_res[1]:
                for candidate in wiki_res[1]:
                    cand_clean = re.sub(r'\(.*?\)', '', candidate).strip()
                    for noise in ['series', 'tv series', 'movie', 'film', 'season']:
                        cand_clean = re.sub(rf'\b{noise}\b', '', cand_clean, flags=re.I).strip()
                    cand_clean = ' '.join(cand_clean.split())
                    if cand_clean:
                        res = query_api(cand_clean)
                        if res:
                            return res
                        res = query_api(cand_clean.title())
                        if res:
                            return res
                        if not cand_clean.lower().endswith('s'):
                            res = query_api(cand_clean + 's')
                            if res:
                                return res
        except Exception:
            pass

        # Fallback 2: Google Suggestion for typo / spelling correction
        try:
            for s_q in [norm_q, norm_q + ' series', norm_q + ' movie']:
                suggest_url = f"https://suggestqueries.google.com/complete/search?client=firefox&q={urllib.parse.quote(s_q)}"
                s_res = requests.get(suggest_url, timeout=4).json()
                if len(s_res) > 1 and s_res[1]:
                    for sug in s_res[1][:5]:
                        cleaned_sug = sug
                        for noise in ['netflix series', 'web series', 'series cast', 'series review', 'series rating', 'series', 'imdb', 'cast', 'movie', 'film', 'ott', 'where to watch', 'meaning', 'in hindi', 'jacket', 'bike', 'season']:
                            cleaned_sug = re.sub(rf'\b{noise}\b', '', cleaned_sug, flags=re.I).strip()
                        cand = ' '.join(cleaned_sug.split())
                        if cand and len(cand) > 2:
                            res = query_api(cand)
                            if res:
                                return res
                            res = query_api(cand.title())
                            if res:
                                return res
                            if not cand.lower().endswith('s'):
                                res = query_api(cand + 's')
                                if res:
                                    return res
                            if '&' in cand:
                                res = query_api(cand.replace('&', 'and'))
                                if res: return res
                            elif 'and' in cand.lower():
                                res = query_api(re.sub(r'\band\b', '&', cand, flags=re.I))
                                if res: return res
        except Exception:
            pass

    except Exception:
        pass
    return []

def handle_video_stream_only(url):
    """Extract direct stream URL using yt-dlp and launch in VLC immediately without saving."""
    if not HAS_YTDLP:
        print("[WARN] yt-dlp is required for streaming.")
        return False
    print("> Extracting direct stream URL for instant playback...")
    try:
        ydl_opts = get_ytdlp_opts({'quiet': True, 'no_warnings': True, 'format': 'bestvideo+bestaudio/best'})
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            stream_url = info.get('url')
            if not stream_url and 'formats' in info:
                for f in reversed(info['formats']):
                    if f.get('url'):
                        stream_url = f.get('url')
                        break
            if stream_url:
                title = info.get('title', 'Video Stream')
                print(f" Video: {title}")
                launch_player(stream_url, player_choice="vlc", custom_headers=info.get('http_headers'), title=title)
                print("\n" + "="*65)
                print("  STREAMING ACTIVE IN VLC PLAYER")
                print("="*65)
                print(f"  Media: {title}")
                print(" > Press [Enter] or [Ctrl+C] to stop stream and return to menu...")
                try:
                    input()
                except (KeyboardInterrupt, EOFError):
                    pass
                print("\n[OK] Streaming session ended.")
                return True
    except Exception as e:
        print(f"[WARN] Could not launch stream player: {e}")
    return False

def handle_video_stream_and_download(url):
    """Launch stream in VLC immediately, then start downloading in terminal."""
    handle_video_stream_only(url)
    return download(url)

def get_ott_providers_for_title(title, m_type, year):
    """Fetch OTT platforms / networks hosting this title via TMDB API."""
    clean_q = re.sub(r'\[.*?\]', '', title).strip()
    api_key = 'c8dea14dc917687ac631a52620e4f7ad'
    try:
        sr = requests.get(f'https://api.themoviedb.org/3/search/{m_type}?query={urllib.parse.quote(clean_q)}&api_key={api_key}', timeout=2).json()
        hits = sr.get('results', [])
        matched_id = None
        for h in hits:
            h_year = (h.get('release_date') or h.get('first_air_date') or '')[:4]
            if not year or h_year == str(year):
                matched_id = h['id']
                break
        if not matched_id and hits:
            matched_id = hits[0]['id']
        if matched_id:
            ott_list = []
            if m_type == 'tv':
                try:
                    s_r = requests.get(f'https://api.themoviedb.org/3/tv/{matched_id}?api_key={api_key}', timeout=2).json()
                    for n in s_r.get('networks', []):
                        ott_list.append(n['name'])
                except Exception:
                    pass
            try:
                r_p = requests.get(f'https://api.themoviedb.org/3/{m_type}/{matched_id}/watch/providers?api_key={api_key}', timeout=2).json()
                for c in ['IN', 'US']:
                    for p in r_p.get('results', {}).get(c, {}).get('flatrate', []):
                        pname = p['provider_name']
                        if 'hotstar' in pname.lower(): pname = 'JioHotstar'
                        elif 'disney' in pname.lower(): pname = 'Disney+'
                        elif 'netflix' in pname.lower(): pname = 'Netflix'
                        elif 'prime' in pname.lower(): pname = 'Prime Video'
                        elif 'apple' in pname.lower(): pname = 'Apple TV+'
                        elif 'zee' in pname.lower(): pname = 'Zee5'
                        elif 'sony' in pname.lower(): pname = 'SonyLIV'
                        elif 'peacock' in pname.lower(): pname = 'Peacock'
                        elif 'paramount' in pname.lower(): pname = 'Paramount+'
                        elif 'max' in pname.lower() or 'hbo' in pname.lower(): pname = 'Max'
                        if pname not in ott_list:
                            ott_list.append(pname)
            except Exception:
                pass
            return ott_list[:2]
    except Exception:
        pass
    return []

def compute_query_relevance(title, query):
    if not query:
        return 0
    clean_t = re.sub(r'[^\w\s]', '', str(title).lower()).strip()
    clean_q = re.sub(r'[^\w\s]', '', str(query).lower()).strip()
    if not clean_t or not clean_q:
        return 0
    if clean_t == clean_q:
        return 1000
    if clean_t.startswith(clean_q + ' '):
        return 800
    if clean_q in clean_t:
        return 500
    q_words = set(clean_q.split())
    t_words = set(clean_t.split())
    if q_words and q_words.issubset(t_words):
        return 350
    overlap = len(q_words.intersection(t_words))
    return overlap * 60

def group_and_deduplicate_results(results, query=None):
    """Group duplicate results by title + type + year, extract audios, attach OTT platforms, and rank by query relevance."""
    groups = {}
    for it in results:
        t = it.get('title') or it.get('name') or ''
        clean_t = re.sub(r'\[.*?\]', '', t).strip()
        clean_t = re.sub(r'\s+', ' ', clean_t)
        m_type = (it.get('media_type') or it.get('type') or 'movie').lower()
        m_id = it.get('id')
        yr = it.get('num_year', 0)
        if not yr and it.get('year'):
            try:
                yr = int(str(it['year']).split(',')[-1].strip()[:4])
            except Exception:
                yr = 0
                
        # Detect audio with full language coverage and title tag priority
        t_lower = t.lower()
        cn = str(it.get('cn') or it.get('country') or '').lower().strip()
        if any(h in t_lower for h in ('[hindi]', ' hindi', '(hindi)', 'hindi audio', 'dual audio')):
            aud = 'Hindi'
        elif any(ta in t_lower for ta in ('[tamil]', ' tamil', '(tamil)')):
            aud = 'Tamil'
        elif any(te in t_lower for te in ('[telugu]', ' telugu', '(telugu)')):
            aud = 'Telugu'
        elif any(b in t_lower for b in ('[bengali]', ' bengali', '(bengali)')):
            aud = 'Bengali'
        elif any(m in t_lower for m in ('[malayalam]', ' malayalam', '(malayalam)')):
            aud = 'Malayalam'
        elif any(k in t_lower for k in ('[kannada]', ' kannada', '(kannada)')):
            aud = 'Kannada'
        elif any(i in t_lower for i in ('[indonesian]', ' indonesian', '(indonesian)')):
            aud = 'Indonesian'
        elif any(j in t_lower for j in ('[japanese]', ' japanese', '(japanese)')):
            aud = 'Japanese'
        elif any(ko in t_lower for ko in ('[korean]', ' korean', '(korean)')):
            aud = 'Korean'
        elif any(s in t_lower for s in ('[spanish]', ' spanish', '(spanish)')):
            aud = 'Spanish'
        elif any(f in t_lower for f in ('[french]', ' french', '(french)')):
            aud = 'French'
        elif any(g in t_lower for g in ('[german]', ' german', '(german)')):
            aud = 'German'
        elif any(r in t_lower for r in ('[russian]', ' russian', '(russian)')):
            aud = 'Russian'
        elif any(p in t_lower for p in ('[portuguese]', ' portuguese', '(portuguese)')):
            aud = 'Portuguese'
        elif any(a in t_lower for a in ('[arabic]', ' arabic', '(arabic)')):
            aud = 'Arabic'
        elif any(e in t_lower for e in ('[english]', ' english', '(english)')):
            aud = 'English'
        elif cn in ('india', 'in'):
            aud = 'Hindi'
        elif cn in ('japan', 'jp'):
            aud = 'Japanese'
        elif cn in ('south korea', 'korea', 'kr'):
            aud = 'Korean'
        elif cn in ('indonesia', 'id'):
            aud = 'Indonesian'
        elif '[' not in t:
            aud = 'English'
        else:
            aud = 'Other'

        if m_type in ('tv', 'series'):
            clean_show = re.sub(r'[\s\-_]+(S\d+|Season\s*\d+|Complete\s*Series).*', '', clean_t, flags=re.I).strip()
            key = (clean_show.lower(), 'tv')
            display_title = clean_show or clean_t
        else:
            key = (clean_t.lower(), m_type, yr)
            display_title = clean_t

        poster_url = it.get('poster') or it.get('backdrop_path') or ''
        if key not in groups:
            groups[key] = {
                'title': display_title,
                'type': m_type.upper(),
                'year': yr,
                'poster': poster_url,
                'audios': {},
                'items': []
            }
        elif not groups[key].get('poster') and poster_url:
            groups[key]['poster'] = poster_url

        existing = groups[key]['audios'].get(aud)
        # Never allow a bracketed / dubbed version to overwrite a clean original release
        if not (existing and '[' not in existing.get('title', '') and '[' in t):
            groups[key]['audios'][aud] = {
                'id': m_id,
                'title': t,
                'url': it.get('url') or f"https://netmirror.center/{m_type}/{m_id}",
                'poster': poster_url,
            }
        groups[key]['items'].append(it)

    # Fetch OTT providers in parallel
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as ex:
        f_map = {ex.submit(get_ott_providers_for_title, g['title'], g['type'].lower(), g['year']): k for k, g in groups.items()}
        for f in concurrent.futures.as_completed(f_map):
            k = f_map[f]
            groups[k]['otts'] = f.result() or []

    # Rank by query relevance score first (highest relevance first), then year, then title
    for g in groups.values():
        g['relevance'] = compute_query_relevance(g['title'], query)

    sorted_groups = sorted(
        groups.values(),
        key=lambda g: (-g.get('relevance', 0), -g.get('year', 0), g.get('title', ''))
    )
    return sorted_groups

def prompt_results_list(results, query, target_dir=None):
    sorted_groups = group_and_deduplicate_results(results, query=query)
    if not sorted_groups:
        print("[WARN] No matching results found on NetMirror. Searching YouTube instead...")
        return search_youtube_and_prompt(query, target_dir=target_dir)

    print("\n" + "="*65)
    print(f"  NetMirror Search Results for: \"{query}\"")
    print("="*65)
    for idx, g in enumerate(sorted_groups[:10], 1):
        yr_str = f" ({g['year']})" if g['year'] else ""
        aud_list = list(g['audios'].keys())
        aud_str = f" • Audio: [{', '.join(aud_list)}]"
        ott_str = f" • [{' / '.join(g['otts'])}]" if g['otts'] else ""
        print(f"   [{idx}] {g['title']} [{g['type']}{yr_str}]{ott_str}{aud_str}")
    print("   [Y]  Search on YouTube instead")
    print("="*65)

    while True:
        try:
            sel = input(f"> Select result [1-{len(sorted_groups[:10])}] (Default: 1, Y: YouTube): ").strip()
            if not sel:
                sel = '1'
        except (KeyboardInterrupt, EOFError):
            return
        if sel.lower() in ('y', 'yt', 'youtube'):
            return search_youtube_and_prompt(query, target_dir=target_dir)
        elif sel.isdigit() and 1 <= int(sel) <= len(sorted_groups[:10]):
            selected_group = sorted_groups[int(sel) - 1]
            audios = selected_group['audios']
            chosen_url = None
            if len(audios) > 1:
                print("\n" + "="*65)
                print(f" Select Audio Track for: {selected_group['title']} ({selected_group['year']})")
                print("="*65)
                audio_keys = list(audios.keys())
                for a_idx, lang in enumerate(audio_keys, 1):
                    flag = '[Hindi]' if lang == 'Hindi' else ('' if lang == 'English' else '')
                    print(f"   [{a_idx}] {flag} {lang}")
                print("="*65)
                try:
                    a_sel = input(f"> Select audio [1-{len(audio_keys)}] (Default: 1): ").strip()
                    if not a_sel or not a_sel.isdigit() or not (1 <= int(a_sel) <= len(audio_keys)):
                        a_sel = '1'
                except (KeyboardInterrupt, EOFError):
                    return
                chosen_lang = audio_keys[int(a_sel) - 1]
                chosen_url = audios[chosen_lang]['url']
                print(f"> Selected Audio: {chosen_lang}")
            else:
                chosen_url = list(audios.values())[0]['url']
                
            print(f"\n> Selected: {selected_group['title']}")
            prompt_action_mode_and_run(chosen_url, query_context=query, all_results=sorted_groups, target_dir=target_dir)
            return
        else:
            print(f"[FAIL] Invalid selection. Please select choice 1 to {len(sorted_groups[:10])} or 'Y'.")

def prompt_action_mode_and_run(url, query_context=None, all_results=None, target_dir=None):
    is_netmirror = any(nm in url.lower() for nm in ['netmirror.', 'net52.', 'net77.', 'watch21.', 'watch22.'])
    while True:
        print("\n" + "="*65)
        print(" Select Action / Mode:")
        print("   [1] ⬇ Download Video Only (Multi-threaded aria2c 16x in Terminal)")
        print("   [2]  Copy Direct Stream Link (No Pre-Download, for ADM / 1DM)")
        print("   [3]  Stream in VLC Player (Instant Play, No Storage Used)")
        print("   [4]  Stream in VLC & Download Video (Instant Play + Save)")
        print("="*65)
        try:
            mode_choice = input("> Select choice [1-4] (Default: 1): ").strip()
            if not mode_choice:
                mode_choice = '1'
        except (KeyboardInterrupt, EOFError):
            return

        res = None
        try:
            if mode_choice == '1':
                if is_netmirror:
                    res = handle_netmirror(url, action_mode='download_only', target_dir=target_dir)
                else:
                    res = download(url, target_dir=target_dir)
            elif mode_choice == '2':
                if is_netmirror:
                    res = handle_netmirror(url, action_mode='direct_link_only', target_dir=target_dir)
                else:
                    res = generate_adm_links_for_url(url)
            elif mode_choice == '3':
                if is_netmirror:
                    res = handle_netmirror(url, action_mode='stream_only', target_dir=target_dir)
                else:
                    res = handle_video_stream_only(url)
            elif mode_choice == '4':
                if is_netmirror:
                    res = handle_netmirror(url, action_mode='stream_and_download', target_dir=target_dir)
                else:
                    res = handle_video_stream_and_download(url)
            else:
                print("[FAIL] Invalid selection. Please select choice 1 to 4.")
                continue
        except Exception as e:
            print(f"[WARN] Stream processing encountered an issue: {e}")
            res = None

        if not res and is_netmirror:
            print("\n" + "="*65)
            print(" Select an alternative option:")
            search_title = query_context or "this title"
            print(f"   [1]  Search on YouTube for \"{search_title}\" (Instant Stream & Download)")
            if all_results and len(all_results) > 1:
                print("   [2]  Choose another result from NetMirror search list")
                print("   [3] ↩ Return to main menu")
            else:
                print("   [2] ↩ Return to main menu")
            print("="*65)
            try:
                alt = input("> Select choice (Default: 1): ").strip()
                if not alt:
                    alt = '1'
            except (KeyboardInterrupt, EOFError):
                return
            if alt == '1':
                return search_youtube_and_prompt(search_title, target_dir=target_dir)
            elif alt == '2' and all_results and len(all_results) > 1:
                return prompt_results_list(all_results, query_context, target_dir=target_dir)
            else:
                return
        break

def search_netmirror_and_prompt(query, target_dir=None):
    print(f"\n> Searching NetMirror for: \"{query}\"...")
    try:
        results = search_netmirror(query)
    except KeyboardInterrupt:
        print("\n[WARN] Search cancelled.")
        return
    if not results:
        print(f"\n[WARN] No search results found on NetMirror for: \"{query}\"")
        print("="*60)
        print(" Select an option:")
        print(f"   [1]  Search on YouTube for \"{query}\"")
        print("   [2]  Try a different search term")
        print("   [3] ↩ Return to main menu")
        print("="*60)
        try:
            choice = input("> Select choice [1-3] (Default: 2): ").strip()
            if not choice:
                choice = '2'
        except (KeyboardInterrupt, EOFError):
            return
        if choice == '1':
            return search_youtube_and_prompt(query, target_dir=target_dir)
        elif choice == '2':
            try:
                new_q = input("\n> Enter movie or video name: ").strip()
                if new_q and new_q.lower() not in ('q', 'quit', 'exit'):
                    return search_netmirror_and_prompt(new_q, target_dir=target_dir)
            except (KeyboardInterrupt, EOFError):
                return
        return
    return prompt_results_list(results, query, target_dir=target_dir)

def search_youtube_and_prompt(query, target_dir=None):
    print(f"\n> Searching YouTube for: \"{query}\"...")
    yt_entries = []
    if HAS_YTDLP:
        try:
            ydl_opts = get_ytdlp_opts({'quiet': True, 'no_warnings': True, 'extract_flat': 'in_playlist', 'socket_timeout': 6})
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                res = ydl.extract_info(f"ytsearch10:{query}", download=False)
                yt_entries = [e for e in res.get('entries', []) if e] if res else []
        except KeyboardInterrupt:
            print("\n[WARN] YouTube search cancelled.")
            return
        except Exception:
            yt_entries = []

    if not yt_entries:
        print("[WARN] No search results found on YouTube.")
        return

    print("\n" + "="*65)
    print(f"  YouTube Search Results for: \"{query}\"")
    print("="*65)
    for idx, entry in enumerate(yt_entries[:10], 1):
        title = entry.get('title', 'Unknown Title')
        uploader = entry.get('uploader') or entry.get('channel') or 'Unknown Uploader'
        duration = entry.get('duration')
        dur_str = f"{int(duration)//60}:{int(duration)%60:02d}" if isinstance(duration, (int, float)) else "N/A"
        print(f"   [{idx}] {title}")
        print(f"       Channel: {uploader} | Duration: {dur_str}")
    print("="*65)

    while True:
        try:
            sel = input(f"> Select result [1-{len(yt_entries[:10])}] (Default: 1): ").strip()
            if not sel:
                sel = '1'
        except (KeyboardInterrupt, EOFError):
            return
        if sel.isdigit() and 1 <= int(sel) <= len(yt_entries[:10]):
            selected_entry = yt_entries[int(sel) - 1]
            selected_url = selected_entry.get('url') or selected_entry.get('webpage_url') or f"https://www.youtube.com/watch?v={selected_entry.get('id')}"
            print(f"\n> Selected: {selected_entry.get('title')}")
            prompt_action_mode_and_run(selected_url)
            return
        else:
            print(f"[FAIL] Invalid selection. Please select choice 1 to {len(yt_entries[:10])}.")

def search_by_name(target_dir=None):
    print("\n" + "="*65)
    print(" Select Search Platform:")
    print("   [1]  NetMirror (Movies / Series / Web Shows)")
    print("   [2]  YouTube (Video / Music / Audio)")
    print("="*65)
    try:
        p_choice = input("> Select choice [1-2] (Default: 1): ").strip()
        if not p_choice:
            p_choice = '1'
    except (KeyboardInterrupt, EOFError):
        return

    if p_choice == '1':
        try:
            query = input("\n> Enter Movie / Series Name to Search NetMirror: ").strip()
        except (KeyboardInterrupt, EOFError):
            return
        if query:
            search_netmirror_and_prompt(query, target_dir=target_dir)
    elif p_choice == '2':
        try:
            query = input("\n> Enter Video / Song Name to Search YouTube: ").strip()
        except (KeyboardInterrupt, EOFError):
            return
        if query:
            search_youtube_and_prompt(query, target_dir=target_dir)

def generate_adm_links_for_url(url):
    is_yt = any(v in url.lower() for v in ['youtube.com', 'youtu.be'])
    if is_yt:
        print("\n" + "="*65)
        print(" ℹ NOTE FOR YOUTUBE DIRECT LINKS:")
        print(" • Modern YouTube splits HD/4K videos into separate video and audio streams.")
        print(" • Raw single video links extracted for ADM will NOT contain audio.")
        print(" • > To download 1080p/4K WITH sound merged into an MP4 file,")
        print("   select Option [1] in 'dl' (downloads at 16x speed and merges in 1s).")
        print("="*65)

    format_spec, is_audio = prompt_video_quality(url)
    
    if is_audio:
        stream_format = 'bestaudio/best'
    else:
        stream_format = f"{format_spec}/bestvideo+bestaudio/best"

    print(f"\n> Extracting direct stream links for: {url}...")
    if not HAS_YTDLP:
        print("[FAIL] yt-dlp is required for extracting stream links.")
        return
        
    try:
        ydl_opts = get_ytdlp_opts({'quiet': True, 'no_warnings': True, 'extract_flat': 'in_playlist'})
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            
        if not info:
            print("[FAIL] Could not extract link metadata.")
            return

        entries = info.get('entries', []) if isinstance(info, dict) else []
        adm_links = []
        
        if entries:
            pl_title = info.get('title', 'Playlist')
            print("\n" + "="*65)
            print(f"  Playlist / Batch Detected: {pl_title} ({len(entries)} items)")
            print("="*65)
            
            for idx, entry in enumerate(entries, 1):
                if not entry: continue
                v_title = entry.get('title', f'Item {idx}')
                v_url = entry.get('url') or f"https://www.youtube.com/watch?v={entry.get('id')}"
                
                try:
                    with yt_dlp.YoutubeDL(get_ytdlp_opts({'quiet': True, 'format': stream_format})) as v_ydl:
                        v_info = v_ydl.extract_info(v_url, download=False)
                        stream_url = None
                        if v_info and isinstance(v_info, dict):
                            stream_url = v_info.get('url')
                            if not stream_url and v_info.get('requested_formats'):
                                stream_url = v_info['requested_formats'][0].get('url')
                        if stream_url:
                            adm_links.append((idx, v_title, stream_url))
                            print(f"   [{idx}] {v_title}")
                            print(f"        Direct Stream Link: {stream_url[:90]}...\n")
                except Exception as e:
                    print(f"   [{idx}] {v_title} - Error extracting stream link: {e}")
        else:
            v_title = info.get('title', 'Video')
            try:
                with yt_dlp.YoutubeDL(get_ytdlp_opts({'quiet': True, 'format': stream_format})) as v_ydl:
                    v_info = v_ydl.extract_info(url, download=False)
                    stream_url = v_info.get('url')
                    audio_stream_url = None
                    if not stream_url and v_info.get('requested_formats'):
                        stream_url = v_info['requested_formats'][0].get('url')
                        if len(v_info['requested_formats']) > 1:
                            audio_stream_url = v_info['requested_formats'][1].get('url')
                    if stream_url:
                        adm_links.append((1, v_title, stream_url))
                        print("\n" + "="*65)
                        print(f"  Video: {v_title}")
                        print("="*65)
                        print(f"  Direct Video Stream Link: {stream_url}\n")
                        if audio_stream_url:
                            print(f"  Direct Audio Stream Link: {audio_stream_url}\n")
                            print(" [WARN] Note: Single video stream link above has no sound because YouTube")
                            print("   serves video and audio separately. Use Mode [1] for auto-merged MP4.\n")
            except Exception as e:
                print(f"[FAIL] Error extracting stream link: {e}")

        if adm_links:
            if len(adm_links) > 1:
                txt_path = "/sdcard/Download/adm_playlist_links.txt"
                all_urls_str = "\n".join([stream for _, _, stream in adm_links])
                with open(txt_path, "w") as f:
                    f.write(all_urls_str + "\n")
                
                start_local_http_server()
                single_hosted_url = "http://localhost:8080/adm_playlist_links.txt"
                my_ip = "127.0.0.1"
                try:
                    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
                    s.connect(("1.1.1.1", 80))
                    my_ip = s.getsockname()[0]
                    s.close()
                except Exception:
                    pass
                lan_url = f"http://{my_ip}:8080/adm_playlist_links.txt"

                print("="*65)
                print(f"  Saved {len(adm_links)} direct link(s) to: {txt_path}")
                print(f"  Single All-in-One Link for ADM: {single_hosted_url}")
                print(f"  Wi-Fi LAN Download Link:       {lan_url}")
                clipboard_target = single_hosted_url
            else:
                clipboard_target = adm_links[0][2]
                print("="*65)
                print(f"  Direct Stream Link for ADM / VLC: {clipboard_target}")
            
            print("="*65 + "\n")
            
    except Exception as e:
        print(f"[FAIL] Error extracting direct links: {e}")

def interactive_menu():
    while True:
        print("\n" + "="*65)
        print("            UNIVERSAL DOWNLOADER CLI            ")
        print("="*65)
        try:
            user_input = input("> Enter URL or Movie/Video Name (or 'q' to quit): ").strip()
        except (KeyboardInterrupt, EOFError):
            print("\nExiting...")
            sys.exit(0)
            
        if not user_input:
            continue
        if user_input.lower() in ('q', 'quit', 'exit'):
            print("Exiting...")
            sys.exit(0)

        # Backward compatibility for entering '1' or '2'
        if user_input == '1':
            try:
                user_input = input("> Enter URL / Playlist Link: ").strip()
            except (KeyboardInterrupt, EOFError):
                return
            if not user_input:
                continue
        elif user_input == '2':
            search_by_name()
            break

        # Check if user_input is a URL
        is_url = user_input.startswith(('http://', 'https://', 't.me/', 'telegram.me/', 'magnet:')) or (
            any(ext in user_input.lower() for ext in ['.com', '.org', '.net', '.in', '.cc', '.pro', '.io', '.me', '.plus', '.gg', '.shop'])
            and ' ' not in user_input
        )

        if is_url:
            url = user_input if user_input.startswith(('http://', 'https://', 'magnet:')) else f"https://{user_input}"
            prompt_action_mode_and_run(url)
            break
        else:
            # It's a search query!
            if user_input.lower().startswith(('yt:', 'yt ', 'youtube:', 'youtube ')):
                yt_query = re.sub(r'^(?:yt:|yt\s+|youtube:|youtube\s+)', '', user_input, flags=re.I).strip()
                search_youtube_and_prompt(yt_query)
                break
            else:
                search_netmirror_and_prompt(user_input)
                break

def download(url, out_name=None, cookie=None, target_dir=None, quality_arg=None, standalone=False, **kwargs):
    url_lower = url.lower()
    custom_headers = {}
    if cookie:
        custom_headers['Cookie'] = cookie

    PROCESS_MONITOR.start_key_listener()
    PROCESS_MONITOR.set_status("Analyzing URL...")
    PROCESS_MONITOR.log(f"Starting download request for: {url}")

    try:
        # Check Bypasser for DDLs (MediaFire, Terabox, Uptobox, Zippyshare, etc.)
        PROCESS_MONITOR.log("Checking Direct Link Bypasser...")
        bypassed_path = handle_bypasser(url, out_name=out_name, target_dir=target_dir)
        if bypassed_path:
            PROCESS_MONITOR.set_status("Completed (Bypassed)")
            return bypassed_path

        if url_lower.startswith("igl:") or any(dom in url_lower for dom in ["freeforall.dev", "indiassgetlentp", "okcdn.ru"]):
            PROCESS_MONITOR.set_status("Downloading India's Got Latent...")
            igl_res = app_igl_resolve(url, quality=quality_arg or "best")
            headers = igl_res.get("stream_headers") or igl_res.get("headers") or {}
            clean_name = clean_filename(out_name or igl_res.get("title") or "igl_episode", default_ext=".mp4")
            return download_file_stream(igl_res["url"], clean_name, custom_headers=headers, target_dir=target_dir)

        if any(nm in url_lower for nm in ['netmirror.', 'net52.cc', 'net77.cc', 'watch21.', 'watch22.']):
            PROCESS_MONITOR.set_status("Bypassing NetMirror Stream...")
            return handle_netmirror(url, out_name=out_name, target_dir=target_dir, quality_arg=quality_arg)
        elif any(tg_domain in url_lower for tg_domain in ['t.me/', 'telegram.me/', 'telegram.dog/']):
            PROCESS_MONITOR.set_status("Downloading Telegram Media...")
            return handle_telegram(url, out_name=out_name, target_dir=target_dir)
        elif 'studocu.com' in url_lower:
            PROCESS_MONITOR.set_status("Downloading Studocu Document...")
            return handle_studocu(url, out_name=out_name, custom_headers=custom_headers, target_dir=target_dir)
        elif 'selfstudys.com' in url_lower:
            PROCESS_MONITOR.set_status("Downloading SelfStudys Document...")
            return handle_selfstudys(url, target_dir=target_dir)
        elif 'studiestoday.com' in url_lower:
            PROCESS_MONITOR.set_status("Downloading StudiesToday Document...")
            return handle_studiestoday(url, target_dir=target_dir)
        elif any(v in url_lower for v in ['youtube.com', 'youtu.be', 'vimeo.com', 'tiktok.com', 'twitch.tv', 'facebook.com', 'instagram.com']):
            PROCESS_MONITOR.set_status("Extracting Video Streams...")
            video_path = handle_youtube_or_video(url, target_dir=target_dir, quality_arg=quality_arg, cookie=cookie, standalone=standalone)
            if video_path:
                return video_path
        
        PROCESS_MONITOR.set_status("Generic Stream Download...")
        res = handle_generic(url, out_name=out_name, custom_headers=custom_headers, target_dir=target_dir)
        if res:
            return res

        raise Exception("Extractor returned no stream data.")
    except Exception as e:
        PROCESS_MONITOR.log(f"Primary download failed: {e}")
        print(f"[FAIL] Error downloading from primary handler: {e}")
        print("> Attempting generic fallback...")
        try:
            res = handle_generic(url, out_name=out_name, custom_headers=custom_headers, target_dir=target_dir)
            if res:
                return res
        except Exception as e2:
            PROCESS_MONITOR.log(f"Generic fallback failed: {e2}")
            log_unsupported_url(url, error_msg=f"{e} | Fallback: {e2}")
            return None
    finally:
        PROCESS_MONITOR.stop_key_listener()

def app_cli_capabilities():
    return {
        "name": "Universal Downloader Engine",
        "version": "repo-backed",
        "runtime": "embedded-capable",
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
        "actions": ["download", "resolve", "stream", "copy_link"],
    }


def app_search_youtube(query):
    if not HAS_YTDLP:
        raise Exception("yt-dlp is not installed")
    opts = get_ytdlp_opts({
        "quiet": True,
        "no_warnings": True,
        "extract_flat": "in_playlist",
        "socket_timeout": 10,
    })
    with yt_dlp.YoutubeDL(opts) as ydl:
        data = ydl.extract_info(f"ytsearch10:{query}", download=False) or {}
    results = []
    for entry in data.get("entries", [])[:10]:
        if not entry:
            continue
        duration = entry.get("duration")
        entry_id = entry.get("id") or ""
        poster = entry.get("thumbnail") or (entry.get("thumbnails", [{}])[-1].get("url") if entry.get("thumbnails") else "") or (f"https://i.ytimg.com/vi/{entry_id}/hqdefault.jpg" if entry_id else "")
        results.append({
            "title": entry.get("title") or "Untitled",
            "source": "YouTube",
            "uploader": entry.get("uploader") or entry.get("channel") or "",
            "duration": duration,
            "url": entry.get("webpage_url") or entry.get("url") or (f"https://www.youtube.com/watch?v={entry_id}" if entry_id else ""),
            "id": entry_id,
            "poster": poster,
        })
    return results


def get_igl_module():
    import sys
    try:
        import igl
        return igl
    except ImportError:
        pass
    igl_script = os.path.expanduser("~/scripts/igl.py")
    if os.path.exists(igl_script):
        scripts_dir = os.path.dirname(igl_script)
        if scripts_dir not in sys.path:
            sys.path.insert(0, scripts_dir)
        try:
            import igl
            return igl
        except Exception:
            return None
    return None


def classify_igl_ep(ep):
    t = str(ep.get('title') or '').lower()
    did = str(ep.get('dataId') or '').lower()
    desc = str(ep.get('description') or '').lower()
    text = f"{t} {did} {desc}"
    if did == 's2-08':
        return 'NORMAL'
    if did in ('s2-09', 's2-06-rakhi') or 'uncensored' in text or 'member' in text:
        return 'MEMBERS'
    if any(k in text for k in ['bts', 'clip', 'discarded', 'deleted', 'special', 'talk show', 'segment', 'documentary', 'still alive']):
        return 'BONUS'
    if 'bonus' in t or 'bonus' in did:
        return 'BONUS'
    return 'NORMAL'


def get_ep_sort_key(ep):
    did = str(ep.get('dataId') or '')
    t = str(ep.get('title') or '')
    m_did = re.search(r'(?:s\d+-|ep-)(\d+)', did, re.I)
    if m_did:
        return int(m_did.group(1))
    m = re.search(r'\b(?:ep|episode)\s*(\d+)\b', t, re.I)
    if m:
        return int(m.group(1))
    return 0


def get_igl_poster(ep=None, category='NORMAL'):
    if ep:
        yt_id = ep.get('youtubeId')
        thumb = ep.get('thumbnail')
        if thumb and str(thumb).startswith('http'):
            return thumb
        if yt_id:
            return f"https://img.youtube.com/vi/{yt_id}/hqdefault.jpg"
    if category == 'MEMBERS':
        return "https://i.ytimg.com/vi/GTxMSPqtbRc/hqdefault.jpg"
    elif category == 'BONUS':
        return "https://i.ytimg.com/vi/NQ6gWHMvna8/hq720.jpg"
    return "https://i.ytimg.com/vi/CswOUBuokjo/hqdefault.jpg"


def search_igl_episodes(query):
    igl = get_igl_module()
    if not igl:
        return []
    try:
        base_url, episodes, _ = igl.get_episodes_data()
    except Exception:
        return []

    q_clean = str(query).strip().lower()
    is_igl_query = any(k in q_clean for k in ["igl", "latent", "india's got latent", "indias got latent", "samay raina", "samay"])
    wants_bonus = any(w in q_clean for w in ["bonus", "bts", "special", "discarded", "deleted", "extra", "clip"])

    stopwords = {"the", "a", "an", "in", "on", "at", "to", "for", "of", "and", "or", "is", "it", "india", "india's", "indias", "indias'", "got", "latent", "igl", "samay", "raina", "season", "episode", "ep", "episodes", "part", "show", "video", "videos", "s1", "s2"}
    tokens = [w for w in re.findall(r'[a-zA-Z0-9]+', q_clean) if len(w) > 2]
    specific_keywords = [w for w in tokens if w not in stopwords]

    def get_ep_order(ep):
        did = str(ep.get('dataId') or '')
        s = 2 if ('s2' in did.lower() or '2' in str(ep.get('season') or '')) else 1
        m_did = re.search(r'(?:s\d+-|ep-)(\d+)', did, re.I)
        num = int(m_did.group(1)) if m_did else 0
        return (s, num)

    matches = []
    for ep in episodes:
        t = ep.get('title', '').lower()
        c = classify_igl_ep(ep)

        # Update 67 style: Do not show everything.
        # Only show full episodes and members only unless user specifically searches for bonus/specials
        if not wants_bonus and c == 'BONUS':
            continue

        score = 0
        if is_igl_query:
            if specific_keywords:
                matched_kws = [kw for kw in specific_keywords if re.search(r'\b' + re.escape(kw) + r'\b', t)]
                if len(matched_kws) == len(specific_keywords):
                    score = 100 + len(matched_kws) * 10
                elif matched_kws:
                    score = 50 + len(matched_kws) * 10
            else:
                score = 20
        else:
            if specific_keywords and len(specific_keywords) >= 2:
                matched_kws = [kw for kw in specific_keywords if re.search(r'\b' + re.escape(kw) + r'\b', t)]
                if len(matched_kws) == len(specific_keywords):
                    score = 80 + len(matched_kws) * 10
            elif specific_keywords and len(specific_keywords) == 1 and len(specific_keywords[0]) >= 6:
                if re.search(r'\b' + re.escape(specific_keywords[0]) + r'\b', t):
                    score = 70

        if score > 0:
            poster = get_igl_poster(ep, category=c)
            did = ep.get('dataId') or ep.get('id')
            matches.append((score, get_ep_order(ep), {
                "title": ep.get('title'),
                "type": "IGL",
                "year": "",
                "audios": [{"language": "Original", "title": ep.get('title'), "url": f"igl:{did}", "poster": poster}],
                "providers": [],
                "source": "ARC",
                "poster": poster,
                "url": f"igl:{did}",
            }))

    if specific_keywords:
        matches.sort(key=lambda x: (x[0], x[1]), reverse=True)
    else:
        matches.sort(key=lambda x: x[1], reverse=True)

    return [m[2] for m in matches]


def app_igl_catalog():
    igl = get_igl_module()
    if not igl:
        return {"ok": False, "episodes": []}
    base_url, episodes, is_cached = igl.get_episodes_data()
    categorized = []
    for idx, ep in enumerate(episodes, 1):
        c = classify_igl_ep(ep)
        badge = "MEMBERS ONLY" if c == "MEMBERS" else ("BONUS / BTS" if c == "BONUS" else "FULL EPISODE")
        data_id = ep.get('dataId', f'ep-{idx}')
        poster = get_igl_poster(ep, category=c)
        categorized.append({
            "idx": idx,
            "id": ep.get('id'),
            "data_id": data_id,
            "title": ep.get('title'),
            "duration": ep.get('duration', ''),
            "badge": badge,
            "type": ep.get('type'),
            "url": f"igl:{data_id}",
            "poster": poster,
        })
    return {"ok": True, "episodes": categorized, "base_url": base_url}


def app_igl_resolve(target_id, quality="best", season=None, episode=None):
    igl = get_igl_module()
    if not igl:
        raise Exception("IGL module unavailable")
    base_url, episodes, _ = igl.get_episodes_data()
    clean_id = str(target_id).replace("igl:", "").strip()

    match_ep = None
    for ep in episodes:
        if ep.get('dataId') == clean_id or str(ep.get('id')) == clean_id:
            match_ep = ep
            break
    if not match_ep:
        for ep in episodes:
            if clean_id.lower() in ep.get('title', '').lower():
                match_ep = ep
                break
    if not match_ep:
        raise Exception(f"IGL Episode '{clean_id}' not found.")

    info = igl.resolve_stream_info(match_ep, base_url, target_quality=quality)
    if not info or not info.get('stream_url'):
        raise Exception("Could not resolve streaming link for this episode.")

    stream_url = info['stream_url']
    ep_type = info.get('type')
    poster = get_igl_poster(match_ep, category=classify_igl_ep(match_ep))
    resolved_title = info.get('title') or match_ep.get('title')

    # If YouTube episode, resolve playable direct video stream for instant VLC streaming
    if ep_type == 'youtube' or "youtube.com" in stream_url or "youtu.be" in stream_url:
        try:
            yt_res = app_resolve(stream_url, quality=quality, skip_igl=True)
            if yt_res and yt_res.get('url'):
                direct_stream = yt_res['url']
                return {
                    "url": direct_stream,
                    "stream_url": direct_stream,
                    "direct_url": direct_stream,
                    "target_url": f"igl:{match_ep.get('dataId') or match_ep.get('id')}",
                    "title": resolved_title,
                    "source": "ARC",
                    "type": "IGL",
                    "poster": poster,
                    "quality": yt_res.get('quality', 'Best Available'),
                    "qualities": yt_res.get('qualities', ["Best Available", "1080p (Full HD)", "720p (HD)"]),
                    "stream_headers": yt_res.get('stream_headers', {}),
                    "headers": yt_res.get('headers', {}),
                    "is_series": False,
                    "can_stream": True,
                    "media_type": "video",
                    "ep_type": "youtube",
                    "yt_url": stream_url,
                }
        except Exception as e:
            print(f"[WARN] IGL YouTube direct stream resolution: {e}")

    # OKCDN direct stream: NEVER include Origin header (okcdn.ru rejects Origin with HTTP 400)
    clean_headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
    }

    return {
        "url": stream_url,
        "direct_url": stream_url,
        "stream_url": stream_url,
        "target_url": f"igl:{match_ep.get('dataId') or match_ep.get('id')}",
        "title": resolved_title,
        "source": "ARC",
        "type": "IGL",
        "poster": poster,
        "quality": info.get('quality', 'best'),
        "qualities": ["Best Available", "4K (2160p)", "1080p (Full HD)", "720p (HD)", "480p (SD)"],
        "stream_headers": clean_headers,
        "headers": clean_headers,
        "is_series": False,
        "filename": info.get('filename'),
        "can_stream": True,
        "media_type": "video",
        "ep_type": ep_type,
    }


RESOLVED_TORRENT_MAP = {}

def clean_title_for_search(raw_title):
    t = re.sub(r"\b(Season \d+|S\d+E\d+|S\d+)\b", "", raw_title, flags=re.I)
    t = re.sub(r"\[.*?\]|\(.*?\)", "", t)
    t = t.replace(".", " ").replace("_", " ")
    return re.sub(r"\s+", " ", t).strip()

def get_cinemeta_imdb_id(title, is_series=False):
    clean = clean_title_for_search(title)
    if not clean:
        return None
    q = urllib.parse.quote(clean)
    url = f"https://v3-cinemeta.strem.io/catalog/series/top/search={q}.json" if is_series else f"https://v3-cinemeta.strem.io/catalog/movie/top/search={q}.json"
    try:
        resp = requests.get(url, headers={"User-Agent": "Mozilla/5.0"}, timeout=5)
        if resp.status_code == 200:
            metas = resp.json().get("metas", [])
            if metas:
                return metas[0].get("imdb_id") or metas[0].get("id")
    except Exception:
        pass
    return None

def fetch_torrent_tiers(title, is_series=False, season=None, episode=None):
    imdb_id = get_cinemeta_imdb_id(title, is_series)
    if not imdb_id:
        return {}
    if is_series and season and episode:
        url = f"https://torrentio.strem.fun/stream/series/{imdb_id}:{season}:{episode}.json"
    else:
        url = f"https://torrentio.strem.fun/stream/movie/{imdb_id}.json"
        
    try:
        resp = requests.get(url, headers={"User-Agent": "Mozilla/5.0"}, timeout=6)
        if resp.status_code != 200:
            return {}
        streams = resp.json().get("streams", [])
        tiers = {}
        for s in streams:
            n = s.get("name", "")
            t = s.get("title", "")
            full_text = (n + " " + t).lower()
            tier = None
            if "4k" in full_text or "2160p" in full_text:
                tier = "4K UHD (2160p)"
            elif "1080p" in full_text or "fhd" in full_text:
                tier = "1080p (Full HD)"
            elif "720p" in full_text or "hd" in full_text:
                tier = "720p (HD)"
            elif "480p" in full_text or "sd" in full_text:
                tier = "480p (SD)"
            elif "360p" in full_text:
                tier = "360p"
            if not tier:
                continue
            seeds_m = re.search(r"👤\s*(\d+)", t)
            seeds = int(seeds_m.group(1)) if seeds_m else 0
            size_m = re.search(r"💾\s*([\d\.]+\s*(?:GB|MB|GiB|MiB))", t)
            size_str = size_m.group(1) if size_m else ""
            if tier not in tiers or seeds > tiers[tier]["seeds"]:
                info_hash = s.get("infoHash")
                file_idx = s.get("fileIdx")
                magnet = f"magnet:?xt=urn:btih:{info_hash}&dn={urllib.parse.quote(title)}"
                label_str = tier + (f" • {size_str}" if size_str else "")
                tiers[tier] = {
                    "tier": tier,
                    "label": label_str,
                    "seeds": seeds,
                    "size": size_str,
                    "magnet": magnet,
                    "file_idx": file_idx,
                    "filename": s.get("behaviorHints", {}).get("filename", "")
                }
        return tiers
    except Exception as e:
        return {}

def download_torrent(magnet_url, title="media", target_dir=None, file_idx=None, job_id=None):
    jid = job_id or get_current_job_id()
    target_dir = target_dir or DEFAULT_DIR
    os.makedirs(target_dir, exist_ok=True)
    clean_title = clean_filename(title)
    
    print("="*65)
    print(" [BITTORRENT] IN-APP ACCELERATED DOWNLOAD")
    print("="*65)
    print(f" Target: {clean_title}")
    print(f" Save Directory: {target_dir}")
    if file_idx is not None and str(file_idx).strip().isdigit():
        print(f" Mode: Single Episode Extraction (File #{file_idx})")
    print("="*65)

    cmd = [
        "aria2c",
        f"--dir={target_dir}",
        "--enable-dht=true",
        "--dht-listen-port=6881",
        "--enable-peer-exchange=true",
        "--bt-enable-lpd=true",
        "--bt-max-peers=55",
        "--seed-time=0",
        "--max-connection-per-server=16",
        "--split=16",
        "--summary-interval=1",
        "--file-allocation=none",
        "--allow-overwrite=true",
        "--auto-file-renaming=false",
        "--console-log-level=warn",
    ]
    if file_idx is not None and str(file_idx).strip().isdigit():
        cmd.append(f"--select-file={file_idx}")
    cmd.append(magnet_url)

    ret = run_command_with_clean_progress(cmd)
    if ret == -1:
        print("[WARN] Torrent download cancelled.")
        return None
    if ret != 0:
        print(f"[FAIL] aria2c exited with code {ret}")
        return None

    dest_file = None
    for root, dirs, files in os.walk(target_dir):
        for f in files:
            if not f.endswith(".aria2") and not f.endswith(".part"):
                fp = os.path.join(root, f)
                try:
                    if os.path.getsize(fp) > 512 * 1024:
                        if time.time() - os.path.getmtime(fp) < 600:
                            dest_file = fp
                            break
                except Exception:
                    pass
        if dest_file:
            break
            
    if dest_file:
        sync_to_sdcard_if_needed(dest_file)
        render_download_progress(100.0, total_str="Complete", speed_str="--", eta_str="0s", job_id=jid)
        return dest_file
    return target_dir


def app_search(query, platform="netmirror"):
    if not query or not str(query).strip():
        raise Exception("Search query is empty")
    print(f"> Query: '{query}'")
    
    # Check IGL catalog matches with smart query filtering
    igl_matches = search_igl_episodes(query)

    if str(platform).lower() in ("youtube", "yt"):
        print("> Querying YouTube search engine...")
        results = app_search_youtube(str(query).strip())
        print(f"[OK] Found {len(results)} YouTube result(s).")
        return {"platform": "youtube", "results": igl_matches + results}

    print("> Querying catalog...")
    raw_results = search_netmirror(str(query).strip())
    print(f"> Found {len(raw_results)} catalog match(es). Grouping multi-audio streams and ranking by relevance...")
    groups = group_and_deduplicate_results(raw_results, query=str(query).strip())
    results = []
    def aud_prio(k):
        kl = k.lower()
        if 'hindi' in kl: return 1
        if 'english' in kl: return 2
        if 'original' in kl: return 3
        world = ['tamil', 'telugu', 'bengali', 'spanish', 'french', 'arabic', 'portuguese', 'russian', 'german', 'japanese', 'korean', 'chinese', 'indonesian']
        for i, w in enumerate(world):
            if w in kl: return 10 + i
        return 100

    for group in groups[:10]:
        audio_items = []
        sorted_auds = sorted(group.get("audios", {}).items(), key=lambda x: aud_prio(x[0]))
        for language, item in sorted_auds:
            audio_items.append({
                "language": language,
                "title": item.get("title", ""),
                "url": item.get("url", ""),
                "id": item.get("id"),
                "poster": item.get("poster", "") or group.get("poster", ""),
            })
        first_url = audio_items[0]["url"] if audio_items else ""
        results.append({
            "title": group.get("title", ""),
            "type": group.get("type", "MOVIE"),
            "year": group.get("year", 0),
            "audios": audio_items,
            "providers": group.get("otts", []),
            "source": "",
            "poster": group.get("poster", ""),
            "url": first_url,
        })
    print(f"[OK] Grouped into {len(results) + len(igl_matches)} distinct title(s).")
    return {"platform": "catalog", "results": igl_matches + results}


def select_progressive_youtube_format(formats, quality="best"):
    """Pick one YouTube format with both audio and video under the requested cap.
    Direct video-only URLs have no sound in an external player, so streaming and
    single-file downloads strictly require both audio and video tracks unless
    explicitly selected as video-only.
    """
    q = str(quality or "best").lower()
    cap = None
    if "4k" in q or "2160" in q:
        cap = 2160
    elif "2k" in q or "1440" in q:
        cap = 1440
    else:
        match = re.search(r"(4320|2160|1440|1080|720|480|360|240|144)\s*p?", q)
        if match:
            cap = int(match.group(1))

    # If the user explicitly chose a format labeled "video only"
    if "video only" in q:
        v_candidates = []
        for fmt in (formats or []):
            if not isinstance(fmt, dict) or not fmt.get("url") or fmt.get("vcodec") in (None, "none"):
                continue
            height = fmt.get("height") or 0
            try:
                height = int(height)
            except (TypeError, ValueError):
                height = 0
            if cap and height > cap:
                continue
            v_candidates.append((height, fmt.get("tbr") or 0, fmt))
        if v_candidates:
            v_candidates.sort(key=lambda item: (item[0], item[1]))
            return v_candidates[-1][2]

    candidates = []
    for fmt in (formats or []):
        if not isinstance(fmt, dict) or not fmt.get("url"):
            continue
        if fmt.get("vcodec") in (None, "none") or fmt.get("acodec") in (None, "none"):
            continue
        height = fmt.get("height") or 0
        try:
            height = int(height)
        except (TypeError, ValueError):
            height = 0
        if cap and height > cap:
            continue
        candidates.append((height, fmt.get("tbr") or 0, fmt))

    if candidates:
        candidates.sort(key=lambda item: (item[0], item[1]))
        return candidates[-1][2]

    all_prog = [
        (int(f.get("height") or 0), f.get("tbr") or 0, f)
        for f in (formats or [])
        if isinstance(f, dict) and f.get("url") and f.get("vcodec") not in (None, "none") and f.get("acodec") not in (None, "none")
    ]
    if all_prog:
        all_prog.sort(key=lambda item: (item[0], item[1]))
        return all_prog[-1][2]

    return None


def app_resolve(url, quality="best", season=None, episode=None, cookie=None, audio_lang=None, telegram_config=None, skip_igl=False):
    if not url:
        raise Exception("A URL is required")
    lower = url.lower()
    clean_quality = quality if (quality and "ask" not in quality.lower()) else "best"
    print(f"> Resolving target: {url}")
    print(f"> Preferred quality: {clean_quality}")

    if not skip_igl and (lower.startswith("igl:") or any(dom in lower for dom in ["freeforall.dev", "indiassgetlentp", "okcdn.ru"])):
        print("> Resolving India's Got Latent episode stream via IGL engine...")
        return app_igl_resolve(url, quality=clean_quality)

    if not skip_igl and ("youtube.com" in lower or "youtu.be" in lower) and any(w in lower for w in ["latent", "samay", "igl"]):
        igl = get_igl_module()
        if igl:
            try:
                _, episodes, _ = igl.get_episodes_data()
                for ep in episodes:
                    yt_id = ep.get('youtubeId')
                    if yt_id and yt_id.lower() in lower:
                        print(f"> Detected India's Got Latent episode: {ep.get('title')}")
                        return app_igl_resolve(ep.get('dataId') or ep.get('id'), quality=clean_quality)
            except Exception:
                pass

    if lower.startswith("magnet:"):
        print("> Detected Magnet link...")
        dn_match = re.search(r"dn=([^&]+)", url)
        display_name = urllib.parse.unquote(dn_match.group(1)).replace("+", " ") if dn_match else "Torrent Media"
        lbl = "Best Available (Torrent)"
        RESOLVED_TORRENT_MAP[lbl] = {
            "magnet": url,
            "file_idx": None,
            "label": lbl,
        }
        return {
            "url": url,
            "target_url": url,
            "title": display_name,
            "source": "Torrent",
            "poster": "",
            "quality": lbl,
            "qualities": [lbl],
            "can_stream": False,
            "media_type": "file",
            "torrent_sources": {
                lbl: { "magnet": url, "file_idx": None }
            }
        }

    if any(nm in lower for nm in ["netmirror.", "net52.cc", "net77.cc", "watch21.", "watch22."]):
        print("> Connecting to stream endpoint and resolving qualities...")
        res = handle_netmirror(
            url,
            action_mode="direct_link_only",
            quality_arg=clean_quality,
            target_season=season,
            target_episode=episode,
            audio_lang=audio_lang,
        )
        if not res:
            raise Exception("No active stream was found for this NetMirror link")
        if isinstance(res, dict):
            if "target_url" not in res:
                res["target_url"] = url
            return res
        return {
            "url": res,
            "target_url": url,
            "title": "Resolved stream",
            "source": "NetMirror",
            "quality": clean_quality,
            "qualities": ["Best Available"],
        }

    if any(host in lower for host in ["youtube.com", "youtu.be", "vimeo.com", "tiktok.com", "twitch.tv", "facebook.com", "instagram.com"]):
        if not HAS_YTDLP:
            raise Exception("yt-dlp is not available")
        
        is_playlist = ("/playlist" in lower or "list=" in lower)
        if is_playlist:
            print("> Detecting playlist entries...")
            ydl_opts = get_ytdlp_opts({
                "quiet": True,
                "no_warnings": True,
                "extract_flat": "in_playlist",
                "ignoreerrors": True,
                "socket_timeout": 15,
            })
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                info = ydl.extract_info(url, download=False) or {}
            entries_raw = info.get("entries", []) or []
            entries_list = []
            for i, e in enumerate(entries_raw):
                if not e: continue
                e_id = e.get("id") or ""
                e_title = e.get("title") or f"Item #{i+1}"
                e_url = f"https://www.youtube.com/watch?v={e_id}" if e_id else e.get("url", "")
                e_thumb = (e.get("thumbnails", [{}])[-1].get("url") if e.get("thumbnails") else "") or (f"https://i.ytimg.com/vi/{e_id}/hqdefault.jpg" if e_id else "")
                entries_list.append({
                    "id": e_id,
                    "title": e_title,
                    "url": e_url,
                    "thumbnail": e_thumb,
                    "duration": e.get("duration", 0),
                })
            print(f"[OK] Playlist recognized: {info.get('title', 'Playlist')} ({len(entries_list)} items)")
            return {
                "is_playlist": True,
                "title": info.get("title", "YouTube Playlist"),
                "source": "YouTube",
                "poster": entries_list[0]["thumbnail"] if entries_list else "",
                "total_count": len(entries_list),
                "entries": entries_list,
                "url": url,
                "qualities": ["Best Available", "1080p (Full HD)", "720p (HD)", "480p (SD)", "360p (Low)"],
                "can_stream": True,
                "media_type": "video",
            }

        print("> Analyzing video manifest with yt-dlp...")
        info = None
        formats = []
        last_err = None

        client_attempts = [
            None,
            ['tv_embedded'],
            ['android_creator'],
            ['visionos', 'android'],
            ['android', 'ios'],
        ]

        safari_headers = {
            "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15"
        }

        for client in client_attempts:
            ydl_opts = get_ytdlp_opts({
                "quiet": True,
                "no_warnings": True,
                "skip_download": True,
                "noplaylist": True,
                "socket_timeout": 12,
            }, cookie=cookie)
            if client:
                ydl_opts["extractor_args"] = {"youtube": {"player_client": client}}
            try:
                with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                    extracted = ydl.extract_info(url, download=False)
                    if extracted and isinstance(extracted, dict):
                        extracted_fmts = [f for f in extracted.get("formats", []) if isinstance(f, dict) and f.get("url")]
                        if extracted_fmts:
                            info = extracted
                            formats = extracted.get("formats", [])
                            break
            except Exception as e:
                last_err = str(e)
                continue

        if not info or not formats or not any(isinstance(f, dict) and f.get("url") for f in formats):
            err_msg = last_err or "No playable video streams found (YouTube bot verification or restriction)"
            if "not a bot" in err_msg.lower() or "confirm you" in err_msg.lower():
                raise Exception("YouTube IP verification detected. Toggle Airplane Mode to refresh cellular IP or add cookies.txt to /sdcard/Download/")
            clean_err = re.sub(r'https?://\S+', '', err_msg).strip()
            raise Exception(f"YouTube resolution failed: {clean_err}")

        # 1. Progressive streams (combined video + audio)
        progressive = [
            f for f in formats
            if isinstance(f, dict) and f.get("url") and f.get("vcodec") not in (None, "none") and f.get("acodec") not in (None, "none")
        ]

        # 2. Collect HLS audio and video formats (Google Video HLS manifests)
        hls_audio_formats = [
            f for f in formats
            if isinstance(f, dict) and f.get("url") and
               ('m3u8' in f.get('protocol', '') or 'm3u8' in str(f.get('url', '')) or 'manifest/hls_playlist' in str(f.get('url', ''))) and
               f.get('vcodec') in (None, 'none') and
               (f.get('resolution') == 'audio only' or f.get('height') in (None, 0))
        ]
        # Prefer 128k (itag 234) over 48k (itag 233)
        hls_audio_formats.sort(
            key=lambda f: (
                1 if ('234' in str(f.get('format_id') or '') or '234' in str(f.get('url') or '')) else 0,
                f.get('tbr') or f.get('abr') or 0
            ),
            reverse=True
        )
        hls_audio_fmt = None
        clean_audio_lang = str(audio_lang or "").strip().lower()
        if clean_audio_lang and clean_audio_lang not in ("default", "original", "none"):
            for af in hls_audio_formats:
                lang = str(af.get('language') or af.get('format_note') or '').lower()
                if clean_audio_lang in lang or clean_audio_lang[:2] == lang[:2]:
                    hls_audio_fmt = af
                    break

        if not hls_audio_fmt:
            # Priority 1: Hindi
            for af in hls_audio_formats:
                lang = str(af.get('language') or af.get('format_note') or '').lower()
                if 'hi' in lang or 'hindi' in lang:
                    hls_audio_fmt = af
                    break
        if not hls_audio_fmt:
            # Priority 2: English / Original / Default
            for af in hls_audio_formats:
                lang = str(af.get('language') or af.get('format_note') or '').lower()
                if 'en' in lang or 'default' in lang or 'original' in lang:
                    hls_audio_fmt = af
                    break
        if not hls_audio_fmt and hls_audio_formats:
            hls_audio_fmt = hls_audio_formats[0]

        hls_video_formats = [
            f for f in formats
            if isinstance(f, dict) and f.get("url") and
               ('m3u8' in f.get('protocol', '') or 'm3u8' in str(f.get('url', '')) or 'manifest/hls_playlist' in str(f.get('url', ''))) and
               f.get('vcodec') not in (None, 'none')
        ]

        # Extract REAL video quality tiers in descending order (exclude storyboard/thumbnails like sb0)
        tiers = set()
        for f in formats:
            if f.get('vcodec') == 'none':
                continue
            note = f.get('format_note') or ''
            m = re.search(r'(\d+)p', str(note))
            if m:
                tiers.add(int(m.group(1)))
            else:
                w = f.get('width') or 0
                h = f.get('height') or 0
                dim = max(w, h)
                if dim >= 3840 or h >= 2160: tiers.add(2160)
                elif dim >= 2560 or h >= 1440: tiers.add(1440)
                elif dim >= 1920 or h >= 1080: tiers.add(1080)
                elif dim >= 1280 or h >= 720: tiers.add(720)
                elif dim >= 854 or h >= 480: tiers.add(480)
                elif dim >= 640 or h >= 360: tiers.add(360)
                elif dim >= 426 or h >= 240: tiers.add(240)
                elif dim >= 256 or h >= 144: tiers.add(144)

        for vf in hls_video_formats:
            vh = vf.get('height') or 0
            if vh: tiers.add(vh)

        # Enforce all standard descending tiers based on real detected maximum video resolution
        valid_heights = [info.get('height') or 0] + [f.get('height') or 0 for f in formats if isinstance(f, dict) and f.get('vcodec') != 'none'] + list(tiers)
        detected_max = max(valid_heights) if valid_heights else 720
        max_h = detected_max if detected_max > 0 else 720
        standard_tiers = [4320, 2160, 1440, 1080, 720, 480, 360, 240, 144]
        for st in standard_tiers:
            if st <= max_h:
                tiers.add(st)
        if not tiers:
            tiers = {max_h}

        quality_labels = {
            4320: "4320p (8K)",
            2160: "2160p (4K UHD)",
            1440: "1440p (2K QHD)",
            1080: "1080p (Full HD)",
            720: "720p (HD)",
            480: "480p (SD)",
            360: "360p",
            240: "240p",
            144: "144p",
        }

        sorted_tiers = sorted(list(tiers), reverse=True)
        real_qualities = ["Best Available"] + [quality_labels.get(t, f"{t}p") for t in sorted_tiers]

        quality_urls_map = {}

        # Direct progressive and HLS stream mapping for clean, crash-free playback
        for q_lbl in real_qualities:
            cap_match = re.search(r"(\d+)p", q_lbl)
            target_h = int(cap_match.group(1)) if cap_match else None

            # 1. If 360p or lower, prefer direct progressive mp4 stream (format 18) only if user didn't pick a specific audio language
            if target_h and target_h <= 360 and progressive and not (clean_audio_lang and clean_audio_lang not in ("default", "original", "none")):
                matching_prog = [f for f in progressive if (f.get("height") or 0) == target_h and f.get("url")]
                if matching_prog:
                    quality_urls_map[q_lbl] = matching_prog[0]["url"]
                    continue
                p_fallback = select_progressive_youtube_format(formats, q_lbl)
                if p_fallback and p_fallback.get("url"):
                    quality_urls_map[q_lbl] = p_fallback["url"]
                    continue

            # 2. For HD/SD tiers, pair HLS video with HLS audio into a streamable Master Playlist
            if target_h and hls_audio_fmt and hls_audio_fmt.get('url'):
                matching_hls_v = [f for f in hls_video_formats if (f.get("height") or 0) == target_h and f.get("url")]
                if not matching_hls_v:
                    matching_hls_v = [f for f in hls_video_formats if (f.get("height") or 0) <= target_h and f.get("url")]
                    matching_hls_v.sort(key=lambda f: f.get("height") or 0, reverse=True)
                # Universal hardware decoder compatibility: Prefer AVC1 (H.264) over VP9
                matching_hls_v.sort(key=lambda f: (1 if 'avc1' in str(f.get('vcodec') or '') else 0), reverse=True)

                if matching_hls_v:
                    v_fmt = matching_hls_v[0]
                    v_w = v_fmt.get('width') or (target_h * 16 // 9)
                    v_h = v_fmt.get('height') or target_h
                    bw = int((v_fmt.get('tbr') or (target_h * 3.5)) * 1000)
                    a_url = hls_audio_fmt['url']
                    v_url = v_fmt['url']
                    v_codec = v_fmt.get('vcodec') or 'avc1.640028'
                    codecs_str = f'CODECS="{v_codec},mp4a.40.2",' if v_codec else ''
                    selected_lang_name = (hls_audio_fmt.get('language') or clean_audio_lang or 'Audio').capitalize()
                    media_audio_lines = [
                        f'#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="{selected_lang_name}",LANGUAGE="{clean_audio_lang[:2] if clean_audio_lang else "en"}",DEFAULT=YES,AUTOSELECT=YES,CHANNELS="2",URI="{hls_audio_fmt["url"]}"'
                    ]
                    master_m3u8 = (
                        "#EXTM3U\n"
                        "#EXT-X-VERSION:6\n"
                        "#EXT-X-INDEPENDENT-SEGMENTS\n" +
                        "\n".join(media_audio_lines) + "\n" +
                        f'#EXT-X-STREAM-INF:BANDWIDTH={bw},RESOLUTION={v_w}x{v_h},{codecs_str}AUDIO="audio"\n'
                        f'{v_url}\n'
                    )
                    quality_urls_map[q_lbl] = register_dynamic_playlist(f"yt_master_{target_h}.m3u8", master_m3u8)
                    continue

            # 3. Progressive fallback if available
            if target_h:
                matching_prog = [f for f in progressive if (f.get("height") or 0) == target_h and f.get("url")]
                if matching_prog:
                    quality_urls_map[q_lbl] = matching_prog[0]["url"]
                elif progressive:
                    quality_urls_map[q_lbl] = progressive[0]["url"]

        # Map "Best Available"
        top_res_key = next((q for q in real_qualities if q != "Best Available" and q in quality_urls_map), None)
        if top_res_key:
            quality_urls_map["Best Available"] = quality_urls_map[top_res_key]
        elif progressive and not (clean_audio_lang and clean_audio_lang not in ("default", "original", "none")):
            progressive_sorted = sorted(progressive, key=lambda f: f.get("height") or 0, reverse=True)
            quality_urls_map["Best Available"] = progressive_sorted[0].get("url")
        else:
            for q_lbl in real_qualities:
                if q_lbl != "Best Available" and q_lbl in quality_urls_map:
                    quality_urls_map["Best Available"] = quality_urls_map[q_lbl]
                    break

        # Map audio-only qualities
        audio_formats = [
            f for f in formats
            if isinstance(f, dict) and f.get("url") and f.get("acodec") not in (None, "none") and f.get("vcodec") in (None, "none")
        ]
        if audio_formats:
            audio_formats_sorted = sorted(audio_formats, key=lambda f: f.get("abr") or f.get("tbr") or 0, reverse=True)
            best_audio_url = audio_formats_sorted[0].get("url")
            for a_lbl in ["Best Audio (320kbps)", "High Audio (256kbps)", "Standard Audio (192kbps)", "Compact Audio (128kbps)", "Audio Only"]:
                quality_urls_map[a_lbl] = best_audio_url

        audio_choices = []
        seen_langs = set()
        for af in hls_audio_formats:
            lang_code = str(af.get('language') or '').lower()
            note = str(af.get('format_note') or '')
            if lang_code:
                if 'en' in lang_code: lang_display = 'English'
                elif 'hi' in lang_code: lang_display = 'Hindi'
                elif 'es' in lang_code: lang_display = 'Spanish'
                elif 'fr' in lang_code: lang_display = 'French'
                elif 'de' in lang_code: lang_display = 'German'
                elif 'ja' in lang_code: lang_display = 'Japanese'
                elif 'ko' in lang_code: lang_display = 'Korean'
                elif 'pt' in lang_code: lang_display = 'Portuguese'
                elif 'ru' in lang_code: lang_display = 'Russian'
                elif 'zh' in lang_code: lang_display = 'Chinese'
                elif 'ar' in lang_code: lang_display = 'Arabic'
                elif 'ta' in lang_code: lang_display = 'Tamil'
                elif 'te' in lang_code: lang_display = 'Telugu'
                elif 'bn' in lang_code: lang_display = 'Bengali'
                elif 'id' in lang_code: lang_display = 'Indonesian'
                else: lang_display = lang_code.upper()
            elif 'default' in note.lower() or 'original' in note.lower():
                lang_display = 'Original'
            else:
                lang_display = note.strip().capitalize() or 'Audio'

            if lang_display in seen_langs:
                continue
            seen_langs.add(lang_display)
            audio_choices.append({
                "language": lang_display,
                "url": url,
            })

        def get_audio_priority(choice):
            lang = choice.get("language", "").lower()
            if "hindi" in lang: return 1
            if "english" in lang: return 2
            if "original" in lang or "default" in lang: return 3
            world_prio = [
                "tamil", "telugu", "bengali", "spanish", "french", "arabic",
                "portuguese", "russian", "german", "japanese", "korean", "chinese", "indonesian"
            ]
            for i, w in enumerate(world_prio):
                if w in lang: return 10 + i
            return 100

        audio_choices.sort(key=get_audio_priority)

        stream_url = None
        if clean_quality in quality_urls_map:
            stream_url = quality_urls_map[clean_quality]
        else:
            clean_q_lower = clean_quality.lower()
            for q_key, q_val in quality_urls_map.items():
                if clean_q_lower in q_key.lower() or \
                   (clean_q_lower.startswith("1080") and "1080" in q_key) or \
                   (clean_q_lower.startswith("720") and "720" in q_key) or \
                   (clean_q_lower.startswith("480") and "480" in q_key) or \
                   (clean_q_lower.startswith("360") and "360" in q_key):
                    stream_url = q_val
                    break
        if not stream_url and quality_urls_map.get("Best Available"):
            stream_url = quality_urls_map["Best Available"]
        elif not stream_url and progressive:
            progressive_sorted = sorted(progressive, key=lambda f: f.get("height") or 0, reverse=True)
            stream_url = progressive_sorted[0].get("url")
        else:
            v_fmts = [f for f in formats if f.get("vcodec") != "none" and f.get("url")]
            if v_fmts:
                v_sorted = sorted(v_fmts, key=lambda f: f.get("height") or 0, reverse=True)
                stream_url = v_sorted[0].get("url")
            else:
                any_fmts = [f for f in formats if f.get("url")]
                if any_fmts:
                    stream_url = any_fmts[0].get("url")
                else:
                    raise Exception("No direct playable stream could be extracted from YouTube format manifest")

        if not stream_url or stream_url == url or "youtube.com" in stream_url or "youtu.be" in stream_url:
            raise Exception("Failed to resolve direct playable stream URL for this YouTube video")

        thumb = info.get("thumbnail") or (info.get("thumbnails", [{}])[-1].get("url") if info.get("thumbnails") else "") or (f"https://i.ytimg.com/vi/{info.get('id')}/hqdefault.jpg" if info.get('id') else "")

        print(f"[OK] Stream resolved: {info.get('title', 'Video')}")
        return {
            "url": stream_url,
            "stream_url": stream_url,
            "direct_url": stream_url,
            "target_url": url,
            "title": info.get("title", "Resolved video"),
            "source": info.get("extractor_key", "YouTube"),
            "poster": thumb,
            "quality": clean_quality,
            "qualities": real_qualities,
            "quality_urls": quality_urls_map,
            "audios": audio_choices,
            "audio_choices": audio_choices,
            "selected_audio_lang": (audio_lang or (audio_choices[0]["language"] if audio_choices else "")),
            "audio_only": False,
            "can_stream": True,
            "media_type": "video",
            "headers": info.get("http_headers", {}),
            "stream_headers": info.get("http_headers", {}),
        }

    if any(tg in lower for tg in ["t.me/", "telegram.me/", "telegram.dog/"]):
        print("> Resolving Telegram link...")
        parsed = urllib.parse.urlparse(url)
        path_parts = [p for p in parsed.path.split('/') if p]
        title = "Telegram media"
        stream_url = None
        can_stream = False
        media_type = "file"
        embed_url = None

        if len(path_parts) >= 3 and path_parts[0] == 'c':
            msg_id = path_parts[2]
            title = f"Telegram Document #{msg_id}"
        elif len(path_parts) >= 2:
            channel = path_parts[1] if path_parts[0] == 's' else path_parts[0]
            msg_id = path_parts[-1]
            title = f"Telegram {channel} #{msg_id}"
            embed_url = f"https://t.me/s/{channel}/{msg_id}"

        if embed_url:
            headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"}
            try:
                r = requests.get(embed_url, headers=headers, timeout=2)
                if r.status_code == 200:
                    soup = BeautifulSoup(r.text, 'html.parser')
                    video = soup.find('video')
                    if video and video.get('src'):
                        stream_url = video['src']
                        can_stream = True
                        media_type = "video"
                    doc = soup.find('a', class_=re.compile(r'tgme_widget_message_document'))
                    if doc:
                        doc_title = doc.find(class_=re.compile(r'document_title'))
                        if doc_title and doc_title.text:
                            title = doc_title.text.strip()
                        if doc.get('href') and not stream_url:
                            stream_url = doc['href']
                        can_stream = False
                        media_type = "file"
            except Exception as e:
                print(f"[WARN] Telegram web embed check: {e}")
        if not stream_url:
            stream_url = url
        print(f"[OK] Telegram link resolved: {title} (can_stream={can_stream})")
        return {
            "url": stream_url,
            "target_url": url,
            "direct_url": stream_url if can_stream else "",
            "title": title,
            "source": "Telegram",
            "quality": "Best",
            "qualities": ["Best Available"],
            "can_stream": can_stream,
            "media_type": media_type,
        }

    # Check DDL / Bypasser services (MediaFire, TeraBox, Google Drive, Pixeldrain, Dropbox, etc.)
    if HAS_BYPASSER and any(domain in lower for domain in [
        "mediafire.com", "terabox.", "1024tera.", "teraboxapp.", "drive.google.com",
        "pixeldrain.com", "dropbox.com", "wetransfer.com", "solidfiles.com", "anonfiles.com"
    ]):
        try:
            print(f"> Bypassing direct link for {url}...")
            bypassed = direct_link_generator(url)
            if bypassed and isinstance(bypassed, str) and not bypassed.startswith("ERROR:") and bypassed.startswith(("http://", "https://", "//")):
                if bypassed.startswith("//"):
                    bypassed = "https:" + bypassed
                name = os.path.basename(urllib.parse.urlparse(bypassed).path) or "downloaded_file"
                print(f"[OK] Bypassed link: {name}")
                ext = os.path.splitext(name)[1].lower()
                is_streamable = ext in ('.mp4', '.mkv', '.webm', '.m3u8', '.mpd', '.mov', '.avi', '.ts', '.mp3', '.m4a', '.flac')
                return {
                    "url": bypassed,
                    "title": name,
                    "source": "Direct link",
                    "quality": "Original",
                    "qualities": ["Original"],
                    "can_stream": is_streamable,
                    "media_type": "video" if is_streamable else "file",
                }
        except Exception as e:
            print(f"[WARN] DDL bypass error: {e}")

    if url.startswith(("http://", "https://")):
        name = os.path.basename(urllib.parse.urlparse(url).path) or "Direct file"
        content_type = ""
        try:
            head_res = requests.head(url, headers=HEADERS, timeout=2.5, allow_redirects=True)
            cd = head_res.headers.get("content-disposition", "")
            content_type = head_res.headers.get("content-type", "").lower()
            if cd:
                m_star = re.search(r"filename\*\s*=\s*([a-zA-Z0-9_\-]+)''([^;\r\n]+)", cd, re.I)
                if m_star:
                    try:
                        name = urllib.parse.unquote(m_star.group(2), encoding=m_star.group(1))
                    except Exception:
                        name = urllib.parse.unquote(m_star.group(2))
                else:
                    m_simple = re.search(r'filename\s*=\s*(?:"([^"]+)"|([^;\s\r\n]+))', cd, re.I)
                    if m_simple:
                        name = m_simple.group(1) or m_simple.group(2)
        except Exception:
            pass

        ext = os.path.splitext(name)[1].lower()
        if not ext and content_type:
            if 'zip' in content_type: ext = '.zip'
            elif 'pdf' in content_type: ext = '.pdf'
            elif 'apk' in content_type: ext = '.apk'
            elif 'mp4' in content_type: ext = '.mp4'
            if ext: name += ext

        print(f"[OK] Direct file verified: {name}")
        is_streamable = ext in ('.mp4', '.mkv', '.webm', '.m3u8', '.mpd', '.mov', '.avi', '.ts', '.mp3', '.m4a', '.flac')
        return {
            "url": url,
            "target_url": url,
            "title": name,
            "source": "Direct file",
            "quality": "Best",
            "qualities": ["Best"],
            "can_stream": is_streamable,
            "media_type": "video" if is_streamable else "file",
        }
    raise Exception("Unsupported URL")


def app_download(payload):
    jid = str(payload.get("job_id") or payload.get("id") or "job")
    set_current_job_id(jid)
    url = str(payload.get("url", "")).strip()
    stream_url = str(payload.get("stream_url", "")).strip()
    quality = str(payload.get("quality", "best")).strip() or "best"
    if payload.get("audio_only") or payload.get("is_audio"):
        quality = "audio"
    out_name = payload.get("out_name") or None
    target_dir = payload.get("target_dir") or DEFAULT_DIR
    cache_dir = payload.get("cache_dir") or None
    cookie = payload.get("cookie") or None
    telegram_config = payload.get("telegram") if isinstance(payload.get("telegram"), dict) else None
    season = payload.get("season")
    episode = payload.get("episode")
    scope = payload.get("scope", "episode")
    custom_headers = payload.get("headers") if isinstance(payload.get("headers"), dict) else {}
    title = payload.get("title") or "download"

    lower = url.lower()
    stream_lower = stream_url.lower()

    ACTIVE_DOWNLOAD_PROGRESS[jid] = {
        "percent": 0.0,
        "downloaded_str": "0 MB",
        "total_str": "--",
        "speed": "--",
        "eta": "--",
        "status": "Downloading",
        "path": "",
    }

    print(f"> Starting download request for: {title}")
    print(f"> Target URL: {url or stream_url}")
    print(f"> Save directory: {target_dir}")
    print(f"> Quality profile: {quality}")

    path = None

    # Priority 0: DDL links needing bypass (MediaFire, TeraBox, Google Drive, Pixeldrain, etc.)
    if HAS_BYPASSER and any(domain in lower for domain in ["mediafire.com", "terabox.", "1024tera.", "teraboxapp.", "drive.google.com"]):
        try:
            bypassed = direct_link_generator(url)
            if bypassed and isinstance(bypassed, str) and bypassed.startswith(("http://", "https://", "//")):
                if bypassed.startswith("//"):
                    bypassed = "https:" + bypassed
                stream_url = bypassed
                stream_lower = stream_url.lower()
        except Exception as e:
            print(f"[WARN] DDL bypass during download: {e}")

    # Priority 1: India's Got Latent
    if lower.startswith("igl:") or any(dom in lower for dom in ["freeforall.dev", "indiassgetlentp", "okcdn.ru"]):
        try:
            igl_res = app_igl_resolve(url, quality=quality)
        except Exception as e:
            print(f"[WARN] IGL re-resolve failed: {e}")
            if stream_url and stream_url.startswith(("http://", "https://")):
                clean_headers = {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
                }
                igl_res = {
                    "url": stream_url,
                    "title": title,
                    "stream_headers": clean_headers,
                }
            else:
                raise
        clean_name = clean_filename(out_name or igl_res.get("title") or title, default_ext=".mp4", quality=quality)
        if not clean_name.lower().endswith(".mp4"):
            clean_name += ".mp4"

        # If this episode is a YouTube stream, download directly with yt-dlp
        yt_target = igl_res.get("yt_url") or (igl_res["url"] if any(y in igl_res["url"] for y in ("youtube.com", "youtu.be")) else None)
        if yt_target:
            path = handle_youtube_or_video(
                yt_target,
                target_dir=target_dir,
                quality_arg=quality,
                cookie=cookie,
                standalone=True,
                job_id=jid,
                cache_dir=cache_dir,
                out_name=clean_name,
            )
        else:
            headers = igl_res.get("stream_headers") or {}
            path = download_file_stream(igl_res["url"], clean_name, custom_headers=headers, target_dir=target_dir, cache_dir=cache_dir, job_id=jid)

    # Priority 1.5: Direct pre-resolved stream URL (Instant Single Movie / Episode Download)
    elif stream_url and stream_url.startswith(("http://", "https://")) and scope not in ("season", "series") and not any(h in lower or h in stream_lower for h in ["youtube.com", "youtu.be", "googlevideo.com", "t.me/", "telegram.me/", "telegram.dog/"]):
        path_ext = os.path.splitext(urllib.parse.urlparse(stream_url).path)[1].lower()
        default_ext = path_ext if path_ext else ""
        clean_name = clean_filename(out_name or title, default_ext=default_ext, quality=quality)
        dest_path = get_dest_path(clean_name, target_dir)
        s_headers = get_stream_headers_for_url(stream_url, custom_headers)

        if ".m3u8" in stream_url.lower() or "m3u8" in str(s_headers.get("Accept", "")).lower():
            print(f"> Downloading HLS stream via in-process engine: {stream_url[:70]}...")
            ret = download_hls_with_python(stream_url, dest_path, s_headers)
            if ret == 0 and os.path.exists(dest_path) and os.path.getsize(dest_path) > 0:
                path = dest_path
            else:
                raise Exception("HLS stream download failed or produced empty file")
        else:
            print(f"> Downloading direct media stream: {stream_url[:70]}...")
            path = download_file_stream(stream_url, clean_name, custom_headers=s_headers, target_dir=target_dir, cache_dir=cache_dir, job_id=jid)

    # Priority 2: NetMirror movies and series (for batch season/series scope or fallback)
    elif any(nm in lower for nm in ["netmirror.", "net52.cc", "net77.cc", "watch21.", "watch22."]) or season is not None:
        path = handle_netmirror(
            url,
            out_name=out_name,
            target_dir=target_dir,
            cache_dir=cache_dir,
            action_mode="download_only",
            quality_arg=quality,
            target_season=int(season) if season is not None else None,
            target_episode=int(episode) if episode is not None else None,
            download_scope=scope,
            job_id=jid,
            audio_lang=payload.get("audio_lang") or payload.get("selected_audio_lang") or payload.get("audio"),
        )

    # Priority 3: YouTube and supported video platforms
    elif any(yt in lower for yt in ["youtube.com", "youtu.be", "vimeo.com", "tiktok.com", "twitch.tv", "facebook.com", "instagram.com"]):
        path = handle_youtube_or_video(
            url,
            target_dir=target_dir,
            quality_arg=quality,
            cookie=cookie,
            standalone=True,
            job_id=jid,
            cache_dir=cache_dir,
            out_name=out_name or title,
            audio_lang=payload.get("audio_lang") or payload.get("selected_audio_lang") or payload.get("audio"),
        )

    # Priority 4: Telegram media
    elif any(domain in lower for domain in ["t.me/", "telegram.me/", "telegram.dog/"]):
        path = handle_telegram(
            url,
            out_name=out_name,
            target_dir=target_dir,
            telegram_config=telegram_config,
            job_id=jid,
        )

    # Priority 5: Direct CDN stream URL or direct file download
    else:
        target_direct = stream_url if (stream_url and stream_url.startswith(("http://", "https://"))) else url
        path_ext = os.path.splitext(urllib.parse.urlparse(target_direct).path)[1].lower()
        default_ext = path_ext if path_ext else ""
        clean_name = clean_filename(out_name or title, default_ext=default_ext, quality=quality)
        dest_path = get_dest_path(clean_name, target_dir)
        s_headers = get_stream_headers_for_url(target_direct, custom_headers)

        if ".m3u8" in target_direct.lower() or "m3u8" in str(s_headers.get("Accept", "")).lower():
            print(f"> Downloading HLS video stream via in-process engine: {target_direct[:70]}...")
            ret = download_hls_with_python(target_direct, dest_path, s_headers)
            if ret == 0 and os.path.exists(dest_path) and os.path.getsize(dest_path) > 0:
                path = dest_path
            else:
                raise Exception("HLS stream download failed or produced empty file")
        else:
            print(f"> Downloading direct media stream: {target_direct[:70]}...")
            path = download_file_stream(target_direct, clean_name, custom_headers=s_headers, target_dir=target_dir, cache_dir=cache_dir, job_id=jid)

    if jid in CANCELLED_DOWNLOAD_JOBS:
        return {"cancelled": True, "status": "Cancelled", "job_id": jid}

    if not path:
        if jid in CANCELLED_DOWNLOAD_JOBS:
            return {"cancelled": True, "status": "Cancelled", "job_id": jid}
        raise Exception("The downloader returned no output file")

    _cleanup_download_cache(cache_dir, target_dir, jid, keep_path=path)

    ACTIVE_DOWNLOAD_PROGRESS[jid] = {
        "percent": 100.0,
        "downloaded_str": "Done",
        "total_str": "Done",
        "speed": "Done",
        "eta": "0s",
        "status": "Complete",
        "path": path,
    }
    return {"path": path, "quality": quality, "target_dir": target_dir, "job_id": jid, "status": "Complete"}


_PENDING_TG_CLIENTS = {}

def app_telegram_check(payload):
    fb_id, fb_hash, fb_sess = _get_fallback_tg_creds()
    raw_sess = payload.get("session")
    session = str(raw_sess).strip() if (raw_sess is not None) else fb_sess
    api_id = re.sub(r'\s+', '', str(payload.get("api_id") or fb_id)).strip()
    api_hash = re.sub(r'\s+', '', str(payload.get("api_hash") or fb_hash)).strip()

    if not api_id or not str(api_id).strip().isdigit():
        raise Exception("API ID must be a numeric integer")
    if not api_hash:
        raise Exception("API HASH is required")
    if not session:
        raise Exception("String session is required")

    missing = len(session) % 4
    if missing:
        session += '=' * (4 - missing)

    try:
        from pyrogram import Client
    except ImportError:
        raise Exception("Pyrogram is not available in environment")

    import asyncio
    try:
        loop = asyncio.get_event_loop()
    except RuntimeError:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)

    async def _verify():
        client = Client(
            "temp_tg_checker",
            api_id=int(api_id),
            api_hash=api_hash,
            session_string=session,
            in_memory=True,
        )
        await client.start()
        try:
            me = await client.get_me()
            user_info = f"{me.first_name or ''} {me.last_name or ''}".strip()
            if me.username:
                user_info += f" (@{me.username})"
            phone = me.phone_number or ""
            return {
                "valid": True,
                "user": user_info or "Telegram User",
                "phone": phone,
                "api_id": api_id,
                "api_hash": api_hash,
                "session": session,
            }
        finally:
            await client.stop()

    try:
        res = loop.run_until_complete(_verify())
        if res.get("valid"):
            _save_fallback_tg_creds(api_id, api_hash, session)
        return res
    except Exception as e:
        err_msg = str(e)
        if "API_ID_INVALID" in err_msg:
            raise Exception("Invalid API ID or Hash")
        if "SESSION_STRING" in err_msg or "SESSION_PASSWORD" in err_msg:
            raise Exception("Invalid or expired string session")
        if "AUTH_KEY" in err_msg:
            raise Exception("Session revoked or expired")
        if "diff" in err_msg.lower() or "check" in err_msg.lower() or "padding" in err_msg.lower():
            raise Exception("Invalid string session format")
        raise Exception(f"Connection failed: {err_msg[:60]}")

def app_telegram_send_code(payload):
    fb_id, fb_hash, _ = _get_fallback_tg_creds()
    api_id = re.sub(r'\s+', '', str(payload.get("api_id") or fb_id)).strip()
    api_hash = re.sub(r'\s+', '', str(payload.get("api_hash") or fb_hash)).strip()
    phone = re.sub(r'[\s\-\(\)]+', '', str(payload.get("phone") or "")).strip()

    if not phone:
        raise Exception("Phone number is required")
    if not api_id or not api_id.isdigit():
        raise Exception("API ID must be numeric")

    try:
        from pyrogram import Client
    except ImportError:
        raise Exception("Pyrogram is not available in environment")

    import asyncio
    try:
        loop = asyncio.get_event_loop()
    except RuntimeError:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)

    client = Client(":memory:", api_id=int(api_id), api_hash=api_hash, in_memory=True)

    async def _send():
        await client.connect()
        sent = await client.send_code(phone)
        return sent.phone_code_hash, getattr(sent, "timeout", 60)

    phone_code_hash, timeout = loop.run_until_complete(_send())
    _PENDING_TG_CLIENTS[phone] = {
        "client": client,
        "phone": phone,
        "phone_code_hash": phone_code_hash,
        "api_id": api_id,
        "api_hash": api_hash,
        "loop": loop
    }
    return {
        "ok": True,
        "phone": phone,
        "phone_code_hash": phone_code_hash,
        "timeout": timeout
    }

def _safe_export_session(client, loop):
    res = client.export_session_string()
    if asyncio.iscoroutine(res):
        return loop.run_until_complete(res)
    return res

def app_telegram_sign_in(payload):
    phone = re.sub(r'[\s\-\(\)]+', '', str(payload.get("phone") or "")).strip()
    code = re.sub(r'\s+', '', str(payload.get("code") or "")).strip()
    phone_code_hash = str(payload.get("phone_code_hash") or "").strip()

    if not phone or not code:
        raise Exception("Phone and verification code are required")

    entry = _PENDING_TG_CLIENTS.get(phone)
    if not entry or not entry.get("client"):
        raise Exception("No active login session found for this phone. Please request a new code.")

    client = entry["client"]
    loop = entry["loop"]
    h = phone_code_hash or entry.get("phone_code_hash")

    from pyrogram.errors import SessionPasswordNeeded

    async def _auth():
        try:
            user = await client.sign_in(phone, h, code)
            return user, False
        except SessionPasswordNeeded:
            return None, True
        except Exception as se:
            s_err = str(se)
            if "PHONE_CODE_INVALID" in s_err:
                raise Exception("Invalid verification code")
            if "PHONE_CODE_EXPIRED" in s_err:
                raise Exception("Verification code expired. Please resend code")
            raise Exception(s_err)

    user, need_password = loop.run_until_complete(_auth())
    if need_password:
        return {"ok": True, "need_password": True, "phone": phone}

    session_str = _safe_export_session(client, loop)
    user_name = f"{user.first_name or ''} {user.last_name or ''}".strip() or (user.username or "Telegram User")
    saved_api_id = entry.get("api_id")
    saved_api_hash = entry.get("api_hash")
    _PENDING_TG_CLIENTS.pop(phone, None)
    if session_str:
        _save_fallback_tg_creds(saved_api_id, saved_api_hash, session_str)

    return {
        "ok": True,
        "session": session_str,
        "user": user_name,
        "username": user.username or "",
        "phone": user.phone_number or phone,
        "api_id": saved_api_id,
        "api_hash": saved_api_hash,
    }

def app_telegram_password(payload):
    session_key = str(payload.get("session_key") or "").strip()
    phone = re.sub(r'[\s\-\(\)]+', '', str(payload.get("phone") or "")).strip()
    password = str(payload.get("password") or "").strip()

    if not password:
        raise Exception("2FA password is required")

    entry = _PENDING_TG_CLIENTS.get(session_key) or _PENDING_TG_CLIENTS.get(phone)
    if not entry or not entry.get("client"):
        raise Exception("No active login session found. Please request a new code or QR.")

    client = entry["client"]
    loop = entry["loop"]

    async def _pwd():
        try:
            user = await client.check_password(password)
            return user
        except Exception as pe:
            p_err = str(pe)
            if "PASSWORD_HASH_INVALID" in p_err or "PASSWORD_INVALID" in p_err:
                raise Exception("Incorrect 2FA password")
            raise Exception(p_err)

    user = loop.run_until_complete(_pwd())
    session_str = _safe_export_session(client, loop)
    user_name = f"{user.first_name or ''} {user.last_name or ''}".strip() or (user.username or "Telegram User")
    saved_api_id = entry.get("api_id")
    saved_api_hash = entry.get("api_hash")
    if session_key:
        _PENDING_TG_CLIENTS.pop(session_key, None)
    if phone:
        _PENDING_TG_CLIENTS.pop(phone, None)
    if session_str:
        _save_fallback_tg_creds(saved_api_id, saved_api_hash, session_str)

    return {
        "ok": True,
        "session": session_str,
        "user": user_name,
        "username": user.username or "",
        "phone": user.phone_number or phone,
        "api_id": saved_api_id,
        "api_hash": saved_api_hash,
    }

def app_telegram_qr_init(payload):
    fb_id, fb_hash, _ = _get_fallback_tg_creds()
    api_id = int(re.sub(r'\s+', '', str(payload.get("api_id") or fb_id)).strip())
    api_hash = re.sub(r'\s+', '', str(payload.get("api_hash") or fb_hash)).strip()

    try:
        from pyrogram import Client
        from pyrogram.raw.functions.auth import ExportLoginToken
    except ImportError:
        raise Exception("Pyrogram is not available in environment")

    import asyncio, base64
    try:
        loop = asyncio.get_event_loop()
    except RuntimeError:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)

    client = Client(":memory:", api_id=api_id, api_hash=api_hash, in_memory=True)

    async def _qr():
        await client.connect()
        token_res = await client.invoke(ExportLoginToken(api_id=api_id, api_hash=api_hash, except_ids=[]))
        return token_res

    res = loop.run_until_complete(_qr())
    token_b64 = base64.urlsafe_b64encode(res.token).decode("utf-8").rstrip("=")
    qr_url = f"tg://login?token={token_b64}"

    session_key = f"qr_{int(time.time())}"
    _PENDING_TG_CLIENTS[session_key] = {
        "client": client,
        "loop": loop,
        "api_id": api_id,
        "api_hash": api_hash,
        "expires": getattr(res, "expires", 0),
    }

    return {
        "ok": True,
        "session_key": session_key,
        "qr_url": qr_url,
        "token": token_b64,
        "expires": getattr(res, "expires", 0)
    }

def app_telegram_qr_check(payload):
    session_key = str(payload.get("session_key") or "").strip()
    entry = _PENDING_TG_CLIENTS.get(session_key)
    if not entry or not entry.get("client"):
        raise Exception("QR session expired or not found")

    client = entry["client"]
    loop = entry["loop"]

    from pyrogram.raw.functions.auth import ExportLoginToken, ImportLoginToken
    from pyrogram.raw.types.auth import LoginTokenSuccess, LoginTokenMigrateTo
    from pyrogram.session import Auth, Session
    from pyrogram.errors import SessionPasswordNeeded

    async def _chk():
        token_res = await client.invoke(ExportLoginToken(api_id=entry["api_id"], api_hash=entry["api_hash"], except_ids=[]))
        if isinstance(token_res, LoginTokenMigrateTo):
            # The user's account is on a different DC (e.g. DC 5 for Asia/India)
            # Migrate the Pyrogram client session to that DC
            await client.session.stop()
            await client.storage.dc_id(token_res.dc_id)
            await client.storage.auth_key(
                await Auth(
                    client, await client.storage.dc_id(),
                    await client.storage.test_mode()
                ).create()
            )
            client.session = Session(
                client, await client.storage.dc_id(),
                await client.storage.auth_key(),
                await client.storage.test_mode()
            )
            await client.session.start()
            # Import the accepted login token on the migrated DC
            token_res = await client.invoke(ImportLoginToken(token=token_res.token))
        return token_res

    try:
        res = loop.run_until_complete(_chk())
    except SessionPasswordNeeded:
        return {"ok": True, "connected": False, "need_password": True, "session_key": session_key}
    except Exception as e:
        err_msg = str(e)
        if "SESSION_PASSWORD_NEEDED" in err_msg:
            return {"ok": True, "connected": False, "need_password": True, "session_key": session_key}
        return {"ok": True, "connected": False, "error": err_msg}

    if isinstance(res, LoginTokenSuccess):
        auth_obj = getattr(res, "authorization", None)
        user = getattr(auth_obj, "user", None) or getattr(res, "user", None)
        if user:
            try:
                loop.run_until_complete(client.storage.user_id(user.id))
                loop.run_until_complete(client.storage.is_bot(False))
            except Exception:
                pass
        session_str = _safe_export_session(client, loop)
        user_name = f"{getattr(user, 'first_name', '')} {getattr(user, 'last_name', '')}".strip() or "Telegram User"
        saved_api_id = entry["api_id"]
        saved_api_hash = entry["api_hash"]
        _PENDING_TG_CLIENTS.pop(session_key, None)
        if session_str:
            _save_fallback_tg_creds(saved_api_id, saved_api_hash, session_str)
        return {
            "ok": True,
            "connected": True,
            "session": session_str,
            "user": user_name,
            "username": getattr(user, 'username', '') or "",
            "phone": getattr(user, 'phone', '') or "",
            "api_id": saved_api_id,
            "api_hash": saved_api_hash,
        }
    return {"ok": True, "connected": False}

def app_telegram_logout(payload):
    _clear_fallback_tg_creds()
    global _PENDING_TG_CLIENTS
    for k, v in list(_PENDING_TG_CLIENTS.items()):
        try:
            cl = v.get("client")
            lp = v.get("loop")
            if cl and lp and cl.is_connected:
                lp.run_until_complete(cl.stop())
        except Exception:
            pass
    _PENDING_TG_CLIENTS.clear()

    fb_id, fb_hash, _ = _get_fallback_tg_creds()
    raw_api_id = str(payload.get("api_id") or fb_id or "30909654").strip()
    api_id = re.sub(r'\D+', '', raw_api_id) or "30909654"
    api_hash = re.sub(r'\s+', '', str(payload.get("api_hash") or fb_hash or "d4b340a406c5c1ed1d7f26d44749602b")).strip()
    session = re.sub(r'\s+', '', str(payload.get("session") or payload.get("session_string") or "")).strip()

    if not session:
        return {"ok": True, "logged_out": True, "message": "No active session to terminate"}

    missing = len(session) % 4
    if missing:
        session += '=' * (4 - missing)

    try:
        from pyrogram import Client
    except ImportError:
        return {"ok": True, "logged_out": True}

    import asyncio
    try:
        loop = asyncio.get_event_loop()
    except RuntimeError:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)

    async def _do_logout():
        client = Client(
            "temp_tg_logout",
            api_id=int(api_id),
            api_hash=api_hash,
            session_string=session,
            in_memory=True,
        )
        await client.start()
        try:
            await client.log_out()
        except Exception as e:
            print(f"[WARN] Telegram client log_out: {e}")
        finally:
            if client.is_connected:
                await client.stop()

    try:
        loop.run_until_complete(_do_logout())
        for p in [
            os.path.join(os.path.dirname(__file__), "credentials.local.json"),
            os.path.join(os.path.dirname(__file__), "assets", "credentials.local.json"),
            os.path.expanduser("~/.telegram_credentials.json"),
        ]:
            if os.path.exists(p):
                try: os.remove(p)
                except Exception: pass
        return {"ok": True, "logged_out": True}
    except Exception as e:
        print(f"[WARN] Telegram logout: {e}")
        for p in [
            os.path.join(os.path.dirname(__file__), "credentials.local.json"),
            os.path.join(os.path.dirname(__file__), "assets", "credentials.local.json"),
            os.path.expanduser("~/.telegram_credentials.json"),
        ]:
            if os.path.exists(p):
                try: os.remove(p)
                except Exception: pass
        return {"ok": True, "logged_out": True, "error": str(e)}


def sanitize_app_logs(raw_text):
    clean = re.sub(r'\x1b\[[0-9;]*[a-zA-Z]|\x1b\([a-zA-Z]', '', raw_text)
    clean = re.sub(r'[\r\x00-\x08\x0b\x0c\x0e-\x1f]', '', clean)
    clean = re.sub(r'>\s*Searching platform:[^\n]*\n?', '', clean, flags=re.I)
    clean = re.sub(r'>\s*Detected NetMirror link\.\.\.\n?', '', clean, flags=re.I)
    clean = re.sub(r'>\s*Resolving target: https?://[^\n]*\n?', '> Resolving target media...\n', clean, flags=re.I)
    clean = re.sub(r'\(vidsync\.pro\)', '', clean, flags=re.I)
    clean = re.sub(r'\(watchbox\)', '', clean, flags=re.I)
    clean = re.sub(r'(?i)netmirror\.[a-z]+', 'catalog', clean)
    clean = re.sub(r'(?i)\bnetmirror\b', 'Catalog', clean)
    clean = re.sub(r'(?i)yt-dlp', 'Engine', clean)
    clean = re.sub(r'\n{3,}', '\n\n', clean).strip()
    return clean

def run_app_json_request(payload):
    action = str(payload.get("action", "")).lower().strip()
    if action == "capabilities":
        return {"ok": True, "capabilities": app_cli_capabilities()}
    if action == "progress":
        return {"ok": True, "progress": get_download_progress(payload.get("job_id"))}
    if action == "cancel":
        cancel_download(payload.get("job_id"))
        return {"ok": True, "cancelled": payload.get("job_id")}

    output = io.StringIO()
    try:
        with contextlib.redirect_stdout(output), contextlib.redirect_stderr(output):
            if action == "search":
                result = app_search(payload.get("query", ""), payload.get("platform", "netmirror"))
            elif action == "resolve":
                result = app_resolve(
                    payload.get("url", ""),
                    quality=payload.get("quality", "best"),
                    season=payload.get("season"),
                    episode=payload.get("episode"),
                    cookie=payload.get("cookie"),
                    audio_lang=payload.get("audio_lang") or payload.get("selected_audio_lang"),
                    telegram_config=payload.get("telegram"),
                )
            elif action == "download":
                result = app_download(payload)
            elif action == "telegram_check":
                result = app_telegram_check(payload)
            elif action == "telegram_send_code":
                result = app_telegram_send_code(payload)
            elif action == "telegram_sign_in":
                result = app_telegram_sign_in(payload)
            elif action == "telegram_password":
                result = app_telegram_password(payload)
            elif action == "telegram_qr_init":
                result = app_telegram_qr_init(payload)
            elif action == "telegram_qr_check":
                result = app_telegram_qr_check(payload)
            elif action == "telegram_logout":
                result = app_telegram_logout(payload)
            elif action == "igl":
                result = app_igl_catalog()
            elif action == "igl_resolve":
                result = app_igl_resolve(
                    payload.get("data_id") or payload.get("id") or payload.get("url"),
                    quality=payload.get("quality", "best"),
                    season=payload.get("season"),
                    episode=payload.get("episode"),
                )
            elif action == "stream_proxy":
                proxy_url = start_local_stream_proxy(payload.get("url"), custom_headers=payload.get("headers"))
                result = {"ok": True, "proxy_url": proxy_url}
            else:
                raise Exception("Unknown app action. Use capabilities, search, resolve, download, progress, or cancel.")
        
        clean_logs = sanitize_app_logs(output.getvalue())
        return {"ok": True, "action": action, "result": result, "logs": clean_logs[-12000:]}
    except Exception as error:
        clean_logs = sanitize_app_logs(output.getvalue())
        return {"ok": False, "action": action, "error": str(error), "logs": clean_logs[-12000:]}


def run_app_json_request_json(raw_payload):
    """Chaquopy entry point: accept and return JSON strings without Java/Python type conversion."""
    try:
        payload = json.loads(str(raw_payload))
        return json.dumps(run_app_json_request(payload), ensure_ascii=False)
    except Exception as error:
        return json.dumps({"ok": False, "error": str(error)}, ensure_ascii=False)


def main():
    if len(sys.argv) < 2:
        interactive_menu()
        return

    args = sys.argv[1:]
    if args[0] in ('--app-json', '--app-json-b64'):
        if len(args) < 2:
            print(json.dumps({"ok": False, "error": "Missing app request payload"}))
            return
        try:
            raw_payload = args[1]
            if args[0] == '--app-json-b64':
                raw_payload = base64.urlsafe_b64decode(raw_payload.encode('ascii')).decode('utf-8')
            payload = json.loads(raw_payload)
            print(json.dumps(run_app_json_request(payload), ensure_ascii=False))
        except Exception as error:
            print(json.dumps({"ok": False, "error": str(error)}))
        return

    if args[0] in ('-i', '--interactive', 'menu'):
        interactive_menu()
        return

    out_name = None
    target_dir = None
    cookie = None
    quality_arg = None
    
    if '-q' in args:
        idx = args.index('-q')
        if idx + 1 < len(args):
            quality_arg = args[idx + 1]
            args = args[:idx] + args[idx + 2:]
    elif '--quality' in args:
        idx = args.index('--quality')
        if idx + 1 < len(args):
            quality_arg = args[idx + 1]
            args = args[:idx] + args[idx + 2:]

    if '-o' in args:
        idx = args.index('-o')
        if idx + 1 < len(args):
            out_name = args[idx + 1]
            args = args[:idx] + args[idx + 2:]

    if '-d' in args:
        idx = args.index('-d')
        if idx + 1 < len(args):
            target_dir = args[idx + 1]
            args = args[:idx] + args[idx + 2:]

    if '--cookies' in args:
        idx = args.index('--cookies')
        if idx + 1 < len(args):
            cookie = args[idx + 1]
            args = args[:idx] + args[idx + 2:]
    elif '-c' in args:
        idx = args.index('-c')
        if idx + 1 < len(args):
            cookie = args[idx + 1]
            args = args[:idx] + args[idx + 2:]
    elif '--cookie' in args:
        idx = args.index('--cookie')
        if idx + 1 < len(args):
            cookie = args[idx + 1]
            args = args[:idx] + args[idx + 2:]
            
    for raw_url in args:
        url = "".join(raw_url.split()) if raw_url.startswith(('http://', 'https://')) else raw_url
        if url.startswith('http://') or url.startswith('https://') or url.startswith('magnet:'):
            download(url, out_name=out_name, cookie=cookie, target_dir=target_dir, quality_arg=quality_arg)
        elif len(args) == 1 and not url.startswith('-'):
            search_netmirror_and_prompt(url, target_dir=target_dir)
            break
        elif not url.startswith('-'):
            full_query = " ".join(args)
            search_netmirror_and_prompt(full_query, target_dir=target_dir)
            break
        else:
            print(f"[WARN] Skipping invalid argument: {url}")

if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n Exited.")
        try:
            sys.exit(0)
        except SystemExit:
            os._exit(0)
