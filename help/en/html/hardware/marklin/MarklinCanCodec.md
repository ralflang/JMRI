# Märklin CAN Protocol Codec Library

## Overview

The `MarklinCanCodec` class provides a centralized library for encoding and decoding Märklin CAN (MCAN) protocol messages. This refactoring was implemented as part of issue #14104 to address code duplication and improve maintainability of the MCAN protocol implementation.

## Problem Statement

Prior to this refactoring, MCAN message encoding and decoding logic was scattered across multiple classes:

- **MarklinMessage**: Manual bit manipulation for encoding messages
- **MarklinMon**: Direct byte access for decoding and displaying messages
- **MarklinThrottle**: Direct byte access for processing loco control replies
- **MarklinTurnout**: Direct byte access for processing accessory replies

This approach had several issues:

1. **Code Duplication**: Similar decoding logic appeared in multiple places
2. **Error-Prone**: Manual bit shifting and array indexing increased bug risk
3. **Hard to Maintain**: Protocol changes required updates across multiple files
4. **Poor Readability**: Magic numbers and array indices obscured intent
5. **No Separation of Concerns**: Protocol logic was mixed with business logic

### Example of Old Approach

```java
// Old decoding in MarklinThrottle
if (m.getCommand() == MarklinConstants.LOCOSPEED) {
    int speed = m.getElement(9);
    speed = (speed << 8) + (m.getElement(10));
    // ...
}

// Old encoding in MarklinMessage
public static MarklinMessage setLocoSpeed(int addr, int speed) {
    MarklinMessage m = new MarklinMessage();
    m.setElement(0, (MarklinConstants.LOCOSPEED >> 7) & 0xFF);
    m.setElement(1, (MarklinConstants.LOCOSPEED << 1) & 0xFF);
    m.setElement(2, MarklinConstants.HASHBYTE1 & 0xFF);
    m.setElement(3, MarklinConstants.HASHBYTE2 & 0xFF);
    m.setElement(4, 0x06 & 0xFF);
    m.setElement(5, (addr >> 24) & 0xFF);
    // ... more manual byte manipulation
}
```

## Solution: MarklinCanCodec Library

The codec library provides a clean separation between protocol mechanics and application logic through three main components:

### 1. DecodedMessage Class

An immutable data structure representing a fully decoded MCAN message:

```java
MarklinCanCodec.DecodedMessage decoded = MarklinCanCodec.decode(rawMessage);

// Access decoded fields with type-safe methods
int priority = decoded.getPriority();
int command = decoded.getCommand();
boolean isResponse = decoded.isResponse();
long address = decoded.getAddress();
int speed = (decoded.getDataByte(0) << 8) + decoded.getDataByte(1);
```

### 2. MessageBuilder Class

A fluent API for constructing encoded messages:

```java
int[] encoded = MarklinCanCodec.builder()
    .setCommand(MarklinConstants.LOCOSPEED)
    .setAddress(0xC003)
    .setData((speed >> 8) & 0xff, speed & 0xff)
    .setDataLength(2)
    .build();
```

### 3. Utility Methods

Protocol analysis helpers for common operations:

```java
String priorityDesc = MarklinCanCodec.getPriorityDescription(priority);
String protocol = MarklinCanCodec.getProtocolFromAddress(address);
long baseAddr = MarklinCanCodec.getBaseAddress(address);
```

## MCAN Protocol Structure

The codec handles the 13-byte MCAN message format:

| Bytes | Field | Description |
|-------|-------|-------------|
| 0 | Priority + Command[14:7] | Priority (bits 7-6), Command high bits (bits 5-0) |
| 1 | Command[6:0] + Response | Command low bits (bits 7-1), Response flag (bit 0) |
| 2-3 | Hash | 16-bit hash/identifier |
| 4 | DLC | Data Length Code (number of data bytes + 4 address bytes) |
| 5-8 | Address | 32-bit CAN address (big-endian) |
| 9-12 | Data | 0-4 data bytes |

### Priority Levels

- **Priority 1 (0x00)**: Stop/Go/Short messages
- **Priority 2 (0x01)**: Feedback
- **Priority 3 (0x02)**: Engine Stop
- **Priority 4 (0x03)**: Engine/Accessory Commands

### Command Categories

| Range | Category | Example Commands |
|-------|----------|------------------|
| 0x00 | System | Stop (0x00), Go (0x01), Halt (0x02) |
| 0x01-0x0A | Management | Speed (0x04), Direction (0x05), Function (0x06) |
| 0x0B-0x0D | Accessory | Turnout control |
| 0x18-0x1C | Software | PING (0x18), CAN BOOT (0x1B) |
| 0x20-0x22 | GUI | GUI commands |
| 0x10-0x12 | Feedback | S88 events (0x11) |
| 0x30-0xFF | Automation | Automation commands |

## Benefits of the Refactoring

### 1. Separation of Concerns

Protocol encoding/decoding is now completely isolated from:
- **Display logic** (MarklinMon)
- **Business logic** (MarklinThrottle, MarklinTurnout)
- **Message construction** (MarklinMessage)

### 2. Improved Readability

Compare the old vs. new approach:

```java
// OLD: Direct array access with magic indices
int speed = m.getElement(9);
speed = (speed << 8) + m.getElement(10);

// NEW: Named methods with clear intent
int speed = (decoded.getDataByte(0) << 8) + decoded.getDataByte(1);
```

### 3. Enhanced Maintainability

Protocol changes only require updates in one location (the codec), automatically affecting all consumers.

### 4. Better Testability

The codec has comprehensive unit tests (`MarklinCanCodecTest.java`) that verify:
- Encoding correctness for all message types
- Decoding correctness for all message types
- Round-trip consistency (encode → decode → same values)
- Edge cases and error conditions

### 5. Reduced Error Risk

Centralizing bit manipulation logic reduces the chance of:
- Off-by-one errors in array indexing
- Incorrect bit shifting operations
- Misaligned byte ordering
- DLC calculation errors

### 6. Type Safety

`DecodedMessage` provides typed access to protocol fields instead of raw array indices:

```java
// OLD: Easy to mix up byte 9 vs byte 10
int value = m.getElement(9);  // Which field is this?

// NEW: Clear what field you're accessing
int function = decoded.getDataByte(0);
int state = decoded.getDataByte(1);
```

## Usage Examples

### Decoding Messages (MarklinMon, MarklinThrottle, MarklinTurnout)

```java
@Override
public void reply(MarklinReply m) {
    // Decode once
    int[] rawData = new int[13];
    for (int i = 0; i < 13; i++) {
        rawData[i] = m.getElement(i);
    }
    MarklinCanCodec.DecodedMessage decoded = MarklinCanCodec.decode(rawData);

    // Work with structured data
    if (decoded.getCommand() == MarklinConstants.LOCOSPEED) {
        int speed = (decoded.getDataByte(0) << 8) + decoded.getDataByte(1);
        processSpeed(speed);
    }
}
```

### Encoding Messages (MarklinMessage)

```java
public static MarklinMessage setLocoSpeed(int addr, int speed) {
    int[] encoded = MarklinCanCodec.builder()
        .setCommand(MarklinConstants.LOCOSPEED)
        .setAddress(addr)
        .setData((speed >> 8) & 0xff, speed & 0xff)
        .setDataLength(2)
        .build();
    return new MarklinMessage(encoded);
}
```

### Display Logic (MarklinMon)

```java
private static void appendCommand(MarklinCanCodec.DecodedMessage decoded, StringBuilder sb) {
    sb.append(" Command: ");
    if (decoded.getCommand() == MarklinConstants.LOCOSPEED) {
        int speed = (decoded.getDataByte(0) << 8) + decoded.getDataByte(1);
        sb.append("Change of speed ").append(speed);
    }
    // ...
}
```

## Performance Considerations

The codec introduces minimal overhead:

- **Memory**: One additional object allocation per decode (DecodedMessage)
- **CPU**: One array copy operation per decode
- **Trade-off**: Negligible performance cost for significant maintainability gain

In the context of model railroad control systems where message rates are relatively low (typically < 1000 messages/second), this overhead is insignificant compared to network and hardware latency.

## Migration Notes

All existing MCAN protocol code has been migrated to use the codec:

- ✅ **MarklinMessage**: All factory methods now use `MessageBuilder`
- ✅ **MarklinMon**: Display logic now uses `DecodedMessage`
- ✅ **MarklinThrottle**: Reply processing now uses `DecodedMessage`
- ✅ **MarklinTurnout**: Reply processing now uses `DecodedMessage`

No API changes were made to public interfaces, so external code continues to work without modification.

## References

- **Issue**: [#14104 - Märklin CAN Boot Protocol Fix](https://github.com/JMRI/JMRI/issues/14104)
- **Protocol Documentation**:
  - [CS2 CAN Protocol 1.0](https://www.maerklin.de/fileadmin/media/produkte/CS2_can-protokoll_1-0.pdf)
  - [CS2 CAN Protocol 2.0](https://streaming.maerklin.de/public-media/cs2/cs2CAN-Protokoll-2_0.pdf)
- **Source Code**:
  - `java/src/jmri/jmrix/marklin/MarklinCanCodec.java`
  - `java/test/jmri/jmrix/marklin/MarklinCanCodecTest.java`

## Future Enhancements

Possible future improvements to the codec:

1. **Validation**: Add message validation to detect malformed packets
2. **Builder Presets**: Add factory methods for common message patterns
3. **Streaming API**: Support for processing message streams efficiently
4. **Protocol Version Detection**: Auto-detect protocol version from messages
5. **Extended Logging**: Built-in support for detailed protocol tracing

## Conclusion

The `MarklinCanCodec` library represents a significant improvement in code quality and maintainability for JMRI's Märklin CAN protocol implementation. By centralizing protocol logic, improving readability, and reducing error potential, this refactoring makes the codebase more robust and easier to maintain for future development.
