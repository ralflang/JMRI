package jmri.jmrix.marklin.mcan.classify;

import jmri.jmrix.marklin.mcan.McanFrame;
import jmri.jmrix.marklin.mcan.RawFrame;
import jmri.jmrix.marklin.mcan.UnknownFrame;
import jmri.jmrix.marklin.mcan.cdb.CdBFrameParser;
import jmri.jmrix.marklin.mcan.cs1.Cs1Frame;
import jmri.jmrix.marklin.mcan.cs1.Cs1FrameParser;
import jmri.jmrix.marklin.mcan.cs2.Cs2FrameParser;
import jmri.jmrix.marklin.mcan.cs2.Cs2Header;

/**
 * Protocol classifier for 13-byte M&auml;rklin CAN frames.
 * <p>
 * Determines the protocol family and returns the fully parsed, typed frame:
 * a {@link jmri.jmrix.marklin.mcan.cs2.Cs2Frame} subtype, a
 * {@link jmri.jmrix.marklin.mcan.cdb.CdBFrameParser.CdBFrame} subtype,
 * a {@link Cs1Frame} subtype or an {@link UnknownFrame}.
 * <p>
 * This package sits above {@code cs2}, {@code cdb} and {@code cs1}.
 * None of them may depend on it.
 */
public final class ProtocolClassifier {

    private ProtocolClassifier() {}

    /**
     * Classify and parse a raw 13-byte CAN frame.
     *
     * <ol>
     *   <li>If the CS2 fingerprint is present: CS2 native frame</li>
     *   <li>Else if the CdB hash pattern is present: CdB configuration frame</li>
     *   <li>Else if a documented MS1 M-CAN layout matches: CS1/MS1 frame</li>
     *   <li>Else: {@link UnknownFrame}</li>
     * </ol>
     *
     * @param raw13 13-byte CAN frame
     * @return typed frame, never null
     * @throws IllegalArgumentException if {@code raw13} is null or not 13 bytes long
     */
    public static McanFrame classify(int[] raw13) {
        RawFrame frame = new RawFrame(raw13);
        Cs2Header cs2 = Cs2Header.tryParse(raw13);
        if (cs2 != null) {
            return Cs2FrameParser.parse(frame, cs2);
        }
        if (isCdBConfigPattern(raw13)) {
            return CdBFrameParser.parse(frame);
        }
        if (isCs1Pattern(frame)) {
            return Cs1FrameParser.parse(frame);
        }
        return new UnknownFrame(frame);
    }

    /**
     * Structural recognition rather than a unique fingerprint. Based on sections 6.1,
     * 6.2, 6.3.1 and 9.3 of
     * <a href="https://www.skrauss.de/modellbahn/CAN_Doku_V101.pdf">MS1 CAN,
     * documentation, version 1.1</a>. Undocumented ECoSLink E-CAN is not covered.
     * CS2 and CdB recognition must take precedence.
     */
    private static boolean isCs1Pattern(RawFrame frame) {
        if (frame.dlc() > 8 || (frame.byteAt(0) & 0xE0) != 0) {
            return false;
        }
        int canId = (frame.byteAt(0) << 24) | (frame.byteAt(1) << 16)
                | (frame.byteAt(2) << 8) | frame.byteAt(3);
        int priority = (canId >> 26) & 7;
        int node = canId & 0x7F;
        if (priority == 7) {
            if (node == 127) {
                return true; // Firmware transfer has its own identifier layout.
            }
            if (node != 126) {
                return false;
            }
            int uidByte = (canId >> 18) & 0xFF;
            int stage = (canId >> 15) & 7;
            int command = (canId >> 7) & 7;
            boolean master = (canId & 0x80) != 0;
            if (command > 1 || (stage >= 4 && uidByte != 0)) {
                return false;
            }
            if (master && stage < 4) {
                return true; // Fragment acknowledgements are identified by their header.
            }
            if (master && stage == 7 && frame.dlc() == 7) {
                return true; // Node assignment without the optional flags byte.
            }
            return frame.dlc() == (master || stage >= 4 ? 8 : 0);
        }
        if (canId == 0x380 && frame.dlc() == 8) {
            return true; // MS1 master bus-occupied probe, with object handle zero.
        }
        int objectHandle = (canId >> 10) & 0xFFFF;
        int command = (canId >> 7) & 7;
        // The original MS1 master uses handle zero for monitoring.
        if (priority == 3 && command == 7 && node <= 125 && objectHandle == 0) {
            return (frame.dlc() == 4 || frame.dlc() == 8)
                    && frame.byteAt(5) == 0 && frame.byteAt(6) == 0;
        }
        // Normal traffic uses assigned nodes, nonzero handles and priorities 2-6.
        // Command bits 110 are reserved for the CS2 fingerprint.
        return priority >= 2 && priority <= 6 && node <= 125
                && objectHandle != 0 && command != 6;
    }

    /**
     * True when bytes 2–3 of the frame carry the CdB config area hash pattern.
     * The pattern {@code 0x0A80} occupies bits 11–7 of the 16-bit hash field,
     * which is mutually exclusive with the CS2 fingerprint.
     *
     * @param raw13 13-byte CAN frame
     * @return true for a CdB configuration frame
     */
    public static boolean isCdBConfigPattern(int[] raw13) {
        int hash16 = ((raw13[2] & 0xFF) << 8) | (raw13[3] & 0xFF);
        return (hash16 & 0x0F80) == 0x0A80;
    }
}
