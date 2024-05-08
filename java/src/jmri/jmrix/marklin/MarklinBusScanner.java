package jmri.jmrix.marklin;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jmri.LocoAddress;
import jmri.SpeedStepMode;
import jmri.jmrix.AbstractThrottle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Marklin Bus Message Scanner
 * <p>
 * Detect hardware throttles and CanDigitalBahn components
 * For now, save them into MEMORY variables rather than implement complete models.
 * TODO: Should we listen for Mfx Bind events?
 * TODO: Should we listen for MobileStation2 sync messages to autodetect locos into the JMRI roster?
 * </p>
 *
 * @author Ralf Lang Copyright (C) 2024
 */
public class MarklinBusScanner implements MarklinListener {

    /**
     * Constructor.
     * @param memo system connection.
     * @param address loco address.
     */
    public MarklinBusScanner(MarklinSystemConnectionMemo memo) {
        tc = memo.getTrafficController();

        synchronized(this) {
            this.speedSetting = 0;
        }
        tc.addMarklinListener(this);
    }

    @Override
    public void message(MarklinMessage m) {
        // messages are ignored
    }

    @Override
    public void reply(MarklinReply m) {
    }

    // initialize logging
    private final static Logger log = LoggerFactory.getLogger(MarklinThrottle.class);

}
