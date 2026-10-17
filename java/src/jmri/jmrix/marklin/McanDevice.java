package jmri.jmrix.marklin;

import jmri.implementation.AbstractNamedBean;
import jmri.util.ThreadingUtil;

import java.time.Instant;

/**
 * Represents a physical MCAN bus device (Native Märklin or CAN-digital-Bahn device) in JMRI.
 */
public class McanDevice extends AbstractNamedBean {

    private final McanDeviceIdentity identity;
    private final String familyName;

    private McanDeviceState deviceState = McanDeviceState.DISCOVERED;
    private int firmwareVersion = -1;
    private int hardwareVersion = -1;
    private int moduleType = -1;
    private int pingReplyDlc = -1;
    private Instant lastPingReply = null;
    private Instant lastSeen = null;
    private boolean addressConflict = false;

    public McanDevice(String sysName, McanDeviceIdentity identity, String familyName) {
        super(sysName, identity != null ? identity.toHumanString() : sysName);
        this.identity = identity;
        this.familyName = familyName;
        this.lastSeen = Instant.now();
    }

    public McanDeviceIdentity getIdentity() {
        return identity;
    }

    public String getFamilyName() {
        return familyName;
    }

    public McanDeviceState getDeviceState() {
        return deviceState;
    }

    public void setDeviceState(McanDeviceState newState) {
        if (this.deviceState != newState) {
            McanDeviceState oldState = this.deviceState;
            this.deviceState = newState;
            ThreadingUtil.runOnGUI(() -> firePropertyChange("deviceState", oldState, newState));
        }
    }

    public int getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(int firmwareVersion) {
        if (this.firmwareVersion != firmwareVersion) {
            int old = this.firmwareVersion;
            this.firmwareVersion = firmwareVersion;
            ThreadingUtil.runOnGUI(() -> firePropertyChange("firmwareVersion", old, firmwareVersion));
        }
    }

    public int getHardwareVersion() {
        return hardwareVersion;
    }

    public void setHardwareVersion(int hardwareVersion) {
        if (this.hardwareVersion != hardwareVersion) {
            int old = this.hardwareVersion;
            this.hardwareVersion = hardwareVersion;
            ThreadingUtil.runOnGUI(() -> firePropertyChange("hardwareVersion", old, hardwareVersion));
        }
    }

    public int getModuleType() {
        return moduleType;
    }

    public void setModuleType(int moduleType) {
        if (this.moduleType != moduleType) {
            int old = this.moduleType;
            this.moduleType = moduleType;
            ThreadingUtil.runOnGUI(() -> firePropertyChange("moduleType", old, moduleType));
        }
    }

    public int getPingReplyDlc() {
        return pingReplyDlc;
    }

    public void setPingReplyDlc(int pingReplyDlc) {
        this.pingReplyDlc = pingReplyDlc;
    }

    public Instant getLastPingReply() {
        return lastPingReply;
    }

    public void touchPingReply() {
        this.lastPingReply = Instant.now();
        touchSeen();
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void touchSeen() {
        this.lastSeen = Instant.now();
    }

    public boolean isAddressConflict() {
        return addressConflict;
    }

    public void setAddressConflict(boolean addressConflict) {
        if (this.addressConflict != addressConflict) {
            boolean old = this.addressConflict;
            this.addressConflict = addressConflict;
            ThreadingUtil.runOnGUI(() -> firePropertyChange("addressConflict", old, addressConflict));
        }
    }

    @Override
    public String getBeanType() {
        return "MCAN Device";
    }

    @Override
    public int getState() {
        return deviceState.ordinal();
    }

    @Override
    public void setState(int value) {
        if (value >= 0 && value < McanDeviceState.values().length) {
            setDeviceState(McanDeviceState.values()[value]);
        }
    }
}
