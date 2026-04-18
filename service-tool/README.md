# SolWear Service Tool

> Desktop debugger and developer console for [**SolWearOS**](https://github.com/SolWear/solwear_os) firmware running on the Waveshare RP2040-Touch-LCD-1.69 smartwatch.

[![Python](https://img.shields.io/badge/python-3.9%2B-blue)]()
[![GUI](https://img.shields.io/badge/gui-tkinter-green)]()
[![Platform](https://img.shields.io/badge/platform-Windows-blueviolet)]()
[![License](https://img.shields.io/badge/license-private-lightgrey)]()

---

## Features

- **Live console** — color-coded serial log (`[HAL] [CORE] [BATT] [STATUS] [BOOT] [APP]`), command input, save log, auto-trim
- **Watch control tab** — one-click app navigation, brightness, diagnostics, buzzer, reboot/power commands
- **Status dashboard** — battery %, voltage, charging, free heap, steps, uptime, connection, heartbeat freshness
- **Prototype-aware detection** — auto-detects prototype/model from firmware heartbeat (`proto`, `mcu`, `display`, `caps`)
- **Settings editor** — local JSON override file (`device_settings.json`) with bool/int fields, save/reset/export
- **Push settings to watch** — sends brightness/sound/watchface/wallpaper/step-goal commands to the connected device
- **Firmware flash** — auto-detects mounted `RPI-RP2` BOOTSEL drive, drag-or-pick `.uf2`, one-click flash
- **Zero extra deps** — pure Python + Tkinter + `pyserial`. Bundles into a single Windows `.exe` with PyInstaller
- **Dark theme** matching SolWearOS UI (Solana purple + cyan accents)

## Screenshots

```
┌──────────────────────────────────────────────────────────┐
│  SolWear Service Tool       Port: COM4 ▾  ↻  [Connect]  │
├──────────────────────────────────────────────────────────┤
│  [Console] [Status] [Settings] [Firmware]                │
│                                                          │
│  10:23:01 [BOOT] Starting...                             │
│  10:23:01 [HAL] Display: full-frame sprite OK            │
│  10:23:01 [HAL] Touch...                                 │
│  10:23:01 [HAL] IMU...                                   │
│  10:23:01 [HAL] NFC: lazy mode (off until needed)        │
│  10:23:02 [CORE] Settings loaded                         │
│  10:23:02 === Boot complete ===                          │
│  10:23:07 [STATUS] batt=87 volt=4.05 heap=180432 ...     │
│                                                          │
│  > help                                                  │
└──────────────────────────────────────────────────────────┘
```

## Quick Start

### Run from source (development)

```bash
pip install -r requirements.txt
python main.py
```

Or just double-click `run.bat` on Windows — it'll use `.venv` if present.

### Build the standalone `.exe`

```bash
build.bat
```

This creates `.venv`, installs `pyserial` + `pyinstaller`, and bundles everything into:

```
dist\SolWearServiceTool.exe
```

The exe is self-contained — copy it anywhere and run.

## How It Works

1. **Connect** — pick the watch's USB COM port and click *Connect*. The reader thread opens the port at 115200 baud.
2. **Console** streams every line from the firmware in real time. Lines are categorized by their `[PREFIX]` and color-coded.
3. **Status** parses the firmware's `[STATUS]` heartbeat (emitted every 5 s) into a live device dashboard. Stale readings turn the heartbeat indicator orange/red.
4. **Control** exposes common watch actions through buttons (with direct command fallback in Console).
5. **Settings** edits a local JSON file and can push those values to the connected watch.
6. **Firmware** is prototype-aware: RP2040 boards use UF2 copy flow, while non-UF2 boards are marked as serial flashing workflows.

### Heartbeat format

```
[STATUS] batt=<pct> volt=<volts> heap=<bytes> steps=<n> uptime=<sec> charging=<0|1> temp=<C> proto=<id> mcu=<id> display=<id> caps=<csv>
```

## Project Layout

```
SolWear_ServiceTool/
├── main.py                 # Entry point — creates Tk root and AppWindow
├── core/
│   ├── device_state.py     # Thread-safe shared state
│   └── serial_reader.py    # Background reader + [STATUS] parser
├── ui/
│   ├── theme.py            # Colors, fonts (matches SolWearOS)
│   ├── app_window.py       # Notebook + topbar + statusbar
│   ├── tab_console.py      # Live log + command input
│   ├── tab_control.py      # One-click watch controls
│   ├── tab_status.py       # Live device dashboard
│   ├── tab_settings.py     # JSON settings editor
│   └── tab_firmware.py     # UF2 flasher
├── requirements.txt        # pyserial
├── build.bat               # Windows .exe builder
├── run.bat                 # Quick dev launcher
└── README.md
```

## Requirements

- **Python 3.9+** (Windows recommended; the firmware tab uses Windows drive-letter detection)
- **pyserial** (`pip install pyserial`)
- **pyinstaller** *(only for `build.bat`)*

## Companion Firmware

This tool is built for [**SolWearOS**](https://github.com/SolWear/solwear_os) — the smartwatch firmware that emits the structured logs and `[STATUS]` heartbeat this tool consumes. Both projects evolve together.

## Roadmap

- [ ] BLE pairing flow (when firmware supports it)
- [ ] Solana address QR display
- [ ] Wallet seed-phrase setup wizard
- [ ] Cross-platform `.app` build for macOS
- [ ] Plot battery curve over time
- [ ] Export device snapshot for bug reports

## License

Private — © SolWear. All rights reserved.
