package jmri.jmrix.marklin.swing.devicetable;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import jmri.jmrix.marklin.CdBMessage;
import jmri.jmrix.marklin.MarklinCanCodec;
import jmri.jmrix.marklin.MarklinConstants;
import jmri.jmrix.marklin.MarklinMessage;
import jmri.jmrix.marklin.MarklinSystemConnectionMemo;
import jmri.jmrix.marklin.McanDeviceManager;
import jmri.jmrix.marklin.swing.MarklinPanel;

/**
 * Panel displaying discovered MCAN devices (Märklin and CdB) with discovery controls.
 *
 * @author Ralf Lang (ralf.lang@ralf-lang.de)
 */
public class McanDevicePanel extends MarklinPanel {

    public static class CdBClassEntry {
        private final int canClass;
        private final String name;

        public CdBClassEntry(int canClass, String name) {
            this.canClass = canClass;
            this.name = name;
        }

        public int getCanClass() {
            return canClass;
        }

        public String getName() {
            return name;
        }
    }

    private static final CdBClassEntry[] KNOWN_CDB_CLASSES = new CdBClassEntry[]{
        new CdBClassEntry(0x00, "System / Gleisbox (0x00)"),
        new CdBClassEntry(0x01, "Lok / RailCom (0x01)"),
        new CdBClassEntry(0x10, "Display / Control (0x10)"),
        new CdBClassEntry(0x20, "Magnet / Turnout (0x20)"),
        new CdBClassEntry(0x30, "Sensor / Rückmelder (0x30)"),
    };

    public static CdBClassEntry[] getKnownCdbClasses() {
        return KNOWN_CDB_CLASSES.clone();
    }

    private JButton discoverMarklinButton;
    private JButton discoverCdbButton;
    private final Map<Integer, JCheckBox> cdbClassCheckBoxes = new HashMap<>();

    private McanDeviceTableDataModel dataModel;
    private JTable deviceTable;

    public McanDevicePanel() {
        super();
    }

    @Override
    public void initComponents() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT));

        discoverMarklinButton = new JButton("Discover Marklin");
        discoverMarklinButton.setToolTipText("Send MCAN PING query to discover Märklin devices (CS2, MS2, Gleisbox, etc.)");
        discoverMarklinButton.addActionListener(this::discoverMarklinActionPerformed);
        buttonRow.add(discoverMarklinButton);

        discoverCdbButton = new JButton("Discover CdB");
        discoverCdbButton.setToolTipText("Send CdB PING query for selected CdB classes");
        discoverCdbButton.addActionListener(this::discoverCdbActionPerformed);
        buttonRow.add(discoverCdbButton);

        topPanel.add(buttonRow);

        JPanel cdbCheckBoxRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (CdBClassEntry entry : KNOWN_CDB_CLASSES) {
            JCheckBox checkBox = new JCheckBox(entry.getName(), true);
            cdbClassCheckBoxes.put(entry.getCanClass(), checkBox);
            cdbCheckBoxRow.add(checkBox);
        }
        topPanel.add(cdbCheckBoxRow);

        add(topPanel, BorderLayout.NORTH);

        deviceTable = new JTable();
        add(new JScrollPane(deviceTable), BorderLayout.CENTER);
    }

    @Override
    public void initComponents(MarklinSystemConnectionMemo memo) {
        super.initComponents(memo);
        initComponents();

        McanDeviceManager mcanDeviceManager = null;
        if (memo != null) {
            mcanDeviceManager = memo.getMcanDeviceManager();
        }

        dataModel = new McanDeviceTableDataModel(mcanDeviceManager);
        deviceTable.setModel(dataModel);
    }

    private void discoverMarklinActionPerformed(ActionEvent e) {
        if (memo == null || memo.getTrafficController() == null) {
            return;
        }

        // Send PING request (cmd 0x18, DLC=0)
        int[] encodedPing = MarklinCanCodec.builder()
                .setCommand(MarklinConstants.CMDPING)
                .setResponse(false)
                .build();
        MarklinMessage msg = new MarklinMessage(encodedPing);
        memo.getTrafficController().sendMarklinMessage(msg, null);
    }

    private void discoverCdbActionPerformed(ActionEvent e) {
        if (memo == null || memo.getTrafficController() == null) {
            return;
        }

        for (CdBClassEntry entry : KNOWN_CDB_CLASSES) {
            JCheckBox cb = cdbClassCheckBoxes.get(entry.getCanClass());
            if (cb != null && cb.isSelected()) {
                int[] rawFrame = CdBMessage.createConfigFrame(entry.getCanClass(), 0x00, 0x00, new int[0], 0x0A80);
                MarklinMessage msg = new MarklinMessage(rawFrame);
                memo.getTrafficController().sendMarklinMessage(msg, null);
            }
        }
    }

    @Override
    public String getTitle() {
        if (memo != null) {
            return "MCAN Devices (" + memo.getUserName() + ")";
        }
        return "MCAN Devices";
    }

    @Override
    public String getHelpTarget() {
        return "package.jmri.jmrix.marklin.swing.devicetable.McanDevicePanel";
    }

    public JButton getDiscoverMarklinButton() {
        return discoverMarklinButton;
    }

    public JButton getDiscoverCdbButton() {
        return discoverCdbButton;
    }

    public Map<Integer, JCheckBox> getCdbClassCheckBoxes() {
        return cdbClassCheckBoxes;
    }

    public JTable getDeviceTable() {
        return deviceTable;
    }

    public McanDeviceTableDataModel getDataModel() {
        return dataModel;
    }

    public static class Default extends jmri.jmrix.marklin.swing.MarklinNamedPaneAction {

        public Default() {
            super("MCAN Devices",
                    new jmri.util.swing.sdi.JmriJFrameInterface(),
                    McanDevicePanel.class.getName(),
                    jmri.InstanceManager.getDefault(MarklinSystemConnectionMemo.class));
        }
    }
}
