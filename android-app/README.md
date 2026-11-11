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

The `.bat` file is only a thin Python launcher. A shell can use the same builder with:

```sh
./build-android.sh
```

The APK will be at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

After a successful build, the builder also creates the next local artifact:

```text
app\build\outputs\apk\debug\A4C 1.apk
```

The number increments on each build. If `tt` is available on PATH, that numbered APK is automatically sent with:

```text
tt send 2 <apk-path>
```

A failed `tt` transfer does not hide or delete the local APK.

For a complete on-device Termux setup, see the repository-level [`TERMUX.md`](../TERMUX.md).

Requirements:

- Android Studio with JDK 17.
- Python 3.12, 3.13, or 3.14 on the build machine (Chaquopy packages whichever supported version the builder selects).
- Android SDK 35.
- Internet access during the first build so Gradle and the Python wheels can be resolved.
- An Android device/emulator with Android 8.0 or newer.

## Standalone downloader engine

The app does not seed demo search results, fake files, fake progress, or fake stream URLs. The repository's real `dl.py` engine is bundled into the APK with Chaquopy, together with its Python dependencies (`requests`, BeautifulSoup, yt-dlp, and Pyrogram).

Search, resolve, stream, and download requests execute inside the app process. No Termux installation, shell, `RUN_COMMAND` permission, or companion service is required. The builder synchronizes the repository root `dl.py` into `app/src/main/python/dl.py` before Gradle packages the APK.

The UI mirrors the engine capabilities: direct files, supported video platforms, Telegram, educational PDFs, NetMirror, quality selection, audio-only formats, direct links, retries, resume, and real download output. If a dependency, network request, extractor, or Telegram credential is unavailable, Arc reports the actual error instead of showing fallback data.

Downloads are written to the app's private Downloads directory. This avoids Termux and scoped-storage workarounds; the returned file path is shown in Activity and Library so it can be opened or shared by a later file action.

### Telegram setup

Open **Settings → Telegram → Setup**. Public channel posts may work through the web fallback. Private channels and reliable media downloads require:

- Telegram API ID.
- Telegram API hash.
- A Pyrogram string session.

These values are stored in the app's private preferences and sent only to the bundled engine for a Telegram download. No credentials are embedded in the repository.

## GitHub updates

Settings checks:

```text
https://api.github.com/repos/Erawat4l/universal-downloader/commits/master
https://api.github.com/repos/Erawat4l/universal-downloader/releases/latest
```

Source commits are detected for the CLI. A one-tap APK update becomes available when a GitHub Release contains a signed `.apk` asset. Android should not replace an installed APK from an arbitrary source commit.

For CI, replace `SOURCE_COMMIT` in `app/build.gradle.kts` with the commit used to build each APK and publish the signed APK in a GitHub Release.
