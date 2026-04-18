"""Parse SolWearOS serial log lines into structured data."""

import re


FW_RE = re.compile(r"SolWearOS\s+(v[\d.]+)")


def classify(line: str) -> str:
    """Return a tag name for color-coding the line in the console view."""
    s = line.strip()
    if s.startswith("[HAL]"):    return "hal"
    if s.startswith("[CORE]"):   return "core"
    if s.startswith("[BATT]"):   return "batt"
    if s.startswith("[STATUS]"): return "status"
    if s.startswith("[BOOT]"):   return "boot"
    if s.startswith("[APP]"):    return "app"
    if "FATAL" in s or "Error" in s or "ERROR" in s or "not found" in s:
        return "error"
    return "plain"


def parse_status(line: str):
    """If line is a [STATUS] heartbeat, return parsed dict; else None."""
    s = line.strip()
    if not s.startswith("[STATUS]"):
        return None

    payload = s[len("[STATUS]"):].strip()
    kv = {}
    for token in payload.split():
        if "=" not in token:
            continue
        key, value = token.split("=", 1)
        kv[key.strip().lower()] = value.strip()

    def _int(key, default=0):
        try:
            return int(float(kv.get(key, default)))
        except Exception:
            return default

    def _float(key, default=0.0):
        try:
            return float(kv.get(key, default))
        except Exception:
            return default

    temp = None
    if "temp" in kv or "tempc" in kv:
        temp = _float("temp", _float("tempc", 0.0))

    return {
        "batt":     _int("batt", _int("battery", 0)),
        "volt":     _float("volt", _float("voltage", 0.0)),
        "heap":     _int("heap", _int("freeheap", 0)),
        "steps":    _int("steps", _int("step", 0)),
        "uptime":   _int("uptime", _int("up", 0)),
        "charging": str(kv.get("charging", "0")).lower() in ("1", "true", "yes", "on"),
        "temp":     temp,
        "proto":    kv.get("proto", kv.get("prototype", "")),
        "mcu":      kv.get("mcu", ""),
        "display":  kv.get("display", ""),
        "caps":     kv.get("caps", ""),
    }


def parse_fw(line: str):
    """If line is the boot banner, return version string; else None."""
    m = FW_RE.search(line)
    return m.group(1) if m else None
