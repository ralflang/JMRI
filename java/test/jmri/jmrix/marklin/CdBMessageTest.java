package jmri.jmrix.marklin;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link CdBMessage} and {@link McanFrameSender}.
 *
 * @author Ralf Lang (ralf.lang@ralf-lang.de)
 */
class CdBMessageTest {

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

    @Test
    void testDeselectModuleFrame() {
        int[] frame = CdBMessage.createDeselectModule(0x27, 3);
        assertEquals(13, frame.length);
        // byte[3] = 0x80 + 3 = 0x83 (write_id for class 0x27 with module 3)
        assertEquals(0x83, frame[3] & 0xFF);
        // DLC = 2 at byte[4]
        assertEquals(2, frame[4] & 0xFF);
        // data[0] = subcmd 0x00, data[1] = 0xFE (deselect)
        assertEquals(0x00, frame[5] & 0xFF);
        assertEquals(0xFE, frame[6] & 0xFF);
    }

    @Test
    void testSelectModuleFrame() {
        int[] frame = CdBMessage.createSelectModule(0x27, 5);
        assertEquals(13, frame.length);
        assertEquals(0x85, frame[3] & 0xFF); // module 5
        assertEquals(4, frame[4] & 0xFF);    // DLC = 4
        assertEquals(0x00, frame[5] & 0xFF);
        assertEquals(0xF7, frame[6] & 0xFF); // 247 = select command
        assertEquals(0x00, frame[7] & 0xFF);
        assertEquals(0x01, frame[8] & 0xFF);
    }

    @Test
    void testEncodeAddressType2_DCC() {
        assertEquals(14336 + 10 - 1, CdBMessage.encodeAddress("DCC", 10));
    }

    @Test
    void testEncodeAddressType2_MM() {
        assertEquals(12288 + 5 - 1, CdBMessage.encodeAddress("MM", 5));
    }

    @Test
    void testEncodeAddressType2_CAN() {
        assertEquals(100, CdBMessage.encodeAddress("CAN", 100));
    }

    @Test
    void testDecodeAddressRoundTrip_DCC() {
        int raw = CdBMessage.encodeAddress("DCC", 42);
        CdBMessage.AddressEncoding enc = CdBMessage.decodeAddress(raw);
        assertEquals("DCC", enc.format);
        assertEquals(42, enc.address);
    }

    @Test
    void testDecodeAddressRoundTrip_MM() {
        int raw = CdBMessage.encodeAddress("MM", 15);
        CdBMessage.AddressEncoding enc = CdBMessage.decodeAddress(raw);
        assertEquals("MM", enc.format);
        assertEquals(15, enc.address);
    }

    @Test
    void testDecodeAddressRoundTrip_CAN() {
        int raw = CdBMessage.encodeAddress("CAN", 200);
        CdBMessage.AddressEncoding enc = CdBMessage.decodeAddress(raw);
        assertEquals("CAN", enc.format);
        assertEquals(200, enc.address);
    }

    @Test
    void testEncodeAddressType3_DCC() {
        int raw = CdBMessage.encodeAddressType3("DCC", 1);
        CdBMessage.AddressEncoding enc = CdBMessage.decodeAddressType3(raw);
        assertEquals("DCC", enc.format);
        assertEquals(1, enc.address);
    }

    @Test
    void testEncodeAddressType3_DCCext() {
        int raw = CdBMessage.encodeAddressType3("DCC ext.", 10);
        CdBMessage.AddressEncoding enc = CdBMessage.decodeAddressType3(raw);
        assertEquals("DCC ext.", enc.format);
        assertEquals(10, enc.address);
    }

    @Test
    void testPingIdRoundTrip_class27() {
        int pingId = CdBMessage.CdBDeviceClass.WEICHEN_CHEF_KOMBI.getPingId();
        // The class value 0x27 should be recoverable from the pingId
        int classHigh = (pingId >> 21) & 0x0F;
        int classLow  = (pingId >> 12) & 0x0F;
        int recovered = (classHigh << 4) | classLow;
        assertEquals(0x27, recovered);
    }

    @Test
    void testCreateWriteChSwitchingTimeFrame() {
        int[] frame = CdBMessage.createWriteChSwitchingTime(0x27, 3, 2, 500);
        assertEquals(13, frame.length);
        assertEquals(0x83, frame[3] & 0xFF); // module 3
        assertEquals(4, frame[4] & 0xFF);    // DLC = 4
        assertEquals(2, frame[5] & 0xFF);    // channel
        assertEquals(3, frame[6] & 0xFF);    // reg 3 = switching time
        // 500 = 0x01F4: hi=0x01 at frame[7], lo=0xF4 at frame[8]
        assertEquals(0x01, frame[7] & 0xFF);
        assertEquals(0xF4, frame[8] & 0xFF);
    }

    @Test
    void testCreateReadAllPropertiesWeichenChef_notEmpty() {
        int[][] frames = CdBMessage.createReadAllPropertiesWeichenChef(0x27, 3);
        assertTrue(frames.length > 0, "should return at least one frame");
        for (int[] f : frames) {
            assertEquals(13, f.length);
        }
    }

    @Test
    void testCreateReadAllPropertiesReporter_notEmpty() {
        int[][] frames = CdBMessage.createReadAllPropertiesReporter(0x0D, 1);
        assertTrue(frames.length > 0, "should return at least one frame");
        for (int[] f : frames) {
            assertEquals(13, f.length);
        }
    }
}
