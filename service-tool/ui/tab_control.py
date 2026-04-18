"""Watch Control tab - quick remote actions for connected prototypes."""

import tkinter as tk
import time
from datetime import datetime
from tkinter import messagebox

from ui import theme


APP_COMMANDS = [
    ("Watchface", "app watchface"),
    ("Home", "app home"),
    ("Settings", "app settings"),
    ("Wallet", "app wallet"),
    ("NFC", "app nfc"),
    ("Health", "app health"),
    ("Game", "app game"),
    ("Alarm", "app alarm"),
    ("Stats", "app stats"),
]

TOUCH_PRESETS = [
    ("Top Bar", "tap 100 12"),
    ("Wallet", "tap 56 71"),
    ("Health / Stats", "tap 144 71"),
    ("NFC / Alarm", "tap 56 153"),
    ("Settings", "tap 144 153"),
    ("Bottom Home", "tap 100 185"),
]


class ControlTab(tk.Frame):
    def __init__(self, parent, state, serial_reader):
        super().__init__(parent, bg=theme.BG_PRIMARY)
        self.state = state
        self.serial = serial_reader
        self._build()

    def _card(self, parent, title):
        wrap = tk.Frame(parent, bg=theme.BG_CARD, padx=14, pady=12)
        tk.Label(
            wrap,
            text=title,
            bg=theme.BG_CARD,
            fg=theme.TEXT_MUTED,
            font=theme.FONT_LABEL,
        ).pack(anchor="w")
        return wrap

    def _build(self):
        outer = tk.Frame(self, bg=theme.BG_PRIMARY, padx=16, pady=16)
        outer.pack(fill=tk.BOTH, expand=True)

        for i in range(2):
            outer.grid_columnconfigure(i, weight=1, uniform="col")
        for i in range(4):
            outer.grid_rowconfigure(i, weight=1, uniform="row")

        info = self._card(outer, "CONNECTED PROTOTYPE")
        info.grid(row=0, column=0, sticky="nsew", padx=6, pady=6)
        self.proto_label = tk.Label(
            info,
            text="Unknown prototype",
            bg=theme.BG_CARD,
            fg=theme.ACCENT_GREEN,
            font=theme.FONT_HEADER,
        )
        self.proto_label.pack(anchor="w", pady=(8, 0))
        self.proto_sub = tk.Label(
            info,
            text="mcu=unknown  display=unknown",
            bg=theme.BG_CARD,
            fg=theme.TEXT_MUTED,
            font=theme.FONT_LABEL,
        )
        self.proto_sub.pack(anchor="w")

        power = self._card(outer, "POWER / SYSTEM")
        power.grid(row=0, column=1, sticky="nsew", padx=6, pady=6)
        row = tk.Frame(power, bg=theme.BG_CARD)
        row.pack(anchor="w", pady=(8, 0))
        self._button(row, "Status now", "status now").pack(side=tk.LEFT, padx=(0, 6))
        self._button(row, "Reboot BOOTSEL", "reboot bootsel").pack(side=tk.LEFT, padx=(0, 6))
        self._button(row, "Power off", "power off", danger=True).pack(side=tk.LEFT)

        row2 = tk.Frame(power, bg=theme.BG_CARD)
        row2.pack(anchor="w", pady=(6, 0))
        self._button(row2, "Diag ON", "diag on").pack(side=tk.LEFT, padx=(0, 6))
        self._button(row2, "Diag OFF", "diag off").pack(side=tk.LEFT, padx=(0, 6))
        self._button(row2, "Back", "nav back").pack(side=tk.LEFT, padx=(0, 6))
        self._button(row2, "Home", "nav home").pack(side=tk.LEFT)

        row3 = tk.Frame(power, bg=theme.BG_CARD)
        row3.pack(anchor="w", pady=(6, 0))
        self._button(row3, "E-ink Test", "eink test").pack(side=tk.LEFT, padx=(0, 6))
        self._button(row3, "E-ink Clear", "eink clear").pack(side=tk.LEFT, padx=(0, 6))
        self._button(row3, "Display Probe", "display probe").pack(side=tk.LEFT)

        row4 = tk.Frame(power, bg=theme.BG_CARD)
        row4.pack(anchor="w", pady=(6, 0))
        self._button(row4, "GM Solana", "__gm_solana__").pack(side=tk.LEFT, padx=(0, 6))
        self._button(row4, "Clock Sync", "__clock_sync__").pack(side=tk.LEFT, padx=(0, 6))
        self._button(row4, "Clock 12H", "clock format 12").pack(side=tk.LEFT, padx=(0, 6))
        self._button(row4, "Clock 24H", "clock format 24").pack(side=tk.LEFT, padx=(0, 6))
        self._button(row4, "Face Next", "watchface next").pack(side=tk.LEFT, padx=(0, 6))
        self._button(row4, "Face 0", "set watchface 0").pack(side=tk.LEFT, padx=(0, 6))
        self._button(row4, "Face 1", "set watchface 1").pack(side=tk.LEFT, padx=(0, 6))
        self._button(row4, "Face 2", "set watchface 2").pack(side=tk.LEFT, padx=(0, 6))
        self._button(row4, "Face 3", "set watchface 3").pack(side=tk.LEFT)

        audio = self._card(outer, "AUDIO")
        audio.grid(row=1, column=0, sticky="nsew", padx=6, pady=6)
        row_audio = tk.Frame(audio, bg=theme.BG_CARD)
        row_audio.pack(anchor="w", pady=(8, 0))
        self._button(row_audio, "Buzz test", "buzz test").pack(side=tk.LEFT, padx=(0, 6))
        self._button(row_audio, "Buzz alarm", "buzz alarm").pack(side=tk.LEFT, padx=(0, 6))
        self._button(row_audio, "Buzz ON", "buzz on").pack(side=tk.LEFT, padx=(0, 6))
        self._button(row_audio, "Buzz OFF", "buzz off").pack(side=tk.LEFT)

        brightness = self._card(outer, "BRIGHTNESS")
        brightness.grid(row=1, column=1, sticky="nsew", padx=6, pady=6)
        slider_row = tk.Frame(brightness, bg=theme.BG_CARD)
        slider_row.pack(fill=tk.X, pady=(10, 0))
        self.bri_var = tk.IntVar(value=80)
        self.bri_scale = tk.Scale(
            slider_row,
            from_=0,
            to=100,
            orient=tk.HORIZONTAL,
            variable=self.bri_var,
            bg=theme.BG_CARD,
            fg=theme.TEXT_PRIMARY,
            highlightthickness=0,
            troughcolor=theme.BG_INPUT,
            activebackground=theme.ACCENT,
        )
        self.bri_scale.pack(side=tk.LEFT, fill=tk.X, expand=True)
        tk.Button(
            slider_row,
            text="Apply",
            command=self._send_brightness,
            bg=theme.ACCENT,
            fg=theme.BG_PRIMARY,
            bd=0,
            padx=12,
            pady=4,
            activebackground=theme.ACCENT_GREEN,
        ).pack(side=tk.LEFT, padx=8)

        touch = self._card(outer, "TOUCH PAD")
        touch.grid(row=2, column=0, sticky="nsew", padx=6, pady=6)
        touch_grid = tk.Frame(touch, bg=theme.BG_CARD)
        touch_grid.pack(fill=tk.X, pady=(8, 0))

        for idx, (label, cmd) in enumerate(TOUCH_PRESETS):
            tk.Button(
                touch_grid,
                text=label,
                command=lambda c=cmd: self._send(c),
                bg=theme.BG_INPUT,
                fg=theme.TEXT_PRIMARY,
                bd=0,
                padx=12,
                pady=8,
                activebackground=theme.ACCENT,
            ).grid(row=idx // 2, column=idx % 2, padx=4, pady=4, sticky="ew")
            touch_grid.grid_columnconfigure(idx % 2, weight=1)

        nav = tk.Frame(touch, bg=theme.BG_CARD)
        nav.pack(anchor="w", pady=(8, 0))
        self._button(nav, "Back", "nav back").pack(side=tk.LEFT, padx=(0, 6))
        self._button(nav, "Home", "nav home").pack(side=tk.LEFT, padx=(0, 6))
        self._button(nav, "Watchface", "app watchface").pack(side=tk.LEFT)

        apps = self._card(outer, "APPS")
        apps.grid(row=2, column=1, sticky="nsew", padx=6, pady=6)
        apps_grid = tk.Frame(apps, bg=theme.BG_CARD)
        apps_grid.pack(fill=tk.X, pady=(8, 0))
        for idx, (label, cmd) in enumerate(APP_COMMANDS):
            tk.Button(
                apps_grid,
                text=label,
                command=lambda c=cmd: self._send(c),
                bg=theme.BG_INPUT,
                fg=theme.TEXT_PRIMARY,
                bd=0,
                padx=12,
                pady=6,
                activebackground=theme.ACCENT_SOL,
            ).grid(row=idx // 5, column=idx % 5, padx=4, pady=4, sticky="ew")
            apps_grid.grid_columnconfigure(idx % 5, weight=1)

        self.status_label = tk.Label(
            outer,
            text="",
            bg=theme.BG_PRIMARY,
            fg=theme.TEXT_MUTED,
            font=theme.FONT_LABEL,
        )
        self.status_label.grid(row=4, column=0, columnspan=2, sticky="w", padx=6, pady=(8, 0))

    def _button(self, parent, title, cmd, danger=False):
        return tk.Button(
            parent,
            text=title,
            command=lambda: self._send(cmd),
            bg=theme.DANGER if danger else theme.BG_INPUT,
            fg=theme.BG_PRIMARY if danger else theme.TEXT_PRIMARY,
            bd=0,
            padx=10,
            pady=5,
            activebackground=theme.WARNING if danger else theme.ACCENT,
        )

    def _local_epoch(self):
        offset = datetime.now().astimezone().utcoffset()
        offset_sec = int(offset.total_seconds()) if offset else 0
        return int(time.time()) + offset_sec

    def _send(self, command):
        if self.serial is None or not self.serial.is_running():
            messagebox.showwarning("Not connected", "Connect to a device first.")
            return
        if command == "__gm_solana__":
            epoch = self._local_epoch()
            sequence = [
                f"clock sync {epoch}\n",
                "set watchface 3\n",
                "app watchface\n",
            ]
            for cmd in sequence:
                if not self.serial.write(cmd):
                    self.status_label.config(text=f"Send failed: {cmd.strip()}", fg=theme.DANGER)
                    return
            self.status_label.config(
                text=f"Sent: clock sync {epoch} + set watchface 3 + app watchface",
                fg=theme.ACCENT_GREEN,
            )
            return
        if command == "__clock_sync__":
            epoch = self._local_epoch()
            command = f"clock sync {epoch}"
        if not self.serial.write(command + "\n"):
            self.status_label.config(text=f"Send failed: {command}", fg=theme.DANGER)
            return
        self.status_label.config(text=f"Sent: {command}", fg=theme.ACCENT_GREEN)

    def _send_brightness(self):
        self._send(f"bri {self.bri_var.get()}")

    def refresh(self, snap):
        self.proto_label.config(text=snap.get("prototype_name", "Unknown prototype"))
        self.proto_sub.config(
            text=f"mcu={snap.get('mcu', 'unknown')}  display={snap.get('display', 'unknown')}"
        )
