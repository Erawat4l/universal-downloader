@echo off
setlocal

where py >nul 2>&1
if %errorlevel%==0 (
    py -3 "%~dp0tools\build_android.py" %*
    exit /b %errorlevel%
)

where python >nul 2>&1
if %errorlevel%==0 (
    python "%~dp0tools\build_android.py" %*
    exit /b %errorlevel%
)

echo Python 3 was not found. Install Python 3 or add it to PATH.
exit /b 1
