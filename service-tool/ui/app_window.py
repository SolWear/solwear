"""Main application window — hosts the tabbed notebook of service-tool views."""
import queue
import time
import tkinter as tk
from tkinter import ttk
from datetime import datetime

from core.device_state import DeviceState
from core.serial_reader import SerialReader
from ui import theme
from ui.tab_console import ConsoleTab
from ui.tab_control import ControlTab
from ui.tab_status import StatusTab
from ui.tab_settings import SettingsTab
from ui.tab_firmware import FirmwareTab


class AppWindow:
    def __init__(self, root):
        self.root = root
        self.root.title("SolWear Service Tool")
        self.root.geometry("1040x720")
        self.root.configure(bg=theme.BG_PRIMARY)
        self.root.minsize(820, 560)

        self.state = DeviceState()
        self.log_queue = queue.Queue()
        self.serial = SerialReader(self.state, self.log_queue)

        self._build_style()
        self._build_topbar()
        self._build_notebook()
        self._build_statusbar()

        # 30 fps UI refresh tick
        self.root.after(33, self._tick)

        # Clean shutdown
        self.root.protocol("WM_DELETE_WINDOW", self._on_close)

    # ---------- styling ----------
    def _build_style(self):
        style = ttk.Style()
        try:
            style.theme_use("clam")
        except tk.TclError:
            pass

        style.configure("TNotebook", background=theme.BG_PRIMARY, borderwidth=0)
        style.configure(
            "TNotebook.Tab",
            background=theme.BG_INPUT,
            foreground=theme.TEXT_SECONDARY,
            padding=(18, 9),
            font=theme.FONT_LABEL,
        )
        style.map(
            "TNotebook.Tab",
            background=[("selected", theme.BG_SECONDARY)],
            foreground=[("selected", theme.ACCENT)],
        )
        style.configure(
            "TCombobox",
            fieldbackground=theme.BG_INPUT,
            background=theme.BG_SECONDARY,
            foreground=theme.TEXT_PRIMARY,
            arrowcolor=theme.ACCENT,
        )
        style.configure(
            "TButton",
            background=theme.BG_CARD,
            foreground=theme.TEXT_PRIMARY,
            borderwidth=0,
            focusthickness=0,
            padding=8,
        )
        style.map("TButton", background=[("active", theme.BG_INPUT)])
        style.configure("TFrame", background=theme.BG_PRIMARY)
        style.configure("TLabel", background=theme.BG_PRIMARY, foreground=theme.TEXT_PRIMARY)

    # ---------- top bar (port picker) ----------
    def _build_topbar(self):
        bar = tk.Frame(self.root, bg=theme.BG_PRIMARY, height=58)
        bar.pack(side=tk.TOP, fill=tk.X)
        bar.pack_propagate(False)

        inner = tk.Frame(
            bar,
            bg=theme.BG_CARD,
            highlightthickness=1,
            highlightbackground="#2a343b",
        )
        inner.pack(fill=tk.BOTH, expand=True, padx=12, pady=8)

        tk.Label(
            inner, text="SolWear Service Tool",
            bg=theme.BG_CARD, fg=theme.ACCENT,
            font=theme.FONT_HEADER,
        ).pack(side=tk.LEFT, padx=14)

        tk.Label(inner, text="Port:", bg=theme.BG_CARD, fg=theme.TEXT_SECONDARY,
                 font=theme.FONT_LABEL).pack(side=tk.LEFT, padx=(20, 4))

        self.port_var = tk.StringVar()
        self.port_combo = ttk.Combobox(inner, textvariable=self.port_var, width=14, state="readonly")
        self.port_combo.pack(side=tk.LEFT)

        tk.Button(inner, text="Refresh", command=self._refresh_ports,
                  bg=theme.BG_INPUT, fg=theme.TEXT_PRIMARY, bd=0, padx=10,
                  activebackground=theme.BG_SECONDARY, activeforeground=theme.ACCENT
                  ).pack(side=tk.LEFT, padx=4)

        tk.Button(inner, text="⚡ Quick Connect", command=self._quick_connect,
                  bg=theme.BG_INPUT, fg=theme.ACCENT_GREEN, bd=0, padx=10,
                  activebackground=theme.BG_SECONDARY, activeforeground=theme.ACCENT_GREEN,
                  font=theme.FONT_LABEL
                  ).pack(side=tk.LEFT, padx=4)

        self.connect_btn = tk.Button(
            inner, text="Connect", command=self._toggle_connect,
            bg=theme.ACCENT_SOL, fg=theme.BG_SECONDARY, bd=0, padx=14, pady=4,
            activebackground=theme.ACCENT, activeforeground=theme.BG_PRIMARY,
            font=theme.FONT_LABEL,
        )
        self.connect_btn.pack(side=tk.LEFT, padx=8)

        self._refresh_ports()

    def _refresh_ports(self):
        from core.serial_reader import list_serial_ports
        ports = list_serial_ports()
        self.port_combo["values"] = ports
        if ports and not self.port_var.get():
            self.port_var.set(ports[0])

    def _toggle_connect(self):
        if self.serial.is_running():
            self.serial.disconnect()
            self.connect_btn.config(text="Connect", bg=theme.ACCENT_SOL)
        else:
            port = self.port_var.get()
            if not port:
                return
            if self.serial.connect(port):
                self.connect_btn.config(text="Disconnect", bg=theme.ACCENT_GREEN)
                offset = datetime.now().astimezone().utcoffset()
                offset_sec = int(offset.total_seconds()) if offset else 0
                epoch = int(time.time()) + offset_sec
                if self.serial.write(f"clock sync {epoch}\n"):
                    self.log_queue.put(("info", f"Auto clock sync: {epoch}"))

    def _quick_connect(self):
        from core.serial_reader import detect_solwear_ports
        if self.serial.is_running():
            self.log_queue.put(("info", "Already connected"))
            return
        candidates = detect_solwear_ports()
        if not candidates:
            self.log_queue.put(("error", "No SolWear board detected (checked VID:PID)"))
            return
        device, vid, pid, desc = candidates[0]
        self.port_var.set(device)
        self.log_queue.put(("info", f"Quick connect: {device} [{vid:04X}:{pid:04X}] {desc}"))
        self._toggle_connect()

    # ---------- notebook ----------
    def _build_notebook(self):
        self.nb = ttk.Notebook(self.root)
        self.nb.pack(fill=tk.BOTH, expand=True, padx=12, pady=(4, 0))

        self.console_tab  = ConsoleTab(self.nb, self.serial)
        self.control_tab  = ControlTab(self.nb, self.state, self.serial)
        self.status_tab   = StatusTab(self.nb, self.state, self.serial)
        self.settings_tab = SettingsTab(self.nb, self.serial)
        self.firmware_tab = FirmwareTab(self.nb, self.serial, self.state)

        self.nb.add(self.console_tab,  text="Console")
        self.nb.add(self.control_tab,  text="Control")
        self.nb.add(self.status_tab,   text="Status")
        self.nb.add(self.settings_tab, text="Settings")
        self.nb.add(self.firmware_tab, text="Firmware")

    # ---------- status bar ----------
    def _build_statusbar(self):
        bar = tk.Frame(self.root, bg=theme.BG_PRIMARY, height=30)
        bar.pack(side=tk.BOTTOM, fill=tk.X)
        bar.pack_propagate(False)

        inner = tk.Frame(
            bar,
            bg=theme.BG_CARD,
            highlightthickness=1,
            highlightbackground="#2a343b",
        )
        inner.pack(fill=tk.BOTH, expand=True, padx=12, pady=(0, 8))

        self.conn_label = tk.Label(
            inner, text="● Disconnected",
            bg=theme.BG_CARD, fg=theme.DANGER, font=theme.FONT_LABEL,
        )
        self.conn_label.pack(side=tk.LEFT, padx=10)

        self.fw_label = tk.Label(
            inner, text="FW: --",
            bg=theme.BG_CARD, fg=theme.TEXT_MUTED, font=theme.FONT_LABEL,
        )
        self.fw_label.pack(side=tk.RIGHT, padx=10)

    # ---------- 30 fps tick ----------
    def _tick(self):
        # Drain log queue into the console widget
        drained = 0
        while drained < 200:
            try:
                cat, line = self.log_queue.get_nowait()
            except queue.Empty:
                break
            self.console_tab.append(cat, line)
            drained += 1

        snap = self.state.snapshot()
        if snap["connected"]:
            self.conn_label.config(text=f"● Connected ({snap['port']})", fg=theme.ACCENT_GREEN)
        else:
            self.conn_label.config(text="● Disconnected", fg=theme.DANGER)
        self.fw_label.config(text=f"FW: {snap['fw_version']}")

        self.control_tab.refresh(snap)
        self.status_tab.refresh(snap)
        self.firmware_tab.refresh(snap)

        self.root.after(33, self._tick)

    def _on_close(self):
        try:
            self.serial.disconnect()
        except Exception:
            pass
        self.root.destroy()
