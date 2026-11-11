#!/usr/bin/env python3
"""
India's Got Latent — Ultra-Fast Universal CLI Downloader & Streamer (igl)
Features:
- Instant startup (cached metadata with background refresh)
- Concurrent domain resolution
- Accurate badging ([MEMBERS ONLY], [FULL EPISODE], [YOUTUBE])
- Direct Streaming to VLC, Chrome, Video Players, or Web Player
- Side-by-side simultaneous downloading via 16-connection aria2c
- Shareable Direct Stream URLs for PC / TV / LAN streaming anywhere
"""

import sys
import os
import re
import json
import time
import subprocess
import urllib.parse
import shutil
import threading
from concurrent.futures import ThreadPoolExecutor
import requests

# Fast cache location
CACHE_FILE = os.path.expanduser("~/.cache/igl_episodes_cache.json")
CACHE_TTL = 1800  # 30 minutes

# Default fallback domains
PRIMARY_SHORT_LINK = "https://gdls.me/instaiglreel"
FALLBACK_DOMAINS = [
    "https://indiassgetlentp.freeforall.dev",
    "https://igl-8wky.freeforall.dev",
    "https://india-got-latent-3ms.pages.dev"
]
WORKER_URL = "https://okcdn.moivies.workers.dev"

def get_dest_dir():
    target = os.environ.get("ARC_DOWNLOAD_DIR") or "/sdcard/Download"
    if os.path.exists(target):
        return target
    fallback = os.path.expanduser("~/downloads")
    return fallback

OUTPUT_DIR = "/sdcard/Download"

# ANSI Colors
CLR_RESET = "\033[0m"
CLR_BOLD = "\033[1m"
CLR_CYAN = "\033[36m"
CLR_GREEN = "\033[32m"
CLR_YELLOW = "\033[33m"
CLR_MAGENTA = "\033[35m"
CLR_BLUE = "\033[34m"
CLR_RED = "\033[31m"
CLR_DIM = "\033[2m"

def print_banner(base_url, is_cached=False):
    cache_tag = f" {CLR_GREEN}(cached){CLR_RESET}" if is_cached else ""
    print(f"\n{CLR_BOLD}{CLR_MAGENTA}========================================================================={CLR_RESET}")
    print(f"{CLR_BOLD}{CLR_CYAN}  🎭 INDIA'S GOT LATENT — CLI DOWNLOADER & STREAMER {CLR_RESET}")
    print(f"{CLR_DIM}  Active Source Domain: {CLR_YELLOW}{base_url}{CLR_RESET}{cache_tag}")
    print(f"{CLR_DIM}  Saving Downloads To: {CLR_GREEN}{OUTPUT_DIR}{CLR_RESET}")
    print(f"{CLR_BOLD}{CLR_MAGENTA}========================================================================={CLR_RESET}\n")

def check_domain(domain):
    try:
        url = f"{domain}/okcdn.json"
        res = requests.get(url, timeout=2.5)
        if res.status_code == 200 and isinstance(res.json(), list) and len(res.json()) > 0:
            return domain, res.json()
    except Exception:
        pass
    return None, None

def resolve_domain_and_episodes_concurrent():
    """Concurrently check domains to find active endpoint in sub-second time."""
    try:
        res = requests.get(PRIMARY_SHORT_LINK, allow_redirects=True, timeout=2.0)
        if res.status_code == 200:
            parsed = urllib.parse.urlparse(res.url)
            base = f"{parsed.scheme}://{parsed.netloc}"
            domain, data = check_domain(base)
            if domain and data:
                return domain, data
    except Exception:
        pass

    with ThreadPoolExecutor(max_workers=5) as executor:
        futures = [executor.submit(check_domain, dom) for dom in FALLBACK_DOMAINS]
        for f in futures:
            dom, data = f.result()
            if dom and data:
                return dom, data

    return FALLBACK_DOMAINS[0], []

def get_episodes_data(force_refresh=False):
    """Retrieve episodes data using local cache for 0.05s startup."""
    os.makedirs(os.path.dirname(CACHE_FILE), exist_ok=True)

    if not force_refresh and os.path.exists(CACHE_FILE):
        try:
            mtime = os.path.getmtime(CACHE_FILE)
            if time.time() - mtime < CACHE_TTL:
                with open(CACHE_FILE, "r") as f:
                    cache_data = json.load(f)
                    base_url = cache_data.get("base_url")
                    episodes = cache_data.get("episodes")
                    if base_url and episodes:
                        threading.Thread(target=refresh_cache_background, daemon=True).start()
                        return base_url, episodes, True
        except Exception:
            pass

    base_url, episodes = resolve_domain_and_episodes_concurrent()
    if episodes:
        try:
            with open(CACHE_FILE, "w") as f:
                json.dump({"base_url": base_url, "episodes": episodes, "timestamp": time.time()}, f)
        except Exception:
            pass
    return base_url, episodes, False

def refresh_cache_background():
    try:
        base_url, episodes = resolve_domain_and_episodes_concurrent()
        if episodes:
            with open(CACHE_FILE, "w") as f:
                json.dump({"base_url": base_url, "episodes": episodes, "timestamp": time.time()}, f)
    except Exception:
        pass

def clean_filename(title):
    title = re.sub(r'[\\/*?:"<>|]', '', title)
    title = re.sub(r'\s+', '_', title).strip('_')
    return title

def sync_to_sdcard_if_needed(file_path):
    if file_path and not file_path.startswith("/sdcard"):
        filename = os.path.basename(file_path)
        dest_sdcard = f"/sdcard/Download/{filename}"
        try:
            res = subprocess.run(["adb", "devices"], capture_output=True, text=True, timeout=5)
            if "device" in res.stdout and len(res.stdout.strip().splitlines()) > 1:
                print(f"{CLR_CYAN}➜ Syncing to /sdcard/Download/{filename} via ADB...{CLR_RESET}")
                subprocess.run(["adb", "push", file_path, dest_sdcard], capture_output=True, text=True, timeout=60)
                print(f"{CLR_GREEN}✔ Synced to SD Card!{CLR_RESET}")
        except Exception:
            pass

def is_members_only_content(ep):
    title = ep.get('title', '').lower()
    data_id = ep.get('dataId', '').lower()
    desc = ep.get('description', '').lower()
    keywords = ['bonus', 'bts', 'extra', 'discarded', 'deleted', 'clip', 'member']
    return any(k in title or k in data_id or k in desc for k in keywords)

def launch_player(target_uri, player_choice="vlc", is_local=False):
    """
    Launch target URI or file in VLC, Chrome, or Android App Chooser.
    Unsets LD_LIBRARY_PATH to avoid Android 14 dynamic linker conflicts.
    """
    clean_env = os.environ.copy()
    clean_env.pop('LD_LIBRARY_PATH', None)

    # 1. Check if ADB is active to open directly via intent
    try:
        adb_proc = subprocess.run(["adb", "devices"], capture_output=True, text=True, timeout=1.5)
        if "device" in adb_proc.stdout and len(adb_proc.stdout.strip().splitlines()) > 1:
            if player_choice == "vlc":
                res = subprocess.run([
                    "adb", "shell", "am", "start", "-a", "android.intent.action.VIEW",
                    "-d", target_uri, "-t", "video/mp4", "-p", "org.videolan.vlc"
                ], capture_output=True, text=True, timeout=3)
                if res.returncode == 0:
                    return True
            elif player_choice == "chrome":
                res = subprocess.run([
                    "adb", "shell", "am", "start", "-a", "android.intent.action.VIEW",
                    "-d", target_uri, "-p", "com.android.chrome"
                ], capture_output=True, text=True, timeout=3)
                if res.returncode == 0:
                    return True
    except Exception:
        pass

    # 2. Use native termux-open with clean environment
    try:
        if is_local:
            cmd = ['termux-open', '--content-type', 'video/mp4', target_uri]
        elif player_choice == "chrome":
            cmd = ['termux-open', target_uri]
        elif player_choice == "chooser":
            cmd = ['termux-open', '--chooser', '--content-type', 'video/mp4', target_uri]
        else: # default VLC or video player
            cmd = ['termux-open', '--content-type', 'video/mp4', target_uri]

        subprocess.run(cmd, env=clean_env, timeout=5)
        return True
    except Exception as e:
        print(f"{CLR_RED}✖ Player launch notice: {e}{CLR_RESET}")
        return False

def resolve_stream_info(ep, base_url, target_quality="best"):
    """Resolve stream URL, quality, web player URL, and target destination path."""
    ep_type = ep.get('type')
    title = ep.get('title')
    data_id = ep.get('dataId', 'ep')
    filename = clean_filename(f"{data_id}_{title}") + ".mp4"
    dest_path = os.path.join(OUTPUT_DIR, filename)
    web_player_url = f"{base_url}/player.html?id={data_id}"

    if ep_type == 'youtube':
        yt_id = ep.get('youtubeId')
        yt_url = f"https://www.youtube.com/watch?v={yt_id}"
        return {
            'type': 'youtube',
            'title': title,
            'data_id': data_id,
            'filename': filename,
            'dest_path': dest_path,
            'stream_url': yt_url,
            'quality': '1080p',
            'web_player_url': web_player_url
        }

    # OKCDN Stream
    item_id = ep.get('id')
    headers = {'Referer': base_url}
    print(f"{CLR_YELLOW}➜ Resolving high-speed video streams from Worker...{CLR_RESET}")
    try:
        resp = requests.get(f"{WORKER_URL}/?id={item_id}", headers=headers, timeout=12).json()
    except Exception as e:
        print(f"{CLR_RED}✖ Worker API request failed: {e}{CLR_RESET}")
        return None

    if resp.get('status') != 'success' or not resp.get('streams'):
        print(f"{CLR_RED}✖ Worker error: {resp.get('message') or resp.get('error') or 'Stream unavailable'}{CLR_RESET}")
        return None

    streams = resp.get('streams')
    quality_order = {'4k': 2160, '2k': 1440, '1080p': 1080, '720p': 720, '480p': 480, '360p': 360, '240p': 240, '144p': 144}
    streams_sorted = sorted(streams, key=lambda s: quality_order.get(s.get('type', ''), 0), reverse=True)

    selected_stream = None
    if target_quality == "best":
        selected_stream = streams_sorted[0]
    else:
        for s in streams_sorted:
            if s.get('type') == target_quality or str(quality_order.get(s.get('type'), '')) == target_quality.replace('p', ''):
                selected_stream = s
                break
        if not selected_stream:
            selected_stream = streams_sorted[0]

    return {
        'type': 'okcdn',
        'title': title,
        'data_id': data_id,
        'filename': filename,
        'dest_path': dest_path,
        'stream_url': selected_stream.get('url'),
        'quality': selected_stream.get('type'),
        'web_player_url': web_player_url
    }

def print_stream_card(info, player_name="VLC"):
    print(f"\n{CLR_BOLD}{CLR_GREEN}========================================================================={CLR_RESET}")
    print(f"{CLR_BOLD}{CLR_CYAN}  🎬 STREAM READY:{CLR_RESET} {CLR_BOLD}{info['title']}{CLR_RESET}")
    print(f"{CLR_BOLD}{CLR_GREEN}========================================================================={CLR_RESET}")
    print(f"  {CLR_BOLD}Resolution:{CLR_RESET} {CLR_YELLOW}{info['quality']}{CLR_RESET} | {CLR_BOLD}Format:{CLR_RESET} MP4")
    print(f"\n  {CLR_BOLD}{CLR_MAGENTA}🔗 Direct Stream URL (VLC / Chrome / PotPlayer / TV / PC):{CLR_RESET}")
    print(f"  {CLR_CYAN}{info['stream_url']}{CLR_RESET}")
    print(f"\n  {CLR_BOLD}{CLR_BLUE}🌐 Web Player Link:{CLR_RESET}")
    print(f"  {CLR_DIM}{info['web_player_url']}{CLR_RESET}")
    if player_name != "none":
        print(f"\n  {CLR_BOLD}{CLR_GREEN}🚀 Opening in {player_name}...{CLR_RESET}")
    print(f"{CLR_BOLD}{CLR_GREEN}========================================================================={CLR_RESET}\n")

def prompt_action_choice(preselected=None):
    if preselected in ('stream_and_download', 'download', 'stream'):
        return preselected

    print(f"\n{CLR_BOLD}Choose Action:{CLR_RESET}")
    print(f"  [{CLR_CYAN}1{CLR_RESET}] {CLR_BOLD}Stream & Download{CLR_RESET} {CLR_GREEN}(Watch in VLC/Chrome + Download simultaneously) [Default]{CLR_RESET}")
    print(f"  [{CLR_CYAN}2{CLR_RESET}] Download Only (Save to disk)")
    print(f"  [{CLR_CYAN}3{CLR_RESET}] Stream Only (Instant playback in VLC/Chrome, no download)")

    try:
        ans = input(f"{CLR_YELLOW}➜ Select action [1-3] (Default: 1): {CLR_RESET}").strip()
    except (EOFError, KeyboardInterrupt):
        ans = "1"

    mapping = {"1": "stream_and_download", "2": "download", "3": "stream"}
    return mapping.get(ans, "stream_and_download")

def prompt_player_choice(preselected=None):
    if preselected in ('vlc', 'chrome', 'chooser', 'none'):
        return preselected

    print(f"\n{CLR_BOLD}Launch Video Stream In:{CLR_RESET}")
    print(f"  [{CLR_CYAN}1{CLR_RESET}] {CLR_BOLD}VLC Player{CLR_RESET} {CLR_GREEN}(Default){CLR_RESET}")
    print(f"  [{CLR_CYAN}2{CLR_RESET}] Google Chrome / Browser")
    print(f"  [{CLR_CYAN}3{CLR_RESET}] Android App Chooser (Pick any player)")
    print(f"  [{CLR_CYAN}4{CLR_RESET}] Do Not Launch (Display Stream Link Only)")

    try:
        ans = input(f"{CLR_YELLOW}➜ Select player [1-4] (Default: 1): {CLR_RESET}").strip()
    except (EOFError, KeyboardInterrupt):
        ans = "1"

    mapping = {"1": "vlc", "2": "chrome", "3": "chooser", "4": "none"}
    return mapping.get(ans, "vlc")

def prompt_quality_choice():
    print(f"\n{CLR_BOLD}Select Video Quality:{CLR_RESET}")
    print(f"  [{CLR_CYAN}1{CLR_RESET}] Best Available (1080p / 2K / 4K) {CLR_GREEN}(Default){CLR_RESET}")
    print(f"  [{CLR_CYAN}2{CLR_RESET}] 1080p (Full HD)")
    print(f"  [{CLR_CYAN}3{CLR_RESET}] 720p (HD)")
    print(f"  [{CLR_CYAN}4{CLR_RESET}] 480p (SD)")

    try:
        ans = input(f"{CLR_YELLOW}➜ Select quality [1-4] (Default: 1): {CLR_RESET}").strip()
    except (EOFError, KeyboardInterrupt):
        ans = "1"

    mapping = {"1": "best", "2": "1080p", "3": "720p", "4": "480p"}
    return mapping.get(ans, "best")

def execute_download(info):
    """Run aria2c or yt-dlp download."""
    dest_path = info['dest_path']
    filename = info['filename']
    stream_url = info['stream_url']

    if info['type'] == 'youtube':
        print(f"\n{CLR_CYAN}➜ Starting YouTube download via yt-dlp / d.py...{CLR_RESET}")
        if shutil.which("yt-dlp"):
            cmd = [
                "yt-dlp",
                "-f", "bestvideo[height<=1080]+bestaudio/best",
                "-o", dest_path,
                "--no-warnings",
                stream_url
            ]
            if shutil.which("aria2c"):
                cmd.extend(["--downloader", "aria2c", "--downloader-args", "aria2c:-j 16 -s 16 -x 16 -k 1M"])
            subprocess.run(cmd)
            if os.path.exists(dest_path):
                print(f"\n{CLR_BOLD}{CLR_GREEN}✔ Download complete! Saved to: {dest_path}{CLR_RESET}")
                sync_to_sdcard_if_needed(dest_path)
                return dest_path

        dl_script = os.path.expanduser("~/scripts/d.py")
        if os.path.exists(dl_script):
            subprocess.run(["python3", dl_script, stream_url, "-d", OUTPUT_DIR, "-q", "1080p"])
            return dest_path
        print(f"{CLR_RED}✖ Download tool unavailable for YouTube.{CLR_RESET}")
        return None

    # OKCDN multi-threaded aria2c download
    if shutil.which("aria2c"):
        cmd = [
            "aria2c",
            "--allow-overwrite=true",
            "-j", "16",
            "-s", "16",
            "-x", "16",
            "-k", "1M",
            "--min-split-size=1M",
            "--file-allocation=falloc",
            "-d", OUTPUT_DIR,
            "-o", filename,
            stream_url
        ]
        print(f"\n{CLR_CYAN}➜ Starting multi-threaded download via aria2c (16 connections)...{CLR_RESET}\n")
        res = subprocess.run(cmd)
        if res.returncode == 0 and os.path.exists(dest_path):
            file_mb = os.path.getsize(dest_path) / (1024*1024)
            print(f"\n{CLR_BOLD}{CLR_GREEN}✔ Download complete! Saved to: {dest_path} ({file_mb:.1f} MB){CLR_RESET}")
            sync_to_sdcard_if_needed(dest_path)
            return dest_path
        else:
            print(f"{CLR_RED}✖ aria2c returned code {res.returncode}. Falling back to python downloader...{CLR_RESET}")

    # Fallback direct streaming download
    print(f"{CLR_CYAN}➜ Downloading stream directly via Python...{CLR_RESET}")
    with requests.get(stream_url, stream=True, timeout=30) as r:
        r.raise_for_status()
        total_len = int(r.headers.get('content-length', 0))
        downloaded = 0
        with open(dest_path, 'wb') as f:
            for chunk in r.iter_content(chunk_size=65536):
                if chunk:
                    f.write(chunk)
                    downloaded += len(chunk)
                    if total_len > 0:
                        pct = (downloaded / total_len) * 100
                        mb_done = downloaded / (1024*1024)
                        mb_tot = total_len / (1024*1024)
                        print(f"\rDownloading... [{mb_done:.1f} MB / {mb_tot:.1f} MB] {pct:.1f}%", end='', flush=True)
    print(f"\n{CLR_BOLD}{CLR_GREEN}✔ Download complete! Saved to: {dest_path}{CLR_RESET}")
    sync_to_sdcard_if_needed(dest_path)
    return dest_path

def handle_episode(ep, base_url, action=None, player_choice=None, target_quality=None):
    """
    Unified episode handler for Stream & Download, Download Only, and Stream Only.
    """
    # 1. Determine quality
    if not target_quality:
        target_quality = prompt_quality_choice()

    # 2. Check if file already exists locally
    data_id = ep.get('dataId', 'ep')
    title = ep.get('title')
    filename = clean_filename(f"{data_id}_{title}") + ".mp4"
    dest_path = os.path.join(OUTPUT_DIR, filename)

    # 3. Determine action
    if not action:
        action = prompt_action_choice()

    # 4. Handle pre-existing file in stream mode
    if os.path.exists(dest_path) and os.path.getsize(dest_path) > 10 * 1024 * 1024:
        file_mb = os.path.getsize(dest_path) / (1024*1024)
        print(f"\n{CLR_GREEN}✔ Episode is already fully downloaded on device ({file_mb:.1f} MB)!{CLR_RESET}")
        print(f"  {CLR_DIM}Path: {dest_path}{CLR_RESET}")
        if action in ("stream", "stream_and_download"):
            if not player_choice:
                player_choice = prompt_player_choice()
            if player_choice != "none":
                print(f"{CLR_CYAN}➜ Playing local downloaded file in {player_choice.upper()}...{CLR_RESET}")
                launch_player(dest_path, player_choice=player_choice, is_local=True)
        return dest_path

    # 5. Resolve stream details
    stream_info = resolve_stream_info(ep, base_url, target_quality=target_quality)
    if not stream_info:
        print(f"{CLR_RED}✖ Could not obtain video stream URL.{CLR_RESET}")
        return None

    # 6. Stream logic
    if action in ("stream", "stream_and_download"):
        if not player_choice:
            player_choice = prompt_player_choice()

        player_label = {
            "vlc": "VLC Player",
            "chrome": "Google Chrome",
            "chooser": "Android App Chooser",
            "none": "none"
        }.get(player_choice, "VLC Player")

        print_stream_card(stream_info, player_name=player_label)

        if player_choice != "none":
            launch_player(stream_info['stream_url'], player_choice=player_choice)

    # 7. Download logic
    if action in ("download", "stream_and_download"):
        if action == "stream_and_download":
            print(f"\n{CLR_BOLD}{CLR_CYAN}➜ Video stream opened! Starting side-by-side background download...{CLR_RESET}")
        execute_download(stream_info)

    return dest_path

def run_cli_interactive(episodes, base_url, is_cached=False, default_action=None):
    print_banner(base_url, is_cached=is_cached)

    # Categorize episodes
    s2_bonus = []
    s2_normal = []
    s1_episodes = []
    s1_bonus = []
    specials = []

    for ep in episodes:
        season = ep.get('season')
        if season == 's2':
            if is_members_only_content(ep):
                s2_bonus.append(ep)
            else:
                s2_normal.append(ep)
        elif season == 's1':
            if is_members_only_content(ep):
                s1_bonus.append(ep)
            else:
                s1_episodes.append(ep)
        else:
            specials.append(ep)

    flat_list = []
    current_idx = 1

    def render_section(heading, items):
        nonlocal current_idx
        if not items:
            return
        print(f"{CLR_BOLD}{CLR_YELLOW}═══ {heading} ({len(items)}) ═══{CLR_RESET}")
        for ep in items:
            dur = ep.get('duration', '')
            dur_str = f" ({dur})" if dur else ""
            stype = ep.get('type', 'okcdn').lower()

            if is_members_only_content(ep):
                badge = f"{CLR_YELLOW}MEMBERS ONLY{CLR_RESET}"
            elif stype == 'okcdn':
                badge = f"{CLR_GREEN}FULL EPISODE{CLR_RESET}"
            else:
                badge = f"{CLR_RED}YOUTUBE{CLR_RESET}"

            print(f"  {CLR_CYAN}[{current_idx:02d}]{CLR_RESET} {CLR_BOLD}{ep.get('title')}{CLR_RESET}{CLR_DIM}{dur_str}{CLR_RESET} [{badge}]")
            flat_list.append(ep)
            current_idx += 1
        print()

    render_section("🎁 SEASON 2 BONUS & BTS EPISODES", s2_bonus)
    render_section("🎬 SEASON 2 MAIN EPISODES", s2_normal)
    render_section("🌟 SEASON 1 BONUS & DISCARDED SEGMENTS", s1_bonus)
    render_section("🍿 SEASON 1 MAIN EPISODES", s1_episodes)
    render_section("🎭 SPECIALS & TALK SHOWS", specials)

    print(f"{CLR_DIM}-------------------------------------------------------------------------{CLR_RESET}")
    print(f" {CLR_BOLD}Commands & Shortcuts:{CLR_RESET}")
    print(f"  • {CLR_CYAN}<num>{CLR_RESET}          : Select episode (e.g. {CLR_CYAN}1{CLR_RESET})")
    print(f"  • {CLR_CYAN}s <num>{CLR_RESET}        : Stream & Download directly (e.g. {CLR_CYAN}s 1{CLR_RESET})")
    print(f"  • {CLR_CYAN}vlc <num>{CLR_RESET}      : Stream in VLC & Download (e.g. {CLR_CYAN}vlc 1{CLR_RESET})")
    print(f"  • {CLR_CYAN}chrome <num>{CLR_RESET}   : Stream in Chrome & Download (e.g. {CLR_CYAN}chrome 1{CLR_RESET})")
    print(f"  • {CLR_CYAN}so <num>{CLR_RESET}       : Stream Only, no download (e.g. {CLR_CYAN}so 1{CLR_RESET})")
    print(f"  • {CLR_CYAN}<search>{CLR_RESET}      : Search titles (e.g. {CLR_CYAN}Raghav{CLR_RESET}, {CLR_CYAN}members{CLR_RESET})")
    print(f"  • {CLR_RED}q{CLR_RESET}              : Exit\n")

    while True:
        try:
            user_input = input(f"{CLR_BOLD}{CLR_GREEN}➜ Enter command or selection: {CLR_RESET}").strip()
        except (EOFError, KeyboardInterrupt):
            print("\nExiting.")
            sys.exit(0)

        if not user_input:
            continue
        if user_input.lower() in ('q', 'quit', 'exit'):
            print("Goodbye!")
            sys.exit(0)

        # Parse prefix shortcuts: s 1, vlc 1, chrome 1, so 1, dl 1
        action = default_action
        player_choice = None
        target_num_str = None

        tokens = user_input.split()
        if len(tokens) >= 2 and tokens[-1].isdigit():
            prefix = tokens[0].lower()
            target_num_str = tokens[-1]
            if prefix in ('s', 'stream', 'watch'):
                action = 'stream_and_download'
            elif prefix == 'vlc':
                action = 'stream_and_download'
                player_choice = 'vlc'
            elif prefix in ('chrome', 'browser'):
                action = 'stream_and_download'
                player_choice = 'chrome'
            elif prefix in ('so', 'streamonly'):
                action = 'stream'
            elif prefix in ('d', 'dl', 'download'):
                action = 'download'

        if not target_num_str and user_input.isdigit():
            target_num_str = user_input

        # Check if number selected
        if target_num_str and target_num_str.isdigit():
            num = int(target_num_str)
            if 1 <= num <= len(flat_list):
                selected_ep = flat_list[num - 1]
                handle_episode(
                    selected_ep,
                    base_url,
                    action=action,
                    player_choice=player_choice
                )
                print(f"\n{CLR_GREEN}✔ Done!{CLR_RESET}\n")
                break
            else:
                print(f"{CLR_RED}✖ Invalid number. Please enter 1 to {len(flat_list)}.{CLR_RESET}")
                continue

        # Search matching items
        search_results = []
        q = user_input.lower()
        for idx, ep in enumerate(flat_list, 1):
            title = ep.get('title', '').lower()
            data_id = ep.get('dataId', '').lower()
            if q in title or q in data_id:
                search_results.append((idx, ep))
            elif q in ('members', 'member', 'bonus') and is_members_only_content(ep):
                search_results.append((idx, ep))

        if not search_results:
            print(f"{CLR_RED}✖ No episodes matching query '{user_input}'. Try again.{CLR_RESET}")
            continue

        if len(search_results) == 1:
            idx, selected_ep = search_results[0]
            print(f"\n{CLR_GREEN}✔ Found exact match:{CLR_RESET} [{idx}] {selected_ep.get('title')}")
            handle_episode(
                selected_ep,
                base_url,
                action=action,
                player_choice=player_choice
            )
            print(f"\n{CLR_GREEN}✔ Done!{CLR_RESET}\n")
            break

        print(f"\n{CLR_BOLD}{CLR_YELLOW}Found {len(search_results)} matching episodes:{CLR_RESET}")
        for idx, ep in search_results:
            print(f"  [{CLR_CYAN}{idx}{CLR_RESET}] {ep.get('title')}")
        print()

def main():
    force_refresh = "--refresh" in sys.argv or "-r" in sys.argv
    raw_args = [a for a in sys.argv[1:] if a not in ("--refresh", "-r")]

    base_url, episodes, is_cached = get_episodes_data(force_refresh=force_refresh)

    if not episodes:
        print(f"{CLR_RED}✖ Error: Could not fetch episode list.{CLR_RESET}")
        sys.exit(1)

    # CLI direct argument handling
    if len(raw_args) > 0:
        action = None
        player_choice = None
        clean_args = []

        # Parse flags like -s, --stream, -d, --download, vlc, chrome
        for a in raw_args:
            al = a.lower()
            if al in ('-s', '--stream', 'stream', 'watch'):
                action = 'stream_and_download'
            elif al in ('-so', '--stream-only', 'streamonly', 'so'):
                action = 'stream'
            elif al in ('-d', '--download', 'download'):
                action = 'download'
            elif al == 'vlc':
                action = 'stream_and_download'
                player_choice = 'vlc'
            elif al in ('chrome', 'browser'):
                action = 'stream_and_download'
                player_choice = 'chrome'
            else:
                clean_args.append(a)

        cmd_arg = " ".join(clean_args).strip()

        # If only stream flag was given (e.g. `igl stream` or `igl -s`), open interactive with stream default
        if not cmd_arg and action:
            run_cli_interactive(episodes, base_url, is_cached=is_cached, default_action=action)
            sys.exit(0)

        if cmd_arg.lower() in ('--latest', 'latest'):
            print_banner(base_url, is_cached=is_cached)
            ep = episodes[0]
            print(f"{CLR_GREEN}➜ Processing latest episode: {ep.get('title')}{CLR_RESET}")
            handle_episode(ep, base_url, action=action or 'stream_and_download', player_choice=player_choice, target_quality="best")
            sys.exit(0)

        if cmd_arg.isdigit():
            print_banner(base_url, is_cached=is_cached)
            num = int(cmd_arg)
            if 1 <= num <= len(episodes):
                handle_episode(episodes[num - 1], base_url, action=action, player_choice=player_choice)
                sys.exit(0)
            else:
                print(f"{CLR_RED}✖ Number out of range (1 - {len(episodes)}){CLR_RESET}")
                sys.exit(1)

        # Search query on CLI
        q = cmd_arg.lower()
        matches = []
        for e in episodes:
            if q in e.get('title', '').lower() or q in e.get('dataId', '').lower():
                matches.append(e)
            elif q in ('members', 'member', 'bonus') and is_members_only_content(e):
                matches.append(e)

        if len(matches) == 1:
            print_banner(base_url, is_cached=is_cached)
            handle_episode(matches[0], base_url, action=action, player_choice=player_choice)
            sys.exit(0)
        elif len(matches) > 1:
            print_banner(base_url, is_cached=is_cached)
            print(f"{CLR_BOLD}{CLR_YELLOW}Matches for '{cmd_arg}':{CLR_RESET}")
            for idx, m in enumerate(matches, 1):
                print(f"  [{idx}] {m.get('title')}")
            try:
                ans = input("\nSelect number to open: ").strip()
                if ans.isdigit() and 1 <= int(ans) <= len(matches):
                    handle_episode(matches[int(ans) - 1], base_url, action=action, player_choice=player_choice)
                    sys.exit(0)
            except (EOFError, KeyboardInterrupt):
                sys.exit(0)

    # Launch interactive mode
    run_cli_interactive(episodes, base_url, is_cached=is_cached)

if __name__ == '__main__':
    main()
