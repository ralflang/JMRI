package jmri.jmrix.marklin;

/**
 * MCanV2Type contains helpers to read or create the internal structure of a MS2/CS2/TrackBox native message
 * Anything that can be expected of a more generic CAN message should be relegated to MCanMessage or a base class derived from it
 *
 */
public abstract class MCanV2Type implements MCanMessage{

    int[] message;

    MCanV2Type (int[] message)
    {
        this.message = message;
    }


    public int[] toIntField() {
        return this.message.clone();
    }

    public byte[] toByteField()
    {
        var byteField = new byte[this.message.length];
        for (int i=0; i < this.message.length; i++) {
            byteField[i] = (byte) this.message[i];
        }
        return byteField;
    }
}
