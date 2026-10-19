package jmri.jmrix.marklin.swing.devicetable;

import jmri.jmrix.marklin.*;
import jmri.util.JUnitUtil;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link McanDevicePanel}.
 *
 * @author Ralf Lang (ralf.lang@ralf-lang.de)
 */
class McanDevicePanelTest {

    private MarklinSystemConnectionMemo memo;
    private MarklinTrafficControlScaffold tc;

    @BeforeEach
    void setUp() {
        JUnitUtil.setUp();
        tc = new MarklinTrafficControlScaffold();
        memo = new MarklinSystemConnectionMemo(tc);
        memo.configureManagers();
    }

    @AfterEach
    void tearDown() {
        tc.dispose();
        memo.dispose();
        JUnitUtil.tearDown();
    }

    @Test
    @jmri.util.junit.annotations.DisabledIfHeadless
    void testPanelInitialization() {
        McanDevicePanel panel = new McanDevicePanel();
        panel.initComponents(memo);

        assertNotNull(panel.getDiscoverMarklinButton());
        assertNotNull(panel.getDiscoverCdbButton());
        assertNotNull(panel.getDeviceTable());
        assertNotNull(panel.getDataModel());
        assertEquals("MCAN Devices (" + memo.getUserName() + ")", panel.getTitle());
    }

    @Test
    @jmri.util.junit.annotations.DisabledIfHeadless
    void testDiscoverMarklinButtonAction() {
        McanDevicePanel panel = new McanDevicePanel();
        panel.initComponents(memo);

        int initialSent = tc.getSentMessages().size();
        panel.getDiscoverMarklinButton().doClick();

        assertEquals(initialSent + 1, tc.getSentMessages().size());
        MarklinMessage sent = tc.getLastMessageSent();
        assertNotNull(sent);
        MarklinCanCodec.DecodedMessage decoded = MarklinCanCodec.decode(sent);
        assertEquals(MarklinConstants.CMDPING, decoded.getCommand());
        assertFalse(decoded.isResponse());
    }

    @Test
    @jmri.util.junit.annotations.DisabledIfHeadless
    void testDiscoverCdbButtonAction() {
        McanDevicePanel panel = new McanDevicePanel();
        panel.initComponents(memo);

        int initialSent = tc.getSentMessages().size();
        panel.getDiscoverCdbButton().doClick();

        // All CdB device classes enabled by default
        int expectedCount = CdBMessage.CdBDeviceClass.values().length;
        assertEquals(initialSent + expectedCount, tc.getSentMessages().size());
    }

    @Test
    @jmri.util.junit.annotations.DisabledIfHeadless
    void testCdbPingFormat() {
        McanDevicePanel panel = new McanDevicePanel();
        panel.initComponents(memo);

        tc.getSentMessages().clear();
        panel.getDiscoverCdbButton().doClick();

        // Verify each ping has correct format: zero DLC, proper ping_id calculation
        CdBMessage.CdBDeviceClass[] deviceClasses = CdBMessage.CdBDeviceClass.values();
        assertEquals(deviceClasses.length, tc.getSentMessages().size());

        for (int i = 0; i < deviceClasses.length; i++) {
            MarklinMessage msg = tc.getSentMessages().get(i);
            CdBMessage.CdBDeviceClass deviceClass = deviceClasses[i];
            int expectedPingId = deviceClass.getPingId();

            // Verify CAN identifier
            int actualId = ((msg.getElement(0) & 0xFF) << 24) |
                          ((msg.getElement(1) & 0xFF) << 16) |
                          ((msg.getElement(2) & 0xFF) << 8) |
                          (msg.getElement(3) & 0xFF);
            assertEquals(expectedPingId, actualId,
                String.format("Ping ID mismatch for %s (0x%02X)",
                    deviceClass.getDisplayName(), deviceClass.getClassId()));

            // Verify DLC is 0
            assertEquals(0, msg.getElement(4),
                String.format("DLC should be 0 for %s ping", deviceClass.getDisplayName()));
        }
    }

    @Test
    @jmri.util.junit.annotations.DisabledIfHeadless
    void testTableReactsToNewDevices() {
        McanDevicePanel panel = new McanDevicePanel();
        panel.initComponents(memo);

        // Initially no devices
        assertEquals(0, panel.getDataModel().getRowCount());

        // Simulate a device being discovered on the bus
        McanDeviceIdentity.CdB identity = McanDeviceIdentity.forCdB(0x27, 0x01);
        McanDevice device = memo.getMcanDeviceManager().provideDevice(identity);
        device.setDeviceState(McanDeviceState.ONLINE);

        // Table should now show the device
        assertEquals(1, panel.getDataModel().getRowCount());
        assertEquals(device.getSystemName(), panel.getDataModel().getValueAt(0, McanDeviceTableDataModel.COLUMN_SYS_NAME));
    }

    @Test
    @jmri.util.junit.annotations.DisabledIfHeadless
    void testTableReactsToDeviceUpdates() {
        McanDevicePanel panel = new McanDevicePanel();
        panel.initComponents(memo);

        // Add a device
        McanDeviceIdentity.CdB identity = McanDeviceIdentity.forCdB(0x22, 0x02);
        McanDevice device = memo.getMcanDeviceManager().provideDevice(identity);
        device.setDeviceState(McanDeviceState.DISCOVERED);

        assertEquals(1, panel.getDataModel().getRowCount());
        assertEquals(McanDeviceState.DISCOVERED, panel.getDataModel().getValueAt(0, McanDeviceTableDataModel.COLUMN_STATE));

        // Update device state
        device.setDeviceState(McanDeviceState.ONLINE);

        // Table should reflect the change
        assertEquals(McanDeviceState.ONLINE, panel.getDataModel().getValueAt(0, McanDeviceTableDataModel.COLUMN_STATE));
    }
}
}
