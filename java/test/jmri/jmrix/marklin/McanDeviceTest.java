package jmri.jmrix.marklin;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link McanDevice}.
 */
public class McanDeviceTest {

    @Test
    public void testDeviceCreationAndProperties() {
        McanDeviceIdentity.Native identity = new McanDeviceIdentity.Native(0x00112233L, 0x0001, 0x1234);
        McanDevice device = new McanDevice("M:dev:uid-00112233", identity, "Märklin Gleisbox");

        assertEquals("M:dev:uid-00112233", device.getSystemName());
        assertEquals(identity, device.getIdentity());
        assertEquals("Märklin Gleisbox", device.getFamilyName());
        assertEquals(McanDeviceState.DISCOVERED, device.getDeviceState());

        device.setDeviceState(McanDeviceState.ONLINE);
        assertEquals(McanDeviceState.ONLINE, device.getDeviceState());

        device.setFirmwareVersion(0x0102);
        assertEquals(0x0102, device.getFirmwareVersion());

        assertNotNull(device.getLastSeen());
    }
}
