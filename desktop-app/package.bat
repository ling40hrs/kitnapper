@echo off
rem KitNapper Windows packaging: a portable app-image with its own runtime.
rem
rem   package.bat    compile, then create build\dist\KitNapper\
rem
rem The output is a folder, not an installer: KitNapper.exe + app\kitnapper.jar
rem + runtime\. It needs no JDK, no Ant and no PATH setup on the target PC.
rem The app keeps kitnapper-data.json and kitnapper-images\ in its working
rem directory, so copy the folder somewhere writable (Desktop, Documents, a USB
rem stick) - never under Program Files.
rem
rem jpackage only builds for the host OS, so this script is Windows-only. The
rem bundled runtime is the JDK that runs it (17 or newer, full JDK - not a JRE).

setlocal
cd /d "%~dp0"

where jpackage >nul 2>nul
if errorlevel 1 (
    echo ERROR: jpackage was not found. Use a full JDK 17 or newer, not a JRE.
    exit /b 1
)

call "%~dp0build.bat"
if errorlevel 1 exit /b 1

if exist "build\package" rmdir /s /q "build\package"
mkdir "build\package"
rem The jar carries the classes plus resources\ (fonts and seed photos): both
rem view\Fonts and service\SeedPhotos try /fonts and /photos on the classpath
rem before the resources\ folder on disk.
jar --create --file "build\package\kitnapper.jar" --main-class kitnapper.Main ^
    -C "build\classes" . -C "resources" .
if errorlevel 1 exit /b 1

if exist "build\dist\KitNapper" rmdir /s /q "build\dist\KitNapper"
jpackage --type app-image --name KitNapper --app-version 1.0 ^
    --vendor YAPPERS --description "Equipment lending tracker" ^
    --input "build\package" --main-jar kitnapper.jar --main-class kitnapper.Main ^
    --add-modules java.desktop --dest "build\dist"
if errorlevel 1 exit /b 1

echo.
echo PACKAGE OK -^> build\dist\KitNapper\KitNapper.exe
echo Copy that whole folder to the demo PC, then double-click the exe.
exit /b 0
