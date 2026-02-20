# CC-Schnitte Bridge Build Options

## Overview

The CC-Schnitte bridge can be built in two modes:

1. **Shared Libraries (Default)** - Small JAR with external dependencies
2. **Standalone Fat JAR (Optional)** - Single self-contained JAR

## Build Modes

### 1. Shared Libraries Build (Default)

**Command:**
```bash
./build.sh
```

**Output:**
- `marklin-cdb-bridge.jar` (14 KB)
- `jSerialComm-2.11.4.jar` (866 KB)
- `slf4j-api-2.0.17.jar` (69 KB)
- `log4j-slf4j2-impl-2.25.3.jar` (30 KB)
- `log4j-api-2.25.3.jar` (343 KB)
- `log4j-core-2.25.3.jar` (2.0 MB)

**Total:** 6 files, 3.3 MB

**Characteristics:**
- ✅ Small bridge JAR (only 14 KB)
- ✅ Shares libraries with JMRI installation
- ✅ Standard JMRI build pattern
- ✅ Easy to update individual libraries
- ⚠️ Requires all 6 files in same directory

**Use When:**
- Building as part of JMRI distribution
- Deploying alongside JMRI
- Library versions may be updated independently

### 2. Standalone Fat JAR Build (Opt-In)

**Command:**
```bash
./build.sh fatjar
```

**Output:**
- `marklin-cdb-bridge-standalone.jar` (3.2 MB)

**Total:** 1 file, 3.2 MB

**Characteristics:**
- ✅ Single self-contained file
- ✅ No external dependencies
- ✅ Easy distribution (one file)
- ✅ Works anywhere with Java
- ⚠️ Larger file size
- ⚠️ Duplicate libraries if used with JMRI

**Use When:**
- Distributing to end users
- Standalone deployment (no JMRI)
- Simple installation preferred
- Network/remote deployments

## Comparison

| Feature | Shared Libraries | Fat JAR |
|---------|-----------------|---------|
| **Files** | 6 files | 1 file |
| **Total Size** | 3.3 MB | 3.2 MB |
| **Bridge JAR** | 14 KB | 3.2 MB |
| **Dependencies** | External | Embedded |
| **Distribution** | Copy 6 files | Copy 1 file |
| **Updates** | Update individual libs | Rebuild entire JAR |
| **JMRI Integration** | Standard pattern | Non-standard |
| **Ease of Use** | Moderate | Very easy |

## Technical Details

### Shared Libraries Build

**Implementation:**
- JAR manifest includes `Class-Path` entries
- References dependency JARs by filename
- Java loads dependencies from same directory

**Manifest:**
```
Main-Class: jmri.jmrix.marklin.cdb.bridge.CdbSerialToTcpBridge
Class-Path: jSerialComm-2.11.4.jar slf4j-api-2.0.17.jar log4j-slf4j2-impl-2.25.3.jar log4j-api-2.25.3.jar log4j-core-2.25.3.jar
```

### Fat JAR Build

**Implementation:**
- Extracts all dependency JARs
- Merges all classes into single JAR
- Excludes signature files (META-INF/*.SF, *.RSA, *.DSA)

**Process:**
1. Compile bridge classes
2. Extract dependency JARs: `unzip -q -o <jar>`
3. Create single JAR with all classes: `jar cfm ... -C . .`
4. Result: Self-contained executable

**Included:**
- Bridge classes: `jmri/jmrix/marklin/cdb/bridge/*.class`
- jSerialComm classes: `com/fazecast/...`
- SLF4J classes: `org/slf4j/...`
- Log4j classes: `org/apache/logging/...`
- Platform natives: `Android/`, `Linux/`, `Windows/`, `OSX/`, etc.

## Usage

Both build modes produce functionally identical JARs:

**Shared Libraries:**
```bash
java -jar marklin-cdb-bridge.jar --port COM3
```
*(Requires dependency JARs in same directory)*

**Fat JAR:**
```bash
java -jar marklin-cdb-bridge-standalone.jar --port COM3
```
*(No other files needed)*

## Recommendations

### For JMRI Distribution
**Use:** Shared Libraries (default)

**Rationale:**
- Follows JMRI build patterns
- Efficient if JMRI already includes libraries
- Easier to update individual components

### For End Users
**Use:** Fat JAR

**Rationale:**
- Single file to download
- No installation complexity
- Works anywhere

### For Developers
**Use:** Shared Libraries

**Rationale:**
- Faster build times
- See library updates immediately
- Standard development workflow

## Build Script Integration

The build script follows JMRI conventions:

**Default behavior:**
```bash
./build.sh          # Shared libraries (JMRI standard)
```

**Explicit opt-in:**
```bash
./build.sh fatjar   # Standalone fat JAR
```

This matches JMRI's pattern where:
- Default targets follow standard practices
- Special builds require explicit selection
- No ambiguity about what gets built

## Future Integration

### Potential Ant Target

Could add to JMRI's `build.xml`:

```xml
<target name="cdb-bridge"
        description="Build CC-Schnitte bridge (shared libs)">
    <exec executable="bash">
        <arg value="java/src/jmri/jmrix/marklin/cdb/bridge/build.sh"/>
    </exec>
</target>

<target name="cdb-bridge-standalone"
        description="Build CC-Schnitte bridge (fat JAR)">
    <exec executable="bash">
        <arg value="java/src/jmri/jmrix/marklin/cdb/bridge/build.sh"/>
        <arg value="fatjar"/>
    </exec>
</target>
```

Then users could run:
```bash
ant cdb-bridge              # Default (shared libs)
ant cdb-bridge-standalone   # Fat JAR
```

---

**Summary:** Default shared library build follows JMRI standards. Fat JAR is explicit opt-in for special use cases.
