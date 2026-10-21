package jmri.jmrix.marklin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import jmri.util.ThreadingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Protocol service for WeichenChef Kombi (CdB class 0x27) modules.
 * <p>
 * Owns all CAN bus interaction for module discovery, property read/write,
 * address encoding, and configuration persistence. All listener callbacks
 * are dispatched on the Swing EDT via {@link ThreadingUtil#runOnGUI}.
 *
 * @author Ralf Lang (ralf.lang@ralf-lang.de)
 */
public class WeichenChefKombiParameterService {

    /**
     * Voltage calculation result.
     */
    public static class VoltageResult {
        public final float volts;
        public final boolean lowVoltageWarning;
        public final String text;

        public VoltageResult(float volts, boolean lowVoltageWarning, String text) {
            this.volts = volts;
            this.lowVoltageWarning = lowVoltageWarning;
            this.text = text;
        }
    }

    /**
     * Callback interface for WeichenChef Kombi protocol events.
     */
    public interface WeichenChefKombiListener {
        default void firmwareReceived(int hi, int lo) {}
        default void hardwareReceived(int hi, int lo, Integer chipType) {}
        default void voltageReceived(VoltageResult result) {}
        default void moduleTypeReceived(int moduleType) {}
        default void addressReceived(int channel, String format, int address) {}
        default void rmkReceived(int channel, int rmkAddr, int connectionNumber) {}
        default void switchingTimeReceived(int channel, int timeMs) {}
        default void cdBFlagReceived(int channel, boolean enabled) {}
        default void positionReceived(int channel, int statusByte) {}
        default void factoryIdModeEntered() {}
        default void moduleFound(int rawId) {}
    }

    private final McanDevice device;
    private final McanFrameSender sender;
    private final List<WeichenChefKombiListener> listeners = new CopyOnWriteArrayList<>();

    private int currentRawId = 0; // 0 = none selected
    private boolean factorySelectSent = false;
    private int factoryChallenge0 = 0;
    private int factoryChallenge1 = 0;
    private int moduleType = 0;
    private String chipType = null;

    // Stored module addresses (5 outputs) for position matching
    private final int[] moduleAddresses = new int[5];

    public WeichenChefKombiParameterService(McanDevice device, McanFrameSender sender) {
        this.device = Objects.requireNonNull(device, "device cannot be null");
        this.sender = Objects.requireNonNull(sender, "sender cannot be null");
    }

    public void addListener(WeichenChefKombiListener listener) {
        listeners.add(listener);
    }

    public void removeListener(WeichenChefKombiListener listener) {
        listeners.remove(listener);
    }

    /**
     * Select a module: deselect current (if any), select new, send property query.
     * For factory-fresh modules (rawId == 0), also sends the normal select frame first
     * (matching the reference tool), then waits for the DLC=6 ping reply to complete
     * the challenge/response handshake reactively.
     *
     * @param rawId module raw ID (0 for factory-fresh, 1..127 for normal modules)
     */
    public void selectModule(int rawId) {
        int canClass = getCanClass();
        if (currentRawId > 0) {
            sender.sendFrame(CdBMessage.createDeselectModule(canClass, currentRawId));
        }
        currentRawId = rawId;
        factorySelectSent = false;
        if (rawId > 0) {
            sender.sendFrame(CdBMessage.createSelectModule(canClass, rawId));
            startPropertyQuery();
        }
        // rawId == 0: do NOT send a normal select — the factory handshake (DLC=6)
        // must be the first and only select frame, sent reactively on the next ping reply.
    }

    /**
     * Deselect the currently selected module (if any).
     */
    public void deselectModule() {
        if (currentRawId > 0) {
            sender.sendFrame(CdBMessage.createDeselectModule(getCanClass(), currentRawId));
            currentRawId = 0;
        }
        factorySelectSent = false;
    }

    /**
     * Send all property read frames for the currently selected module.
     */
    public void startPropertyQuery() {
        if (currentRawId == 0) return;
        int canClass = getCanClass();
        for (int[] frame : CdBMessage.createReadAllPropertiesWeichenChef(canClass, currentRawId)) {
            sender.sendFrame(frame);
        }
    }

    /**
     * Write all configuration properties.
     *
     * @param config configuration to write
     */
    public void writeConfiguration(WeichenChefKombiConfig config) {
        if (currentRawId == 0) return;
        int canClass = getCanClass();

        // Write switching times (outputs 1-4)
        for (int ch = 0; ch < 4; ch++) {
            sender.sendFrame(CdBMessage.createWriteChSwitchingTime(canClass, currentRawId, ch + 1, config.switchingTimes[ch]));
        }

        // Write CdB flags (outputs 1-4)
        for (int ch = 0; ch < 4; ch++) {
            int flag = 0;
            if (config.cdBFlags[ch]) {
                flag = (moduleType == 2) ? 2 : 1;
            }
            sender.sendFrame(CdBMessage.createWriteChCdBFlag(canClass, currentRawId, ch + 1, flag));
        }

        // Write addresses (outputs 1-5)
        for (int ch = 0; ch < 5; ch++) {
            sender.sendFrame(CdBMessage.createWriteChAddress(canClass, currentRawId, ch + 1, config.addresses[ch]));
        }

        // Write RMK addresses (outputs 1-8)
        for (int ch = 0; ch < 8; ch++) {
            sender.sendFrame(CdBMessage.createWriteChRmk(canClass, currentRawId, ch + 1, config.rmkAddresses[ch]));
        }
    }

    /**
     * Send factory reset frame, then re-read all properties.
     * The module stays selected; the UI should repopulate with factory-default values.
     */
    public void resetModule() {
        if (currentRawId == 0) return;
        sender.sendFrame(CdBMessage.createFactoryReset(getCanClass(), currentRawId));
        startPropertyQuery();
    }

    /**
     * Write new module ID (factory ID assignment flow).
     * Sends write reg=2 to the currently selected module, then deselects.
     * The module is no longer reachable at the old ID after this call.
     *
     * @param newId new raw ID to assign (1..127)
     */
    public void writeNewModuleId(int newId) {
        if (currentRawId < 0) return;
        int canClass = getCanClass();
        int[] writeFrame = CdBMessage.createWriteNewModuleId(canClass, currentRawId, newId,
                factoryChallenge0, factoryChallenge1);
        int[] deselectFrame = CdBMessage.createDeselectModule(canClass, currentRawId);
        log.info("writeNewModuleId: currentRawId={} newId={} challenge=[{},{}] writeFrame={} deselectFrame={}",
                currentRawId, newId,
                String.format("%02X", factoryChallenge0), String.format("%02X", factoryChallenge1),
                formatFrame(writeFrame), formatFrame(deselectFrame));
        sender.sendFrame(writeFrame);
        sender.sendFrame(deselectFrame);
        currentRawId = 0;
        // Keep factorySelectSent=true so subsequent pings don't re-enter the handshake
    }

    /**
     * Handle an incoming CdB reply frame.
     *
     * @param cls classification from CdBClassFilter
     * @param raw raw 13-byte reply
     */
    public void handleCdBReply(CdBClassFilter.Classification cls, MarklinReply raw) {
        if (!cls.isCdBConfig()) return;
        if (cls.getCanClass() != getCanClass()) return;

        if (cls.getFrameType() == CdBClassFilter.CdBFrameType.PING_REPLY) {
            int moduleRawId = cls.getModule();
            if (moduleRawId > 0) {
                final int id = moduleRawId;
                ThreadingUtil.runOnGUI(() -> listeners.forEach(l -> l.moduleFound(id)));
            } else if (moduleRawId == 0 && (raw.getElement(4) & 0xFF) == 6 && !factorySelectSent) {
                int challenge0 = raw.getElement(6) & 0xFF;
                int challenge1 = raw.getElement(7) & 0xFF;
                int[] selectFrame = CdBMessage.createFactorySelectModule(getCanClass(), challenge0, challenge1);
                log.info("Factory module ping: challenge=[{}, {}], sending select frame: {}",
                        String.format("%02X", challenge0), String.format("%02X", challenge1),
                        formatFrame(selectFrame));
                currentRawId = 0;
                factoryChallenge0 = challenge0;
                factoryChallenge1 = challenge1;
                factorySelectSent = true;
                sender.sendFrame(selectFrame);
                ThreadingUtil.runOnGUI(() -> listeners.forEach(WeichenChefKombiListener::factoryIdModeEntered));
            }
            return;
        }

        if (cls.getFrameType() == CdBClassFilter.CdBFrameType.READ_REPLY) {
            if (cls.getModule() != currentRawId) return;
            dispatchReadReply(raw);
        }
    }

    /**
     * Handle a standard MCAN status frame (position/occupancy from accessory decoder).
     * Packet format: byte[0]=0, byte[1]=23, byte[3]=moduleAddr-128, bytes[7-8]=address, byte[9]=status.
     *
     * @param raw raw 13-byte reply
     */
    public void handleStatusFrame(MarklinReply raw) {
        if (raw.getElement(0) != 0 || raw.getElement(1) != 23) return;
        int frameAddr = raw.getElement(3) & 0xFF;
        if (frameAddr != currentRawId && frameAddr != 48) return;

        int addr = ((raw.getElement(7) & 0xFF) << 8) + (raw.getElement(8) & 0xFF);
        int status = raw.getElement(9) & 0xFF;

        for (int i = 0; i < moduleAddresses.length; i++) {
            if (moduleAddresses[i] == addr) {
                final int ch = i;
                final int st = status;
                ThreadingUtil.runOnGUI(() -> listeners.forEach(l -> l.positionReceived(ch, st)));
            }
        }
    }

    // ---- private dispatch ----

    private void dispatchReadReply(MarklinReply raw) {
        int subcmd = raw.getElement(5) & 0xFF;
        int reg = raw.getElement(6) & 0xFF;

        switch (reg) {
            case 1:   handleFirmware(raw);                   break;
            case 244: handleHardware(raw);                   break;
            case 246: handleVoltage(raw, subcmd);            break;
            case 245: handleModuleType(raw);                 break;
            case 4:   if (subcmd >= 1 && subcmd <= 5) handleAddress(raw, subcmd);        break;
            case 5:   if (subcmd >= 1 && subcmd <= 8) handleRmk(raw, subcmd);           break;
            case 3:   if (subcmd >= 1 && subcmd <= 4) handleSwitchingTime(raw, subcmd); break;
            case 6:   if (subcmd >= 1 && subcmd <= 4) handleCdBFlag(raw, subcmd);       break;
            default:  break;
        }
    }

    private void handleFirmware(MarklinReply raw) {
        final int hi = raw.getElement(7) & 0xFF;
        final int lo = raw.getElement(8) & 0xFF;
        ThreadingUtil.runOnGUI(() -> listeners.forEach(l -> l.firmwareReceived(hi, lo)));
    }

    private void handleHardware(MarklinReply raw) {
        final int hi = raw.getElement(7) & 0xFF;
        final int lo = raw.getElement(8) & 0xFF;
        Integer chip = null;
        if ((raw.getElement(4) & 0xFF) == 5) {
            chip = raw.getElement(9) & 0xFF;
            chipType = String.format("%02x", chip);
        }
        final Integer chipVal = chip;
        ThreadingUtil.runOnGUI(() -> listeners.forEach(l -> l.hardwareReceived(hi, lo, chipVal)));
    }

    private void handleVoltage(MarklinReply raw, int subcmd) {
        if (subcmd != 0) return;
        int rawVoltage = ((raw.getElement(7) & 0xFF) << 8) + (raw.getElement(8) & 0xFF);
        VoltageResult result = calcVoltage(rawVoltage, chipType);
        ThreadingUtil.runOnGUI(() -> listeners.forEach(l -> l.voltageReceived(result)));
    }

    private void handleModuleType(MarklinReply raw) {
        moduleType = ((raw.getElement(7) & 0xFF) << 8) + (raw.getElement(8) & 0xFF);
        final int mt = moduleType;
        ThreadingUtil.runOnGUI(() -> listeners.forEach(l -> l.moduleTypeReceived(mt)));
    }

    private void handleAddress(MarklinReply raw, int subcmd) {
        int rawAddr = ((raw.getElement(8) & 0xFF) << 8) + (raw.getElement(7) & 0xFF); // little-endian
        if (subcmd >= 1 && subcmd <= 5) {
            moduleAddresses[subcmd - 1] = rawAddr;
        }
        CdBMessage.AddressEncoding enc = (moduleType == 3)
            ? CdBMessage.decodeAddressType3(rawAddr)
            : CdBMessage.decodeAddress(rawAddr);
        final int ch = subcmd;
        final String fmt = enc.format;
        final int addr = enc.address;
        ThreadingUtil.runOnGUI(() -> listeners.forEach(l -> l.addressReceived(ch, fmt, addr)));
    }

    private void handleRmk(MarklinReply raw, int subcmd) {
        int rmkAddr = ((raw.getElement(8) & 0xFF) << 8) + (raw.getElement(7) & 0xFF); // little-endian
        int connNum = (moduleType == 3) ? (raw.getElement(9) & 0xFF) + 1 : 0;
        final int ch = subcmd;
        final int rmk = (moduleType == 3) ? rmkAddr + 1 : rmkAddr;
        final int conn = connNum;
        ThreadingUtil.runOnGUI(() -> listeners.forEach(l -> l.rmkReceived(ch, rmk, conn)));
    }

    private void handleSwitchingTime(MarklinReply raw, int subcmd) {
        int time = ((raw.getElement(8) & 0xFF) << 8) + (raw.getElement(7) & 0xFF); // little-endian
        final int ch = subcmd;
        final int t = time;
        ThreadingUtil.runOnGUI(() -> listeners.forEach(l -> l.switchingTimeReceived(ch, t)));
    }

    private void handleCdBFlag(MarklinReply raw, int subcmd) {
        boolean on = (raw.getElement(8) & 0xFF) > 0;
        final int ch = subcmd;
        final boolean flag = on;
        ThreadingUtil.runOnGUI(() -> listeners.forEach(l -> l.cdBFlagReceived(ch, flag)));
    }

    // ---- static helpers ----

    /**
     * Calculate voltage from raw ADC value.
     *
     * @param rawVoltage 16-bit ADC value
     * @param chipType chip type string (use "61" for bus-voltage chip)
     * @return VoltageResult
     */
    public static VoltageResult calcVoltage(int rawVoltage, String chipType) {
        long voltCalc;
        String label;
        if ("61".equals(chipType)) {
            voltCalc = (long) rawVoltage * 33 / 100;
            label = "Bus voltage";
        } else {
            voltCalc = (long) rawVoltage * 68 / 1000;
            label = "Operating voltage";
        }
        long voltV = voltCalc / 100;
        long voltFrac = (voltCalc % 100) / 10;
        boolean warn = (voltV < 9 || voltV > 18);
        float voltsFloat = voltCalc / 100.0f;
        String text = label + ": " + voltV + "." + voltFrac + " V";
        return new VoltageResult(voltsFloat, warn, text);
    }

    private int getCanClass() {
        McanDeviceIdentity identity = device.getIdentity();
        if (identity.isCdB()) {
            return ((McanDeviceIdentity.CdB) identity).getCanClass();
        }
        return 0x27; // fallback
    }

    private static String formatFrame(int[] frame) {
        StringBuilder sb = new StringBuilder();
        for (int b : frame) sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }

    /**
     * Configuration data object for WeichenChef Kombi.
     */
    public static class WeichenChefKombiConfig {
        public int[] addresses = new int[5];
        public String[] formats = new String[]{"CAN", "CAN", "CAN", "CAN", "CAN"};
        public int[] switchingTimes = new int[4];
        public boolean[] cdBFlags = new boolean[4];
        public int[] rmkAddresses = new int[8];
        public int[] connectionNumbers = new int[8];
        public int moduleType = 2;
    }

    private static final Logger log = LoggerFactory.getLogger(WeichenChefKombiParameterService.class);
}
