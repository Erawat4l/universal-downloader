# Build Arc from Termux

This guide builds the **standalone Arc APK** on an ARM64 Android device using Termux.

Termux is only the build environment. The APK itself bundles `dl.py`, Python, yt-dlp, Pyrogram, Requests, and BeautifulSoup. The installed APK does **not** require Termux, a shell, or a Termux service to download files.

## 1. Install the Termux build packages

Use the current Termux release from F-Droid or the official Termux GitHub releases. Then run:

```sh
pkg update && pkg upgrade -y
pkg install -y git python openjdk-17 wget unzip zip
pkg install -y aapt2 apksigner zipalign
termux-setup-storage
```

Check the versions before continuing:

```sh
python --version
java -version
```

The Arc builder currently packages the embedded engine with **Python 3.12**. If `python --version` does not report Python 3.12, update Termux packages before building.

## 2. Clone the app branch

Clone the Arena branch directly so the standalone-engine changes are included:

```sh
git clone --branch arena/87a00d95-universal-downloader --single-branch https://github.com/Erawat4l/universal-downloader.git
cd universal-downloader
```

Verify the branch:

```sh
git branch --show-current
git log -1 --oneline
```

It should show:

```text
arena/87a00d95-universal-downloader
```

## 3. Install Android SDK 35

The Gradle project needs Android platform 35 and build tools 35.0.0. The following keeps the SDK inside Termux storage:

```sh
export ANDROID_HOME="$PREFIX/opt/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
mkdir -p "$ANDROID_HOME"
cd "$ANDROID_HOME"
```

Download the Android command-line tools:

```sh
wget -O cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
SDK_TMP="$(mktemp -d)"
unzip -q cmdline-tools.zip -d "$SDK_TMP"
mkdir -p "$ANDROID_HOME/cmdline-tools"
rm -rf "$ANDROID_HOME/cmdline-tools/latest"
mv "$SDK_TMP/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
rm -rf "$SDK_TMP" cmdline-tools.zip
```

Add the SDK tools to the current shell:

```sh
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
```

Install the required SDK components:

```sh
yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses
sdkmanager --sdk_root="$ANDROID_HOME" "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

To make the paths available in future Termux sessions:

```sh
cat >> "$HOME/.profile" <<'EOF'
export ANDROID_HOME="$PREFIX/opt/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
EOF
source "$HOME/.profile"
```

## 4. Build the standalone APK

From the repository root:

```sh
chmod +x android-app/build-android.sh
./android-app/build-android.sh
```

The builder will:

- Verify Python 3.12 and Java 17.
- Configure the Android SDK path.
- Use the Termux ARM64 `aapt2` package instead of an incompatible desktop `aapt2` binary.
- Synchronize the root `dl.py` into the APK Python source directory.
- Download Gradle and Chaquopy dependencies when needed.
- Build a local debug APK.
- Create the next numbered APK: `A4C 1.apk`, `A4C 2.apk`, and so on.
- Run `tt send 2 <apk>` automatically if the `tt` command is installed.

The files are created here:

```text
android-app/app/build/outputs/apk/debug/app-debug.apk
android-app/app/build/outputs/apk/debug/A4C N.apk
```

The first build can require significant storage and network data because it packages the Python runtime and downloader dependencies.

## 5. Update and rebuild later

```sh
cd "$HOME/universal-downloader"
git pull --ff-only origin arena/87a00d95-universal-downloader
./android-app/build-android.sh
```

The branch is intentionally fixed to `arena/87a00d95-universal-downloader` for this Arena session.

## Troubleshooting

### Python version error

```sh
python --version
pkg update && pkg upgrade -y
```

The current Android build is configured for Python 3.12. Do not change the app's Chaquopy Python version independently from the builder check.

### `aapt2` is missing

Install the Termux ARM64 package and rerun the build:

```sh
pkg install aapt2
```

The builder will pass `android.aapt2FromMavenOverride` automatically when it detects Termux.

### `sdkmanager` is not found

Reload the SDK path:

```sh
export ANDROID_HOME="$PREFIX/opt/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
```

Then confirm:

```sh
sdkmanager --version
```

### Storage is full

Check available space before retrying:

```sh
df -h "$HOME"
du -sh "$HOME/.gradle" "$ANDROID_HOME" 2>/dev/null
```

The APK build cache and Chaquopy Python packages are large. Do not delete signing keys; this workflow only creates local debug APKs.

### Telegram setup in the APK

After installing the APK, open:

```text
Settings → Telegram → Setup
```

Enter the Telegram API ID, API hash, and Pyrogram string session. These are stored in the app's private storage and are not hardcoded into the repository.
