package jmri.jmrix.marklin;

import jmri.util.JUnitUtil;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MarklinCanCodec class.
 *
 * @author Generated for issue #14104
 */
public class MarklinCanCodecTest {

    @Test
    public void testDecodeSystemGoCommand() {
        // System Go command
        int[] message = new int[]{
            0x00, 0x00, 0x47, 0x11, 0x05,
            0x00, 0x00, 0x00, 0x00,
            0x01, 0x00, 0x00, 0x00
        };

        MarklinCanCodec.DecodedMessage decoded = MarklinCanCodec.decode(message);

        assertEquals(MarklinConstants.PRIO_1, decoded.getPriority());
        assertEquals(MarklinConstants.SYSCOMMANDSTART, decoded.getCommand());
        assertFalse(decoded.isResponse());
        assertEquals(0x47, decoded.getHash()[0]);
        assertEquals(0x11, decoded.getHash()[1]);
        assertEquals(0x00, decoded.getAddress());
        assertEquals(1, decoded.getDataLength());
        assertEquals(MarklinConstants.CMDGOSYS, decoded.getDataByte(0));
    }

    @Test
    public void testDecodeLocoSpeedCommand() {
        // Loco speed command for DCC address 3, speed 500
        int[] message = new int[]{
            0x00, 0x08, 0x47, 0x11, 0x06,
            0x00, 0x00, 0xC0, 0x03, // DCC address 3
            0x01, (byte) 0xF4, 0x00, 0x00  // speed 500 (0x01F4)
        };

        MarklinCanCodec.DecodedMessage decoded = MarklinCanCodec.decode(message);

        assertEquals(MarklinConstants.PRIO_1, decoded.getPriority());
        assertEquals(MarklinConstants.LOCOSPEED, decoded.getCommand());
        assertEquals(0xC003, decoded.getAddress());
        assertEquals(2, decoded.getDataLength());
        assertEquals(0x01, decoded.getDataByte(0));
        assertEquals(0xF4, decoded.getDataByte(1));
    }

    @Test
    public void testDecodeCanBootCommand() {
        // CAN BOOT command (0x1B)
        int[] message = new int[]{
            0x00, 0x36, 0x47, 0x11, 0x04,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00
        };

        MarklinCanCodec.DecodedMessage decoded = MarklinCanCodec.decode(message);

        assertEquals(MarklinConstants.PRIO_1, decoded.getPriority());
        assertEquals(MarklinConstants.CMDCANBOOT, decoded.getCommand());
        assertFalse(decoded.isResponse());
        assertEquals(0, decoded.getDataLength());
    }

    @Test
    public void testDecodeTurnoutCommand() {
        // Accessory command for DCC turnout 1, state closed (1), power on
        int[] message = new int[]{
            0x00, 0x16, 0x47, 0x11, 0x06,
            0x00, 0x00, 0x38, 0x00, // DCC accessory address 0x3800
            0x01, 0x01, 0x00, 0x00
        };

        MarklinCanCodec.DecodedMessage decoded = MarklinCanCodec.decode(message);

        assertEquals(MarklinConstants.PRIO_1, decoded.getPriority());
        assertEquals(MarklinConstants.ACCCOMMANDSTART, decoded.getCommand());
        assertEquals(0x3800, decoded.getAddress());
        assertEquals(2, decoded.getDataLength());
        assertEquals(0x01, decoded.getDataByte(0)); // closed
        assertEquals(0x01, decoded.getDataByte(1)); // power on
    }

    @Test
    public void testBuildSystemGoCommand() {
        int[] encoded = MarklinCanCodec.builder()
            .setCommand(MarklinConstants.SYSCOMMANDSTART)
            .setAddress(0x00)
            .setData(MarklinConstants.CMDGOSYS)
            .setDataLength(1)
            .build();

        assertEquals(13, encoded.length);
        assertEquals(0x00, encoded[0]); // Priority 0, command high bits
        assertEquals(0x00, encoded[1]); // Command low bits, not response
        assertEquals(MarklinConstants.HASHBYTE1, encoded[2]);
        assertEquals(MarklinConstants.HASHBYTE2, encoded[3]);
        assertEquals(0x05, encoded[4]); // DLC: 4 address bytes + 1 data byte
        assertEquals(0x00, encoded[5]); // Address byte 1
        assertEquals(0x00, encoded[6]); // Address byte 2
        assertEquals(0x00, encoded[7]); // Address byte 3
        assertEquals(0x00, encoded[8]); // Address byte 4
        assertEquals(MarklinConstants.CMDGOSYS, encoded[9]); // Data byte
    }

    @Test
    public void testBuildLocoSpeedCommand() {
        int speed = 500;
        int[] encoded = MarklinCanCodec.builder()
            .setCommand(MarklinConstants.LOCOSPEED)
            .setAddress(0xC003) // DCC address 3
            .setData((speed >> 8) & 0xff, speed & 0xff)
            .setDataLength(2)
            .build();

        assertEquals(13, encoded.length);
        assertEquals(0x00, encoded[0] & 0xC0); // Priority 0
        assertEquals(0xC003,
            ((long)encoded[5] << 24) | ((long)encoded[6] << 16) |
            ((long)encoded[7] << 8) | (long)encoded[8]);
        assertEquals(0x01, encoded[9]); // Speed high byte
        assertEquals(0xF4, encoded[10]); // Speed low byte (500 = 0x01F4)
    }

    @Test
    public void testBuildCanBootCommand() {
        int[] encoded = MarklinCanCodec.builder()
            .setCommand(MarklinConstants.CMDCANBOOT)
            .setAddress(0x00)
            .setDataLength(0)
            .build();

        assertEquals(13, encoded.length);
        assertEquals(0x00, encoded[0]);
        assertEquals(0x36, encoded[1]); // Command 0x1B encoded: (0x1B << 1) = 0x36
        assertEquals(0x04, encoded[4]); // DLC: 4 address bytes + 0 data bytes
    }

    @Test
    public void testGetPriorityDescription() {
        assertEquals("Priority 1: Stop/Go/Short",
            MarklinCanCodec.getPriorityDescription(MarklinConstants.PRIO_1));
        assertEquals("Priority 2: Feedback",
            MarklinCanCodec.getPriorityDescription(MarklinConstants.PRIO_2));
        assertEquals("Priority 3: Engine Stop",
            MarklinCanCodec.getPriorityDescription(MarklinConstants.PRIO_3));
        assertEquals("Priority 4: Engine/Accessory Command",
            MarklinCanCodec.getPriorityDescription(MarklinConstants.PRIO_4));
    }

    @Test
    public void testGetProtocolFromAddress() {
        assertEquals("Broadcast", MarklinCanCodec.getProtocolFromAddress(0));
        assertEquals("MM1/MM2 Loco", MarklinCanCodec.getProtocolFromAddress(0x0001));
        assertEquals("DCC", MarklinCanCodec.getProtocolFromAddress(0xC003));
        assertEquals("DCC Accessory", MarklinCanCodec.getProtocolFromAddress(0x3800));
        assertEquals("MFX", MarklinCanCodec.getProtocolFromAddress(0x4001));
        assertEquals("Selectrix SX2", MarklinCanCodec.getProtocolFromAddress(0x8001));
    }

    @Test
    public void testGetBaseAddress() {
        assertEquals(0, MarklinCanCodec.getBaseAddress(0));
        assertEquals(1, MarklinCanCodec.getBaseAddress(0x0001));
        assertEquals(3, MarklinCanCodec.getBaseAddress(0xC003));
        assertEquals(0, MarklinCanCodec.getBaseAddress(0x3800));
        assertEquals(1, MarklinCanCodec.getBaseAddress(0x4001));
    }

    @Test
    public void testDecodeResponseFlag() {
        // Request message
        int[] request = new int[]{
            0x00, 0x00, 0x47, 0x11, 0x05,
            0x00, 0x00, 0x00, 0x00,
            0x01, 0x00, 0x00, 0x00
        };

        // Response message (bit 0 of byte 1 set)
        int[] response = new int[]{
            0x00, 0x01, 0x47, 0x11, 0x05,
            0x00, 0x00, 0x00, 0x00,
            0x01, 0x00, 0x00, 0x00
        };

        assertFalse(MarklinCanCodec.decode(request).isResponse());
        assertTrue(MarklinCanCodec.decode(response).isResponse());
    }

    @Test
    public void testCommandCategories() {
        int[] systemMsg = new int[]{0x00, 0x00, 0x47, 0x11, 0x05, 0, 0, 0, 0, 0x01, 0, 0, 0};
        assertEquals("SYSTEM", MarklinCanCodec.decode(systemMsg).getCommandCategory());

        int[] locoMsg = new int[]{0x00, 0x08, 0x47, 0x11, 0x06, 0, 0, 0xC0, 3, 1, 0xF4, 0, 0};
        assertEquals("MANAGEMENT", MarklinCanCodec.decode(locoMsg).getCommandCategory());

        int[] accMsg = new int[]{0x00, 0x16, 0x47, 0x11, 0x06, 0, 0, 0x38, 0, 1, 1, 0, 0};
        assertEquals("ACCESSORY", MarklinCanCodec.decode(accMsg).getCommandCategory());

        int[] bootMsg = new int[]{0x00, 0x36, 0x47, 0x11, 0x04, 0, 0, 0, 0, 0, 0, 0, 0};
        assertEquals("SOFTWARE", MarklinCanCodec.decode(bootMsg).getCommandCategory());
    }

    @Test
    public void testRoundTrip() {
        // Build a message, then decode it and verify all fields match
        int[] encoded = MarklinCanCodec.builder()
            .setPriority(MarklinConstants.PRIO_4)
            .setCommand(MarklinConstants.LOCOFUNCTION)
            .setResponse(false)
            .setAddress(0xC003)
            .setData(5, 1) // Function 5, state on
            .setDataLength(2)
            .build();

        MarklinCanCodec.DecodedMessage decoded = MarklinCanCodec.decode(encoded);

        assertEquals(MarklinConstants.PRIO_4, decoded.getPriority());
        assertEquals(MarklinConstants.LOCOFUNCTION, decoded.getCommand());
        assertFalse(decoded.isResponse());
        assertEquals(0xC003, decoded.getAddress());
        assertEquals(2, decoded.getDataLength());
        assertEquals(5, decoded.getDataByte(0));
        assertEquals(1, decoded.getDataByte(1));
    }

    @BeforeEach
    public void setUp() {
        JUnitUtil.setUp();
    }

    @AfterEach
    public void tearDown() {
        JUnitUtil.tearDown();
    }
}
