@echo off
setlocal

if exist "%~dp0android-app\build.bat" (
    call "%~dp0android-app\build.bat" %*
    exit /b %errorlevel%
)

echo Android project not found. Run this file from the repository root.
exit /b 1
