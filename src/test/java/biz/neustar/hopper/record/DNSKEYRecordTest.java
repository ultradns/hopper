/**
 * Copyright 2013 NeuStar, Inc. All rights reserved.
 * NeuStar, the Neustar logo and related names and logos are registered
 * trademarks, service marks or tradenames of NeuStar, Inc. All other
 * product names, company names, marks, logos and symbols may be trademarks
 * of their respective owners.
 */

package biz.neustar.hopper.record;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;
import java.util.Arrays;

import org.junit.Test;

import biz.neustar.hopper.exception.RelativeNameException;
import biz.neustar.hopper.exception.TextParseException;
import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.DNSSEC;
import biz.neustar.hopper.message.DNSSEC.Algorithm;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.util.Tokenizer;

public class DNSKEYRecordTest {
    
    // TODO: this not great, but previously it would use 0, which isnt in a valid algorithm as a default..
    @Test(expected=NullPointerException.class)
    public void testFootPrintThrows() throws Exception {
        DNSKEYRecord ar = new DNSKEYRecord();
        assertNull(ar.getAlgorithm());
        assertEquals(0, ar.getFootprint());
    }
    
    @Test
    public void testFieldDefaults() throws UnknownHostException {
        DNSKEYRecord ar = new DNSKEYRecord();
        assertNull(ar.getName());
        assertEquals(0, ar.getType());
        assertNull(ar.getDClass());
        assertEquals(0, ar.getTTL());
        assertNull(ar.getAlgorithm());
        assertEquals(0, ar.getFlags());
        assertEquals(0, ar.getProtocol());
        assertNull(ar.getKey());
        
        Record r = ar.getObject();
        assertNotNull(r);
        assertTrue(r instanceof DNSKEYRecord);
    }

    @Test(expected=RelativeNameException.class)
    public void testBadParams() throws Exception {
        new DNSKEYRecord(Name.fromString("something.relative"), 
                DClass.IN, 0x24AC, 0x9832, 0x12, Algorithm.valueOf(0x67), new byte[] { 0, 1, 3, 5, 7, 9 });
    }
    
    @Test
    public void testSettingFields() throws TextParseException {
        Name name = Name.fromString("My.Absolute.Name.");
        byte[] key = new byte[] { 0, 1, 3, 5, 7, 9 };

        Algorithm algorithm = Algorithm.valueOf(0x67);
        DNSKEYRecord keyRec = new DNSKEYRecord(name, DClass.IN, 0x24AC, 0x9832, 0x12,
                algorithm, key);
        
        assertEquals(name, keyRec.getName());
        assertEquals(Type.DNSKEY, keyRec.getType());
        assertEquals(DClass.IN, keyRec.getDClass());
        assertEquals(0x24AC, keyRec.getTTL());
        assertEquals(0x9832, keyRec.getFlags());
        assertEquals(0x12, keyRec.getProtocol());
        assertEquals(algorithm, keyRec.getAlgorithm());
        assertTrue(Arrays.equals(key, keyRec.getKey()));
        assertEquals(46248, keyRec.getFootprint());
    }

    @Test
    public void testStringData() throws Exception {
        // basic
        DNSKEYRecord kr = new DNSKEYRecord();
        Tokenizer st = new Tokenizer(0xABCD + " " + 0x81
                + " RSASHA1 AQIDBAUGBwgJ");
        kr.rdataFromString(st, null);
        assertEquals(0xABCD, kr.getFlags());
        assertEquals(0x81, kr.getProtocol());
        assertEquals(DNSSEC.Algorithm.RSASHA1, kr.getAlgorithm());
        assertTrue(Arrays.equals(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                kr.getKey()));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testInvalidAlgorithmString() throws Exception {
        // invalid algorithm
        DNSKEYRecord kr = new DNSKEYRecord();
        Tokenizer st = new Tokenizer(0x1212 + " " + 0xAA + " ZONE AQIDBAUGBwgJ");
        kr.rdataFromString(st, null);
    }
}
