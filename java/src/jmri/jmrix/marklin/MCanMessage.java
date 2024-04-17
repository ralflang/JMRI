package jmri.jmrix.marklin;

/**
 * An MCanMessage is anything sent or received through the MCAN bus, both via UDP or serial adapter.
 * It does not imply if it is MS2/CS2 native protocol, CdB, MS1/CS1/Ecos CAN or entirely alien
 * See MCanV2Type for an abstract base class of the MS2/CS2 native protocol
 * See MCanAlienMessage for a generic "other" message container.
 */
interface MCanMessage {
    /**
     * Expose the literal message content as a sequence of bytes
     * @return
     */
    public byte[] toByteField();

    /**
     * Expose the literal message content padded to integers
     * The higher width of the integer is wasted and content of two adjacent bytes are not merged into one integer
     * Useful for emitting MarklinMessage/MarklinReply types expected by the wider JMRI ecosystem
     *
     * @return
     */
    public int[] toIntField();
}