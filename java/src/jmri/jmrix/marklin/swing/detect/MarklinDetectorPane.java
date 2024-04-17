package jmri.jmrix.marklin.swing.detect;

import jmri.jmrix.marklin.MarklinListener;
import jmri.jmrix.marklin.MarklinMessage;
import jmri.jmrix.marklin.MarklinReply;

/**
 * Detect JMRI items from the bus by listening passively or by sending detection commands
 * <p>
 * - TrackBox
 * - MS2 Throttles which may hold train rosters
 * - CdB sensors, turnouts, configurable gear
 */
@SuppressWarnings("unused")
public class MarklinDetectorPane extends jmri.jmrix.marklin.swing.MarklinPanel implements MarklinListener {

    public MarklinDetectorPane() {
        super();
    }

    //
    public String getTitle() {
        return Bundle.getMessage("DetectMCANDevices");
    }

    // Mandatory from listener interface
    public void message(MarklinMessage m) {
    }

    /**
     * {@inheritDoc}
     * Mandatory from listener interface
     */
    @Override
    public void reply(MarklinReply r) {

    }

    /**
     * Need to release listener when closing
     */
    public void dispose() {
        if ( memo != null ) {
            memo.getTrafficController().removeMarklinListener(this);
        }
        super.dispose();
    }
}
