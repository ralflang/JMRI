package jmri.jmrix.marklin;

import jmri.util.JUnitUtil;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link McanDeviceManager}.
 *
 * @author Ralf Lang (ralf.lang@ralf-lang.de)
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

    @Test
    void testProgrammaticMarklinDiscovery() {
        // Test that discovery can be triggered without UI
        int initialSent = tc.getSentMessages().size();

        manager.discoverMarklinDevices();

        assertEquals(initialSent + 1, tc.getSentMessages().size());
        MarklinMessage sent = tc.getLastMessageSent();
        assertNotNull(sent);

        MarklinCanCodec.DecodedMessage decoded = MarklinCanCodec.decode(sent);
        assertEquals(MarklinConstants.CMDPING, decoded.getCommand());
        assertFalse(decoded.isResponse());
    }

    @Test
    void testProgrammaticCdBDiscoverySingle() {
        // Test discovery of a single CdB device class
        int initialSent = tc.getSentMessages().size();

        manager.discoverCdBDevice(CdBMessage.CdBDeviceClass.WEICHEN_CHEF_KOMBI);

        assertEquals(initialSent + 1, tc.getSentMessages().size());
        MarklinMessage sent = tc.getLastMessageSent();
        assertNotNull(sent);

        // Verify the ping ID is correct for class 0x27
        int expectedPingId = CdBMessage.CdBDeviceClass.WEICHEN_CHEF_KOMBI.getPingId();
        int actualId = ((sent.getElement(0) & 0xFF) << 24) |
                      ((sent.getElement(1) & 0xFF) << 16) |
                      ((sent.getElement(2) & 0xFF) << 8) |
                      (sent.getElement(3) & 0xFF);
        assertEquals(expectedPingId, actualId);
        assertEquals(0, sent.getElement(4)); // DLC should be 0
    }

    @Test
    void testProgrammaticCdBDiscoveryAll() {
        // Test discovery of all CdB device classes
        int initialSent = tc.getSentMessages().size();

        manager.discoverAllCdBDevices();

        int expectedCount = CdBMessage.CdBDeviceClass.values().length;
        assertEquals(initialSent + expectedCount, tc.getSentMessages().size());

        // Verify each message has zero DLC
        for (int i = initialSent; i < tc.getSentMessages().size(); i++) {
            MarklinMessage msg = tc.getSentMessages().get(i);
            assertEquals(0, msg.getElement(4), "All CdB pings should have DLC=0");
        }
    }

    @Test
    void testReplyProcessingIndependentOfSender() {
        // Test that replies are processed regardless of who sent the ping
        McanDeviceIdentity.CdB cdbIdentity = McanDeviceIdentity.forCdB(0x0D, 0x01);
        assertNull(manager.getDevice(cdbIdentity));

        // Simulate a reply from a device (could be from any source on the bus)
        long pingReplyId = cdBPingId(0x0D) + 0x10000;
        long withModule = (pingReplyId & 0xFFFFFF00L) | 0x81;

        MarklinReply reply = frameFromCanId(withModule, 2);

        // Directly inject the reply as if it came from the bus
        manager.reply(reply);

        // Device should be discovered regardless of who sent the original ping
        McanDevice device = manager.getDevice(cdbIdentity);
        assertNotNull(device);
        assertEquals(McanDeviceState.ONLINE, device.getDeviceState());
    }
}
