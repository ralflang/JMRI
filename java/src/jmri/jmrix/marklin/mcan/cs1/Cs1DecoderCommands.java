package jmri.jmrix.marklin.mcan.cs1;

import jmri.jmrix.marklin.mcan.RawFrame;

/**
 * Locomotive metadata, ownership and stack frames.
 * Handles are not track addresses.
 */
public final class Cs1DecoderCommands {

    private Cs1DecoderCommands() {}

    static Cs1Frame parse(Cs1Frame frame) {
        int command = (frame.canId() >> 7) & 7;
        boolean master = (frame.node() & 1) == 0;
        int dlc = frame.dlc();
        if (command == 0 && dlc >= 2 && frame.dataByte(0) == 3
                && frame.dataByte(1) <= 1) {
            if (dlc == 2) {
                return new ControllerQueryFrame(frame.raw());
            }
            if (dlc == 7 && frame.dataByte(1) == 0) {
                return new ControllerStateFrame(frame.raw());
            }
            if (dlc == 6) {
                return master ? new LocoAssignmentReplyFrame(frame.raw())
                        : new LocoAssignmentFrame(frame.raw());
            }
        }
        if (command == 5 && dlc >= 6) {
            return new StackRemoveFrame(frame.raw());
        }
        if (dlc < 4) {
            return frame;
        }
        if (command == 1) {
            if (frame.data32(0) == 0x40030000L) {
                if (dlc == 4) {
                    return new LocoTypeQueryFrame(frame.raw());
                }
                if (dlc == 6) {
                    return new LocoTypeReplyFrame(frame.raw());
                }
            }
            if (frame.data16(0) == 0x4001 && frame.dataByte(2) <= 1
                    && frame.dataByte(3) == 0) {
                if (dlc == 4) {
                    return new RailFormatQueryFrame(frame.raw());
                }
                if (dlc == 8) {
                    return new RailFormatReplyFrame(frame.raw());
                }
            }
            if (frame.data32(0) == 0x40000000L && dlc == 4) {
                return new DecoderResetFrame(frame.raw());
            }
            if (frame.data32(0) == 0x40020000L && dlc == 8) {
                return new ProgrammingValueFrame(frame.raw());
            }
            if (frame.data32(0) == 0x40040000L) {
                return dlc == 4 ? new VolumeQueryFrame(frame.raw())
                        : new VolumeDataFrame(frame.raw());
            }
            if (frame.dataByte(1) == 1 && frame.dataByte(2) <= 1
                    && frame.dataByte(3) == 0 && dlc == 6) {
                return frame.dataByte(2) == 1 ? new AddressValueFrame(frame.raw())
                        : new DecoderPropertyFrame(frame.raw());
            }
            if ((frame.dataByte(1) == 1 && frame.dataByte(2) <= 1)
                    || (frame.dataByte(1) == 3 && frame.dataByte(2) == 0)) {
                return new DecoderPropertyFrame(frame.raw());
            }
        }
        return frame;
    }

    public static final class LocoTypeQueryFrame extends Cs1ObjectFrame {
        LocoTypeQueryFrame(RawFrame raw) { super(raw); }
    }

    public static final class LocoTypeReplyFrame extends Cs1ObjectFrame {
        LocoTypeReplyFrame(RawFrame raw) { super(raw); }
        /** 0=none, 1=electric, 2=diesel, 3=steam; other values retained. */
        public int locoType() { return dataByte(5); }
    }

    public static final class RailFormatQueryFrame extends Cs1ObjectFrame {
        RailFormatQueryFrame(RawFrame raw) { super(raw); }
        /** 0=rail protocol, 1=rail address and state flags. */
        public int selector() { return dataByte(2); }
    }

    public static final class RailFormatReplyFrame extends Cs1ObjectFrame {
        RailFormatReplyFrame(RawFrame raw) { super(raw); }
        public int selector() { return dataByte(2); }
        /** For selector 0: 1=mfx, 2=DCC, 3=Motorola. */
        public int formatOrDetailCode() { return dataByte(5); }
        public int[] formatData() { return dataBytes(4); }
        /** Rail address (mfx SID) when selector is 1. Not a stable decoder UID. */
        public int address() { return data16(4); }
        /** Capability flags for selector 0 or conflict/no-answer flags for selector 1. */
        public int flags() { return data16(6); }
    }

    /** Slave take/release, carrying controller handle and object change counter. */
    public static final class LocoAssignmentFrame extends Cs1ObjectFrame {
        LocoAssignmentFrame(RawFrame raw) { super(raw); }
        public int controlByte() { return dataByte(1); }
        public int ownershipCode() { return data16(2); }
        public int assignmentToken() { return data16(4); }
        public int controllerHandle() { return data16(2); }
        public int changeCounter() { return data16(4); }
    }

    /** Master ownership response/notification, not a subtype of the slave command. */
    public static final class LocoAssignmentReplyFrame extends Cs1ObjectFrame {
        LocoAssignmentReplyFrame(RawFrame raw) { super(raw); }
        public int controlByte() { return dataByte(1); }
        public int ownershipCode() { return data16(2); }
        public int assignmentToken() { return data16(4); }
        public int controllerHandle() { return data16(2); }
        public int changeCounter() { return data16(4); }
    }

    public static final class StackEntryQueryFrame extends Cs1ObjectFrame {
        StackEntryQueryFrame(RawFrame raw) { super(raw); }
        public int previousHandle() { return data16(2); }
        public int kind() { return dataByte(0); }
    }

    public static final class StackEntryReplyFrame extends Cs1ObjectFrame {
        StackEntryReplyFrame(RawFrame raw) { super(raw); }
        public int previousHandle() { return data16(2); }
        public int locoHandle() { return data16(4); }
        public int kind() { return dataByte(0); }
        public boolean hasChangeCounter() { return dlc() == 8; }
        public int changeCounter() { return data16(6); }
    }

    public static final class StackEndFrame extends Cs1ObjectFrame {
        StackEndFrame(RawFrame raw) { super(raw); }
        public int previousHandle() { return data16(2); }
        public int kind() { return dataByte(0); }
    }

    public static final class StackAddFrame extends Cs1ObjectFrame {
        StackAddFrame(RawFrame raw) { super(raw); }
        public int locoHandle() { return data16(4); }
        public int kind() { return dataByte(0); }
        public int controllerHandle() { return data16(2); }
        public int changeCounter() { return data16(6); }
    }

    public static final class StackRemoveFrame extends Cs1ObjectFrame {
        StackRemoveFrame(RawFrame raw) { super(raw); }
        public int locoHandle() { return data16(4); }
        public int controllerHandle() { return data16(2); }
        public boolean hasChangeCounter() { return dlc() == 8; }
        public int changeCounter() { return data16(6); }
    }

    /** Sliced metadata without assuming absent bytes or a command/reply role. */
    public static final class DecoderPropertyFrame extends Cs1ObjectCommands.PropertyFrame {
        DecoderPropertyFrame(RawFrame raw) { super(raw); }
    }

    public static final class DecoderResetFrame extends Cs1ObjectFrame {
        DecoderResetFrame(RawFrame raw) { super(raw); }
    }

    public static final class ProgrammingValueFrame extends Cs1ObjectFrame {
        ProgrammingValueFrame(RawFrame raw) { super(raw); }
        public int value() { return dataByte(5); }
        public int cv() { return data16(6); }
    }

    public static final class AddressValueFrame extends Cs1ObjectFrame {
        AddressValueFrame(RawFrame raw) { super(raw); }
        public int address() { return data16(4); }
    }

    /** Also matches the firmware's pending-volume send, which transmits no value. */
    public static final class VolumeQueryFrame extends Cs1ObjectFrame {
        VolumeQueryFrame(RawFrame raw) { super(raw); }
    }

    /** Raw volume bytes: The firmware consumes D4 but emits a reply with volume in D5. */
    public static final class VolumeDataFrame extends Cs1ObjectCommands.PropertyFrame {
        VolumeDataFrame(RawFrame raw) { super(raw); }
    }

    public static final class ControllerQueryFrame extends Cs1ObjectFrame {
        ControllerQueryFrame(RawFrame raw) { super(raw); }
        public int controlByte() { return dataByte(1); }
    }

    public static final class ControllerStateFrame extends Cs1ObjectFrame {
        ControllerStateFrame(RawFrame raw) { super(raw); }
        public int controllerHandle() { return data16(2); }
        public int changeCounter() { return data16(4); }
        public int flags() { return dataByte(6); }
        public boolean masterLocallyDriven() { return (flags() & 1) != 0; }
    }
}
