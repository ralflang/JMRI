package jmri.jmrix.marklin;

import jmri.util.JUnitUtil;
import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link McanParameterManager}.
 *
 * @author Ralf Lang (ralf.lang@ralf-lang.de) Copyright (C) 2026
 */
class McanParameterManagerTest {

    @BeforeEach
    void setUp() {
        JUnitUtil.setUp();
    }

    @AfterEach
    void tearDown() {
        JUnitUtil.tearDown();
    }

    @Test
    void testCdBReadAndWriteParameter() {
        McanDeviceIdentity identity = McanDeviceIdentity.forCdB(0x22, 0x03);
        McanDevice device = new McanDevice("M", identity);
        McanParameterManager paramManager = new McanParameterManager(device);

        assertEquals(device, paramManager.getDevice());

        AtomicReference<int[]> sentFrame = new AtomicReference<>();
        McanFrameSender sender = sentFrame::set;

        // Read parameter 1
        paramManager.readParameter(1, sender);
        assertNotNull(sentFrame.get());
        MarklinCanCodec.DecodedMessage decodedRead = MarklinCanCodec.decode(sentFrame.get());
        assertEquals(CdBMessage.CMD_CONFIG, decodedRead.getCommand());
        assertEquals(0x22, decodedRead.getDataByte(0)); // Class
        assertEquals(0x03, decodedRead.getDataByte(1)); // Module
        assertEquals(0x01, decodedRead.getDataByte(2)); // Read command
        assertEquals(0x01, decodedRead.getDataByte(3)); // Param index 1

        // Write parameter 1 value 0x55
        paramManager.writeParameter(1, 0x55, sender);
        assertNotNull(sentFrame.get());
        MarklinCanCodec.DecodedMessage decodedWrite = MarklinCanCodec.decode(sentFrame.get());
        assertEquals(CdBMessage.CMD_CONFIG, decodedWrite.getCommand());
        assertEquals(0x22, decodedWrite.getDataByte(0)); // Class
        assertEquals(0x03, decodedWrite.getDataByte(1)); // Module
        assertEquals(0x02, decodedWrite.getDataByte(2)); // Write command
        assertEquals(0x01, decodedWrite.getDataByte(3)); // Param index 1
    }
}
