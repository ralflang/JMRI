package jmri.util.startup;

import jmri.JmriException;

import org.junit.Assert;
import org.junit.jupiter.api.*;

/**
 *
 * @author Randall Wood (C) 2016
 */
public class AbstractStartupModelTest {

    /**
     * Test of getName method, of class AbstractStartupModel.
     */
    @Test
    public void testGetName() {
        AbstractStartupModel model = new AbstractStartupModelImpl();
        Assert.assertNull("Name defaults to null", model.getName());
        model.setName("");
        Assert.assertNotNull("Name should be empty", model.getName());
        Assert.assertTrue("Name should be empty", model.getName().isEmpty());
        Assert.assertEquals("Name should be empty", "", model.getName());
        model.setName("name");
        Assert.assertNotNull("Name should not be empty", model.getName());
        Assert.assertFalse("Name should not be empty", model.getName().isEmpty());
        Assert.assertEquals("Name should not be empty", "name", model.getName());
    }

    /**
     * Test of setName method, of class AbstractStartupModel.
     */
    @Test
    public void testSetName() {
        AbstractStartupModel model = new AbstractStartupModelImpl();
        Assert.assertNull("Name defaults to null", model.getName());
        model.setName("");
        Assert.assertNotNull("Name should be empty", model.getName());
        Assert.assertTrue("Name should be empty", model.getName().isEmpty());
        Assert.assertEquals("Name should be empty", "", model.getName());
        model.setName("name");
        Assert.assertNotNull("Name should not be empty", model.getName());
        Assert.assertFalse("Name should not be empty", model.getName().isEmpty());
        Assert.assertEquals("Name should not be empty", "name", model.getName());
    }

    /**
     * Test of toString method, of class AbstractStartupModel.
     */
    @Test
    public void testToString() {
        AbstractStartupModel model = new AbstractStartupModelImpl();
        Assert.assertNotNull("toString defaults to nonnull", model.toString());
        model.setName("");
        Assert.assertNotNull("toString should be empty", model.toString());
        Assert.assertTrue("toString should be empty", model.toString().isEmpty());
        Assert.assertEquals("toString should be empty", "", model.toString());
        model.setName("name");
        Assert.assertNotNull("toString should not be empty", model.toString());
        Assert.assertFalse("toString should not be empty", model.toString().isEmpty());
        Assert.assertEquals("toString should not be empty", "name", model.toString());
    }

    /**
     * Test of isValid method, of class AbstractStartupModel.
     */
    @Test
    public void testIsValid() {
        AbstractStartupModel model = new AbstractStartupModelImpl();
        Assert.assertFalse("Model default state is invalid", model.isValid());
        model.setName("");
        Assert.assertFalse("Empty name is invalid", model.isValid());
        model.setName("name");
        Assert.assertTrue("Nonempty name is valid", model.isValid());
    }

    @BeforeEach
    public void setUp() {
        jmri.util.JUnitUtil.setUp();
    }

    @AfterEach
    public void tearDown() {
        jmri.util.JUnitUtil.tearDown();
    }

    /**
     * Minimal implementation of AbstractStartupModel
     */
    private static class AbstractStartupModelImpl extends AbstractStartupModel {

        @Override
        public void performAction() throws JmriException {
            // empty method not tested as abstract in class being tested
        }
    }

}
