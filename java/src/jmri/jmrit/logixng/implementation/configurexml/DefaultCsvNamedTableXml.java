package jmri.jmrit.logixng.implementation.configurexml;

import jmri.InstanceManager;
import jmri.jmrit.logixng.NamedTable;
import jmri.jmrit.logixng.NamedTableManager;
import jmri.jmrit.logixng.Table;
import jmri.jmrit.logixng.implementation.DefaultCsvNamedTable;

import org.jdom2.Element;

/**
 * Handle XML configuration for DefaultCsvNamedTable objects.
 *
 * @author Bob Jacobsen Copyright: Copyright (c) 2004, 2008, 2010
 * @author Daniel Bergqvist Copyright (C) 2019
 */
public class DefaultCsvNamedTableXml extends jmri.managers.configurexml.AbstractNamedBeanManagerConfigXML {

    public DefaultCsvNamedTableXml() {
    }
    
    /**
     * Default implementation for storing the contents of a DefaultCsvNamedTable
     *
     * @param o Object to store, of type DefaultCsvNamedTable
     * @return Element containing the complete info
     */
    @Override
    public Element store(Object o) {
        DefaultCsvNamedTable p = (DefaultCsvNamedTable) o;

        Element element = new Element("CsvTable");
        element.setAttribute("class", this.getClass().getName());
        element.addContent(new Element("systemName").addContent(p.getSystemName()));
        
        storeCommon(p, element);
        
        element.addContent(new Element("fileName").addContent(p.getFileName()));
        Table.CsvType csvType = p.getCsvType();
        Element csvElement = new Element("csvType");
        if (csvType == null)
        {
            csvElement.addContent(Table.CsvType.TABBED.name());
        } else {
            csvElement.addContent(p.getCsvType().name());
        }
        element.addContent(csvElement);
        return element;
    }
    
    @Override
    public boolean load(Element shared, Element perNode) {
        String sys = getSystemName(shared);
        String uname = getUserName(shared);
        String fileName = shared.getChild("fileName").getTextTrim();
        Table.CsvType csvType = Table.CsvType.TABBED;
        if (shared.getChild("csvType") != null) {
            csvType = Table.CsvType.valueOf(shared.getChild("csvType").getText());
        }
        NamedTable h = InstanceManager.getDefault(NamedTableManager.class).newCSVTable(sys, uname, fileName, csvType);
        
        loadCommon(h, shared);
        
        return true;
    }
    
//    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultCsvNamedTableXml.class);
}
