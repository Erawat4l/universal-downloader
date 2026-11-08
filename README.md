# Universal Downloader (`dl`) 🚀

**Universal Downloader** is a high-speed CLI downloader for stream extraction, video/audio downloading, and Telegram media retrieval. Compatible with **Ubuntu, Debian, Arch Linux, Fedora, macOS, Android (Termux), and Windows (WSL / PowerShell)**.

---

## ✨ Features

- **Multi-Engine Speed**: Combines `yt-dlp` stream parser with `aria2c` multi-connection downloader (up to 16 parallel threads).
- **Telegram Downloader**: Supports private & public Telegram channel messages and direct media files (`t.me/c/chat_id/msg_id`).
- **Progressive Stream Merging**: Auto-merges `bestvideo+bestaudio` streams with FFmpeg.
- **Resumable & Fault Tolerant**: Pause, resume, auto-retry, and session cookie support.
- **Zero Hardcoded Secrets**: Fully configurable via environment variables (`.env`) or interactive prompts.

---

## 📋 Prerequisites & Installation

### 1. Ubuntu / Debian / Linux Mint
```bash
sudo apt update && sudo apt install python3 ffmpeg aria2 git -y
pip install yt-dlp pyrogram tgcrypto requests
```

### 2. Arch Linux / Manjaro
```bash
sudo pacman -S python ffmpeg aria2 git
pip install yt-dlp pyrogram tgcrypto requests
```

### 3. Fedora / RHEL
```bash
sudo dnf install python3 ffmpeg aria2 git
pip install yt-dlp pyrogram tgcrypto requests
```

### 4. macOS (Homebrew)
```bash
brew install python ffmpeg aria2 git
pip install yt-dlp pyrogram tgcrypto requests
```

### 5. Android (Termux)
```bash
pkg update && pkg install python ffmpeg aria2c git -y
pip install yt-dlp pyrogram tgcrypto requests
```

---

## ⚙️ Configuration (Telegram Credentials)

To download files from Telegram, configure your Telegram `API_ID` and `API_HASH` (obtained from [my.telegram.org](https://my.telegram.org)).

Set them in your shell environment or `.env` file (or enter interactively when prompted):

```bash
export TELEGRAM_API_ID="1234567"
export TELEGRAM_API_HASH="your_api_hash_here"
# Optional session string:
export TELEGRAM_SESSION="your_string_session"
```

---

## 🛠️ Usage Examples

```bash
# 1. Download YouTube / Web Video
python3 dl.py "https://www.youtube.com/watch?v=dQw4w9WgXcQ"

# 2. Download from Telegram Link
python3 dl.py "https://t.me/c/1234567890/100"

# 3. Specify Custom Directory and Output Name
python3 dl.py "https://example.com/video.mp4" -o "custom_name.mp4" -d "/path/to/downloads"
```

---

## 📱 Android app

The native Android project is in `android-app/`. From a full checkout of the app branch, update and build with:

```bat
git pull --ff-only origin arena/87a00d95-universal-downloader
build.bat
```

`build.bat` delegates to the shared Python builder, which creates a local debug APK at `android-app/app/build/outputs/apk/debug/app-debug.apk`. It does not publish or modify signing keys.

## 📄 License

Licensed under the MIT License.
