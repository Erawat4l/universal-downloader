#!/usr/bin/env python3
"""Self-healing, repeatable debug builder for the Arc Android app.

The .bat/.sh files intentionally only locate Python. All build decisions live here
so Windows and Unix-like shells use the same workflow.
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
REPOSITORY_ROOT = APP_ROOT.parent
PYTHON_ENGINE_SOURCE = REPOSITORY_ROOT / "dl.py"
PYTHON_ENGINE_DEST = APP_ROOT / "app" / "src" / "main" / "python" / "dl.py"
PYTHON_IGL_SOURCE = REPOSITORY_ROOT / "igl.py"
PYTHON_IGL_DEST = APP_ROOT / "app" / "src" / "main" / "python" / "igl.py"
TOOLS_DIR = APP_ROOT / ".tools"
GRADLE_VERSION = "8.9"
GRADLE_DISTRIBUTION_URL = f"https://downloads.gradle.org/distributions/gradle-{GRADLE_VERSION}-bin.zip"
GRADLE_NETWORK_TIMEOUT_MS = "600000"
ANDROID_API = "35"
BUILD_TOOLS = "35.0.0"
REPOSITORY = "Erawat4l/universal-downloader"
BRANCH = "main"


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


def ensure_build_python() -> str:
    """Find a Python version supported by Chaquopy 17.2 and return its major/minor."""
    supported = ("3.14", "3.13", "3.12")
    for wanted in supported:
        major, minor = (int(value) for value in wanted.split("."))
        candidates: list[list[str]] = []
        if sys.version_info[:2] == (major, minor):
            candidates.append([sys.executable])
        if os.name == "nt":
            candidates.extend([["py", f"-{wanted}"], [f"python{wanted}"], ["python"]])
        else:
            candidates.extend([[f"python{wanted}"], ["python3"], ["python"]])

        for candidate in candidates:
            try:
                result = subprocess.run(
                    [*candidate, "--version"],
                    capture_output=True,
                    text=True,
                    check=True,
                )
            except (OSError, subprocess.SubprocessError):
                continue
            version = f"{result.stdout} {result.stderr}".strip()
            if re.search(rf"Python {re.escape(wanted)}(?:\.|$)", version):
                say(f"Python {wanted} found for Chaquopy: {' '.join(candidate)}")
                return wanted

    raise BuildError(
        "Python 3.12, 3.13, or 3.14 is required to package the embedded downloader engine. "
        "Install one supported version and run this same command again."
    )


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
    system_sdkmanager = shutil.which("sdkmanager")
    if system_sdkmanager:
        return Path(system_sdkmanager)
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

    url = GRADLE_DISTRIBUTION_URL
    say(f"Downloading Gradle {GRADLE_VERSION}…")
    urllib.request.urlretrieve(url, archive)
    with zipfile.ZipFile(archive) as bundle:
        bundle.extractall(TOOLS_DIR)
    return gradle_bin


def pin_wrapper_properties(wrapper_properties: Path) -> None:
    expected_distribution = f"gradle-{GRADLE_VERSION}-bin.zip"
    properties = wrapper_properties.read_text(encoding="utf-8") if wrapper_properties.exists() else ""
    changed = False

    if expected_distribution not in properties or GRADLE_DISTRIBUTION_URL not in properties:
        properties = re.sub(r"distributionUrl=.*", f"distributionUrl={GRADLE_DISTRIBUTION_URL}", properties)
        changed = True

    timeout_line = f"networkTimeout={GRADLE_NETWORK_TIMEOUT_MS}"
    timeout_match = re.search(r"^networkTimeout=.*", properties, re.MULTILINE)
    if timeout_match:
        if timeout_match.group(0) != timeout_line:
            properties = re.sub(r"^networkTimeout=.*", timeout_line, properties, flags=re.MULTILINE)
            changed = True
    else:
        properties = properties.rstrip("\\n") + "\\n" + timeout_line + "\\n"
        changed = True

    if changed:
        wrapper_properties.parent.mkdir(parents=True, exist_ok=True)
        wrapper_properties.write_text(properties, encoding="utf-8")


def ensure_wrapper() -> Path:
    wrapper = APP_ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    wrapper_properties = APP_ROOT / "gradle" / "wrapper" / "gradle-wrapper.properties"

    if wrapper.exists() and wrapper_properties.exists():
        properties_before = wrapper_properties.read_text(encoding="utf-8")
        if f"gradle-{GRADLE_VERSION}-bin.zip" not in properties_before:
            # A previous Termux run may have generated a wrapper for the
            # system Gradle (for example 9.9). AGP 8.7.3 is built with 8.9.
            say(f"Resetting Gradle wrapper to {GRADLE_VERSION} (existing wrapper was different).")
        pin_wrapper_properties(wrapper_properties)
        if os.name != "nt":
            wrapper.chmod(wrapper.stat().st_mode | 0o111)
        return wrapper

    gradle = shutil.which("gradle.bat" if os.name == "nt" else "gradle")
    gradle_command = gradle or str(download_gradle_distribution())
    command = [
        gradle_command,
        "wrapper",
        f"--gradle-version={GRADLE_VERSION}",
        f"--gradle-distribution-url={GRADLE_DISTRIBUTION_URL}",
        "--distribution-type=bin",
    ]
    if os.name == "nt":
        command = ["cmd", "/d", "/c", *command]
    run(command)
    if not wrapper.exists():
        raise BuildError("Gradle wrapper generation completed but gradlew was not created.")
    if not wrapper_properties.exists():
        raise BuildError("Gradle wrapper generation completed but its properties file was not created.")
    pin_wrapper_properties(wrapper_properties)
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


def running_in_termux() -> bool:
    prefix = os.environ.get("PREFIX", "")
    return os.name != "nt" and "/com.termux/" in prefix


def configure_sdk_for_gradle(sdk: Path) -> None:
    """Make the SDK explicit for both Gradle and Android Studio."""
    sdk_path = sdk.resolve()
    os.environ["ANDROID_HOME"] = str(sdk_path)
    os.environ["ANDROID_SDK_ROOT"] = str(sdk_path)
    # local.properties is machine-local and ignored by Git. Forward slashes work on Windows too.
    local_properties = APP_ROOT / "local.properties"
    local_properties.write_text(f"sdk.dir={sdk_path.as_posix()}\n", encoding="utf-8")
    say(f"Gradle SDK path configured: {sdk_path}")

    if running_in_termux():
        aapt2 = shutil.which("aapt2")
        if not aapt2:
            raise BuildError("Termux requires the ARM64 aapt2 package. Run: pkg install aapt2")
        os.environ["ARC_AAPT2_OVERRIDE"] = aapt2
        say(f"Termux aapt2 override: {aapt2}")


def sync_python_engine() -> None:
    """Package the same downloader engine used by the repository into the APK."""
    if not PYTHON_ENGINE_SOURCE.exists():
        raise BuildError(f"Bundled Python engine source is missing: {PYTHON_ENGINE_SOURCE}")
    PYTHON_ENGINE_DEST.parent.mkdir(parents=True, exist_ok=True)
    if not PYTHON_ENGINE_DEST.exists() or PYTHON_ENGINE_DEST.read_bytes() != PYTHON_ENGINE_SOURCE.read_bytes():
        shutil.copy2(PYTHON_ENGINE_SOURCE, PYTHON_ENGINE_DEST)
        say(f"Bundled Python engine synced from {PYTHON_ENGINE_SOURCE}")
    else:
        say("Bundled Python engine is up to date.")

    if PYTHON_IGL_SOURCE.exists():
        if not PYTHON_IGL_DEST.exists() or PYTHON_IGL_DEST.read_bytes() != PYTHON_IGL_SOURCE.read_bytes():
            shutil.copy2(PYTHON_IGL_SOURCE, PYTHON_IGL_DEST)
            say(f"Bundled IGL module synced from {PYTHON_IGL_SOURCE}")


def number_and_send_artifact(artifact: Path) -> Path:
    output_dir = artifact.parent
    sdcard_downloads = Path("/sdcard/Download")
    numbers = []
    for directory in [output_dir, sdcard_downloads]:
        if directory.exists():
            for existing in directory.glob("* *.apk"):
                match = re.fullmatch(r"(?:A4C|ARC)\s+(\d+)\.apk", existing.name, re.IGNORECASE)
                if match:
                    numbers.append(int(match.group(1)))
    next_num = (max(numbers) + 1) if numbers else 1
    numbered = output_dir / f"ARC {next_num}.apk"
    shutil.copy2(artifact, numbered)
    say(f"Numbered APK: {numbered}")

    if sdcard_downloads.exists():
        sdcard_apk = sdcard_downloads / f"ARC {next_num}.apk"
        shutil.copy2(artifact, sdcard_apk)
        say(f"Saved to public Downloads: {sdcard_apk}")
        send_target = sdcard_apk
    else:
        send_target = numbered

    tt = shutil.which("tt")
    if not tt:
        say("tt was not found on PATH; skipped transfer.")
        return numbered

    say(f"Sending {send_target.name} to device 2 with tt…")
    try:
        result = subprocess.run(
            [tt, "send", "2", str(send_target)],
            cwd=APP_ROOT,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
        )
        if result.returncode == 0:
            say("tt transfer completed.")
        else:
            say(f"tt transfer failed with exit code {result.returncode}; the APK is still available locally.")
            if result.stdout.strip():
                print(result.stdout.strip(), flush=True)
            if result.stderr.strip():
                print(result.stderr.strip(), file=sys.stderr, flush=True)
    except OSError as error:
        say(f"tt transfer could not start: {error}; the APK is still available locally.")
    return numbered


def main() -> int:
    if os.environ.get("ARC_PUBLISH", "").lower() in {"1", "true", "yes"}:
        raise BuildError("This script only creates local debug APKs. Publishing is intentionally refused.")

    chaquopy_python_version = ensure_build_python()
    java, java_home = find_java()
    os.environ["JAVA_HOME"] = str(java_home)
    os.environ["PATH"] = str(Path(java).parent) + os.pathsep + os.environ.get("PATH", "")
    say(f"Java found: {java}")

    sdk = ensure_android_sdk()
    configure_sdk_for_gradle(sdk)
    sync_python_engine()
    wrapper = ensure_wrapper()
    version_name, version_code = build_version()

    gradle_arguments = [
        "--no-daemon",
        "assembleDebug",
        f"-PVERSION_NAME={version_name}",
        f"-PVERSION_CODE={version_code}",
        f"-PSOURCE_COMMIT={source_revision()}",
        f"-PCHAQUOPY_PYTHON_VERSION={chaquopy_python_version}",
    ]
    aapt2_override = os.environ.get("ARC_AAPT2_OVERRIDE")
    if aapt2_override:
        gradle_arguments.append(f"-Pandroid.aapt2FromMavenOverride={aapt2_override}")

    if os.name == "nt":
        command = ["cmd", "/d", "/c", str(wrapper), *gradle_arguments]
    else:
        command = [str(wrapper), *gradle_arguments]
    run(command)

    artifact = APP_ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
    if not artifact.exists():
        raise BuildError(f"Gradle finished but the expected APK is missing: {artifact}")
    say(f"Built local debug APK: {artifact}")
    number_and_send_artifact(artifact)
    say("This is a local debug artifact; it is not a release upload.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except BuildError as error:
        print(f"[arc-build] ERROR: {error}", file=sys.stderr)
        raise SystemExit(1)
