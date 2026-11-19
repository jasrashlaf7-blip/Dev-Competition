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
    echo Decoding debug.keystore.base64 into debug.keystore...
    certutil -decode "debug.keystore.base64" "debug.keystore" >nul 2>&1
    if exist "debug.keystore" (
        echo [SUCCESS] debug.keystore generated from debug.keystore.base64!
        echo You can now click Run / Build APK in Android Studio.
        pause
        exit /b 0
    )
)

echo Fallback: generating standard Android debug.keystore via keytool...
keytool -genkeypair -v -keystore "debug.keystore" -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
if exist "debug.keystore" (
    echo [SUCCESS] debug.keystore created!
) else (
    echo [ERROR] Could not create debug.keystore. Run this from Command Prompt with JDK keytool on PATH.
)
pause
