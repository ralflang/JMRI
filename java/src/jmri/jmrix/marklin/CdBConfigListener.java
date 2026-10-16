package jmri.jmrix.marklin;

/**
 * Callback interface for CAN-digital-Bahn device configuration frames.
 * <p>
 * Implementations receive positively identified CdB config traffic (ping, indexed parameter
 * read/write, read replies) see {@link CdBClassFilter}.
 * <p>
 * Standard MCAN frames including CdB operational traffic are not delivered through this interface.
 *
 * @see CdBClassFilter
 */
public interface CdBConfigListener {

    /**
     * Called when a CdB device-configuration frame has been received.
     *
     * @param classification the pre-computed class, frame type, and module
     *                       address extracted by the filter
     * @param rawReply       the original 13-byte reply, available for
     *                       low-level access if needed
     */
    void cdBConfigReply(CdBClassFilter.Classification classification,
                        MarklinReply rawReply);
}
