@echo off
rem KitNapper launcher for Windows: builds, then opens the login screen.
rem
rem Double-click this file, or run it from a prompt. The save file
rem (kitnapper-data.json) and photos (kitnapper-images\) are created in this
rem folder, so the demo state lives next to the app, not in the user profile.

setlocal
cd /d "%~dp0"

call "%~dp0build.bat"
if errorlevel 1 (
    echo.
    echo Build failed - KitNapper was not started.
    pause
    exit /b 1
)

java -cp "build\classes" kitnapper.Main
if errorlevel 1 pause
exit /b 0
