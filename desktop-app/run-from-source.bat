@echo off
REM ============================================================================
REM  Arc Desktop - run straight from source (no .exe build)
REM
REM  Double-click to install dependencies into .venv and launch Arc.
REM  Use this to try the app before building, or to develop on it.
REM
REM  arc-run.bat      same thing, this file's short name
REM ============================================================================
setlocal EnableDelayedExpansion
cd /d "%~dp0"

where python >nul 2>nul
if errorlevel 1 (
    echo  [FAIL] Python was not found on PATH.
    echo         Install from https://www.python.org/downloads/windows/
    echo         and tick "Add python.exe to PATH".
    pause
    exit /b 1
)

if not exist ".venv\Scripts\python.exe" (
    echo  Creating .venv ...
    python -m venv .venv
    if errorlevel 1 ( echo  [FAIL] venv creation failed & pause & exit /b 1 )
)
set PY=.venv\Scripts\python.exe

REM Only install when the marker is missing, so relaunching is instant.
if not exist ".venv\.arc-deps-ok" (
    echo  Installing dependencies (first run only) ...
    "%PY%" -m pip install --upgrade pip >nul
    "%PY%" -m pip install -r requirements.txt
    if errorlevel 1 ( echo  [FAIL] dependency install failed & pause & exit /b 1 )
    echo ok > ".venv\.arc-deps-ok"
)

echo  Starting Arc ...
"%PY%" -m arc_desktop %*
if errorlevel 1 (
    echo.
    echo  Arc exited with an error. If the window did not appear, run this
    echo  file from a terminal to read the message.
    pause
)
endlocal
exit /b 0
