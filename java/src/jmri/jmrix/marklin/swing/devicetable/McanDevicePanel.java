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
import jmri.jmrix.marklin.MarklinSystemConnectionMemo;
import jmri.jmrix.marklin.McanDeviceManager;
import jmri.jmrix.marklin.swing.MarklinPanel;

/**
 * Panel displaying discovered MCAN devices (Märklin and CdB) with discovery controls.
 *
 * @author Ralf Lang (ralf.lang@ralf-lang.de)
 */
public class McanDevicePanel extends MarklinPanel {

    private JButton discoverMarklinButton;
    private JButton discoverCdbButton;
    private final Map<CdBMessage.CdBDeviceClass, JCheckBox> cdbClassCheckBoxes = new HashMap<>();

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
        for (CdBMessage.CdBDeviceClass deviceClass : CdBMessage.CdBDeviceClass.values()) {
            String label = String.format("%s (0x%02X)", deviceClass.getDisplayName(), deviceClass.getClassId());
            JCheckBox checkBox = new JCheckBox(label, true);
            cdbClassCheckBoxes.put(deviceClass, checkBox);
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
        if (memo == null || memo.getMcanDeviceManager() == null) {
            return;
        }
        memo.getMcanDeviceManager().discoverMarklinDevices();
    }

    private void discoverCdbActionPerformed(ActionEvent e) {
        if (memo == null || memo.getMcanDeviceManager() == null) {
            return;
        }

        for (CdBMessage.CdBDeviceClass deviceClass : CdBMessage.CdBDeviceClass.values()) {
            JCheckBox cb = cdbClassCheckBoxes.get(deviceClass);
            if (cb != null && cb.isSelected()) {
                memo.getMcanDeviceManager().discoverCdBDevice(deviceClass);
            }
        }
    }

    @Override
    public String getTitle() {
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

    public Map<CdBMessage.CdBDeviceClass, JCheckBox> getCdbClassCheckBoxes() {
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
