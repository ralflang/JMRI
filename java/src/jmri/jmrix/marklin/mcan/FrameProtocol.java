package jmri.jmrix.marklin.mcan;

/**
 * Top-level protocol family of a CAN frame as determined by {@link jmri.jmrix.marklin.mcan.classify.ProtocolClassifier}.
 */
public enum FrameProtocol {

    /** Native Märklin CS2/MS2 protocol frame. CS2 header fingerprint present, DLC within expected range. */
    CS2_NATIVE,

    /** CdB (Can-Digital-Bahn) device configuration frame. Hash pattern 0x_A80 present. */
    CDB_CONFIG,

    /** CS1/MS1 M-CAN identifier layout, also used between ECoS and MS1. */
    CS1_NATIVE,

    /**
     * Frame not recognised by the protocol classifiers.
     * Includes undocumented ECoSLink E-CAN, unexplored modules and future protocol extensions.
     * Raw bytes are preserved. No interpretation is attempted.
     */
    UNKNOWN
}
