package jmri.jmrix.marklin.mcan.cs1;

import jmri.jmrix.marklin.mcan.RawFrame;

/** Registration layouts emitted by the MS1. */
public final class Cs1ConfigCommands {

    private Cs1ConfigCommands() {}

    static Cs1Frame parse(Cs1Frame frame) {
        int stage = (frame.canId() >> 15) & 7;
        boolean master = (frame.canId() & 0x80) != 0;
        if (!master && stage < 4 && frame.dlc() == 0) {
            return new UidFragmentFrame(frame.raw());
        }
        if (!master && stage >= 4 && frame.dlc() == 8) {
            return stage == 4 ? new RegistrationUidFrame(frame.raw())
                    : new RegistrationDataFrame(frame.raw());
        }
        if (master) {
            if (stage < 4) {
                return stage == 0 && frame.dlc() >= 4 ? new MasterIdentityReplyFrame(frame.raw())
                        : new RegistrationReplyFrame(frame.raw());
            }
            if (stage == 7 && frame.dlc() >= 7) {
                return new NodeAssignmentFrame(frame.raw());
            }
        }
        return frame;
    }

    /** Registration-specific header, distinct from the operating header. */
    public abstract static class RegistrationFrame extends Cs1Frame {
        RegistrationFrame(RawFrame raw) { super(raw); }

        /** @return registration stage 0-7 */
        public final int stage() { return (canId() >> 15) & 7; }
        /** @return registration modifier assigned by the master */
        public final int modifierId() { return (canId() >> 10) & 0x1F; }
        /** @return the UID byte echoed in stages 0-3 */
        public final int uidFragment() { return (canId() >> 18) & 0xFF; }
    }

    /** Slave's stages 0-3 carry successive UID bytes in the identifier. */
    public static final class UidFragmentFrame extends RegistrationFrame {
        UidFragmentFrame(RawFrame raw) { super(raw); }
    }

    /** Slave's stage 4 conveys the complete UID. */
    public static final class RegistrationUidFrame extends RegistrationFrame {
        RegistrationUidFrame(RawFrame raw) { super(raw); }
        public long slaveUid() { return data32(0); }
        public int registrationKind() { return data16(4); }
        public int configurationCode() { return data16(6); }
    }

    /** Slave's stages 5-7 carry version information as eight raw bytes. */
    public static final class RegistrationDataFrame extends RegistrationFrame {
        RegistrationDataFrame(RawFrame raw) { super(raw); }
        public int[] registrationData() { return dataBytes(0); }
    }

    /** Master's stage 0 identifies the master, not the registering slave. */
    public static final class MasterIdentityReplyFrame extends RegistrationFrame {
        MasterIdentityReplyFrame(RawFrame raw) { super(raw); }
        public long masterUid() { return data32(0); }
        public boolean hasRegistrationKind() { return dlc() >= 6; }
        public int registrationKind() { return data16(4); }
        public boolean hasMonitorHandle() { return dlc() == 8; }
        public int monitorHandle() { return data16(6); }
    }

    /** Master's stages 1-3 acknowledge the UID fragment and convey device information. */
    public static final class RegistrationReplyFrame extends RegistrationFrame {
        RegistrationReplyFrame(RawFrame raw) { super(raw); }
        public int[] registrationData() { return dataBytes(0); }
    }

    /** Master's stage 7 assigns a node and initial object handle or requests flashing. */
    public static final class NodeAssignmentFrame extends RegistrationFrame {
        NodeAssignmentFrame(RawFrame raw) { super(raw); }
        public long slaveUid() { return data32(0); }
        public int initialObjectHandle() { return data16(4); }
        public int assignedNode() { return dataByte(6) & 0x7F; }
        public boolean multiSlave() { return (dataByte(6) & 0x80) != 0; }
        public boolean hasFlags() { return dlc() == 8; }
        public boolean flashRequested() { return (flags() & 1) != 0; }
        public boolean registrationInhibited() { return (flags() & 2) != 0; }
        public int assignedMasterNode() { return multiSlave() ? assignedNode() - 1 : 0; }
        public int flags() { return dataByte(7); }
    }
}
