"""Status tab — live device dashboard."""
import time
import tkinter as tk
from tkinter import simpledialog, messagebox

from ui import theme


def _fmt_uptime(seconds):
    h = seconds // 3600
    m = (seconds % 3600) // 60
    s = seconds % 60
    return f"{h:d}:{m:02d}:{s:02d}"


def _fmt_bytes(n):
    if n >= 1024 * 1024:
        return f"{n / (1024*1024):.2f} MB"
    if n >= 1024:
        return f"{n / 1024:.1f} KB"
    return f"{n} B"


class StatusTab(tk.Frame):
    def __init__(self, parent, state, serial=None):
        super().__init__(parent, bg=theme.BG_PRIMARY)
        self.state = state
        self.serial = serial
        self._volt_history = []
        self._build()

    def _card(self, parent, title):
        wrap = tk.Frame(parent, bg=theme.BG_CARD, padx=16, pady=12, bd=0)
        tk.Label(
            wrap, text=title, bg=theme.BG_CARD, fg=theme.TEXT_MUTED,
            font=theme.FONT_LABEL,
        ).pack(anchor="w")
        return wrap

    def _build(self):
        outer = tk.Frame(self, bg=theme.BG_PRIMARY, padx=16, pady=16)
        outer.pack(fill=tk.BOTH, expand=True)

        # Grid of cards: 2 cols x 4 rows
        for i in range(2):
            outer.grid_columnconfigure(i, weight=1, uniform="col")
        for r in range(4):
            outer.grid_rowconfigure(r, weight=1, uniform="row")

        # Battery card
        self.bat_card = self._card(outer, "BATTERY")
        self.bat_card.grid(row=0, column=0, sticky="nsew", padx=6, pady=6)
        self.bat_pct_label = tk.Label(self.bat_card, text="--%",
                                      bg=theme.BG_CARD, fg=theme.ACCENT_GREEN,
                                      font=theme.FONT_BIG)
        self.bat_pct_label.pack(anchor="w", pady=(8, 0))
        self.bat_volt_label = tk.Label(self.bat_card, text="-- V",
                                       bg=theme.BG_CARD, fg=theme.TEXT_SECONDARY,
                                       font=theme.FONT_LABEL)
        self.bat_volt_label.pack(anchor="w")
        self.bat_charge_label = tk.Label(self.bat_card, text="Discharging",
                                         bg=theme.BG_CARD, fg=theme.TEXT_MUTED,
                                         font=theme.FONT_LABEL)
        self.bat_charge_label.pack(anchor="w", pady=(4, 0))
        self.bat_spark = tk.Canvas(
            self.bat_card, height=36, bg=theme.BG_PRIMARY,
            highlightthickness=0, bd=0,
        )
        self.bat_spark.pack(fill=tk.X, pady=(6, 0))
        self.bat_cal_btn = tk.Button(
            self.bat_card, text="Calibrate…", command=self._calibrate_battery,
            bg=theme.BG_PRIMARY, fg=theme.ACCENT, bd=0, padx=10, pady=4,
            activebackground=theme.ACCENT, activeforeground=theme.BG_PRIMARY,
            font=theme.FONT_LABEL,
        )
        self.bat_cal_btn.pack(anchor="w", pady=(6, 0))

        # Temperature card
        self.temp_card = self._card(outer, "TEMPERATURE")
        self.temp_card.grid(row=3, column=0, columnspan=2, sticky="nsew", padx=6, pady=6)
        self.temp_label = tk.Label(self.temp_card, text="-- C",
                                   bg=theme.BG_CARD, fg=theme.ACCENT_GREEN,
                                   font=theme.FONT_BIG)
        self.temp_label.pack(anchor="w", pady=(8, 0))
        self.temp_sub = tk.Label(self.temp_card, text="Device-reported sensor",
                                 bg=theme.BG_CARD, fg=theme.TEXT_MUTED,
                                 font=theme.FONT_LABEL)
        self.temp_sub.pack(anchor="w")

        # Heap card
        self.heap_card = self._card(outer, "FREE HEAP")
        self.heap_card.grid(row=0, column=1, sticky="nsew", padx=6, pady=6)
        self.heap_label = tk.Label(self.heap_card, text="--",
                                   bg=theme.BG_CARD, fg=theme.ACCENT,
                                   font=theme.FONT_BIG)
        self.heap_label.pack(anchor="w", pady=(8, 0))
        self.heap_pct_label = tk.Label(self.heap_card, text="free RAM estimate",
                                       bg=theme.BG_CARD, fg=theme.TEXT_MUTED,
                                       font=theme.FONT_LABEL)
        self.heap_pct_label.pack(anchor="w")

        # Steps card
        self.steps_card = self._card(outer, "STEPS TODAY")
        self.steps_card.grid(row=1, column=0, sticky="nsew", padx=6, pady=6)
        self.steps_label = tk.Label(self.steps_card, text="0",
                                    bg=theme.BG_CARD, fg=theme.ACCENT_SOL,
                                    font=theme.FONT_BIG)
        self.steps_label.pack(anchor="w", pady=(8, 0))

        # Uptime card
        self.up_card = self._card(outer, "UPTIME")
        self.up_card.grid(row=1, column=1, sticky="nsew", padx=6, pady=6)
        self.up_label = tk.Label(self.up_card, text="0:00:00",
                                 bg=theme.BG_CARD, fg=theme.TEXT_PRIMARY,
                                 font=theme.FONT_BIG)
        self.up_label.pack(anchor="w", pady=(8, 0))

        # Connection card
        self.conn_card = self._card(outer, "CONNECTION")
        self.conn_card.grid(row=2, column=0, sticky="nsew", padx=6, pady=6)
        self.conn_main = tk.Label(self.conn_card, text="Disconnected",
                                  bg=theme.BG_CARD, fg=theme.DANGER,
                                  font=theme.FONT_HEADER)
        self.conn_main.pack(anchor="w", pady=(8, 0))
        self.conn_sub = tk.Label(self.conn_card, text="No serial port open",
                                 bg=theme.BG_CARD, fg=theme.TEXT_MUTED,
                                 font=theme.FONT_LABEL)
        self.conn_sub.pack(anchor="w")

        # Last update card
        self.last_card = self._card(outer, "LAST HEARTBEAT")
        self.last_card.grid(row=2, column=1, sticky="nsew", padx=6, pady=6)
        self.last_label = tk.Label(self.last_card, text="never",
                                   bg=theme.BG_CARD, fg=theme.TEXT_MUTED,
                                   font=theme.FONT_HEADER)
        self.last_label.pack(anchor="w", pady=(8, 0))
        self.last_sub = tk.Label(self.last_card,
                                 text="[STATUS] heartbeat from watch",
                                 bg=theme.BG_CARD, fg=theme.TEXT_MUTED,
                                 font=theme.FONT_LABEL)
        self.last_sub.pack(anchor="w")

    def refresh(self, snap):
        # Battery
        pct = snap["battery_pct"]
        self.bat_pct_label.config(text=f"{pct}%")
        if pct < 20:
            self.bat_pct_label.config(fg=theme.DANGER)
        elif pct < 50:
            self.bat_pct_label.config(fg=theme.WARNING)
        else:
            self.bat_pct_label.config(fg=theme.ACCENT_GREEN)
        self.bat_volt_label.config(text=f"{snap['battery_volt']:.2f} V")
        if snap["charging"]:
            self.bat_charge_label.config(text="⚡ Charging", fg=theme.ACCENT)
        else:
            self.bat_charge_label.config(text="Discharging", fg=theme.TEXT_MUTED)

        volt = snap.get("battery_volt", 0.0)
        if volt > 0 and (not self._volt_history or self._volt_history[-1] != volt):
            self._volt_history.append(volt)
            if len(self._volt_history) > 60:
                self._volt_history = self._volt_history[-60:]
            self._draw_spark()

        # Heap
        heap = snap["heap_bytes"]
        self.heap_label.config(text=_fmt_bytes(heap))
        mcu = snap.get("mcu", "unknown")
        if mcu == "rp2040":
            pct_heap = (heap / 264192.0) * 100 if heap else 0
            self.heap_pct_label.config(text=f"{pct_heap:.0f}% free of ~256 KB")
        else:
            self.heap_pct_label.config(text=f"mcu={mcu} (absolute free heap)")

        # Steps & uptime
        self.steps_label.config(text=f"{snap['steps']:,}")
        self.up_label.config(text=_fmt_uptime(snap["uptime_sec"]))

        # Connection
        if snap["connected"]:
            self.conn_main.config(text="Connected", fg=theme.ACCENT_GREEN)
            self.conn_sub.config(
                text=f"Port: {snap['port']} | {snap.get('prototype_name', 'Unknown prototype')}"
            )
        else:
            self.conn_main.config(text="Disconnected", fg=theme.DANGER)
            self.conn_sub.config(text="No serial port open")

        # Last heartbeat freshness
        if snap["last_update"] > 0:
            age = time.time() - snap["last_update"]
            self.last_label.config(text=f"{age:.1f}s ago")
            if age > 10:
                self.last_label.config(fg=theme.DANGER)
            elif age > 6:
                self.last_label.config(fg=theme.WARNING)
            else:
                self.last_label.config(fg=theme.ACCENT_GREEN)
        else:
            self.last_label.config(text="never", fg=theme.TEXT_MUTED)

        # Temperature
        t = snap.get("temperature_c")
        if t is None:
            self.temp_label.config(text="-- C", fg=theme.TEXT_MUTED)
        else:
            self.temp_label.config(text=f"{t:.1f} C")
            if t > 65:
                self.temp_label.config(fg=theme.DANGER)
            elif t > 50:
                self.temp_label.config(fg=theme.WARNING)
            elif t > 40:
                self.temp_label.config(fg=theme.ACCENT)
            else:
                self.temp_label.config(fg=theme.ACCENT_GREEN)

    def _draw_spark(self):
        c = self.bat_spark
        c.delete("all")
        w = c.winfo_width() or 240
        h = c.winfo_height() or 36
        pts = self._volt_history
        if len(pts) < 2:
            return
        lo = min(pts + [3.2])
        hi = max(pts + [4.3])
        span = max(hi - lo, 0.1)
        step = w / max(len(pts) - 1, 1)
        coords = []
        for i, v in enumerate(pts):
            x = i * step
            y = h - ((v - lo) / span) * (h - 4) - 2
            coords.extend([x, y])
        c.create_line(*coords, fill=theme.ACCENT_GREEN, width=2, smooth=True)

    def _calibrate_battery(self):
        if self.serial is None or not self.serial.is_running():
            messagebox.showwarning(
                "Not connected",
                "Connect to the watch first, then click Calibrate."
            )
            return
        v = simpledialog.askfloat(
            "Calibrate battery",
            "Measure the battery voltage with a multimeter and enter it (V):",
            minvalue=2.5, maxvalue=4.5,
        )
        if v is None:
            return
        if not self.serial.write(f"calbat {v:.3f}\n"):
            messagebox.showerror("Error", "Failed to send calibration command.")
            return
        messagebox.showinfo(
            "Calibration sent",
            f"Sent calbat {v:.3f}\nWatch the console for [CAL] confirmation.",
        )
