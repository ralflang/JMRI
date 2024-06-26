package jmri.jmrix.internal.configurexml;

import org.jdom2.Element;

/**
 * Provides load and store functionality for configuring
 * InternalTurnoutManagers.
 * <p>
 * Uses the store method from the abstract base class, but provides a load
 * method here.
 *
 * @author Bob Jacobsen Copyright: Copyright (c) 2006
 */
public class InternalTurnoutManagerXml extends jmri.managers.configurexml.AbstractTurnoutManagerConfigXML {

    public InternalTurnoutManagerXml() {
        super();
    }

    @Override
    public void setStoreElementClass(Element turnouts) {
        turnouts.setAttribute("class", this.getClass().getName());
    }

    @Override
    public boolean load(Element shared, Element perNode) {
        // load individual turnouts
        return loadTurnouts(shared, perNode);
    }

//    private final static Logger log = LoggerFactory.getLogger(InternalTurnoutManagerXml.class);

}
