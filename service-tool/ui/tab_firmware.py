"""Firmware tab — flash a .uf2 to a mounted RPI-RP2 drive.

The RP2040 in BOOTSEL mode mounts as a removable drive named "RPI-RP2".
Copying a .uf2 onto it triggers an immediate firmware update.
"""
import os
import shutil
import string
import threading
import time
import tkinter as tk
from tkinter import filedialog
import subprocess
import sys

from ui import theme


def find_rpi_rp2_drives():
    """Return a list of drive letters mounted as RPI-RP2 (Windows)."""
    drives = []
    for letter in string.ascii_uppercase:
        root = f"{letter}:/"
        if not os.path.exists(root):
            continue
        # Quick heuristic: RPI-RP2 has INFO_UF2.TXT at the root
        info = os.path.join(root, "INFO_UF2.TXT")
        if os.path.isfile(info):
            drives.append(root)
    return drives


class FirmwareTab(tk.Frame):
    def __init__(self, parent, serial_reader, state=None):
        super().__init__(parent, bg=theme.BG_PRIMARY)
        self.serial = serial_reader
        self.state = state
        self.uf2_path = None
        self.flash_method = "uf2"
        self._build()
        self._refresh_drives()

    def _ui(self, fn):
        # Tkinter widgets must only be touched on the UI thread.
        self.after(0, fn)

    def _build(self):
        outer = tk.Frame(self, bg=theme.BG_PRIMARY, padx=20, pady=20)
        outer.pack(fill=tk.BOTH, expand=True)

        tk.Label(
            outer, text="Firmware Flash",
            bg=theme.BG_PRIMARY, fg=theme.ACCENT_SOL,
            font=theme.FONT_HEADER,
        ).pack(anchor="w")

        tk.Label(
            outer,
            text="1. Hold BOOTSEL on the watch and plug in USB.\n"
                 "2. Wait for RPI-RP2 to mount as a removable drive.\n"
                 "3. Pick a .uf2 file and click Flash.",
            bg=theme.BG_PRIMARY, fg=theme.TEXT_MUTED,
            font=theme.FONT_LABEL, justify=tk.LEFT,
        ).pack(anchor="w", pady=(8, 16))

        self.mode_label = tk.Label(
            outer,
            text="Mode: UF2 drag-and-drop (RP2040)",
            bg=theme.BG_PRIMARY,
            fg=theme.ACCENT_GREEN,
            font=theme.FONT_LABEL,
        )
        self.mode_label.pack(anchor="w", pady=(0, 8))

        # File picker
        file_card = tk.Frame(outer, bg=theme.BG_CARD, padx=14, pady=12)
        file_card.pack(fill=tk.X, pady=4)
        tk.Label(file_card, text="Firmware File (.uf2 or .bin):", bg=theme.BG_CARD,
                 fg=theme.TEXT_MUTED, font=theme.FONT_LABEL).pack(anchor="w")
        self.path_label = tk.Label(file_card, text="(none selected)",
                                   bg=theme.BG_CARD, fg=theme.TEXT_PRIMARY,
                                   font=theme.FONT_MONO, anchor="w")
        self.path_label.pack(fill=tk.X, pady=(2, 6))
        tk.Button(file_card, text="Browse...", command=self._pick_firmware,
                  bg=theme.ACCENT, fg=theme.BG_PRIMARY, bd=0, padx=12, pady=4,
                  activebackground=theme.ACCENT_GREEN).pack(anchor="w")

        # Port/Drive picker
        target_card = tk.Frame(outer, bg=theme.BG_CARD, padx=14, pady=12)
        target_card.pack(fill=tk.X, pady=8)
        tk.Label(target_card, text="Target (Drive / Serial Port):", bg=theme.BG_CARD,
                 fg=theme.TEXT_MUTED, font=theme.FONT_LABEL).pack(anchor="w")

        row = tk.Frame(target_card, bg=theme.BG_CARD)
        row.pack(fill=tk.X, pady=6)

        self.target_var = tk.StringVar()
        self.target_entry = tk.Entry(row, textvariable=self.target_var,
                                    bg=theme.BG_INPUT, fg=theme.ACCENT_GREEN,
                                    font=theme.FONT_MONO, relief=tk.FLAT)
        self.target_entry.pack(side=tk.LEFT, fill=tk.X, expand=True, ipady=4)

        tk.Button(row, text="↻ Rescan RP2 Drives", command=self._refresh_drives,
                  bg=theme.BG_SECONDARY, fg=theme.ACCENT, bd=0, padx=12,
                  activebackground=theme.ACCENT).pack(side=tk.RIGHT, padx=4)

        tk.Button(row, text="↻ Get Serial Port", command=self._refresh_serial,
                  bg=theme.BG_SECONDARY, fg=theme.ACCENT, bd=0, padx=12,
                  activebackground=theme.ACCENT).pack(side=tk.RIGHT, padx=4)

        # Action buttons
        action_row = tk.Frame(outer, bg=theme.BG_PRIMARY)
        action_row.pack(fill=tk.X, pady=14)

        tk.Button(action_row, text="Reboot to BOOTSEL",
                  command=self._reboot_bootsel,
                  bg=theme.WARNING, fg=theme.BG_PRIMARY, bd=0, padx=14, pady=6,
                  activebackground=theme.DANGER).pack(side=tk.LEFT, padx=4)

        self.flash_btn = tk.Button(action_row, text="Flash Firmware",
                                   command=self._flash,
                                   bg=theme.ACCENT_GREEN, fg=theme.BG_PRIMARY, bd=0,
                                   padx=18, pady=6, font=theme.FONT_LABEL,
                                   activebackground=theme.ACCENT)
        self.flash_btn.pack(side=tk.LEFT, padx=4)

        # Progress
        self.progress_label = tk.Label(outer, text="", bg=theme.BG_PRIMARY,
                                       fg=theme.TEXT_MUTED, font=theme.FONT_LABEL)
        self.progress_label.pack(anchor="w", pady=10)

    def _pick_firmware(self):
        path = filedialog.askopenfilename(
            filetypes=[("Firmware files", "*.uf2 *.bin"), ("UF2 files", "*.uf2"), ("BIN files", "*.bin"), ("All files", "*.*")],
        )
        if path:
            self.firmware_path = path
            self.path_label.config(text=path)

    def _refresh_drives(self):
        drives = find_rpi_rp2_drives()
        if drives:
            self.target_var.set(drives[0])
            self.target_entry.config(fg=theme.ACCENT_GREEN)
        else:
            self.target_var.set("No RPI-RP2 drive detected")
            self.target_entry.config(fg=theme.TEXT_MUTED)

    def _refresh_serial(self):
        port = self.serial.port if self.serial else None
        if port:
            self.target_var.set(port)
            self.target_entry.config(fg=theme.ACCENT_GREEN)
        else:
            self.target_var.set("COMx (Enter port manually)")
            self.target_entry.config(fg=theme.TEXT_MUTED)

    def _reboot_bootsel(self):
        # Try sending a special serial command — firmware can act on it,
        # otherwise the user does it manually.
        if self.serial.write("reboot bootsel\n"):
            self.progress_label.config(text="Sent reboot bootsel command",
                                       fg=theme.ACCENT)
        else:
            self.progress_label.config(
                text="Not connected — hold BOOTSEL and replug USB manually",
                fg=theme.WARNING,
            )

    def _flash(self):
        if self.flash_method == "serial":
            self._flash_serial()
        else:
            self._flash_uf2()

    def _flash_uf2(self):
        target_dir = self.target_var.get().strip()
        if not target_dir or not os.path.exists(target_dir):
            self.progress_label.config(text="Invalid RPI-RP2 drive selected", fg=theme.DANGER)
            return
        if not hasattr(self, 'firmware_path') or not self.firmware_path or not os.path.isfile(self.firmware_path):
            self.progress_label.config(text="Pick a .uf2 file first", fg=theme.DANGER)
            return

        target_file = os.path.join(target_dir, os.path.basename(self.firmware_path))
        self.flash_btn.config(state=tk.DISABLED)
        self.progress_label.config(text=f"Flashing UF2 → {target_file}", fg=theme.ACCENT)

        def worker():
            try:
                shutil.copy2(self.firmware_path, target_file)
                self._ui(lambda: self.progress_label.config(
                    text="Flash complete — device rebooting",
                    fg=theme.ACCENT_GREEN,
                ))
            except Exception as e:
                if os.path.getsize(self.firmware_path) > 0:
                    self._ui(lambda: self.progress_label.config(
                        text=f"Flash sent (drive unmounted): {e}",
                        fg=theme.ACCENT_GREEN,
                    ))
                else:
                    self._ui(lambda: self.progress_label.config(
                        text=f"Flash failed: {e}", fg=theme.DANGER,
                    ))
            finally:
                time.sleep(0.5)
                self._ui(lambda: self.flash_btn.config(state=tk.NORMAL))
                self._ui(self._refresh_drives)

        threading.Thread(target=worker, daemon=True).start()

    def _flash_serial(self):
        port = self.target_var.get().strip()
        if not port or port.startswith("No") or port.startswith("COMx"):
            self.progress_label.config(text="Specify a valid COM port", fg=theme.DANGER)
            return
        if not hasattr(self, 'firmware_path') or not self.firmware_path or not os.path.isfile(self.firmware_path):
            self.progress_label.config(text="Pick a .bin file first", fg=theme.DANGER)
            return

        self.flash_btn.config(state=tk.DISABLED)
        self.progress_label.config(text=f"Flashing Serial → {port}...", fg=theme.ACCENT)

        was_connected = self.serial.is_running() if self.serial else False
        if was_connected:
            self.serial.disconnect()

        def worker():
            try:
                chip = "esp32s3"
                if self.state is not None:
                    snap = self.state.snapshot()
                    mcu = (snap.get("mcu") or "").lower()
                    if "esp32s3" in mcu:
                        chip = "esp32s3"
                    elif "esp32" in mcu:
                        chip = "esp32"
                cmd = [
                    sys.executable, "-m", "esptool",
                    "--chip", chip,
                    "--port", port,
                    "--baud", "921600",
                    "--before", "default_reset",
                    "--after", "hard_reset",
                    "write_flash", "-z",
                    "--flash_mode", "dio",
                    "--flash_freq", "80m",
                    "0x10000", self.firmware_path,
                ]
                self._ui(lambda: self.progress_label.config(
                    text=f"esptool {chip} → {port} @ 921600…", fg=theme.ACCENT))
                proc = subprocess.Popen(
                    cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                    text=True, bufsize=1,
                )
                last = ""
                for line in proc.stdout:
                    line = line.rstrip()
                    if not line:
                        continue
                    last = line
                    if "%" in line or line.startswith("Writing") or line.startswith("Wrote"):
                        self._ui(lambda t=line: self.progress_label.config(text=t))
                proc.wait()
                if proc.returncode == 0:
                    self._ui(lambda: self.progress_label.config(
                        text="Flash complete — device rebooting",
                        fg=theme.ACCENT_GREEN,
                    ))
                else:
                    self._ui(lambda t=last: self.progress_label.config(
                        text=f"Flash failed: {t}", fg=theme.DANGER,
                    ))
            except Exception as e:
                self._ui(lambda: self.progress_label.config(
                    text=f"Flash error: {e}", fg=theme.DANGER,
                ))
            finally:
                self._ui(lambda: self.flash_btn.config(state=tk.NORMAL))
                if was_connected:
                    time.sleep(1.0)
                    try:
                        self.serial.connect(port)
                    except Exception:
                        pass

        threading.Thread(target=worker, daemon=True).start()

    def refresh(self, snap):
        method = snap.get("flash_method", "unknown")
        if method == "uf2":
            self.flash_method = "uf2"
            self.mode_label.config(
                text="Mode: UF2 drag-and-drop (RP2040)",
                fg=theme.ACCENT_GREEN,
            )
            self.flash_btn.config(state=tk.NORMAL)
        elif method == "serial":
            self.flash_method = "serial"
            self.mode_label.config(
                text="Mode: Serial flashing (ESP32 / non-UF2)",
                fg=theme.WARNING,
            )
        else:
            self.flash_method = "unknown"
            self.mode_label.config(
                text="Mode: Unknown flash method",
                fg=theme.TEXT_MUTED,
            )
