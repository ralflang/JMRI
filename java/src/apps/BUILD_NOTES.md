# Build Notes - MCAN Protocol Bridge

**Note:** This bridge supports MCAN (Märklin CAN) protocol serial adapters including
CC-Schnitte by CanDigitalBahn (CDB), a third-party adapter for Märklin CAN systems.

## Build System

The bridge is built using ant targets in JMRI's main build.xml.

### Prerequisites

**Required Libraries (from JMRI lib/):**
- jSerialComm-2.11.4.jar (serial port access)
- slf4j-api-2.0.17.jar (logging API)
- log4j-slf4j2-impl-2.25.3.jar (SLF4J to Log4j binding)
- log4j-api-2.25.3.jar (Log4j API)
- log4j-core-2.25.3.jar (Log4j implementation)

**Java:** Java 11 or higher

### Build Commands

**Normal JMRI build (includes bridge):**
```bash
# Using Ant
cd /path/to/JMRI
ant compile

# Using Maven
mvn compile
```

**Standalone JAR for deployment:**
```bash
# Using Ant
ant mcan-tcp-bridge-jar

# Using Maven
mvn antrun:run -Danttarget=mcan-tcp-bridge-jar
```

**Run from development environment:**
```bash
# Using Ant
ant mcan-tcp-bridge
```

**Output:** `dist/mcan-tcp-bridge.jar` - Standalone fat JAR with all dependencies embedded (~3.2 MB)

### Ant Target Implementation

**`mcan-tcp-bridge` target:**
- Depends on `debug` (compiles all JMRI code)
- Uses `-run-jmri-application` macro
- Runs `apps.McanTcpBridge` with full JMRI classpath
- Useful for development and debugging

**`mcan-tcp-bridge-jar` target:**
- Depends on `debug` (compiles all JMRI code)
- Creates standalone fat JAR in dist/ directory
- Includes bridge classes and required JMRI Marklin protocol classes
- Embeds all runtime dependencies (jSerialComm, slf4j, log4j)
- Single file deployment - no external dependencies needed

### Testing

```bash
# Test help
java -jar dist/mcan-tcp-bridge.jar --help

# Test port listing
java -jar dist/mcan-tcp-bridge.jar --list

# Test single device
java -jar dist/mcan-tcp-bridge.jar --port COM3

# Test multiple devices
java -jar dist/mcan-tcp-bridge.jar --port COM2,COM3:49999,COM4
```

## Implementation Consolidation

**Previous:** Two implementations (McanTcpBridge for single device, MultiDeviceBridge for multiple)

**Current:** Single unified implementation
- McanTcpBridge.java handles both use cases
- Multi-device support is the default implementation
- Single-device is just a special case of multi-device with one port

**Benefits:**
- Simpler to maintain (one codebase)
- No duplicate code
- Single JAR for all use cases
- Consistent behavior

## Changes from Original Plan

1. **Logging Implementation**
   - Uses log4j-slf4j2-impl + log4j-api + log4j-core
   - JMRI's standard logging stack

2. **Unified Implementation**
   - Originally had separate single-device and multi-device versions
   - Consolidated to single implementation after realizing multi-device handles single-device case

3. **Correct Attribution**
   - Fixed branding to clarify CC-Schnitte is CanDigitalBahn (third-party) product
   - Not a Märklin product

4. **JAR Packaging**
   - Uses manifest with Class-Path instead of fat JAR
   - Dependencies must be in same directory as JAR
   - Simpler, more maintainable

## Distribution

To distribute the bridge:

1. Copy these files to target system:
   - mcan-tcp-bridge.jar
   - jSerialComm-2.11.4.jar
   - slf4j-api-2.0.17.jar
   - log4j-slf4j2-impl-2.25.3.jar
   - log4j-api-2.25.3.jar
   - log4j-core-2.25.3.jar

2. Keep all files in same directory

3. Run with: `java -jar mcan-tcp-bridge.jar --port <port_spec>`

## Troubleshooting

**"NoClassDefFoundError: org/slf4j/LoggerFactory"**
- Cause: JAR dependencies not found
- Solution: Ensure all dependency JARs are in same directory as bridge JAR

**"Package com.fazecast.jSerialComm is not found"**
- Cause: Classpath not set correctly during compilation
- Solution: Check OSTYPE detection and classpath separator in build script

---

**Build System Version:** 2026-02-20
**Tested On:** Windows 11 with Git Bash, Java 21
**Implementation:** Unified single+multi-device bridge
