@echo off
REM Quick dev run — uses venv if present, otherwise system python.
setlocal
cd /d "%~dp0"

if exist .venv\Scripts\python.exe (
    .venv\Scripts\python.exe main.py
) else (
    python3 main.py
)
