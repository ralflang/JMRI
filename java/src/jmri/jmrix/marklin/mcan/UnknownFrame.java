package jmri.jmrix.marklin.mcan;

/**
 * A valid 13-byte frame that matches none of the CS2, CdB or CS1/MS1 patterns,
 * for example undocumented ECoSLink E-CAN or not yet modelled traffic.
 * <p>
 * Only protocol-neutral access is offered. No header fields are interpreted.
 */
public final class UnknownFrame extends McanFrame {

    public UnknownFrame(RawFrame raw) {
        super(FrameProtocol.UNKNOWN, raw);
    }

    /** @return data length code (0-8) */
    public int dlc() {
        return raw().dlc();
    }
}
