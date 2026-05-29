# solwear_st

`solwear_st` is the Rust SDK, CLI, and desktop service tool for SolWear
devices. It replaces the legacy Python/Tkinter tool with a clean-room Rust
core and Tauri UI while preserving the same device-facing capabilities.

## Workspace

| Path | Purpose |
| --- | --- |
| `crates/solwear-st-core` | Device profiles, serial transport, protocol parsing, flashing helpers |
| `crates/solwear-st-cli` | CLI for AI agents and automation |
| `src-tauri` | Tauri desktop shell using the Rust core |
| `ui` | Web UI for the desktop shell |

## CLI

```powershell
cargo run -p solwear-st-cli -- devices
cargo run -p solwear-st-cli -- status --port COM8
cargo run -p solwear-st-cli -- logs --port COM8
cargo run -p solwear-st-cli -- send --port COM8 "status now"
cargo run -p solwear-st-cli -- flash --target prototype-2 --port COM8 --firmware firmware.bin
cargo run -p solwear-st-cli -- doctor
```

## Desktop

```powershell
npm install
npm run tauri dev
```

The UI is intentionally SDK-like: device selector, logcat-style stream,
device inspector, command palette, firmware manager, and prototype catalog.

## Legacy Policy

The previous Python service tool is preserved in the
`service-tool-legacy-YYYYMMDD` release. This repository branch does not copy
legacy source; it uses legacy behavior only as a feature checklist and protocol
reference.
