package jmri.jmrix.marklin;

import java.util.Objects;

/**
 * Factory utility for constructing CAN-digital-bahn (CdB) configuration frames.
 * <p>
 * Uses {@link MarklinCanCodec} to construct raw frames. Operates without requiring
 * full JMRI runtime instances and supports sending directly via {@link McanFrameSender}.
 *
 * @author Ralf Lang (ralf.lang@ralf-lang.de) Copyright (C) 2026
 */
public class CdBMessage {

    /** CdB Configuration command (0x11 / 17 in MCAN protocol) */
    public static final int CMD_CONFIG = 0x11;

    /**
     * Known CdB device classes (CAN class identifiers).
     * Each class requires a separate discovery ping.
     */
    public enum CdBDeviceClass {
        STROM_SNIFFER(0x0B, "StromSniffer"),
        GLEIS_REPORTER(0x0D, "GleisReporter"),
        REPORTER_CHEF_002(0x21, "ReporterChef 002"),
        WEICHEN_CHEF_MAGNET(0x22, "WeichenChef Magnet M 012 / deLuxe 007"),
        WEICHEN_CHEF_AC(0x23, "WeichenChef AC 002"),
        WEICHEN_CHEF_BASIC(0x26, "WeichenChef Basic 002"),
        WEICHEN_CHEF_KOMBI(0x27, "WeichenChef Kombi");

        private final int classId;
        private final String displayName;

        CdBDeviceClass(int classId, String displayName) {
            this.classId = classId;
            this.displayName = displayName;
        }

        public int getClassId() {
            return classId;
        }

        public String getDisplayName() {
            return displayName;
        }

        /**
         * Calculate the CdB ping identifier for this device class.
         * Formula: ping_id = ((C >> 4) << 21) + ((C & 0x0F) << 12) + 0x0A80
         *
         * @return 32-bit CAN identifier for zero-DLC ping
         */
        public int getPingId() {
            return ((classId >> 4) << 21) + ((classId & 0x0F) << 12) + 0x0A80;
        }
    }

    private CdBMessage() {
    }

    /**
     * Create a CdB PING query frame (class 0x00, module 0x00).
     *
     * @param hash 16-bit sender hash
     * @return 13-element raw CAN frame
     */
    public static int[] createPing(int hash) {
        return createConfigFrame(0x00, 0x00, 0x00, new int[0], hash);
    }

    /**
     * Create a CdB class-specific discovery ping (zero-DLC).
     *
     * @param deviceClass target CdB device class
     * @return 13-element raw CAN frame with zero DLC
     */
    public static int[] createClassPing(CdBDeviceClass deviceClass) {
        int pingId = deviceClass.getPingId();
        int[] rawFrame = new int[13];
        // CAN identifier (big-endian, 4 bytes)
        rawFrame[0] = (pingId >> 24) & 0xFF;
        rawFrame[1] = (pingId >> 16) & 0xFF;
        rawFrame[2] = (pingId >> 8) & 0xFF;
        rawFrame[3] = pingId & 0xFF;
        // DLC = 0
        rawFrame[4] = 0;
        // Data bytes 5-12 remain zero
        return rawFrame;
    }

    /**
     * Create a CdB parameter read frame.
     *
     * @param canClass CdB target CAN class
     * @param moduleId CdB target module ID
     * @param paramIndex parameter index to read
     * @param hash 16-bit sender hash
     * @return 13-element raw CAN frame
     */
    public static int[] createReadParam(int canClass, int moduleId, int paramIndex, int hash) {
        return createConfigFrame(canClass, moduleId, 0x01, new int[]{paramIndex & 0xFF}, hash);
    }

    /**
     * Create a CdB parameter write frame.
     *
     * @param canClass CdB target CAN class
     * @param moduleId CdB target module ID
     * @param paramIndex parameter index
     * @param paramValue parameter value byte
     * @param hash 16-bit sender hash
     * @return 13-element raw CAN frame
     */
    public static int[] createWriteParam(int canClass, int moduleId, int paramIndex, int paramValue, int hash) {
        return createConfigFrame(canClass, moduleId, 0x02, new int[]{paramIndex & 0xFF, paramValue & 0xFF}, hash);
    }

    /**
     * Create a CdB module reset frame.
     *
     * @param canClass CdB target CAN class
     * @param moduleId CdB target module ID
     * @param hash 16-bit sender hash
     * @return 13-element raw CAN frame
     */
    public static int[] createReset(int canClass, int moduleId, int hash) {
        return createConfigFrame(canClass, moduleId, 0x03, new int[0], hash);
    }

    /**
     * Create a general CdB configuration frame.
     *
     * @param canClass target CAN class
     * @param moduleId target module ID
     * @param cdbCommand CdB command code
     * @param payload extra payload bytes
     * @param hash 16-bit sender hash
     * @return 13-element raw CAN frame
     */
    public static int[] createConfigFrame(int canClass, int moduleId, int cdbCommand, int[] payload, int hash) {
        Objects.requireNonNull(payload, "payload cannot be null");
        int dlc = Math.min(3 + payload.length, 4); // Max DLC is 4 in 13-byte frame format
        int[] data = new int[dlc];
        data[0] = canClass & 0xFF;
        if (dlc > 1) data[1] = moduleId & 0xFF;
        if (dlc > 2) data[2] = cdbCommand & 0xFF;
        for (int i = 0; i < payload.length && (3 + i) < dlc; i++) {
            data[3 + i] = payload[i] & 0xFF;
        }

        int hash1 = (hash >> 8) & 0xFF;
        int hash2 = hash & 0xFF;

        return MarklinCanCodec.builder()
                .setPriority(MarklinConstants.PRIO_1)
                .setCommand(CMD_CONFIG)
                .setResponse(false)
                .setHash(hash1, hash2)
                .setData(data)
                .build();
    }

    /**
     * Send a CdB frame via a {@link McanFrameSender}.
     *
     * @param sender transport frame sender
     * @param frame raw frame to send
     */
    public static void send(McanFrameSender sender, int[] frame) {
        Objects.requireNonNull(sender, "sender cannot be null");
        Objects.requireNonNull(frame, "frame cannot be null");
        sender.sendFrame(frame);
    }
}
