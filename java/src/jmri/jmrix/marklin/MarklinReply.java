package jmri.jmrix.marklin;

/**
 * Represents a received message on the MCAN bus.
 * This can either be a response to a message JMRI sent or it could be an event message sent by another party on the bus.
 *
 * @author Bob Jacobsen Copyright (C) 2001, 2008
 * @author Kevin Dickerson Copyright (C) 2007
 *
 */
public class MarklinReply extends jmri.jmrix.AbstractMRReply {

    // create a new one
    public MarklinReply() {
        super();
    }

    public MarklinReply(String s) {
        super(s);
    }

    public MarklinReply(MarklinReply l) {
        super(l);
    }

    // create a new one from an array
    public MarklinReply(int[] d) {
        //this(header);
        this();
        _nDataChars = d.length;
        System.arraycopy(d, 0, _dataChars, 0, d.length);
    }

    //Maximum size of a reply packet is 13 bytes.
    @Override
    public int maxSize() {
        return 13;
    }

    // no need to do anything
    @Override
    protected int skipPrefix(int index) {
        return index;
    }

    @Override
    public int value() {
        if (isBinary()) {
            return getElement(0);
        } else {
            return super.value();
        }
    }

    @Override  // avoid stupid sign extension
    public int getElement(int n) {
        return super.getElement(n) & 0xff;
    }

    //An event message is Unsolicited
    @Override
    public boolean isUnsolicited() {
        return !isResponse();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString(){
        // Represent message as groups of four hex digits separated by spaces
        // This is the format used by other handlers of MCAN, notably the official docs and Teddy's railcontrol
        StringBuilder buf = new StringBuilder();
        buf.append(String.format("0x%02X", _dataChars[0]));
        for (int i = 1; i < _nDataChars; i++) {
            if (i % 2 == 0) {
                buf.append(" ");
            }
            buf.append(String.format("%02X", _dataChars[i]));
        }
        return buf.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toMonitorString(){
        // eventually, the MarklinMon class should probably be integrated here.
        return jmri.jmrix.marklin.swing.monitor.MarklinMon.displayReply(this);
    }

    public boolean isResponse() {
        return (getElement(1) & 0x01) == 0x01;
    }

    public int getCanDataLength() {
        return getElement(4);
    }

    public int[] getCanData() {
        int[] arr = new int[maxSize() - 5];
        for (int i = 5; i < maxSize(); i++) {
            arr[i - 5] = getElement(i);
        }
        return arr;
    }

    public int[] getCanAddress() {
        int[] arr = new int[4];
        for (int i = 5; i < 9; i++) {
            arr[i - 5] = getElement(i);
        }
        return arr;
    }

    public long getAddress() {
        long addr = (getElement(MarklinConstants.CANADDRESSBYTE1));
        addr = (addr << 8) + (getElement(MarklinConstants.CANADDRESSBYTE2));
        addr = (addr << 8) + (getElement(MarklinConstants.CANADDRESSBYTE3));
        addr = (addr << 8) + (getElement(MarklinConstants.CANADDRESSBYTE4));
        return addr;
    }

    public int getPriority() {
        return (getElement(0) >> 1);
    }

    public int getCommand() {
        int result = getElement(0) << 7;
        result = result + getElement(1) >> 1;
        return result;
    }

    public int[] getHash() {
        int[] arr = new int[2];
        arr[0] = getElement(2);
        arr[1] = getElement(3);
        return arr;
    }
}
