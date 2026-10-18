package jmri.jmrix.marklin;

import java.util.Objects;
import jmri.Manager;
import jmri.managers.AbstractManager;

/**
 * Manager for discovering, tracking, and maintaining MCAN devices on a Märklin connection.
 * <p>
 * Registered with {@link MarklinSystemConnectionMemo}. Listens to both native MCAN
 * messages ({@link MarklinListener}) and CdB config frames ({@link CdBConfigListener}).
 *
 * @author Ralf Lang (ralf.lang@ralf-lang.de) Copyright (C) 2026
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

        if (module < 0) {
            return;
        }

        McanDeviceIdentity.CdB identity = McanDeviceIdentity.forCdB(canClass, module);
        McanDevice device = provideDevice(identity);
        device.touch();
        device.setDeviceState(McanDeviceState.ONLINE);
    }

    @Override
    public void dispose() {
        if (memo.getTrafficController() != null) {
            memo.getTrafficController().removeMarklinListener(this);
            memo.getTrafficController().getCdBFilter().removeCdBConfigListener(this);
        }
        super.dispose();
    }
}
