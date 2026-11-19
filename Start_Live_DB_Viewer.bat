@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
echo ============================================================
echo   FirstStep Live SQLite Dashboard (PC ^<--^> Phone USB)
echo ============================================================

set "PY_CMD="

:: 1. Check py launcher first (avoiding Microsoft Store python.exe alias)
where py >nul 2>&1
if %errorlevel%==0 (
    set "PY_CMD=py -3"
    goto :run_server
)

:: 2. Check python on PATH only if it is NOT the Microsoft Store WindowsApps stub
for /f "delims=" %%P in ('where python 2^>nul') do (
    echo "%%P" | findstr /i "WindowsApps" >nul
    if errorlevel 1 (
        set "PY_CMD="%%P""
        goto :run_server
    )
)

:: 3. Check standard Windows Python installation directories
for %%D in (
    "%LocalAppData%\Programs\Python\Python313\python.exe"
    "%LocalAppData%\Programs\Python\Python312\python.exe"
    "%LocalAppData%\Programs\Python\Python311\python.exe"
    "%LocalAppData%\Programs\Python\Python310\python.exe"
    "C:\Python313\python.exe"
    "C:\Python312\python.exe"
    "C:\Python311\python.exe"
    "C:\Program Files\Python313\python.exe"
    "C:\Program Files\Python312\python.exe"
    "C:\Program Files\Python311\python.exe"
) do (
    if exist %%D (
        set "PY_CMD=%%D"
        goto :run_server
    )
)

:: 4. Python is not installed yet
echo.
echo [!] Python 3 is not installed on this Windows PC.
echo.
where winget >nul 2>&1
if %errorlevel%==0 (
    set /p INSTALL_PY="Install Python 3.12 automatically via winget now? (Y/N): "
    if /i "!INSTALL_PY!"=="Y" (
        winget install -e --id Python.Python.3.12 --accept-package-agreements --accept-source-agreements
        echo.
        echo Please close this window and double-click Start_Live_DB_Viewer.bat again.
        pause
        exit /b 0
    )
)

echo Manual install options:
echo   1) Run in PowerShell/CMD:  winget install Python.Python.3.12
echo   2) Or download Python from https://www.python.org/downloads/
echo      (IMPORTANT: check "Add python.exe to PATH" during install)
echo.
echo Tip: You can also inspect/edit the live DB directly inside Android Studio:
echo      View ^> Tool Windows ^> App Inspection ^> Database Inspector
pause
exit /b 1

:run_server
echo Using Python: %PY_CMD%
%PY_CMD% tools\live_db_dashboard.py
if errorlevel 1 (
    echo.
    echo [ERROR] Dashboard exited with an error.
    pause
)
