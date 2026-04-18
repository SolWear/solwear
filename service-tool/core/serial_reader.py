"""Background serial reader thread.

Streams serial logs from SolWear prototypes and parses [STATUS] heartbeats
into shared DeviceState.
"""
import threading

try:
    import serial
    from serial.tools import list_ports
except ImportError:
    serial = None
    list_ports = None
def _to_int(value, default=0):
    try:
        return int(float(value))
    except Exception:
        return default


def _to_float(value, default=0.0):
    try:
        return float(value)
    except Exception:
        return default


def _to_bool(value):
    s = str(value).strip().lower()
    return s in ("1", "true", "yes", "on")


def parse_status_line(text):
    if not text.startswith("[STATUS]"):
        return None

    payload = text[len("[STATUS]"):].strip()
    kv = {}
    for token in payload.split():
        if "=" not in token:
            continue
        key, value = token.split("=", 1)
        kv[key.strip().lower()] = value.strip()

    out = {
        "batt": _to_int(kv.get("batt", kv.get("battery", 0))),
        "volt": _to_float(kv.get("volt", kv.get("voltage", 0.0))),
        "heap": _to_int(kv.get("heap", kv.get("freeheap", 0))),
        "steps": _to_int(kv.get("steps", kv.get("step", 0))),
        "uptime": _to_int(kv.get("uptime", kv.get("up", 0))),
        "charging": _to_bool(kv.get("charging", 0)),
        "temp": kv.get("temp", kv.get("tempc", None)),
        "proto": kv.get("proto", kv.get("prototype", "")),
        "mcu": kv.get("mcu", ""),
        "display": kv.get("display", ""),
        "caps": kv.get("caps", ""),
    }

    if out["temp"] is not None:
        out["temp"] = _to_float(out["temp"], 0.0)
    return out


def list_serial_ports():
    if list_ports is None:
        return []
    return [p.device for p in list_ports.comports()]


# Known USB VID:PID pairs that identify SolWear prototypes.
# 0x303A = Espressif (ESP32-S3 USB CDC native); 0x2E8A = Raspberry Pi (RP2040).
_SOLWEAR_VID_PIDS = {
    (0x303A, 0x4001),  # ESP32-S3 Board CDC (Lolin S3 Mini / S3-Zero)
    (0x303A, 0x1001),  # ESP32-S3 generic
    (0x2E8A, 0x000A),  # RP2040 (Pico)
    (0x2E8A, 0x0003),  # RP2040 BOOTSEL
}


def detect_solwear_ports():
    """Return a list of (device, vid, pid, description) for likely SolWear boards.

    Ordered: ESP32-S3 CDC first, then RP2040, then everything else matching.
    """
    if list_ports is None:
        return []
    out = []
    for p in list_ports.comports():
        vid = getattr(p, "vid", None)
        pid = getattr(p, "pid", None)
        if vid is None or pid is None:
            continue
        if (vid, pid) in _SOLWEAR_VID_PIDS:
            out.append((p.device, vid, pid, p.description or ""))
    # Sort: ESP32-S3 (0x303A) first, then RP2040
    out.sort(key=lambda x: (0 if x[1] == 0x303A else 1, x[0]))
    return out


class SerialReader:
    def __init__(self, state, log_queue):
        self.state = state
        self.log_queue = log_queue
        self._thread = None
        self._stop = threading.Event()
        self._ser = None
        self.port = None
        self.baud = 115200

    def is_running(self):
        return self._thread is not None and self._thread.is_alive()

    def connect(self, port, baud=115200):
        if serial is None:
            self.log_queue.put(("error", "pyserial not installed — pip install pyserial"))
            return False
        self.disconnect()
        self.port = port
        self.baud = baud
        try:
            self._ser = serial.Serial(port, baud, timeout=0.1)
        except Exception as e:
            self.log_queue.put(("error", f"Failed to open {port}: {e}"))
            return False
        self.state.set_connected(True, port)
        self._stop.clear()
        self._thread = threading.Thread(target=self._run, daemon=True)
        self._thread.start()
        self.log_queue.put(("info", f"Connected to {port} @ {baud}"))
        return True

    def disconnect(self):
        self._stop.set()
        if self._ser is not None:
            try:
                self._ser.close()
            except Exception:
                pass
            self._ser = None
        self.state.set_connected(False)
        if self._thread is not None:
            self._thread.join(timeout=1.0)
            self._thread = None

    def write(self, data):
        if self._ser is None:
            return False
        try:
            if isinstance(data, str):
                data = data.encode("utf-8")
            self._ser.write(data)
            return True
        except Exception as e:
            self.log_queue.put(("error", f"Write failed: {e}"))
            return False

    def _run(self):
        buf = b""
        while not self._stop.is_set():
            try:
                chunk = self._ser.read(256)
            except Exception as e:
                self.log_queue.put(("error", f"Serial error: {e}"))
                break
            if not chunk:
                continue
            buf += chunk
            while b"\n" in buf:
                line, _, buf = buf.partition(b"\n")
                try:
                    text = line.decode("utf-8", errors="replace").rstrip("\r")
                except Exception:
                    text = repr(line)
                self._handle_line(text)
        self.state.set_connected(False)

    def _handle_line(self, text):
        # Parse [STATUS] heartbeat
        st = parse_status_line(text)
        if st:
            self.state.update_status(
                st["batt"],
                st["volt"],
                st["heap"],
                st["steps"],
                st["uptime"],
                st["charging"],
                temperature_c=st["temp"],
            )
            if st["proto"] or st["mcu"] or st["display"] or st["caps"]:
                self.state.update_identity(
                    proto_id=st["proto"],
                    mcu=st["mcu"],
                    display=st["display"],
                    caps=st["caps"],
                )

        # FW banner
        if "SolWearOS v" in text:
            try:
                tail = text.split("SolWearOS v", 1)[1].strip()
                ver = tail.split()[0]
                self.state.set_fw(ver)
                tokens = {}
                for token in tail.split():
                    if "=" in token:
                        k, v = token.split("=", 1)
                        tokens[k.strip().lower()] = v.strip()
                if tokens:
                    self.state.update_identity(
                        proto_id=tokens.get("proto", ""),
                        mcu=tokens.get("mcu", ""),
                        display=tokens.get("display", ""),
                        caps=tokens.get("caps", ""),
                    )
            except Exception:
                pass

        # Categorize for color
        if text.startswith("[HAL]"):
            cat = "hal"
        elif text.startswith("[CORE]"):
            cat = "core"
        elif text.startswith("[BATT]"):
            cat = "batt"
        elif text.startswith("[STATUS]"):
            cat = "status"
        elif text.startswith("[CMD]"):
            cat = "core"
        elif text.startswith("[CAL]"):
            cat = "core"
        elif text.startswith("[BOOT]"):
            cat = "boot"
        elif text.startswith("[APP]"):
            cat = "app"
        elif "FATAL" in text or "ERROR" in text or "fail" in text.lower():
            cat = "error"
        else:
            cat = "plain"

        self.log_queue.put((cat, text))
