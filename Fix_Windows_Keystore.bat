@echo off
cd /d "%~dp0"
echo ============================================================
echo   FIRSTSTEP - Fix Missing debug.keystore on Windows
echo ============================================================

if exist "debug.keystore" (
    echo [OK] debug.keystore already exists.
    pause
    exit /b 0
)

if exist "debug.keystore.base64" (
    powershell -NoProfile -Command "[IO.File]::WriteAllBytes('debug.keystore', [Convert]::FromBase64String((Get-Content 'debug.keystore.base64' -Raw).Trim()))" >nul 2>&1
    if exist "debug.keystore" (
        echo [SUCCESS] debug.keystore decoded from debug.keystore.base64!
        pause
        exit /b 0
    )
)

if exist "%USERPROFILE%\.android\debug.keystore" (
    copy /Y "%USERPROFILE%\.android\debug.keystore" "debug.keystore" >nul
    if exist "debug.keystore" (
        echo [SUCCESS] Copied standard Android debug.keystore from %USERPROFILE%\.android\debug.keystore!
        pause
        exit /b 0
    )
)

set "KEYTOOL_EXE=keytool"
where keytool >nul 2>&1
if errorlevel 1 (
    if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\keytool.exe" (
        set "KEYTOOL_EXE=%ProgramFiles%\Android\Android Studio\jbr\bin\keytool.exe"
    ) else if exist "%LocalAppData%\Programs\Android Studio\jbr\bin\keytool.exe" (
        set "KEYTOOL_EXE=%LocalAppData%\Programs\Android Studio\jbr\bin\keytool.exe"
    )
)

echo Generating standard Android debug.keystore via keytool...
"%KEYTOOL_EXE%" -genkeypair -v -keystore "debug.keystore" -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
if exist "debug.keystore" (
    echo [SUCCESS] debug.keystore created! You can now click Run / Build APK in Android Studio.
) else (
    echo [ERROR] Could not locate keytool.exe. Create C:\Users\Dell\Desktop\FIRSTSTEP\debug.keystore using Android Studio JDK keytool.
)
pause
