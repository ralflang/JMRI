#!/bin/bash
# Build script for CC-Schnitte (CanDigitalBahn) Serial-to-TCP Bridge
# Bridges third-party CC-Schnitte adapters to Märklin CS2 TCP protocol
#
# Author: Ralf Lang (ralf.lang@ralf-lang.de)
# Copyright: (C) 2026 JMRI Community
#
# Usage:
#   ./build.sh          - Build with shared libraries (default)
#   ./build.sh fatjar   - Build standalone fat JAR with embedded dependencies

BUILD_TYPE="${1:-shared}"

echo "Building CC-Schnitte Serial-to-TCP Bridge..."
echo "Build type: $BUILD_TYPE"
echo

# Check if JMRI_HOME is set
if [ -z "$JMRI_HOME" ]; then
    # From java/src/jmri/jmrix/marklin/cdb/bridge, go up to JMRI root
    JMRI_HOME="$(cd "$(dirname "$0")/../../../../../../.." && pwd)"
    echo "JMRI_HOME not set, using: $JMRI_HOME"
fi

cd "$JMRI_HOME" || exit 1

# Create output directory
mkdir -p target/bridge

# Find required JARs
JSERIALCOMM=$(find lib -name "jSerialComm-*.jar" | head -1)
SLF4J_API=$(find lib -name "slf4j-api-*.jar" | head -1)
LOG4J_IMPL=$(find lib -name "log4j-slf4j2-impl-*.jar" | head -1)
LOG4J_API=$(find lib -name "log4j-api-*.jar" | head -1)
LOG4J_CORE=$(find lib -name "log4j-core-*.jar" | head -1)

if [ -z "$JSERIALCOMM" ] || [ -z "$SLF4J_API" ] || [ -z "$LOG4J_IMPL" ]; then
    echo "Error: Required libraries not found in lib/"
    echo "  jSerialComm: $JSERIALCOMM"
    echo "  slf4j-api: $SLF4J_API"
    echo "  log4j-slf4j2-impl: $LOG4J_IMPL"
    echo "  log4j-api: $LOG4J_API"
    echo "  log4j-core: $LOG4J_CORE"
    exit 1
fi

# Use semicolon for Windows classpath, colon for Unix
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
    CLASSPATH="$JSERIALCOMM;$SLF4J_API;$LOG4J_IMPL;$LOG4J_API;$LOG4J_CORE"
else
    CLASSPATH="$JSERIALCOMM:$SLF4J_API:$LOG4J_IMPL:$LOG4J_API:$LOG4J_CORE"
fi

echo "Using libraries:"
echo "  $JSERIALCOMM"
echo "  $SLF4J_API"
echo "  $LOG4J_IMPL"
echo "  $LOG4J_API"
echo "  $LOG4J_CORE"
echo

# Compile bridge (codec and formatter already compiled by JMRI)
echo "Compiling bridge..."

# First, ensure JMRI classes are compiled
if [ ! -f "target/classes/jmri/jmrix/marklin/MarklinCanCodec.class" ]; then
    echo "JMRI classes not found, compiling JMRI first..."
    ant compile >/dev/null 2>&1
    if [ $? -ne 0 ]; then
        echo "Error: JMRI compilation failed"
        exit 1
    fi
fi

# Compile bridge with JMRI classes in classpath
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
    FULL_CLASSPATH="$CLASSPATH;target/classes"
else
    FULL_CLASSPATH="$CLASSPATH:target/classes"
fi

javac -d target/bridge \
      -cp "$FULL_CLASSPATH" \
      -source 11 -target 11 \
      java/src/jmri/jmrix/marklin/cdb/bridge/CdbSerialToTcpBridge.java

if [ $? -ne 0 ]; then
    echo "Compilation failed"
    exit 1
fi

# Copy needed JMRI classes to bridge output
echo "Copying JMRI classes to bridge..."
mkdir -p target/bridge/jmri/jmrix/marklin
cp -r target/classes/jmri/jmrix/marklin/MarklinCanCodec*.class target/bridge/jmri/jmrix/marklin/
cp target/classes/jmri/jmrix/marklin/MarklinConstants.class target/bridge/jmri/jmrix/marklin/
cp target/classes/jmri/jmrix/marklin/MarklinMessageFormatter*.class target/bridge/jmri/jmrix/marklin/

cd target/bridge || exit 1

if [ "$BUILD_TYPE" = "fatjar" ]; then
    # Build fat JAR with embedded dependencies
    echo "Creating standalone fat JAR..."

    # Extract all dependency JARs (use absolute paths)
    echo "Extracting dependencies..."
    for jar in "$JMRI_HOME/$JSERIALCOMM" "$JMRI_HOME/$SLF4J_API" "$JMRI_HOME/$LOG4J_IMPL" "$JMRI_HOME/$LOG4J_API" "$JMRI_HOME/$LOG4J_CORE"; do
        unzip -q -o "$jar" -x 'META-INF/*.SF' 'META-INF/*.RSA' 'META-INF/*.DSA' 'META-INF/MANIFEST.MF'
    done

    # Create manifest for fat JAR
    cat > manifest-fatjar.txt <<EOF
Main-Class: jmri.jmrix.marklin.cdb.bridge.CdbSerialToTcpBridge

EOF

    # Create fat JAR with all classes (use . to include everything)
    jar cfm ../../marklin-cdb-bridge-standalone.jar manifest-fatjar.txt \
        -C . . 2>/dev/null

    cd ../.. || exit 1

    echo
    echo "Build complete!"
    echo
    echo "Output:"
    ls -lh marklin-cdb-bridge-standalone.jar 2>/dev/null
    echo
    echo "This is a standalone JAR with all dependencies embedded."
    echo "No other files needed!"
    echo
    echo "Usage:"
    echo "  Single device:    java -jar marklin-cdb-bridge-standalone.jar --port COM3"
    echo "  Custom TCP port:  java -jar marklin-cdb-bridge-standalone.jar --port COM3:50000"
    echo "  Multiple devices: java -jar marklin-cdb-bridge-standalone.jar --port COM2,COM3:49999,COM4:50000"
    echo "  Help:             java -jar marklin-cdb-bridge-standalone.jar --help"

else
    # Build with shared libraries (default)
    echo "Creating JAR with shared libraries..."

    # Create manifest with classpath
    cat > manifest.txt <<EOF
Main-Class: jmri.jmrix.marklin.cdb.bridge.CdbSerialToTcpBridge
Class-Path: jSerialComm-2.11.4.jar slf4j-api-2.0.17.jar log4j-slf4j2-impl-2.25.3.jar log4j-api-2.25.3.jar log4j-core-2.25.3.jar

EOF

    jar cfm ../../marklin-cdb-bridge.jar manifest.txt \
        jmri/jmrix/marklin/cdb/bridge/*.class \
        jmri/jmrix/marklin/MarklinCanCodec*.class \
        jmri/jmrix/marklin/MarklinConstants.class \
        jmri/jmrix/marklin/MarklinMessageFormatter.class \
        jmri/jmrix/marklin/MarklinMessageFormatter\$I18nProvider.class

    cd ../.. || exit 1

    # Copy dependencies
    echo "Copying dependencies..."
    cp "$JSERIALCOMM" .
    cp "$SLF4J_API" .
    cp "$LOG4J_IMPL" .
    cp "$LOG4J_API" .
    cp "$LOG4J_CORE" .

    echo
    echo "Build complete!"
    echo
    echo "Output:"
    ls -lh marklin-cdb-bridge.jar 2>/dev/null
    echo
    echo "Dependencies (must be in same directory):"
    ls -lh jSerialComm-*.jar slf4j-*.jar log4j-*.jar 2>/dev/null
    echo
    echo "Usage:"
    echo "  Single device:    java -jar marklin-cdb-bridge.jar --port COM3"
    echo "  Custom TCP port:  java -jar marklin-cdb-bridge.jar --port COM3:50000"
    echo "  Multiple devices: java -jar marklin-cdb-bridge.jar --port COM2,COM3:49999,COM4:50000"
    echo "  Help:             java -jar marklin-cdb-bridge.jar --help"
    echo
    echo "To build standalone fat JAR: ./build.sh fatjar"
fi
