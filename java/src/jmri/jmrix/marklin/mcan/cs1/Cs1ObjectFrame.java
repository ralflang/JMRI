package jmri.jmrix.marklin.mcan.cs1;

import jmri.jmrix.marklin.mcan.RawFrame;

/**
 * Shared header access for operating frames.
 * Object handles are session assigned references.
 */
public abstract class Cs1ObjectFrame extends Cs1Frame {

    protected Cs1ObjectFrame(RawFrame raw) { super(raw); }

    /** @return identifier bits 25-10 */
    public final int objectHandle() { return (canId() >> 10) & 0xFFFF; }

    /** @return identifier bits 9-7 */
    public final int commandBits() { return (canId() >> 7) & 7; }

    /** @return true for an even, master-side assigned node */
    public final boolean fromMaster() { return (node() & 1) == 0; }
}
