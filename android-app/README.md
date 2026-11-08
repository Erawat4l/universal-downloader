# Arc Android app

**Arc** is the premium black-and-white Android frontend for the Universal Downloader CLI in this repository.

I chose **Arc** as the working name: short, classic, memorable, and aligned with a clean monochrome product. The display name is isolated in `app/src/main/res/values/strings.xml` if you want to rename it later.

## Build on Windows

The Arena workspace cannot write directly to `C:\Users\kavi\Desktop\erawat\app`. Copy this entire `android-app` folder there:

```text
C:\Users\kavi\Desktop\erawat\app
```

From the project folder, run one command. It finds or installs the supported Java/SDK/Gradle pieces, generates the wrapper when needed, selects a version from GitHub Releases, and builds a local debug APK:

```bat
build.bat
```

The `.bat` file is only a thin Python launcher. Termux/fish can use the same builder with:

```sh
./build-android.sh
```

The APK will be at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Requirements:

- Android Studio with JDK 17.
- Android SDK 35.
- An Android device/emulator with Android 8.0 or newer.

## CLI connection

The UI mirrors the actual `dl.py` capabilities: direct files, supported video platforms, Telegram, educational PDFs, NetMirror, quality selection, audio-only formats, direct links, VLC streaming, retries, resume, and progress.

When Termux is installed, the Download action sends the URL and selected quality to:

```text
~/universal-downloader/dl.py
```

The path is defined in `TermuxBridge.kt` and can be changed if the CLI is stored elsewhere. A future packaged engine can replace that bridge without changing the UI.

## GitHub updates

Settings checks:

```text
https://api.github.com/repos/Erawat4l/universal-downloader/commits/master
https://api.github.com/repos/Erawat4l/universal-downloader/releases/latest
```

Source commits are detected for the CLI. A one-tap APK update becomes available when a GitHub Release contains a signed `.apk` asset. Android should not replace an installed APK from an arbitrary source commit.

For CI, replace `SOURCE_COMMIT` in `app/build.gradle.kts` with the commit used to build each APK and publish the signed APK in a GitHub Release.
