package jmri.jmrix.marklin.mcan.cs1;

import jmri.jmrix.marklin.mcan.RawFrame;

/**
 * Firmware frame receiving.
 * <p>
 */
public final class Cs1FirmwareCommands {

    private Cs1FirmwareCommands() {}

    static Cs1Frame parse(Cs1Frame frame) {
        int handle = (frame.canId() >> 18) & 0xFF;
        int request = (frame.canId() >> 15) & 7;
        int stage = (frame.canId() >> 10) & 0x1F;
        int flags = (frame.canId() >> 7) & 7;
        int dlc = frame.dlc();
        if (request == 5 && handle == 0 && stage == 0 && flags == 0 && dlc == 4) {
            return new FlashReadyFrame(frame.raw());
        }
        if (request != 0) {
            return frame;
        }
        if (flags == 0) {
            if (stage == 0 && dlc == 4) {
                return handle == 0 ? new FlashAbortFrame(frame.raw())
                        : new FlashAuthorizeFrame(frame.raw());
            }
            if (handle != 0 && stage >= 1 && stage <= 3) {
                if (dlc == 8) {
                    return new IdentityReplyFrame(frame.raw());
                }
                // Stage 3/DLC 4 is both an identity query and completion in the trace.
                if (dlc == 4 && stage < 3) {
                    return new IdentityQueryFrame(frame.raw());
                }
            }
        }
        if (handle != 0 && stage == 0) {
            if (flags == 1 && dlc == 8) {
                return new FlashDataQueryFrame(frame.raw());
            }
            if (flags == 2 && dlc >= 5 && dlc <= 8) {
                return new FlashDataFrame(frame.raw());
            }
            if (flags == 3 && dlc == 4) {
                return new FlashEndFrame(frame.raw());
            }
        }
        return frame;
    }

    /** Firmware specific header. Bit 7 is not the registration direction flag. */
    public abstract static class FirmwareFrame extends Cs1Frame {
        FirmwareFrame(RawFrame raw) { super(raw); }
        public final int flashHandle() { return (canId() >> 18) & 0xFF; }
        public final int requestCode() { return (canId() >> 15) & 7; }
        public final int stage() { return (canId() >> 10) & 0x1F; }
        public final int flags() { return (canId() >> 7) & 7; }
    }

    public static final class FlashReadyFrame extends FirmwareFrame {
        FlashReadyFrame(RawFrame raw) { super(raw); }
        public long slaveUid() { return data32(0); }
    }

    public static final class FlashAbortFrame extends FirmwareFrame {
        FlashAbortFrame(RawFrame raw) { super(raw); }
        public long slaveUid() { return data32(0); }
    }

    public static final class FlashAuthorizeFrame extends FirmwareFrame {
        FlashAuthorizeFrame(RawFrame raw) { super(raw); }
        public long slaveUid() { return data32(0); }
    }

    /** Stage 1 requests vendor identification. Stage 2 requests HW/SW information. */
    public static final class IdentityQueryFrame extends FirmwareFrame {
        IdentityQueryFrame(RawFrame raw) { super(raw); }
        public long slaveUid() { return data32(0); }
    }

    /** Raw identity response: vendor (stage 1), HW/SW (2), unknown (3). */
    public static final class IdentityReplyFrame extends FirmwareFrame {
        IdentityReplyFrame(RawFrame raw) { super(raw); }
        public long slaveUid() { return data32(0); }
        public long identityData() { return data32(4); }
    }

    public static final class FlashDataQueryFrame extends FirmwareFrame {
        FlashDataQueryFrame(RawFrame raw) { super(raw); }
        public long offset() { return data32(0); }
        public long byteCount() { return data32(4); }
    }

    public static final class FlashDataFrame extends FirmwareFrame {
        FlashDataFrame(RawFrame raw) { super(raw); }
        public long offset() { return data32(0); }
        public int[] firmwareBytes() { return dataBytes(4); }
    }

    public static final class FlashEndFrame extends FirmwareFrame {
        FlashEndFrame(RawFrame raw) { super(raw); }
        public long slaveUid() { return data32(0); }
    }
}
