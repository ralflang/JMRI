package jmri.jmrix.marklin;

import java.util.EnumSet;
import jmri.LocoAddress;
import jmri.SpeedStepMode;
import jmri.jmrix.AbstractThrottleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MarklinDCC implementation of a ThrottleManager.
 * <p>
 * Based on early NCE code and on work by Bob Jacobsen.
 *
 * @author Kevin Dickerson Copyright (C) 2012
 */
public class MarklinThrottleManager extends AbstractThrottleManager implements MarklinListener {

    /**
     * Constructor.
     * @param memo system connection.
     */
    public MarklinThrottleManager(MarklinSystemConnectionMemo memo) {
        super(memo);
    }

    @Override
    public void reply(MarklinReply m) {
        //We are not sending commands from here yet!
    }

    @Override
    public void message(MarklinMessage m) {
        // messages are ignored
    }

    @Override
    public void requestThrottleSetup(LocoAddress address, boolean control) {
        /* Here we do not set notifythrottle, we simply create a new Marklin throttle.
         The Marklin throttle in turn will notify the throttle manager of a successful or
         unsuccessful throttle connection. */
        log.debug("new MarklinThrottle for {}", address);
        notifyThrottleKnown(new MarklinThrottle((MarklinSystemConnectionMemo) adapterMemo, address), address);
    }

    @Override
    public boolean hasDispatchFunction() {
        return false;
    }

    /**
     * Address 100 and above is a long address
     *
     */
    @Override
    public boolean canBeLongAddress(int address) {
        return isLongAddress(address);
    }

    /**
     * Address 99 and below is a short address
     *
     */
    @Override
    public boolean canBeShortAddress(int address) {
        return !isLongAddress(address);
    }

    /**
     * Are there any ambiguous addresses (short vs long) on this system?
     *
     * This implementation is likely wrong as soon as we have MM2/Selectrix/DCC ambiguity.
     */
    @Override
    public boolean addressTypeUnique() {
        return false;
    }

    /**
     * Returns false
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected boolean singleUse() {
        return false;
    }

    @Override
    public String[] getAddressTypes() {
        return new String[]{
            LocoAddress.Protocol.DCC.getPeopleName(),
            LocoAddress.Protocol.MFX.getPeopleName(),
            LocoAddress.Protocol.M4.getPeopleName(), // Remove this once we agree that M4 and MFX are the same
            LocoAddress.Protocol.MOTOROLA.getPeopleName(), // Don't we ever consider MM1/Delta/MM2/EDITS? Let's just pretend they are all crippled versions of MM2
            LocoAddress.Protocol.SELECTRIX.getPeopleName() // Do we even have a concept of discerning SX1 and SX2 protocols?
        };
    }

    @Override
    public LocoAddress.Protocol[] getAddressProtocolTypes() {
        return new LocoAddress.Protocol[]{
            LocoAddress.Protocol.DCC,
            LocoAddress.Protocol.MFX,
            LocoAddress.Protocol.M4, // Remove this once we agree that M4 and MFX are the same
            LocoAddress.Protocol.MOTOROLA, // Don't we ever consider MM1/Delta/MM2/EDITS? Let's just pretend they are all crippled versions of MM2
            LocoAddress.Protocol.SELECTRIX // Do we even have a concept of discerning SX1 and SX2 protocols?
        };
    }

    /*
     * Local method for deciding short/long address
     */
    static boolean isLongAddress(int num) {
        // TODO: And protocol is DCC
        return (num >= 100);
    }

    @Override
    public EnumSet<SpeedStepMode> supportedSpeedModes() {
        /**
         * This implementation is misleading. Both high tier CS2/CS3 and low tier TrackBox stations supports
         * different speed step modes for different protocols and decoders. Should we just add all versions per protocol?
         */

        return EnumSet.of(SpeedStepMode.NMRA_DCC_128, SpeedStepMode.NMRA_DCC_28);
    }

    @Override
    public boolean disposeThrottle(jmri.DccThrottle t, jmri.ThrottleListener l) {
        if (super.disposeThrottle(t, l)) {
            if (t instanceof MarklinThrottle) {
                MarklinThrottle lnt = (MarklinThrottle) t;
                lnt.throttleDispose();
                return true;
            }
        }
        return false;
    }

    private final static Logger log = LoggerFactory.getLogger(MarklinThrottleManager.class);

}
