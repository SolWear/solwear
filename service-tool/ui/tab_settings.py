"""Settings tab — local JSON editor for development settings overrides."""
import json
import os
import tkinter as tk
from tkinter import filedialog, messagebox

from ui import theme


DEFAULT_SETTINGS = {
    "brightness": 80,
    "soundEnabled": True,
    "vibrationEnabled": True,
    "watchFaceIndex": 0,
    "wallpaperIndex": 0,
    "stepGoal": 10000,
}

SETTINGS_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "device_settings.json",
)


class SettingsTab(tk.Frame):
    def __init__(self, parent, serial_reader=None):
        super().__init__(parent, bg=theme.BG_PRIMARY)
        self.serial = serial_reader
        self.vars = {}
        self._build()
        self._load()

    def _build(self):
        outer = tk.Frame(self, bg=theme.BG_PRIMARY, padx=20, pady=20)
        outer.pack(fill=tk.BOTH, expand=True)

        tk.Label(
            outer, text="Device Settings (local override)",
            bg=theme.BG_PRIMARY, fg=theme.ACCENT,
            font=theme.FONT_HEADER,
        ).pack(anchor="w", pady=(0, 4))

        tk.Label(
            outer,
            text="Edit values below and click Save. Use 'Push to device'\n"
                 "to send a settings.update command over serial.",
            bg=theme.BG_PRIMARY, fg=theme.TEXT_MUTED,
            font=theme.FONT_LABEL, justify=tk.LEFT,
        ).pack(anchor="w", pady=(0, 16))

        form = tk.Frame(outer, bg=theme.BG_CARD, padx=20, pady=16)
        form.pack(fill=tk.X)

        rows = [
            ("Brightness (0-100)", "brightness",     "int"),
            ("Sound enabled",      "soundEnabled",   "bool"),
            ("Vibration enabled",  "vibrationEnabled", "bool"),
            ("Watch face index",   "watchFaceIndex", "int"),
            ("Wallpaper index",    "wallpaperIndex", "int"),
            ("Daily step goal",    "stepGoal",       "int"),
        ]

        for i, (label, key, kind) in enumerate(rows):
            tk.Label(form, text=label, bg=theme.BG_CARD, fg=theme.TEXT_SECONDARY,
                     font=theme.FONT_LABEL).grid(row=i, column=0, sticky="w", pady=6)
            if kind == "bool":
                var = tk.BooleanVar()
                tk.Checkbutton(
                    form, variable=var,
                    bg=theme.BG_CARD, activebackground=theme.BG_CARD,
                    selectcolor=theme.BG_INPUT, fg=theme.TEXT_PRIMARY,
                ).grid(row=i, column=1, sticky="w", padx=12)
            else:
                var = tk.StringVar()
                tk.Entry(
                    form, textvariable=var, width=10,
                    bg=theme.BG_INPUT, fg=theme.TEXT_PRIMARY,
                    insertbackground=theme.ACCENT, bd=0, relief=tk.FLAT,
                ).grid(row=i, column=1, sticky="w", padx=12, ipady=3)
            self.vars[key] = (var, kind)

        # Buttons
        btn_row = tk.Frame(outer, bg=theme.BG_PRIMARY)
        btn_row.pack(fill=tk.X, pady=14)

        tk.Button(btn_row, text="Save Local", command=self._save,
                  bg=theme.ACCENT_GREEN, fg=theme.BG_PRIMARY, bd=0, padx=14, pady=6,
                  activebackground=theme.ACCENT).pack(side=tk.LEFT, padx=4)

        tk.Button(btn_row, text="Push To Device", command=self._push_to_device,
              bg=theme.ACCENT, fg=theme.BG_PRIMARY, bd=0, padx=14, pady=6,
              activebackground=theme.ACCENT_GREEN).pack(side=tk.LEFT, padx=4)

        tk.Button(btn_row, text="Reset Defaults", command=self._reset,
                  bg=theme.BG_CARD, fg=theme.TEXT_PRIMARY, bd=0, padx=14, pady=6,
                  activebackground=theme.WARNING).pack(side=tk.LEFT, padx=4)

        tk.Button(btn_row, text="Export JSON...", command=self._export,
                  bg=theme.BG_CARD, fg=theme.TEXT_PRIMARY, bd=0, padx=14, pady=6,
                  activebackground=theme.ACCENT_SOL).pack(side=tk.LEFT, padx=4)

        self.status = tk.Label(outer, text="", bg=theme.BG_PRIMARY,
                               fg=theme.TEXT_MUTED, font=theme.FONT_LABEL)
        self.status.pack(anchor="w", pady=8)

    def _to_dict(self):
        out = {}
        for k, (var, kind) in self.vars.items():
            try:
                if kind == "bool":
                    out[k] = bool(var.get())
                else:
                    out[k] = int(var.get())
            except (ValueError, tk.TclError):
                out[k] = DEFAULT_SETTINGS[k]
        return out

    def _from_dict(self, d):
        for k, (var, kind) in self.vars.items():
            v = d.get(k, DEFAULT_SETTINGS[k])
            if kind == "bool":
                var.set(bool(v))
            else:
                var.set(str(int(v)))

    def _load(self):
        if os.path.exists(SETTINGS_PATH):
            try:
                with open(SETTINGS_PATH) as f:
                    self._from_dict(json.load(f))
                self.status.config(text=f"Loaded {SETTINGS_PATH}")
                return
            except Exception as e:
                self.status.config(text=f"Load failed: {e}", fg=theme.DANGER)
        self._from_dict(DEFAULT_SETTINGS)

    def _save(self):
        d = self._to_dict()
        try:
            with open(SETTINGS_PATH, "w") as f:
                json.dump(d, f, indent=2)
            self.status.config(text=f"Saved → {SETTINGS_PATH}", fg=theme.ACCENT_GREEN)
        except Exception as e:
            self.status.config(text=f"Save failed: {e}", fg=theme.DANGER)

    def _reset(self):
        if messagebox.askyesno("Reset", "Reset all settings to defaults?"):
            self._from_dict(DEFAULT_SETTINGS)
            self.status.config(text="Reset to defaults (not yet saved)")

    def _export(self):
        path = filedialog.asksaveasfilename(
            defaultextension=".json",
            filetypes=[("JSON", "*.json"), ("All files", "*.*")],
        )
        if not path:
            return
        try:
            with open(path, "w") as f:
                json.dump(self._to_dict(), f, indent=2)
            self.status.config(text=f"Exported → {path}", fg=theme.ACCENT_GREEN)
        except Exception as e:
            self.status.config(text=f"Export failed: {e}", fg=theme.DANGER)

    def _push_to_device(self):
        if self.serial is None or not self.serial.is_running():
            self.status.config(text="Push failed: watch not connected", fg=theme.DANGER)
            return

        d = self._to_dict()
        commands = [
            f"bri {int(d['brightness'])}",
            "buzz on" if bool(d["soundEnabled"]) else "buzz off",
            f"set watchface {int(d['watchFaceIndex'])}",
            f"set wallpaper {int(d['wallpaperIndex'])}",
            f"set stepgoal {int(d['stepGoal'])}",
            "status now",
        ]

        sent = 0
        for cmd in commands:
            if self.serial.write(cmd + "\n"):
                sent += 1

        if sent == len(commands):
            self.status.config(text="Pushed settings to device", fg=theme.ACCENT_GREEN)
        else:
            self.status.config(text=f"Partial push: {sent}/{len(commands)} commands sent", fg=theme.WARNING)
