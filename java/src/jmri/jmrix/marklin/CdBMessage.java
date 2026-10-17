package jmri.jmrix.marklin;

/**
 * Factory for creating CAN-digital-Bahn (CdB) configuration frames.
 */
public class CdBMessage {

    /**
     * Create a CdB PING discovery message for a given CAN class.
     * @param canClass CAN class ID (e.g. 0x22)
     * @return MarklinMessage formatted as a DLC-0 CdB ping
     */
    public static MarklinMessage ping(int canClass) {
        McanDeviceIdentity.CdB addr = new McanDeviceIdentity.CdB(canClass, 0);
        long pingId = addr.getPingId();
        return createMessageFromId(pingId, 0, new int[0]);
    }

    /**
     * Create a CdB parameter READ request.
     * @param addr target device identity
     * @param index parameter index
     * @param param parameter ID
     * @return MarklinMessage formatted as a DLC-2 parameter read
     */
    public static MarklinMessage readParam(McanDeviceIdentity.CdB addr, int index, int param) {
        long readId = addr.getReadId();
        return createMessageFromId(readId, 2, new int[]{index & 0xFF, param & 0xFF});
    }

    /**
     * Create a CdB parameter WRITE request.
     * @param addr target device identity
     * @param index parameter index
     * @param param parameter ID
     * @param valueHi high byte value
     * @param valueLo low byte value
     * @return MarklinMessage formatted as a DLC-4 parameter write
     */
    public static MarklinMessage writeParam(McanDeviceIdentity.CdB addr, int index, int param, int valueHi, int valueLo) {
        long writeId = addr.getWriteId();
        return createMessageFromId(writeId, 4, new int[]{index & 0xFF, param & 0xFF, valueHi & 0xFF, valueLo & 0xFF});
    }

    /**
     * Create a CdB RESET command (00 FF).
     * @param addr target device identity
     * @return MarklinMessage formatted as a DLC-2 reset
     */
    public static MarklinMessage reset(McanDeviceIdentity.CdB addr) {
        long writeId = addr.getWriteId();
        return createMessageFromId(writeId, 2, new int[]{0x00, 0xFF});
    }

    /**
     * Create a CdB RELEASE command (00 FE).
     * @param addr target device identity
     * @return MarklinMessage formatted as a DLC-2 release
     */
    public static MarklinMessage release(McanDeviceIdentity.CdB addr) {
        long writeId = addr.getWriteId();
        return createMessageFromId(writeId, 2, new int[]{0x00, 0xFE});
    }

    /**
     * Create a CdB module setup command (00 F7 00 01).
     * @param addr target device identity
     * @return MarklinMessage formatted as a DLC-4 module setup
     */
    public static MarklinMessage moduleSetup(McanDeviceIdentity.CdB addr) {
        long writeId = addr.getWriteId();
        return createMessageFromId(writeId, 4, new int[]{0x00, 0xF7, 0x00, 0x01});
    }

    private static MarklinMessage createMessageFromId(long canId, int dlc, int[] data) {
        int prio = (int) ((canId >> 26) & 0x0F);
        int cmd  = (int) ((canId >> 15) & 0x07FF);
        boolean isResp = ((canId >> 14) & 0x01) == 0x01;
        int hash1 = (int) ((canId >> 8) & 0xFF);
        int hash2 = (int) (canId & 0xFF);

        int[] encoded = MarklinCanCodec.builder()
                .setPriority(prio)
                .setCommand(cmd)
                .setResponse(isResp)
                .setHash(hash1, hash2)
                .setDataLength(dlc)
                .setData(data)
                .build();

        MarklinMessage msg = new MarklinMessage(13);
        for (int i = 0; i < 13; i++) {
            msg.setElement(i, encoded[i]);
        }
        return msg;
    }
}
