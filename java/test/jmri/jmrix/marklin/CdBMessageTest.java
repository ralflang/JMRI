package jmri.jmrix.marklin;

import jmri.util.JUnitUtil;
import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CdBMessage} and {@link McanFrameSender}.
 *
 * @author Ralf Lang (ralf.lang@ralf-lang.de) Copyright (C) 2026
 */
class CdBMessageTest {

    @BeforeEach
    void setUp() {
        JUnitUtil.setUp();
    }

    @AfterEach
    void tearDown() {
        JUnitUtil.tearDown();
    }

    @Test
    void testCreatePingFrame() {
        int[] frame = CdBMessage.createPing(0x1234);
        assertNotNull(frame);
        assertEquals(13, frame.length);

        MarklinCanCodec.DecodedMessage decoded = MarklinCanCodec.decode(frame);
        assertNotNull(decoded);
        assertEquals(CdBMessage.CMD_CONFIG, decoded.getCommand());
        assertFalse(decoded.isResponse());
        assertEquals(3, decoded.getDataLength());
        assertEquals(0x00, decoded.getDataByte(0)); // class 0
        assertEquals(0x00, decoded.getDataByte(1)); // module 0
        assertEquals(0x00, decoded.getDataByte(2)); // cdb cmd 0
    }

    @Test
    void testCreateReadParamFrame() {
        int[] frame = CdBMessage.createReadParam(0x22, 0x03, 0x01, 0x5678);
        assertNotNull(frame);

        MarklinCanCodec.DecodedMessage decoded = MarklinCanCodec.decode(frame);
        assertNotNull(decoded);
        assertEquals(CdBMessage.CMD_CONFIG, decoded.getCommand());
        assertEquals(4, decoded.getDataLength());
        assertEquals(0x22, decoded.getDataByte(0)); // class 0x22
        assertEquals(0x03, decoded.getDataByte(1)); // module 3
        assertEquals(0x01, decoded.getDataByte(2)); // cmd read
        assertEquals(0x01, decoded.getDataByte(3)); // param index 1
    }

    @Test
    void testCreateWriteParamFrame() {
        int[] frame = CdBMessage.createWriteParam(0x22, 0x03, 0x01, 0xAB, 0x5678);
        assertNotNull(frame);

        MarklinCanCodec.DecodedMessage decoded = MarklinCanCodec.decode(frame);
        assertNotNull(decoded);
        assertEquals(4, decoded.getDataLength());
        assertEquals(0x22, decoded.getDataByte(0));
        assertEquals(0x03, decoded.getDataByte(1));
        assertEquals(0x02, decoded.getDataByte(2)); // cmd write
        assertEquals(0x01, decoded.getDataByte(3)); // param index
    }

    @Test
    void testSendViaMcanFrameSender() {
        AtomicReference<int[]> sentFrame = new AtomicReference<>();
        McanFrameSender sender = sentFrame::set;

        int[] pingFrame = CdBMessage.createPing(0x0001);
        CdBMessage.send(sender, pingFrame);

        assertNotNull(sentFrame.get());
        assertArrayEquals(pingFrame, sentFrame.get());
    }
}
