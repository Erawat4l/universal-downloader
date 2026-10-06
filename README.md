# Universal Downloader (`dl`) 🚀

**Universal Downloader** is a feature-packed CLI tool for high-speed multi-threaded media downloading, stream extraction, and Telegram file downloader.

---

## ✨ Key Features

- **Multi-Engine Downloader**: Uses `yt-dlp` for videos/audio streams and fallback multi-threaded `aria2c` engine for maximum bandwidth speed.
- **Telegram Downloader**: Supports downloading private and public Telegram files, media, and channel messages by link (e.g. `t.me/c/chat_id/msg_id`).
- **Auto Video & Audio Merging**: Automatically picks and merges progressive `bestvideo+bestaudio` streams with FFmpeg.
- **Network Resuming**: Supports pause, resume, auto-retry, and custom session cookies.
- **CLI Utility Integration**: Instant launch with simple commands like `dl "<URL>"`.

---

## 📋 Prerequisites & Installation

### Requirements
- **Python**: `3.8` or higher
- **System Packages**: `yt-dlp`, `aria2`, `ffmpeg`

### Installation Commands

```bash
# Termux (Android)
pkg update && pkg install python ffmpeg aria2c git -y
pip install yt-dlp pyrogram tgcrypto requests

# Ubuntu / Debian / macOS
sudo apt update && sudo apt install python3 ffmpeg aria2 git -y
pip install yt-dlp pyrogram tgcrypto requests

# Clone Repository
git clone https://github.com/Erawat4l/universal-downloader.git
cd universal-downloader
chmod +x dl.py
```

---

## ⚙️ Configuration (Telegram Credentials)

To download files from Telegram, configure your Telegram `API_ID` and `API_HASH` (obtained from [my.telegram.org](https://my.telegram.org)).

You can either pass them as environment variables or enter them interactively when prompted:

```bash
export TELEGRAM_API_ID="1234567"
export TELEGRAM_API_HASH="your_api_hash_here"
# Optional session string if using account session:
export TELEGRAM_SESSION="your_string_session"
```

---

## 🛠️ Usage Examples

### 1. Download Media / YouTube Video
```bash
python3 dl.py "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
```

### 2. Download from Telegram Link
```bash
python3 dl.py "https://t.me/c/1234567890/100"
```

### 3. Custom Output Directory and Filename
```bash
python3 dl.py "https://example.com/file.mp4" -o "my_video.mp4" -d "/sdcard/Downloads"
```

---

## 📄 License

Licensed under the MIT License.
