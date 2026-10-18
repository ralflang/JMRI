# MCAN Device Model Action Plan

This document breaks down the **MCAN Device Model Specification** (`help/en/html/hardware/marklin/MCAN-Device-Model-Spec.shtml`) into an actionable implementation roadmap for JMRI.

---

## Overview

The MCAN Device Model introduces a first-class JMRI representation for devices discovered on Märklin CAN (MCAN) and CAN-digital-Bahn (CdB) buses. It unifies device identity, state management, parameter access, and UI representation within JMRI's `NamedBean` / `Manager` architecture while keeping low-level protocol logic zero-dependency.

---

## 1. Domain Model Core (`McanDeviceIdentity`)
- Define immutable value objects representing device identity.
- Support Native MCAN devices identified by 32-bit UID.
- Support CdB modules identified by CAN Class and Module ID.
- Keep `McanDeviceIdentity` zero-dependency (no JMRI imports).

## 2. Frame Assembly & Bus Transport (`CdBMessage` & `McanFrameSender`)
- Implement `CdBMessage` factory methods to construct CdB config frames (Ping, Ping Reply, Read, Write, Reset).
- Create `@FunctionalInterface McanFrameSender` abstraction for frame transmission.
- Support parameter encoding/decoding for CdB 16-bit address offsets.

## 3. Device Bean & Lifecycle (`McanDeviceState` & `McanDevice`)
- Define `McanDeviceState` enum: `DISCOVERED`, `INITIALIZING`, `ONLINE`, `STALE`, `OFFLINE`.
- Implement `McanDevice` extending `AbstractNamedBean`.
- Manage property changes on Swing EDT (`ThreadingUtil.runOnGUI`).
- Expose system name `M:dev:<type>:<id>` and user name.

## 4. Device Manager & Listener Integration (`McanDeviceManager`)
- Implement `McanDeviceManager` extending `AbstractManager<McanDevice>`.
- Override `getSystemNamePrefix()` returning `"M:dev:"`.
- Implement `MarklinListener` and `CdBConfigListener` to listen to bus frames and discover/update devices.
- Register `McanDeviceManager` in `MarklinSystemConnectionMemo`.

## 5. Parameter Access Service (`McanParameterManager`)
- Create `McanParameterManager` abstraction for asynchronous parameter read/write operations.
- Implement `CdBConfigListener` to complete `CompletableFuture`s on receiving `READ_REPLY` frames.
- Support timeout and retry logic for robust parameter exchange.

## 6. Swing Table Model (`McanDeviceTableDataModel`)
- Create `McanDeviceTableDataModel` in `jmri.jmrix.marklin.swing.devicetable`.
- Implement `ManagerDataListener<McanDevice>` and `PropertyChangeListener` to update UI dynamically on device state or parameter changes.
