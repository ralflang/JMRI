package jmri.util.startup.configurexml;

import jmri.util.startup.PerformFileModel;

import jmri.util.startup.StartupActionsManager;
import jmri.InstanceManager;
import jmri.util.FileUtil;

import org.jdom2.Attribute;
import org.jdom2.Element;

/**
 * Handle XML persistence of PerformFileModel objects
 *
 * @author Bob Jacobsen Copyright: Copyright (c) 2003
 * @author Ken Cameron Copyright: 2014(c)
 * @see jmri.util.startup.PerformFileModelFactory
 */
public class PerformFileModelXml extends jmri.configurexml.AbstractXmlAdapter {

    public PerformFileModelXml() {
    }

    /**
     * Default implementation for storing the model contents
     *
     * @param o Object to store, of type PerformActonModel
     * @return Element containing the complete info
     */
    @Override
    public Element store(Object o) {
        Element e = new Element("perform");
        PerformFileModel g = (PerformFileModel) o;

        e.setAttribute("name", FileUtil.getPortableFilename(g.getFileName()));
        e.setAttribute("type", "XmlFile");
        e.setAttribute("enabled", g.isEnabled() ? "yes" : "no");
        e.setAttribute("class", this.getClass().getName());
        return e;
    }

    /**
     * Object should be loaded after basic GUI constructed
     *
     * @return true to defer loading
     * @see jmri.configurexml.AbstractXmlAdapter#loadDeferred()
     * @see jmri.configurexml.XmlAdapter#loadDeferred()
     */
    @Override
    public boolean loadDeferred() {
        return true;
    }

    @Override
    public boolean load(Element shared, Element perNode) {
        boolean result = true;
        String fileName = FileUtil.getAbsoluteFilename(shared.getAttribute("name").getValue());
        PerformFileModel m = new PerformFileModel();

        Attribute enabled = shared.getAttribute("enabled");
        if (enabled != null) {
            m.setEnabled("yes".equals(enabled.getValue()));
        } else {
            m.setEnabled(true);
        }

        m.setFileName(fileName);
        InstanceManager.getDefault(StartupActionsManager.class).addAction(m);
        return result;
    }

    // initialize logging
//    private final static Logger log = LoggerFactory.getLogger(PerformFileModelXml.class);

}
