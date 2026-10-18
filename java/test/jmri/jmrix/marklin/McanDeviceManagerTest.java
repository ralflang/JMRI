package jmri.jmrix.marklin;

import jmri.util.JUnitUtil;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link McanDeviceManager}.
 *
 * @author Ralf Lang (ralf.lang@ralf-lang.de) Copyright (C) 2026
 */
class McanDeviceManagerTest {

    private MarklinSystemConnectionMemo memo;
    private MarklinTrafficControlScaffold tc;
    private McanDeviceManager manager;

    private static long cdBPingId(int canClass) {
        return ((long)(canClass >> 4) << 21)
             | ((long)(canClass & 0x0F) << 12)
             | 0x0A80L;
    }

    private static MarklinReply frameFromCanId(long canId, int dlc, int... data) {
        int[] raw = new int[13];
        raw[0] = (int)((canId >> 24) & 0xFF);
        raw[1] = (int)((canId >> 16) & 0xFF);
        raw[2] = (int)((canId >> 8)  & 0xFF);
        raw[3] = (int)( canId        & 0xFF);
        raw[4] = dlc;
        for (int i = 0; i < data.length && i < 8; i++) {
            raw[5 + i] = data[i] & 0xFF;
        }
        return new MarklinReply(raw);
    }

    @BeforeEach
    void setUp() {
        JUnitUtil.setUp();
        tc = new MarklinTrafficControlScaffold();
        memo = new MarklinSystemConnectionMemo(tc);
        memo.configureManagers();
        manager = memo.getMcanDeviceManager();
    }

    @AfterEach
    void tearDown() {
        tc.dispose();
        memo.dispose();
        JUnitUtil.tearDown();
    }

    @Test
    void testProvideAndGetDevice() {
        assertNotNull(manager);
        McanDeviceIdentity.Native nativeIdentity = McanDeviceIdentity.forNative(0x00112233L, MarklinDeviceIdentifier.DEVICE_TYPE_MS2);

        McanDevice device = manager.provideDevice(nativeIdentity);
        assertNotNull(device);
        assertEquals("M:dev:uid-00112233", device.getSystemName());

        McanDevice retrieved = manager.getDevice(nativeIdentity);
        assertSame(device, retrieved);
    }

    @Test
    void testPingResponseDiscovery() {
        McanDeviceIdentity.Native nativeIdentity = McanDeviceIdentity.forNative(0x00112233L, MarklinDeviceIdentifier.DEVICE_TYPE_MS2);
        assertNull(manager.getDevice(nativeIdentity));

        // PING response format (17 bytes: 5 header + 4 UID + 2 version + 2 device type)
        int[] pingData = new int[]{
            0x00, 0x31,             // Command 0x18, response flag set
            0x12, 0x34,             // Hash
            0x08,                   // DLC = 8
            0x00, 0x00, 0x00, 0x00, // CAN Address
            0x00, 0x11, 0x22, 0x33, // UID 0x00112233
            0x01, 0x02,             // SW v1.2
            0x00, 0x30              // MS2 type
        };

        MarklinCanCodec.DecodedMessage decoded = MarklinCanCodec.decode(pingData);
        MarklinDeviceIdentifier.DeviceInfo pingInfo = MarklinDeviceIdentifier.fromPingResponse(decoded);
        assertNotNull(pingInfo);
        assertEquals(0x00112233L, pingInfo.getUid());

        // Discovered device check
        McanDevice discovered = manager.provideDevice(nativeIdentity);
        discovered.setSoftwareVersion(pingInfo.getSoftwareVersion());
        discovered.setDeviceState(McanDeviceState.ONLINE);

        assertEquals(McanDeviceState.ONLINE, discovered.getDeviceState());
        assertEquals(0x0102, discovered.getSoftwareVersion());
    }

    @Test
    void testCdBConfigReplyDiscovery() {
        McanDeviceIdentity.CdB cdbIdentity = McanDeviceIdentity.forCdB(0x22, 0x03);
        assertNull(manager.getDevice(cdbIdentity));

        long pingReplyId = cdBPingId(0x22) + 0x10000;
        long withModule = (pingReplyId & 0xFFFFFF00L) | 0x83;

        MarklinReply reply = frameFromCanId(withModule, 2);
        CdBClassFilter.Classification classification = CdBClassFilter.classify(reply);
        assertTrue(classification.isCdBConfig());

        manager.cdBConfigReply(classification, reply);

        McanDevice device = manager.getDevice(cdbIdentity);
        assertNotNull(device);
        assertEquals(McanDeviceState.ONLINE, device.getDeviceState());
    }
}
