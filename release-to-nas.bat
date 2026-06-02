@echo off
REM ---------------------------------------------------------------------------
REM release-to-nas.bat
REM
REM End-to-end Android release publish for MediaPlayer:
REM   1. Build a signed release APK via gradlew assembleRelease.
REM   2. SMB copy the APK to the NAS app-update share.
REM   3. (optional) copy a sidecar release-notes .txt next to the APK.
REM   4. Prune older APKs on the share — keep only the latest versionCode.
REM   5. Post-flight verification — confirm the APK landed on the NAS.
REM
REM No SSH / container restart needed. The share
REM   \\NAS70E9E0\Public\mediaplayer\android
REM is bind-mounted into the backend container at
REM   /var/lib/mediaplayer/updates   (mediaplayer.yml)
REM and AppUpdateScanner re-scans it every 30s on the next /updates poll,
REM picking the highest-versionCode `mediaplayer-<code>-<name>.apk`. So
REM dropping the file in is the whole deploy — clients see the update within
REM ~30s with no backend redeploy.
REM
REM PREREQUISITES
REM   * local.properties must carry all four keystore.* keys, else gradle
REM     emits an UNSIGNED release APK (Android refuses to install it).
REM     keystore.file / keystore.password / keystore.alias / keystore.key.password
REM   * local.properties should set base.url.release to the prod backend
REM     (release has no localhost fallback). See local.properties.example.
REM   * The SMB share must be reachable in Explorer (credentials cached in
REM     Windows Credential Manager from the backend deploy).
REM
REM Run from cmd:  release-to-nas.bat
REM       or double-click in Explorer (self-relaunches under cmd /k so the
REM       window stays open on success or failure).
REM ---------------------------------------------------------------------------

REM --- Self-relaunch trampoline: keep the window open when double-clicked. ---
if not "%MP_RELEASE_INNER%"=="1" (
    set "MP_RELEASE_INNER=1"
    cmd /k ""%~f0" %*"
    exit /b
)

REM Plain setlocal (no DelayedExpansion) — env-var scoping only. The whole
REM script uses %VAR% / `if errorlevel N` and never relies on !VAR!, so it
REM behaves identically whether or not DelayedExpansion happens to be active in
REM the parent shell (the cause of an earlier false "[FAIL] ... !RC!" on re-run).
setlocal

call :main %*
set "FINAL_RC=%ERRORLEVEL%"
echo.
if "%FINAL_RC%"=="0" (
    echo *** Release OK ***
) else (
    echo *** Release FAILED with exit code %FINAL_RC% ***
)
echo.
echo Window stays open. Type `exit` or close it when done reading.
endlocal ^& exit /b %FINAL_RC%

:main
set "SCRIPT_DIR=%~dp0"
set "ANDROID_DIR=%SCRIPT_DIR%"

REM === Configuration =========================================================
REM Android Studio's bundled JBR 21. The system JDK is 25, but this module
REM pins jvmToolchain(21)/sourceCompatibility 21 and AGP wants JDK 21 — point
REM JAVA_HOME at the JBR so the build resolves the right toolchain. Edit if
REM Android Studio is installed elsewhere (JetBrains Toolbox, custom path).
set "JBR_HOME=C:\Program Files\Android\Android Studio\jbr"

REM SMB share that bind-mounts to the backend's app-update dir (mediaplayer.yml:
REM /share/CACHEDEV1_DATA/Public/mediaplayer/android -> /var/lib/mediaplayer/updates).
set "NAS_UNC=\\NAS70E9E0\Public\mediaplayer\android"

set "APK_OUT_DIR=%ANDROID_DIR%app\build\outputs\apk\release"
REM ===========================================================================

echo.
echo === [0/5] pre-flight =====================================================
if not exist "%JBR_HOME%\bin\java.exe" (
    echo [FAIL] Android Studio JBR not found at "%JBR_HOME%"
    echo        Edit JBR_HOME at the top of this script if installed elsewhere.
    exit /b 1
)
set "JAVA_HOME=%JBR_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo   JAVA_HOME = %JAVA_HOME%

if not exist "%NAS_UNC%" (
    echo [FAIL] NAS app-update share unreachable: %NAS_UNC%
    echo        Open it in Explorer and re-cache credentials, then retry:
    echo        explorer "%NAS_UNC%"
    exit /b 1
)
echo   NAS share OK: %NAS_UNC%

echo.
echo === [1/5] gradlew assembleRelease ========================================
REM `call` (not cmd /c) keeps the same shell so errorlevel propagates cleanly.
REM Absolute path: NoDefaultCurrentDirectoryInExePath can make bare gradlew.bat
REM unresolvable even when cd'd into the module dir.
pushd "%ANDROID_DIR%"
call "%ANDROID_DIR%gradlew.bat" assembleRelease
REM Check errorlevel BEFORE popd — popd clobbers ERRORLEVEL. `if errorlevel 1`
REM is true for any non-zero exit and needs no delayed expansion (robust even
REM when this script is re-run in a shell where DelayedExpansion isn't active).
if errorlevel 1 (
    popd
    echo [FAIL] gradlew assembleRelease failed. Aborting.
    exit /b 1
)
popd

REM Locate the freshly built APK (mediaplayer-<code>-<name>.apk).
set "APK_NAME="
for /f "delims=" %%F in ('dir /b /a-d "%APK_OUT_DIR%\mediaplayer-*.apk" 2^>nul') do set "APK_NAME=%%F"
if "%APK_NAME%"=="" (
    echo [FAIL] No mediaplayer-*.apk found in %APK_OUT_DIR%
    exit /b 1
)
echo   Built: %APK_NAME%

echo.
echo === [1b/5] force v1+v2+v3 signing =========================================
REM AGP 9.x silently drops v1 (JAR) signing for minSdk^>=24, leaving a v2-only
REM APK that some OEM installers reject ("scaricamento interrotto"). Re-sign the
REM built APK to add v1 back. force-v1-sign.ps1 reads the same keystore.* keys
REM from local.properties that the gradle build used.
powershell -NoProfile -ExecutionPolicy Bypass -File "%ANDROID_DIR%force-v1-sign.ps1" "%APK_OUT_DIR%\%APK_NAME%"
if errorlevel 1 (
    echo [FAIL] v1 re-sign failed. Aborting before publish.
    exit /b 1
)

echo.
echo === [2/5] SMB copy APK -^> %NAS_UNC% ======================================
REM /Z restartable, /J unbuffered (fast on LAN), single-file arg scopes the
REM copy to just this APK. robocopy exit codes are a bitmask: 0/1 OK, >=8 error.
robocopy "%APK_OUT_DIR%" "%NAS_UNC%" "%APK_NAME%" /Z /J /R:2 /W:5
REM robocopy exit codes are a bitmask: 0/1 = OK, 2/3 = warnings, >=8 = error.
REM Check FIRST — any following command (even echo) resets ERRORLEVEL. No
REM delayed expansion needed.
if errorlevel 8 (
    echo [FAIL] APK SMB copy failed (robocopy ^>=8). Verify the share is reachable:
    echo        explorer "%NAS_UNC%"
    exit /b 1
)
echo   APK copied.

echo.
echo === [3/5] sidecar release notes (optional) ===============================
REM AppUpdateScanner reads `mediaplayer-<code>-<name>.txt` next to the APK and
REM exposes it as releaseNotes in the update manifest. If a matching .txt sits
REM in the release output dir, ship it too; otherwise skip silently.
set "NOTES_NAME=%APK_NAME:.apk=.txt%"
if exist "%APK_OUT_DIR%\%NOTES_NAME%" (
    robocopy "%APK_OUT_DIR%" "%NAS_UNC%" "%NOTES_NAME%" /Z /J /R:2 /W:5 /NFL /NDL >nul
    echo   shipped release notes: %NOTES_NAME%
) else (
    echo   no sidecar notes %NOTES_NAME% - skipping
)

echo.
echo === [4/5] prune old APKs on share ========================================
REM Keep only the highest-versionCode APK (+ its .txt). Old ones are harmless —
REM the scanner ignores all but the newest — but they pile up on the share.
powershell -NoProfile -Command ^
  "$dir='%NAS_UNC%';" ^
  "$files=@(Get-ChildItem -Path $dir -Filter 'mediaplayer-*.apk' -ErrorAction SilentlyContinue);" ^
  "if($files.Count -le 1){ Write-Host ('  '+$files.Count+' APK on share, nothing to prune'); return };" ^
  "$code={ if($_.Name -match 'mediaplayer-(\d+)-'){[int]$Matches[1]}else{0} };" ^
  "$latest=$files | Sort-Object @{e=$code} -Descending | Select-Object -First 1;" ^
  "$keepCode=& { $n=$latest.Name; if($n -match 'mediaplayer-(\d+)-'){[int]$Matches[1]}else{0} };" ^
  "$files | Where-Object {$_.FullName -ne $latest.FullName} | ForEach-Object {" ^
  "  Remove-Item $_.FullName -Force; Write-Host ('  removed '+$_.Name);" ^
  "  $txt=[IO.Path]::ChangeExtension($_.FullName,'.txt'); if(Test-Path $txt){ Remove-Item $txt -Force; Write-Host ('  removed '+[IO.Path]::GetFileName($txt)) }" ^
  "};" ^
  "Write-Host ('  kept '+$latest.Name)"

echo.
echo === [5/5] post-flight verification =======================================
REM Read back what actually landed so a silent miscopy can't slip past.
powershell -NoProfile -Command ^
  "Write-Host '  APK on NAS:';" ^
  "Get-ChildItem '%NAS_UNC%' -Filter 'mediaplayer-*.apk' -ErrorAction SilentlyContinue | Sort-Object Name | ForEach-Object { Write-Host ('    '+$_.LastWriteTime.ToString('yyyy-MM-dd HH:mm')+'  '+('{0,12:N0}' -f $_.Length)+'  '+$_.Name) }"

echo.
echo === Release published =====================================================
echo APK on NAS:  %NAS_UNC%\%APK_NAME%
echo Backend re-scans within ~30s; clients see the update on next /updates poll.
exit /b 0
