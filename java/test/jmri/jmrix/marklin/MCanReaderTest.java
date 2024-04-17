package jmri.jmrix.marklin;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class MCanReaderTest {

    public void testMcanFromMarklinMessage()
    {
        var marklinMessage = MarklinMessage.getEnableMain();
        var mcanMessage = MCanReader.identify(marklinMessage);
        assertInstanceOf(MCanMessage.class, mcanMessage);
    }
}
