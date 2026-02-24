# CC-Schnitte Bridge Build Options

## Overview

The CC-Schnitte bridge is integrated into JMRI's standard build system via ant targets.

## Build Commands

### 1. Normal JMRI Build (Default)

**Command:**
```bash
ant compile
```

The bridge is compiled automatically with all other JMRI applications. No separate build step needed.

### 2. Standalone JAR for Distribution

**Command:**
```bash
ant cdbbridge-jar
```

**Output:**
- `dist/marklin-cdb-bridge.jar` (~5 MB fat JAR)

**Characteristics:**
- ✅ Single self-contained file
- ✅ All dependencies embedded (jSerialComm, slf4j, log4j)
- ✅ No external files needed
- ✅ Easy distribution
- ✅ Works anywhere with Java 11+

**Use When:**
- Distributing to end users
- Standalone deployment (no JMRI)
- Network/remote deployments
- Simple installation preferred

### 3. Run from Development Environment

**Command:**
```bash
ant cdbbridge
```

Runs the bridge with full JMRI classpath (useful for development and debugging).

## Technical Details

### Ant Target: `cdbbridge`

- Depends on `debug` (compiles all JMRI code)
- Uses standard `-run-jmri-application` macro
- Runs `apps.CdbSerialToTcpBridge` with full classpath
- Follows same pattern as `panelpro`, `jmrifaceless`, etc.

### Ant Target: `cdbbridge-jar`

**Implementation:**
- Depends on `debug` (compiles all JMRI code)
- Creates fat JAR with embedded dependencies
- Includes bridge and required JMRI Marklin protocol classes
- Extracts and embeds: jSerialComm, slf4j-api, log4j (api/core/impl)
- Excludes signature files (META-INF/*.SF, *.RSA, *.DSA)

**Manifest:**
```
Main-Class: apps.CdbSerialToTcpBridge
Implementation-Title: CC-Schnitte Serial-to-TCP Bridge
Implementation-Version: ${release}
Implementation-Vendor: JMRI
```

**Included Classes:**
- Bridge: `apps/CdbSerialToTcpBridge*.class`
- JMRI Marklin: `jmri/jmrix/marklin/MarklinCanCodec*.class`, `MarklinConstants.class`, `MarklinMessageFormatter*.class`
- jSerialComm: `com/fazecast/...`
- Logging: `org/slf4j/...`, `org/apache/logging/...`
- Platform natives: `Android/`, `Linux/`, `Windows/`, `OSX/`, etc.

## Usage

```bash
# Build standalone JAR
ant cdbbridge-jar

# Run standalone JAR
java -jar dist/marklin-cdb-bridge.jar --port COM3
java -jar dist/marklin-cdb-bridge.jar --port COM2,COM3:49999,COM4:50000
java -jar dist/marklin-cdb-bridge.jar --help
```

## Integration with JMRI Build System

The bridge follows JMRI's standard application patterns:

| Application | Run Target | Standalone JAR | Pattern |
|-------------|------------|----------------|---------|
| PanelPro | `ant panelpro` | N/A | GUI app, part of JMRI distribution |
| DecoderPro | `ant decoderpro` | N/A | GUI app, part of JMRI distribution |
| JmriFaceless | `ant jmrifaceless` | N/A | CLI app, part of JMRI distribution |
| CdbBridge | `ant cdbbridge` | `ant cdbbridge-jar` | CLI tool, also standalone |

The bridge is unique in providing a standalone JAR target because it's designed to be deployed independently from JMRI.

---

**Build System Version:** 2026-02-24
**Integration:** JMRI ant targets in build.xml
