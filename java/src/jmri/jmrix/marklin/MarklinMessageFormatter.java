package jmri.jmrix.marklin;

/**
 * Formats MCAN protocol messages for display and logging.
 * <p>
 * This class provides message formatting with optional i18n support.
 * When used without i18n (standalone/bridge), it falls back to English text.
 * <p>
 * Supports multiple format styles (currently implements MarklinMon-compatible format).
 *
 * @author Ralf Lang (ralf.lang@ralf-lang.de) Copyright (C) 2026
 */
public class MarklinMessageFormatter {

    /**
     * Optional i18n provider interface for localized messages.
     */
    public interface I18nProvider {
        /**
         * Get localized message.
         * @param key message key
         * @param args optional arguments for message formatting
         * @return localized message, or null if not available
         */
        String getMessage(String key, Object... args);
    }

    /**
     * Format a decoded MCAN message in MarklinMon style.
     * <p>
     * Output format: [Priority] Command: [details] [Request/Response] [Address] [Hex bytes]
     *
     * @param decoded the decoded MCAN message
     * @param rawData raw 13-byte message data for hex display
     * @param i18n optional i18n provider (null for English-only)
     * @return formatted message string
     */
    public static String format(MarklinCanCodec.DecodedMessage decoded, int[] rawData, I18nProvider i18n) {
        StringBuilder sb = new StringBuilder();

        appendPriority(decoded, sb);
        appendCommand(decoded, sb, i18n);
        appendResponse(decoded, sb, i18n);
        appendAddress(decoded, sb, i18n);
        appendCanHex(rawData, sb);

        return sb.toString();
    }

    /**
     * Format from raw bytes (convenience method).
     *
     * @param rawData 13-byte MCAN message
     * @param i18n optional i18n provider (null for English-only)
     * @return formatted message string
     */
    public static String formatRaw(int[] rawData, I18nProvider i18n) {
        MarklinCanCodec.DecodedMessage decoded = MarklinCanCodec.decode(rawData);
        return format(decoded, rawData, i18n);
    }

    private static void appendPriority(MarklinCanCodec.DecodedMessage decoded, StringBuilder sb) {
        sb.append(MarklinCanCodec.getPriorityDescription(decoded.getPriority()));
    }

    private static void appendCommand(MarklinCanCodec.DecodedMessage decoded, StringBuilder sb, I18nProvider i18n) {
        sb.append(" Command: ");
        int command = decoded.getCommand();

        if (command == MarklinConstants.SYSCOMMANDSTART) {
            // System command - check data byte to determine subcommand
            int subCmd = decoded.getDataByte(0);
            if (subCmd == MarklinConstants.CMDSTOPSYS) {
                sb.append("System Stop");
            } else if (subCmd == MarklinConstants.CMDGOSYS) {
                sb.append("System Go");
            } else if (subCmd == MarklinConstants.CMDHALTSYS) {
                sb.append("System Halt");
            } else {
                sb.append("System (subcmd 0x").append(Integer.toHexString(subCmd)).append(")");
            }
        } else if (command >= MarklinConstants.MANCOMMANDSTART && command <= MarklinConstants.MANCOMMANDEND) {
            switch (command) {
                case 0x01:
                    sb.append("Management (0x01)");
                    break;
                case 0x02:
                    sb.append("Management (0x02)");
                    break;
                case MarklinConstants.LOCOEMERGENCYSTOP:
                    sb.append("Loco Emergency Stop");
                    break;
                case MarklinConstants.LOCOSPEED:
                    sb.append("Change of speed ").append((decoded.getDataByte(0) << 8) + decoded.getDataByte(1));
                    break;
                case MarklinConstants.LOCODIRECTION:
                    sb.append("Change of direction ").append(decoded.getDataByte(0));
                    break;
                case MarklinConstants.LOCOFUNCTION:
                    sb.append("Function: ").append(decoded.getDataByte(0)).append(" state: ").append(decoded.getDataByte(1));
                    break;
                case 0x07:
                    sb.append("Management (0x07)");
                    break;
                case 0x08:
                    sb.append("Management (0x08)");
                    break;
                case 0x09:
                    sb.append("Management (0x09)");
                    break;
                case 0x0A:
                    sb.append("Management (0x0A)");
                    break;
                default:
                    sb.append("Management");
            }
        } else if (command >= MarklinConstants.ACCCOMMANDSTART && command <= MarklinConstants.ACCCOMMANDEND) {
            sb.append("Accessory");
            int state = decoded.getDataByte(0);
            switch (state) {
                case 0x00:
                    // Try i18n first, fall back to English
                    if (i18n != null) {
                        String thrown = i18n.getMessage("TurnoutStateThrown");
                        String msg = i18n.getMessage("SetTurnoutState", thrown != null ? thrown : "Thrown");
                        if (msg != null) {
                            sb.append(msg);
                            break;
                        }
                    }
                    sb.append(" Set Turnout State Thrown");
                    break;
                case 0x01:
                    // Try i18n first, fall back to English
                    if (i18n != null) {
                        String closed = i18n.getMessage("TurnoutStateClosed");
                        String msg = i18n.getMessage("SetTurnoutState", closed != null ? closed : "Closed");
                        if (msg != null) {
                            sb.append(msg);
                            break;
                        }
                    }
                    sb.append(" Set Turnout State Closed");
                    break;
                default:
                    sb.append(" Unknown state command ").append(state);
            }
        } else if (command >= MarklinConstants.SOFCOMMANDSTART && command <= MarklinConstants.SOFCOMMANDEND) {
            switch (command) {
                case MarklinConstants.CMDPING:
                    sb.append("PING");
                    break;
                case 0x19:
                    sb.append("Software Update (0x19)");
                    break;
                case 0x1A:
                    sb.append("Software Update (0x1A)");
                    break;
                case MarklinConstants.CMDCANBOOT:
                    // Distinguish between Gleisbox activation (DLC=5, data[0]=0x11)
                    // and bootloader mode (DLC=0)
                    if (decoded.getDataLength() == 5 && decoded.getDataByte(0) == 0x11) {
                        sb.append("CAN BOOT (Gleisbox Activation)");
                    } else if (decoded.getDataLength() == 0) {
                        sb.append("CAN BOOT (Bootloader Mode)");
                    } else {
                        sb.append("CAN BOOT");
                    }
                    break;
                case 0x1C:
                    sb.append("Software (0x1C)");
                    break;
                default:
                    sb.append("Software");
            }
        } else if (command >= MarklinConstants.GUICOMMANDSTART && command <= MarklinConstants.GUICOMMANDEND) {
            sb.append("GUI");
        } else if (command >= MarklinConstants.AUTCOMMANDSTART && command <= MarklinConstants.AUTCOMMANDEND) {
            sb.append("Automation");
        } else if (command >= MarklinConstants.FEECOMMANDSTART && command <= MarklinConstants.FEECOMMANDEND) {
            switch (command) {
                case 0x10:
                    sb.append("Feedback (0x10)");
                    break;
                case MarklinConstants.S88EVENT:
                    sb.append("S88 Event");
                    break;
                case 0x12:
                    sb.append("Feedback (0x12)");
                    break;
                default:
                    sb.append("Feedback");
            }
        }
    }

    private static void appendResponse(MarklinCanCodec.DecodedMessage decoded, StringBuilder sb, I18nProvider i18n) {
        sb.append(" ");
        if (decoded.isResponse()) {
            // Try i18n first, fall back to English
            if (i18n != null) {
                String msg = i18n.getMessage("ReplyMessage");
                if (msg != null) {
                    sb.append(msg);
                    return;
                }
            }
            sb.append("Reply");
        } else {
            // Try i18n first, fall back to English
            if (i18n != null) {
                String msg = i18n.getMessage("RequestMessage");
                if (msg != null) {
                    sb.append(msg);
                    return;
                }
            }
            sb.append("Request");
        }
    }

    private static void appendAddress(MarklinCanCodec.DecodedMessage decoded, StringBuilder sb, I18nProvider i18n) {
        long addr = decoded.getAddress();
        long baseAddr = MarklinCanCodec.getBaseAddress(addr);
        String protocol = MarklinCanCodec.getProtocolFromAddress(addr);

        if (addr == 0) {
            sb.append(" Broadcast");
        } else if (protocol.equals("MM Function Decoder")) {
            sb.append(" to MM Function decoder ").append(baseAddr);
        } else if (protocol.contains("Loco")) {
            // Try i18n first, fall back to English
            if (i18n != null) {
                String msg = i18n.getMessage("MonTrafToLocoAddress", baseAddr);
                if (msg != null) {
                    sb.append(" ").append(msg);
                    return;
                }
            }
            sb.append(" to Loco Address ").append(baseAddr);
        } else if (protocol.contains("Accessory")) {
            sb.append(" to ").append(protocol).append(" ").append(baseAddr);
        } else if (protocol.equals("DCC")) {
            sb.append(" to DCC Address ").append(baseAddr);
        } else if (protocol.equals("MFX")) {
            sb.append(" to MFX Address ").append(baseAddr);
        } else if (protocol.contains("SX")) {
            sb.append(" to ").append(protocol).append(" Address ").append(baseAddr);
        }
    }

    private static void appendCanHex(int[] rawData, StringBuilder sb) {
        sb.append(" 0x").append(Integer.toHexString(rawData[0]));
        for (int i = 1; i < rawData.length; i++) {
            sb.append(", 0x").append(Integer.toHexString(rawData[i]));
        }
    }
}
