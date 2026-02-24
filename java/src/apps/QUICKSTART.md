# CC-Schnitte Bridge - Quick Start

**Note:** CC-Schnitte is a third-party USB/serial adapter for Märklin CAN systems,
manufactured by CanDigitalBahn (CDB). It is not a Märklin product.

## 1. Build

```bash
cd java/src/jmri/jmrix/marklin/cdb/bridge
./build.sh          # Unix/Linux/macOS
build.bat           # Windows (if available)
```

## 2. Run

```bash
# List ports
java -jar marklin-cdb-bridge.jar --list

# Single device
java -jar marklin-cdb-bridge.jar --port COM3        # Windows
java -jar marklin-cdb-bridge.jar --port /dev/ttyUSB0  # Linux

# Multiple devices
java -jar marklin-cdb-bridge.jar --port COM2,COM3,COM4
```

## 3. Configure JMRI

For each device, add a CS2 Ethernet connection:

1. **Edit → Preferences → Connections**
2. **Add (+):**
   - System: Märklin
   - Connection: CS2 Ethernet
   - Host: `localhost`
   - Port: `15731` (or custom port from --port specification)
3. **Save** and restart

## 4. Done!

Your serial CC-Schnitte now appears as a network device to JMRI.

---

## Port Specification

```
<serial>[:<tcp_port>][,<serial>[:<tcp_port>],...]
```

**Examples:**

| Command | Result |
|---------|--------|
| `--port COM3` | COM3 → 15731 |
| `--port COM3:50000` | COM3 → 50000 |
| `--port COM2,COM3` | COM2 → 15731, COM3 → 15732 |
| `--port COM2,COM3,COM4` | COM2 → 15731, COM3 → 15732, COM4 → 15733 |
| `--port COM2,COM3:49999` | COM2 → 15731, COM3 → 49999 |
| `--port COM2:50000,COM3:50001` | COM2 → 50000, COM3 → 50001 |

---

## Troubleshooting

```bash
# Debug logging
java -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG -jar marklin-cdb-bridge.jar --port COM3

# Linux permissions
sudo usermod -a -G dialout $USER
# (then logout and login)
```

---

## Files

- **CdbSerialToTcpBridge.java** - Bridge implementation (single + multi-device)
- **README.md** - Full documentation
- **BUILD_NOTES.md** - Build system details
- **build.sh** - Build script
