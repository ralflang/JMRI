package jmri.jmrit.dispatcher;

import java.awt.GraphicsEnvironment;

import jmri.InstanceManager;
import jmri.util.JUnitUtil;

import org.junit.jupiter.api.*;
import org.junit.Assert;
import org.junit.Assume;

/**
 *
 * @author Paul Bender Copyright (C) 2017
 */
public class AllocatedSectionTest {

    @Test
    public void testCTor() {
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());
        OptionsFile.setDefaultFileName("java/test/jmri/jmrit/dispatcher/dispatcheroptions.xml");  // exist?
        DispatcherFrame d = InstanceManager.getDefault(DispatcherFrame.class);
        jmri.Transit transit = new jmri.implementation.DefaultTransit("TT1");
        ActiveTrain at = new ActiveTrain(transit, "Train", ActiveTrain.USER);
        jmri.Section section1 = new jmri.implementation.DefaultSection("TS1");
        jmri.Section section2 = new jmri.implementation.DefaultSection("TS2");
        AllocatedSection t = new AllocatedSection(section1, at, 1, section2, 2);
        Assert.assertNotNull("exists", t);
        JUnitUtil.dispose(d);
    }

    @BeforeEach
    public void setUp() {
        JUnitUtil.setUp();
        JUnitUtil.resetProfileManager();
    }

    @AfterEach
    public void tearDown() {
        JUnitUtil.deregisterBlockManagerShutdownTask();
        JUnitUtil.tearDown();
    }

    // private final static Logger log = LoggerFactory.getLogger(AllocatedSectionTest.class);
}
