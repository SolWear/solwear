"""Shared device state — written by serial reader thread, read by UI thread."""
import threading
import time

from core.prototype_profiles import resolve_profile


class DeviceState:
    def __init__(self):
        self._lock = threading.Lock()
        self.battery_pct = 0
        self.battery_volt = 0.0
        self.heap_bytes = 0
        self.steps = 0
        self.uptime_sec = 0
        self.charging = False
        self.temperature_c = None  # None until firmware reports it
        self.last_update_ts = 0.0  # epoch seconds when last [STATUS] arrived
        self.connected = False
        self.port = ""
        self.fw_version = "unknown"
        profile = resolve_profile("", "", "")
        self.prototype_id = profile["id"]
        self.prototype_name = profile["name"]
        self.mcu = profile["mcu"]
        self.display = profile["display"]
        self.flash_method = profile["flash"]
        self.capabilities = sorted(profile["caps"])

    def update_status(self, batt, volt, heap, steps, uptime, charging, temperature_c=None):
        with self._lock:
            self.battery_pct = batt
            self.battery_volt = volt
            self.heap_bytes = heap
            self.steps = steps
            self.uptime_sec = uptime
            self.charging = charging
            if temperature_c is not None:
                self.temperature_c = temperature_c
            self.last_update_ts = time.time()

    def set_connected(self, connected, port=""):
        with self._lock:
            self.connected = connected
            self.port = port

    def set_fw(self, version):
        with self._lock:
            self.fw_version = version

    def update_identity(self, proto_id="", mcu="", display="", caps=None):
        profile = resolve_profile(proto_id, mcu, display, caps)
        with self._lock:
            self.prototype_id = profile["id"]
            self.prototype_name = profile["name"]
            self.mcu = profile["mcu"]
            self.display = profile["display"]
            self.flash_method = profile["flash"]
            self.capabilities = sorted(profile["caps"])

    def snapshot(self):
        """Return a thread-safe copy as a plain dict."""
        with self._lock:
            return {
                "battery_pct":   self.battery_pct,
                "battery_volt":  self.battery_volt,
                "heap_bytes":    self.heap_bytes,
                "steps":         self.steps,
                "uptime_sec":    self.uptime_sec,
                "charging":      self.charging,
                "temperature_c": self.temperature_c,
                "last_update":   self.last_update_ts,
                "connected":     self.connected,
                "port":          self.port,
                "fw_version":    self.fw_version,
                "prototype_id":  self.prototype_id,
                "prototype_name": self.prototype_name,
                "mcu":           self.mcu,
                "display":       self.display,
                "flash_method":  self.flash_method,
                "capabilities":  list(self.capabilities),
            }
