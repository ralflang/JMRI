package jmri.jmrix.marklin;

import java.util.Objects;
import jmri.Manager;
import jmri.managers.AbstractManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for discovering, tracking, and maintaining MCAN devices on a Märklin connection.
 * <p>
 * Registered with {@link MarklinSystemConnectionMemo}. Listens to both native MCAN
 * messages ({@link MarklinListener}) and CdB config frames ({@link CdBConfigListener}).
 *
 * @author Ralf Lang (ralf.lang@ralf-lang.de)
 */
public class McanDeviceManager extends AbstractManager<McanDevice> implements MarklinListener, CdBConfigListener {

    private final MarklinSystemConnectionMemo memo;

    public McanDeviceManager(MarklinSystemConnectionMemo memo) {
        super(Objects.requireNonNull(memo, "memo cannot be null"));
        this.memo = memo;
        if (memo.getTrafficController() != null) {
            memo.getTrafficController().addMarklinListener(this);
            memo.getTrafficController().getCdBFilter().addCdBConfigListener(this);
        }
    }

    /**
     * Provide or create an {@link McanDevice} for the given identity.
     *
     * @param identity device identity
     * @return McanDevice bean
     */
    public McanDevice provideDevice(McanDeviceIdentity identity) {
        Objects.requireNonNull(identity, "identity cannot be null");
        String sysName = identity.getSystemName(getSystemPrefix());
        McanDevice device = getBySystemName(sysName);
        if (device == null) {
            device = new McanDevice(getSystemPrefix(), identity);
            register(device);
        }
        return device;
    }

    /**
     * Get device by identity.
     * @param identity device identity
     * @return McanDevice or null if not found
     */
    public McanDevice getDevice(McanDeviceIdentity identity) {
        if (identity == null) {
            return null;
        }
        return getBySystemName(identity.getSystemName(getSystemPrefix()));
    }

    /**
     * Trigger discovery of native Märklin MCAN devices (CS2, MS2, Gleisbox, etc.).
     * Sends a standard MCAN PING (command 0x18, DLC=0).
     * Replies will be automatically captured and processed by this manager.
     */
    public void discoverMarklinDevices() {
        if (memo.getTrafficController() == null) {
            return;
        }
        int[] encodedPing = MarklinCanCodec.builder()
                .setCommand(MarklinConstants.CMDPING)
                .setResponse(false)
                .build();
        MarklinMessage msg = new MarklinMessage(encodedPing);
        msg.setReplyExpected(false);
        memo.getTrafficController().sendMarklinMessage(msg, null);
    }

    /**
     * Trigger discovery of a specific CdB device class.
     * Sends a zero-DLC class-specific ping.
     * Replies will be automatically captured and processed by this manager.
     *
     * @param deviceClass target CdB device class to discover
     */
    public void discoverCdBDevice(CdBMessage.CdBDeviceClass deviceClass) {
        if (memo.getTrafficController() == null || deviceClass == null) {
            return;
        }
        int[] rawFrame = CdBMessage.createClassPing(deviceClass);
        log.info("CdB discovery ping for {} (class 0x{}) frame: {}", deviceClass.getDisplayName(),
                Integer.toHexString(deviceClass.getClassId()), formatFrame(rawFrame));
        MarklinMessage msg = new MarklinMessage(rawFrame);
        msg.setReplyExpected(false);
        memo.getTrafficController().sendMarklinMessage(msg, null);
    }

    private static String formatFrame(int[] frame) {
        StringBuilder sb = new StringBuilder();
        for (int b : frame) sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }

    /**
     * Trigger discovery of all known CdB device classes.
     * Sends zero-DLC pings for each registered CdB device class.
     * Replies will be automatically captured and processed by this manager.
     */
    public void discoverAllCdBDevices() {
        for (CdBMessage.CdBDeviceClass deviceClass : CdBMessage.CdBDeviceClass.values()) {
            discoverCdBDevice(deviceClass);
        }
    }

    @Override
    public Class<McanDevice> getNamedBeanClass() {
        return McanDevice.class;
    }

    @Override
    public String getBeanTypeHandled(boolean plural) {
        return Bundle.getMessage(plural ? "BeanNameMcanDevices" : "BeanNameMcanDevice");
    }

    @Override
    public int getXMLOrder() {
        return Manager.SENSORS;
    }

    @Override
    public char typeLetter() {
        return 'D';
    }

    @Override
    public String getSystemNamePrefix() {
        return getSystemPrefix() + ":dev:";
    }

    @Override
    public void message(MarklinMessage m) {
        processRawMessage(MarklinCanCodec.decode(m));
    }

    @Override
    public void reply(MarklinReply r) {
        processRawMessage(MarklinCanCodec.decode(r));
    }

    private void processRawMessage(MarklinCanCodec.DecodedMessage decoded) {
        if (decoded == null) {
            return;
        }

        // Check for PING response
        MarklinDeviceIdentifier.DeviceInfo pingInfo = MarklinDeviceIdentifier.fromPingResponse(decoded);
        if (pingInfo != null && !pingInfo.isBroadcast() && !pingInfo.isUninitialized()) {
            McanDeviceIdentity.Native identity = McanDeviceIdentity.forNative(pingInfo.getUid(), pingInfo.getDeviceType());
            McanDevice device = provideDevice(identity);
            device.touch();
            if (pingInfo.getSoftwareVersion() >= 0) {
                device.setSoftwareVersion(pingInfo.getSoftwareVersion());
            }
            device.setDeviceState(McanDeviceState.ONLINE);
        }
    }

    @Override
    public void cdBConfigReply(CdBClassFilter.Classification classification, MarklinReply rawReply) {
        if (classification == null || !classification.isCdBConfig()) {
            return;
        }

        int canClass = classification.getCanClass();
        int module = classification.getModule();

        log.info("CdB config reply received: frameType={} canClass=0x{} module={}",
                classification.getFrameType(), Integer.toHexString(canClass), module);

        if (module < 0) {
            return;
        }

        // Only PING_REPLY frames prove a device exists at this class+module combination.
        // READ_REPLY and WRITE frames are config protocol traffic for an already-selected
        // module and must not create phantom entries in the device table.
        if (classification.getFrameType() != CdBClassFilter.CdBFrameType.PING_REPLY) {
            return;
        }

        // A genuine discovery ping reply always carries payload (DLC > 0).
        // Post-deselect status broadcasts use DLC=0 and are classified as PING_REPLY
        // due to their CAN ID structure, but they are not device advertisements.
        if ((rawReply.getElement(4) & 0xFF) == 0) {
            log.debug("Ignoring DLC=0 PING_REPLY for canClass=0x{} module={} (post-deselect broadcast)",
                    Integer.toHexString(canClass), module);
            return;
        }

        McanDeviceIdentity.CdB identity = McanDeviceIdentity.forCdB(canClass, module);
        McanDevice device = provideDevice(identity);
        device.touch();
        device.setDeviceState(McanDeviceState.ONLINE);

        // Factory-fresh module: stash challenge bytes so the configure dialog can use them
        if (module == 0
                && classification.getFrameType() == CdBClassFilter.CdBFrameType.PING_REPLY
                && (rawReply.getElement(4) & 0xFF) == 6) {
            device.setFactoryChallengeBytes(rawReply.getElement(6) & 0xFF, rawReply.getElement(7) & 0xFF);
        }

        // Hardware fingerprint tracking — strictly class 0x27 (WeichenChef Kombi) only.
        // Bytes 9–12 of the ping reply are stable per physical device across ID changes.
        // FF C7 suffix means factory-fresh (not yet commissioned), skip it.
        if (canClass == 0x27
                && classification.getFrameType() == CdBClassFilter.CdBFrameType.PING_REPLY) {
            int b9  = rawReply.getElement(9)  & 0xFF;
            int b10 = rawReply.getElement(10) & 0xFF;
            int b11 = rawReply.getElement(11) & 0xFF;
            int b12 = rawReply.getElement(12) & 0xFF;
            if (!(b11 == 0xFF && b12 == 0xC7)) {
                long fp = ((long) b9 << 24) | ((long) b10 << 16) | ((long) b11 << 8) | b12;
                long existingFp = device.getHwFingerprint();
                if (existingFp == -1L) {
                    device.setHwFingerprint(fp);
                } else if (existingFp != fp) {
                    log.warn("WeichenChef Kombi fingerprint mismatch on {}: had 0x{} got 0x{}",
                            device.getSystemName(),
                            Long.toHexString(existingFp), Long.toHexString(fp));
                }
                // Look for a stale entry with this fingerprint under a different system name
                for (McanDevice existing : getNamedBeanSet()) {
                    if (existing == device) continue;
                    McanDeviceIdentity existingId = existing.getIdentity();
                    if (!existingId.isCdB()) continue;
                    if (((McanDeviceIdentity.CdB) existingId).getCanClass() != 0x27) continue;
                    if (existing.getHwFingerprint() != fp) continue;
                    // Same physical device, different module ID — carry user name across
                    String oldUserName = existing.getUserName();
                    log.info("WeichenChef Kombi fingerprint match: migrating user name '{}' from {} to {}",
                            oldUserName, existing.getSystemName(), device.getSystemName());
                    deregister(existing);
                    if (oldUserName != null && !oldUserName.isEmpty()) {
                        device.setUserName(oldUserName);
                    }
                    device.setHwFingerprint(fp);
                    break;
                }
            }
        }
    }

    @Override
    public void dispose() {
        if (memo.getTrafficController() != null) {
            memo.getTrafficController().removeMarklinListener(this);
            memo.getTrafficController().getCdBFilter().removeCdBConfigListener(this);
        }
        super.dispose();
    }

    private static final Logger log = LoggerFactory.getLogger(McanDeviceManager.class);
}
