# CC-Schnitte Bridge - Quick Start

**Note:** CC-Schnitte is a third-party USB/serial adapter for Märklin CAN systems,
manufactured by CanDigitalBahn (CDB). It is not a Märklin product.

## 1. Build

**Using Ant:**
```bash
cd /path/to/JMRI
ant cdbbridge-jar
```

**Using Maven:**
```bash
cd /path/to/JMRI
mvn antrun:run -Danttarget=cdbbridge-jar
```

**Output:** `dist/marklin-cdb-bridge.jar` (~3.2 MB standalone JAR)

## 2. Run

```bash
# List ports
java -jar dist/marklin-cdb-bridge.jar --list

# Single device
java -jar dist/marklin-cdb-bridge.jar --port COM3        # Windows
java -jar dist/marklin-cdb-bridge.jar --port /dev/ttyUSB0  # Linux

# Multiple devices
java -jar dist/marklin-cdb-bridge.jar --port COM2,COM3,COM4
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
<serial>[:<bind_addr>][:<tcp_port>][,<serial>[:<bind_addr>][:<tcp_port>],...]
```

**Examples:**

| Command | Result |
|---------|--------|
| `--port COM3` | COM3 → 15731 (all interfaces) |
| `--port COM3:50000` | COM3 → 50000 (all interfaces) |
| `--port COM3:127.0.0.1:15731` | COM3 → localhost:15731 |
| `--port COM2,COM3` | COM2 → 15731, COM3 → 15732 |
| `--port COM2,COM3,COM4` | COM2 → 15731, COM3 → 15732, COM4 → 15733 |
| `--port COM2,COM3:49999` | COM2 → 15731, COM3 → 49999 |
| `--port COM2:50000,COM3:50001` | COM2 → 50000, COM3 → 50001 |

---

## Troubleshooting

```bash
# Debug logging
java -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG -jar dist/marklin-cdb-bridge.jar --port COM3

# Linux permissions
sudo usermod -a -G dialout $USER
# (then logout and login)
```

---

## Files

- **CdbSerialToTcpBridge.java** - Bridge implementation (single + multi-device)
- **README.md** - Full documentation
- **BUILD_NOTES.md** - Build system details
- **BUILD_MODES.md** - Build options explained
