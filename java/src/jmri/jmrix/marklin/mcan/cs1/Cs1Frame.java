package jmri.jmrix.marklin.mcan.cs1;

import jmri.jmrix.marklin.mcan.FrameProtocol;
import jmri.jmrix.marklin.mcan.McanFrame;
import jmri.jmrix.marklin.mcan.RawFrame;

/**
 * Base class for CS1/MS1 M-CAN frames.
 *
 * Also used by ECoS to communicate with MS1.
 * <p>
 * Identifier fields have
 * different meanings during registration, operation and firmware transfer.
 * This class is the fallback for unmodelled or ambiguous commands.
 */
public class Cs1Frame extends McanFrame {

    public Cs1Frame(RawFrame raw) {
        super(FrameProtocol.CS1_NATIVE, raw);
    }

    /** @return data length code (0-8) */
    public int dlc() {
        return raw().dlc();
    }

    /** @return the 29 bit CAN identifier */
    public int canId() {
        return (raw().byteAt(0) << 24) | (raw().byteAt(1) << 16)
                | (raw().byteAt(2) << 8) | raw().byteAt(3);
    }

    /** @return identifier bits 28-26 */
    public int priority() { return (canId() >> 26) & 7; }

    /** @return identifier bits 6-0: Assigned, registration or firmware node */
    public int node() { return canId() & 0x7F; }

    protected final int dataByte(int index) {
        if (index < 0 || index >= dlc()) {
            throw new IndexOutOfBoundsException("CS1 payload index outside DLC: " + index);
        }
        return raw().byteAt(5 + index);
    }

    protected final int data16(int index) {
        return (dataByte(index) << 8) | dataByte(index + 1);
    }

    protected final long data32(int index) {
        return ((long) data16(index) << 16) | data16(index + 2);
    }

    protected final int[] dataBytes(int start) {
        int[] result = new int[dlc() - start];
        for (int i = 0; i < result.length; i++) {
            result[i] = dataByte(start + i);
        }
        return result;
    }
}
