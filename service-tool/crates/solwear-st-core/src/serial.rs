use std::{
    io::{BufRead, BufReader, Write},
    time::Duration,
};

use crate::protocol::{parse_status_line, Command, StatusHeartbeat};

pub struct SerialSession {
    port_name: String,
    baud: u32,
}

impl SerialSession {
    pub fn new(port_name: impl Into<String>) -> Self {
        Self {
            port_name: port_name.into(),
            baud: 115_200,
        }
    }

    pub fn with_baud(mut self, baud: u32) -> Self {
        self.baud = baud;
        self
    }

    pub fn send(&self, command: Command) -> anyhow::Result<()> {
        let mut port = serialport::new(&self.port_name, self.baud)
            .timeout(Duration::from_millis(500))
            .open()?;
        writeln!(port, "{}", command.as_wire())?;
        Ok(())
    }

    pub fn read_status(&self) -> anyhow::Result<Option<StatusHeartbeat>> {
        let port = serialport::new(&self.port_name, self.baud)
            .timeout(Duration::from_secs(3))
            .open()?;
        let mut reader = BufReader::new(port);
        let mut line = String::new();
        for _ in 0..200 {
            line.clear();
            if reader.read_line(&mut line)? == 0 {
                continue;
            }
            if let Some(status) = parse_status_line(line.trim_end()) {
                return Ok(Some(status));
            }
        }
        Ok(None)
    }

    pub fn stream_logs<F>(&self, mut on_line: F) -> anyhow::Result<()>
    where
        F: FnMut(&str),
    {
        let port = serialport::new(&self.port_name, self.baud)
            .timeout(Duration::from_millis(250))
            .open()?;
        let mut reader = BufReader::new(port);
        let mut line = String::new();
        loop {
            line.clear();
            if reader.read_line(&mut line)? == 0 {
                continue;
            }
            on_line(line.trim_end());
        }
    }
}
