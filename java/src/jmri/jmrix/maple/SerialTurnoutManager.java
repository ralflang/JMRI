package jmri.jmrix.maple;

import java.util.Locale;
import javax.annotation.Nonnull;
import jmri.Turnout;
import jmri.managers.AbstractTurnoutManager;

/**
 * Implement turnout manager for serial systems
 * <p>
 * System names are "KTnnn", where K is the user configurable system prefix,
 * nnn is the turnout number without padding.
 *
 * @author Bob Jacobsen Copyright (C) 2003, 2008
 */
public class SerialTurnoutManager extends AbstractTurnoutManager {

    public SerialTurnoutManager(MapleSystemConnectionMemo memo) {
        super(memo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public MapleSystemConnectionMemo getMemo() {
        return (MapleSystemConnectionMemo) memo;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    protected Turnout createNewTurnout(@Nonnull String systemName, String userName) throws IllegalArgumentException {
        // validate the system name, and normalize it
        String sName = SerialAddress.normalizeSystemName(systemName, getSystemPrefix());
        if (sName.isEmpty()) {
            // system name is not valid
            throw new IllegalArgumentException("Cannot create System Name from " + systemName);
        }
        // does this turnout already exist
        Turnout t = getBySystemName(sName);
        if (t != null) {
            return t;
        }

        // check if the addressed output bit is available
        int bitNum = SerialAddress.getBitFromSystemName(sName, getSystemPrefix());
        if (bitNum == 0) {
            throw new IllegalArgumentException("Cannot get Bit from System Name " + systemName + " " + sName);
        }
        String conflict = SerialAddress.isOutputBitFree(bitNum, getSystemPrefix());
        if ((!conflict.isEmpty()) && (!conflict.equals(sName))) {
            log.error("{} assignment conflict with {}.", sName, conflict);
            throw new IllegalArgumentException("The output bit " + bitNum + ", is currently assigned to " + conflict + ".");
        }

        // create the turnout
        t = new SerialTurnout(sName, userName, getMemo());

        // does system name correspond to configured hardware
        if (!SerialAddress.validSystemNameConfig(sName, 'T', getMemo())) {
            // system name does not correspond to configured hardware
            log.warn("Turnout '{}' refers to an unconfigured output bit.", sName);
            jmri.util.swing.JmriJOptionPane.showMessageDialog(null, "WARNING - The Turnout just added, "
                    + sName + ", refers to an unconfigured output bit.", "Configuration Warning",
                    jmri.util.swing.JmriJOptionPane.INFORMATION_MESSAGE);
        }
        return t;
    }

    @Override
    public boolean allowMultipleAdditions(@Nonnull String systemName) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public String validateSystemNameFormat(@Nonnull String name, @Nonnull Locale locale) {
        return SerialAddress.validateSystemNameFormat(name, this, locale);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NameValidity validSystemNameFormat(@Nonnull String systemName) {
        return (SerialAddress.validSystemNameFormat(systemName, typeLetter(), getSystemPrefix()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEntryToolTip() {
        return Bundle.getMessage("AddOutputEntryToolTip");
    }

    /**
     * Get from the user, the number of addressed bits used to control a
     * turnout. Normally this is 1, and the default routine returns 1
     * automatically. Turnout Managers for systems that can handle multiple
     * control bits should override this method with one which asks the user to
     * specify the number of control bits. If the user specifies more than one
     * control bit, this method should check if the additional bits are
     * available (not assigned to another object). If the bits are not
     * available, this method should return 0 for number of control bits, after
     * informing the user of the problem. This function is called whenever a new
     * turnout is defined in the Turnout table. It can also be used to set up
     * other turnout control options, such as pulsed control of turnout
     * machines.
     */
// Code below to do with having a pulsed turnout type is commented out for current Maple version
// /**
//  * Get from the user, the type of output to be used bits to control a turnout.
//  * Normally this is 0 for 'steady state' control, and the default routine
//  * returns 0 automatically.
//  * Turnout Managers for systems that can handle pulsed control as well as
//  * steady state control should override this method with one which asks
//  * the user to specify the type of control to be used.  The routine should
//  * return 0 for 'steady state' control, or n for 'pulsed' control, where n
//  * specifies the duration of the pulse (normally in seconds).
// */
//  public int askControlType(String systemName) {
//  // ask if user wants 'steady state' output (stall motors, e.g., Tortoises) or
//  //   'pulsed' output (some turnout controllers).
//  int iType = selectOutputType();
//  if (iType == javax.swing.JOptionPane.CLOSED_OPTION) {
//   /* user cancelled without selecting an output type */
//   iType = 0;
//   log.warn("User cancelled without selecting output type. Defaulting to 'steady state'.");
//  }
//  // Note: If the user selects 'pulsed', this routine defaults to 1 second.
//  return (iType);
// }
//    /**
//     * Public method to allow user to specify one or two output bits for turnout control
//  *  Note: This method returns 1 or 2 if the user selected, or 0 if the user cancelled
//  *         without selecting.
//  */
// public int selectNumberOfControlBits() {
//  int iNum = 0;
//  iNum = javax.swing.JOptionPane.showOptionDialog(null,
//    "How many output bits should be used to control this turnout?",
//     "Turnout Question",javax.swing.JOptionPane.DEFAULT_OPTION,
//      javax.swing.JOptionPane.QUESTION_MESSAGE,
//      null, new String[] {"Use 1 bit", "Use 2 bits"}, "Use 1 bit");
//  return iNum;
// }
//    /**
//     * Public method to allow user to specify pulsed or steady state for two output bits
//  * for turnout control
//  *  Note: This method returns 1 for steady state or 2 for pulsed if the user selected,
//  *   or 0 if the user cancelled without selecting.
//  */
// public int selectOutputType() {
//  int iType = 0;
//  iType = javax.swing.JOptionPane.showOptionDialog(null,
//    "Should the output bit(s) be 'steady state' or 'pulsed'?",
//     "Output Bits Question",javax.swing.JOptionPane.DEFAULT_OPTION,
//      javax.swing.JOptionPane.QUESTION_MESSAGE,
//      null, new String[] {"Steady State Output", "Pulsed Output"}, "Steady State Output");
//  return iType;
// }
//    /**
//     * Public method to notify user when the second bit of a proposed two output bit turnout
//  *  has a conflict with another assigned bit
//     */
// public void notifySecondBitConflict(String conflict,int bitNum) {
//  javax.swing.JOptionPane.showMessageDialog(null,"The second output bit, "+bitNum+
//   ", is currently assigned to "+conflict+". Turnout cannot be created as "+
//     "you specified.", "Assignment Conflict",
//       javax.swing.JOptionPane.INFORMATION_MESSAGE,null);
// }


    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SerialTurnoutManager.class);

}
