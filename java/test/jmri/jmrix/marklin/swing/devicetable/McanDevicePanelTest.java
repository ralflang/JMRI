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

        // 5 known CdB classes enabled by default
        assertEquals(initialSent + 5, tc.getSentMessages().size());
    }
}
