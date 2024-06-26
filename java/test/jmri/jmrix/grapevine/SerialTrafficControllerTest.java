package jmri.jmrix.grapevine;

import java.io.*;

import jmri.jmrix.AbstractMRMessage;
import jmri.jmrix.AbstractMRReply;
import jmri.util.JUnitAppender;
import jmri.util.JUnitUtil;

import org.junit.Assert;
import org.junit.jupiter.api.*;

/**
 * JUnit tests for the SerialTrafficController class
 *
 * @author Bob Jacobsen Copyright 2005, 2007, 2008
 */
public class SerialTrafficControllerTest extends jmri.jmrix.AbstractMRNodeTrafficControllerTest {

    byte[] testBuffer;
    boolean invoked;

    @Test
    public void testStateMachine1() throws java.io.IOException {
        SerialTrafficController c = (SerialTrafficController) tc;

        DataInputStream i = new DataInputStream(new ByteArrayInputStream(
                new byte[]{1, 2, 3, 4}));

        c.doNextStep(new SerialReply(), i);

        JUnitAppender.assertWarnMessage("1st byte not address: 1");
        Assert.assertEquals("not invoked", false, invoked);
    }

    @Test
    public void testStateMachine2() throws java.io.IOException {
        SerialTrafficController c = (SerialTrafficController) tc;

        DataInputStream i = new DataInputStream(new ByteArrayInputStream(
                new byte[]{(byte) 128, (byte) 129, 3, 4}));

        c.doNextStep(new SerialReply(), i);

        JUnitAppender.assertWarnMessage("2nd byte HOB set: 129, going to state 1");
        Assert.assertEquals("not invoked", false, invoked);
    }

    @Test
    public void testStateMachine3() throws java.io.IOException {
        SerialTrafficController c = (SerialTrafficController) tc;

        DataInputStream i = new DataInputStream(new ByteArrayInputStream(
                new byte[]{(byte) 128, (byte) 12, 1, 4}));

        c.doNextStep(new SerialReply(), i);

        JUnitAppender.assertWarnMessage("addresses don't match: 128, 1. going to state 1");
        Assert.assertEquals("not invoked", false, invoked);
    }

    @Test
    public void testStateMachine4() throws java.io.IOException {
        SerialTrafficController c = (SerialTrafficController) tc;

        DataInputStream i = new DataInputStream(new ByteArrayInputStream(
                new byte[]{(byte) 128, (byte) 12, (byte) 128, (byte) 129}));

        c.doNextStep(new SerialReply(), i);

        JUnitAppender.assertWarnMessage("3rd byte HOB set: 129, going to state 1");
        Assert.assertEquals("not invoked", false, invoked);
    }

    @Test
    public void testStateMachine5() throws java.io.IOException {
        SerialTrafficController c = (SerialTrafficController) tc;

        DataInputStream i = new DataInputStream(new ByteArrayInputStream(
                new byte[]{(byte) 129, (byte) 90, (byte) 129, (byte) 32}));


        c.doNextStep(new SerialReply(), i);

        JUnitAppender.assertWarnMessage("parity mismatch: 18, going to state 2 with content 129, 32");
        Assert.assertEquals("not invoked", false, invoked);
    }

    @Test
    public void testStateMachineOK1() throws java.io.IOException {
        SerialTrafficController c = (SerialTrafficController) tc;

        DataInputStream i = new DataInputStream(new ByteArrayInputStream(
                new byte[]{(byte) 129, (byte) 90, (byte) 129, (byte) 31}));

        c.doNextStep(new SerialReply(), i);

        Assert.assertEquals("invoked", true, invoked);
        Assert.assertEquals("byte 0", (byte) 129, testBuffer[0]);
        Assert.assertEquals("byte 1", (byte) 90, testBuffer[1]);
        Assert.assertEquals("byte 2", (byte) 129, testBuffer[2]);
        Assert.assertEquals("byte 3", (byte) 31, testBuffer[3]);
    }

    @Test
    public void testStateMachineOK2() throws java.io.IOException {
        SerialTrafficController c = (SerialTrafficController) tc;

        DataInputStream i = new DataInputStream(new ByteArrayInputStream(
                new byte[]{(byte) 0xE2, (byte) 119, (byte) 0xE2, (byte) 119}));

        c.doNextStep(new SerialReply(), i);

        Assert.assertEquals("invoked", true, invoked);
        Assert.assertEquals("byte 0", (byte) 0xE2, testBuffer[0]);
        Assert.assertEquals("byte 1", (byte) 119, testBuffer[1]);
        Assert.assertEquals("byte 2", (byte) 0xE2, testBuffer[2]);
        Assert.assertEquals("byte 3", (byte) 119, testBuffer[3]);
    }

    @Test
    public void testStateMachineOK3() throws java.io.IOException {
        SerialTrafficController c = (SerialTrafficController) tc;

        DataInputStream i = new DataInputStream(new ByteArrayInputStream(
                new byte[]{(byte) 0xE2, (byte) 13, (byte) 0xE2, (byte) 88}));

        c.doNextStep(new SerialReply(), i);

        Assert.assertEquals("invoked", true, invoked);
        Assert.assertEquals("byte 0", (byte) 0xE2, testBuffer[0]);
        Assert.assertEquals("byte 1", (byte) 13, testBuffer[1]);
        Assert.assertEquals("byte 2", (byte) 0xE2, testBuffer[2]);
        Assert.assertEquals("byte 3", (byte) 88, testBuffer[3]);
    }

    @Test
    public void testStateMachineOK4() throws java.io.IOException {
        SerialTrafficController c = (SerialTrafficController) tc;

        DataInputStream i = new DataInputStream(new ByteArrayInputStream(
                new byte[]{(byte) 0xE2, (byte) 14, (byte) 0xE2, (byte) 86}));

        c.doNextStep(new SerialReply(), i);

        Assert.assertEquals("invoked", true, invoked);
        Assert.assertEquals("byte 0", (byte) 0xE2, testBuffer[0]);
        Assert.assertEquals("byte 1", (byte) 14, testBuffer[1]);
        Assert.assertEquals("byte 2", (byte) 0xE2, testBuffer[2]);
        Assert.assertEquals("byte 3", (byte) 86, testBuffer[3]);
    }

    @Test
    public void testStateMachineOK5() throws java.io.IOException {
        SerialTrafficController c = (SerialTrafficController) tc;

        DataInputStream i = new DataInputStream(new ByteArrayInputStream(
                new byte[]{(byte) 0xE2, (byte) 15, (byte) 0xE2, (byte) 84}));

        c.doNextStep(new SerialReply(), i);

        Assert.assertEquals("invoked", true, invoked);
        Assert.assertEquals("byte 0", (byte) 0xE2, testBuffer[0]);
        Assert.assertEquals("byte 1", (byte) 15, testBuffer[1]);
        Assert.assertEquals("byte 2", (byte) 0xE2, testBuffer[2]);
        Assert.assertEquals("byte 3", (byte) 84, testBuffer[3]);
    }

    @Test
    public void testStateMachineRecover1() throws java.io.IOException {
        SerialTrafficController c = (SerialTrafficController) tc;

        DataInputStream i = new DataInputStream(new ByteArrayInputStream(
                new byte[]{12, (byte) 129, (byte) 90, (byte) 129, (byte) 31}));

        c.doNextStep(new SerialReply(), i);
        c.doNextStep(new SerialReply(), i);

        JUnitAppender.assertWarnMessage("1st byte not address: 12");
        Assert.assertEquals("invoked", true, invoked);
        Assert.assertEquals("byte 0", (byte) 129, testBuffer[0]);
        Assert.assertEquals("byte 1", (byte) 90, testBuffer[1]);
        Assert.assertEquals("byte 2", (byte) 129, testBuffer[2]);
        Assert.assertEquals("byte 3", (byte) 31, testBuffer[3]);
    }

    @Test
    public void testStateMachineRecover2() throws java.io.IOException {
        SerialTrafficController c = (SerialTrafficController) tc;

        DataInputStream i = new DataInputStream(new ByteArrayInputStream(
                new byte[]{(byte) 129, (byte) 129, (byte) 90, (byte) 129, (byte) 31}));

        c.doNextStep(new SerialReply(), i);
        c.doNextStep(new SerialReply(), i);

        JUnitAppender.assertWarnMessage("2nd byte HOB set: 129, going to state 1");
        Assert.assertEquals("invoked", true, invoked);
        Assert.assertEquals("byte 0", (byte) 129, testBuffer[0]);
        Assert.assertEquals("byte 1", (byte) 90, testBuffer[1]);
        Assert.assertEquals("byte 2", (byte) 129, testBuffer[2]);
        Assert.assertEquals("byte 3", (byte) 31, testBuffer[3]);
    }

    @Test
    public void testStateMachineRecover3() throws java.io.IOException {
        SerialTrafficController c = (SerialTrafficController) tc;

        DataInputStream i = new DataInputStream(new ByteArrayInputStream(
                new byte[]{(byte) 128, (byte) 12, (byte) 129, (byte) 90, (byte) 129, (byte) 31}));

        c.doNextStep(new SerialReply(), i);
        c.doNextStep(new SerialReply(), i);

        JUnitAppender.assertWarnMessage("addresses don't match: 128, 129. going to state 1");
        Assert.assertEquals("invoked", true, invoked);
        Assert.assertEquals("byte 0", (byte) 129, testBuffer[0]);
        Assert.assertEquals("byte 1", (byte) 90, testBuffer[1]);
        Assert.assertEquals("byte 2", (byte) 129, testBuffer[2]);
        Assert.assertEquals("byte 3", (byte) 31, testBuffer[3]);
    }

    @Test
    public void testStateMachineRecover4() throws java.io.IOException {
        SerialTrafficController c = (SerialTrafficController) tc;

        DataInputStream i = new DataInputStream(new ByteArrayInputStream(
                new byte[]{(byte) 129, (byte) 12, (byte) 129, (byte) 90, (byte) 129, (byte) 31}));

        c.doNextStep(new SerialReply(), i);
        c.doNextStep(new SerialReply(), i);

        JUnitAppender.assertWarnMessage("parity mismatch: 25, going to state 2 with content 129, 90");
        Assert.assertEquals("invoked", true, invoked);
        Assert.assertEquals("byte 0", (byte) 129, testBuffer[0]);
        Assert.assertEquals("byte 1", (byte) 90, testBuffer[1]);
        Assert.assertEquals("byte 2", (byte) 129, testBuffer[2]);
        Assert.assertEquals("byte 3", (byte) 31, testBuffer[3]);
    }

    @Test
    public void testSerialNodeEnumeration() {
        SerialTrafficController c = (SerialTrafficController) tc;
        SerialNode b = new SerialNode(1, SerialNode.NODE2002V6, c);
        SerialNode f = new SerialNode(3, SerialNode.NODE2002V1, c);
        SerialNode d = new SerialNode(2, SerialNode.NODE2002V1, c);
        SerialNode e = new SerialNode(6, SerialNode.NODE2002V6, c);
        Assert.assertEquals("1st Node", b, c.getNode(0));
        Assert.assertEquals("2nd Node", f, c.getNode(1));
        Assert.assertEquals("3rd Node", d, c.getNode(2));
        Assert.assertEquals("4th Node", e, c.getNode(3));
        Assert.assertEquals("no more Nodes", null, c.getNode(4));
        Assert.assertEquals("1st Node Again", b, c.getNode(0));
        Assert.assertEquals("2nd Node Again", f, c.getNode(1));
        Assert.assertEquals("node with address 6", e, c.getNodeFromAddress(6));
        Assert.assertEquals("3rd Node again", d, c.getNode(2));
        Assert.assertEquals("no node with address 0", null, c.getNodeFromAddress(0));
        c.deleteNode(6);
        Assert.assertEquals("1st Node after del", b, c.getNode(0));
        Assert.assertEquals("2nd Node after del", f, c.getNode(1));
        Assert.assertEquals("3rd Node after del", d, c.getNode(2));
        Assert.assertEquals("no more Nodes after del", null, c.getNode(3));
        c.deleteNode(1);
        JUnitAppender.assertWarnMessage("Deleting the serial node active in the polling loop");
        Assert.assertEquals("1st Node after del2", f, c.getNode(0));
        Assert.assertEquals("2nd Node after del2", d, c.getNode(1));
        Assert.assertEquals("no more Nodes after del2", null, c.getNode(2));
    }

    @Test
    public void testSerialOutput() {
        SerialTrafficController c = (SerialTrafficController) tc;
        SerialNode a = new SerialNode(c);
        SerialNode g = new SerialNode(5, SerialNode.NODE2002V1, c);
        Assert.assertTrue("must Send", g.mustSend());
        g.resetMustSend();
        Assert.assertNotNull("exists", a);
        Assert.assertTrue("must Send off", !(g.mustSend()));
        //c.setSerialOutput("GL5B2", false); // test and 12 year old method removed, called nowhere as of 4.9.4
        AbstractMRMessage m = g.createOutPacket();
        Assert.assertEquals("packet size", 4, m.getNumDataElements());
        Assert.assertEquals("node address", 5, m.getElement(0));
        Assert.assertEquals("packet type", 17, m.getElement(1));  // 'T'
    }

    @Test
    public void testListenerScaffolds() {

        SerialListenerScaffold s = new SerialListenerScaffold();
        Assertions.assertNull(rcvdReply);
        Assertions.assertNull(rcvdMsg);

        SerialMessage msg = new SerialMessage(new byte[]{0,1,2,3});
        s.message(msg);
        Assertions.assertEquals(msg, rcvdMsg);

        SerialReply reply = new SerialReply("0123");
        s.reply(reply);
        Assertions.assertEquals(reply, rcvdReply);

    }

    @Test
    public void testPortControllerScaffold() throws IOException {

        SerialPortControllerScaffold spcs = new SerialPortControllerScaffold(memo);
        Assertions.assertNotNull(spcs,"no exception");
        Assertions.assertNotNull(tostream);
        Assertions.assertNotNull(tistream);
        spcs.dispose();
        memo.dispose();
    }

    // internal class to simulate a Listener
    private class SerialListenerScaffold implements SerialListener {

        SerialListenerScaffold() {
            rcvdReply = null;
            rcvdMsg = null;
        }

        @Override
        public void message(SerialMessage m) {
            rcvdMsg = m;
        }

        @Override
        public void reply(SerialReply r) {
            rcvdReply = r;
        }
    }
    SerialReply rcvdReply;
    SerialMessage rcvdMsg;

    // internal class to simulate a PortController
    private class SerialPortControllerScaffold extends SerialPortController {

        protected SerialPortControllerScaffold(GrapevineSystemConnectionMemo memo) throws IOException {
            super(memo);
            PipedInputStream tempPipe;
            tempPipe = new PipedInputStream();
            tostream = new DataInputStream(tempPipe);
            ostream = new DataOutputStream(new PipedOutputStream(tempPipe));
            tempPipe = new PipedInputStream();
            istream = new DataInputStream(tempPipe);
            tistream = new DataOutputStream(new PipedOutputStream(tempPipe));
        }

        @Override
        public java.util.Vector<String> getPortNames() {
            return null;
        }

        @Override
        public String openPort(String portName, String appName) {
            return null;
        }

        @Override
        public void configure() {
        }

        @Override
        public String[] validBaudRates() {
            return new String[] {};
        }

        //@Override
        @Override
        public int[] validBaudNumbers() {
            return new int[] {};
        }

        // returns the InputStream from the port
        @Override
        public DataInputStream getInputStream() {
            return istream;
        }

        // returns the outputStream to the port
        @Override
        public DataOutputStream getOutputStream() {
            return ostream;
        }

        // check that this object is ready to operate
        @Override
        public boolean status() {
            return true;
        }
    }

    private DataOutputStream ostream;  // Traffic controller writes to this
    private DataInputStream tostream; // so we can read it from this

    private DataOutputStream tistream; // tests write to this
    private DataInputStream istream;  // so the traffic controller can read from this

    private GrapevineSystemConnectionMemo memo;

    private class SerialTrafficControllerImpl extends SerialTrafficController {

        SerialTrafficControllerImpl(GrapevineSystemConnectionMemo memo){
            super(memo);
        }

        @Override
        protected void loadBuffer(AbstractMRReply msg) {
            testBuffer[0] = buffer[0];
            testBuffer[1] = buffer[1];
            testBuffer[2] = buffer[2];
            testBuffer[3] = buffer[3];
            invoked = true;
        }

    }

    @Override
    @BeforeEach
    public void setUp() {
        JUnitUtil.setUp();
        testBuffer = new byte[4];
        invoked = false;
        memo = new GrapevineSystemConnectionMemo();
        tc = new SerialTrafficControllerImpl(memo);
    }

    @Override
    @AfterEach
    public void tearDown() {
        memo.dispose();
        JUnitUtil.clearShutDownManager(); // put in place because AbstractMRTrafficController implementing subclass was not terminated properly
        JUnitUtil.tearDown();

    }

    // private final static Logger log = LoggerFactory.getLogger(SerialTrafficControllerTest.class);

}
