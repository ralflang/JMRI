package jmri.jmrix.marklin;

import java.util.Objects;

/**
 * Identity model for MCAN bus devices (Native Märklin devices and CAN-digital-Bahn third-party devices).
 * Immutable value object carrying enough information to address a device on the bus.
 *
 * @author Generated based on Spec ralflang/JMRI#3
 */
public abstract class McanDeviceIdentity {

    /**
     * Get NamedBean system name fragment/suffix (e.g. uid-00112233 or cdb-22-03).
     * @return system name fragment
     */
    public abstract String toSystemIdFragment();

    /**
     * Human-readable representation for UI display.
     * @return display string
     */
    public abstract String toHumanString();

    /**
     * Native MCAN device identity (CS2/CS3, Gleisbox, MS2, GFP, Connect 6021, etc.).
     */
    public static final class Native extends McanDeviceIdentity {
        private final long uid;
        private final int deviceType;
        private final int hash;

        public Native(long uid, int deviceType, int hash) {
            this.uid = uid & 0xFFFFFFFFL;
            this.deviceType = deviceType & 0xFFFF;
            this.hash = hash & 0xFFFF;
        }

        public long getUid() {
            return uid;
        }

        public int getDeviceType() {
            return deviceType;
        }

        public int getHash() {
            return hash;
        }

        @Override
        public String toSystemIdFragment() {
            return String.format("uid-%08x", uid);
        }

        @Override
        public String toHumanString() {
            String typeName = MarklinDeviceIdentifier.getDeviceTypeName(deviceType);
            return String.format("%s (UID 0x%08X)", typeName, uid);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Native aNative = (Native) o;
            return uid == aNative.uid && deviceType == aNative.deviceType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(uid, deviceType);
        }

        @Override
        public String toString() {
            return toHumanString();
        }
    }

    /**
     * CAN-digital-Bahn (CdB) OEM device identity (WeichenChef, GleisReporter, StromSniffer, ReporterChef, etc.).
     */
    public static final class CdB extends McanDeviceIdentity {
        private final int canClass;
        private final int moduleId;

        public CdB(int canClass, int moduleId) {
            this.canClass = canClass & 0xFF;
            this.moduleId = moduleId & 0x3F; // 0..63
        }

        public int getCanClass() {
            return canClass;
        }

        public int getModuleId() {
            return moduleId;
        }

        /**
         * Compute CAN ID for PING discovery frame (module byte 0x80).
         * @return ping 29-bit CAN ID
         */
        public long getPingId() {
            return computeCanId(0x00000L);
        }

        /**
         * Compute CAN ID for parameter READ request frame.
         * @return read 29-bit CAN ID
         */
        public long getReadId() {
            return computeCanId(0x40000L);
        }

        /**
         * Compute CAN ID for parameter READ_REPLY frame.
         * @return read reply 29-bit CAN ID
         */
        public long getReadReplyId() {
            return computeCanId(0x50000L);
        }

        /**
         * Compute CAN ID for parameter WRITE / command frame.
         * @return write 29-bit CAN ID
         */
        public long getWriteId() {
            return computeCanId(0x60000L);
        }

        private long computeCanId(long typeOffset) {
            int highClass = (canClass >> 4) & 0x0F;
            int lowClass  = canClass & 0x0F;
            long pingId   = ((long) highClass << 21) | ((long) lowClass << 12) | 0x0A80L;
            return pingId + typeOffset + (moduleId + 0x80);
        }

        /**
         * Create a new CdB identity with same CAN class but a different module ID.
         * @param newModuleId module ID (0..63)
         * @return new CdB identity
         */
        public CdB withModule(int newModuleId) {
            return new CdB(this.canClass, newModuleId);
        }

        @Override
        public String toSystemIdFragment() {
            return String.format("cdb-%02x-%02d", canClass, moduleId);
        }

        @Override
        public String toHumanString() {
            String className = getCdBClassName(canClass);
            return String.format("%s #%d", className, moduleId);
        }

        private static String getCdBClassName(int canClass) {
            switch (canClass) {
                case 0x0B: return "StromSniffer Class 0x0B";
                case 0x0D: return "GleisReporter Class 0x0D";
                case 0x21: return "ReporterChef Class 0x21";
                case 0x22: return "WeichenChef Class 0x22";
                case 0x23: return "WeichenChef AC Class 0x23";
                case 0x26: return "WeichenChef Basic Class 0x26";
                case 0x27: return "WeichenChef Kombi Class 0x27";
                default:   return String.format("CdB Device Class 0x%02X", canClass);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CdB cdB = (CdB) o;
            return canClass == cdB.canClass && moduleId == cdB.moduleId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(canClass, moduleId);
        }

        @Override
        public String toString() {
            return toHumanString();
        }
    }
}
