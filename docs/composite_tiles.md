# Composite Track Tiles Architecture

This document describes the architectural approach for handling track tiles that have inherent endpoint properties, such as buffer stops, bumpers, or dead ends.

## Problem Statement

Track tiles with inherent endpoints create a conceptual mismatch between physical reality and JMRI's object model:

- **Physical Reality**: A "buffer stop tile" is a single unit with both track length and endpoint functionality
- **JMRI Architecture**: Track segments and endpoints are separate objects (TrackSegment + PositionablePoint with END_BUMPER type)

This mismatch leads to complexity in tile placement and XML representation.

## Evaluated Options

### Option 1: Composite Approach with Auto-Creation ✅ **SELECTED**

**Concept**: Track tiles describe physical reality, tile placement auto-creates appropriate JMRI objects.

**XML Representation**:
```xml
<tracktile vendor="Märklin" family="C-Track" type="buffer-stop-24077">
    <geometry>
        <length>77.5</length>
        <endpoint type="buffer" position="end"/>
    </geometry>
</tracktile>

<!-- In layout XML, auto-create both objects -->
<tracksegment id="TS1" tile="buffer-stop-24077"/>
<positionablepoint id="PP1" type="END_BUMPER" connect1="TS1"/>
```

**Code Implementation**:
```java
// When placing tile with endpoint
TrackTile tile = getTrackTile("buffer-stop-24077");
if (tile.hasInherentEndpoint()) {
    TrackSegment segment = createTrackSegment(tile);
    PositionablePoint bumper = createEndBumper();
    autoConnect(segment, bumper, tile.getEndpointPosition());
}
```

### Option 2: Extended TrackSegment

**Concept**: Extend TrackSegment class to include endpoint type information.

```java
class TrackSegment {
    private EndpointType connect1Type = EndpointType.NORMAL; // NORMAL, BUFFER, FIXED_BUFFER
    private EndpointType connect2Type = EndpointType.NORMAL;
}
```

**Issues**:
- Requires significant changes to existing JMRI architecture
- Breaks compatibility with existing code that expects separate objects
- Complicates the TrackSegment class responsibilities

### Option 3: Specialized Track Classes

**Concept**: Create specialized track classes for different endpoint types.

```java
class BufferStopTrack extends TrackSegment {
    // Inherent endpoint behavior
}
```

**Issues**:
- Proliferates class hierarchy
- Still doesn't align with JMRI's operational model
- Complicates polymorphic handling

## Selected Approach: Option 1 - Composite with Auto-Creation

### Rationale

Option 1 was selected as the winning proposal because it provides the best balance of:

1. **Semantic Accuracy**: XML representation reflects real-world track pieces accurately
2. **JMRI Compatibility**: Maintains existing object model and operational semantics
3. **User Transparency**: Users place one tile and get the correct JMRI structure automatically
4. **Extensible Design**: Works for other endpoint types (turntable connections, crossings, etc.)
5. **Minimal Disruption**: No changes required to core JMRI track handling code

### Implementation Benefits

- **Tile Catalogs**: Can accurately represent manufacturer track pieces
- **Layout Placement**: Single tile placement creates appropriate object structure
- **XML Storage**: Clear separation between tile definition and layout instance
- **Backwards Compatibility**: Existing layouts continue to work unchanged
- **Future Extensibility**: Pattern works for other composite tiles (crossings with signals, etc.)

### XML Schema Extension

```xml
<xs:element name="endpoint">
    <xs:complexType>
        <xs:attribute name="type" type="EndpointType"/> <!-- buffer, turntable, signal, etc -->
        <xs:attribute name="position" type="EndPosition"/> <!-- start, end, both -->
    </xs:complexType>
</xs:element>

<xs:simpleType name="EndpointType">
    <xs:restriction base="xs:string">
        <xs:enumeration value="buffer"/>
        <xs:enumeration value="bumper"/>
        <xs:enumeration value="turntable"/>
        <xs:enumeration value="signal"/>
    </xs:restriction>
</xs:simpleType>

<xs:simpleType name="EndPosition">
    <xs:restriction base="xs:string">
        <xs:enumeration value="start"/>
        <xs:enumeration value="end"/>
        <xs:enumeration value="both"/>
    </xs:restriction>
</xs:simpleType>
```

### Usage Examples

**Buffer Stop Tile**:
```xml
<tracktile vendor="Märklin" family="C-Track" type="24077">
    <geometry>
        <length>77.5</length>
        <endpoint type="buffer" position="end"/>
    </geometry>
</tracktile>
```

**Double Buffer (maintenance track)**:
```xml
<tracktile vendor="Peco" family="Code-100" type="SL-E295">
    <geometry>
        <length>168</length>
        <endpoint type="buffer" position="both"/>
    </geometry>
</tracktile>
```

**Turntable Connection Track**:
```xml
<tracktile vendor="Fleischmann" family="Profi" type="6152">
    <geometry>
        <length>104.2</length>
        <endpoint type="turntable" position="start"/>
    </geometry>
</tracktile>
```

## Implementation Status

- **Status**: Architecture approved
- **Next Steps**:
  1. Extend TrackTile XML schema with endpoint elements
  2. Implement auto-creation logic in tile placement code
  3. Update tile catalogs with endpoint information
  4. Create unit tests for composite tile scenarios

## Related Documentation

- [Track Tile Architecture](../docs/geometry.md)
- [JMRI Layout Editor Architecture](../.github/copilot-instructions.md)
- [Turnout Paths and Routes](../docs/turnout_paths.md)
