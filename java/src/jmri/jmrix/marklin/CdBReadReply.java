package jmri.jmrix.marklin;

/**
 * Value object holding decoded fields from a CAN-digital-Bahn (CdB) parameter READ_REPLY frame.
 */
public class CdBReadReply {
    private final int index;
    private final int param;
    private final int valueHi;
    private final int valueLo;
    private final int dlc;
    private final int[] rawData;

    public CdBReadReply(int index, int param, int valueHi, int valueLo, int dlc, int[] rawData) {
        this.index = index;
        this.param = param;
        this.valueHi = valueHi;
        this.valueLo = valueLo;
        this.dlc = dlc;
        this.rawData = rawData != null ? rawData.clone() : new int[0];
    }

    public int getIndex() {
        return index;
    }

    public int getParam() {
        return param;
    }

    public int getValueHi() {
        return valueHi;
    }

    public int getValueLo() {
        return valueLo;
    }

    public int getValue16() {
        return ((valueHi & 0xFF) << 8) | (valueLo & 0xFF);
    }

    public int getDlc() {
        return dlc;
    }

    public int[] getRawData() {
        return rawData.clone();
    }
}
