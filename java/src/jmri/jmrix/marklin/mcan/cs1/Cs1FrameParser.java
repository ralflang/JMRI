package jmri.jmrix.marklin.mcan.cs1;

import jmri.jmrix.marklin.mcan.RawFrame;

/**
 * Stateless decoder for already classified CS1/MS1 M-CAN frames.
 * <p>
 * Based on <a href="https://www.skrauss.de/modellbahn/CAN_Doku_V101.pdf">MS1 CAN
 * documentation, version 1.1</a> and observed traffic.
 *
 * Command bits alone do not identify an operation. Payload selectors and DLC distinguish message types, not session roles.
 * Unknown message types retain the base {@link Cs1Frame}
 */
public final class Cs1FrameParser {

    private Cs1FrameParser() {}

    /**
     * @param raw An already classified CS1 frame
     * @return A typed subclass or the raw base frame
     */
    public static Cs1Frame parse(RawFrame raw) {
        Cs1Frame frame = new Cs1Frame(raw);
        if (frame.priority() == 7) {
            if (frame.node() == 126) {
                return Cs1ConfigCommands.parse(frame);
            }
            if (frame.node() == 127) {
                return Cs1FirmwareCommands.parse(frame);
            }
            return frame;
        }
        int command = (frame.canId() >> 7) & 7;
        if (frame.canId() == 0x380 || command == 7) {
            return Cs1SystemCommands.parse(frame);
        }
        if (command == 0 || command == 5) {
            return Cs1DecoderCommands.parse(frame);
        }
        if (command == 1 && frame.dlc() >= 4 && frame.dataByte(0) == 0x40) {
            return Cs1DecoderCommands.parse(frame);
        }
        return Cs1ObjectCommands.parse(frame);
    }
}
