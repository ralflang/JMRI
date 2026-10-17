package jmri.jmrix.marklin;

/**
 * State machine enum representing the lifecycle of an MCAN bus device.
 */
public enum McanDeviceState {
    /** Ping response received, identity created. */
    DISCOVERED,
    /** Optional post-discovery parameter enrichment in progress. */
    INITIALIZING,
    /** Fully initialized and actively communicating on the bus. */
    ONLINE,
    /** No frame received within configured staleness threshold. */
    STALE,
    /** Disconnected or missing from the bus. */
    OFFLINE
}
