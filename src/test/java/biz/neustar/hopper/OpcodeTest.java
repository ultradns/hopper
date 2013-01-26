package biz.neustar.hopper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import biz.neustar.hopper.message.Opcode;

public class OpcodeTest {
    
    @Test(expected=IllegalArgumentException.class)
    public void testInvalidValue() {
        // (max is 0xF)
        Opcode.valueOf(0x10);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testInvalidValue2() {
        Opcode.valueOf(-1);
    }
    
    
    @Test
    public void testValidStrings() {
        // a regular one
        assertEquals("IQUERY", Opcode.IQUERY.getName());

        // one that doesn't exist
        assertTrue(Opcode.valueOf(6).getName().startsWith("RESERVED"));
    }

    @Test
    public void testValidValueOf() {
        assertEquals(Opcode.STATUS, Opcode.valueOf("STATUS"));
    }
    
    @Test
    public void testReservedValue() {
        assertEquals(6, Opcode.valueOf("RESERVED6").getValue());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testInvalidReservedValue() {
        assertEquals(-1, Opcode.valueOf("RESERVED" + 0x10).getValue());
    }
    
    
    @Test(expected=IllegalArgumentException.class)
    public void testUnknownValueOf() {
        assertEquals(-1, Opcode.valueOf("SOMETHING THAT IS UNKNOWN").getValue());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testEmptyValueOf() {
        assertEquals(-1, Opcode.valueOf("").getValue());
    }
}

