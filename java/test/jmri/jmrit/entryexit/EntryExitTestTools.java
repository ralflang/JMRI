package jmri.jmrit.entryexit;

import java.util.HashMap;

import jmri.*;
import jmri.configurexml.JmriConfigureXmlException;
import jmri.jmrit.display.EditorManager;
import jmri.jmrit.display.layoutEditor.LayoutEditor;

class EntryExitTestTools {
    static HashMap<String, LayoutEditor> getPanels() throws JmriConfigureXmlException, JmriException, IllegalArgumentException {
        HashMap<String, LayoutEditor> panels = new HashMap<>();
        jmri.configurexml.ConfigXmlManager cm = new jmri.configurexml.ConfigXmlManager();
        java.io.File f = new java.io.File("java/test/jmri/jmrit/entryexit/load/EntryExitTest.xml");
        cm.load(f);

        for (LayoutEditor panel : InstanceManager.getDefault(EditorManager.class).getAll(LayoutEditor.class)) {
            switch (panel.getLayoutName()) {
                case "Alpha":
                    panels.put("Alpha", panel);
                    break;
                case "Beta":
                    panels.put("Beta", panel);
                    break;
                default:
                    break;
            }
        }

        InstanceManager.getDefault(SensorManager.class).provideSensor("Reset").setKnownState(Sensor.ACTIVE);
        InstanceManager.getDefault(jmri.jmrit.display.layoutEditor.LayoutBlockManager.class).initializeLayoutBlockPaths();
        return panels;
    }

    PointDetails getPoint(Sensor sensor, LayoutEditor panel, EntryExitPairs eep) {
        return (sensor == null) ? null : eep.providePoint(sensor, panel);
    }

    Source getSourceInstance(Sensor sensor, LayoutEditor panel, EntryExitPairs eep) {
        PointDetails pd = getPoint(sensor, panel, eep);
        return (pd == null) ? null : eep.getSourceForPoint(pd);
    }

    DestinationPoints getDestinationPoint(Sensor srcSensor, Sensor destSensor, LayoutEditor panel,  EntryExitPairs eep) {
        Source src = getSourceInstance(srcSensor, panel, eep);
        PointDetails pd = getPoint(destSensor, panel, eep);
        return (src == null || pd == null) ? null : src.getDestForPoint(pd);
    }

//     private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EntryExitTestTools.class);
}
