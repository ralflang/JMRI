package jmri.jmrix.marklin.swing.configure;

import java.lang.reflect.Field;
import java.text.ParseException;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import jmri.jmrix.marklin.McanDevice;
import jmri.jmrix.marklin.McanDeviceIdentity;
import jmri.jmrix.marklin.MarklinSystemConnectionMemo;
import jmri.jmrix.marklin.MarklinTrafficControlScaffold;
import jmri.util.JUnitUtil;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link WeichenChefKombiConfigPanel}.
 *
 * @author Ralf Lang (ralf.lang@ralf-lang.de)
 */
class WeichenChefKombiConfigPanelTest {

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
    void testPanelCreatesWithoutError() {
        McanDeviceIdentity.CdB id = McanDeviceIdentity.forCdB(0x27, 3);
        McanDevice dev = new McanDevice("M", id);
        WeichenChefKombiConfigPanel panel = new WeichenChefKombiConfigPanel(dev, memo);
        assertNotNull(panel);
    }

    @Test
    @jmri.util.junit.annotations.DisabledIfHeadless
    void testNewModuleIdSpinnerClampedToProtocolRange() throws Exception {
        McanDeviceIdentity.CdB id = McanDeviceIdentity.forCdB(0x27, 3);
        McanDevice dev = new McanDevice("M", id);
        WeichenChefKombiConfigPanel panel = new WeichenChefKombiConfigPanel(dev, memo);

        Field f = WeichenChefKombiConfigPanel.class.getDeclaredField("newIdSpinner");
        f.setAccessible(true);
        JSpinner spinner = (JSpinner) f.get(panel);
        SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();

        // CdB module IDs are 7-bit: valid range is 1..127.
        assertEquals(Integer.valueOf(1), model.getMinimum(), "new module ID lower bound must be 1");
        assertEquals(Integer.valueOf(127), model.getMaximum(), "new module ID upper bound must be 127");

        // Stepping cannot leave the 1..127 range.
        spinner.setValue(127);
        assertNull(model.getNextValue(), "must not step above 127");
        spinner.setValue(1);
        assertNull(model.getPreviousValue(), "must not step below 1");

        // The spinner editor rejects out-of-range typed input: committing an
        // out-of-range string throws and the field keeps a valid value.
        JSpinner.NumberEditor editor = (JSpinner.NumberEditor) spinner.getEditor();
        JFormattedTextField ftf = editor.getTextField();

        spinner.setValue(5);
        ftf.setText("200");
        assertThrows(ParseException.class, ftf::commitEdit, "typing 200 must be rejected");
        assertTrue(((Integer) spinner.getValue()) <= 127, "value must stay within 127 after rejected input");

        ftf.setText("0");
        assertThrows(ParseException.class, ftf::commitEdit, "typing 0 must be rejected");
        assertTrue(((Integer) spinner.getValue()) >= 1, "value must stay within 1 after rejected input");
    }
}
