#!/usr/bin/env python3
"""Self-healing, repeatable debug builder for the Arc Android app.

The .bat/.sh files intentionally only locate Python. All build decisions live here
so Windows and Termux use the same workflow.
"""

from __future__ import annotations

import json
import os
import platform
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import zipfile
from pathlib import Path

APP_ROOT = Path(__file__).resolve().parents[1]
TOOLS_DIR = APP_ROOT / ".tools"
GRADLE_VERSION = "8.9"
ANDROID_API = "35"
BUILD_TOOLS = "35.0.0"
REPOSITORY = "Erawat4l/universal-downloader"
BRANCH = "master"


class BuildError(RuntimeError):
    pass


def say(message: str) -> None:
    print(f"[arc-build] {message}", flush=True)


def command_exists(name: str) -> bool:
    return shutil.which(name) is not None


def run(command: list[str], cwd: Path = APP_ROOT, input_text: str | None = None) -> subprocess.CompletedProcess[str]:
    say("$ " + " ".join(str(part) for part in command))
    try:
        return subprocess.run(command, cwd=cwd, input=input_text, text=True, check=True)
    except FileNotFoundError as error:
        raise BuildError(f"Missing command: {command[0]}") from error
    except subprocess.CalledProcessError as error:
        raise BuildError(f"Command failed with exit code {error.returncode}.") from error


def run_capture(command: list[str], cwd: Path = APP_ROOT) -> str:
    try:
        result = subprocess.run(command, cwd=cwd, capture_output=True, text=True, check=True)
        return result.stdout.strip()
    except (OSError, subprocess.SubprocessError):
        return ""


def install_with_winget(package_id: str, label: str) -> bool:
    if os.name != "nt" or not command_exists("winget"):
        return False
    say(f"Installing {label} with winget…")
    run([
        "winget", "install", "--id", package_id, "--exact",
        "--accept-source-agreements", "--accept-package-agreements",
    ])
    return True


def java_home_candidates() -> list[Path]:
    candidates: list[Path] = []
    for key in ("JAVA_HOME", "JDK_HOME"):
        if os.environ.get(key):
            candidates.append(Path(os.environ[key]))
    if os.name == "nt":
        candidates.extend([
            Path(os.environ.get("ProgramFiles", "C:/Program Files")) / "Android/Android Studio/jbr",
            Path(os.environ.get("LOCALAPPDATA", "")) / "Programs/Android Studio/jbr",
            Path(os.environ.get("ProgramFiles", "C:/Program Files")) / "Java",
        ])
    else:
        candidates.extend([
            Path(os.environ.get("PREFIX", "")) / "lib/jvm/openjdk-17",
            Path(os.environ.get("JAVA_HOME", "")),
            Path("/usr/lib/jvm/java-17-openjdk-amd64"),
        ])
    return candidates


def find_java() -> tuple[str, Path]:
    executable = "java.exe" if os.name == "nt" else "java"
    for candidate in java_home_candidates():
        if candidate.is_file() and candidate.name.lower() == executable.lower():
            return str(candidate), candidate.parent.parent
        path = candidate / "bin" / executable
        if path.exists():
            return str(path), candidate
    system_java = shutil.which(executable)
    if system_java:
        return system_java, Path(system_java).resolve().parent.parent

    if os.name == "nt" and install_with_winget("EclipseAdoptium.Temurin.17.JDK", "JDK 17"):
        system_java = shutil.which(executable)
        if system_java:
            return system_java, Path(system_java).resolve().parent.parent
    if command_exists("pkg"):
        run(["pkg", "install", "-y", "openjdk-17"])
        system_java = shutil.which(executable)
        if system_java:
            return system_java, Path(system_java).resolve().parent.parent

    raise BuildError(
        "Java 17 is required. Install Android Studio (its bundled JDK is accepted) "
        "or JDK 17, then run this same command again."
    )


def sdk_candidates() -> list[Path]:
    values = [os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT")]
    if os.name == "nt":
        values.extend([
            str(Path(os.environ.get("LOCALAPPDATA", "")) / "Android/Sdk"),
            str(Path.home() / "AppData/Local/Android/Sdk"),
        ])
    else:
        values.extend([
            os.environ.get("ANDROID_HOME"),
            str(Path.home() / "Android/Sdk"),
            str(Path(os.environ.get("PREFIX", "")) / "opt/android-sdk"),
        ])
    return [Path(value) for value in values if value]


def find_sdk() -> Path | None:
    for candidate in sdk_candidates():
        if (candidate / "platforms" / f"android-{ANDROID_API}").exists():
            return candidate
    return None


def find_sdkmanager(sdk: Path) -> Path | None:
    names = ["sdkmanager.bat", "sdkmanager"]
    candidates = [sdk / "cmdline-tools" / "latest" / "bin", sdk / "cmdline-tools" / "bin", sdk / "tools" / "bin"]
    for folder in candidates:
        for name in names:
            path = folder / name
            if path.exists():
                return path
    return None


def ensure_android_sdk() -> Path:
    sdk = find_sdk()
    if sdk:
        sdkmanager = find_sdkmanager(sdk)
        if sdkmanager:
            say(f"Android SDK found at {sdk}")
            run([str(sdkmanager), f"--sdk_root={sdk}", "platform-tools", f"platforms;android-{ANDROID_API}", f"build-tools;{BUILD_TOOLS}"], input_text="y\n" * 50)
        return sdk

    if os.name == "nt" and install_with_winget("Google.AndroidStudio", "Android Studio"):
        sdk = find_sdk()
        if sdk:
            sdkmanager = find_sdkmanager(sdk)
            if sdkmanager:
                run([str(sdkmanager), f"--sdk_root={sdk}", "platform-tools", f"platforms;android-{ANDROID_API}", f"build-tools;{BUILD_TOOLS}"], input_text="y\n" * 50)
                return sdk

    raise BuildError(
        "Android SDK 35 was not found. Install Android Studio/SDK 35, or set "
        "ANDROID_HOME, then run this same command again."
    )


def download_gradle_distribution() -> Path:
    TOOLS_DIR.mkdir(parents=True, exist_ok=True)
    archive = TOOLS_DIR / f"gradle-{GRADLE_VERSION}-bin.zip"
    install_dir = TOOLS_DIR / f"gradle-{GRADLE_VERSION}"
    gradle_bin = install_dir / ("bin/gradle.bat" if os.name == "nt" else "bin/gradle")
    if gradle_bin.exists():
        return gradle_bin

    url = f"https://services.gradle.org/distributions/gradle-{GRADLE_VERSION}-bin.zip"
    say(f"Downloading Gradle {GRADLE_VERSION}…")
    urllib.request.urlretrieve(url, archive)
    with zipfile.ZipFile(archive) as bundle:
        bundle.extractall(TOOLS_DIR)
    return gradle_bin


def ensure_wrapper() -> Path:
    wrapper = APP_ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    if wrapper.exists():
        if os.name != "nt":
            wrapper.chmod(wrapper.stat().st_mode | 0o111)
        return wrapper

    gradle = shutil.which("gradle.bat" if os.name == "nt" else "gradle")
    gradle_command = gradle or str(download_gradle_distribution())
    command = [gradle_command, "wrapper", f"--gradle-version={GRADLE_VERSION}", "--distribution-type=bin"]
    if os.name == "nt":
        command = ["cmd", "/d", "/c", *command]
    run(command)
    if not wrapper.exists():
        raise BuildError("Gradle wrapper generation completed but gradlew was not created.")
    if os.name != "nt":
        wrapper.chmod(wrapper.stat().st_mode | 0o111)
    return wrapper


def latest_published_version() -> tuple[str, str]:
    url = f"https://api.github.com/repos/{REPOSITORY}/releases/latest"
    request = urllib.request.Request(url, headers={"Accept": "application/vnd.github+json", "User-Agent": "arc-build"})
    try:
        with urllib.request.urlopen(request, timeout=8) as response:
            data = json.loads(response.read().decode("utf-8"))
        tag = str(data.get("tag_name", ""))
        match = re.search(r"(\d+)\.(\d+)\.(\d+)", tag)
        if match:
            return ".".join(match.groups()), f"GitHub Release {tag}"
    except Exception:
        pass
    return "0.1.0", "No published release found; using the initial project baseline"


def source_revision() -> str:
    revision = run_capture(["git", "rev-parse", "--short", "HEAD"])
    return revision or "local"


def build_version() -> tuple[str, str]:
    base, source = latest_published_version()
    revision = source_revision()
    local_name = f"{base}-local.{revision}"
    match = re.match(r"(\d+)\.(\d+)\.(\d+)", base)
    if not match:
        raise BuildError(f"Published version is not valid semver: {base}")
    major, minor, patch = (int(value) for value in match.groups())
    version_code = str(major * 1_000_000 + minor * 1_000 + patch + 1)
    say(f"Version source: {source}")
    say(f"Local debug version: {local_name} (versionCode {version_code})")
    return local_name, version_code


def main() -> int:
    if os.environ.get("ARC_PUBLISH", "").lower() in {"1", "true", "yes"}:
        raise BuildError("This script only creates local debug APKs. Publishing is intentionally refused.")

    java, java_home = find_java()
    os.environ["JAVA_HOME"] = str(java_home)
    os.environ["PATH"] = str(Path(java).parent) + os.pathsep + os.environ.get("PATH", "")
    say(f"Java found: {java}")

    ensure_android_sdk()
    wrapper = ensure_wrapper()
    version_name, version_code = build_version()

    if os.name == "nt":
        command = ["cmd", "/d", "/c", str(wrapper), "--no-daemon", "assembleDebug", f"-PVERSION_NAME={version_name}", f"-PVERSION_CODE={version_code}"]
    else:
        command = [str(wrapper), "--no-daemon", "assembleDebug", f"-PVERSION_NAME={version_name}", f"-PVERSION_CODE={version_code}"]
    run(command)

    artifact = APP_ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
    if not artifact.exists():
        raise BuildError(f"Gradle finished but the expected APK is missing: {artifact}")
    say(f"Built local debug APK: {artifact}")
    say("This is a local debug artifact; it is not a release upload.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except BuildError as error:
        print(f"[arc-build] ERROR: {error}", file=sys.stderr)
        raise SystemExit(1)
