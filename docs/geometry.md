# JMRI Layout Editor Track Geometry Architecture

This document describes the abstraction architecture for track geometry calculations in JMRI's Layout Editor, focusing on orientation at anchor points and path length calculations for tiled track elements.

## Overview

The geometry system provides a unified interface for calculating track properties across all track types, supporting both tiled and non-tiled track elements through a polymorphic design.

## Core Concepts

### Track Elements Hierarchy
```
LayoutTrack (abstract base)
├── TrackSegment (2 anchors: A,B - 1 path: AB)
├── LayoutTurnout (3 anchors: A,B,C - 2 paths: AB,AC)
├── LayoutSlip (4 anchors: A,B,C,D - 4 paths: AC,BD,AD,BC)
└── LevelXing (4 anchors: A,B,C,D - 2 paths: AB,CD)
```

### Tiled vs Non-Tiled Tracks
- **Tiled LayoutTracks**: Have associated TrackTile with geometry data (radius, arc, length)
- **Non-Tiled LayoutTracks**: Use default/manual geometry or return default values
- **Mixed Support**: Same API works for both types

## LayoutTrack Base Class API

### Abstract Methods
```java
abstract class LayoutTrack {
    // Tile support detection
    public abstract boolean isTiled();

    // Topology definition
    public abstract List<String> getAnchorPoints(); // ["A", "B"] for segments
    public abstract List<String> getPathIdentifiers(); // ["AB"] for segments

    // Core geometry calculations
    public abstract double getOrientationAtAnchor(String anchor, LayoutEditor le);
    public abstract double getPathLength(String pathId);

    // Internal support methods
    protected abstract String findPathForAnchor(String anchor);
    protected abstract Point2D getAnchorCoordinates(String anchor, LayoutEditor le);
    protected abstract Point2D getOtherEndpoint(String pathId, String anchor, LayoutEditor le);
}
```

### Base Implementation Strategy
```java
// Common path length calculation
public double getPathLength(String pathId) {
    if (!isTiled()) return 0.0;

    TrackTilePath path = getPathFromTile(pathId);
    if (path == null) return 0.0;

    if (path.isStraight()) {
        return path.getLength();
    } else if (path.isCurved()) {
        return LayoutTileGeometry.calculateArcLength(path.getRadius(), path.getArc());
    }
    return 0.0;
}

// Common orientation calculation
public double getOrientationAtAnchor(String anchor, LayoutEditor le) {
    if (!isTiled()) return -1.0;

    String pathId = findPathForAnchor(anchor);
    TrackTilePath path = getPathFromTile(pathId);

    Point2D anchorPoint = getAnchorCoordinates(anchor, le);
    Point2D otherPoint = getOtherEndpoint(pathId, anchor, le);

    if (path.isStraight()) {
        return LayoutTileGeometry.calculateStraightOrientation(anchorPoint, otherPoint);
    } else if (path.isCurved()) {
        Point2D center = calculatePathCenter(path, anchorPoint, otherPoint);
        return LayoutTileGeometry.calculateCurvedOrientation(
            anchorPoint, center, path.getArc(), path.isFlipped());
    }
    return -1.0;
}
```

## Track Type Implementations

### TrackSegment (Simple Case)
```java
class TrackSegment extends LayoutTrack {
    public List<String> getAnchorPoints() {
        return Arrays.asList("A", "B");
    }

    public List<String> getPathIdentifiers() {
        return Arrays.asList("AB");
    }

    protected String findPathForAnchor(String anchor) {
        return "AB"; // Only one path connects both anchors
    }

    // Convenience methods delegate to base class
    public double getDefaultPathLength() {
        return getPathLength("AB");
    }

    public double getConnect1Orientation(LayoutEditor le) {
        return getOrientationAtAnchor("A", le);
    }

    public double getConnect2Orientation(LayoutEditor le) {
        return getOrientationAtAnchor("B", le);
    }
}
```

### LayoutTurnout (Multi-Path Case)
```java
class LayoutTurnout extends LayoutTrack {
    public List<String> getAnchorPoints() {
        return Arrays.asList("A", "B", "C");
    }

    public List<String> getPathIdentifiers() {
        return Arrays.asList("AB", "AC");
    }

    protected String findPathForAnchor(String anchor) {
        switch (anchor) {
            case "A": return getCurrentPathFromA(); // "AB" or "AC" based on turnout state
            case "B": return "AB";
            case "C": return "AC";
            default: return null;
        }
    }

    // Convenience methods
    public double getAnchorPointAOrientation(LayoutEditor le) {
        return getOrientationAtAnchor("A", le);
    }

    public double getPathABLength() {
        return getPathLength("AB");
    }

    public double getPathACLength() {
        return getPathLength("AC");
    }
}
```

## Geometry Calculation Framework

### Pure Geometry Functions (LayoutTileGeometry)
```java
public class LayoutTileGeometry {
    // Testable with minimal inputs
    public static double calculateStraightOrientation(Point2D endpoint1, Point2D endpoint2);
    public static double calculateCurvedOrientation(Point2D endpoint, Point2D center,
                                                   double arc, boolean isFlip);
    public static double calculateArcLength(double radius, double arcDegrees);
    public static Point2D calculateCircleCenter(Point2D endpoint1, Point2D endpoint2, double radius);
}
```

### Path Geometry Types
- **Straight Paths**: Defined by length, orientation calculated from endpoints
- **Curved Paths**: Defined by radius and arc angle, orientation calculated as tangent to circle
- **Future Extensions**: Framework ready for splines, compound curves, transition curves

## Path Identification Strategy

### Naming Convention
- **Two-letter codes**: "AB" = path from anchor A to anchor B
- **Consistent across types**: Same pattern for all track elements
- **State-independent**: Path names don't change with turnout position

### Path-to-Anchor Mapping
| Track Type | Anchors | Paths | Path Routing |
|------------|---------|-------|--------------|
| TrackSegment | A, B | AB | A↔B |
| RH/LH Turnout | A, B, C | AB, AC | A→B (normal), A→C (thrown) |
| Wye Turnout | A, B, C | AB, AC | A→B, A→C (both diverging) |
| Double XOver | A, B, C, D | AB, CD, AC, BD | A↔B, C↔D, A↔C, B↔D |
| Level Crossing | A, B, C, D | AB, CD | A↔B (straight), C↔D (crossing) |

## Implementation Benefits

### Testability
```java
@Test
void testStraightOrientation() {
    Point2D p1 = new Point2D.Double(0, 0);
    Point2D p2 = new Point2D.Double(100, 0);
    double orientation = LayoutTileGeometry.calculateStraightOrientation(p1, p2);
    assertEquals(0.0, orientation, 0.01); // East direction
}

@Test
void testTrackSegmentPathLength() {
    TrackSegment segment = createTestSegment();
    segment.setTrackTile(createStraightTile(1000)); // 1000mm tile
    assertEquals(1000.0, segment.getPathLength("AB"), 0.01);
}
```

### Code Reuse
- **Geometry calculations**: Shared across all track types through LayoutTileGeometry
- **Tile data access**: Common pattern in base class
- **Error handling**: Consistent null checks and fallback values

### Extensibility
- **New track types**: Inherit base geometry, define topology
- **New geometry types**: Add to LayoutTileGeometry, automatic support in all tracks
- **Enhanced tile data**: Modify TrackTilePath, calculations update automatically

## Migration Strategy

### Phase 1: Base Class Enhancement
1. Add abstract methods to LayoutTrack
2. Implement base geometry calculations using tile data
3. Add LayoutTileGeometry pure functions

### Phase 2: TrackSegment Refactoring
1. Implement LayoutTrack abstract methods in TrackSegment
2. Refactor existing orientation methods to use base class
3. Update TrackSegmentEditor to use model methods

### Phase 3: Turnout Implementation
1. Extend LayoutTurnout with multi-path support
2. Implement path-to-anchor mapping for turnout types
3. Add turnout orientation display in editor

### Phase 4: Complete Track Support
1. Implement remaining track types (slips, crossings)
2. Add specialized path routing logic where needed
3. Deprecate view-based geometry calculations

## Future Enhancements

### Advanced Geometry Types
- **Transition curves**: Gradual radius change (clothoid, spiral)
- **Compound curves**: Multiple radius segments in one path
- **Vertical geometry**: Grade and elevation calculations
- **3D curves**: Banking and superelevation

### Performance Optimizations
- **Cached calculations**: Store computed values in track objects
- **Batch operations**: Calculate multiple orientations efficiently
- **Lazy evaluation**: Compute geometry only when needed

### Integration Points
- **Path routing algorithms**: Use orientation for connection matching
- **Signal placement**: Automatic positioning based on track geometry
- **Export/import**: Preserve calculated geometry in external formats
- **Collision detection**: Use path geometry for clearance calculations

*Generated for JMRI feat/14621-tiled-tracks branch on November 30, 2025*
