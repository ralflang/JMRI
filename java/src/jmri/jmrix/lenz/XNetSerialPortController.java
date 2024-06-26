package jmri.jmrix.lenz;

/**
 * Abstract base for classes representing an XNet communications port
 *
 * @author Bob Jacobsen Copyright (C) 2001, 2008
 * @author Paul Bender Copyright (C) 2004,2010
 */
public abstract class XNetSerialPortController extends jmri.jmrix.AbstractSerialPortController implements XNetPortController {

    private boolean outputBufferEmpty = true;

    private boolean timeSlot = true;

    public XNetSerialPortController() {
        super(new XNetSystemConnectionMemo());
    }

    public XNetSerialPortController(XNetSystemConnectionMemo memo) {
        super(memo);
    }

    /**
     * Can the port accept additional characters? The state of CTS determines
     * this, as there seems to be no way to check the number of queued bytes and
     * buffer length. This might go false for short intervals, but it might also
     * stick off if something goes wrong.
     */
    @Override
    public boolean okToSend() {
        if (getFlowControl(currentSerialPort) == FlowControl.RTSCTS) {
            if (checkBuffer) {
                log.debug("CTS: {} Buffer Empty {}",currentSerialPort.getCTS(),outputBufferEmpty);
                return (currentSerialPort.getCTS() && outputBufferEmpty);
            } else {
                log.debug("CTS: {}",currentSerialPort.getCTS());
                return (currentSerialPort.getCTS());
            }
        } else {
            if (checkBuffer) {
                log.debug("Buffer Empty: {}", outputBufferEmpty);
                return (outputBufferEmpty && hasTimeSlot() );
            } else {
                log.debug("No Flow Control or Buffer Check");
                return (hasTimeSlot());
            }
        }
    }

    /**
     * Indicate whether the Command Station is currently providing a timeslot to this
     * port controller.
     *
     * @return true if the command station is currently providing a timeslot.
     */
    @Override
    public boolean hasTimeSlot(){
        return timeSlot;
    }
    
    /**
     * Set a variable indicating whether or not the command station is
     * providing a timeslot.
     * <p>
     * This method should be called with the paramter set to false if
     * a "Command Station No Longer Providing a timeslot for communications"
     * (01 05 04) is received.
     * <p>
     * This method should be called with the parameter set to true if
     * a "Command Station is providing a timeslot for communications again."
     * (01 07 06) is received.
     *
     * @param timeslot true if a timeslot is being sent, false otherwise.
     */
    @Override
    public void setTimeSlot(boolean timeslot){
       timeSlot = timeslot;
    }    


    /**
     * We need a way to say if the output buffer is empty or full.
     * <p>
     * This should only be set to false by external processes.
     */
    @Override
    public synchronized void setOutputBufferEmpty(boolean s) {
        outputBufferEmpty = s;
    }


    /* Option 2 is not currenddtly used with RxTx 2.0.  In the past, it
     was used for the "check buffer status when sending" If this is still set
     in a configuration file, we need to handle it, but we are not writing it
     to new configuration files. */
    protected String[] validOption2 = new String[]{"yes", "no"};
    private boolean checkBuffer = false;

    /**
     * Allow derived classes to set the private checkBuffer value.
     * @param b new checkBuffer value
     */
    protected void setCheckBuffer(boolean b) {
        checkBuffer = b;
    }

    @Override
    public XNetSystemConnectionMemo getSystemConnectionMemo() {
        return (XNetSystemConnectionMemo) super.getSystemConnectionMemo();
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(XNetSerialPortController.class);

}
