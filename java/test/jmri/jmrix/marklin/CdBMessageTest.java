package jmri.jmrix.marklin;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CdBMessage} and {@link CdBReadReply}.
 */
public class CdBMessageTest {

    @Test
    public void testCdBMessageFactories() {
        McanDeviceIdentity.CdB addr = new McanDeviceIdentity.CdB(0x22, 3);

        MarklinMessage pingMsg = CdBMessage.ping(0x22);
        assertNotNull(pingMsg);
        assertEquals(13, pingMsg.getNumDataElements());

        MarklinMessage readMsg = CdBMessage.readParam(addr, 0, 1);
        assertNotNull(readMsg);
        assertEquals(2, readMsg.getElement(4)); // DLC 2

        MarklinMessage writeMsg = CdBMessage.writeParam(addr, 0, 1, 0x12, 0x34);
        assertNotNull(writeMsg);
        assertEquals(4, writeMsg.getElement(4)); // DLC 4
    }

    @Test
    public void testCdBReadReply() {
        int[] raw = new int[]{0, 1, 0x12, 0x34};
        CdBReadReply reply = new CdBReadReply(0, 1, 0x12, 0x34, 4, raw);

        assertEquals(0, reply.getIndex());
        assertEquals(1, reply.getParam());
        assertEquals(0x12, reply.getValueHi());
        assertEquals(0x34, reply.getValueLo());
        assertEquals(0x1234, reply.getValue16());
        assertEquals(4, reply.getDlc());
        assertArrayEquals(raw, reply.getRawData());
    }
}
