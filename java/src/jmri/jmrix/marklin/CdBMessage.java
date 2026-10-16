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
