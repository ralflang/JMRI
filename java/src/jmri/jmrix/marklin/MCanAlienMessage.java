package jmri.jmrix.marklin;

public class MCanAlienMessage implements MCanMessage {
    int[] message;

    MCanAlienMessage(int[] message)
    {
        this.message = message;
    }
}
