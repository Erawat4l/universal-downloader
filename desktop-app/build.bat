@echo off
REM ============================================================================
REM  Arc Desktop - build Arc.exe
REM
REM  Double-click this file, or run it from a terminal:
REM      build.bat                one-file Arc.exe
REM      build.bat --onedir       folder build (starts faster)
REM
REM  Requirements: Windows 10/11, Python 3.9+ from python.org with
REM  "Add python.exe to PATH" ticked. Everything else is installed for you.
REM ============================================================================
setlocal EnableDelayedExpansion
cd /d "%~dp0"

echo.
echo  Arc Desktop - Windows build
echo  ----------------------------------------------------------------

where python >nul 2>nul
if errorlevel 1 (
    echo  [FAIL] Python was not found on PATH.
    echo.
    echo         Install it from https://www.python.org/downloads/windows/
    echo         and tick "Add python.exe to PATH" in the first screen.
    echo.
    pause
    exit /b 1
)

for /f "delims=" %%v in ('python -c "import sys;print('%%d.%%d'%%sys.version_info[:2])"') do set PYVER=%%v
echo  Python %PYVER%

REM --- virtual environment keeps the build reproducible ----------------------
if not exist ".venv\Scripts\python.exe" (
    echo  Creating a build environment in .venv ...
    python -m venv .venv
    if errorlevel 1 (
        echo  [FAIL] Could not create .venv
        pause
        exit /b 1
    )
)
set PY=.venv\Scripts\python.exe

echo  Installing runtime requirements ...
"%PY%" -m pip install --upgrade pip >nul
"%PY%" -m pip install -r requirements.txt
if errorlevel 1 (
    echo  [FAIL] Dependency install failed. Check your connection and retry.
    pause
    exit /b 1
)

REM --- self-test the GUI headless before freezing ----------------------------
echo  Running the headless UI self-test ...
set QT_QPA_PLATFORM=offscreen
"%PY%" -m arc_desktop --selftest
if errorlevel 1 (
    echo  [FAIL] The UI self-test did not pass, so the .exe would be broken too.
    echo         Fix the reported problem and run build.bat again.
    pause
    exit /b 1
)
set QT_QPA_PLATFORM=

REM --- freeze ----------------------------------------------------------------
echo  Freezing Arc.exe ...
"%PY%" build_exe.py --skip-checks %*
if errorlevel 1 (
    echo  [FAIL] PyInstaller reported an error.
    pause
    exit /b 1
)

echo.
echo  ----------------------------------------------------------------
if exist "dist\Arc.exe" (
    echo  DONE   dist\Arc.exe
    echo         Double-click it to run Arc.
) else if exist "dist\Arc\Arc.exe" (
    echo  DONE   dist\Arc\Arc.exe
    echo         Ship the whole dist\Arc folder, zipped.
)
echo.
echo  For maximum speed also install:
echo      winget install aria2.aria2
echo      winget install Gyan.FFmpeg
echo  ----------------------------------------------------------------
echo.
pause
endlocal
exit /b 0
