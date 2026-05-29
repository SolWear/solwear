pub mod device;
pub mod flash;
pub mod protocol;
pub mod serial;

pub use device::{detect_devices, DeviceInfo, FlashMethod, PrototypeProfile};
pub use protocol::{parse_status_line, Command, StatusHeartbeat};
pub use serial::SerialSession;
