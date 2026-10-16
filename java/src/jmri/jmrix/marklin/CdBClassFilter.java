package jmri.jmrix.marklin;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Intercepts OEM CAN-digital-Bahn (CdB) device configuration frames before they
 * reach the standard Marklin CAN (MCAN) listener chain.
 * <p>
 * CdB config frames share command code space with MCAN but are distinguished by
 * the low 12 bits of their hash field (bytes 2–3) always equalling {@code 0x0A80}.
 * Operational CdB frames (turnout, sensor, occupancy) use standard MCAN encoding
 * and pass through unfiltered.
 * <p>
 * Call {@link #offer(MarklinReply)} from the traffic controller before forwarding
 * to the standard listener chain.
 *
 * @see MarklinCanCodec
 */
public class CdBClassFilter {

    // CdB class-ID formula:
    //   ping_id       = ((C >> 4) << 21) | ((C & 0x0F) << 12) | 0x0A80
    //   ping_reply_id = ping_id + 0x10000
    //   read_id       = ping_id + 0x40000
    //   read_reply_id = ping_id + 0x50000
    //   write_id      = ping_id + 0x60000
    //
    // 0x0A80 always lands in the low 12 bits of the hash (bytes 2–3).
    // Frame-type offsets affect only bytes 0–1, so the hash test covers all five types.

    /** Low 12 bits of the hash field (bytes 2–3) that identify a CdB config frame. */
    static final int CDB_HASH_MASK  = 0x0FFF;
    static final int CDB_HASH_VALUE = 0x0A80;

    /**
     * Frame types within a CdB class family
     */
    public enum CdBFrameType {
        /** Zero DLC discovery probe. */
        PING,
        /** Device response to a ping. */
        PING_REPLY,
        /** Indexed parameter read request. */
        READ,
        /** Device response carrying parameter value. */
        READ_REPLY,
        /** Indexed parameter write / control command. */
        WRITE,
        /** Frame matches the CdB hash pattern but offset is unrecognised. */
        UNKNOWN
    }

    /**
     * Result of classifying a single 13-byte frame.
     */
    public static class Classification {
        private final boolean cdBConfig;
        private final int canClass;
        private final CdBFrameType frameType;
        private final int module;
        private final MarklinCanCodec.DecodedMessage decoded;

        Classification(boolean cdBConfig, int canClass, CdBFrameType frameType,
                        int module, MarklinCanCodec.DecodedMessage decoded) {
            this.cdBConfig = cdBConfig;
            this.canClass  = canClass;
            this.frameType = frameType;
            this.module    = module;
            this.decoded   = decoded;
        }

        /** {@code true} if the frame is a CdB config frame, otherwise standard MCAN (or something else). */
        public boolean isCdBConfig()  { return cdBConfig; }
        /** CdB CAN class number (e.g. 0x22 for Magnet M 012). 0 if not CdB. */
        public int     getCanClass()  { return canClass; }
        /** Discriminated frame type within the class family. */
        public CdBFrameType getFrameType() { return frameType; }
        /** Target module number extracted from the last ID byte (byte 3 − 0x80),
         *  or −1 for a broadcast / unparseable frame. */
        public int     getModule()    { return module; }
        /** Full decoded fields for further processing. */
        public MarklinCanCodec.DecodedMessage getDecoded() { return decoded; }
    }

    // ---- listener registry ----------------------------------------------

    private final Set<CdBConfigListener> listeners = new CopyOnWriteArraySet<>();

    public void addCdBConfigListener(CdBConfigListener l) {
        listeners.add(l);
    }

    public void removeCdBConfigListener(CdBConfigListener l) {
        listeners.remove(l);
    }

    // ---- fast-path identification ---------------------------------------

    /**
     * Returns {@code true} if bytes 2–3 match the CdB hash pattern.
     * Cheap enough to call on every incoming frame.
     */
    public static boolean isCdBConfigFrame(MarklinReply r) {
        int hash16 = ((r.getElement(2) & 0xFF) << 8) | (r.getElement(3) & 0xFF);
        return (hash16 & CDB_HASH_MASK) == CDB_HASH_VALUE;
    }

    // ---- full classification -------------------------------------------

    /**
     * Classifies a frame already identified as CdB config traffic.
     * Guard with {@link #isCdBConfigFrame(MarklinReply)} first.
     *
     * @param r a frame whose hash matches the CdB pattern
     * @return class, frame type, module, and fully decoded fields
     */
    public static Classification classify(MarklinReply r) {
        MarklinCanCodec.DecodedMessage decoded = MarklinCanCodec.decode(r);

        int hash16 = (decoded.getHash()[0] << 8) | decoded.getHash()[1];

        // Reconstruct the full 32-bit CAN ID from the raw frame bytes.
        long canId = ((long)(r.getElement(0) & 0xFF) << 24)
                   | ((long)(r.getElement(1) & 0xFF) << 16)
                   | ((long)(r.getElement(2) & 0xFF) << 8)
                   |  (long)(r.getElement(3) & 0xFF);

        // Class low nibble is encoded in the upper byte of the hash.
        int classLow = (hash16 >> 8) & 0x0F;

        // Recover class and frame type by trying each known frame-type offset.
        // For each candidate subtract the offset (module byte cleared) and verify
        // that the residual decomposes cleanly via the ping_id formula.
        long[] offsets = {0x00000L, 0x10000L, 0x40000L, 0x50000L, 0x60000L};
        CdBFrameType[] types = {
            CdBFrameType.PING, CdBFrameType.PING_REPLY,
            CdBFrameType.READ, CdBFrameType.READ_REPLY,
            CdBFrameType.WRITE
        };

        int canClass = 0;
        CdBFrameType frameType = CdBFrameType.UNKNOWN;

        for (int i = 0; i < offsets.length; i++) {
            // Subtract the offset and set the module byte to 0x80 (broadcast base).
            long candidatePing = (canId & 0xFFFFFF00L) - offsets[i] + 0x80;
            // Check whether (candidatePing - 0x0A80) factors as ((C>>4)<<21) | ((C&0xF)<<12).
            long base = candidatePing - 0x0A80;
            int highPart = (int)((base >> 21) & 0x0F);
            int lowPart  = (int)((base >> 12) & 0x0F);
            int candidate = (highPart << 4) | lowPart;
            // Verify the candidate class reproduces the known low nibble
            if (lowPart == classLow && base == ((long)highPart << 21) + ((long)lowPart << 12)) {
                canClass  = candidate;
                frameType = types[i];
                break;
            }
        }

        // Byte 3 encodes the module as (module + 0x80); broadcast pings use 0x80.
        int idByte3 = r.getElement(3) & 0xFF;
        int module = (idByte3 >= 0x80) ? idByte3 - 0x80 : -1;

        return new Classification(true, canClass, frameType, module, decoded);
    }

    // ---- dispatch entry point ------------------------------------------

    /**
     * Offers a frame to the CdB pre-filter.
     * Call this before the standard Märklin listener chain.
     *
     * @param r incoming 13-byte frame
     * @return {@code true} if the frame is CdB config traffic (notified to
     *         {@link CdBConfigListener}s, must not be forwarded further);
     *         {@code false} if it is normal MCAN traffic
     */
    public boolean offer(MarklinReply r) {
        if (!isCdBConfigFrame(r)) {
            return false;
        }
        Classification c = classify(r);
        for (CdBConfigListener l : listeners) {
            l.cdBConfigReply(c, r);
        }
        return true;
    }

    private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(CdBClassFilter.class);
}
