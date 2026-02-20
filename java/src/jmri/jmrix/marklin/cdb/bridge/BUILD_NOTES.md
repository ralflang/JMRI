# Build Notes - CC-Schnitte Bridge

**Important:** CC-Schnitte is a third-party USB/serial adapter for Märklin CAN systems,
manufactured by CanDigitalBahn (CDB). It is not a Märklin product.

## Build System

The `build.sh` script compiles the unified bridge implementation supporting both
single and multiple devices.

### Prerequisites

**Required Libraries (from JMRI lib/):**
- jSerialComm-2.11.4.jar (serial port access)
- slf4j-api-2.0.17.jar (logging API)
- log4j-slf4j2-impl-2.25.3.jar (SLF4J to Log4j binding)
- log4j-api-2.25.3.jar (Log4j API)
- log4j-core-2.25.3.jar (Log4j implementation)

**Java:** Java 11 or higher

### Build Process

```bash
cd /path/to/JMRI
bash java/src/jmri/jmrix/marklin/cdb/bridge/build.sh
```

**Output Files (in JMRI root):**
- `marklin-cdb-bridge.jar` - Bridge implementation (13KB)
- Dependency JARs (copied from lib/)

### Build Script Features

1. **Automatic JMRI_HOME Detection**
   - Navigates up from bridge directory to find JMRI root
   - Locates required libraries in lib/

2. **Cross-Platform Classpath**
   - Detects Windows (msys/cygwin) vs Unix
   - Uses semicolon separator on Windows, colon on Unix

3. **Manifest Generation**
   - Creates JAR manifest with Main-Class and Class-Path entries
   - Dependencies referenced by filename (must be in same directory as JAR)

4. **Unified Implementation**
   - Single CdbSerialToTcpBridge.java handles both single and multiple devices
   - No separate implementations needed

### Platform Notes

**Windows (Git Bash/MSYS):**
- Script detects Windows environment via `$OSTYPE`
- Uses semicolon for classpath separator
- Handles Windows paths correctly

**Linux/macOS:**
- Uses colon for classpath separator
- Standard Unix paths

### Testing

```bash
# Test help
java -jar marklin-cdb-bridge.jar --help

# Test port listing
java -jar marklin-cdb-bridge.jar --list

# Test single device
java -jar marklin-cdb-bridge.jar --port COM3

# Test multiple devices
java -jar marklin-cdb-bridge.jar --port COM2,COM3:49999,COM4
```

### Known Warnings

The following warnings during compilation are harmless:

```
Warnung: [options] Systemmodulpfad nicht zusammen mit -source 11 festgelegt
```
This warns that the module path isn't set when using `-source 11`. Can be ignored.

```
Hinweis: Die Annotationsverarbeitung ist aktiviert...
```
Annotation processing warning from Log4j. Can be suppressed with `-proc:none` if desired.

## Implementation Consolidation

**Previous:** Two implementations (CdbSerialToTcpBridge for single device, MultiDeviceBridge for multiple)

**Current:** Single unified implementation
- CdbSerialToTcpBridge.java handles both use cases
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
   - marklin-cdb-bridge.jar
   - jSerialComm-2.11.4.jar
   - slf4j-api-2.0.17.jar
   - log4j-slf4j2-impl-2.25.3.jar
   - log4j-api-2.25.3.jar
   - log4j-core-2.25.3.jar

2. Keep all files in same directory

3. Run with: `java -jar marklin-cdb-bridge.jar --port <port_spec>`

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
