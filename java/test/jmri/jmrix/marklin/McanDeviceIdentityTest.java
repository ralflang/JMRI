package jmri.jmrix.marklin;

import jmri.util.JUnitUtil;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link McanDeviceIdentity}.
 *
 * @author Ralf Lang (ralf.lang@ralf-lang.de) Copyright (C) 2026
 */
class McanDeviceIdentityTest {

    @BeforeEach
    void setUp() {
        JUnitUtil.setUp();
    }

    @AfterEach
    void tearDown() {
        JUnitUtil.tearDown();
    }

    @Test
    void testNativeIdentityCreationAndProperties() {
        McanDeviceIdentity.Native nativeDev = McanDeviceIdentity.forNative(0x00112233L, MarklinDeviceIdentifier.DEVICE_TYPE_MS2);

        assertTrue(nativeDev.isNative());
        assertFalse(nativeDev.isCdB());
        assertEquals(0x00112233L, nativeDev.getUid());
        assertEquals(MarklinDeviceIdentifier.DEVICE_TYPE_MS2, nativeDev.getDeviceType());
        assertEquals("uid-00112233", nativeDev.getSystemIdFragment());
        assertEquals("M:dev:uid-00112233", nativeDev.getSystemName("M"));
        assertEquals("M2:dev:uid-00112233", nativeDev.getSystemName("M2"));
        assertEquals("M:dev:uid-00112233", nativeDev.getSystemName(null));
        assertTrue(nativeDev.getDisplayName().contains("MS2"));
    }

    @Test
    void testCdBIdentityCreationAndProperties() {
        McanDeviceIdentity.CdB cdbDev = McanDeviceIdentity.forCdB(0x22, 0x03);

        assertFalse(cdbDev.isNative());
        assertTrue(cdbDev.isCdB());
        assertEquals(0x22, cdbDev.getCanClass());
        assertEquals(0x03, cdbDev.getModuleId());
        assertEquals("cdb-22-03", cdbDev.getSystemIdFragment());
        assertEquals("M:dev:cdb-22-03", cdbDev.getSystemName("M"));
        assertTrue(cdbDev.getDisplayName().contains("CdB Module"));
    }

    @Test
    void testEqualityAndHashCode() {
        McanDeviceIdentity.Native native1 = McanDeviceIdentity.forNative(0x12345678L, 0x0010);
        McanDeviceIdentity.Native native2 = McanDeviceIdentity.forNative(0x12345678L, 0x0010);
        McanDeviceIdentity.Native native3 = McanDeviceIdentity.forNative(0x87654321L, 0x0010);

        assertEquals(native1, native2);
        assertEquals(native1.hashCode(), native2.hashCode());
        assertNotEquals(native1, native3);

        McanDeviceIdentity.CdB cdb1 = McanDeviceIdentity.forCdB(0x10, 0x05);
        McanDeviceIdentity.CdB cdb2 = McanDeviceIdentity.forCdB(0x10, 0x05);
        McanDeviceIdentity.CdB cdb3 = McanDeviceIdentity.forCdB(0x10, 0x06);

        assertEquals(cdb1, cdb2);
        assertEquals(cdb1.hashCode(), cdb2.hashCode());
        assertNotEquals(cdb1, cdb3);

        assertNotEquals(native1, cdb1);
    }
}
