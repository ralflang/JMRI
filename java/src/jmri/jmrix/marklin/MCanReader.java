package jmri.jmrix.marklin;
import jmri.jmrix.Message;

/**
 * Interprets an MCAN Message as a known Marklin first party, CdB or alien message
 */
class MCanReader
{

    /**
     * Read MarklinMessage or MarklinReply into a specific MCAN message class
     * @param jmriMessage
     * @return MCanMessage
     */
    public static MCanMessage identify(Message jmriMessage)
    {
        var numMessageElements = jmriMessage.getNumDataElements();
        // Is it worth it to convert to char and get rid of all the & "ffff" masking?
        var elements = new int[numMessageElements];

        // Message is the common root of MarklinMessage and MarklinReply but contains no wholesale exposure of elements
        // TODO: Retrofit to an interface and implement in both?
        for (int i = 0; i < numMessageElements;  i++)  {
            elements[i] = jmriMessage.getElement(i);
        }
        // For now, skip identifying and treat all messages as alien
        return new MCanAlienMessage(elements);
    }
}