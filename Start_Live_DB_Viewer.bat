@echo off
cd /d "%~dp0"
echo Starting FirstStep Live SQLite Dashboard...
python tools\live_db_dashboard.py
if errorlevel 1 (
    py -3 tools\live_db_dashboard.py
)
pause
