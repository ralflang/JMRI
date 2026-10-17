package jmri.jmrix.marklin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link McanDeviceManager}.
 */
public class McanDeviceManagerTest {

    private MarklinSystemConnectionMemo memo;
    private McanDeviceManager manager;

    @BeforeEach
    public void setUp() {
        memo = new MarklinSystemConnectionMemo();
        MarklinTrafficController tc = new MarklinTrafficController() {
            @Override
            public void sendMarklinMessage(MarklinMessage m, MarklinListener l) {
                // mock sender
            }
        };
        memo.setMarklinTrafficController(tc);
        manager = new McanDeviceManager(memo);
        memo.store(manager, McanDeviceManager.class);
    }

    @Test
    public void testProvideDevice() {
        McanDeviceIdentity.Native identity = new McanDeviceIdentity.Native(0x00112233L, 0x0001, 0x1234);
        McanDevice device = manager.provideDevice(identity, "Gleisbox");

        assertNotNull(device);
        assertEquals("M:dev:uid-00112233", device.getSystemName());
        assertEquals(1, manager.getKnownDevices().size());
        assertTrue(manager.getDevice(identity).isPresent());
    }

    @Test
    public void testNativePingReplyHandling() {
        // Native ping reply frame:
        // DecodedMessage expects 13 raw bytes array:
        // rawMessage[0..1]: priority/cmd (0x00, 0x31 -> cmd=0x18, resp=true)
        // rawMessage[2..3]: hash (0x11, 0x22)
        // rawMessage[4]: DLC (8)
        // rawMessage[5..8]: address (0x00000000)
        // rawMessage[9..12]: data bytes 0..3 (UID hi -> lo)
        // But for SW version & deviceType, MarklinDeviceIdentifier.fromPingResponse calls decoded.getDataByte(4..7)
        // which requires decoded.getData() to have at least 8 bytes.
        // Therefore, rawMessage DLC must be 8 and we mock decoded message directly.
        MarklinCanCodec.DecodedMessage decoded = new MarklinCanCodec.DecodedMessage(
                0, MarklinConstants.CMDPING, true, new int[]{0x11, 0x22},
                8, 0L, new int[]{0x00, 0x10, 0x00, 0x01, 0x01, 0x02, 0x00, 0x10}
        );

        MarklinDeviceIdentifier.DeviceInfo info = MarklinDeviceIdentifier.fromPingResponse(decoded);
        assertNotNull(info);
        assertEquals(0x0102, info.getSoftwareVersion());

        McanDeviceIdentity.Native identity = new McanDeviceIdentity.Native(info.getUid(), info.getDeviceType(), info.getHash());
        McanDevice device = manager.provideDevice(identity, info.getDeviceTypeName());
        device.setFirmwareVersion(info.getSoftwareVersion());
        device.setDeviceState(McanDeviceState.ONLINE);

        assertEquals(1, manager.getKnownDevices().size());
        assertEquals(McanDeviceState.ONLINE, device.getDeviceState());
        assertEquals(0x0102, device.getFirmwareVersion());
    }

    @Test
    public void testCdBReadReplyCorrelation() {
        McanDeviceIdentity.CdB cdbAddr = new McanDeviceIdentity.CdB(0x22, 3);
        CompletableFuture<CdBReadReply> future = manager.readParam(cdbAddr, 0, 1);

        assertFalse(future.isDone());

        // Simulate CdB read reply frame
        MarklinMessage msg = CdBMessage.readParam(cdbAddr, 0, 1);
        int[] raw = new int[13];
        for (int i = 0; i < 13; i++) {
            raw[i] = msg.getElement(i);
        }
        MarklinReply reply = new MarklinReply(raw);

        MarklinCanCodec.DecodedMessage decoded = MarklinCanCodec.decode(reply);

        CdBClassFilter.Classification classification = new CdBClassFilter.Classification(
                true, 0x22, CdBClassFilter.CdBFrameType.READ_REPLY, 3, decoded
        );

        manager.cdBConfigReply(classification, reply);

        // Verify device registration
        assertEquals(1, manager.getKnownDevices().size());
    }
}
