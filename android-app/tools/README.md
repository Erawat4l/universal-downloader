# Android build tool

`build-android.bat` and `build-android.sh` are intentionally thin launchers. They only find Python and hand control to `tools/build_android.py`.

The Python builder:

- Requires Python 3.12, 3.13, or 3.14 for the embedded Chaquopy engine.
- Finds or installs Java 17 where a supported package manager is available.
- Finds Android SDK 35 and required build tools.
- Generates the Gradle wrapper if the project does not have one.
- Reads the latest published GitHub Release version.
- Uses a local-only version such as `1.2.3-local.abc1234`.
- Runs `gradlew --no-daemon assembleDebug -PVERSION_NAME=<ver>`.
- Refuses publish mode and never touches signing keys.
- Works with the same Python entry point on Windows and Unix-like shells where Java, Gradle, and Android SDK are available.
