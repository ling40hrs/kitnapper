@echo off
rem KitNapper build helper for Windows - needs only the JDK (17+), no Ant.
rem
rem   build.bat    compile every source file into build\classes
rem
rem NetBeans still owns the canonical Ant build (build.xml + nbproject\);
rem this script mirrors build.sh so a Windows machine without Ant can compile.
rem Source paths are written with forward slashes: javac treats "\" inside an
rem @argfile as an escape, and the list must survive folders with spaces.

setlocal enabledelayedexpansion
cd /d "%~dp0"

where javac >nul 2>nul
if errorlevel 1 (
    echo ERROR: javac was not found on PATH. Install JDK 17 or add it to PATH.
    exit /b 1
)

if not exist "src" (
    echo ERROR: run this from the desktop-app folder - no src\ directory found.
    exit /b 1
)

if not exist "build\classes" mkdir "build\classes"
( for /r "src" %%f in (*.java) do (
    set "source=%%f"
    echo "!source:\=/!"
) ) > "build\sources.txt"

javac -Xlint:all -encoding UTF-8 -d "build\classes" "@build\sources.txt"
if errorlevel 1 exit /b 1

echo BUILD OK -^> build\classes
exit /b 0
