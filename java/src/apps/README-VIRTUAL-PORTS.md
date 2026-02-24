# Virtual COM Port for Serial Software

**Author**: Ralf Lang (ralf.lang@ralf-lang.de)
**Copyright**: (C) 2026 JMRI Community

This guide explains how to create a virtual COM/serial port that connects to the bridge's TCP server. This allows serial-only software (like MCAN programmer tools) to work alongside JMRI.

## Architecture

```
Physical MCAN Adapter (COM3, e.g. CC-Schnitte)
         ↕
   Bridge (TCP :15731)
    ↕              ↕
  JMRI    Virtual COM Port Client
              ↕
         Virtual COM99
              ↕
    Programmer Software
```

Both JMRI and the programmer software can send commands without collisions thanks to the bridge's message queuing.

---

## Linux / macOS Setup

### Prerequisites

Install `socat` (available in most package managers):

```bash
# Debian/Ubuntu
sudo apt-get install socat

# Fedora/RHEL
sudo dnf install socat

# macOS (Homebrew)
brew install socat

# Arch Linux
sudo pacman -S socat
```

### Usage

#### 1. Start the Bridge

```bash
java -jar mcan-tcp-bridge.jar --port /dev/ttyUSB0
```

The bridge listens on TCP port 15731 by default.

#### 2. Create Virtual Port (Using Helper Script)

```bash
./create-virtual-port.sh /dev/ttyVCOM0 localhost 15731
```

Or manually:

```bash
socat PTY,link=/dev/ttyVCOM0,raw,echo=0 TCP:localhost:15731
```

#### 3. Use Virtual Port in Programmer Software

Configure your programmer software to use `/dev/ttyVCOM0`.

**Note**: The virtual port appears only while socat is running. You may need to run socat as root or add your user to the `dialout` group to avoid permission issues:

```bash
sudo usermod -a -G dialout $USER
# Then log out and log back in
```

---

## Windows Setup

### Option 1: Using com2tcp (Recommended - FOSS)

#### Prerequisites

Install com2tcp via winget:

```powershell
winget install --id=Eterlogic.com2tcp -e
```

#### Usage

1. **Start the Bridge**:
   ```powershell
   java -jar mcan-tcp-bridge.jar --port COM3
   ```

2. **Create Virtual Port** (Using Helper Script):
   ```powershell
   .\create-virtual-port.ps1 -ComPort COM99 -TcpHost localhost -TcpPort 15731
   ```

   Or manually:
   ```powershell
   com2tcp --baud 115200 \\.\COM99 localhost 15731
   ```

3. **Use Virtual Port**:
   - Configure programmer software to use `COM99`
   - Configure JMRI to use TCP connection to `localhost:15731`

**Note**: com2tcp creates a "virtual" COM port but still requires a physical or com0com port pair. See Option 2 for true virtual ports.

### Option 2: Using com0com + hub4com (FOSS, More Complex)

#### Prerequisites

1. Install com0com (creates COM port pairs):
   - Download from: https://sourceforge.net/projects/com0com/
   - Install and create a port pair: `COM98 ↔ COM99`

2. Install hub4com (routes data):
   - Included with com0com or download separately
   - Or use the PowerShell script below

#### Setup

1. **Start the Bridge**:
   ```powershell
   java -jar mcan-tcp-bridge.jar --port COM3
   ```

2. **Connect COM Port Pair to TCP** (Using Helper Script):
   ```powershell
   .\create-virtual-port-com0com.ps1 -ComPort COM99 -TcpHost localhost -TcpPort 15731
   ```

   Or manually:
   ```powershell
   hub4com --baud=115200 --route=All:COM99,TCP \\.\COM99 localhost:15731
   ```

3. **Use COM98 in Programmer Software**:
   - Programmer connects to `COM98`
   - hub4com bridges `COM99 ↔ TCP:15731`
   - JMRI connects to TCP `localhost:15731`

---

## Helper Scripts

### Linux/macOS: create-virtual-port.sh

Location: `java/src/apps/create-virtual-port.sh`

```bash
./create-virtual-port.sh /dev/ttyVCOM0 localhost 15731
```

### Windows (PowerShell): create-virtual-port.ps1

Location: `java/src/apps/create-virtual-port.ps1`

```powershell
.\create-virtual-port.ps1 -VirtualPort COM99 -TcpHost localhost -TcpPort 15731
```

---

## Troubleshooting

### Linux/macOS

**Problem**: Permission denied on `/dev/ttyVCOM0`
```bash
# Run socat with sudo
sudo socat PTY,link=/dev/ttyVCOM0,raw,echo=0 TCP:localhost:15731

# Or add user to dialout group (permanent solution)
sudo usermod -a -G dialout $USER
```

**Problem**: Port already in use
```bash
# Check if socat is already running
ps aux | grep socat
killall socat
```

### Windows

**Problem**: COM port not available
```powershell
# List available COM ports
mode
# Or in PowerShell
[System.IO.Ports.SerialPort]::GetPortNames()
```

**Problem**: com0com installation issues on Windows 11
- Run installer as Administrator
- Disable Secure Boot temporarily during installation
- Use test-signing mode: `bcdedit /set testsigning on`

**Problem**: Connection refused
```powershell
# Verify bridge is running and listening
netstat -an | findstr "15731"
```

---

## Performance Notes

- Virtual port adds minimal latency (<1ms typically)
- Message queuing in bridge prevents command collisions
- Both TCP and virtual port clients benefit from the same queuing
- Virtual port client gets exclusive connection (like real serial port)

---

## Alternative: Direct TCP Support

If your programmer software supports TCP connections, you can connect directly to the bridge without needing a virtual port:

```
Bridge: localhost:15731
```

This is simpler and more reliable if supported by your software.
