package jmri.jmrix.marklin.mcan.cs1;

import jmri.jmrix.marklin.mcan.FrameProtocol;
import jmri.jmrix.marklin.mcan.classify.ProtocolClassifier;
import jmri.util.JUnitUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Documented CS1 layouts decoded through the public protocol classifier. */
class Cs1FrameParserTest {

    @BeforeEach
    void setUp() { JUnitUtil.setUp(); }

    @AfterEach
    void tearDown() { JUnitUtil.tearDown(); }

    private static int[] raw(long canId, int... data) {
        int[] result = new int[13];
        result[0] = (int) (canId >> 24) & 0xFF;
        result[1] = (int) (canId >> 16) & 0xFF;
        result[2] = (int) (canId >> 8) & 0xFF;
        result[3] = (int) canId & 0xFF;
        result[4] = data.length;
        System.arraycopy(data, 0, result, 5, data.length);
        return result;
    }

    private static long objectId(int priority, int command, boolean master) {
        return ((long) priority << 26) | (0x1234L << 10) | (command << 7)
                | (master ? 42 : 43);
    }

    private static long registrationId(int stage, int uidByte, boolean master) {
        return 0x1C000000L | ((long) uidByte << 18) | (stage << 15)
                | (12 << 10) | (master ? 0xFE : 0x7E);
    }

    private static long firmwareId(int handle, int request, int stage, int flags) {
        return 0x1C000000L | (handle << 18) | (request << 15)
                | (stage << 10) | (flags << 7) | 127;
    }

    private static <T extends Cs1Frame> T parse(Class<T> type, long id, int... data) {
        int[] bytes = raw(id, data);
        T result = assertInstanceOf(type, ProtocolClassifier.classify(bytes));
        assertEquals(FrameProtocol.CS1_NATIVE, result.protocol());
        assertArrayEquals(bytes, result.raw().toArray());
        assertEquals(data.length, result.dlc());
        return result;
    }

    @Test
    void registrationUsesItsOwnHeaderAndSeparatesMasterAndSlaveIdentity() {
        for (int stage = 0; stage < 4; stage++) {
            Cs1ConfigCommands.UidFragmentFrame fragment = parse(
                    Cs1ConfigCommands.UidFragmentFrame.class, registrationId(stage, 0xAB, false));
            assertEquals(stage, fragment.stage());
            assertEquals(0xAB, fragment.uidFragment());
            assertEquals(12, fragment.modifierId());
            assertEquals(126, fragment.node());
        }
        Cs1ConfigCommands.MasterIdentityReplyFrame master = parse(
                Cs1ConfigCommands.MasterIdentityReplyFrame.class, registrationId(0, 0, true),
                0xFE, 0xDC, 0xBA, 0x98, 0, 1, 0, 1);
        assertEquals(0xFEDCBA98L, master.masterUid());
        assertEquals(1, master.monitorHandle());

        Cs1ConfigCommands.RegistrationReplyFrame reply = parse(
                Cs1ConfigCommands.RegistrationReplyFrame.class, registrationId(1, 2, true),
                0, 0, 0, 0x97, 0, 0, 1, 3);
        assertEquals(0x97, reply.registrationData()[3]);

        Cs1ConfigCommands.RegistrationUidFrame slave = parse(
                Cs1ConfigCommands.RegistrationUidFrame.class, registrationId(4, 0, false),
                0x80, 0x12, 0x34, 0x56, 0, 2, 0, 3);
        assertEquals(0x80123456L, slave.slaveUid());
        parse(Cs1ConfigCommands.RegistrationDataFrame.class, registrationId(5, 0, false),
                0, 0, 0, 0x97, 1, 0, 0x40, 0x10);

        Cs1ConfigCommands.NodeAssignmentFrame assignment = parse(
                Cs1ConfigCommands.NodeAssignmentFrame.class, registrationId(7, 0, true),
                0, 2, 4, 0xB3, 0x12, 0x34, 0xAB, 1);
        assertEquals(0x000204B3L, assignment.slaveUid());
        assertEquals(0x1234, assignment.initialObjectHandle());
        assertEquals(43, assignment.assignedNode());
        assertTrue(assignment.multiSlave());
        assertTrue(assignment.flashRequested());
    }

    @Test
    void keepAliveCanBeInitiatedByEitherSide() {
        parse(Cs1SystemCommands.BusProbeFrame.class, 0x380, 0, 0, 0, 0, 0, 0, 0, 0);
        for (boolean master : new boolean[]{false, true}) {
            Cs1SystemCommands.KeepAliveFrame poll = parse(
                    Cs1SystemCommands.KeepAliveFrame.class, objectId(3, 7, master),
                    0, 0, 0, 42);
            assertEquals(42, poll.targetNode());
            assertEquals(master, poll.fromMaster());
            assertEquals(0x1234, poll.objectHandle());
            assertEquals(7, poll.commandBits());
            Cs1SystemCommands.KeepAliveReplyFrame reply = parse(
                    Cs1SystemCommands.KeepAliveReplyFrame.class, objectId(3, 7, !master),
                    0, 0, 0, master ? 0 : 42, 0xFE, 0x12, 0x34, 0x56);
            assertEquals(0xFE123456L, reply.deviceUid());
        }
    }

    @Test
    void descriptionAndSystemHandlesAreNotAssumedFixed() {
        Cs1ObjectCommands.SlaveDescriptionQueryFrame query = parse(
                Cs1ObjectCommands.SlaveDescriptionQueryFrame.class, objectId(6, 4, false),
                0x40, 42);
        assertEquals(42, query.slaveNode());
        Cs1ObjectCommands.SlaveDescriptionReplyFrame reply = parse(
                Cs1ObjectCommands.SlaveDescriptionReplyFrame.class, objectId(6, 4, true),
                0x40, 42, 0, 0, 0xAB, 0xCD, 0, 1);
        assertEquals(0xABCD, reply.descriptionHandle());
        parse(Cs1ObjectCommands.SystemHandleQueryFrame.class, objectId(6, 3, false), 3, 0);
        Cs1ObjectCommands.SystemHandleReplyFrame system = parse(
                Cs1ObjectCommands.SystemHandleReplyFrame.class, objectId(6, 3, true),
                0x40, 42, 0, 0, 0x43, 0x21, 0, 1);
        assertEquals(0x4321, system.systemHandle());
        Cs1ObjectCommands.SystemHandleReplyFrame trace = parse(
                Cs1ObjectCommands.SystemHandleReplyFrame.class, 0x180009AAL,
                3, 0, 0, 0, 0, 3, 0, 1);
        assertEquals(3, trace.systemHandle());
    }

    @Test
    void namesRemainNeutralBetweenRepliesAndRenameOperations() {
        parse(Cs1ObjectCommands.StackSizeQueryFrame.class, objectId(6, 1, true),
                0x41, 0, 0, 0);
        Cs1ObjectCommands.StackSizeReplyFrame size = parse(
                Cs1ObjectCommands.StackSizeReplyFrame.class, objectId(6, 1, false),
                0x41, 0, 0, 0, 0, 10);
        assertEquals(10, size.stackSize());
        for (boolean master : new boolean[]{false, true}) {
            Cs1ObjectCommands.NameQueryFrame query = parse(
                    Cs1ObjectCommands.NameQueryFrame.class, objectId(6, 1, master),
                    2, 0, 0, 12);
            assertEquals(12, query.offset());
            Cs1ObjectCommands.NameFragmentFrame fragment = parse(
                    Cs1ObjectCommands.NameFragmentFrame.class, objectId(6, 1, master),
                    2, 0, 0, 12, 'O', 'N', 0);
            assertEquals(12, fragment.offset());
            assertArrayEquals(new int[]{'O', 'N', 0}, fragment.nameBytes());
            int[] copy = fragment.nameBytes();
            copy[0] = 0;
            assertEquals('O', fragment.nameBytes()[0]);
        }
    }

    @Test
    void indexedTrafficDoesNotGuessSystemVersusLocoHandles() {
        Cs1ObjectCommands.IndexedQueryFrame query = parse(
                Cs1ObjectCommands.IndexedQueryFrame.class, objectId(6, 2, false), 1, 1);
        assertTrue(query.requestsDescription());
        parse(Cs1ObjectCommands.IndexedQueryFrame.class, objectId(6, 2, false), 1, 0);
        parse(Cs1ObjectCommands.IndexedListEndFrame.class, objectId(6, 2, true), 5, 1);
        Cs1ObjectCommands.IndexedTypeReplyFrame type = parse(
                Cs1ObjectCommands.IndexedTypeReplyFrame.class, objectId(6, 2, true), 2, 1, 4);
        assertEquals(4, type.type());
        Cs1ObjectCommands.FunctionDescriptionReplyFrame function = parse(
                Cs1ObjectCommands.FunctionDescriptionReplyFrame.class, objectId(6, 2, true),
                3, 1, 2, 0x0B);
        assertEquals(2, function.actuationType());
        assertEquals(0x0B, function.iconOrSpeed());
        for (int priority : new int[]{2, 3, 6}) {
            Cs1ObjectCommands.IndexedValueFrame value = parse(
                    Cs1ObjectCommands.IndexedValueFrame.class, objectId(priority, 2, false),
                    1, 0, 0x81);
            assertEquals(1, value.index());
            assertEquals(0x81, value.value());
        }
        Cs1ObjectCommands.IndexedValueFrame status = parse(
                Cs1ObjectCommands.IndexedValueFrame.class, objectId(4, 2, true), 2, 0, 1);
        assertEquals(2, status.index());
        assertEquals(1, status.value());
    }

    @Test
    void locoDisplayMetadataPreservesAmbiguousRailDetails() {
        parse(Cs1DecoderCommands.LocoTypeQueryFrame.class, objectId(6, 1, false),
                0x40, 3, 0, 0);
        Cs1DecoderCommands.LocoTypeReplyFrame type = parse(
                Cs1DecoderCommands.LocoTypeReplyFrame.class, objectId(6, 1, true),
                0x40, 3, 0, 0, 0, 3);
        assertEquals(3, type.locoType());
        for (int selector = 0; selector < 2; selector++) {
            Cs1DecoderCommands.RailFormatQueryFrame query = parse(
                    Cs1DecoderCommands.RailFormatQueryFrame.class, objectId(6, 1, false),
                    0x40, 1, selector, 0);
            assertEquals(selector, query.selector());
            Cs1DecoderCommands.RailFormatReplyFrame reply = parse(
                    Cs1DecoderCommands.RailFormatReplyFrame.class, objectId(6, 1, true),
                    0x40, 1, selector, 0, 0, 3, 0, 0);
            assertEquals(selector, reply.selector());
            assertEquals(3, reply.formatOrDetailCode());
            assertArrayEquals(new int[]{0, 3, 0, 0}, reply.formatData());
        }
    }

    @Test
    void ownershipCommandsAndRepliesAreSiblingTypes() {
        for (int priority : new int[]{5, 6}) {
            Cs1DecoderCommands.LocoAssignmentFrame command = parse(
                    Cs1DecoderCommands.LocoAssignmentFrame.class, objectId(priority, 0, false),
                    3, 1, 0, 8, 0x12, 0x34);
            assertEquals(1, command.controlByte());
            assertEquals(8, command.ownershipCode());
            assertEquals(0x1234, command.assignmentToken());
            Cs1DecoderCommands.LocoAssignmentReplyFrame reply = parse(
                    Cs1DecoderCommands.LocoAssignmentReplyFrame.class, objectId(priority, 0, true),
                    3, 0, 0xFF, 0xFF, 0, 1);
            assertEquals(0xFFFF, reply.ownershipCode());
            assertFalse(Cs1DecoderCommands.LocoAssignmentFrame.class.isInstance(reply));
        }
    }

    @Test
    void stackEnumerationAndNotificationsUseBigEndianHandles() {
        Cs1DecoderCommands.StackEntryQueryFrame query = parse(
                Cs1DecoderCommands.StackEntryQueryFrame.class, objectId(6, 3, false),
                0x80, 2, 0x12, 0x34);
        assertEquals(0x1234, query.previousHandle());
        Cs1DecoderCommands.StackEntryReplyFrame reply = parse(
                Cs1DecoderCommands.StackEntryReplyFrame.class, objectId(6, 3, true),
                0x80, 2, 0x12, 0x34, 0xAB, 0xCD, 0, 1);
        assertEquals(0x1234, reply.previousHandle());
        assertEquals(0xABCD, reply.locoHandle());
        Cs1DecoderCommands.StackEndFrame end = parse(
                Cs1DecoderCommands.StackEndFrame.class, objectId(6, 3, true),
                0x80, 2, 0xAB, 0xCD);
        assertEquals(0xABCD, end.previousHandle());
        Cs1DecoderCommands.StackAddFrame added = parse(
                Cs1DecoderCommands.StackAddFrame.class, objectId(6, 4, true),
                0x80, 0, 0xFF, 0xFF, 0xAB, 0xCD, 0, 3);
        assertEquals(0xABCD, added.locoHandle());
        Cs1DecoderCommands.StackRemoveFrame removed = parse(
                Cs1DecoderCommands.StackRemoveFrame.class, objectId(6, 5, true),
                0x80, 0, 0, 0, 0xAB, 0xCD, 0, 1);
        assertEquals(0xABCD, removed.locoHandle());
    }

    @Test
    void firmwareUsesDistinctFieldsAndUnsignedOffsets() {
        Cs1FirmwareCommands.FlashReadyFrame ready = parse(
                Cs1FirmwareCommands.FlashReadyFrame.class, firmwareId(0, 5, 0, 0),
                0x80, 0x12, 0x34, 0x56);
        assertEquals(0x80123456L, ready.slaveUid());
        parse(Cs1FirmwareCommands.FlashAbortFrame.class, firmwareId(0, 0, 0, 0), 0, 2, 4, 0xB3);
        parse(Cs1FirmwareCommands.FlashAuthorizeFrame.class, firmwareId(2, 0, 0, 0), 0, 2, 4, 0xB3);
        for (int stage = 1; stage <= 3; stage++) {
            if (stage < 3) {
                parse(Cs1FirmwareCommands.IdentityQueryFrame.class, firmwareId(2, 0, stage, 0),
                        0, 2, 4, 0xB3);
            }
            Cs1FirmwareCommands.IdentityReplyFrame reply = parse(
                    Cs1FirmwareCommands.IdentityReplyFrame.class, firmwareId(2, 0, stage, 0),
                    0, 2, 4, 0xB3, 0xFE, 0xDC, 0xBA, 0x98);
            assertEquals(stage, reply.stage());
            assertEquals(0xFEDCBA98L, reply.identityData());
        }
        Cs1FirmwareCommands.FlashDataQueryFrame query = parse(
                Cs1FirmwareCommands.FlashDataQueryFrame.class, firmwareId(2, 0, 0, 1),
                0xFE, 0x12, 0x34, 0x56, 0x81, 0x23, 0x45, 0x67);
        assertEquals(2, query.flashHandle());
        assertEquals(1, query.flags());
        assertEquals(0xFE123456L, query.offset());
        assertEquals(0x81234567L, query.byteCount());
        Cs1FirmwareCommands.FlashDataFrame data = parse(
                Cs1FirmwareCommands.FlashDataFrame.class, firmwareId(2, 0, 0, 2),
                0, 0, 2, 0x24, 0x36, 0x74);
        assertEquals(0x224, data.offset());
        assertArrayEquals(new int[]{0x36, 0x74}, data.firmwareBytes());
        parse(Cs1FirmwareCommands.FlashEndFrame.class, firmwareId(2, 0, 0, 3),
                0, 2, 4, 0xB3);
    }

    @Test
    void firmwareRegistrationAndZeroHandleMonitoringMatchActualIdentifiers() {
        Cs1ConfigCommands.UidFragmentFrame fragment = parse(
                Cs1ConfigCommands.UidFragmentFrame.class, 0x1C1C807E);
        assertEquals(7, fragment.uidFragment());
        assertEquals(1, fragment.stage());
        assertEquals(0, fragment.modifierId());
        Cs1ConfigCommands.MasterIdentityReplyFrame identity = parse(
                Cs1ConfigCommands.MasterIdentityReplyFrame.class, 0x1C0000FE,
                0, 7, 0x52, 0xE2, 0, 2, 0, 0);
        assertEquals(0x000752E2L, identity.masterUid());
        assertEquals(0, identity.monitorHandle());
        Cs1ConfigCommands.NodeAssignmentFrame node = parse(
                Cs1ConfigCommands.NodeAssignmentFrame.class, 0x1C0380FE,
                0, 7, 0x52, 0xE2, 0, 1, 1, 0);
        assertEquals(1, node.assignedNode());
        assertEquals(0, node.assignedMasterNode());
        parse(Cs1SystemCommands.KeepAliveFrame.class, 0x0C000381, 0, 0, 0, 0);
        Cs1SystemCommands.KeepAliveReplyFrame monitor = parse(
                Cs1SystemCommands.KeepAliveReplyFrame.class, 0x0C000380,
                0, 0, 0, 1, 0, 7, 0x52, 0xE2);
        assertEquals(1, monitor.targetNode());
        assertEquals(0x000752E2L, monitor.deviceUid());

        Cs1ConfigCommands.MasterIdentityReplyFrame shortIdentity = parse(
                Cs1ConfigCommands.MasterIdentityReplyFrame.class, registrationId(0, 0, true),
                0xFE, 0xDC, 0xBA, 0x98);
        assertFalse(shortIdentity.hasMonitorHandle());
        assertFalse(shortIdentity.hasRegistrationKind());
        assertThrows(IndexOutOfBoundsException.class, shortIdentity::monitorHandle);
        Cs1ConfigCommands.NodeAssignmentFrame shortNode = parse(
                Cs1ConfigCommands.NodeAssignmentFrame.class, registrationId(7, 0, true),
                0, 7, 0x52, 0xE2, 0xAB, 0xCD, 0xAB);
        assertFalse(shortNode.hasFlags());
        assertEquals(42, shortNode.assignedMasterNode());
        assertThrows(IndexOutOfBoundsException.class, shortNode::flags);
    }

    @Test
    void directoryShapesPreserveKindsControllersAndChangingCounters() {
        Cs1ObjectCommands.DirectoryFrame directory = parse(
                Cs1ObjectCommands.DirectoryFrame.class, objectId(6, 3, false), 0, 0xFF);
        assertEquals(-1, directory.step());
        assertFalse(directory.hasPreviousHandle());
        assertFalse(directory.hasResult());
        assertThrows(IndexOutOfBoundsException.class, directory::previousHandle);
        Cs1ObjectCommands.DirectoryFrame result = parse(
                Cs1ObjectCommands.DirectoryFrame.class, objectId(6, 3, true),
                0x40, 1, 0xAB, 0xCD, 0xFE, 0xDC, 0x81, 0x23);
        assertEquals(0xFEDC, result.resultHandle());
        assertEquals(0x8123, result.changeCounter());
        for (int kind = 0x80; kind <= 0x82; kind++) {
            Cs1DecoderCommands.StackEntryReplyFrame entry = parse(
                    Cs1DecoderCommands.StackEntryReplyFrame.class, objectId(6, 3, true),
                    kind, 2, 0xAB, 0xCD, 0xFE, 0xDC, 0x81, 0x23);
            assertEquals(kind, entry.kind());
            assertEquals(0x8123, entry.changeCounter());
            Cs1DecoderCommands.StackAddFrame added = parse(
                    Cs1DecoderCommands.StackAddFrame.class, objectId(6, 4, true),
                    kind, 0, 1, 1, 0xFE, 0xDC, 0x81, 0x23);
            assertEquals(0x101, added.controllerHandle());
            assertEquals(0x8123, added.changeCounter());
        }
        Cs1DecoderCommands.StackRemoveFrame removal = parse(
                Cs1DecoderCommands.StackRemoveFrame.class, objectId(6, 5, false),
                0, 0, 1, 1, 0xFE, 0xDC, 0x81, 0x23);
        assertEquals(0xFEDC, removal.locoHandle());
        assertEquals(0x101, removal.controllerHandle());
        Cs1ObjectCommands.ObjectAllocationFrame allocation = parse(
                Cs1ObjectCommands.ObjectAllocationFrame.class, objectId(6, 4, false), 0x80);
        assertFalse(allocation.hasTag());
        assertFalse(allocation.hasController());
        parse(Cs1ObjectCommands.ObjectAllocationFrame.class, objectId(6, 4, false),
                0x81, 0, 1, 1);
        Cs1DecoderCommands.StackEntryReplyFrame shortResult = parse(
                Cs1DecoderCommands.StackEntryReplyFrame.class, objectId(6, 3, true),
                0x80, 2, 0, 0, 0xFE, 0xDC);
        assertEquals(0xFEDC, shortResult.locoHandle());
        assertFalse(shortResult.hasChangeCounter());
        assertThrows(IndexOutOfBoundsException.class, shortResult::changeCounter);
        Cs1ObjectCommands.SystemHandleReplyFrame shortSystem = parse(
                Cs1ObjectCommands.SystemHandleReplyFrame.class, objectId(6, 3, true),
                3, 0, 0, 0, 0xFE, 0xDC);
        assertFalse(shortSystem.hasChangeCounter());
        assertEquals(0xFEDC, shortSystem.systemHandle());
        Cs1DecoderCommands.StackRemoveFrame shortRemoval = parse(
                Cs1DecoderCommands.StackRemoveFrame.class, objectId(6, 5, true),
                0, 0, 0, 0, 0xFE, 0xDC);
        assertFalse(shortRemoval.hasChangeCounter());
        assertEquals(0xFEDC, shortRemoval.locoHandle());
    }

    @Test
    void identificationAndKindResponsesExposeOnlyTheirTransmittedSegments() {
        int[][] selectors = {{0, 0}, {1, 0}, {1, 1}, {1, 2}, {2, 0}, {2, 1},
            {3, 0}, {3, 1}, {3, 2}, {3, 3}, {4, 0}, {5, 0}, {5, 1}};
        for (int[] selector : selectors) {
            for (int offset = 0; offset < 4; offset++) {
                Cs1ObjectCommands.IdentificationFrame query = parse(
                        Cs1ObjectCommands.IdentificationFrame.class, objectId(6, 1, false),
                        1, selector[0], selector[1], offset);
                assertFalse(query.hasValue());
                Cs1ObjectCommands.IdentificationFrame segment = parse(
                        Cs1ObjectCommands.IdentificationFrame.class, objectId(6, 1, true),
                        1, selector[0], selector[1], offset, 0xFE);
                assertEquals(offset, segment.offset());
                assertArrayEquals(new int[]{0xFE}, segment.valueBytes());
            }
        }
        Cs1ObjectCommands.ObjectKindFrame kinds = parse(
                Cs1ObjectCommands.ObjectKindFrame.class, objectId(6, 1, true),
                0, 0, 0, 0, 0x80, 0x82);
        assertArrayEquals(new int[]{0x80, 0x82}, kinds.valueBytes());
        parse(Cs1ObjectCommands.StackSizeReplyFrame.class, objectId(6, 1, true),
                0x41, 0, 0x80, 0, 0, 10);
    }

    @Test
    void decoderPropertiesAndControllerStateDoNotInventMissingOrAmbiguousValues() {
        parse(Cs1DecoderCommands.DecoderResetFrame.class, objectId(6, 1, false),
                0x40, 0, 0, 0);
        Cs1DecoderCommands.ProgrammingValueFrame programming = parse(
                Cs1DecoderCommands.ProgrammingValueFrame.class, objectId(6, 1, false),
                0x40, 2, 0, 0, 0, 0xFE, 0xAB, 0xCD);
        assertEquals(0xFE, programming.value());
        assertEquals(0xABCD, programming.cv());
        Cs1DecoderCommands.AddressValueFrame address = parse(
                Cs1DecoderCommands.AddressValueFrame.class, objectId(6, 1, false),
                0x40, 1, 1, 0, 0xAB, 0xCD);
        assertEquals(0xABCD, address.address());
        Cs1DecoderCommands.RailFormatReplyFrame details = parse(
                Cs1DecoderCommands.RailFormatReplyFrame.class, objectId(6, 1, true),
                0x40, 1, 1, 0, 0xAB, 0xCD, 0, 3);
        assertEquals(0xABCD, details.address());
        assertEquals(3, details.flags());
        Cs1DecoderCommands.DecoderPropertyFrame slice = parse(
                Cs1DecoderCommands.DecoderPropertyFrame.class, objectId(6, 1, true),
                0x40, 1, 0, 3, 0xFE);
        assertArrayEquals(new int[]{0xFE}, slice.valueBytes());
        parse(Cs1DecoderCommands.VolumeQueryFrame.class, objectId(6, 1, false),
                0x40, 4, 0, 0);
        Cs1DecoderCommands.VolumeDataFrame volume = parse(
                Cs1DecoderCommands.VolumeDataFrame.class, objectId(6, 1, true),
                0x40, 4, 0, 0, 0, 0x40);
        assertArrayEquals(new int[]{0, 0x40}, volume.valueBytes());
        parse(Cs1DecoderCommands.ControllerQueryFrame.class, objectId(6, 0, false), 3, 0);
        Cs1DecoderCommands.ControllerStateFrame controller = parse(
                Cs1DecoderCommands.ControllerStateFrame.class, objectId(5, 0, true),
                3, 0, 0xAB, 0xCD, 0x81, 0x23, 1);
        assertEquals(0xABCD, controller.controllerHandle());
        assertEquals(0x8123, controller.changeCounter());
        assertTrue(controller.masterLocallyDriven());
    }

    @Test
    void functionKindsAndParametersAreIndependentOfPriorityAndIndex() {
        for (int priority : new int[]{2, 4, 6}) {
            Cs1ObjectCommands.FunctionDescriptionReplyFrame function = parse(
                    Cs1ObjectCommands.FunctionDescriptionReplyFrame.class,
                    objectId(priority, 2, true), 7, 1, 2, 0x8B);
            assertTrue(function.momentary());
            assertEquals(0x0B, function.icon());
            assertFalse(function.describesSpeed());
            Cs1ObjectCommands.FunctionDescriptionReplyFrame speed = parse(
                    Cs1ObjectCommands.FunctionDescriptionReplyFrame.class,
                    objectId(priority, 2, true), 7, 1, 1, 1);
            assertTrue(speed.describesSpeed());
            for (int parameter = 0; parameter <= 2; parameter++) {
                parse(Cs1ObjectCommands.SpeedParameterQueryFrame.class,
                        objectId(priority, 2, false), 7, 2, parameter, 0);
                Cs1ObjectCommands.SpeedParameterValueFrame value = parse(
                        Cs1ObjectCommands.SpeedParameterValueFrame.class,
                        objectId(priority, 2, true), 7, 2, parameter, 0, 0xFE);
                assertEquals(parameter, value.parameter());
                assertEquals(0xFE, value.value());
            }
        }
    }

    @Test
    void shortAndUnmodelledPayloadsRetainBaseFrameWithoutReadingPadding() {
        int[][] candidates = {
            raw(objectId(6, 2, false), 2, 2),
            raw(objectId(5, 0, false), 3, 1, 0, 8, 0),
            raw(firmwareId(2, 0, 0, 1), 0, 0, 0, 0, 0, 2, 0x26),
            raw(firmwareId(2, 0, 3, 0), 0, 2, 4, 0xB3),
            raw(registrationId(5, 0, true), 0, 0, 0, 0, 0, 0, 0, 0)
        };
        for (int[] bytes : candidates) {
            for (int i = 5 + bytes[4]; i < bytes.length; i++) {
                bytes[i] = 0xFF;
            }
            Cs1Frame frame = assertInstanceOf(Cs1Frame.class, ProtocolClassifier.classify(bytes));
            assertEquals(Cs1Frame.class, frame.getClass());
            assertArrayEquals(bytes, frame.raw().toArray());
        }
        int[] shortName = raw(objectId(6, 1, true), 2, 0, 0, 0, 'X');
        shortName[10] = 'Y';
        Cs1ObjectCommands.NameFragmentFrame name = assertInstanceOf(
                Cs1ObjectCommands.NameFragmentFrame.class, ProtocolClassifier.classify(shortName));
        assertArrayEquals(new int[]{'X'}, name.nameBytes());
    }

    @Test
    void truncatedShapesNeverReadTransportPaddingDuringClassification() {
        int[][] payloads = {
            {3, 0, 1, 1, 0, 1, 1}, {0x40, 1, 1, 0, 0, 3, 0, 1},
            {0x80, 2, 0, 0, 2, 0, 0, 1}, {0x80, 0, 0, 0, 2, 0, 0, 1},
            {1, 1, 2, 0, 0xFE, 0xDC}, {7, 1, 2, 0x8B},
            {7, 2, 1, 0, 0xFE}, {0, 0, 0, 1, 0xFE, 0xDC, 0xBA, 0x98}
        };
        int[] commands = {0, 1, 3, 4, 1, 2, 2, 7};
        for (int i = 0; i < payloads.length; i++) {
            for (int length = 0; length <= payloads[i].length; length++) {
                int[] bytes = raw(objectId(6, commands[i], true),
                        java.util.Arrays.copyOf(payloads[i], length));
                for (int padding = 5 + length; padding < bytes.length; padding++) {
                    bytes[padding] = 0xFF;
                }
                assertDoesNotThrow(() -> ProtocolClassifier.classify(bytes));
            }
        }
    }
}
