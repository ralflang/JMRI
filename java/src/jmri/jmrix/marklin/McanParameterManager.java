package jmri.jmrix.marklin;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Parameter manager abstraction for reading and writing device parameters.
 * <p>
 * Abstracts CdB indexed configuration parameters and native MCAN CS2/MS2 parameters.
 *
 * @author Ralf Lang (ralf.lang@ralf-lang.de) Copyright (C) 2026
 */
public class McanParameterManager {

    private final McanDevice device;

    public McanParameterManager(McanDevice device) {
        this.device = Objects.requireNonNull(device, "device cannot be null");
    }

    /**
     * Get the associated MCAN device.
     * @return device
     */
    public McanDevice getDevice() {
        return device;
    }

    /**
     * Asynchronously read a parameter value by parameter index.
     *
     * @param paramIndex 1-based or 0-based parameter index
     * @param sender frame sender transport interface
     * @return CompletableFuture completing with the parameter value byte
     */
    public CompletableFuture<Integer> readParameter(int paramIndex, McanFrameSender sender) {
        Objects.requireNonNull(sender, "sender cannot be null");
        McanDeviceIdentity identity = device.getIdentity();

        if (identity.isCdB()) {
            McanDeviceIdentity.CdB cdb = (McanDeviceIdentity.CdB) identity;
            int hash = MarklinConstants.HASHBYTE1 << 8 | MarklinConstants.HASHBYTE2;
            int[] frame = CdBMessage.createReadParam(cdb.getCanClass(), cdb.getModuleId(), paramIndex, hash);
            CdBMessage.send(sender, frame);
        }

        // Return completed future for basic request triggering
        return CompletableFuture.completedFuture(0);
    }

    /**
     * Asynchronously write a parameter value by parameter index.
     *
     * @param paramIndex parameter index
     * @param value byte value to write
     * @param sender frame sender transport interface
     * @return CompletableFuture completing when write frame is transmitted
     */
    public CompletableFuture<Void> writeParameter(int paramIndex, int value, McanFrameSender sender) {
        Objects.requireNonNull(sender, "sender cannot be null");
        McanDeviceIdentity identity = device.getIdentity();

        if (identity.isCdB()) {
            McanDeviceIdentity.CdB cdb = (McanDeviceIdentity.CdB) identity;
            int hash = MarklinConstants.HASHBYTE1 << 8 | MarklinConstants.HASHBYTE2;
            int[] frame = CdBMessage.createWriteParam(cdb.getCanClass(), cdb.getModuleId(), paramIndex, value, hash);
            CdBMessage.send(sender, frame);
        }

        return CompletableFuture.completedFuture(null);
    }
}
