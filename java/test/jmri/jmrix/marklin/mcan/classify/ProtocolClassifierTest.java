package jmri.jmrix.marklin.mcan.classify;
import jmri.jmrix.marklin.mcan.cs2.Cs2Frame;
import jmri.jmrix.marklin.mcan.cs1.Cs1Frame;
import jmri.jmrix.marklin.mcan.UnknownFrame;
import jmri.jmrix.marklin.mcan.cdb.CdBFrameParser;
import jmri.jmrix.marklin.mcan.cs2.Cs2Header;
import jmri.jmrix.marklin.mcan.FrameProtocol;
import jmri.jmrix.marklin.mcan.McanFrame;
import jmri.util.JUnitUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ProtocolClassifier}.
 * <p>
 * Classification contract:
 * <ol>
 *   <li>CS2 fingerprint present -> CS2_NATIVE</li>
 *   <li>CdB hash pattern present -> CDB_CONFIG</li>
 *   <li>MS1 M-CAN layout -> CS1_NATIVE</li>
 *   <li>No match -> UNKNOWN</li>
 *   <li>Null or wrong length -> IllegalArgumentException</li>
 * </ol>
 *
 * @author Ralf Lang (ralf.lang@ralf-lang.de)
 */
public class ProtocolClassifierTest {

    @BeforeEach
    public void setUp() {
        JUnitUtil.setUp();
    }

    @AfterEach
    public void tearDown() {
        JUnitUtil.tearDown();
    }

    // --- helpers ---

    /** Builds a 13-byte frame with the given CAN ID (29 bit, big-endian) and DLC. */
    private static int[] frameWith(long canId, int dlc) {
        int[] f = new int[13];
        f[0] = (int)((canId >> 24) & 0xFF);
        f[1] = (int)((canId >> 16) & 0xFF);
        f[2] = (int)((canId >>  8) & 0xFF);
        f[3] = (int)( canId        & 0xFF);
        f[4] = dlc & 0x0F;
        return f;
    }

    /** A 29 bit CAN ID whose hash carries the CS2 fingerprint (bits 9:7 = 110b). */
    private static long cs2CanId(int priority, int command, boolean response) {
        return ((long)(priority & 0x0F) << 25)
             | ((long)(command  & 0xFF) << 17)
             | ((response ? 1L : 0L)    << 16)
             | 0x0300L;  // hash = 0x0300 -> fingerprint bits 9:7 = 110b
    }

    /** A 29 bit CAN ID whose hash carries the CdB config pattern (hash & 0x0F80 == 0x0A80). */
    private static long cdbCanId() {
        // hash = 0x0A80 satisfies (hash & 0x0F80) == 0x0A80
        return 0x0A80L;
    }

    // --- CS2_NATIVE ---

    @Test
    public void testClassifiesCs2NativeFromFingerprintInHash() {
        int[] frame = frameWith(cs2CanId(0, 0x05, false), 3);
        McanFrame result = ProtocolClassifier.classify(frame);
        assertEquals(FrameProtocol.CS2_NATIVE, result.protocol());
    }

    @Test
    public void testCs2NativeResultHasCs2Header() {
        int[] frame = frameWith(cs2CanId(2, 0x30, true), 5);
        Cs2Frame result = assertInstanceOf(Cs2Frame.class, ProtocolClassifier.classify(frame));
        assertEquals(2,     result.header().priority());
        assertEquals(0x30,  result.header().commandCode());
        assertTrue(result.header().response());
    }

    @Test
    public void testCs2NativeIsKnownProtocol() {
        int[] frame = frameWith(cs2CanId(0, 0, false), 0);
        assertTrue(ProtocolClassifier.classify(frame).isKnownProtocol());
    }

    // --- CDB_CONFIG ---

    @Test
    public void testClassifiesCdBConfigFromHashPattern() {
        int[] frame = frameWith(cdbCanId(), 0);
        McanFrame result = ProtocolClassifier.classify(frame);
        assertEquals(FrameProtocol.CDB_CONFIG, result.protocol());
    }

    @Test
    public void testCdBConfigIsKnownProtocol() {
        int[] frame = frameWith(cdbCanId(), 0);
        assertTrue(ProtocolClassifier.classify(frame).isKnownProtocol());
    }

    @Test
    public void testCdBConfigIsTypedCdBFrame() {
        int[] frame = frameWith(cdbCanId(), 0);
        assertInstanceOf(CdBFrameParser.CdBFrame.class, ProtocolClassifier.classify(frame));
    }

    // --- CS1_NATIVE ---

    @Test
    public void testClassifiesMs1TraceAsBaseCs1Frames() {
        // Registration and operating IDs from the ECoS/MS1 trace in section 11.
        long[] ids = {0x1C00007EL, 0x1C000CFEL, 0x1C08AC7EL, 0x1C038CFEL,
                      0x0C0007AAL, 0x18000A2BL, 0x180020AAL};
        int[] dlcs = {0, 8, 0, 8, 8, 2, 4};
        for (int i = 0; i < ids.length; i++) {
            int[] raw = frameWith(ids[i], dlcs[i]);
            raw[5] = 0x12;
            raw[6] = 0xAB;
            Cs1Frame result = assertInstanceOf(Cs1Frame.class, ProtocolClassifier.classify(raw));
            assertEquals(FrameProtocol.CS1_NATIVE, result.protocol());
            assertTrue(result.isKnownProtocol());
            assertEquals(dlcs[i], result.dlc());
            assertArrayEquals(raw, result.raw().toArray());
        }
    }

    @Test
    public void testClassifiesMs1ProbeAndFirmwareTransfer() {
        assertInstanceOf(Cs1Frame.class, ProtocolClassifier.classify(frameWith(0x380, 8)));
        // Flash readiness.
        assertInstanceOf(Cs1Frame.class, ProtocolClassifier.classify(frameWith(0x1C02807F, 4)));
        // An unmodelled operating command still returns the base class.
        assertInstanceOf(Cs1Frame.class, ProtocolClassifier.classify(frameWith(0x08004281, 3)));
    }

    @Test
    public void testRejectsInvalidOrUndocumentedCs1Layouts() {
        long[] ids = {
            0x3C00007EL, // Outside the 29 bit CAN identifier.
            0x1D02007EL, // Later registration stage must not carry a UID fragment.
            0x1C00017EL, // Registration command must be 0 or 1.
            0x1C00002BL, // Registration priority with an assigned operating node.
            0x18000A7EL, // Operating priority with the registration node.
            0x04000A2BL, // Priority 1 has not been documented for MS1.
            0x10000281L  // Undocumented E-CAN trace entry with zero object handle.
        };
        for (long id : ids) {
            assertInstanceOf(UnknownFrame.class, ProtocolClassifier.classify(frameWith(id, 8)));
        }
        assertInstanceOf(UnknownFrame.class, ProtocolClassifier.classify(frameWith(0x1C00007E, 8)));
        assertInstanceOf(Cs1Frame.class, ProtocolClassifier.classify(frameWith(0x1C0000FE, 0)));
        assertInstanceOf(UnknownFrame.class, ProtocolClassifier.classify(frameWith(0x18000A2B, 9)));
    }

    @Test
    public void testCs2AndCdBTakePrecedenceOverCs1Layouts() {
        // MS1 command bits 110 are reserved for CS2.
        assertInstanceOf(Cs2Frame.class, ProtocolClassifier.classify(frameWith(0x1C00037F, 8)));
        // This could otherwise look like MS1 operating traffic.
        assertInstanceOf(CdBFrameParser.CdBFrame.class,
                ProtocolClassifier.classify(frameWith(0x18000A80, 8)));
    }

    // --- UNKNOWN ---

    @Test
    public void testClassifiesUnknownForUnrecognisedFrame() {
        // Hash 0x0000 has no CS2 fingerprint and no CdB pattern
        int[] frame = frameWith(0x0000_0000L, 0);
        McanFrame result = ProtocolClassifier.classify(frame);
        assertInstanceOf(UnknownFrame.class, result);
        assertEquals(FrameProtocol.UNKNOWN, result.protocol());
        assertArrayEquals(frame, result.raw().toArray());
    }

    @Test
    public void testClassifyRejectsMalformedInput() {
        assertThrows(IllegalArgumentException.class, () -> ProtocolClassifier.classify(null));
        assertThrows(IllegalArgumentException.class, () -> ProtocolClassifier.classify(new int[12]));
        assertThrows(IllegalArgumentException.class, () -> ProtocolClassifier.classify(new int[14]));
    }

    @Test
    public void testUnknownIsNotKnownProtocol() {
        int[] frame = frameWith(0x0000_0000L, 0);
        assertFalse(ProtocolClassifier.classify(frame).isKnownProtocol());
    }

    // --- mutual exclusivity ---

    @Test
    public void testCs2FingerprintAndCdBPatternAreMutuallyExclusive() {
        // Verify that no 16-bit hash value satisfies both patterns simultaneously.
        // CS2 fingerprint: (hash >> 7) & 0b111 == 0b110 -> hash & 0x0380 = 0x0300
        // CdB pattern:     (hash & 0x0F80) == 0x0A80   -> hash & 0x0F80 = 0x0A80
        // If both held: hash & 0x0380 = 0x0300 AND hash & 0x0F80 = 0x0A80
        //   From CdB: bit 11 = 1, bit 10 = 0, bit 9 = 1, bits 8:7 = 0b00
        //   From CS2: bits 9:7 = 0b110  -> bit 9 = 1, bit 8 = 1 (contradiction with bit 8 = 0)
        for (int hash = 0; hash <= 0xFFFF; hash++) {
            boolean isCs2 = Cs2Header.hasCs2Fingerprint(hash);
            boolean isCdB = ProtocolClassifier.isCdBConfigPattern(
                    new int[]{0, 0, (hash >> 8) & 0xFF, hash & 0xFF, 0,
                              0, 0, 0, 0, 0, 0, 0, 0});
            assertFalse(isCs2 && isCdB,
                "Hash 0x" + Integer.toHexString(hash) + " matches both CS2 and CdB patterns");
        }
    }
}
