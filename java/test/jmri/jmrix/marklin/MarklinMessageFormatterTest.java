package jmri.jmrix.marklin;

import jmri.util.JUnitUtil;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MarklinMessageFormatter}.
 *
 * @author Ralf Lang (ralf.lang@ralf-lang.de)
 */
class MarklinMessageFormatterTest {

    @BeforeEach
    void setUp() {
        JUnitUtil.setUp();
    }

    @AfterEach
    void tearDown() {
        JUnitUtil.tearDown();
    }

    @Test
    void testFormatStandardPing() {
        // Standard MCAN PING (command 0x18, DLC=0)
        int[] pingFrame = MarklinCanCodec.builder()
                .setCommand(MarklinConstants.CMDPING)
                .setResponse(false)
                .build();

        String formatted = MarklinMessageFormatter.formatRaw(pingFrame, null);
        assertNotNull(formatted);
        assertTrue(formatted.contains("PING"), "Should identify PING command");
        assertTrue(formatted.contains("Request"), "Should show as Request");
    }

    @Test
    void testFormatCdBPing() {
        // CdB PING for WeichenChef Kombi (class 0x27)
        CdBMessage.CdBDeviceClass deviceClass = CdBMessage.CdBDeviceClass.WEICHEN_CHEF_KOMBI;
        int[] pingFrame = CdBMessage.createClassPing(deviceClass);

        String formatted = MarklinMessageFormatter.formatRaw(pingFrame, null);
        assertNotNull(formatted);
        assertTrue(formatted.contains("CdB"), "Should identify as CdB frame");
        assertTrue(formatted.contains("PING"), "Should identify PING type");
        assertTrue(formatted.contains("0x27"), "Should show class 0x27");
        assertTrue(formatted.contains("WeichenChef Kombi"), "Should show device name");
    }

    @Test
    void testFormatCdBPingReply() {
        // CdB PING reply for GleisReporter (class 0x0D), module 2
        int canClass = 0x0D;
        int module = 2;
        long pingId = ((canClass >> 4) << 21) + ((canClass & 0x0F) << 12) + 0x0A80;
        long replyId = pingId + 0x10000;
        long withModule = (replyId & 0xFFFFFF00L) | (0x80 + module);

        int[] replyFrame = new int[13];
        replyFrame[0] = (int)((withModule >> 24) & 0xFF);
        replyFrame[1] = (int)((withModule >> 16) & 0xFF);
        replyFrame[2] = (int)((withModule >> 8) & 0xFF);
        replyFrame[3] = (int)(withModule & 0xFF);
        replyFrame[4] = 2; // DLC

        String formatted = MarklinMessageFormatter.formatRaw(replyFrame, null);
        assertNotNull(formatted);
        assertTrue(formatted.contains("CdB"), "Should identify as CdB frame");
        assertTrue(formatted.contains("PING Reply"), "Should identify PING Reply type");
        assertTrue(formatted.contains("0x0D"), "Should show class 0x0D");
        assertTrue(formatted.contains("GleisReporter"), "Should show device name");
        assertTrue(formatted.contains("Module 2"), "Should show module number");
    }

    @Test
    void testFormatCdBReadRequest() {
        // CdB READ request for ReporterChef (class 0x21), module 1
        int canClass = 0x21;
        int module = 1;
        long pingId = ((canClass >> 4) << 21) + ((canClass & 0x0F) << 12) + 0x0A80;
        long readId = pingId + 0x40000;
        long withModule = (readId & 0xFFFFFF00L) | (0x80 + module);

        int[] readFrame = new int[13];
        readFrame[0] = (int)((withModule >> 24) & 0xFF);
        readFrame[1] = (int)((withModule >> 16) & 0xFF);
        readFrame[2] = (int)((withModule >> 8) & 0xFF);
        readFrame[3] = (int)(withModule & 0xFF);
        readFrame[4] = 2; // DLC
        readFrame[5] = 0x00; // index
        readFrame[6] = 0x01; // parameter

        String formatted = MarklinMessageFormatter.formatRaw(readFrame, null);
        assertNotNull(formatted);
        assertTrue(formatted.contains("CdB"), "Should identify as CdB frame");
        assertTrue(formatted.contains("Read Request"), "Should identify Read Request type");
        assertTrue(formatted.contains("0x21"), "Should show class 0x21");
        assertTrue(formatted.contains("ReporterChef"), "Should show device name");
        assertTrue(formatted.contains("Module 1"), "Should show module number");
    }

    @Test
    void testFormatStandardS88Event() {
        // Standard MCAN S88 Event (also command 0x11 but NOT CdB)
        int[] s88Frame = MarklinCanCodec.builder()
                .setCommand(MarklinConstants.S88EVENT)
                .setResponse(false)
                .setHash(0x47, 0x11) // Standard MCAN hash, not CdB
                .setData(new int[]{0x00, 0x01})
                .build();

        String formatted = MarklinMessageFormatter.formatRaw(s88Frame, null);
        assertNotNull(formatted);
        assertTrue(formatted.contains("S88"), "Should identify as S88 Event");
        assertFalse(formatted.contains("CdB"), "Should NOT identify as CdB frame");
    }
}
