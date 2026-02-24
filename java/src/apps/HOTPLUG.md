# Hot-Plug Support for MCAN Serial Devices

**Version:** 2026-02-20
**Feature:** USB Device Hot-Plug Support

## Overview

The MCAN Protocol bridge now supports hot-plugging of USB devices without crashing or
requiring a restart. This allows for:

- **Starting without devices:** Bridge can start even if MCAN adapter is not plugged in
- **Unplugging during operation:** Bridge continues running, TCP clients stay connected
- **Automatic reconnection:** Bridge detects when device is replugged and reconnects
- **Graceful degradation:** TCP messages are accepted but not forwarded until device reconnects

## How It Works

### Architecture

Each bridge instance runs three threads:

1. **TCP Acceptor Thread:** Accepts incoming TCP connections (always running)
2. **Serial Reader Thread:** Reads from serial port when available
3. **Reconnection Monitor Thread:** Checks device status every 5 seconds

### State Machine

```
┌─────────────────┐
│  Port Available │
│   (connected)   │
└────────┬────────┘
         │
         │ USB unplugged / read error
         ▼
┌─────────────────┐
│ Port Unavailable│
│ (disconnected)  │
└────────┬────────┘
         │
         │ Reconnection monitor detects device
         ▼
┌─────────────────┐
│  Trying to Open │
│    (reconnect)  │
└────────┬────────┘
         │
         │ Success
         ▼
┌─────────────────┐
│  Port Available │
└─────────────────┘
```

### Behavior in Each State

**Port Available (Connected):**
- Serial → TCP: Data flows normally
- TCP → Serial: Data forwarded to device
- Status: "serial port connected"

**Port Unavailable (Disconnected):**
- Serial → TCP: No data (device unplugged)
- TCP → Serial: Messages received but discarded
- Status: "serial port disconnected"
- Monitor: Attempts reconnection every 5 seconds

**Trying to Open (Reconnecting):**
- Attempts to open serial port
- If successful: transitions to "Connected"
- If failed: stays in "Disconnected", tries again in 5 seconds

## Usage Examples

### Starting Without Device

```bash
$ java -jar mcan-tcp-bridge.jar --port COM3

MCAN Protocol Serial To TCP Multiplexer Bridge
==================================================
Configuration:
  Devices: 1
    - COM3 → TCP port 15731

All bridges running. Press Ctrl+C to stop.
```

**Result:** Bridge starts, TCP server listens on 15731, waiting for device.

### JMRI Connection Before Device

1. Start bridge without CC-Schnitte plugged in
2. Start JMRI and configure CS2 Ethernet connection to localhost:15731
3. JMRI connects successfully to bridge
4. Plug in CC-Schnitte USB device
5. Bridge detects device within 5 seconds
6. Communication begins automatically

### Unplugging During Operation

```
[Before unplug]
JMRI → TCP → Bridge → Serial → CC-Schnitte → CAN Bus → Locomotives

[User unplugs CC-Schnitte]
Bridge detects disconnection
Serial reader: IOException caught
Bridge marks port as disconnected

[During disconnect]
JMRI → TCP → Bridge → (messages discarded)
JMRI connection stays active
No crash, no restart needed

[User replugs CC-Schnitte]
Reconnection monitor detects device
Bridge reopens serial port
Communication resumes

[After replug]
JMRI → TCP → Bridge → Serial → CC-Schnitte → CAN Bus → Locomotives
```

## Implementation Details

### Detection Mechanisms

**Port Unavailable Detection:**
1. IOException on serial read/write
2. End-of-stream (read returns -1)
3. Null input/output streams

**Reconnection Trigger:**
- Reconnection monitor checks every 5 seconds
- Uses `serialPortConnected` flag
- Verifies `serialPort.isOpen()`

### Thread Safety

- `volatile boolean serialPortConnected` - atomic reads/writes
- `volatile SerialPort serialPort` - safe publication
- `AtomicBoolean reconnecting` - prevents concurrent reconnection attempts
- `CopyOnWriteArrayList<ClientHandler>` - thread-safe client list

### Error Handling

**Serial Read Errors:**
```java
catch (IOException e) {
    log.warn("Serial read error (device may be unplugged)");
    serialPortConnected = false;
    // Continue running, don't crash
}
```

**Serial Write Errors:**
```java
if (!serialPortConnected) {
    log.debug("Serial port not connected, discarding bytes");
    return; // Silently discard, don't crash
}
```

## Configuration

### Reconnection Delay

Default: 5 seconds (5000ms)

To change (requires recompilation):
```java
private static final int RECONNECT_DELAY_MS = 5000;
```

### Logging

**Normal operation:**
```
[COM3:15731] Starting bridge
[COM3:15731] Serial port not available: COM3
[COM3:15731] TCP server listening
[COM3:15731] Serial reader started
[COM3:15731] Reconnection monitor started
[COM3:15731] Bridge started (serial port not available)
```

**Reconnection:**
```
[COM3:15731] Serial port disconnected, attempting reconnection...
[COM3:15731] Serial port reconnected successfully
```

**Debug logging:**
```bash
java -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG -jar mcan-tcp-bridge.jar --port COM3
```

Shows:
- Serial port not connected, discarding X bytes
- Detailed reconnection attempts

## Benefits

### For Users

1. **No manual restart:** Unplug/replug works automatically
2. **JMRI stays connected:** No need to restart JMRI
3. **Multiple devices:** Each device reconnects independently
4. **Startup flexibility:** Start bridge before plugging in devices

### For System Integrators

1. **Reliable operation:** USB glitches don't crash the system
2. **Service mode:** Can run as system service without manual intervention
3. **Hot-swap:** Replace failed CC-Schnitte without stopping JMRI
4. **Testing:** Easy to test with/without hardware

## Limitations

### Message Loss During Disconnect

**TCP → Serial messages are discarded** while device is disconnected.

This is intentional because:
- Queuing messages could cause stale commands when device reconnects
- CAN bus commands are time-sensitive
- JMRI will retry failed commands automatically

**Serial → TCP messages are lost** during disconnect (obviously, device is unplugged).

### Reconnection Delay

There's a 5-second delay between disconnect detection and reconnection attempt.

This prevents:
- Rapid reconnection attempts during brief USB glitches
- Log spam from failed connection attempts
- Excessive CPU usage

### TCP Client Buffering

TCP clients (JMRI) continue sending data during disconnect. The TCP stack will buffer
these messages, but they are discarded at the bridge level, not forwarded.

## Testing

### Test 1: Start Without Device

```bash
$ java -jar marklin-cdb-bridge.jar --port COM3
# Bridge starts, waits for device
```

### Test 2: Plug Device After Start

```bash
$ java -jar marklin-cdb-bridge.jar --port COM3
# Wait 5 seconds
# Plug in CC-Schnitte
# Within 5 seconds: "Serial port reconnected successfully"
```

### Test 3: Unplug During Operation

```bash
$ java -jar marklin-cdb-bridge.jar --port COM3
# Connect JMRI
# Send some commands (work normally)
# Unplug CC-Schnitte
# Commands are discarded (logged in debug mode)
# Replug CC-Schnitte
# Commands work again
```

### Test 4: Multiple Devices

```bash
$ java -jar marklin-cdb-bridge.jar --port COM3,COM4
# Each device reconnects independently
# Unplug COM3: only COM3 reconnects, COM4 unaffected
```

## Statistics

The bridge tracks connection status:

```
=== Statistics ===
Serial port status: connected
Serial: 1234 bytes read, 567 bytes written
TCP: 567 bytes read, 1234 bytes written
Connected clients: 1
```

Or when disconnected:

```
=== Statistics ===
Serial port status: disconnected
Serial: 0 bytes read, 0 bytes written
TCP: 567 bytes read, 0 bytes written
Connected clients: 1
```

---

**Implementation Date:** 2026-02-20
**Reconnection Delay:** 5 seconds
**Thread Model:** 3 threads per device (TCP acceptor, serial reader, reconnection monitor)
**State Tracking:** `volatile serialPortConnected` flag
