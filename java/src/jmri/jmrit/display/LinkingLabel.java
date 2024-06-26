package jmri.jmrit.display;

import javax.annotation.Nonnull;
import javax.swing.JPopupMenu;

import jmri.JmriException;
import jmri.jmrit.catalog.NamedIcon;
import jmri.util.swing.JmriMouseEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LinkingLabel is a PositionableLabel that opens a link to another window or
 * URL when clicked
 *
 * @author Bob Jacobsen Copyright (c) 2013
 */
public class LinkingLabel extends PositionableLabel implements LinkingObject {

    public LinkingLabel(@Nonnull String s, @Nonnull Editor editor, @Nonnull String url) {
        super(s, editor);
        this.url = url;
        setPopupUtility(new PositionablePopupUtil(this, this));
    }

    public LinkingLabel(NamedIcon s, @Nonnull Editor editor, @Nonnull String url) {
        super(s, editor);
        this.url = url;
        setPopupUtility(new PositionablePopupUtil(this, this));
    }

    @Override
    public Positionable deepClone() {
        PositionableLabel pos;
        if (_icon) {
            NamedIcon icon = new NamedIcon((NamedIcon) getIcon());
            pos = new LinkingLabel(icon, _editor, url);
        } else {
            pos = new LinkingLabel(_unRotatedText, _editor, url);
        }
        return finishClone(pos);
    }

    protected Positionable finishClone(LinkingLabel pos) {
        return super.finishClone(pos);
    }

    String url;

    @Override
    public String getURL() {
        return url;
    }

    @Override
    public void setULRL(String u) {
        url = u;
    }

    @Override
    public boolean setLinkMenu(JPopupMenu popup) {
        popup.add(CoordinateEdit.getLinkEditAction(this, "EditLink"));
        return true;
    }

    // overide where used - e.g. momentary
//    public void doMousePressed(JmriMouseEvent event) {}
//    public void doMouseReleased(JmriMouseEvent event) {}
    @Override
    public void doMouseClicked(JmriMouseEvent event) {
        log.debug("click to {}", url);
        try {
            if (url.startsWith("frame:")) {
                // locate JmriJFrame and push to front
                String frame = url.substring(6);
                final jmri.util.JmriJFrame jframe = jmri.util.JmriJFrame.getFrame(frame);
                if (jframe != null) {  //ignore if jframe not found
                    java.awt.EventQueue.invokeLater(() -> {
                        //if frame was minimized, restore
                        if (jframe.getExtendedState() == java.awt.Frame.ICONIFIED) {
                            jframe.setExtendedState(java.awt.Frame.NORMAL);
                        }
                        //bring the frame to the foreground
                        jframe.toFront();
                        jframe.repaint();
                    });
                } else {
                    log.error("Frame '{}' not found, cannot link to it.", frame);
                }
            } else if (url.length() > 0) {
                jmri.util.HelpUtil.openWebPage(url);
            }
        } catch (JmriException t) {
            log.error("Error handling link", t);
        }
        super.doMouseClicked(event);
    }

    private final static Logger log = LoggerFactory.getLogger(LinkingLabel.class);

}
