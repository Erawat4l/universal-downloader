@echo off
setlocal

call "%~dp0build-android.bat" %*
exit /b %errorlevel%
