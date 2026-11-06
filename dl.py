#!/usr/bin/env python3
"""
Universal Downloader CLI (d.py / dl)
Supports YouTube/Video quality selector CLI (yt-dlp), Direct Files, Link Bypassing (MediaFire, Terabox, etc.), SelfStudys, StudiesToday, Studocu.
Auto-syncs downloads to /sdcard/Download via ADB when storage permissions are restricted in Termux.
"""

import sys
import os
import re
import subprocess
import urllib.parse
import shutil
import requests
import html
from bs4 import BeautifulSoup
import socket
try:
    import urllib3.util.connection as urllib3_conn
    urllib3_conn.allowed_gai_family = lambda: socket.AF_INET
except Exception:
    pass

# Lazy-loaded modules for instant CLI startup (0.2s launch)
_YTDLP_MODULE = None

def load_ytdlp():
    global _YTDLP_MODULE
    if _YTDLP_MODULE is None:
        try:
            import yt_dlp as _mod
            _YTDLP_MODULE = _mod
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
    """If saved outside /sdcard/Download, attempt pushing to /sdcard/Download via ADB if available."""
    if file_path and not file_path.startswith("/sdcard"):
        filename = os.path.basename(file_path)
        dest_sdcard = f"/sdcard/Download/{filename}"
        try:
            res = subprocess.run(["adb", "devices"], capture_output=True, text=True, timeout=5)
            if "device" in res.stdout and "emulator" in res.stdout or len(res.stdout.strip().splitlines()) > 1:
                push_res = subprocess.run(["adb", "push", file_path, dest_sdcard], capture_output=True, text=True, timeout=60)
                if push_res.returncode == 0:
                    print(f"✔ Synced to: {dest_sdcard}")
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
    ext = os.path.splitext(basename)[1] or ".mp4"
    dir_name = os.path.dirname(file_path) or "/sdcard/Download"
    
    short_name = None
    for i in range(1, 100):
        cand = f"v{i}{ext}"
        cand_p = os.path.join(dir_name, cand)
        if not os.path.exists(cand_p):
            try:
                os.link(file_path, cand_p)
                short_name = cand
            except Exception:
                try:
                    shutil.copyfile(file_path, cand_p)
                    short_name = cand
                except Exception:
                    pass
            break
        elif os.path.exists(cand_p) and os.path.getsize(cand_p) == os.path.getsize(file_path):
            short_name = cand
            break

    display_name = short_name or basename
    encoded = urllib.parse.quote(display_name)
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
    print(" 🚀 DIRECT LINK FOR ADM (ADVANCED DOWNLOAD MANAGER):")
    print("="*65)
    print(f" 🔗 Copy & Paste into ADM App: {local_url}")
    print(f" 🌐 Wi-Fi LAN Download Link:  {lan_url}")
    print("="*65 + "\n")
    try:
        subprocess.run(["termux-clipboard-set"], input=local_url.encode('utf-8'), capture_output=True)
        print(" 📋 Short link copied to Android Clipboard!")
    except Exception:
        pass

def clean_filename(title, default_ext=".pdf"):
    title = re.sub(r'[\\/*?:"<>|]', '', title)
    title = re.sub(r'\s+', '_', title).strip('_')
    if not title:
        title = "downloaded_file"
    if not os.path.splitext(title)[1]:
        title += default_ext
    return title

def get_dest_path(filename, target_dir=None):
    out_dir = target_dir or DEFAULT_DIR
    os.makedirs(out_dir, exist_ok=True)
    if os.path.isabs(filename):
        return filename
    return os.path.join(out_dir, filename)

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
    print(" ⚠ Network connection lost! Download paused.")
    print(" ➜ Waiting for internet connection to recover...")
    print("!"*60, flush=True)
    
    down_start = time.time()
    while not check_network():
        time.sleep(3)
        
    down_duration = int(time.time() - down_start)
    print("\n" + "="*60)
    print(f" ✔ Network connection restored (was offline for {down_duration}s)!")
    print(" ➜ Resuming download...")
    print("="*60, flush=True)

def download_file_stream(url, filename, custom_headers=None, target_dir=None):
    headers = {**HEADERS, **(custom_headers or {})}
    print(f"➜ Fetching: {url}")
    
    dest_path = get_dest_path(filename, target_dir)
    downloaded = 0
    total_bytes = 0

    while True:
        try:
            req_headers = dict(headers)
            if downloaded > 0:
                req_headers['Range'] = f'bytes={downloaded}-'
            
            with requests.get(url, headers=req_headers, stream=True, timeout=30) as r:
                if r.status_code == 416: # Range Not Satisfiable (already completed)
                    break
                r.raise_for_status()
                
                content_type = r.headers.get('content-type', '').lower()
                content_disp = r.headers.get('content-disposition', '')
                
                if downloaded == 0:
                    if 'filename=' in content_disp:
                        match = re.search(r'filename=["\']?([^"\';]+)["\']?', content_disp)
                        if match:
                            filename = clean_filename(match.group(1))
                            dest_path = get_dest_path(filename, target_dir)
                    elif 'application/pdf' in content_type and not filename.endswith('.pdf'):
                        filename += '.pdf'
                        dest_path = get_dest_path(filename, target_dir)
                    elif 'text/html' in content_type and not filename.endswith('.html'):
                        raise Exception("The server returned an HTML webpage instead of a direct file stream.")

                    total_length = r.headers.get('content-length')
                    total_bytes = int(total_length) if total_length and total_length.isdigit() else 0

                mode = 'ab' if downloaded > 0 and r.status_code == 206 else 'wb'
                if mode == 'wb':
                    downloaded = 0

                print(f"➜ Saving to: {dest_path}")
                with open(dest_path, mode) as f:
                    for chunk in r.iter_content(chunk_size=65536):
                        if chunk:
                            f.write(chunk)
                            downloaded += len(chunk)
                            if total_bytes > 0:
                                percent = (downloaded / total_bytes) * 100
                                mb_done = downloaded / (1024 * 1024)
                                mb_total = total_bytes / (1024 * 1024)
                                print(f"\rDownloading... [{mb_done:.2f} MB / {mb_total:.2f} MB] {percent:.1f}%", end='', flush=True)
                            else:
                                mb_done = downloaded / (1024 * 1024)
                                print(f"\rDownloading... [{mb_done:.2f} MB]", end='', flush=True)
            break
        except (requests.exceptions.RequestException, socket.error) as e:
            if not check_network():
                wait_for_network()
            else:
                print(f"\n⚠ Network hiccup: {e}. Retrying in 3s...", flush=True)
                time.sleep(3)

    print("\n✔ Download complete!")
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
            print(f"➜ Direct link bypassed: {direct_url[:80]}...")
            parsed = urllib.parse.urlparse(direct_url)
            base = os.path.basename(parsed.path)
            filename = out_name or (base if base and '.' in base else "downloaded_file")
            return download_file_stream(direct_url, clean_filename(filename), target_dir=target_dir)
    except Exception as e:
        pass
    return None

def handle_selfstudys(url, target_dir=None):
    print("➜ Detected SelfStudys link...")
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
    print("➜ Detected StudiesToday link...")
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
        print(f"✖ {msg}", flush=True)
        msg_l = msg.lower()
        if "failed to resolve" in msg_l or "unreachable" in msg_l or "connection" in msg_l:
            if not check_network():
                wait_for_network()

def get_ytdlp_opts(custom_opts=None, cookie=None):
    opts = {}
    if HAS_YTDLP:
        try:
            _, _, _, default_opts = yt_dlp.parse_options([])
            opts.update(default_opts)
        except Exception:
            pass

    speed_opts = {
        'nopart': True,
        'retries': 100,
        'fragment_retries': 100,
        'file_access_retries': 10,
        'concurrent_fragment_downloads': 16,
        'logger': NetworkAwareLogger(),
    }

    node_bin = shutil.which('node') or '/data/data/com.termux/files/usr/bin/node'
    if os.path.exists(node_bin):
        speed_opts['js_runtimes'] = {'node': {'path': node_bin}}

    if shutil.which('aria2c'):
        speed_opts['external_downloader'] = {'default': 'aria2c'}
        speed_opts['external_downloader_args'] = {
            'default': ['-j', '16', '-s', '16', '-x', '16', '-k', '1M', '--min-split-size=1M', '--allow-overwrite=true', '--disable-ipv6=true', '--async-dns=false', '--file-allocation=falloc']
        }

    opts.update(speed_opts)

    if not cookie:
        for possible_cookie in ["/sdcard/Download/cookies.txt", "/sdcard/Download/youtube_cookies.txt", os.path.expanduser("~/cookies.txt")]:
            if os.path.isfile(possible_cookie):
                cookie = possible_cookie
                break

    if cookie:
        if os.path.isfile(cookie):
            opts['cookiefile'] = cookie
        else:
            opts.setdefault('http_headers', {})['Cookie'] = cookie
    if custom_opts:
        opts.update(custom_opts)
    return opts

def prompt_video_quality(url, quality_arg=None, cookie=None):
    """Interactively or via argument select video quality / format CLI."""
    if not HAS_YTDLP:
        return 'bestvideo+bestaudio/best', False

    # Standard format map (bestvideo+bestaudio ensures 4K/2K/1080p max streams)
    format_map = {
        '1': ('bestvideo+bestaudio/best', 'Best Available (Auto - Highest Quality)'),
        '2160p': ('bestvideo[height<=2160]+bestaudio/best[height<=2160]/best', '2160p (4K)'),
        '1440p': ('bestvideo[height<=1440]+bestaudio/best[height<=1440]/best', '1440p (2K)'),
        '1080p': ('bestvideo[height<=1080]+bestaudio/best[height<=1080]/best', '1080p (Full HD)'),
        '720p': ('bestvideo[height<=720]+bestaudio/best[height<=720]/best', '720p (HD)'),
        '480p': ('bestvideo[height<=480]+bestaudio/best[height<=480]/best', '480p (SD)'),
        '360p': ('bestvideo[height<=360]+bestaudio/best[height<=360]/best', '360p (Low)'),
        '240p': ('bestvideo[height<=240]+bestaudio/best', '240p'),
        '144p': ('bestvideo[height<=144]+bestaudio/best', '144p'),
        'audio': ('bestaudio/best', 'Audio Only (MP3/M4A)'),
        'mp3': ('bestaudio/best', 'Audio Only (MP3)'),
    }

    # If quality was specified via CLI flag -q / --quality
    if quality_arg:
        q_clean = str(quality_arg).lower().strip()
        if q_clean in format_map:
            return format_map[q_clean][0], (q_clean in ('audio', 'mp3'))
        if q_clean.isdigit():
            val = int(q_clean)
            if val in (2160, 1440, 1080, 720, 480, 360, 240, 144):
                key = f"{val}p"
                return format_map[key][0], False

    # Extract available qualities using yt-dlp
    print("➜ Extracting video info and available qualities...")
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
        print(f"⚠ Warning: Could not fetch format metadata ({e}). Using auto best quality.")
        return 'bestvideo+bestaudio/best', False

    # If non-interactive shell (stdin not a tty), return default
    if not sys.stdin.isatty():
        print(f"🎬 Video: {title}")
        print("➜ Non-interactive mode detected. Downloading in Best Available quality.")
        return format_map['1'][0], False

    print("\n" + "="*55)
    print(f" 🎬 Video: {title}")
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
        user_input = input(f"➜ Select choice [1-{len(options)}] (Default: 1): ").strip()
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

    print("➜ Selection invalid or default picked. Downloading Best Available quality.")
    return format_map['1'][0], False

def handle_youtube_or_video(url, target_dir=None, quality_arg=None, cookie=None):
    if not HAS_YTDLP:
        print("⚠ yt-dlp is not installed. Falling back to generic download...")
        return False
        
    try:
        format_spec, is_audio_only = prompt_video_quality(url, quality_arg=quality_arg, cookie=cookie)
    except Exception as e:
        print(f"✖ Error extracting video metadata: {e}")
        return True
    
    out_dir = target_dir or DEFAULT_DIR
    os.makedirs(out_dir, exist_ok=True)
    
    print(f"➜ Downloading video via yt-dlp [{format_spec}]...")
    ydl_opts = get_ytdlp_opts({
        'format': format_spec,
        'outtmpl': os.path.join(out_dir, '%(title)s.%(ext)s'),
        'quiet': False,
        'no_warnings': True,
    }, cookie=cookie)
    
    if is_audio_only:
        ydl_opts['postprocessors'] = [{
            'key': 'FFmpegExtractAudio',
            'preferredcodec': 'mp3',
            'preferredquality': '192',
        }]
    else:
        ydl_opts['merge_output_format'] = 'mp4'
        if shutil.which("aria2c"):
            ydl_opts['external_downloader'] = 'aria2c'
            ydl_opts['external_downloader_args'] = {'aria2c': ['-s', '16', '-x', '16', '-k', '1M', '--console-log-level=warn']}

    max_attempts = 5
    for attempt in range(1, max_attempts + 1):
        try:
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                info = ydl.extract_info(url, download=True)
                if not info:
                    raise Exception("yt-dlp could not extract video info. Content may be members-only or private.")
                filename = ydl.prepare_filename(info)
                base, ext = os.path.splitext(filename)
                if is_audio_only:
                    mp3_file = base + ".mp3"
                    if not os.path.exists(mp3_file) and os.path.exists(filename):
                        try:
                            subprocess.run(["ffmpeg", "-y", "-i", filename, "-codec:a", "libmp3lame", "-qscale:a", "2", mp3_file], check=True, capture_output=True)
                            filename = mp3_file
                        except Exception:
                            pass
                    elif os.path.exists(mp3_file):
                        filename = mp3_file
                sync_to_sdcard_if_needed(filename)
                print_adm_link(filename)
            return True
        except Exception as e:
            err_str = str(e)
            print(f"\n✖ Download error encountered: {err_str}")

            if not check_network():
                wait_for_network()
                continue

            if "Conversion failed" in err_str or "Postprocessing" in err_str or "obu_forbidden_bit" in err_str or "Invalid data" in err_str:
                print("\n" + "!"*60)
                print(" ⚠ Postprocessing failed: Video stream fragments were corrupted due to network disconnection.")
                print(" ➜ Cleaning corrupted partial fragments & retrying with network-resilient H.264 format...")
                print("!"*60 + "\n")
                for f in os.listdir(out_dir):
                    if ".f" in f or ".temp" in f or ".part" in f or ".ytdl" in f:
                        try:
                            os.remove(os.path.join(out_dir, f))
                            print(f"  - Cleaned partial fragment: {f}")
                        except Exception:
                            pass
                ydl_opts['format'] = 'bestvideo+bestaudio/best'
                continue

            if '403' in err_str or 'Forbidden' in err_str:
                print("⚠ HTTP 403 Forbidden encountered. Retrying with fallback player clients...")
                ydl_opts['extractor_args'] = {'youtube': {'player_client': ['mweb', 'android', 'ios', 'web']}}
                continue

            if attempt < max_attempts:
                print(f"➜ Retrying download (attempt {attempt}/{max_attempts})...")
                time.sleep(3)
            else:
                print("✖ Download failed after retries.")
                return True
    return True

def handle_studocu(url, out_name=None, custom_headers=None, target_dir=None):
    print("➜ Detected Studocu link...")
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
            print("\n✖ Access Denied (403 Forbidden) by Studocu.")
            print("➜ Studocu download links require active browser authentication cookies.")
            print("➜ Usage with Cookie:")
            print("   dl \"<URL>\" -c \"session_cookie_string\"")
            print("   or set STUDOCU_COOKIE=\"...\" in your shell.\n")
            return None
        raise e

def handle_telegram(url, out_name=None, target_dir=None):
    print("➜ Detected Telegram link...")
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
                chat_id = int(f"-100{path_parts[1]}")
            except ValueError:
                chat_id = path_parts[1]
            if path_parts[2].isdigit():
                msg_id = int(path_parts[2])
        else:
            chat_id = path_parts[0]
            if path_parts[1].isdigit():
                msg_id = int(path_parts[1])

    def load_tg_creds():
        api_id = os.environ.get("TELEGRAM_API_ID")
        api_hash = os.environ.get("TELEGRAM_API_HASH")
        session = os.environ.get("TELEGRAM_SESSION")
        if not (api_id and api_hash and session):
            agents_file = os.path.expanduser("~/.agents/AGENTS.md")
            if os.path.exists(agents_file):
                try:
                    with open(agents_file, "r") as f:
                        text = f.read()
                    m_id = re.search(r"App API_ID.*?:\s*`?(\d+)`?", text)
                    m_hash = re.search(r"App API_HASH.*?:\s*`?([a-f0-9]{32})`?", text)
                    m_sess = re.search(r"Assistant STRING_SESSION:\s*`?([A-Za-z0-9_\-]+)`?", text)
                    if m_id and not api_id: api_id = int(m_id.group(1))
                    if m_hash and not api_hash: api_hash = m_hash.group(1)
                    if m_sess and not session: session = m_sess.group(1)
                except Exception:
                    pass
        return api_id or 30909654, api_hash or "d4b340a406c5c1ed1d7f26d44749602b", session or "BQHjZYEAnIZGwaOzXIdkpN8pw4eT_rR7zKag16sq6ry9U0n3Fgo5H5EP1K5COi3Mu3OHXsO1iHtsEAo4pjjfcyv4Y1Es0JxijQ-po3SZYsEGMCMosi5S9tTQJwtQ9sz0bWVOO3zSJOhwTtY8LGCMk72gFsaIxmcf52vIea1USV31FJOcVmNPlHag4JqYl3gPK9Fz0rPrYsUkimXhMxbvsDijcVnX--jLrkhL6ZyhS1INiq9VEsswjsVpgzL-6pJGSAsHrYEsUAuLBF43iOMcHawtrB9g2UkJ37QOPmZeEf10lgnOaRMtsTbnn59u_kHqQScqjcXE5B8rGlIG09EahObay4xQWwAAAAH9EnfDAA"

    TELEGRAM_API_ID, TELEGRAM_API_HASH, TELEGRAM_SESSION = load_tg_creds()

    try:
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
                        print(f"\rDownloading Telegram media... [{mb_done:.2f} MB / {mb_total:.2f} MB] {percent:.1f}%", end='', flush=True)
                    else:
                        mb_done = current / (1024 * 1024)
                        print(f"\rDownloading Telegram media... [{mb_done:.2f} MB]", end='', flush=True)

                msg = await app.get_messages(chat_id, msg_id) if msg_id else None
                if msg and msg.media:
                    out_dir = target_dir or DEFAULT_DIR
                    os.makedirs(out_dir, exist_ok=True)
                    target_file = os.path.join(out_dir, out_name) if out_name else (out_dir + "/")

                    print(f"➜ Fetching Telegram media from {chat_id}/{msg_id} via Pyrogram...")
                    file_path = None
                    try:
                        file_path = await app.download_media(msg, file_name=target_file, progress=progress_cb)
                    except Exception as dl_err:
                        print(f"\n⚠ Pyrogram download notice: {dl_err}")
                        # Check if file was downloaded or partially downloaded
                        if isinstance(target_file, str) and not target_file.endswith("/"):
                            if os.path.exists(target_file) and os.path.getsize(target_file) > 0:
                                file_path = target_file
                        else:
                            # Look in out_dir for recently created non-empty files
                            for item in os.listdir(out_dir):
                                item_p = os.path.join(out_dir, item)
                                if os.path.isfile(item_p) and os.path.getsize(item_p) > 0:
                                    file_path = item_p
                                    break
                    if file_path:
                        print(f"\n✔ Telegram media downloaded to: {file_path}")
                        sync_to_sdcard_if_needed(file_path)
                        return file_path
                elif msg:
                    # Message exists but has no direct media attachment (e.g. text/update post)
                    if msg.text or msg.caption:
                        print("\n" + "="*65)
                        print(f" 📄 Telegram Post {chat_id}/{msg_id} (Text Only):")
                        print("="*65)
                        print((msg.text or msg.caption).strip())
                        print("="*65 + "\n")

                    # Scan adjacent preceding messages for referenced media attachments (e.g. APKs, documents, videos)
                    adjacent_media = []
                    for offset in range(1, 5):
                        try:
                            adj_msg = await app.get_messages(chat_id, msg_id - offset)
                            if adj_msg and adj_msg.media:
                                fname = "media_file"
                                fsize = 0
                                if adj_msg.document:
                                    fname = adj_msg.document.file_name or f"document_{msg_id - offset}"
                                    fsize = adj_msg.document.file_size
                                elif adj_msg.video:
                                    fname = adj_msg.video.file_name or f"video_{msg_id - offset}.mp4"
                                    fsize = adj_msg.video.file_size
                                elif adj_msg.audio:
                                    fname = adj_msg.audio.file_name or f"audio_{msg_id - offset}.mp3"
                                    fsize = adj_msg.audio.file_size
                                mb_sz = f" ({fsize / (1024*1024):.1f} MB)" if fsize else ""
                                adjacent_media.append((adj_msg, fname, mb_sz))
                        except Exception:
                            pass

                    if adjacent_media:
                        print(" 📦 Detected media attachments in preceding messages from same post:")
                        for idx, (m, fn, sz) in enumerate(adjacent_media, 1):
                            print(f"   [{idx}] {fn}{sz} (Msg {m.id})")
                        print("="*65)
                        try:
                            pick = input(f"➜ Select media to download [1-{len(adjacent_media)}] (Default: 1): ").strip()
                            if not pick:
                                pick = '1'
                            if pick.isdigit() and 1 <= int(pick) <= len(adjacent_media):
                                chosen_msg, chosen_name, _ = adjacent_media[int(pick) - 1]
                                out_dir = target_dir or DEFAULT_DIR
                                os.makedirs(out_dir, exist_ok=True)
                                target_file = os.path.join(out_dir, out_name or chosen_name)
                                print(f"\n➜ Fetching {chosen_name}...")
                                fpath = await app.download_media(chosen_msg, file_name=target_file, progress=progress_cb)
                                if fpath:
                                    print(f"\n✔ Telegram media downloaded to: {fpath}")
                                    sync_to_sdcard_if_needed(fpath)
                                    return fpath
                        except (KeyboardInterrupt, EOFError):
                            pass
                    return None
                else:
                    print("⚠ No downloadable media found in Telegram message via Pyrogram.")
            finally:
                await app.stop()
            return None

        loop = asyncio.get_event_loop()
        res_path = loop.run_until_complete(pyrogram_dl())
        if res_path:
            return res_path
    except Exception as e:
        print(f"⚠ Pyrogram download error ({e}). Trying Telegram web embed fallback...")

    try:
        clean_channel = chat_id if isinstance(chat_id, str) else None
        if clean_channel and msg_id:
            embed_url = f"https://t.me/s/{clean_channel}/{msg_id}"
            res = requests.get(embed_url, headers=HEADERS, timeout=15)
            soup = BeautifulSoup(res.text, 'html.parser')
            video = soup.find('video')
            if video and video.get('src'):
                return download_file_stream(video['src'], clean_filename(out_name or f"telegram_{clean_channel}_{msg_id}.mp4"), target_dir=target_dir)
            doc = soup.find('a', class_=re.compile(r'tgme_widget_message_document'))
            if doc and doc.get('href'):
                href = doc['href']
                if not href.startswith(('https://t.me/', 'http://t.me/')):
                    return download_file_stream(href, clean_filename(out_name or f"telegram_{clean_channel}_{msg_id}"), target_dir=target_dir)
    except Exception as e:
        print(f"✖ Web embed fallback failed: {e}")

    raise Exception("Could not download Telegram link media.")

def handle_netmirror(url, out_name=None, target_dir=None, direct_link_only=False):
    print("➜ Detected NetMirror link...")
    parsed = urllib.parse.urlparse(url)
    parts = [p for p in parsed.path.split('/') if p]
    
    media_type = 'tv'
    media_id = None
    if len(parts) >= 2:
        media_type = parts[0]
        media_id = parts[1]
    elif len(parts) == 1 and parts[0].isdigit():
        media_id = parts[0]
        
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
        'Referer': 'https://mzfi.me/',
        'Origin': 'https://netmirror.center'
    }

    # Query NetMirror API
    api_url = f'https://api2.imdb3.shop/api/{media_type}/{media_id}'
    res = requests.get(api_url, headers=headers, timeout=15)
    res.raise_for_status()
    data = res.json()
    
    results = data.get('results', [])
    if not results:
        raise Exception(f"No metadata found on NetMirror for ID {media_id}")
        
    meta = results[0]
    title = meta.get('title') or meta.get('name') or f"netmirror_{media_id}"
    year = meta.get('release_date', '').split(',')[-1].strip() if meta.get('release_date') else '2024'
    embed_json = meta.get('embed_json') or []
    direct_stream_url = None
    stream_headers = {
        'User-Agent': HEADERS['User-Agent'],
        'Referer': 'https://netmirror.plus/',
        'Origin': 'https://netmirror.plus'
    }

    if not embed_json:
        # Movie / Single title: Resolve stream via vidsync / NetMirror external server APIs
        print(f"➜ Resolving direct stream for movie: {title}...")
        
        target_lang = None
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
        if tm_id:
            try:
                vidsync_api = f"https://vidsync.pro/api/stream?type={media_type}&id={tm_id}"
                v_headers = {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
                    'Referer': f'https://vidsync.pro/embed/{media_type}/{tm_id}',
                    'Origin': 'https://vidsync.pro'
                }
                v_res = requests.get(vidsync_api, headers=v_headers, timeout=8)
                if v_res.status_code == 200:
                    v_data = v_res.json()
                    sources = v_data.get('sources', [])
                    if sources:
                        matching = [s for s in sources if s.get('audioLanguage') == target_lang] if target_lang else []
                        if not matching:
                            matching = sources
                        
                        quality_order = {'1080p': 4, '720p': 3, '480p': 2, '360p': 1, 'Auto': 0}
                        best = sorted(matching, key=lambda s: quality_order.get(s.get('quality', 'Auto'), 0), reverse=True)[0]
                        direct_stream_url = best.get('url') or best.get('directUrl') or best.get('permanent_m3u8_url') or best.get('rawUrl')
                        stream_headers = best.get('headers') or {
                            'Referer': 'https://api.hlowb.com/',
                            'User-Agent': 'okhttp/4.9.3'
                        }
            except Exception:
                pass

        if not direct_stream_url and tm_id:
            try:
                sig_tm = hmac.new(secret_key, f'{tm_id}:{ts}'.encode('utf-8'), hashlib.sha256).hexdigest()
                ext_url = f'https://webvp.watch21.shop/play/external.php?tm_id={tm_id}&tm_type={media_type}&ts={ts}&sig={sig_tm}&exten=1'
                r_ext = requests.get(ext_url, headers=headers, timeout=6)
                if r_ext.status_code == 200:
                    m = re.search(r'const servers\s*=\s*(\[\s*\{.*?\}\s*\]);', r_ext.text, re.DOTALL)
                    if m:
                        import json
                        servers = json.loads(m.group(1))
                        for s in servers:
                            s_url = s.get('url', '')
                            if 'watchcinemo.php' in s_url:
                                r_c = requests.get(s_url, headers={'User-Agent': headers['User-Agent'], 'Referer': ext_url}, timeout=6)
                                match = re.search(r'art\.tvVideoUrl\s*=\s*[\"\']([^\"\']+)[\"\']', r_c.text) or re.search(r'play_url\([\"\'\`](https?://[^\"\'\`\s]+)[\"\'\`]\)', r_c.text) or re.search(r'https?://[^\s\"\'\`<>]+\.(?:r2\.dev|mp4|m3u8)[^\s\"\'\`<>]*', r_c.text)
                                if match:
                                    direct_stream_url = match.group(1) if match.lastindex else match.group(0)
                                    break
            except Exception:
                pass

        if not direct_stream_url:
            raise Exception(f"No stream items found in NetMirror response for {title}")

        clean_name = out_name or clean_filename(title, default_ext=".mp4")

    else:
        # TV show with multiple episodes
        target_item = embed_json[0]
        if len(embed_json) > 1 and sys.stdin.isatty():
            print("\n" + "="*55)
            print(f" 🎬 NetMirror: {title}")
            print("="*55)
            print(" Available Episodes:")
            for idx, item in enumerate(embed_json, 1):
                se = item.get('se', '1')
                ep = item.get('ep', '1')
                size = item.get('size', 'N/A')
                print(f"   [{idx}] Season {se} Episode {ep} ({size})")
            print("="*55)
            try:
                ep_choice = input(f"➜ Select episode [1-{len(embed_json)}] (Default: 1): ").strip()
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
        if not match:
            match = re.search(r'https?://[^\s\"\'\`]+\.(?:r2\.dev|mp4|m3u8)[^\s\"\'\`]*', player_html)

        if not match:
            raise Exception("Could not parse direct stream URL from NetMirror player HTML.")

        direct_stream_url = match.group(1) if match.lastindex else match.group(0)
        clean_name = out_name or clean_filename(f"{title}_S{target_item.get('se',1)}E{target_item.get('ep',1)}", default_ext=".mp4")

    print(f"➜ Bypassed NetMirror Stream: {title}")
    
    print("\n" + "="*65)
    print(" 🚀 DIRECT STREAM LINK FOR ADM (NO PRE-DOWNLOAD NEEDED):")
    print("="*65)
    print(f" 🔗 Copy & Paste into ADM App: {direct_stream_url}")
    print("="*65 + "\n")
    try:
        subprocess.run(["termux-clipboard-set"], input=direct_stream_url.encode('utf-8'), capture_output=True, timeout=2)
        print("📋 Direct stream link copied to Android Clipboard!")
    except Exception:
        pass

    if direct_link_only:
        return direct_stream_url

    # Direct download execution: support HLS m3u8 multi-threaded download via aria2c/yt-dlp
    if '.m3u8' in direct_stream_url:
        print("➜ Downloading HLS Stream via multi-threaded aria2c / yt-dlp...")
        if not clean_name.lower().endswith('.mp4'):
            clean_name = os.path.splitext(clean_name)[0] + '.mp4'
        dest_path = get_dest_path(clean_name, target_dir)
        cmd = [
            'yt-dlp',
            '--downloader', 'aria2c',
            '--downloader-args', 'aria2c:-x 16 -s 16 -k 1M -j 16',
            '--no-part',
            '--concurrent-fragments', '16',
            '-o', dest_path,
            direct_stream_url
        ]
        for hk, hv in stream_headers.items():
            cmd.extend(['--add-header', f'{hk}:{hv}'])
        subprocess.run(cmd)
        sync_to_sdcard_if_needed(dest_path)
        dest_file = dest_path
    else:
        dest_file = download_file_stream(direct_stream_url, clean_name, custom_headers=stream_headers, target_dir=target_dir)
    
    # Launch video player so user can watch with sound while downloading / when ready
    try:
        if dest_file and os.path.exists(dest_file):
            print("📱 Launching video in native media player...")
            subprocess.Popen(["termux-open", dest_file])
    except Exception:
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
            print(f" 📊 LIVE PROCESS LOGS | Status: {self.status}")
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
    print(" ⚠ Unsupported or Complex Link Detected!")
    print(f" ➜ URL logged for Antigravity AI auto-fix: {url}")
    print(" ➜ CLI status: Running background analysis...")
    print(" ➜ Press Ctrl+O (or type 'o') anytime to view live process logs!")
    print("!"*65 + "\n")

def download_youtube_audio(url, codec='mp3', quality='320', target_dir=None):
    out_dir = target_dir or DEFAULT_DIR
    os.makedirs(out_dir, exist_ok=True)

    print(f"➜ Downloading audio [{codec.upper()}]...")
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
                print(f"✔ Audio downloaded to: {filename}")
                sync_to_sdcard_if_needed(filename)
                return filename
    except Exception as e:
        print(f"✖ Error downloading audio: {e}")
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
        queries_to_try = [norm_q]
        if '&' in norm_q:
            queries_to_try.append(norm_q.replace('&', 'and'))
        if ' and ' in norm_q.lower():
            queries_to_try.append(re.sub(r'\band\b', '&', norm_q, flags=re.I))
            
        cleaned = ''.join(c if c.isalnum() or c.isspace() else ' ' for c in norm_q)
        cleaned_words = ' '.join(cleaned.split())
        if cleaned_words and cleaned_words not in queries_to_try:
            queries_to_try.append(cleaned_words)

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

        def query_api(q):
            clean_q = urllib.parse.quote(q)
            for host in ['api2.imdb3.shop', 'api2.imdb4.shop']:
                try:
                    url = f'https://{host}/api/search2/{clean_q}?page=0'
                    r = s.get(url, timeout=3.5)
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
                                parsed.append({
                                    'title': title,
                                    'type': m_type.upper() if m_type else 'MOVIE/SERIES',
                                    'id': media_id,
                                    'year': year,
                                    'num_year': num_year,
                                    'url': nm_url,
                                    'source': 'NetMirror'
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

        # If still no results, use Google Suggestion for typo / spelling correction
        try:
            suggest_url = f"https://suggestqueries.google.com/complete/search?client=firefox&q={urllib.parse.quote(norm_q + ' series')}"
            s_res = requests.get(suggest_url, timeout=3).json()
            if len(s_res) > 1 and s_res[1]:
                for sug in s_res[1][:3]:
                    cleaned_sug = sug
                    for noise in ['netflix series', 'web series', 'series cast', 'series review', 'series rating', 'series', 'imdb', 'cast']:
                        cleaned_sug = re.sub(rf'\b{noise}\b', '', cleaned_sug, flags=re.I).strip()
                    cand = ' '.join(cleaned_sug.split())
                    if cand and len(cand) > 2 and cand.lower() != norm_q.lower():
                        res = query_api(cand)
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

def prompt_action_mode_and_run(url):
    is_netmirror = any(nm in url.lower() for nm in ['netmirror.', 'net52.', 'net77.', 'watch21.', 'watch22.'])
    while True:
        print("\n" + "="*65)
        print(" Select Download Action / Mode:")
        print("   [1] ⬇ Start Direct Download in Terminal (Multi-threaded aria2c 16x)")
        print("   [2] 🔗 Generate Direct Links (No Pre-Download)")
        print("="*65)
        try:
            mode_choice = input("➜ Select choice [1-2] (Default: 1): ").strip()
            if not mode_choice:
                mode_choice = '1'
        except (KeyboardInterrupt, EOFError):
            return

        if mode_choice == '1':
            if is_netmirror:
                handle_netmirror(url, direct_link_only=False)
            else:
                download(url)
            break
        elif mode_choice == '2':
            if is_netmirror:
                handle_netmirror(url, direct_link_only=True)
            else:
                generate_adm_links_for_url(url)
            break
        else:
            print("✖ Invalid selection. Please select choice 1 or 2.")

def search_netmirror_and_prompt(query, target_dir=None):
    print(f"\n➜ Searching NetMirror for: \"{query}\"...")
    results = search_netmirror(query)
    if not results:
        print("⚠ No search results found on NetMirror. Searching YouTube instead...")
        return search_youtube_and_prompt(query, target_dir=target_dir)

    print("\n" + "="*65)
    print(f" 🎬 NetMirror Search Results for: \"{query}\"")
    print("="*65)
    for idx, item in enumerate(results[:10], 1):
        yr_str = f" ({item['year']})" if item.get('year') else ""
        print(f"   [{idx}] {item['title']} [{item['type']}{yr_str}]")
    print("   [Y] 📺 Search on YouTube instead")
    print("="*65)

    while True:
        try:
            sel = input(f"➜ Select result [1-{len(results[:10])}] (Default: 1, Y: YouTube): ").strip()
            if not sel:
                sel = '1'
        except (KeyboardInterrupt, EOFError):
            return
        if sel.lower() in ('y', 'yt', 'youtube'):
            return search_youtube_and_prompt(query, target_dir=target_dir)
        elif sel.isdigit() and 1 <= int(sel) <= len(results[:10]):
            selected = results[int(sel) - 1]
            print(f"\n➜ Selected: {selected['title']}")
            prompt_action_mode_and_run(selected['url'])
            return
        else:
            print(f"✖ Invalid selection. Please select choice 1 to {len(results[:10])} or 'Y'.")

def search_youtube_and_prompt(query, target_dir=None):
    print(f"\n➜ Searching YouTube for: \"{query}\"...")
    yt_entries = []
    if HAS_YTDLP:
        try:
            ydl_opts = get_ytdlp_opts({'quiet': True, 'no_warnings': True, 'extract_flat': 'in_playlist'})
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                res = ydl.extract_info(f"ytsearch10:{query}", download=False)
                yt_entries = [e for e in res.get('entries', []) if e] if res else []
        except Exception:
            yt_entries = []

    if not yt_entries:
        print("⚠ No search results found on YouTube.")
        return

    print("\n" + "="*65)
    print(f" 🎵 YouTube Search Results for: \"{query}\"")
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
            sel = input(f"➜ Select result [1-{len(yt_entries[:10])}] (Default: 1): ").strip()
            if not sel:
                sel = '1'
        except (KeyboardInterrupt, EOFError):
            return
        if sel.isdigit() and 1 <= int(sel) <= len(yt_entries[:10]):
            selected_entry = yt_entries[int(sel) - 1]
            selected_url = selected_entry.get('url') or selected_entry.get('webpage_url') or f"https://www.youtube.com/watch?v={selected_entry.get('id')}"
            print(f"\n➜ Selected: {selected_entry.get('title')}")
            prompt_action_mode_and_run(selected_url)
            return
        else:
            print(f"✖ Invalid selection. Please select choice 1 to {len(yt_entries[:10])}.")

def search_by_name(target_dir=None):
    print("\n" + "="*65)
    print(" Select Search Platform:")
    print("   [1] 🎬 NetMirror (Movies / Series / Web Shows)")
    print("   [2] 📺 YouTube (Video / Music / Audio)")
    print("="*65)
    try:
        p_choice = input("➜ Select choice [1-2] (Default: 1): ").strip()
        if not p_choice:
            p_choice = '1'
    except (KeyboardInterrupt, EOFError):
        return

    if p_choice == '1':
        try:
            query = input("\n➜ Enter Movie / Series Name to Search NetMirror: ").strip()
        except (KeyboardInterrupt, EOFError):
            return
        if query:
            search_netmirror_and_prompt(query, target_dir=target_dir)
    elif p_choice == '2':
        try:
            query = input("\n➜ Enter Video / Song Name to Search YouTube: ").strip()
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
        print(" • 👉 To download 1080p/4K WITH sound merged into an MP4 file,")
        print("   select Option [1] in 'dl' (downloads at 16x speed and merges in 1s).")
        print("="*65)

    format_spec, is_audio = prompt_video_quality(url)
    
    if is_audio:
        stream_format = 'bestaudio/best'
    else:
        stream_format = f"{format_spec}/bestvideo+bestaudio/best"

    print(f"\n➜ Extracting direct stream links for: {url}...")
    if not HAS_YTDLP:
        print("✖ yt-dlp is required for extracting stream links.")
        return
        
    try:
        ydl_opts = get_ytdlp_opts({'quiet': True, 'no_warnings': True, 'extract_flat': 'in_playlist'})
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            
        if not info:
            print("✖ Could not extract link metadata.")
            return

        entries = info.get('entries', []) if isinstance(info, dict) else []
        adm_links = []
        
        if entries:
            pl_title = info.get('title', 'Playlist')
            print("\n" + "="*65)
            print(f" 🎬 Playlist / Batch Detected: {pl_title} ({len(entries)} items)")
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
                            print(f"       🔗 Direct Stream Link: {stream_url[:90]}...\n")
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
                        print(f" 🎬 Video: {v_title}")
                        print("="*65)
                        print(f" 🔗 Direct Video Stream Link: {stream_url}\n")
                        if audio_stream_url:
                            print(f" 🎵 Direct Audio Stream Link: {audio_stream_url}\n")
                            print(" ⚠ Note: Single video stream link above has no sound because YouTube")
                            print("   serves video and audio separately. Use Mode [1] for auto-merged MP4.\n")
            except Exception as e:
                print(f"✖ Error extracting stream link: {e}")

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
                print(f" 📄 Saved {len(adm_links)} direct link(s) to: {txt_path}")
                print(f" 🚀 Single All-in-One Link for ADM: {single_hosted_url}")
                print(f" 🌐 Wi-Fi LAN Download Link:       {lan_url}")
                clipboard_target = single_hosted_url
            else:
                clipboard_target = adm_links[0][2]
                print("="*65)
                print(f" 🚀 Direct Stream Link for ADM / VLC: {clipboard_target}")
            
            try:
                subprocess.run(["termux-clipboard-set"], input=clipboard_target.encode('utf-8'), capture_output=True, timeout=2)
                print(" 📋 Stream link copied to Android Clipboard!")
            except Exception:
                pass
            print("="*65 + "\n")
            
    except Exception as e:
        print(f"✖ Error extracting direct links: {e}")

def interactive_menu():
    while True:
        print("\n" + "="*65)
        print("           🚀 UNIVERSAL DOWNLOADER CLI 🚀           ")
        print("="*65)
        try:
            user_input = input("➜ Enter URL or Movie/Video Name (or 'q' to quit): ").strip()
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
                user_input = input("➜ Enter URL / Playlist Link: ").strip()
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

def download(url, out_name=None, cookie=None, target_dir=None, quality_arg=None):
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

        if any(nm in url_lower for nm in ['netmirror.', 'net52.cc', 'net77.cc', 'watch21.', 'watch22.']):
            PROCESS_MONITOR.set_status("Bypassing NetMirror Stream...")
            return handle_netmirror(url, out_name=out_name, target_dir=target_dir)
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
            if handle_youtube_or_video(url, target_dir=target_dir, quality_arg=quality_arg, cookie=cookie):
                return
        
        PROCESS_MONITOR.set_status("Generic Stream Download...")
        res = handle_generic(url, out_name=out_name, custom_headers=custom_headers, target_dir=target_dir)
        if res:
            return res

        raise Exception("Extractor returned no stream data.")
    except Exception as e:
        PROCESS_MONITOR.log(f"Primary download failed: {e}")
        print(f"✖ Error downloading from primary handler: {e}")
        print("➜ Attempting generic fallback...")
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

def main():
    if len(sys.argv) < 2:
        interactive_menu()
        return

    args = sys.argv[1:]
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
            print(f"⚠ Skipping invalid argument: {url}")

if __name__ == '__main__':
    main()
