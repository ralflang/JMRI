package jmri.jmrix.marklin.mcan.cs1;

import jmri.jmrix.marklin.mcan.RawFrame;

/** Bus supervision */
public final class Cs1SystemCommands {

    private Cs1SystemCommands() {}

    static Cs1Frame parse(Cs1Frame frame) {
        if (frame.canId() == 0x380) {
            return frame.dlc() == 8 ? new BusProbeFrame(frame.raw()) : frame;
        }
        if (frame.priority() == 3) {
            if (frame.dlc() == 4 && frame.data16(0) == 0) {
                return new KeepAliveFrame(frame.raw());
            }
            if (frame.dlc() == 8 && frame.data16(0) == 0) {
                return new KeepAliveReplyFrame(frame.raw());
            }
        }
        return frame;
    }

    /** MS1 master's bus occupied probe. This is not a CS2 PING. */
    public static final class BusProbeFrame extends Cs1Frame {
        BusProbeFrame(RawFrame raw) { super(raw); }
    }

    /** Poll from either side. The master also polls after a delayed cycle. */
    public static final class KeepAliveFrame extends Cs1ObjectFrame {
        KeepAliveFrame(RawFrame raw) { super(raw); }
        public int targetNode() { return data16(2); }
    }

    /** Identity, poll or reset from either side. UID role requires session context. */
    public static final class KeepAliveReplyFrame extends Cs1ObjectFrame {
        KeepAliveReplyFrame(RawFrame raw) { super(raw); }
        public int targetNode() { return data16(2); }
        public long deviceUid() { return data32(4); }
        public boolean zeroUid() { return deviceUid() == 0; }
    }
}
