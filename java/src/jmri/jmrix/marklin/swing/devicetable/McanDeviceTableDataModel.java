package jmri.jmrix.marklin.swing.devicetable;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import jmri.Manager;
import jmri.jmrix.marklin.McanDevice;
import jmri.jmrix.marklin.McanDeviceManager;
import jmri.util.ThreadingUtil;

/**
 * Table data model for displaying and monitoring MCAN bus devices in Swing UI.
 *
 * @author Ralf Lang (ralf.lang@ralf-lang.de)
 */
public class McanDeviceTableDataModel extends AbstractTableModel implements PropertyChangeListener, Manager.ManagerDataListener<McanDevice> {

    public static final int COLUMN_SYS_NAME = 0;
    public static final int COLUMN_USER_NAME = 1;
    public static final int COLUMN_TYPE = 2;
    public static final int COLUMN_STATE = 3;
    public static final int COLUMN_SOFTWARE = 4;
    public static final int COLUMN_FIRST_SEEN = 5;
    public static final int COLUMN_LAST_SEEN = 6;
    public static final int NUM_COLUMNS = 7;

    private final McanDeviceManager manager;
    private final List<McanDevice> devices = new ArrayList<>();

    public McanDeviceTableDataModel(McanDeviceManager manager) {
        this.manager = manager;
        if (manager != null) {
            manager.addDataListener(this);
            updateDeviceList();
        }
    }

    private void updateDeviceList() {
        ThreadingUtil.runOnGUI(() -> {
            for (McanDevice d : devices) {
                d.removePropertyChangeListener(this);
            }
            devices.clear();
            if (manager != null) {
                for (McanDevice d : manager.getNamedBeanSet()) {
                    devices.add(d);
                    d.addPropertyChangeListener(this);
                }
            }
            fireTableDataChanged();
        });
    }

    @Override
    public void contentsChanged(Manager.ManagerDataEvent<McanDevice> e) {
        updateDeviceList();
    }

    @Override
    public void intervalAdded(Manager.ManagerDataEvent<McanDevice> e) {
        updateDeviceList();
    }

    @Override
    public void intervalRemoved(Manager.ManagerDataEvent<McanDevice> e) {
        updateDeviceList();
    }

    @Override
    public int getRowCount() {
        return devices.size();
    }

    @Override
    public int getColumnCount() {
        return NUM_COLUMNS;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case COLUMN_SYS_NAME:
                return "System Name";
            case COLUMN_USER_NAME:
                return "User Name";
            case COLUMN_TYPE:
                return "Device Type";
            case COLUMN_STATE:
                return "State";
            case COLUMN_SOFTWARE:
                return "Software Version";
            case COLUMN_FIRST_SEEN:
                return "First Found";
            case COLUMN_LAST_SEEN:
                return "Last Seen";
            default:
                return "";
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= devices.size()) {
            return null;
        }
        McanDevice device = devices.get(rowIndex);
        switch (columnIndex) {
            case COLUMN_SYS_NAME:
                return device.getSystemName();
            case COLUMN_USER_NAME:
                return device.getUserName();
            case COLUMN_TYPE:
                return device.getIdentity().getDisplayName();
            case COLUMN_STATE:
                return device.getDeviceState();
            case COLUMN_SOFTWARE:
                return device.getSoftwareVersion() >= 0 ? String.format("v%d.%d", (device.getSoftwareVersion() >> 8) & 0xFF, device.getSoftwareVersion() & 0xFF) : "Unknown";
            case COLUMN_FIRST_SEEN:
                return device.getFirstSeen() != null ? device.getFirstSeen().toString() : "";
            case COLUMN_LAST_SEEN:
                return device.getLastSeen() != null ? device.getLastSeen().toString() : "";
            default:
                return null;
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        ThreadingUtil.runOnGUI(() -> {
            int row = devices.indexOf(evt.getSource());
            if (row >= 0) {
                fireTableRowsUpdated(row, row);
            }
        });
    }

    public McanDevice getDeviceAt(int row) {
        if (row >= 0 && row < devices.size()) {
            return devices.get(row);
        }
        return null;
    }
}
