package jmri.jmrix.marklin;

import jmri.util.JUnitUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CdBClassFilter}.
 * <p>
 * Verifies
 * - that the hash based discriminator correctly separates CdB device-config frames from standard MCAN traffic
 * - that the class recovery arithmetic reproduces every documented CdB class
 * - that standard MCAN operational frames (including CdB turnout/occupancy events) pass through.
 *
 */
public class CdBClassFilterTest {

    // -- helper: build a 13 byte MarklinReply from a 32-bit CAN ID + DLC --

    private static MarklinReply frameFromCanId(long canId, int dlc, int... data) {
        int[] raw = new int[13];
        raw[0] = (int)((canId >> 24) & 0xFF);
        raw[1] = (int)((canId >> 16) & 0xFF);
        raw[2] = (int)((canId >> 8)  & 0xFF);
        raw[3] = (int)( canId        & 0xFF);
        raw[4] = dlc;
        for (int i = 0; i < data.length && i < 8; i++) {
            raw[5 + i] = data[i] & 0xFF;
        }
        return new MarklinReply(raw);
    }

    // -- CdB class ID formula ----------------

    private static long cdBPingId(int canClass) {
        return ((long)(canClass >> 4) << 21)
             | ((long)(canClass & 0x0F) << 12)
             | 0x0A80L;
    }

    // =====================================================================
    //  1.  Hash based discrimination: CdB config frames are recognised
    // =====================================================================

    @ParameterizedTest(name = "class 0x{0} ({1}) ping is CdB config")
    @CsvSource({
        "0B, StromSniffer",
        "0D, GleisReporter",
        "21, ReporterChef 002",
        "22, Magnet M 012",
        "23, WeichenChef AC 002",
        "26, WeichenChef Basic 002",
        "27, WeichenChef Kombi"
    })
    void pingIdentifiedAsCdB(String classHex, String label) {
        int cls = Integer.parseInt(classHex, 16);
        long pingId = cdBPingId(cls);
        MarklinReply r = frameFromCanId(pingId, 0);
        assertTrue(CdBClassFilter.isCdBConfigFrame(r),
            label + " ping should be identified as CdB config");
    }

    @ParameterizedTest(name = "class 0x{0} all five frame types are CdB config")
    @CsvSource({
        "0B", "0D", "21", "22", "23", "26", "27"
    })
    void allFrameTypesIdentifiedAsCdB(String classHex) {
        int cls = Integer.parseInt(classHex, 16);
        long base = cdBPingId(cls);
        long[] offsets = {0x00000, 0x10000, 0x40000, 0x50000, 0x60000};
        for (long off : offsets) {
            MarklinReply r = frameFromCanId(base + off, 2, 0x00, 0x01);
            assertTrue(CdBClassFilter.isCdBConfigFrame(r),
                String.format("class 0x%02X offset 0x%05X", cls, off));
        }
    }

    @ParameterizedTest(name = "class 0x{0} module-addressed frames are CdB config")
    @CsvSource({
        "22", "27"
    })
    void moduleAddressedFramesAreStillCdB(String classHex) {
        int cls = Integer.parseInt(classHex, 16);
        long readId = cdBPingId(cls) + 0x40000;
        // Replace byte 3 with module 5 → 0x85
        long withModule = (readId & 0xFFFFFF00L) | 0x85;
        MarklinReply r = frameFromCanId(withModule, 2, 0x01, 0x04);
        assertTrue(CdBClassFilter.isCdBConfigFrame(r),
            "module-addressed read should still match CdB hash pattern");
    }

    // =====================================================================
    //  2.  Standard MCAN frames are NOT flagged as CdB
    // =====================================================================

    @Test
    void standardSystemGoIsNotCdB() {
        // 00 00 47 11  — standard system command with hash 0x4711
        MarklinReply r = frameFromCanId(0x00004711L, 5, 0,0,0,0, 0x01);
        assertFalse(CdBClassFilter.isCdBConfigFrame(r));
    }

    @Test
    void standardAccessoryCommandIsNotCdB() {
        // 00 16 47 11  — standard accessory (cmd 0x0B)
        MarklinReply r = frameFromCanId(0x00164711L, 6, 0,0,0x30,0x00, 0x01,0x01);
        assertFalse(CdBClassFilter.isCdBConfigFrame(r));
    }

    @Test
    void standardFeedbackEventIsNotCdB() {
        // 00 22 47 11  — standard S88 event (cmd 0x11)
        MarklinReply r = frameFromCanId(0x00224711L, 8, 0,0,0,1, 0x00,0x01,0,0);
        assertFalse(CdBClassFilter.isCdBConfigFrame(r));
    }

    @Test
    void cdBManualTurnoutCommandIsNotConfig() {
        // 00 16 03 00  — CdB manual turnout, but this IS a standard MCAN
        //                accessory command.  Hash = 0x0300, not 0x_A80.
        MarklinReply r = frameFromCanId(0x00160300L, 6, 0,0,0x30,0x00, 0x01,0x01);
        assertFalse(CdBClassFilter.isCdBConfigFrame(r),
            "CdB manual turnout is standard MCAN, must not be intercepted");
    }

    @Test
    void cdBOccupancyEventIsNotConfig() {
        // 00 23 xx xx  — occupancy event, standard MCAN feedback frame
        MarklinReply r = frameFromCanId(0x00230000L, 6, 0,0,0,5, 0x00,0,0,0);
        assertFalse(CdBClassFilter.isCdBConfigFrame(r),
            "CdB occupancy event is standard MCAN, must not be intercepted");
    }

    @Test
    void cdBTurnoutStatusIsNotConfig() {
        // 00 17 xx xx  — turnout position status, standard MCAN
        MarklinReply r = frameFromCanId(0x00170000L, 6, 0,0,0x30,0x00, 0xFD,0,0,0);
        assertFalse(CdBClassFilter.isCdBConfigFrame(r),
            "CdB turnout status is standard MCAN, must not be intercepted");
    }

    // =====================================================================
    //  3.  classify() recovers the correct CAN class and frame type
    // =====================================================================

    @ParameterizedTest(name = "classify class 0x{0} ping")
    @CsvSource({
        "0B", "0D", "21", "22", "23", "26", "27"
    })
    void classifyRecoversPingClass(String classHex) {
        int cls = Integer.parseInt(classHex, 16);
        long pingId = cdBPingId(cls);
        MarklinReply r = frameFromCanId(pingId, 0);

        CdBClassFilter.Classification c = CdBClassFilter.classify(r);

        assertTrue(c.isCdBConfig());
        assertEquals(cls, c.getCanClass(),
            String.format("expected class 0x%02X", cls));
        assertEquals(CdBClassFilter.CdBFrameType.PING, c.getFrameType());
        assertEquals(0, c.getModule(), "broadcast ping → module 0");
    }

    @ParameterizedTest(name = "classify class 0x{0} read_reply to module 3")
    @CsvSource({
        "0B", "0D", "22", "27"
    })
    void classifyRecoversReadReply(String classHex) {
        int cls = Integer.parseInt(classHex, 16);
        long readReplyId = cdBPingId(cls) + 0x50000;
        // Module 3 → byte 3 = 0x83
        long withModule = (readReplyId & 0xFFFFFF00L) | 0x83;
        MarklinReply r = frameFromCanId(withModule, 4, 0x01, 0x04, 0x00, 0x0A);

        CdBClassFilter.Classification c = CdBClassFilter.classify(r);

        assertTrue(c.isCdBConfig());
        assertEquals(cls, c.getCanClass());
        assertEquals(CdBClassFilter.CdBFrameType.READ_REPLY, c.getFrameType());
        assertEquals(3, c.getModule());
    }

    @Test
    void classifyWriteFrame() {
        int cls = 0x23; // WeichenChef AC 002
        long writeId = cdBPingId(cls) + 0x60000;
        long withModule = (writeId & 0xFFFFFF00L) | 0x85;
        MarklinReply r = frameFromCanId(withModule, 4, 0x00, 0x03, 0x00, 0x64);

        CdBClassFilter.Classification c = CdBClassFilter.classify(r);

        assertTrue(c.isCdBConfig());
        assertEquals(cls, c.getCanClass());
        assertEquals(CdBClassFilter.CdBFrameType.WRITE, c.getFrameType());
        assertEquals(5, c.getModule());
    }

    // =====================================================================
    //  4.  offer() dispatches to CdBConfigListener and returns true
    // =====================================================================

    @Test
    void offerDispatchesCdBAndBlocksForwarding() {
        CdBClassFilter filter = new CdBClassFilter();
        final boolean[] called = {false};
        final int[] receivedClass = {0};
        filter.addCdBConfigListener((classification, rawReply) -> {
            called[0] = true;
            receivedClass[0] = classification.getCanClass();
        });

        long readReplyId = cdBPingId(0x22) + 0x50000;
        MarklinReply r = frameFromCanId(readReplyId, 4, 0x00, 0x01, 0x38, 0x00);

        boolean consumed = filter.offer(r);

        assertTrue(consumed, "offer() should consume CdB config frames");
        assertTrue(called[0], "listener should have been called");
        assertEquals(0x22, receivedClass[0]);
    }

    @Test
    void offerPassesThroughStandardMCAN() {
        CdBClassFilter filter = new CdBClassFilter();
        final boolean[] called = {false};
        filter.addCdBConfigListener((c, raw) -> called[0] = true);

        MarklinReply r = frameFromCanId(0x00164711L, 6, 0,0,0x30,0x00, 0x01,0x01);

        boolean consumed = filter.offer(r);

        assertFalse(consumed, "standard MCAN must not be consumed");
        assertFalse(called[0], "listener must not be called for standard MCAN");
    }

    // =====================================================================
    //  Lifecycle
    // =====================================================================

    @BeforeEach
    void setUp() {
        JUnitUtil.setUp();
    }

    @AfterEach
    void tearDown() {
        JUnitUtil.tearDown();
    }
}
