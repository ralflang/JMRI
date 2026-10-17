package jmri.jmrix.marklin;

import jmri.SystemConnectionMemo;
import jmri.managers.AbstractManager;

import java.util.*;
import java.util.concurrent.*;

/**
 * Single device manager and discovery engine for MCAN (Native Märklin and CAN-digital-Bahn) bus devices.
 */
public class McanDeviceManager extends AbstractManager<McanDevice> implements MarklinListener, CdBConfigListener {

    public static final int[] CDB_KNOWN_CLASSES = {
            0x0B, // StromSniffer
            0x0D, // GleisReporter / deLuxe
            0x21, // ReporterChef 002
            0x22, // WeichenChef Magnet M 012 / deLuxe 007
            0x23, // WeichenChef AC 002
            0x26, // WeichenChef Basic 002
            0x27  // WeichenChef Kombi
    };

    private final MarklinTrafficController tc;

    // Key for pending read futures: "canClass:module:index:param"
    private final ConcurrentMap<String, CompletableFuture<CdBReadReply>> pendingReads = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public McanDeviceManager(MarklinSystemConnectionMemo memo) {
        super(memo);
        this.tc = memo.getTrafficController();
        if (tc != null) {
            tc.addMarklinListener(this);
            if (tc.getCdBFilter() != null) {
                tc.getCdBFilter().addCdBConfigListener(this);
            }
        }
    }

    public List<McanDevice> getKnownDevices() {
        return new ArrayList<>(getNamedBeanSet());
    }

    public Optional<McanDevice> getDevice(McanDeviceIdentity identity) {
        if (identity == null) return Optional.empty();
        return Optional.ofNullable(getBySystemName(makeSystemName(identity.toSystemIdFragment())));
    }

    public McanDevice provideDevice(McanDeviceIdentity identity, String familyName) {
        String sysName = makeSystemName(identity.toSystemIdFragment());
        McanDevice device = getBySystemName(sysName);
        if (device == null) {
            device = new McanDevice(sysName, identity, familyName);
            register(device);
        }
        return device;
    }

    @Override
    public String makeSystemName(String suffix) {
        return "M:dev:" + suffix;
    }

    // --- CdB parameter operations ---

    public CompletableFuture<CdBReadReply> readParam(McanDeviceIdentity.CdB device, int index, int param) {
        CompletableFuture<CdBReadReply> future = new CompletableFuture<>();
        String key = makePendingKey(device.getCanClass(), device.getModuleId(), index, param);
        pendingReads.put(key, future);

        // Timeout task
        scheduler.schedule(() -> {
            CompletableFuture<CdBReadReply> removed = pendingReads.remove(key);
            if (removed != null && !removed.isDone()) {
                removed.completeExceptionally(new TimeoutException("Timeout reading param " + param + " index " + index));
            }
        }, 1000, TimeUnit.MILLISECONDS);

        if (tc != null) {
            tc.sendMarklinMessage(CdBMessage.readParam(device, index, param), this);
        }
        return future;
    }

    public void writeParam(McanDeviceIdentity.CdB device, int index, int param, int valueHi, int valueLo) {
        if (tc != null) {
            tc.sendMarklinMessage(CdBMessage.writeParam(device, index, param, valueHi, valueLo), this);
        }
    }

    public void sendReset(McanDeviceIdentity.CdB device) {
        if (tc != null) {
            tc.sendMarklinMessage(CdBMessage.reset(device), this);
        }
    }

    public void sendRelease(McanDeviceIdentity.CdB device) {
        if (tc != null) {
            tc.sendMarklinMessage(CdBMessage.release(device), this);
        }
    }

    private String makePendingKey(int canClass, int module, int index, int param) {
        return canClass + ":" + module + ":" + index + ":" + param;
    }

    // --- Callbacks ---

    @Override
    public void message(MarklinMessage m) {
        // Sent messages
    }

    @Override
    public void reply(MarklinReply r) {
        if (r == null || r.getNumDataElements() < 13) return;
        int[] rawData = new int[13];
        for (int i = 0; i < 13; i++) {
            rawData[i] = r.getElement(i);
        }
        MarklinCanCodec.DecodedMessage decoded = MarklinCanCodec.decode(rawData);
        if (MarklinDeviceIdentifier.isPingResponse(decoded)) {
            MarklinDeviceIdentifier.DeviceInfo info = MarklinDeviceIdentifier.fromPingResponse(decoded);
            if (info != null) {
                McanDeviceIdentity.Native identity = new McanDeviceIdentity.Native(info.getUid(), info.getDeviceType(), info.getHash());
                String familyName = MarklinDeviceIdentifier.getDeviceTypeName(info.getDeviceType());
                McanDevice device = provideDevice(identity, familyName);
                device.setFirmwareVersion(info.getSoftwareVersion());
                device.touchPingReply();
                device.setDeviceState(McanDeviceState.ONLINE);
            }
        }
    }

    @Override
    public void cdBConfigReply(CdBClassFilter.Classification c, MarklinReply r) {
        if (c == null || !c.isCdBConfig()) return;

        McanDeviceIdentity.CdB identity = new McanDeviceIdentity.CdB(c.getCanClass(), c.getModule());
        McanDevice device = provideDevice(identity, "CdB Class 0x" + Integer.toHexString(c.getCanClass()).toUpperCase());

        if (c.getModule() == 0) {
            device.setAddressConflict(true);
        }

        if (c.getFrameType() == CdBClassFilter.CdBFrameType.PING_REPLY) {
            if (r.getNumDataElements() > 4) {
                device.setPingReplyDlc(r.getElement(4));
            }
            device.touchPingReply();
            device.setDeviceState(McanDeviceState.ONLINE);
        } else if (c.getFrameType() == CdBClassFilter.CdBFrameType.READ_REPLY) {
            device.touchSeen();
            MarklinCanCodec.DecodedMessage decoded = c.getDecoded();
            if (decoded != null && decoded.getDataLength() >= 4) {
                int[] data = decoded.getData();
                int index = data[0];
                int param = data[1];
                int valHi = data[2];
                int valLo = data[3];

                CdBReadReply reply = new CdBReadReply(index, param, valHi, valLo, decoded.getDataLength(), data);
                String key = makePendingKey(c.getCanClass(), c.getModule(), index, param);
                CompletableFuture<CdBReadReply> future = pendingReads.remove(key);
                if (future != null) {
                    future.complete(reply);
                }
            }
        } else {
            device.touchSeen();
        }
    }

    @Override
    public SystemConnectionMemo getMemo() {
        return (SystemConnectionMemo) super.getMemo();
    }

    @Override
    public char typeLetter() {
        return 'M';
    }

    @Override
    public Class<McanDevice> getNamedBeanClass() {
        return McanDevice.class;
    }

    @Override
    public String getBeanTypeHandled(boolean plural) {
        return "MCAN Device";
    }

    @Override
    public int getXMLOrder() {
        return 50;
    }
}
