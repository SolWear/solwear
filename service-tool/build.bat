@echo off
REM ============================================================
REM SolWear Service Tool — Windows .exe build script
REM
REM Requires Python 3.9+ on PATH. Installs deps into a venv,
REM then bundles a one-file exe into dist\SolWearServiceTool.exe
REM ============================================================

setlocal
cd /d "%~dp0"

echo.
echo === SolWear Service Tool — build ===
echo.

if not exist .venv (
    echo [1/4] Creating venv...
    python3 -m venv .venv
    if errorlevel 1 goto :err
)

echo [2/4] Installing dependencies...
call .venv\Scripts\activate.bat
python3 -m pip install --upgrade pip >nul
python3 -m pip install -r requirements.txt
python3 -m pip install pyinstaller
if errorlevel 1 goto :err

echo [3/4] Cleaning previous build...
if exist build rmdir /s /q build
if exist dist  rmdir /s /q dist
if exist SolWearServiceTool.spec del SolWearServiceTool.spec

echo [4/4] Building exe with PyInstaller...
python3 -m PyInstaller ^
    --noconfirm ^
    --onefile ^
    --windowed ^
    --name SolWearServiceTool ^
    --add-data "ui;ui" ^
    --add-data "core;core" ^
    main.py
if errorlevel 1 goto :err

echo.
echo === Build complete ===
echo Output: dist\SolWearServiceTool.exe
echo.
goto :eof

:err
echo.
echo *** Build FAILED ***
exit /b 1
