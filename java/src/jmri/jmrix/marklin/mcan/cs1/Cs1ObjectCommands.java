package jmri.jmrix.marklin.mcan.cs1;

import jmri.jmrix.marklin.mcan.RawFrame;

/**
 * Object acquisition, names and indexed exchanges.
 * The same indexed layout serves system status and locomotive functions.
 * Name data can be a reply or a rename operation. Node parity is not a reply bit.
 */
public final class Cs1ObjectCommands {

    private Cs1ObjectCommands() {}

    static Cs1Frame parse(Cs1Frame frame) {
        int command = (frame.canId() >> 7) & 7;
        boolean master = (frame.node() & 1) == 0;
        int dlc = frame.dlc();
        if (command == 2 && dlc >= 2 && frame.dataByte(0) != 0) {
            int selector = frame.dataByte(1);
            if (dlc == 2 && (selector == 0 || selector == 1)) {
                if (master && selector == 1) {
                    return new IndexedListEndFrame(frame.raw());
                }
                return new IndexedQueryFrame(frame.raw());
            }
            if (dlc == 3 && selector == 1) {
                return new IndexedTypeReplyFrame(frame.raw());
            }
            if (dlc >= 4 && selector == 1 && dlc % 2 == 0) {
                return new FunctionDescriptionReplyFrame(frame.raw());
            }
            if (dlc >= 3 && selector == 0) {
                return new IndexedValueFrame(frame.raw());
            }
            if (dlc >= 4 && selector == 2 && frame.dataByte(2) <= 2
                    && frame.dataByte(3) == 0) {
                return dlc == 4 ? new SpeedParameterQueryFrame(frame.raw())
                        : new SpeedParameterValueFrame(frame.raw());
            }
        }
        if (command == 4) {
            if (!master && dlc == 2 && frame.dataByte(0) == 0x40) {
                return new SlaveDescriptionQueryFrame(frame.raw());
            }
            if (dlc == 8 && frame.dataByte(0) == 0x40) {
                return new SlaveDescriptionReplyFrame(frame.raw());
            }
            if (dlc == 1 || dlc == 2 || dlc == 4) {
                return new ObjectAllocationFrame(frame.raw());
            }
            if (dlc == 8 && frame.dataByte(0) >= 0x80 && frame.dataByte(0) <= 0x82
                    && frame.dataByte(1) == 0) {
                return new Cs1DecoderCommands.StackAddFrame(frame.raw());
            }
        }
        if (command == 3) {
            if (!master && dlc == 2 && frame.data16(0) == 0x0300) {
                return new SystemHandleQueryFrame(frame.raw());
            }
            if (master && dlc >= 6
                    && (frame.dataByte(0) == 0x40 || frame.data16(0) == 0x0300)
                    && frame.data16(2) == 0) {
                return new SystemHandleReplyFrame(frame.raw());
            }
            if (dlc >= 4 && frame.dataByte(0) >= 0x80 && frame.dataByte(0) <= 0x82
                    && frame.dataByte(1) == 2) {
                if (dlc >= 6) {
                    return new Cs1DecoderCommands.StackEntryReplyFrame(frame.raw());
                }
                if (dlc == 4 || dlc == 5) {
                    return master ? new Cs1DecoderCommands.StackEndFrame(frame.raw())
                            : new Cs1DecoderCommands.StackEntryQueryFrame(frame.raw());
                }
            }
            if (dlc == 2 || dlc >= 4) {
                return new DirectoryFrame(frame.raw());
            }
        }
        if (command == 1 && dlc >= 4) {
            if (frame.dataByte(0) == 0x41 && frame.dataByte(1) == 0
                    && frame.dataByte(3) == 0) {
                if (dlc == 4) {
                    return new StackSizeQueryFrame(frame.raw());
                }
                if (dlc >= 6) {
                    return new StackSizeReplyFrame(frame.raw());
                }
            }
            if (frame.dataByte(0) == 2 && frame.data16(1) == 0) {
                if (dlc == 4) {
                    return new NameQueryFrame(frame.raw());
                }
                return new NameFragmentFrame(frame.raw());
            }
            if (frame.dataByte(0) == 0 && frame.data16(1) == 0) {
                return new ObjectKindFrame(frame.raw());
            }
            if (frame.dataByte(0) == 1 && identificationSelector(frame)) {
                return new IdentificationFrame(frame.raw());
            }
        }
        return frame;
    }

    private static boolean identificationSelector(Cs1Frame frame) {
        int item = frame.dataByte(1);
        int sub = frame.dataByte(2);
        return (item == 0 && sub == 0) || (item == 1 && sub <= 2)
                || (item == 2 && sub <= 1) || (item == 3 && sub <= 3)
                || (item == 4 && sub == 0) || (item == 5 && sub <= 1);
    }

    /** Query, sliced response or write. No response flag exists in this layout. */
    public abstract static class PropertyFrame extends Cs1ObjectFrame {
        PropertyFrame(RawFrame raw) { super(raw); }
        public int group() { return dataByte(0); }
        public int item() { return dataByte(1); }
        public int subItem() { return dataByte(2); }
        public int offset() { return dataByte(3); }
        public boolean hasValue() { return dlc() > 4; }
        public int[] valueBytes() { return dataBytes(4); }
    }

    public static final class ObjectKindFrame extends PropertyFrame {
        ObjectKindFrame(RawFrame raw) { super(raw); }
    }

    /** Raw identification bytes or string segment. */
    public static final class IdentificationFrame extends PropertyFrame {
        IdentificationFrame(RawFrame raw) { super(raw); }
        public boolean isString() {
            return (item() == 1 && subItem() == 2) || (item() == 3 && subItem() == 3);
        }
    }

    /** Directory query, result or list end. */
    public static final class DirectoryFrame extends Cs1ObjectFrame {
        DirectoryFrame(RawFrame raw) { super(raw); }
        public int kind() { return dataByte(0); }
        public int step() { return (byte) dataByte(1); }
        public boolean hasPreviousHandle() { return dlc() >= 4; }
        public int previousHandle() { return data16(2); }
        public boolean hasResult() { return dlc() >= 6; }
        public int resultHandle() { return data16(4); }
        public boolean hasChangeCounter() { return dlc() == 8; }
        public int changeCounter() { return data16(6); }
    }

    public static final class ObjectAllocationFrame extends Cs1ObjectFrame {
        ObjectAllocationFrame(RawFrame raw) { super(raw); }
        public int kind() { return dataByte(0); }
        public boolean hasTag() { return dlc() >= 2; }
        public int tag() { return dataByte(1); }
        public boolean hasController() { return dlc() == 4; }
        public int controllerHandle() { return data16(2); }
    }

    public static final class SlaveDescriptionQueryFrame extends Cs1ObjectFrame {
        SlaveDescriptionQueryFrame(RawFrame raw) { super(raw); }
        public int slaveNode() { return dataByte(1); }
    }

    public static final class SlaveDescriptionReplyFrame extends Cs1ObjectFrame {
        SlaveDescriptionReplyFrame(RawFrame raw) { super(raw); }
        public int slaveNode() { return dataByte(1); }
        public int controllerHandle() { return data16(2); }
        public int descriptionHandle() { return data16(4); }
        public int changeCounter() { return data16(6); }
    }

    public static final class SystemHandleQueryFrame extends Cs1ObjectFrame {
        SystemHandleQueryFrame(RawFrame raw) { super(raw); }
    }

    public static final class SystemHandleReplyFrame extends Cs1ObjectFrame {
        SystemHandleReplyFrame(RawFrame raw) { super(raw); }
        public int systemHandle() { return data16(4); }
        public boolean hasChangeCounter() { return dlc() == 8; }
        public int changeCounter() { return data16(6); }
    }

    public static final class StackSizeQueryFrame extends Cs1ObjectFrame {
        StackSizeQueryFrame(RawFrame raw) { super(raw); }
        public int kind() { return dataByte(2); }
    }

    public static final class StackSizeReplyFrame extends Cs1ObjectFrame {
        StackSizeReplyFrame(RawFrame raw) { super(raw); }
        public int stackSize() { return data16(4); }
        public int kind() { return dataByte(2); }
    }

    /** Query for four bytes of a slave or locomotive name. */
    public static final class NameQueryFrame extends Cs1ObjectFrame {
        NameQueryFrame(RawFrame raw) { super(raw); }
        public int offset() { return dataByte(3); }
    }

    /** Name bytes including any null terminator. */
    public static final class NameFragmentFrame extends Cs1ObjectFrame {
        NameFragmentFrame(RawFrame raw) { super(raw); }
        public int offset() { return dataByte(3); }
        public int[] nameBytes() { return dataBytes(4); }
    }

    /** Selector 0 asks for a value. Selector 1 asks for its type/description. */
    public static final class IndexedQueryFrame extends Cs1ObjectFrame {
        IndexedQueryFrame(RawFrame raw) { super(raw); }
        public int index() { return dataByte(0); }
        public boolean requestsDescription() { return dataByte(1) == 1; }
    }

    /** Master's two-byte description echo. List end or query depending on session context. */
    public static final class IndexedListEndFrame extends Cs1ObjectFrame {
        IndexedListEndFrame(RawFrame raw) { super(raw); }
        public int index() { return dataByte(0); }
    }

    public static final class IndexedTypeReplyFrame extends Cs1ObjectFrame {
        IndexedTypeReplyFrame(RawFrame raw) { super(raw); }
        public int index() { return dataByte(0); }
        public int type() { return dataByte(2); }
    }

    public static final class FunctionDescriptionReplyFrame extends Cs1ObjectFrame {
        FunctionDescriptionReplyFrame(RawFrame raw) { super(raw); }
        public int index() { return dataByte(0); }
        /** First pair's kind: 1=speed, 2=function, 0=gap. */
        public int descriptionKind() { return dataByte(2); }
        public int actuationType() { return dataByte(2); }
        /** Icon for function outputs, speed information for the motor entry. */
        public int iconOrSpeed() { return dataByte(3); }
        public boolean describesSpeed() { return descriptionKind() == 1 && iconOrSpeed() == 1; }
        public boolean describesFunction() { return descriptionKind() == 2; }
        public boolean momentary() { return describesFunction() && (iconOrSpeed() & 0x80) != 0; }
        public int icon() { return iconOrSpeed() & 0x7F; }
        public int[] descriptionBytes() { return dataBytes(2); }
    }

    /**
     * Value query response or unsolicited change. A system index and a locomotive
     * function index can have identical bytes, including speed versus Stop/Go.
     * Interpretation requires the session's object handle mapping.
     */
    public static final class IndexedValueFrame extends Cs1ObjectFrame {
        IndexedValueFrame(RawFrame raw) { super(raw); }
        public int index() { return dataByte(0); }
        public int value() { return dataByte(2); }
    }

    public static final class SpeedParameterQueryFrame extends Cs1ObjectFrame {
        SpeedParameterQueryFrame(RawFrame raw) { super(raw); }
        public int index() { return dataByte(0); }
        public int parameter() { return dataByte(2); }
    }

    /** Parameter write, response or notification: 0=vmax, 1=acceleration, 2=braking. */
    public static final class SpeedParameterValueFrame extends Cs1ObjectFrame {
        SpeedParameterValueFrame(RawFrame raw) { super(raw); }
        public int index() { return dataByte(0); }
        public int parameter() { return dataByte(2); }
        public int value() { return dataByte(4); }
    }
}
