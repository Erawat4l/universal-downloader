# Build Arc from Termux

This guide builds the **standalone Arc APK** on an ARM64 Android device using Termux.

Termux is only the build environment. The APK itself bundles `dl.py`, Python, yt-dlp, Pyrogram, Requests, and BeautifulSoup. The installed APK does **not** require Termux, a shell, or a Termux service to download files.

## One-block setup and build

For a fresh Termux installation, paste this entire block. It also works when the repository was already cloned:

```sh
set -eu

REPO="$HOME/universal-downloader"
BRANCH="arena/87a00d95-universal-downloader"
SDK="$PREFIX/opt/android-sdk"

pkg update
pkg upgrade -y
pkg install -y git python wget unzip zip aapt2 apksigner openjdk-17 || \
  pkg install -y git python wget unzip zip aapt2 apksigner openjdk-21
termux-setup-storage || true

PYTHON_MINOR="$(python -c 'import sys; print(f"{sys.version_info[0]}.{sys.version_info[1]}")')"
case "$PYTHON_MINOR" in
  3.12|3.13|3.14) ;;
  *)
    echo "This build requires Python 3.12, 3.13, or 3.14; found $PYTHON_MINOR."
    exit 1
    ;;
esac

if [ -d "$REPO/.git" ]; then
  cd "$REPO"
  git pull --ff-only origin "$BRANCH"
else
  git clone --branch "$BRANCH" --single-branch \
    https://github.com/Erawat4l/universal-downloader.git "$REPO"
  cd "$REPO"
fi

export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"
mkdir -p "$SDK"

if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  SDK_TMP="$(mktemp -d)"
  wget -q -O "$SDK_TMP/cmdline-tools.zip" \
    https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q "$SDK_TMP/cmdline-tools.zip" -d "$SDK_TMP"
  mkdir -p "$SDK/cmdline-tools"
  rm -rf "$SDK/cmdline-tools/latest"
  mv "$SDK_TMP/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -rf "$SDK_TMP"
fi

export PATH="$SDK/cmdline-tools/latest/bin:$SDK/platform-tools:$PATH"
yes | sdkmanager --sdk_root="$SDK" --licenses
sdkmanager --sdk_root="$SDK" \
  "platform-tools" "platforms;android-35" "build-tools;35.0.0"

chmod +x android-app/build-android.sh
exec ./android-app/build-android.sh
```

The first run can download more than 600 MB. Wait until the Termux prompt returns before entering another command.

## Fish shell version

The block above uses POSIX shell syntax. If your Termux prompt is Fish (`➜`), use this Fish-compatible block instead:

```fish
set REPO "$HOME/universal-downloader"
set BRANCH "arena/87a00d95-universal-downloader"
set SDK "$PREFIX/opt/android-sdk"

pkg update
pkg upgrade -y
if not pkg install -y git python wget unzip zip aapt2 apksigner openjdk-17
    pkg install -y git python wget unzip zip aapt2 apksigner openjdk-21
end

termux-setup-storage
or true

set PYTHON_MINOR (python -c 'import sys; print(f"{sys.version_info[0]}.{sys.version_info[1]}")')
switch "$PYTHON_MINOR"
    case 3.12 3.13 3.14
    case '*'
        echo "This build requires Python 3.12, 3.13, or 3.14; found $PYTHON_MINOR."
        exit 1
end

if test -d "$REPO/.git"
    cd "$REPO"
    git pull --ff-only origin "$BRANCH"
else
    git clone --branch "$BRANCH" --single-branch https://github.com/Erawat4l/universal-downloader.git "$REPO"
    cd "$REPO"
end

set -gx ANDROID_HOME "$SDK"
set -gx ANDROID_SDK_ROOT "$SDK"
mkdir -p "$SDK"

if not test -x "$SDK/cmdline-tools/latest/bin/sdkmanager"
    set SDK_TMP (mktemp -d)
    wget -q -O "$SDK_TMP/cmdline-tools.zip" https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
    unzip -q "$SDK_TMP/cmdline-tools.zip" -d "$SDK_TMP"
    mkdir -p "$SDK/cmdline-tools"
    rm -rf "$SDK/cmdline-tools/latest"
    mv "$SDK_TMP/cmdline-tools" "$SDK/cmdline-tools/latest"
    rm -rf "$SDK_TMP"
end

set -gx PATH "$SDK/cmdline-tools/latest/bin" "$SDK/platform-tools" $PATH
yes | sdkmanager --sdk_root="$SDK" --licenses
sdkmanager --sdk_root="$SDK" "platform-tools" "platforms;android-35" "build-tools;35.0.0"

chmod +x android-app/build-android.sh
exec ./android-app/build-android.sh
```

## 1. Install the Termux build packages

Use the current Termux release from F-Droid or the official Termux GitHub releases. Then run:

```sh
pkg update && pkg upgrade -y
pkg install -y git python openjdk-17 wget unzip zip
pkg install -y aapt2 apksigner
termux-setup-storage
```

Check the versions before continuing:

```sh
python --version
java -version
```

The Arc builder packages the embedded engine with Python **3.12, 3.13, or 3.14**, selecting the supported version available on the build machine. If Python reports an older or newer version, update Termux packages or install a supported Python version before building.

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

- Verify Python 3.12, 3.13, or 3.14 and Java 17.
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

The builder selects Python 3.12, 3.13, or 3.14 and passes the selected version to Chaquopy. Do not change the app's Chaquopy Python version independently from the builder.

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
