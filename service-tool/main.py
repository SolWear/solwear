#!/usr/bin/env python3
"""
SolWear Service Tool — desktop debugger and controller for SolWear prototypes.

Tabs:
  - Console: live serial log with color-coded prefixes ([HAL] [CORE] [BATT] [STATUS])
  - Control: quick watch actions (apps, brightness, diagnostics, power)
  - Status:  live device dashboard parsed from [STATUS] heartbeat lines
  - Settings: read/edit settings.json (currently a local-file workflow)
  - Firmware: prototype-aware flashing helpers (UF2 or serial workflow)

Run with:  python main.py
Or build a one-file exe:  build.bat
"""
import tkinter as tk
from ui.app_window import AppWindow


def main():
    root = tk.Tk()
    AppWindow(root)
    root.mainloop()


if __name__ == "__main__":
    main()
