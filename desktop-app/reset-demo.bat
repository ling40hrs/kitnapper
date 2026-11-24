@echo off
rem Restores the SRS 6.1 demo baseline: deletes the save file and the photo
rem folder, so the next launch re-seeds relative to today's date
rem (7 AVAILABLE / 3 LOANED, loan L-003 four days overdue at 200.00).
rem
rem Close KitNapper first - a running app rewrites the save file on exit.

setlocal
cd /d "%~dp0"

echo This deletes kitnapper-data.json and kitnapper-images\ in:
echo   %CD%
echo.
set /p answer="Reset demo data? [y/N] "
if /i not "%answer%"=="y" (
    echo Cancelled.
    exit /b 0
)

if exist "kitnapper-data.json" del /q "kitnapper-data.json"
if exist "kitnapper-data.json.tmp" del /q "kitnapper-data.json.tmp"
if exist "kitnapper-images" rmdir /s /q "kitnapper-images"
echo Done. Start KitNapper to re-seed the demo data.
exit /b 0
