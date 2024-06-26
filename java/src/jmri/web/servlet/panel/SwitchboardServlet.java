package jmri.web.servlet.panel;

import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.swing.JFrame;
import jmri.configurexml.ConfigXmlManager;
import jmri.jmrit.display.switchboardEditor.SwitchboardEditor;
import jmri.jmrit.display.switchboardEditor.BeanSwitch;
import jmri.server.json.JSON;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Return xml (for specified SwitchBoard) suitable for use by external clients.
 * <p>
 * See JMRI Web Server - Panel Servlet Help in help/en/html/web/PanelServlet.shtml for an example description of
 * the interaction between the Web Servlets, the Web Browser and the JMRI application.
 *
 * @author Egbert Broerse (C) 2017, 2020 -- based on ControlPanelServlet.java by Randall Wood
 */
@WebServlet(name = "SwitchboardServlet",
        urlPatterns = {"/panel/Switchboard"})
@ServiceProvider(service = HttpServlet.class)
public class SwitchboardServlet extends AbstractPanelServlet {

    @Override
    protected String getPanelType() {
        return "Switchboard";
    }

    @Override
    protected String getXmlPanel(String name) {
        log.debug("Getting {} for {}", getPanelType(), name);
        SwitchboardEditor editor = (SwitchboardEditor) getEditor(name);
        if (editor == null) {
            log.warn("Requested Switchboard [{}] does not exist.", name);
            return "ERROR Requested panel [" + name + "] does not exist.";
        }

        Element panel = new Element("panel");

        JFrame frame = editor.getTargetFrame();

        panel.setAttribute("name", name);
        panel.setAttribute("paneltype", getPanelType());
        panel.setAttribute("height", Integer.toString(frame.getContentPane().getHeight()));
        panel.setAttribute("width", Integer.toString(frame.getContentPane().getWidth()));
        panel.setAttribute("panelheight", Integer.toString(editor.getTargetPanel().getHeight()));
        panel.setAttribute("panelwidth", Integer.toString(editor.getTargetPanel().getWidth()));
        // add more properties
        panel.setAttribute("showtooltips", (editor.showToolTip()) ? "yes" : "no");
        panel.setAttribute("controlling", (editor.allControlling()) ? "yes" : "no");

        panel.setAttribute("hideunconnected", (editor.hideUnconnected()) ? "yes" : "no");
        panel.setAttribute("rangemin", Integer.toString(editor.getPanelMenuRangeMin()));
        panel.setAttribute("rangemax", Integer.toString(editor.getPanelMenuRangeMax()));
        panel.setAttribute("type", editor.getSwitchType());
        panel.setAttribute("connection", editor.getSwitchManu());
        panel.setAttribute("shape", editor.getSwitchShape());
        panel.setAttribute("rows", Integer.toString(editor.getRows()));
        panel.setAttribute("total", Integer.toString(editor.getTotal()));
        panel.setAttribute("showusername", editor.showUserName());

        panel.setAttribute("defaulttextcolor", editor.getDefaultTextColor());
        panel.setAttribute("activecolor", editor.getActiveSwitchColor());
        panel.setAttribute("inactivecolor", editor.getInactiveSwitchColor());
        log.debug("webserver Switchboard panel attribs ready");

        Element color = new Element("backgroundColor");
        if (editor.getBackgroundColor() == null) {
            color.setAttribute("red", Integer.toString(192));
            color.setAttribute("green", Integer.toString(192));
            color.setAttribute("blue", Integer.toString(192));
        } else {
            color.setAttribute("red", Integer.toString(editor.getBackgroundColor().getRed()));
            color.setAttribute("green", Integer.toString(editor.getBackgroundColor().getGreen()));
            color.setAttribute("blue", Integer.toString(editor.getBackgroundColor().getBlue()));
        }
        panel.addContent(color);


        // include switches
        List<BeanSwitch> _switches = editor.getSwitches(); // call method in SwitchboardEditor
        log.debug("SwbServlet contains {} switches", _switches.size());
        for (BeanSwitch sub : _switches) {
            if (sub != null) {
                try {
                    Element e = ConfigXmlManager.elementFromObject(sub);
                    if (e != null) {
                        log.debug("element name: {}", e.getName());
                        e.setAttribute("label", sub.getNameString()); // either the system name or the label
                        e.setAttribute("connected", "false");
                        if (sub.getNamedBean() == null) { // skip unconnected switch
                            log.debug("switch {} NOT connected", sub.getNameString()); // use label instead
                        } else {
                            try {
                                e.setAttribute(JSON.ID, sub.getNamedBean().getSystemName());
                                e.setAttribute("connected", "true"); // activate click action via class
                            } catch (NullPointerException ex) {
                                log.debug("{} {} does not have a SystemName", e.getName(), e.getAttribute(JSON.NAME));
                            }
                        }
                        // read shared attribs to use in beanswitch
                        e.setAttribute("textcolor", editor.getDefaultTextColor());
                        e.setAttribute("type", editor.getSwitchType());
                        e.setAttribute("connection", editor.getSwitchManu());
                        e.setAttribute("shape", editor.getSwitchShape());
                        // process and add
                        parsePortableURIs(e);
                        panel.addContent(e);
                    }
                } catch (Exception ex) {
                    log.error("Error reading xml panel element: {}", ex, ex);
                }
            }
        }

        Document doc = new Document(panel);
        XMLOutputter out = new XMLOutputter();
        out.setFormat(Format.getPrettyFormat()
                .setLineSeparator(System.getProperty("line.separator"))
                .setTextMode(Format.TextMode.TRIM));

        return out.outputString(doc);
    }

    @Override
    protected String getJsonPanel(String name) {
        // TODO Auto-generated method stub
        return "ERROR JSON support not implemented";
    }

    private final static Logger log = LoggerFactory.getLogger(SwitchboardServlet.class);

}
