package jmri.jmrix.can.adapters.gridconnect;

import jmri.jmrix.can.CanSystemConnectionMemo;

/**
 * Abstract base for classes representing a GridConnect communications port.
 *
 * Implementations will provide InputStream and OutputStream
 * objects to CabrsTrafficController classes, who in turn will deal in messages.
 * 
 * @author Bob Jacobsen Copyright (C) 2001
 * @author Andrew Crosland 2008
 */
public abstract class GcPortController extends jmri.jmrix.AbstractSerialPortController {

    /**
     * Create a new GC PortController.
     * @param connectionMemo CAN System Connection.
     */
    protected GcPortController(CanSystemConnectionMemo connectionMemo) {
        super(connectionMemo);
    }

    /**
     * Get the CAN System Connection.
     * {@inheritDoc}
     */
    @Override
    public CanSystemConnectionMemo getSystemConnectionMemo() {
        return (CanSystemConnectionMemo) super.getSystemConnectionMemo();
    }

}



