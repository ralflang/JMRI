package jmri.jmrix.loconet;

import jmri.jmrix.loconet.SlotMapEntry.SlotType;
import jmri.util.JUnitAppender;
import jmri.util.JUnitUtil;

import org.junit.Assert;
import org.junit.jupiter.api.*;

public class LocoNetSlotTest {

    @Test
    public void testGetSlotSend() {
        SlotManager slotmanager = new SlotManager(lnis);
        SlotListener p2 = new SlotListener() {
            @Override
            public void notifyChangedSlot(LocoNetSlot l) {
            }
        };
        slotmanager.slotFromLocoAddress(21, p2);
        Assert.assertEquals("slot request message",
                "BF 00 15 00",
                lnis.outbound.elementAt(lnis.outbound.size() - 1).toString());
        slotmanager.dispose();
    }

    @Test
    public void testCTor() {
        LocoNetSlot t = new LocoNetSlot(5);
        Assert.assertNotNull("exists", t);
    }

    @Test
    public void testMessageCTor() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertNotNull("exists", t);
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testGetSlot() {
        LocoNetSlot t = new LocoNetSlot(5);
        Assert.assertEquals("slot number", 5, t.getSlot());
    }

    @Test
    public void testSetSlot() throws LocoNetException {
        int ia[] = {0xEF, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(1);

        boolean exceptionCaught = false;
        try {
            t.setSlot(lm); // we are checking to make sure this does not throw an
            // exception.
        }
        catch (LocoNetException e) {
            exceptionCaught = true;
        }
        Assert.assertFalse("do not expect an exception", exceptionCaught);
        Assert.assertEquals("slot is 1", 1, t.getSlot());
        Assert.assertEquals("Slot status is 0x30", 0x30, t.slotStatus());
        Assert.assertEquals("Slot decoder type is 0x03", 3, t.decoderType());
        Assert.assertEquals("Address is 5544", 5544, t.locoAddr());
        Assert.assertEquals("Slot speed is 0", 0, t.speed());
        Assert.assertEquals("Slot dirf is 0", 0, t.dirf());
        Assert.assertEquals("Slot trk is 0x47", 0x47, t.getTrackStatus());
        Assert.assertEquals("Slot status2 is 0", 0, t.ss2());
        Assert.assertEquals("slot consist status is 0", 0, t.consistStatus());
        Assert.assertEquals("Slot snd is 0", 0, t.snd());
        Assert.assertEquals("slot throttle id is 0", 0, t.id());

        ia[1] = 0x0f;
        lm = new LocoNetMessage(ia);
        try {
            t.setSlot(lm); // we are checking to make sure this does not throw an
            // exception.
        }
        catch (LocoNetException e) {
            exceptionCaught = true;
        }
        Assert.assertFalse("do not expect an exception", exceptionCaught);

        ia[1] = 0x0E;
        ia[2] = 3;
        lm = new LocoNetMessage(ia);
        try {
            t.setSlot(lm); // we are checking to make sure this does not throw an
            // exception.
        }
        catch (LocoNetException e) {
            exceptionCaught = true;
        }
        Assert.assertFalse("do not expect an exception", exceptionCaught);
        jmri.util.JUnitAppender.assertErrorMessage("Asked to handle message not for this slot (1) EF 0E 03 33 28 00 00 47 00 2B 00 00 00 60");

        ia[0] = 0xE7;
        ia[2] = 1;
        lm = new LocoNetMessage(ia);
        long lastTime = t.getLastUpdateTime();
        try {
            t.setSlot(lm); // we are checking to make sure this does not throw an
            // exception.
        }
        catch (LocoNetException e) {
            exceptionCaught = true;
        }
        Assert.assertFalse("do not expect an exception", exceptionCaught);
        Assert.assertNotEquals("update time was updated", lastTime, t.getLastUpdateTime());

        int ib[] = {0x81, 0x00};
        lm = new LocoNetMessage(ib);
        try {
            t.setSlot(lm); // we are checking to make sure this throws an
            // exception.
        }
        catch (LocoNetException e) {
            exceptionCaught = true;
        }
        Assert.assertTrue("do expect an exception", exceptionCaught);

        exceptionCaught = false;
        int ic[] = {0xb5, 0x01, 0x25, 0};
        lm = new LocoNetMessage(ic);
        try {
            t.setSlot(lm); // we are checking to make sure this does not throw an
            // exception.
        }
        catch (LocoNetException e) {
            exceptionCaught = true;
        }
        Assert.assertFalse("do not expect an exception", exceptionCaught);
        Assert.assertEquals("status updated", 0x20, t.slotStatus());

        int id[] = {0xa2, 0x01, 0x35, 0x00};
        lm = new LocoNetMessage(id);
        try {
            t.setSlot(lm); // we are checking to make sure this does not throw an
            // exception.
        }
        catch (LocoNetException e) {
            exceptionCaught = true;
        }
        Assert.assertFalse("do not expect an exception", exceptionCaught);
        Assert.assertEquals("F5-F8 updated", 5, t.snd());

        id[0] = 0xa0;
        id[2] = 0x7E;
        lm = new LocoNetMessage(id);
        try {
            t.setSlot(lm); // we are checking to make sure this does not throw an
            // exception.
        }
        catch (LocoNetException e) {
            exceptionCaught = true;
        }
        Assert.assertFalse("do not expect an exception", exceptionCaught);
        Assert.assertEquals("Spd updated", 0x7e, t.speed());

        id[0] = 0xa1;
        id[2] = 0x53;
        lm = new LocoNetMessage(id);
        try {
            t.setSlot(lm); // we are checking to make sure this does not throw an
            // exception.
        }
        catch (LocoNetException e) {
            exceptionCaught = true;
        }
        Assert.assertFalse("do not expect an exception", exceptionCaught);
        Assert.assertEquals("Dirf updated", 0x13, t.dirf());

        int ie[] = {0xb5, 0x01, 0x40, 0};
        LocoNetMessage lm2 = new LocoNetMessage(ie);
        t.setSlot(lm2);
        Assert.assertEquals("slot consist status is ", 0x40, t.consistStatus());

        id[2] = 0x08;
        lm = new LocoNetMessage(id);
        try {
            t.setSlot(lm); // we are checking to make sure this does not throw an
            // exception.
        }
        catch (LocoNetException e) {
            exceptionCaught = true;
        }
        Assert.assertFalse("do not expect an exception", exceptionCaught);
        Assert.assertEquals("Dirf updated", 0x08, t.dirf());

        id[2] = 0x37;
        lm = new LocoNetMessage(id);
        try {
            t.setSlot(lm); // we are checking to make sure this does not throw an
            // exception.
        }
        catch (LocoNetException e) {
            exceptionCaught = true;
        }
        Assert.assertFalse("do not expect an exception", exceptionCaught);
        Assert.assertEquals("Dirf updated", 0x17, t.dirf());

        ie[2] = 0x48;
        lm2 = new LocoNetMessage(ie);
        t.setSlot(lm2);
        Assert.assertEquals("slot consist status is ", 0x48, t.consistStatus());

        id[2] = 0x08;
        lm = new LocoNetMessage(id);
        try {
            t.setSlot(lm); // we are checking to make sure this does not throw an
            // exception.
        }
        catch (LocoNetException e) {
            exceptionCaught = true;
        }
        Assert.assertFalse("do not expect an exception", exceptionCaught);
        Assert.assertEquals("Dirf updated", 0x08, t.dirf());

        id[2] = 0x37;
        lm = new LocoNetMessage(id);
        try {
            t.setSlot(lm); // we are checking to make sure this does not throw an
            // exception.
        }
        catch (LocoNetException e) {
            exceptionCaught = true;
        }
        Assert.assertFalse("do not expect an exception", exceptionCaught);
        Assert.assertEquals("Dirf updated", 0x17, t.dirf());

        ie[2] = 0x08;
        lm2 = new LocoNetMessage(ie);
        t.setSlot(lm2);
        Assert.assertEquals("slot consist status is ", 0x08, t.consistStatus());

        id[2] = 0x08;
        lm = new LocoNetMessage(id);
        try {
            t.setSlot(lm); // we are checking to make sure this does not throw an
            // exception.
        }
        catch (LocoNetException e) {
            exceptionCaught = true;
        }
        Assert.assertFalse("do not expect an exception", exceptionCaught);
        Assert.assertEquals("Dirf updated", 0x08, t.dirf());

        id[2] = 0x37;
        lm = new LocoNetMessage(id);
        try {
            t.setSlot(lm); // we are checking to make sure this does not throw an
            // exception.
        }
        catch (LocoNetException e) {
            exceptionCaught = true;
        }
        Assert.assertFalse("do not expect an exception", exceptionCaught);
        Assert.assertEquals("Dirf updated", 0x37, t.dirf());

        ie[2] = 0x00;
        lm2 = new LocoNetMessage(ie);
        t.setSlot(lm2);
        Assert.assertEquals("slot consist status is ", 0x0, t.consistStatus());

        id[2] = 0x08;
        lm = new LocoNetMessage(id);
        try {
            t.setSlot(lm); // we are checking to make sure this does not throw an
            // exception.
        }
        catch (LocoNetException e) {
            exceptionCaught = true;
        }
        Assert.assertFalse("do not expect an exception", exceptionCaught);
        Assert.assertEquals("Dirf updated", 0x08, t.dirf());

        id[2] = 0x37;
        lm = new LocoNetMessage(id);
        try {
            t.setSlot(lm); // we are checking to make sure this does not throw an
            // exception.
        }
        catch (LocoNetException e) {
            exceptionCaught = true;
        }
        Assert.assertFalse("do not expect an exception", exceptionCaught);
        Assert.assertEquals("Dirf updated", 0x37, t.dirf());

    }

    @Test
    public void testDecoderType() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertEquals("decoder type", LnConstants.DEC_MODE_128, t.decoderType());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testSlotStatus() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertEquals("Slot Status", LnConstants.LOCO_IN_USE, t.slotStatus());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testss2() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertEquals("Slot Status", 0x00, t.ss2());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testConsistStatus() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertEquals("Consist Status", LnConstants.CONSIST_NO, t.consistStatus());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsForward() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertTrue("is Forward", t.isForward());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF0() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F0", t.isF0());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF1() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F1", t.isF1());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF2() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F2", t.isF2());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF3() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F3", t.isF3());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF4() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F4", t.isF4());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF5() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F5", t.isF5());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF6() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F6", t.isF6());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF7() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F7", t.isF7());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF8() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F8", t.isF8());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF9() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F9", t.isF9());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF10() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F10", t.isF10());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF11() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F11", t.isF11());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF12() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F12", t.isF12());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF13() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F13", t.isF13());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF14() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F14", t.isF14());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF15() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F15", t.isF15());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF16() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F16", t.isF16());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF17() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F17", t.isF17());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF18() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F18", t.isF18());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF19() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F19", t.isF19());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF20() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F20", t.isF20());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF21() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F21", t.isF21());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF22() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F22", t.isF22());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF23() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F23", t.isF23());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF24() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F24", t.isF24());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF25() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F25", t.isF25());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF26() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F26", t.isF26());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF27() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F27", t.isF27());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIsF28() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertFalse("is F28", t.isF28());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testIbIsF28() throws LocoNetException {
        int ia[] = {0xD4, 0x20, 0x01, 0x05, 0x40, 0x4F};
        LocoNetMessage lm = new LocoNetMessage(ia);
        // you can nolonger set up a slot from anything other than a slot message
        // as you have to know the protocol.
        // LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        LocoNetSlot t = new LocoNetSlot(2);
        t.setSlotType(SlotType.LOCO);
        t.setSlot(lm);
        Assert.assertTrue("is F28", t.isF28());
    }

    @Test
    public void testLocoAddr() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertEquals("address", 5544, t.locoAddr());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testSpeed() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertEquals("speed", 0, t.speed());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testDirf() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertEquals("directions and Functions", 0x00, t.dirf());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testSnd() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertEquals("snd", 0x00, t.snd());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testID() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        Assert.assertEquals("ID", 0x00, t.id());
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testWriteSlot() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x07,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        LocoNetMessage lm2 = t.writeSlot();
        Assert.assertEquals("Opcode", LnConstants.OPC_WR_SL_DATA, lm2.getOpCode());
        for (int i = 1; i <= 12; i++) {
            Assert.assertEquals("Element " + i, lm.getElement(i), lm2.getElement(i));
        }
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testExpWriteSlot() throws LocoNetException {
        int ia[] = {0xE6, 0x15, 0x01, 0x00, 0x03, 0x00, 0x02, 0x47,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        LocoNetMessage lm2 = t.writeSlot();
        Assert.assertEquals("Opcode", LnConstants.OPC_EXP_WR_SL_DATA, lm2.getOpCode());
        for (int i = 1; i <= 19; i++) {
            Assert.assertEquals("Element " + i, lm.getElement(i), lm2.getElement(i));
        }
        JUnitAppender.assertWarnMessage("Slot [128] not in map but reports loco, check command station type");
    }

    @Test
    public void testWriteThrottleID() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x07,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        LocoNetMessage lm2 = t.writeThrottleID(0x0171);
        Assert.assertEquals("Opcode", LnConstants.OPC_WR_SL_DATA, lm2.getOpCode());
        for (int i = 1; i <= 10; i++) {
            Assert.assertEquals("Element " + i, lm.getElement(i), lm2.getElement(i));
        }
        Assert.assertEquals("Element 11", 0x71, lm2.getElement(11));
        Assert.assertEquals("Element 12", 0x02, lm2.getElement(12));
        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testExpWriteThrottleID() throws LocoNetException {
        int ia[]={0xE6, 0x15, 0x01, 0x02, 0x03, 0x00, 0x02, 0x47,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x42, 0x4C, 0x49 };
        LocoNetMessage lm =new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(new LocoNetMessage(lm));
        LocoNetMessage lm2 = t.writeThrottleID(0x0171);
        Assert.assertEquals("Opcode",LnConstants.OPC_EXP_WR_SL_DATA,lm2.getOpCode());
        for(int i = 1;i<=17;i++){
            Assert.assertEquals("Element " + i,lm.getElement(i),lm2.getElement(i));
        }
        Assert.assertEquals("Element 18",0x71,lm2.getElement(18));
        Assert.assertEquals("Element 19",0x02,lm2.getElement(19));
        JUnitAppender.assertWarnMessage("Slot [130] not in map but reports loco, check command station type");
    }

    @Test
    public void testConsistingStateVsSpeedAccept() throws LocoNetException {
        int ia[] = {0xE7, 0x0E, 0x01, 0x33, 0x28, 0x00, 0x00, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};
        LocoNetMessage lm = new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(lm);
        Assert.assertEquals("Consist-mode is unconsisted", LnConstants.CONSIST_NO, t.consistStatus());
        Assert.assertEquals("Speed Set from slot read", 0, t.speed());
        int ib[] = {0xA0, 1, 14, 0};
        lm = new LocoNetMessage(ib);
        t.setSlot(lm);
        Assert.assertEquals("Speed Set for Unconsisted slot", 14, t.speed());
        int id[] = {0xA1, 1, 2, 0};
        lm = new LocoNetMessage(id);
        t.setSlot(lm);
        Assert.assertEquals("Function 1 set for Unconsisted slot", 2, t.dirf());
        id[2] = 0x20;
        lm = new LocoNetMessage(id);
        t.setSlot(lm);
        Assert.assertEquals("Change direction and F1 for unconsisted slot", 0x20, t.dirf());

        int ic[] = {0xE7, 0x0E, 0x01, 0x0b, 0x28, 0x12, 0x02, 0x47,
            0x00, 0x2B, 0x00, 0x00, 0x00, 0x60};   // make slot consist_top
        lm = new LocoNetMessage(ic);
        t.setSlot(lm);
        Assert.assertEquals("Consist-mode is consist-top", LnConstants.CONSIST_TOP, t.consistStatus());
        Assert.assertEquals("Speed Set for consist-top from slot read", 18, t.speed());
        Assert.assertEquals("OPC_LOCO_SPD from slot read for consist-top", 2, t.dirf());

        ib[2] = 3;
        lm = new LocoNetMessage(ib);
        t.setSlot(lm);
        Assert.assertEquals("OPC_LOCO_SPD accepted for consist-top", 3, t.speed());
        id[2] = 7;
        lm = new LocoNetMessage(id);
        t.setSlot(lm);
        Assert.assertEquals("Function F1-F3 set for consist-top slot", 7, t.dirf());
        id[2] = 0x22;
        lm = new LocoNetMessage(id);
        t.setSlot(lm);
        Assert.assertEquals("Change direction and F1 & F3 for consist-top slot", 0x22, t.dirf());

        ic[3] = 0x4b;   // make slot consist_mid, common
        lm = new LocoNetMessage(ic);
        t.setSlot(lm);
        Assert.assertEquals("Consist-mode is consist-mid", LnConstants.CONSIST_MID, t.consistStatus());
        Assert.assertEquals("'Speed' (slot pointer) set for consist-mid from slot read", 18, t.speed());
        ib[2] = 7;
        lm = new LocoNetMessage(ib);
        t.setSlot(lm);
        Assert.assertEquals("OPC_LOCO_SPD ignored when consist-mid", 18, t.speed());
        id[2] = 19;
        lm = new LocoNetMessage(id);
        t.setSlot(lm);
        Assert.assertEquals("Function F0, F2, F1 set for consist-mid slot", 19, t.dirf());
        id[2] = 0x27;
        lm = new LocoNetMessage(id);
        t.setSlot(lm);
        Assert.assertEquals("Change F0, F3 but NOT direction for consist-mid slot", 0x07, t.dirf());

        ic[3] = 0x43;   // make slot consist_sub, common
        ic[6] = 0x28;   // DIRF: reverse, F4 on
        lm = new LocoNetMessage(ic);
        t.setSlot(lm);
        Assert.assertEquals("Consist-mode is consist-sub", LnConstants.CONSIST_SUB, t.consistStatus());
        Assert.assertEquals("'Speed' (slot pointer) set for consist-sub from slot read", 18, t.speed());
        Assert.assertEquals("DIRF for consist-sub from slot read", 0x28, t.dirf());
        ib[2] = 9;
        lm = new LocoNetMessage(ib);
        t.setSlot(lm);
        Assert.assertEquals("OPC_LOCO_SPD ignored when consist-mid", 18, t.speed());
        id[2] = 0x3f;
        lm = new LocoNetMessage(id);
        t.setSlot(lm);
        Assert.assertEquals("Functions F0, F4-F1 set but not direction for consist-mid slot", 63, t.dirf());
        id[2] = 0x02;
        lm = new LocoNetMessage(id);
        t.setSlot(lm);
        Assert.assertEquals("Change F0, F4-F3, F1 for consist-top slot", 0x22, t.dirf());

        ic[6] = 0x27;   // make slot DIRF direction reversed, F3-F1 on
        lm = new LocoNetMessage(ic);
        t.setSlot(lm);
        Assert.assertEquals("Consist-mode is consist-sub", LnConstants.CONSIST_SUB, t.consistStatus());
        Assert.assertEquals("'Speed' (slot pointer) set for consist-sub from slot read", 18, t.speed());
        Assert.assertEquals("dirf is 0x27 from slot read", 0x27, t.dirf());
        ib[2] = 9;
        lm = new LocoNetMessage(ib);
        t.setSlot(lm);
        Assert.assertEquals("OPC_LOCO_SPD ignored when consist-mid", 18, t.speed());
        id[2] = 0x00;
        lm = new LocoNetMessage(id);
        t.setSlot(lm);
        Assert.assertEquals("Functions F0, F4-F1 set but not direction for consist-mid slot", 0x20, t.dirf());
        id[2] = 0x3F;
        lm = new LocoNetMessage(id);
        t.setSlot(lm);
        Assert.assertEquals("Change F0, F4-F1, for consist-sub slot", 0x3F, t.dirf());

        JUnitAppender.assertWarnMessage("Slot [1] not in map but reports loco, check command station type");
    }

    @Test
    public void testExpConsistingStateVsSpeedAccept() throws LocoNetException {
        int ia[]={0xE6, 0x15, 0x01, 0x02, 0x33, 0x28, 0x2B, 0x47,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1D, 0x43, 0x49 };
        LocoNetMessage lm =new LocoNetMessage(ia);
        LocoNetSlot t = new LocoNetSlot(lm);
        Assert.assertEquals("Consist-mode is unconsisted", LnConstants.CONSIST_NO, t.consistStatus());
        Assert.assertEquals("Speed Set from slot read",0, t.speed());
        int ib[] = {0xD5, 0x01, 0x02, 0x1C, 14, 0x30};  // set speed 14 using wrong throttle ID.
        lm = new LocoNetMessage(ib);
        t.setSlot(lm);
        Assert.assertEquals("Ignore speed from wrong throttle ID",0, t.speed());
        ib[3] = 0x1D;  // set speed 14 for correct throttle ID
        lm = new LocoNetMessage(ib);
        t.setSlot(lm);
        Assert.assertEquals("Speed Set for Unconsisted slot",14, t.speed());
        int id[] = {0xD5, 0x11, 0x02, 0x1C, 0x02, 0x23};  //function 2 on
        lm = new LocoNetMessage(id);
        t.setSlot(lm);
        Assert.assertEquals("Ignore Function 2 set for Unconsisted slot from wrong throttle",0, t.dirf());
        id[3] = 0x1D;  //function 2 on for correct throttle id
        lm = new LocoNetMessage(id);
        t.setSlot(lm);
        Assert.assertEquals("Function 2 set for Unconsisted slot",2, t.dirf());
        id[4] = 0x01; // modify message to F2 off F1 on
        lm = new LocoNetMessage(id);
        t.setSlot(lm);
        Assert.assertEquals("F1 ON for unconsisted slot", 0x01, t.dirf());
        ib[1] = 0x09;    // set speed/dir reverse
        ib[4] = 64;      // set speed 64
        lm = new LocoNetMessage(ib);
        t.setSlot(lm);
        Assert.assertEquals("Direction Reverse and F1 ON for unconsisted slot", 0x21, t.dirf());
        Assert.assertEquals("Speed Set for Unconsisted slot in reverse",64, t.speed());

        // Start of Top
        // set up slot 130, loco 5544, fwd 12, f2 on, top consist
        int ic[] = {0xE6, 0x15, 0x01, 0x03, 0x3B, 0x28, 0x2B, 0x47,
                0x0C, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1D, 0x42, 0x34};
        lm = new LocoNetMessage(ic);
        t.setSlot(lm);
        Assert.assertEquals("Consist-mode is consist-top", LnConstants.CONSIST_TOP, t.consistStatus());
        Assert.assertEquals("Speed Set for consist-top from slot read",12, t.speed());
        Assert.assertEquals("OPC_LOCO_SPD from slot read for consist-top",2, t.dirf());
        ib[1] = 0x01;    // set speed/dir fwd
        ib[4] = 3;      // set speed 3
        lm = new LocoNetMessage(ib);
        t.setSlot(lm);
        Assert.assertEquals("OPC_LOCO_SPD accepted for consist-top",3, t.speed());
        id[4] = 0x07;
        lm = new LocoNetMessage(id);
        t.setSlot(lm);
        Assert.assertEquals("Function F1-F3 set for consist-top slot",7, t.dirf());
        ib[1] = 0x09;    // set speed/dir rev
        ib[4] = 18;       // set speed 18
        lm = new LocoNetMessage(ib);
        t.setSlot(lm);
        id[4] = 0x05;    // F1 & F3 ON
        lm = new LocoNetMessage(id);
        t.setSlot(lm);
        Assert.assertEquals("Direction Rev and F1 & F3 for consist-top slot", 0x25, t.dirf());

        //Start of Mid Consist
        ic[4] = 0x4b;   // make slot consist_mid, common
        lm = new LocoNetMessage(ic); // also resets functions, speed etc
        t.setSlot(lm);
        Assert.assertEquals("Consist-mode is consist-mid", LnConstants.CONSIST_MID, t.consistStatus());
        Assert.assertEquals("'Speed' (slot pointer) set for consist-mid from slot read",12, t.speed());
        ib[4] = 50;      // speed 50
        ib[2] = 0x09;    // Direction fwd
        lm = new LocoNetMessage(ib);
        t.setSlot(lm);
        Assert.assertEquals("Speed ignored when consist-mid",12, t.speed());
        Assert.assertEquals("Direction ignored for consist-mid slot", 0x02, t.dirf());
        id[4] = 0x13; // F0,F1, F2 on
        lm = new LocoNetMessage(id);
        t.setSlot(lm);
        Assert.assertEquals("Function F0, F2, F1 set for consist-mid slot",19, t.dirf());
        id[4] = 0x07;  //F1 F2 F3 on
        lm = new LocoNetMessage(id);
        t.setSlot(lm);
        Assert.assertEquals("Function F0 OFF, F3 OFF but NOT direction for consist-mid slot", 0x07, t.dirf());

        // Start of Sub Consist
        ic[4] = 0x43;   // make slot consist_sub, common
        ic[10] = 0x28;   // DIRF: reverse, F4 on
        lm = new LocoNetMessage(ic);
        t.setSlot(lm);   // resets everything else
        Assert.assertEquals("Consist-mode is consist-sub", LnConstants.CONSIST_SUB, t.consistStatus());
        Assert.assertEquals("'Speed' (slot pointer) set for consist-sub from slot read",12, t.speed());
        Assert.assertEquals("DIRF for consist-sub from slot read", 0x28, t.dirf());
        ib[4] = 50;      // speed 50
        ib[2] = 0x09;    // Direction fwd
        lm = new LocoNetMessage(ib);
        t.setSlot(lm);
        Assert.assertEquals("Speed ignored when consist-sub",12, t.speed());
        Assert.assertEquals("Direction ignored for consist-sub slot", 0x28, t.dirf());
        id[4] = 0x1f;
        lm = new LocoNetMessage(id);
        t.setSlot(lm);
        Assert.assertEquals("Functions F0, F4-F1 set but not direction for consist-sub slot",63, t.dirf());
        id[4] = 0x02;
        lm = new LocoNetMessage(id);
        t.setSlot(lm);
        Assert.assertEquals("Change F0, F4-F3, F1 for consist-sub slot", 0x22, t.dirf());

        JUnitAppender.assertWarnMessage("Slot [130] not in map but reports loco, check command station type");
    }

    @Test
    public void checkFunctionMessage() {
        LocoNetSlot s = new LocoNetSlot(15);
        Assert.assertEquals("initial slot function value - F9", false, s.localF9);
        Assert.assertEquals("initial slot function value - F10", false, s.localF10);
        Assert.assertEquals("initial slot function value - F11", false, s.localF11);
        Assert.assertEquals("initial slot function value - F12", false, s.localF12);
        s.functionMessage(0xA1L);
        Assert.assertEquals("F9 now", true, s.localF9);
        s.functionMessage(0xA0L);
        Assert.assertEquals("F9 now", false, s.localF9);
        s.functionMessage(0xA2L);
        Assert.assertEquals("F10 now", true, s.localF10);
        s.functionMessage(0xA0L);
        Assert.assertEquals("F10 now", false, s.localF10);
        s.functionMessage(0xA4L);
        Assert.assertEquals("F11 now", true, s.localF11);
        s.functionMessage(0xA0L);
        Assert.assertEquals("F11 now", false, s.localF11);
        s.functionMessage(0xA8L);
        Assert.assertEquals("F12 now", true, s.localF12);
        s.functionMessage(0xA0L);
        Assert.assertEquals("F12 now", false, s.localF12);

        Assert.assertEquals("initial slot function value - F13", false, s.localF13);
        Assert.assertEquals("initial slot function value - F14", false, s.localF14);
        Assert.assertEquals("initial slot function value - F15", false, s.localF15);
        Assert.assertEquals("initial slot function value - F16", false, s.localF16);
        Assert.assertEquals("initial slot function value - F17", false, s.localF17);
        Assert.assertEquals("initial slot function value - F18", false, s.localF18);
        Assert.assertEquals("initial slot function value - F19", false, s.localF19);
        Assert.assertEquals("initial slot function value - F20", false, s.localF20);
        s.functionMessage(0xDE01L);
        Assert.assertEquals("F13 now", true, s.localF13);
        s.functionMessage(0xDE00L);
        Assert.assertEquals("F13 now", false, s.localF13);
        s.functionMessage(0xDE02L);
        Assert.assertEquals("F14 now", true, s.localF14);
        s.functionMessage(0xDE00L);
        Assert.assertEquals("F14 now", false, s.localF14);
        s.functionMessage(0xDE04L);
        Assert.assertEquals("F15 now", true, s.localF15);
        s.functionMessage(0xDE00L);
        Assert.assertEquals("F15 now", false, s.localF15);
        s.functionMessage(0xDE08L);
        Assert.assertEquals("F16 now", true, s.localF16);
        s.functionMessage(0xDE00L);
        Assert.assertEquals("F16 now", false, s.localF16);
        s.functionMessage(0xDE10L);
        Assert.assertEquals("F17 now", true, s.localF17);
        s.functionMessage(0xDE00L);
        Assert.assertEquals("F17 now", false, s.localF17);
        s.functionMessage(0xDE20L);
        Assert.assertEquals("F18 now", true, s.localF18);
        s.functionMessage(0xDE00L);
        Assert.assertEquals("F18 now", false, s.localF18);
        s.functionMessage(0xDE40L);
        Assert.assertEquals("F19 now", true, s.localF19);
        s.functionMessage(0xDE00L);
        Assert.assertEquals("F19 now", false, s.localF19);
        s.functionMessage(0xDE80L);
        Assert.assertEquals("F20 now", true, s.localF20);
        s.functionMessage(0xDE00L);
        Assert.assertEquals("F20 now", false, s.localF20);
        s.functionMessage(0XDF01L);
        Assert.assertEquals("F21 now", true, s.localF21);
        s.functionMessage(0XDF00L);
        Assert.assertEquals("F21 now", false, s.localF21);
        s.functionMessage(0XDF02L);
        Assert.assertEquals("F22 now", true, s.localF22);
        s.functionMessage(0XDF00L);
        Assert.assertEquals("F22 now", false, s.localF22);
        s.functionMessage(0XDF04L);
        Assert.assertEquals("F23 now", true, s.localF23);
        s.functionMessage(0XDF00L);
        Assert.assertEquals("F23 now", false, s.localF23);
        s.functionMessage(0XDF08L);
        Assert.assertEquals("F24 now", true, s.localF24);
        s.functionMessage(0XDF00L);
        Assert.assertEquals("F24 now", false, s.localF24);
        s.functionMessage(0XDF10L);
        Assert.assertEquals("F25 now", true, s.localF25);
        s.functionMessage(0XDF00L);
        Assert.assertEquals("F25 now", false, s.localF25);
        s.functionMessage(0XDF20L);
        Assert.assertEquals("F26 now", true, s.localF26);
        s.functionMessage(0XDF00L);
        Assert.assertEquals("F26 now", false, s.localF26);
        s.functionMessage(0XDF40L);
        Assert.assertEquals("F27 now", true, s.localF27);
        s.functionMessage(0XDF00L);
        Assert.assertEquals("F27 now", false, s.localF27);
        s.functionMessage(0XDF80L);
        Assert.assertEquals("F28 now", true, s.localF28);
        s.functionMessage(0XDF00L);
        Assert.assertEquals("F28 now", false, s.localF28);
    }

    @Test
    public void checkExpFunctionMessage()  throws LocoNetException {
        LocoNetSlot s = new LocoNetSlot(130); //will have default throttle id = 0
        Assert.assertEquals("initial slot function value - F0", false, s.isF0());
        Assert.assertEquals("initial slot function value - F1", false, s.isF1());
        Assert.assertEquals("initial slot function value - F2", false, s.isF2());
        Assert.assertEquals("initial slot function value - F3", false, s.isF3());
        Assert.assertEquals("initial slot function value - F4", false, s.isF4());
        Assert.assertEquals("initial slot function value - F5", false, s.isF5());
        Assert.assertEquals("initial slot function value - F6", false, s.isF6());
        Assert.assertEquals("initial slot function value - F7", false, s.isF7());
        Assert.assertEquals("initial slot function value - F8", false, s.isF8());
        Assert.assertEquals("initial slot function value - F9", false, s.isF9());
        Assert.assertEquals("initial slot function value - F10", false, s.isF10());
        Assert.assertEquals("initial slot function value - F11", false, s.isF11());
        Assert.assertEquals("initial slot function value - F12", false, s.isF12());
        Assert.assertEquals("initial slot function value - F13", false, s.isF13());
        Assert.assertEquals("initial slot function value - F14", false, s.isF14());
        Assert.assertEquals("initial slot function value - F15", false, s.isF15());
        Assert.assertEquals("initial slot function value - F16", false, s.isF16());
        Assert.assertEquals("initial slot function value - F17", false, s.isF17());
        Assert.assertEquals("initial slot function value - F18", false, s.isF18());
        Assert.assertEquals("initial slot function value - F19", false, s.isF19());
        Assert.assertEquals("initial slot function value - F20", false, s.isF20());
        Assert.assertEquals("initial slot function value - F21", false, s.isF21());
        Assert.assertEquals("initial slot function value - F22", false, s.isF22());
        Assert.assertEquals("initial slot function value - F23", false, s.isF23());
        Assert.assertEquals("initial slot function value - F24", false, s.isF24());
        Assert.assertEquals("initial slot function value - F25", false, s.isF25());
        Assert.assertEquals("initial slot function value - F26", false, s.isF26());
        Assert.assertEquals("initial slot function value - F27", false, s.isF27());
        Assert.assertEquals("initial slot function value - F28", false, s.isF28());
        int funcF0F06[] = {0xD5, 0x11, 0x02, 0x00, 0x10, 0x21}; // F0 On
        LocoNetMessage lM = new LocoNetMessage(funcF0F06);
        s.setSlot(lM);
        Assert.assertEquals("initial slot function value - F0", true, s.isF0());
        funcF0F06[4] =  0x5A; // ON F0,2,4, 6
        lM = new LocoNetMessage(funcF0F06);
        s.setSlot(lM);
        Assert.assertEquals("F0 Now", true, s.isF0());
        Assert.assertEquals("F1 Now", false, s.isF1());
        Assert.assertEquals("F2 Now", true, s.isF2());
        Assert.assertEquals("F3 Now", false, s.isF3());
        Assert.assertEquals("F4 Now", true, s.isF4());
        Assert.assertEquals("F5 Now", false, s.isF5());
        Assert.assertEquals("F6 Now", true, s.isF6());
        funcF0F06[4] =  0x25; // ON F1,3,5
        lM = new LocoNetMessage(funcF0F06);
        s.setSlot(lM);
        Assert.assertEquals("F0 Now", false, s.isF0());
        Assert.assertEquals("F1 Now", true, s.isF1());
        Assert.assertEquals("F2 Now", false, s.isF2());
        Assert.assertEquals("F3 Now", true, s.isF3());
        Assert.assertEquals("F4 Now", false, s.isF4());
        Assert.assertEquals("F5 Now", true, s.isF5());
        Assert.assertEquals("F6 Now", false, s.isF6());
        int funcF7F13[] = {0xD5, 0x19, 0x03, 0x00, 0x55, 0x21}; // F7,9,11,13 ON
        lM = new LocoNetMessage(funcF7F13);
        s.setSlot(lM);
        Assert.assertEquals("F7 Now", true, s.isF7());
        Assert.assertEquals("F8 Now", false, s.isF8());
        Assert.assertEquals("F9 Now", true, s.isF9());
        Assert.assertEquals("F10 Now", false, s.isF10());
        Assert.assertEquals("F11 Now", true, s.isF11());
        Assert.assertEquals("F12 Now", false, s.isF12());
        Assert.assertEquals("F13 Now", true, s.isF13());
        funcF7F13[4] =  0x2A; // ON F1,3,5
        lM = new LocoNetMessage(funcF7F13);
        s.setSlot(lM);
        Assert.assertEquals("F7 Now", false, s.isF7());
        Assert.assertEquals("F8 Now", true, s.isF8());
        Assert.assertEquals("F9 Now", false, s.isF9());
        Assert.assertEquals("F10 Now", true, s.isF10());
        Assert.assertEquals("F11 Now", false, s.isF11());
        Assert.assertEquals("F12 Now", true, s.isF12());
        Assert.assertEquals("F13 Now", false, s.isF13());
        int funcF14F20[] = {0xD5, 0x21, 0x03, 0x00, 0x55, 0x21}; // F7,9,11,13 ON
        lM = new LocoNetMessage(funcF14F20);
        s.setSlot(lM);
        Assert.assertEquals("F14 Now", true, s.isF14());
        Assert.assertEquals("F15 Now", false, s.isF15());
        Assert.assertEquals("F16 Now", true, s.isF16());
        Assert.assertEquals("F17 Now", false, s.isF17());
        Assert.assertEquals("F18 Now", true, s.isF18());
        Assert.assertEquals("F19 Now", false, s.isF19());
        Assert.assertEquals("F20 Now", true, s.isF20());
        funcF14F20[4] =  0x2A; // ON F1,3,5
        lM = new LocoNetMessage(funcF14F20);
        s.setSlot(lM);
        Assert.assertEquals("F14 Now", false, s.isF14());
        Assert.assertEquals("F15 Now", true, s.isF15());
        Assert.assertEquals("F16 Now", false, s.isF16());
        Assert.assertEquals("F17 Now", true, s.isF17());
        Assert.assertEquals("F18 Now", false, s.isF18());
        Assert.assertEquals("F19 Now", true, s.isF19());
        Assert.assertEquals("F20 Now", false, s.isF20());
        int funcF2128[] = {0xD5, 0x29, 0x03, 0x00, 0x55, 0x21}; // F7,9,11,13 ON
        lM = new LocoNetMessage(funcF2128);
        s.setSlot(lM);
        Assert.assertEquals("F21 Now", true, s.isF21());
        Assert.assertEquals("F22 Now", false, s.isF22());
        Assert.assertEquals("F23 Now", true, s.isF23());
        Assert.assertEquals("F24 Now", false, s.isF24());
        Assert.assertEquals("F25 Now", true, s.isF25());
        Assert.assertEquals("F26 Now", false, s.isF26());
        Assert.assertEquals("F27 Now", true, s.isF27());
        Assert.assertEquals("F28 Now", false, s.isF28());
        funcF2128[1] =  0x31; // ON F1,3,5
        funcF2128[4] =  0x2A; // ON F1,3,5
        lM = new LocoNetMessage(funcF2128);
        s.setSlot(lM);
        Assert.assertEquals("F21 Now", false, s.isF21());
        Assert.assertEquals("F22 Now", true, s.isF22());
        Assert.assertEquals("F23 Now", false, s.isF23());
        Assert.assertEquals("F24 Now", true, s.isF24());
        Assert.assertEquals("F25 Now", false, s.isF25());
        Assert.assertEquals("F26 Now", true, s.isF26());
        Assert.assertEquals("F27 Now", false, s.isF27());
        Assert.assertEquals("F28 Now", true, s.isF28());
    }

    @Test
    public void checkFastClockGetSetMethods() {
        LocoNetSlot s = new LocoNetSlot(15);
        s.setFcFracMins(12);
        jmri.util.JUnitAppender.assertErrorMessage("setFcFracMins invalid for slot 15");
        s.setFcHours(1);
        jmri.util.JUnitAppender.assertErrorMessage("setFcHours invalid for slot 15");
        s.setFcMinutes(12);
        jmri.util.JUnitAppender.assertErrorMessage("setFcMinutes invalid for slot 15");
        s.setFcDays(5);
        jmri.util.JUnitAppender.assertErrorMessage("setFcDays invalid for slot 15");
        s.setFcRate(0);
        jmri.util.JUnitAppender.assertErrorMessage("setFcRate invalid for slot 15");

        s.getFcFracMins();
        jmri.util.JUnitAppender.assertErrorMessage("getFcFracMins invalid for slot 15");
        s.getFcHours();
        jmri.util.JUnitAppender.assertErrorMessage("getFcHours invalid for slot 15");
        s.getFcMinutes();
        jmri.util.JUnitAppender.assertErrorMessage("getFcMinutes invalid for slot 15");
        s.getFcDays();
        jmri.util.JUnitAppender.assertErrorMessage("getFcDays invalid for slot 15");
        s.getFcRate();
        jmri.util.JUnitAppender.assertErrorMessage("getFcRate invalid for slot 15");

        s = new LocoNetSlot(123);
        Assert.assertEquals("FcFracMins initial value", 0x0, s.getFcFracMins());
        Assert.assertEquals("FcMinutes initial value", 53, s.getFcMinutes());
        Assert.assertEquals("FcHours initial value", 0, s.getFcHours());
        Assert.assertEquals("FcDays initial value", 0, s.getFcDays());
        s.setFcFracMins(18);
        s.setFcMinutes(41);
        s.setFcHours(2);
        s.setFcDays(3);
        Assert.assertEquals("getFcFracMins", 18, s.getFcFracMins());
        Assert.assertEquals("getFcMinutes", 41, s.getFcMinutes());
        Assert.assertEquals("getFcHours", 2, s.getFcHours());
        Assert.assertEquals("getFcDays", 3, s.getFcDays());
    }

    @Test
    public void checkSetAndGetTrackStatus() {
        LocoNetSlot s = new LocoNetSlot(19);
        Assert.assertEquals("Checking default track status", 7, s.getTrackStatus());
        for (int i = 0; i < 256; ++i) {
            s.setTrackStatus(i);
            Assert.assertEquals("checking set/get track status for status " + i, i, s.getTrackStatus());
        }
    }

    @Test
    public void checkIsF0ToF8() {
        SlotManager sm;
        LocoNetSystemConnectionMemo memo;

        sm = new SlotManager(lnis);
        memo = new LocoNetSystemConnectionMemo(lnis, sm);
        sm.setSystemConnectionMemo(memo);

        LocoNetSlot s = new LocoNetSlot(10);
        Assert.assertEquals("slot number assigned correctly", 10, s.getSlot());
        LocoNetMessage m = new LocoNetMessage(14);

        m.setOpCode(0xef);
        m.setElement(1, 0x0e);
        m.setElement(2, 0x0A);
        m.setElement(3, 0x00);
        m.setElement(4, 0x00);
        m.setElement(5, 0x00);
        m.setElement(7, 0x00);
        m.setElement(8, 0x00);
        m.setElement(9, 0x00);
        m.setElement(10, 0x00);
        m.setElement(11, 0x00);
        m.setElement(12, 0x00);
        m.setElement(13, 0x00);
        for (int i = 0; i < 128; ++i) {
            m.setElement(6, i);
            try {
                s.setSlot(m);
            }
            catch (LocoNetException e) {
                Assert.fail("unexpected exception " + e);
            }
            Assert.assertEquals("F0 value from LocoNet Message, loop " + i, ((i & 0x10) == 0x10), s.isF0());
            Assert.assertEquals("F1 value from LocoNet Message, loop " + i, ((i & 0x01) == 0x01), s.isF1());
            Assert.assertEquals("F2 value from LocoNet Message, loop " + i, ((i & 0x02) == 0x02), s.isF2());
            Assert.assertEquals("F3 value from LocoNet Message, loop " + i, ((i & 0x04) == 0x04), s.isF3());
            Assert.assertEquals("F4 value from LocoNet Message, loop " + i, ((i & 0x08) == 0x08), s.isF4());
            Assert.assertFalse("F5 value from LocoNet Message, loop " + i, s.isF5());
            Assert.assertFalse("F6 value from LocoNet Message, loop " + i, s.isF6());
            Assert.assertFalse("F7 value from LocoNet Message, loop " + i, s.isF7());
            Assert.assertFalse("F8 value from LocoNet Message, loop " + i, s.isF8());
            Assert.assertEquals("Dir value from LocoNet Message, loop " + 1, ((i & 0x20) == 0x00), s.isForward());
        }
        m.setElement(6, 0);
        for (int i = 0; i < 128; ++i) {
            m.setElement(10, i);
            try {
                s.setSlot(m);
            }
            catch (LocoNetException e) {
                Assert.fail("unexpected exception " + e);
            }
            Assert.assertFalse("F0 value from LocoNet Message, loop " + i, s.isF0());
            Assert.assertFalse("F1 value from LocoNet Message, loop " + i, s.isF1());
            Assert.assertFalse("F2 value from LocoNet Message, loop " + i, s.isF2());
            Assert.assertFalse("F3 value from LocoNet Message, loop " + i, s.isF3());
            Assert.assertFalse("F4 value from LocoNet Message, loop " + i, s.isF4());
            Assert.assertEquals("F5 value from LocoNet Message, loop " + i, ((i & 0x01) == 0x01), s.isF5());
            Assert.assertEquals("F6 value from LocoNet Message, loop " + i, ((i & 0x02) == 0x02), s.isF6());
            Assert.assertEquals("F7 value from LocoNet Message, loop " + i, ((i & 0x04) == 0x04), s.isF7());
            Assert.assertEquals("F8 value from LocoNet Message, loop " + i, ((i & 0x08) == 0x08), s.isF8());
            Assert.assertTrue("Dir value from LocoNet Message, loop " + 1, s.isForward());
        }
        sm.dispose();
    }

    LocoNetInterfaceScaffold lnis;

    @BeforeEach
    public void setUp() {
        JUnitUtil.setUp();
        // prepare an interface
        lnis = new LocoNetInterfaceScaffold();
    }

    @AfterEach
    public void tearDown() {
        JUnitUtil.tearDown();
    }

}
