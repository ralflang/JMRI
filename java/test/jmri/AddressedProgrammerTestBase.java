package jmri;

import org.junit.Assert;
import org.junit.jupiter.api.*;

/**
 * Base JUnit tests for the Operations Mode Programmers derived from
 * the AddressedProgrammer interface
 * <p>
 * Copyright: Copyright (c) 2002</p>
 *
 * @author Bob Jacobsen
 * @author Paul Bender copyright (C) 2018 
 */
abstract public class AddressedProgrammerTestBase extends ProgrammerTestBase {

    @Test
    @Override
    public void testDefault() {
        Assumptions.assumeTrue(programmer instanceof AddressedProgrammer);
        Assert.assertEquals("Check Default", ProgrammingMode.OPSBYTEMODE,
                programmer.getMode());        
    }

    @Test
    @Override
    public void testGetCanRead() {
        Assumptions.assumeTrue(programmer instanceof AddressedProgrammer);
        Assert.assertFalse("can read", programmer.getCanRead());
    }
    
    @Test
    @Override
    public void testSetGetMode() {
        Assumptions.assumeTrue(programmer instanceof AddressedProgrammer);
        Assert.assertThrows(IllegalArgumentException.class, () -> programmer.setMode(ProgrammingMode.REGISTERMODE));
    }

    @Test
    public void testGetLongAddress(){
        Assumptions.assumeTrue(programmer instanceof AddressedProgrammer);
        Assert.assertNotNull("long/short address boolean",((AddressedProgrammer)programmer).getLongAddress());
    }

    @Test
    public void testGetAddressNumber(){
        Assumptions.assumeTrue(programmer instanceof AddressedProgrammer);
        Assert.assertNotNull("Numeric Address",((AddressedProgrammer)programmer).getAddressNumber());
    }

    @Test
    public void testGetAddress(){
        Assumptions.assumeTrue(programmer instanceof AddressedProgrammer);
        Assert.assertNotNull("String Address",((AddressedProgrammer)programmer).getAddress());
    }

    // must set the value of programmer in setUp.
    @Override
    abstract public void setUp();

}
