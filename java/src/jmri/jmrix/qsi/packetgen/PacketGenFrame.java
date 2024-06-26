package jmri.jmrix.qsi.packetgen;

import java.awt.Dimension;
import javax.swing.BoxLayout;
import jmri.jmrix.qsi.QsiMessage;
import jmri.jmrix.qsi.QsiSystemConnectionMemo;

/**
 * Frame for user input of QSI messages. Input is a sequence of hex pairs,
 * including the length, but not the lead 'A', checksum or final 'E'.
 *
 * @author Bob Jacobsen Copyright (C) 2007, 2008
 */
public class PacketGenFrame extends jmri.util.JmriJFrame {

    private QsiSystemConnectionMemo _memo = null;

    // member declarations
    javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
    javax.swing.JButton sendButton = new javax.swing.JButton();
    javax.swing.JTextField packetTextField = new javax.swing.JTextField(12);

    public PacketGenFrame(QsiSystemConnectionMemo memo) {
        super();
        _memo = memo;
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void initComponents() {
        // the following code sets the frame's initial state

        jLabel1.setText("Command:");
        jLabel1.setVisible(true);

        sendButton.setText("Send");
        sendButton.setVisible(true);
        sendButton.setToolTipText("Send packet");

        packetTextField.setText("");
        packetTextField.setToolTipText("Enter command as hex string");
        packetTextField.setMaximumSize(
                new Dimension(packetTextField.getMaximumSize().width,
                        packetTextField.getPreferredSize().height
                )
        );

        setTitle("Send QSI command");
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        getContentPane().add(jLabel1);
        getContentPane().add(packetTextField);
        getContentPane().add(sendButton);

        sendButton.addActionListener(this::sendButtonActionPerformed);

        // pack for display
        pack();
    }

    public void sendButtonActionPerformed(java.awt.event.ActionEvent e) {
        String input = packetTextField.getText();
        // TODO check input + feedback on error. Too easy to cause NPE
        _memo.getQsiTrafficController().sendQsiMessage(createPacket(input), null);
    }

    /**
     * Create a well-formed packet from a String
     * 
     * @param s input contents
     * @return The packet, with contents filled-in
     */
    QsiMessage createPacket(String s) {
        // gather bytes in result
        byte b[] = jmri.util.StringUtil.bytesFromHexString(s);
        if (b.length == 0) {
            return null;  // no such thing as a zero-length message
        }
        QsiMessage m = new QsiMessage(b.length);
        for (int i = 0; i < b.length; i++) {
            m.setElement(i, b[i]);
        }
        return m;
    }

}
