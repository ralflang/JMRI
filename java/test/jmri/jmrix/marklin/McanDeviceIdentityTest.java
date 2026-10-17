package jmri.jmrix.marklin;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link McanDeviceIdentity}.
 */
public class McanDeviceIdentityTest {

    @Test
    public void testNativeIdentity() {
        McanDeviceIdentity.Native native1 = new McanDeviceIdentity.Native(0x00112233L, 0x0001, 0x1234);
        McanDeviceIdentity.Native native2 = new McanDeviceIdentity.Native(0x00112233L, 0x0001, 0x5678);
        McanDeviceIdentity.Native native3 = new McanDeviceIdentity.Native(0x00998877L, 0x0002, 0x1234);

        assertEquals("uid-00112233", native1.toSystemIdFragment());
        assertEquals(native1, native2);
        assertNotEquals(native1, native3);
        assertEquals(native1.hashCode(), native2.hashCode());
        assertTrue(native1.toHumanString().contains("0x00112233"));
    }

    @Test
    public void testCdBIdentity() {
        McanDeviceIdentity.CdB cdb1 = new McanDeviceIdentity.CdB(0x22, 3);
        McanDeviceIdentity.CdB cdb2 = cdb1.withModule(3);
        McanDeviceIdentity.CdB cdb3 = cdb1.withModule(4);

        assertEquals("cdb-22-03", cdb1.toSystemIdFragment());
        assertEquals(cdb1, cdb2);
        assertNotEquals(cdb1, cdb3);
        assertEquals(cdb1.hashCode(), cdb2.hashCode());

        // Check CdB CAN ID calculations for class 0x22 module 3
        // ping_id = ((0x2) << 21) | ((0x2) << 12) | 0x0A80 = 0x00402A80
        // ping = 0x00402A80 + 0x00000 + (3 + 0x80) = 0x00402B03
        assertEquals(0x00402B03L, cdb1.getPingId());
        assertEquals(0x00442B03L, cdb1.getReadId());
        assertEquals(0x00452B03L, cdb1.getReadReplyId());
        assertEquals(0x00462B03L, cdb1.getWriteId());
    }
}
