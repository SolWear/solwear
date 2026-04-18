"""Console tab — live serial log + command input."""
import time
import tkinter as tk
from tkinter import filedialog

from ui import theme

CAT_COLOR = {
    "hal":    theme.LOG_HAL,
    "core":   theme.LOG_CORE,
    "batt":   theme.LOG_BATT,
    "status": theme.LOG_STATUS,
    "boot":   theme.LOG_BOOT,
    "app":    theme.LOG_APP,
    "error":  theme.LOG_ERROR,
    "info":   theme.ACCENT,
    "plain":  theme.LOG_PLAIN,
}


class ConsoleTab(tk.Frame):
    def __init__(self, parent, serial_reader):
        super().__init__(parent, bg=theme.BG_PRIMARY)
        self.serial = serial_reader
        self._show_status_lines = tk.BooleanVar(value=True)
        self._build()

    def _build(self):
        # Toolbar
        toolbar = tk.Frame(self, bg=theme.BG_SECONDARY, height=36)
        toolbar.pack(side=tk.TOP, fill=tk.X)
        toolbar.pack_propagate(False)

        tk.Button(toolbar, text="Clear", command=self._clear,
                  bg=theme.BG_CARD, fg=theme.TEXT_PRIMARY, bd=0, padx=12,
                  activebackground=theme.ACCENT_SOL).pack(side=tk.LEFT, padx=6, pady=4)

        tk.Button(toolbar, text="Save Log...", command=self._save_log,
                  bg=theme.BG_CARD, fg=theme.TEXT_PRIMARY, bd=0, padx=12,
                  activebackground=theme.ACCENT_SOL).pack(side=tk.LEFT, padx=4, pady=4)

        tk.Checkbutton(
            toolbar, text="Show [STATUS] heartbeats",
            variable=self._show_status_lines,
            bg=theme.BG_SECONDARY, fg=theme.TEXT_SECONDARY,
            activebackground=theme.BG_SECONDARY,
            activeforeground=theme.TEXT_PRIMARY,
            selectcolor=theme.BG_INPUT,
            font=theme.FONT_LABEL,
        ).pack(side=tk.LEFT, padx=10)

        # Log text area
        text_frame = tk.Frame(self, bg=theme.BG_PRIMARY)
        text_frame.pack(fill=tk.BOTH, expand=True, padx=4, pady=4)

        self.text = tk.Text(
            text_frame, bg=theme.BG_INPUT, fg=theme.TEXT_PRIMARY,
            insertbackground=theme.ACCENT, wrap=tk.NONE,
            font=theme.FONT_MONO, bd=0, padx=8, pady=6, relief=tk.FLAT,
        )
        self.text.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        scroll = tk.Scrollbar(text_frame, command=self.text.yview,
                              bg=theme.BG_CARD, troughcolor=theme.BG_SECONDARY,
                              activebackground=theme.ACCENT, bd=0)
        scroll.pack(side=tk.RIGHT, fill=tk.Y)
        self.text.config(yscrollcommand=scroll.set, state=tk.DISABLED)

        for cat, color in CAT_COLOR.items():
            self.text.tag_config(cat, foreground=color)
        self.text.tag_config("ts", foreground=theme.TEXT_MUTED)

        # Command input
        input_frame = tk.Frame(self, bg=theme.BG_SECONDARY, height=40)
        input_frame.pack(side=tk.BOTTOM, fill=tk.X)
        input_frame.pack_propagate(False)

        tk.Label(input_frame, text=">", bg=theme.BG_SECONDARY,
                 fg=theme.ACCENT, font=theme.FONT_MONO).pack(side=tk.LEFT, padx=(10, 4))

        self.entry = tk.Entry(
            input_frame, bg=theme.BG_INPUT, fg=theme.TEXT_PRIMARY,
            insertbackground=theme.ACCENT, bd=0, font=theme.FONT_MONO,
            relief=tk.FLAT,
        )
        self.entry.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=4, pady=8, ipady=4)
        self.entry.bind("<Return>", self._on_send)

        tk.Button(
            input_frame, text="Send", command=self._on_send,
            bg=theme.ACCENT, fg=theme.BG_PRIMARY, bd=0, padx=14,
            activebackground=theme.ACCENT_GREEN,
        ).pack(side=tk.RIGHT, padx=8, pady=6)

    def append(self, category, line):
        if category == "status" and not self._show_status_lines.get():
            return
        ts = time.strftime("%H:%M:%S")
        self.text.config(state=tk.NORMAL)
        self.text.insert(tk.END, f"{ts} ", "ts")
        self.text.insert(tk.END, line + "\n", category)

        # Auto-trim if log gets huge (> 5000 lines)
        line_count = int(self.text.index("end-1c").split(".")[0])
        if line_count > 5000:
            self.text.delete("1.0", f"{line_count - 4000}.0")

        self.text.see(tk.END)
        self.text.config(state=tk.DISABLED)

    def _on_send(self, event=None):
        cmd = self.entry.get().strip()
        if not cmd:
            return
        if self.serial.write(cmd + "\n"):
            self.append("info", f"-> {cmd}")
        self.entry.delete(0, tk.END)

    def _clear(self):
        self.text.config(state=tk.NORMAL)
        self.text.delete("1.0", tk.END)
        self.text.config(state=tk.DISABLED)

    def _save_log(self):
        path = filedialog.asksaveasfilename(
            defaultextension=".log",
            filetypes=[("Log files", "*.log"), ("Text files", "*.txt"), ("All files", "*.*")],
        )
        if not path:
            return
        with open(path, "w", encoding="utf-8") as f:
            f.write(self.text.get("1.0", tk.END))
